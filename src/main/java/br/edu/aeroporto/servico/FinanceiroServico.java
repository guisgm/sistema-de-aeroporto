package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.Set;

public final class FinanceiroServico {
    private final BancoDados banco;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;
    private final FinanceiroJdbc repositorio=new FinanceiroJdbc();
    private final ReservaJdbc reservas=new ReservaJdbc();

    public FinanceiroServico(BancoDados banco,AutorizacaoJdbc autorizacao,AuditoriaJdbc auditoria,Clock relogio) {
        this.banco=banco;this.autorizacao=autorizacao;this.auditoria=auditoria;this.relogio=relogio;
    }

    public long pagar(Sessao s,long reserva,String chave,String forma,BigDecimal valor,String resultado) {
        String token=Validacao.texto(chave,"Chave de idempotencia",80);BigDecimal quantia=Dados.valor(valor,true);
        Dados.exigir(Set.of("PIX","CARTAO","DINHEIRO").contains(forma) && Set.of("PENDENTE","APROVADO","RECUSADO").contains(resultado),"Pagamento simulado invalido.");
        return banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,s);var r=BloqueiosJdbc.reserva(c,reserva);
            var anterior=Sql.unico(c,"SELECT * FROM pagamento WHERE chave_idempotencia=? FOR UPDATE",Sql::linha,token);
            if(anterior.isPresent()) {
                var p=anterior.get();Dados.exigir(Dados.id(p,"reserva_id")==reserva && forma.equals(p.get("forma")) && quantia.compareTo(Dados.dinheiro(p,"valor"))==0,"Chave ja usada para outro pagamento.");
                if(!"PENDENTE".equals(p.get("situacao"))) { Dados.exigir(resultado.equals(p.get("situacao")),"Resultado difere do pagamento ja processado.");return Dados.id(p,"id"); }
                if(resultado.equals("PENDENTE")) return Dados.id(p,"id");
                processar(c,s,reserva,Dados.id(p,"id"),quantia,resultado,r);return Dados.id(p,"id");
            }
            validarReserva(c,reserva,r);
            long id=Sql.inserir(c,"INSERT INTO pagamento(reserva_id,chave_idempotencia,forma,valor,criado_em) VALUES (?,?,?,?,?) RETURNING id",reserva,token,forma,quantia,relogio.instant());
            if(!resultado.equals("PENDENTE")) processar(c,s,reserva,id,quantia,resultado,r);
            auditoria.registrar(c,s.usuarioId(),"REGISTRAR_PAGAMENTO","pagamento",id);return id;
        });
    }

    private void validarReserva(java.sql.Connection c,long reserva,Map<String,Object> r) throws java.sql.SQLException {
        Dados.exigir(!Set.of("CANCELADA","EXPIRADA","FINALIZADA").contains(r.get("situacao")),"Reserva encerrada.");
        boolean pendente=!Sql.listar(c,"SELECT id FROM item_reserva WHERE reserva_id=? AND situacao='PENDENTE'",l->l.getLong(1),reserva).isEmpty();
        if(pendente) Dados.exigir(Dados.instante(r,"expira_em").isAfter(relogio.instant()),"Reserva vencida; execute expiracao.");
    }

    private void processar(java.sql.Connection c,Sessao s,long reserva,long id,BigDecimal valor,String resultado,Map<String,Object> r) throws java.sql.SQLException {
        validarReserva(c,reserva,r);
        if(resultado.equals("APROVADO")) Dados.exigir(valor.compareTo(repositorio.devido(c,reserva).subtract(repositorio.recebido(c,reserva)))<=0,"Pagamento supera o saldo devido.");
        Sql.executar(c,"UPDATE pagamento SET situacao=?,processado_em=? WHERE id=?",resultado,relogio.instant(),id);
        if(resultado.equals("APROVADO") && repositorio.recebido(c,reserva).compareTo(repositorio.devido(c,reserva))>=0 && !Sql.listar(c,"SELECT id FROM item_reserva WHERE reserva_id=? AND situacao='PENDENTE'",l->l.getLong(1),reserva).isEmpty()) repositorio.confirmar(c,reserva,relogio.instant());
        auditoria.registrar(c,s.usuarioId(),"PROCESSAR_PAGAMENTO","pagamento",id);
    }

    public void cancelarPagamento(Sessao s,long id) {
        banco.transacao(c->{
            autorizacao.exigirAtendimento(c,s);var p=Sql.registro(c,"SELECT reserva_id FROM pagamento WHERE id=?",id);BloqueiosJdbc.reserva(c,Dados.id(p,"reserva_id"));
            Dados.exigir(Sql.executar(c,"UPDATE pagamento SET situacao='CANCELADO',processado_em=? WHERE id=? AND situacao='PENDENTE'",relogio.instant(),id)==1,"Apenas pagamento pendente pode ser cancelado.");
            auditoria.registrar(c,s.usuarioId(),"CANCELAR_PAGAMENTO","pagamento",id);return null;
        });
    }

    public void confirmar(Sessao s,long reserva) {
        banco.transacaoRepetivel(c->{autorizacao.exigirAtendimento(c,s);BloqueiosJdbc.reserva(c,reserva);repositorio.confirmar(c,reserva,relogio.instant());auditoria.registrar(c,s.usuarioId(),"CONFIRMAR_RESERVA","reserva",reserva);return null;});
    }

    public BigDecimal saldo(Sessao s,long reserva) {
        return banco.consultar(c->{autorizacao.exigirAtendimento(c,s);return repositorio.devido(c,reserva).subtract(repositorio.recebido(c,reserva));});
    }

    public long solicitarReembolso(Sessao s,long pagamento,BigDecimal valor,String chave,String motivo) {
        BigDecimal quantia=Dados.valor(valor,true);String token=Validacao.texto(chave,"Chave",80);String razao=Validacao.texto(motivo,"Motivo",500);
        return banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,s);var inicial=Sql.registro(c,"SELECT reserva_id FROM pagamento WHERE id=?",pagamento);long reserva=Dados.id(inicial,"reserva_id");BloqueiosJdbc.reserva(c,reserva);
            var p=Sql.registro(c,"SELECT * FROM pagamento WHERE id=? FOR UPDATE",pagamento);
            var anterior=Sql.unico(c,"SELECT * FROM reembolso WHERE chave_idempotencia=?",Sql::linha,token);
            if(anterior.isPresent()) { Dados.exigir(Dados.id(anterior.get(),"pagamento_id")==pagamento && quantia.compareTo(Dados.dinheiro(anterior.get(),"valor"))==0 && razao.equals(anterior.get().get("motivo")),"Chave de reembolso ja utilizada com outros dados.");return Dados.id(anterior.get(),"id"); }
            Dados.exigir("APROVADO".equals(p.get("situacao")),"Pagamento nao aprovado.");
            BigDecimal comprometido=Sql.unico(c,"SELECT COALESCE(SUM(valor),0) FROM reembolso WHERE pagamento_id=? AND situacao IN ('SOLICITADO','PROCESSADO')",r->r.getBigDecimal(1),pagamento).orElse(BigDecimal.ZERO);
            Dados.exigir(quantia.compareTo(Dados.dinheiro(p,"valor").subtract(comprometido))<=0,"Devolucao supera pagamento disponivel.");
            BigDecimal liberado=repositorio.recebido(c,reserva).subtract(repositorio.devido(c,reserva));
            Dados.exigir(quantia.compareTo(liberado)<=0,"Cancele os itens correspondentes antes de devolver valores devidos.");
            Dados.exigir(quantia.compareTo(reservas.limiteDevolucao(c,reserva))<=0,"Devolucao supera as condicoes de cancelamento vendidas.");
            long id=Sql.inserir(c,"INSERT INTO reembolso(pagamento_id,reserva_id,valor,motivo,chave_idempotencia,solicitado_em) VALUES (?,?,?,?,?,?) RETURNING id",pagamento,reserva,quantia,razao,token,relogio.instant());
            auditoria.registrar(c,s.usuarioId(),"SOLICITAR_REEMBOLSO","reembolso",id);return id;
        });
    }

    public void processarReembolso(Sessao s,long id,boolean aprovar) {
        banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,s);var inicial=Sql.registro(c,"SELECT * FROM reembolso WHERE id=?",id);BloqueiosJdbc.reserva(c,Dados.id(inicial,"reserva_id"));
            var p=Sql.registro(c,"SELECT * FROM pagamento WHERE id=? FOR UPDATE",Dados.id(inicial,"pagamento_id"));var r=Sql.registro(c,"SELECT * FROM reembolso WHERE id=? FOR UPDATE",id);
            String destino=aprovar?"PROCESSADO":"RECUSADO";
            if(destino.equals(r.get("situacao"))) return null;
            Dados.exigir("SOLICITADO".equals(r.get("situacao")),"Reembolso ja encerrado.");
            BigDecimal comprometido=Sql.unico(c,"SELECT COALESCE(SUM(valor),0) FROM reembolso WHERE pagamento_id=? AND situacao IN ('SOLICITADO','PROCESSADO')",l->l.getBigDecimal(1),Dados.id(p,"id")).orElse(BigDecimal.ZERO);
            Dados.exigir(comprometido.compareTo(Dados.dinheiro(p,"valor"))<=0,"Reembolsos superam pagamento.");
            Sql.executar(c,"UPDATE reembolso SET situacao=?,processado_em=? WHERE id=?",destino,relogio.instant(),id);auditoria.registrar(c,s.usuarioId(),"PROCESSAR_REEMBOLSO","reembolso",id);return null;
        });
    }
}
