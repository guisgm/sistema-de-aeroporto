package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class PlanejamentoServico {
    private final BancoDados banco;
    private final PlanejamentoJdbc repositorio;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;

    public PlanejamentoServico(BancoDados banco,PlanejamentoJdbc repositorio,AutorizacaoJdbc autorizacao,AuditoriaJdbc auditoria,Clock relogio) {
        this.banco=banco; this.repositorio=repositorio; this.autorizacao=autorizacao; this.auditoria=auditoria; this.relogio=relogio;
    }

    public long programar(Sessao sessao,ProgramacaoVoo voo) {
        Dados.exigir(voo.partida().isAfter(relogio.instant()),"Programe uma partida futura.");
        return banco.transacao(c->{
            autorizacao.exigirOperacao(c,sessao); repositorio.serializar(c);
            auditoria.definirAutorDoVoo(c,sessao.usuarioId(),"Programacao");
            long id=repositorio.programar(c,voo);
            auditoria.registrar(c,sessao.usuarioId(),"PROGRAMAR_VOO","voo",id); return id;
        });
    }

    public long tarifa(Sessao sessao,long voo,Long id,Tarifa tarifa,boolean ativa) {
        return banco.transacao(c->{
            autorizacao.exigirOperacao(c,sessao);
            var v=BloqueiosJdbc.voo(c,voo);
            Dados.exigir("PROGRAMADO".equals(v.get("situacao")) || "CHECKIN_ABERTO".equals(v.get("situacao")),"Voo nao admite alteracao comercial.");
            long salvo=repositorio.tarifa(c,voo,id,tarifa,ativa);
            auditoria.registrar(c,sessao.usuarioId(),"SALVAR_TARIFA","tarifa_voo",salvo); return salvo;
        });
    }

    public void bloquearAssento(Sessao sessao,long inventario,boolean bloqueado) {
        banco.transacao(c->{
            autorizacao.exigirOperacao(c,sessao);
            var inicial=Sql.registro(c,"SELECT voo_id FROM inventario_assento_voo WHERE id=?",inventario);
            BloqueiosJdbc.voo(c,Dados.id(inicial,"voo_id"));
            Sql.registro(c,"SELECT id FROM inventario_assento_voo WHERE id=? FOR UPDATE",inventario);
            Dados.exigir(Sql.listar(c,"SELECT id FROM ocupacao_assento WHERE inventario_id=? AND liberada_em IS NULL",r->r.getLong(1),inventario).isEmpty(),"Assento ocupado.");
            Sql.executar(c,"UPDATE inventario_assento_voo SET bloqueado=? WHERE id=?",bloqueado,inventario);
            auditoria.registrar(c,sessao.usuarioId(),"BLOQUEAR_ASSENTO","inventario_assento_voo",inventario);return null;
        });
    }

    public void reprogramar(Sessao sessao,long id,ProgramacaoVoo nova,String motivo) {
        String razao=Validacao.texto(motivo,"Motivo",500);
        banco.transacao(c->{
            autorizacao.exigirOperacao(c,sessao);repositorio.serializar(c);
            var anterior=BloqueiosJdbc.voo(c,id);
            Dados.exigir("PROGRAMADO".equals(anterior.get("situacao")),"Reprogramacao somente antes de abrir check-in.");
            Dados.exigir(Dados.id(anterior,"companhia_id")==nova.companhia() && Dados.id(anterior,"rota_id")==nova.rota() && Dados.id(anterior,"aeronave_id")==nova.aeronave() && anterior.get("numero").equals(nova.numero()),"Reprogramacao altera horarios; use a troca de aeronave separadamente.");
            Dados.exigir(nova.partida().isAfter(relogio.instant()),"Partida deve ser futura.");
            repositorio.validarFrota(c,nova.aeronave(),nova.companhia(),nova.rota(),nova.partida(),nova.chegada(),id);
            auditoria.definirAutorDoVoo(c,sessao.usuarioId(),razao);
            Sql.executar(c,"""
                    UPDATE voo SET partida_prevista=?,chegada_prevista=?,checkin_abre=?,checkin_fecha=?,embarque_abre=?,embarque_fecha=?,partida_estimada=NULL,chegada_estimada=NULL WHERE id=?
                    """,nova.partida(),nova.chegada(),nova.checkinAbre(),nova.checkinFecha(),nova.embarqueAbre(),nova.embarqueFecha(),id);
            repositorio.agendas(c,anterior,nova.partida(),nova.chegada());
            OperacaoJdbc.validarEscalasDoVoo(c,id);
            auditoria.registrar(c,sessao.usuarioId(),"REPROGRAMAR_VOO","voo",id);return null;
        });
    }

    public void horarios(Sessao sessao,long id,Instant partida,Instant chegada,boolean reais,String motivo) {
        Dados.exigir(partida!=null && chegada!=null && chegada.isAfter(partida),"Horarios invalidos.");
        String razao=Validacao.texto(motivo,"Motivo",500);
        banco.transacao(c->{
            autorizacao.exigirOperacao(c,sessao);repositorio.serializar(c);
            var v=BloqueiosJdbc.voo(c,id);String estado=Dados.texto(v,"situacao");
            Dados.exigir(!estado.equals("CONCLUIDO") && !estado.equals("CANCELADO"),"Voo encerrado.");
            if(reais) Dados.exigir(estado.equals("EM_VOO") && !partida.isAfter(relogio.instant()) && !chegada.isAfter(relogio.instant()),"Horarios reais requerem voo em voo e instantes ja ocorridos.");
            else Dados.exigir(!estado.equals("EM_VOO"),"Voo em voo requer registro real.");
            repositorio.validarFrota(c,Dados.id(v,"aeronave_id"),Dados.id(v,"companhia_id"),Dados.id(v,"rota_id"),partida,chegada,id);
            auditoria.definirAutorDoVoo(c,sessao.usuarioId(),razao);
            if(reais) Sql.executar(c,"UPDATE voo SET partida_real=?,chegada_real=?,motivo_atraso=? WHERE id=?",partida,chegada,razao,id);
            else {
                Duration delta=Duration.between(PlanejamentoJdbc.efetivo(v,"partida"),partida);
                Sql.executar(c,"UPDATE voo SET partida_estimada=?,chegada_estimada=?,motivo_atraso=? WHERE id=?",partida,chegada,razao,id);
                // O DDL ancora as janelas na partida prevista; em atraso a janela efetiva e calculada pelo atendimento.
            }
            repositorio.agendas(c,v,partida,chegada);OperacaoJdbc.validarEscalasDoVoo(c,id);
            auditoria.registrar(c,sessao.usuarioId(),reais?"HORARIOS_REAIS":"HORARIOS_ESTIMADOS","voo",id);return null;
        });
    }

    public void trocarAeronave(Sessao sessao,long voo,long aeronave,String motivo) {
        String razao=Validacao.texto(motivo,"Motivo",500);
        banco.transacao(c->{
            autorizacao.exigirOperacao(c,sessao);repositorio.serializar(c);
            var v=BloqueiosJdbc.voo(c,voo);
            Dados.exigir("PROGRAMADO".equals(v.get("situacao")),"Troca somente em voo programado.");
            Dados.exigir(Sql.listar(c,"SELECT id FROM item_reserva WHERE voo_id=?",r->r.getLong(1),voo).isEmpty(),"Troca recusada: existem itens historicos.");
            Dados.exigir(Sql.listar(c,"SELECT id FROM alocacao_recurso WHERE voo_id=? AND ativa UNION ALL SELECT id FROM escala_funcionario WHERE voo_id=? AND ativa",r->r.getLong(1),voo,voo).isEmpty(),"Libere recursos e equipe antes de trocar a aeronave.");
            repositorio.validarFrota(c,aeronave,Dados.id(v,"companhia_id"),Dados.id(v,"rota_id"),PlanejamentoJdbc.efetivo(v,"partida"),PlanejamentoJdbc.efetivo(v,"chegada"),voo);
            auditoria.definirAutorDoVoo(c,sessao.usuarioId(),razao);
            Sql.executar(c,"DELETE FROM inventario_assento_voo WHERE voo_id=?",voo);
            Sql.executar(c,"DELETE FROM agenda_aeronave WHERE voo_id=?",voo);
            Sql.executar(c,"UPDATE voo SET aeronave_id=? WHERE id=?",aeronave,voo);
            Sql.executar(c,"INSERT INTO agenda_aeronave(aeronave_id,voo_id,inicio,fim) VALUES (?,?,?,?)",aeronave,voo,PlanejamentoJdbc.efetivo(v,"partida").minus(Duration.ofMinutes(45)),PlanejamentoJdbc.efetivo(v,"chegada").plus(Duration.ofMinutes(30)));
            repositorio.inventario(c,voo,aeronave);auditoria.registrar(c,sessao.usuarioId(),"TROCAR_AERONAVE","voo",voo);return null;
        });
    }
}
