package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ReservaServico {
    private final BancoDados banco;
    private final ReservaJdbc repositorio;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;

    public ReservaServico(BancoDados banco,ReservaJdbc repositorio,AutorizacaoJdbc autorizacao,AuditoriaJdbc auditoria,Clock relogio) {
        this.banco=banco;this.repositorio=repositorio;this.autorizacao=autorizacao;this.auditoria=auditoria;this.relogio=relogio;
    }

    public int expirarPendencias() { return banco.transacaoRepetivel(c->repositorio.expirar(c,relogio.instant())); }

    public int expirar(Sessao sessao) { return banco.transacaoRepetivel(c->{autorizacao.exigirAtendimento(c,sessao);return repositorio.expirar(c,relogio.instant());}); }

    public long reservar(Sessao sessao,long comprador,List<PedidoTrecho> trechos,int prazoMinutos) {
        Dados.exigir(trechos!=null && !trechos.isEmpty() && trechos.size()<=100 && prazoMinutos>=1 && prazoMinutos<=120,"Reserva deve ter de 1 a 100 itens e prazo de 1 a 120 minutos.");
        List<PedidoTrecho> copia=List.copyOf(trechos);expirarPendencias();
        return banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,sessao);
            long id=repositorio.reservar(c,sessao.usuarioId(),comprador,copia,relogio.instant(),prazoMinutos);
            auditoria.registrar(c,sessao.usuarioId(),"RESERVAR","reserva",id);return id;
        });
    }

    public void cancelar(Sessao sessao,long reserva,Long item,String motivo) {
        String razao=Validacao.texto(motivo,"Motivo",500);
        banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,sessao);BloqueiosJdbc.reserva(c,reserva);
            var itens=Sql.listar(c,"SELECT * FROM item_reserva WHERE reserva_id=? AND (?::bigint IS NULL OR id=?) ORDER BY id",Sql::linha,reserva,item,item);
            Dados.exigir(!itens.isEmpty(),"Item nao encontrado nesta reserva.");
            BigDecimal devolucao=BigDecimal.ZERO;
            for(var i:itens) devolucao=devolucao.add(repositorio.cancelarItem(c,i,relogio.instant(),razao,false,sessao.usuarioId()));
            repositorio.solicitarDevolucao(c,reserva,devolucao,"cancel:"+reserva+":"+(item==null?"todos":item),razao);
            repositorio.recalcular(c,reserva);auditoria.registrar(c,sessao.usuarioId(),"CANCELAR_RESERVA","reserva",reserva);return null;
        });
    }

    public long remarcar(Sessao sessao,long item,PedidoTrecho novo,int prazo) {
        Dados.exigir(prazo>=1 && prazo<=120,"Prazo invalido.");expirarPendencias();
        return banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,sessao);
            var inicial=Sql.registro(c,"SELECT reserva_id FROM item_reserva WHERE id=?",item);
            long reserva=Dados.id(inicial,"reserva_id");
            var voos=new ArrayList<>(Sql.listar(c,"SELECT voo_id FROM item_reserva WHERE reserva_id=?",r->r.getLong(1),reserva));voos.add(novo.voo());BloqueiosJdbc.voos(c,voos);
            var r=BloqueiosJdbc.reserva(c,reserva);var antigo=Sql.registro(c,"SELECT * FROM item_reserva WHERE id=? FOR UPDATE",item);
            Dados.exigir(Dados.id(antigo,"passageiro_id")==novo.passageiro(),"Remarcacao preserva passageiro.");
            Dados.exigir(Set.of("PENDENTE","CONFIRMADO").contains(antigo.get("situacao")),"Item nao admite remarcacao.");
            BigDecimal devolucao=repositorio.cancelarItem(c,antigo,relogio.instant(),"Remarcacao",false,sessao.usuarioId());
            repositorio.solicitarDevolucao(c,reserva,devolucao,"remarcar:"+item,"Remarcacao");repositorio.recalcular(c,reserva);
            long nova=repositorio.reservar(c,sessao.usuarioId(),Dados.id(r,"comprador_id"),List.of(novo),relogio.instant(),prazo);
            Sql.executar(c,"INSERT INTO evento_auditoria(usuario_id,acao,entidade,registro_id,detalhes) VALUES (?,'REMARCAR','item_reserva',?,jsonb_build_object('reserva_anterior',?::bigint,'nova_reserva',?::bigint))",sessao.usuarioId(),item,reserva,nova);
            return nova;
        });
    }
}
