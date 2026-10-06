package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Set;

public final class VooServico {
    private final BancoDados banco;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;
    private final ReservaJdbc reservas=new ReservaJdbc();
    private final PlanejamentoJdbc planejamento=new PlanejamentoJdbc();

    public VooServico(BancoDados banco,AutorizacaoJdbc autorizacao,AuditoriaJdbc auditoria,Clock relogio) {
        this.banco=banco;this.autorizacao=autorizacao;this.auditoria=auditoria;this.relogio=relogio;
    }

    public void transicao(Sessao s,long voo,SituacaoVoo proxima,String motivo) {
        String razao=Validacao.texto(motivo,"Motivo",500);
        banco.transacaoRepetivel(c->reservas.expirar(c,relogio.instant()));
        banco.transacaoRepetivel(c->{
            autorizacao.exigirOperacao(c,s);planejamento.serializar(c);
            var ids=Sql.listar(c,"SELECT DISTINCT reserva_id FROM item_reserva WHERE voo_id=? ORDER BY reserva_id",r->r.getLong(1),voo);
            var todos=new ArrayList<Long>();todos.add(voo);
            for(long id:ids) todos.addAll(Sql.listar(c,"SELECT voo_id FROM item_reserva WHERE reserva_id=?",r->r.getLong(1),id));
            BloqueiosJdbc.voos(c,todos);for(long id:ids) BloqueiosJdbc.reserva(c,id);
            var v=BloqueiosJdbc.voo(c,voo);SituacaoVoo atual=SituacaoVoo.valueOf(Dados.texto(v,"situacao"));
            Dados.exigir(atual.permiteTransicaoPara(proxima),"Transicao de voo invalida.");
            auditoria.definirAutorDoVoo(c,s.usuarioId(),razao);
            switch(proxima) {
                case CHECKIN_ABERTO -> Dados.exigir(!relogio.instant().isBefore(ReservaJdbc.janela(v,"checkin_abre")) && relogio.instant().isBefore(ReservaJdbc.janela(v,"checkin_fecha")),"Fora da janela de check-in.");
                case EMBARQUE -> {
                    Dados.exigir(!relogio.instant().isBefore(ReservaJdbc.janela(v,"embarque_abre")) && relogio.instant().isBefore(ReservaJdbc.janela(v,"embarque_fecha")),"Fora da janela de embarque.");
                    OperacaoJdbc.validarTripulacao(c,voo);
                    Dados.exigir(!Sql.listar(c,"SELECT a.id FROM alocacao_recurso a JOIN recurso_aeroportuario r ON r.id=a.recurso_id WHERE a.voo_id=? AND a.ativa AND a.finalidade='PARTIDA' AND r.tipo='PORTAO' AND a.inicio<=? AND a.fim>=?",r->r.getLong(1),voo,ReservaJdbc.janela(v,"embarque_abre"),ReservaJdbc.janela(v,"embarque_fecha")).isEmpty(),"Portao nao cobre a janela de embarque.");
                }
                case EM_VOO -> {
                    Dados.exigir(!relogio.instant().isBefore(ReservaJdbc.janela(v,"embarque_fecha")),"Embarque ainda nao encerrou.");OperacaoJdbc.validarTripulacao(c,voo);
                    Sql.registro(c,"SELECT id FROM aeronave WHERE id=? AND situacao='ATIVA' FOR SHARE",Dados.id(v,"aeronave_id"));
                    Dados.exigir(Sql.listar(c,"SELECT id FROM servico_solo WHERE voo_id=? AND situacao IN ('PENDENTE','EM_EXECUCAO')",r->r.getLong(1),voo).isEmpty(),"Servicos de solo pendentes.");
                    for(var item:Sql.listar(c,"SELECT * FROM item_reserva WHERE voo_id=? AND situacao IN ('PENDENTE','CONFIRMADO') ORDER BY id",Sql::linha,voo)) {
                        String estado="CONFIRMADO".equals(item.get("situacao"))?"NAO_COMPARECEU":"EXPIRADO";
                        Sql.executar(c,"UPDATE item_reserva SET situacao=? WHERE id=?",estado,Dados.id(item,"id"));reservas.invalidar(c,Dados.id(item,"id"),relogio.instant(),s.usuarioId());
                        reservas.recalcular(c,Dados.id(item,"reserva_id"));
                    }
                    Sql.executar(c,"UPDATE voo SET partida_real=? WHERE id=?",relogio.instant(),voo);
                    planejamento.agendas(c,v,relogio.instant(),PlanejamentoJdbc.efetivo(v,"chegada"));
                }
                case CONCLUIDO -> {
                    Dados.exigir(v.get("chegada_real")!=null && !Dados.instante(v,"chegada_real").isAfter(relogio.instant()),"Registre a chegada real antes de concluir.");
                    Sql.executar(c,"UPDATE agenda_aeronave SET ativa=false WHERE voo_id=?",voo);Sql.executar(c,"UPDATE alocacao_recurso SET ativa=false WHERE voo_id=?",voo);Sql.executar(c,"UPDATE escala_funcionario SET ativa=false WHERE voo_id=?",voo);
                }
                case CANCELADO -> {
                    for(long reserva:ids) {
                        BigDecimal valor=BigDecimal.ZERO;
                        for(var item:Sql.listar(c,"SELECT * FROM item_reserva WHERE voo_id=? AND reserva_id=? ORDER BY id",Sql::linha,voo,reserva)) valor=valor.add(reservas.cancelarItem(c,item,relogio.instant(),razao,true,s.usuarioId()));
                        reservas.solicitarDevolucao(c,reserva,valor,"voo:"+voo+":"+reserva,razao);reservas.recalcular(c,reserva);
                    }
                    Sql.executar(c,"UPDATE agenda_aeronave SET ativa=false WHERE voo_id=?",voo);Sql.executar(c,"UPDATE alocacao_recurso SET ativa=false WHERE voo_id=?",voo);Sql.executar(c,"UPDATE escala_funcionario SET ativa=false WHERE voo_id=?",voo);
                    Sql.executar(c,"UPDATE servico_solo SET situacao='CANCELADO' WHERE voo_id=? AND situacao IN ('PENDENTE','EM_EXECUCAO')",voo);
                    var rota=Sql.registro(c,"SELECT origem_id FROM rota WHERE id=?",Dados.id(v,"rota_id"));
                    Sql.executar(c,"INSERT INTO ocorrencia_operacional(aeroporto_id,voo_id,usuario_id,tipo,gravidade,descricao,aberta_em,encerrada_em) VALUES (?,?,?,'OUTRA','MEDIA',?,?,?)",Dados.id(rota,"origem_id"),voo,s.usuarioId(),"Cancelamento: "+razao,relogio.instant(),relogio.instant());
                }
                default -> { }
            }
            auditoria.definirAutorDoVoo(c,s.usuarioId(),razao);Sql.executar(c,"UPDATE voo SET situacao=? WHERE id=?",proxima,voo);auditoria.registrar(c,s.usuarioId(),"TRANSICAO_VOO","voo",voo);return null;
        });
    }
}
