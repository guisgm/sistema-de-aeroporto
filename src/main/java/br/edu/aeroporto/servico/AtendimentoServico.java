package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public final class AtendimentoServico {
    private static final Map<String,Set<String>> BAGAGEM=Map.of(
            "RECEBIDA",Set.of("INSPECIONADA","RETIRADA","EXTRAVIADA"),
            "INSPECIONADA",Set.of("CARREGADA","RETIRADA","EXTRAVIADA"),
            "CARREGADA",Set.of("DESCARREGADA","RETIRADA","EXTRAVIADA"),
            "DESCARREGADA",Set.of("ENTREGUE","EXTRAVIADA"),
            "EXTRAVIADA",Set.of("RECEBIDA","ENTREGUE","RETIRADA"));
    private final BancoDados banco;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;
    private final ReservaJdbc reservas=new ReservaJdbc();
    private final FinanceiroJdbc financeiro=new FinanceiroJdbc();

    public AtendimentoServico(BancoDados banco,AutorizacaoJdbc autorizacao,AuditoriaJdbc auditoria,Clock relogio) {
        this.banco=banco;this.autorizacao=autorizacao;this.auditoria=auditoria;this.relogio=relogio;
    }

    public long checkin(Sessao s,long item,long documento) {
        return banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,s);var i=BloqueiosJdbc.item(c,item);var v=Sql.registro(c,"SELECT v.*,r.origem_id,r.destino_id FROM voo v JOIN rota r ON r.id=v.rota_id WHERE v.id=?",Dados.id(i,"voo_id"));
            Dados.exigir("CONFIRMADO".equals(i.get("situacao")),"Item nao confirmado.");
            Dados.exigir("CHECKIN_ABERTO".equals(v.get("situacao")),"Check-in nao aberto para o voo.");
            janela(v,"checkin_abre","checkin_fecha");
            Sql.registro(c,"SELECT id FROM pessoa WHERE id=? AND ativo",Dados.id(i,"passageiro_id"));
            var d=Sql.registro(c,"SELECT * FROM documento_pessoa WHERE id=? AND pessoa_id=?",documento,Dados.id(i,"passageiro_id"));
            LocalDate dia=PlanejamentoJdbc.efetivo(v,"partida").atZone(java.time.ZoneOffset.UTC).toLocalDate();
            Dados.exigir(d.get("validade")==null || !((java.sql.Date)d.get("validade")).toLocalDate().isBefore(dia),"Documento vencido.");
            long paisOrigem=Dados.id(Sql.registro(c,"SELECT ci.pais_id FROM aeroporto a JOIN cidade ci ON ci.id=a.cidade_id WHERE a.id=?",Dados.id(v,"origem_id")),"pais_id");
            long paisDestino=Dados.id(Sql.registro(c,"SELECT ci.pais_id FROM aeroporto a JOIN cidade ci ON ci.id=a.cidade_id WHERE a.id=?",Dados.id(v,"destino_id")),"pais_id");
            Dados.exigir(paisOrigem==paisDestino?Set.of("CPF","RG","PASSAPORTE").contains(d.get("tipo")):"PASSAPORTE".equals(d.get("tipo")) && d.get("validade")!=null,"Documento inadequado ao trecho.");
            long bilhete=Dados.id(Sql.registro(c,"SELECT id FROM bilhete WHERE item_id=? AND situacao='EMITIDO'",item),"id");
            var o=Sql.registro(c,"SELECT * FROM ocupacao_assento WHERE item_id=? AND liberada_em IS NULL",item);
            Sql.registro(c,"SELECT id FROM inventario_assento_voo WHERE id=? AND NOT bloqueado FOR UPDATE",Dados.id(o,"inventario_id"));
            Dados.exigir(financeiro.recebido(c,Dados.id(i,"reserva_id")).compareTo(financeiro.devido(c,Dados.id(i,"reserva_id")))>=0,"Reserva sem cobertura financeira.");
            var anterior=Sql.unico(c,"SELECT * FROM check_in WHERE item_id=?",Sql::linha,item);
            if(anterior.isPresent() && anterior.get().get("cancelado_em")==null) return Dados.id(anterior.get(),"id");
            long id=Sql.inserir(c,"""
                    INSERT INTO check_in(item_id,voo_id,bilhete_id,ocupacao_id,usuario_id,codigo_cartao,realizado_em)
                    VALUES (?,?,?,?,?,?,?) ON CONFLICT (item_id) DO UPDATE SET ocupacao_id=EXCLUDED.ocupacao_id,usuario_id=EXCLUDED.usuario_id,
                    codigo_cartao=EXCLUDED.codigo_cartao,realizado_em=EXCLUDED.realizado_em,cancelado_em=NULL RETURNING id
                    """,item,Dados.id(i,"voo_id"),bilhete,Dados.id(o,"id"),s.usuarioId(),Dados.codigo("C",32),relogio.instant());
            auditoria.registrar(c,s.usuarioId(),"REALIZAR_CHECKIN","check_in",id);return id;
        });
    }

    public void cancelarCheckin(Sessao s,long item) {
        banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,s);var i=BloqueiosJdbc.item(c,item);var v=Sql.registro(c,"SELECT * FROM voo WHERE id=?",Dados.id(i,"voo_id"));
            Dados.exigir(Set.of("CHECKIN_ABERTO","EMBARQUE").contains(v.get("situacao")) && relogio.instant().isBefore(ReservaJdbc.janela(v,"embarque_fecha")),"Prazo de cancelamento do check-in encerrado.");
            Dados.exigir("CONFIRMADO".equals(i.get("situacao")),"Item nao admite cancelamento de check-in.");
            var ci=Sql.registro(c,"SELECT * FROM check_in WHERE item_id=? AND cancelado_em IS NULL FOR UPDATE",item);
            Dados.exigir(Sql.listar(c,"SELECT id FROM embarque WHERE checkin_id=?",r->r.getLong(1),Dados.id(ci,"id")).isEmpty(),"Passageiro ja embarcou.");
            BigDecimal taxas=Sql.unico(c,"SELECT COALESCE(SUM(taxa_excesso),0) FROM bagagem WHERE checkin_id=? AND situacao<>'RETIRADA'",r->r.getBigDecimal(1),Dados.id(ci,"id")).orElse(BigDecimal.ZERO);
            var rota=Sql.registro(c,"SELECT origem_id FROM rota WHERE id=?",Dados.id(v,"rota_id"));
            for(long id:Sql.listar(c,"SELECT id FROM bagagem WHERE checkin_id=? AND situacao NOT IN ('RETIRADA','ENTREGUE') ORDER BY id FOR UPDATE",r->r.getLong(1),Dados.id(ci,"id"))) evento(c,s,id,Dados.id(rota,"origem_id"),"RETIRADA","Cancelamento de check-in");
            Sql.executar(c,"UPDATE check_in SET cancelado_em=? WHERE id=?",relogio.instant(),Dados.id(ci,"id"));
            reservas.solicitarDevolucao(c,Dados.id(i,"reserva_id"),taxas,"ci:"+Dados.id(ci,"id")+":"+Dados.codigo("",12),"Cancelamento de despacho");
            auditoria.registrar(c,s.usuarioId(),"CANCELAR_CHECKIN","check_in",Dados.id(ci,"id"));return null;
        });
    }

    public Map<String,Object> cartao(Sessao s,long item) {
        return banco.consultar(c->{autorizacao.exigirAtendimento(c,s);return Sql.registro(c,"""
                SELECT ci.id,ci.codigo_cartao,p.nome,b.numero AS bilhete,v.numero AS voo,a.codigo,ro.codigo_icao AS origem,rd.codigo_icao AS destino,
                    v.partida_prevista,v.partida_estimada,v.embarque_abre,v.embarque_fecha
                FROM check_in ci JOIN item_reserva i ON i.id=ci.item_id JOIN pessoa p ON p.id=i.passageiro_id
                JOIN bilhete b ON b.id=ci.bilhete_id JOIN voo v ON v.id=ci.voo_id JOIN rota r ON r.id=v.rota_id
                JOIN aeroporto ro ON ro.id=r.origem_id JOIN aeroporto rd ON rd.id=r.destino_id
                JOIN ocupacao_assento o ON o.id=ci.ocupacao_id JOIN inventario_assento_voo a ON a.id=o.inventario_id
                WHERE ci.item_id=? AND ci.cancelado_em IS NULL AND b.situacao='EMITIDO' AND o.liberada_em IS NULL
                """,item);});
    }

    public long despachar(Sessao s,long item,String etiqueta,BigDecimal peso,String categoria) {
        BigDecimal kg=Dados.valor(peso,true);Dados.exigir(Set.of("DESPACHADA","ESPECIAL").contains(categoria) && kg.compareTo(new BigDecimal(categoria.equals("ESPECIAL")?"60":"32"))<=0,"Categoria ou peso invalido.");
        String tag=etiqueta==null || etiqueta.isBlank()?Dados.codigo("M",24):Validacao.texto(etiqueta,"Etiqueta",30);
        return banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,s);var i=BloqueiosJdbc.item(c,item);var v=Sql.registro(c,"SELECT * FROM voo WHERE id=?",Dados.id(i,"voo_id"));
            Dados.exigir("CONFIRMADO".equals(i.get("situacao")) && "CHECKIN_ABERTO".equals(v.get("situacao")),"Despacho requer item confirmado e check-in aberto.");janela(v,"checkin_abre","checkin_fecha");
            var ci=Sql.registro(c,"SELECT * FROM check_in WHERE item_id=? AND cancelado_em IS NULL",item);
            var antiga=Sql.unico(c,"SELECT * FROM bagagem WHERE etiqueta=?",Sql::linha,tag);
            if(antiga.isPresent()) { Dados.exigir(Dados.id(antiga.get(),"checkin_id")==Dados.id(ci,"id") && kg.compareTo(Dados.dinheiro(antiga.get(),"peso_kg"))==0 && categoria.equals(antiga.get().get("categoria")),"Etiqueta ja usada para outra bagagem.");return Dados.id(antiga.get(),"id"); }
            var resumo=Sql.registro(c,"SELECT COUNT(*) AS pecas,COALESCE(SUM(peso_kg),0) AS kg FROM bagagem WHERE checkin_id=? AND situacao<>'RETIRADA'",Dados.id(ci,"id"));
            int quantidade=((Number)resumo.get("pecas")).intValue();Dados.exigir(quantidade<10,"Limite operacional de 10 pecas.");
            BigDecimal antes=excesso(Dados.dinheiro(resumo,"kg"),quantidade,i),depois=excesso(Dados.dinheiro(resumo,"kg").add(kg),quantidade+1,i);
            long id=Sql.inserir(c,"INSERT INTO bagagem(checkin_id,etiqueta,peso_kg,categoria,taxa_excesso) VALUES (?,?,?,?,?) RETURNING id",Dados.id(ci,"id"),tag,kg,categoria,depois.subtract(antes));
            long origem=Dados.id(Sql.registro(c,"SELECT origem_id FROM rota WHERE id=?",Dados.id(v,"rota_id")),"origem_id");
            evento(c,s,id,origem,"RECEBIDA","Despacho");auditoria.registrar(c,s.usuarioId(),"DESPACHAR_BAGAGEM","bagagem",id);return id;
        });
    }

    private BigDecimal excesso(BigDecimal kg,int pecas,Map<String,Object> item) {
        return kg.subtract(Dados.dinheiro(item,"franquia_bagagem_kg")).max(BigDecimal.ZERO).multiply(new BigDecimal("30"))
                .add(new BigDecimal(Math.max(0,pecas-((Number)item.get("limite_pecas")).intValue())).multiply(new BigDecimal("100")));
    }

    public void rastrear(Sessao s,String etiqueta,long aeroporto,String estado,String observacao) {
        String nota=Validacao.texto(observacao,"Observacao",500);
        banco.transacaoRepetivel(c->{
            autorizacao.exigirOperacao(c,s);var inicial=Sql.registro(c,"SELECT b.id,ci.item_id FROM bagagem b JOIN check_in ci ON ci.id=b.checkin_id WHERE b.etiqueta=?",etiqueta);
            var i=BloqueiosJdbc.item(c,Dados.id(inicial,"item_id"));var b=Sql.registro(c,"SELECT * FROM bagagem WHERE id=? FOR UPDATE",Dados.id(inicial,"id"));
            Dados.exigir(BAGAGEM.getOrDefault(Dados.texto(b,"situacao"),Set.of()).contains(estado),"Transicao de bagagem invalida.");
            var r=Sql.registro(c,"SELECT r.* FROM voo v JOIN rota r ON r.id=v.rota_id WHERE v.id=?",Dados.id(i,"voo_id"));
            long esperado=Set.of("DESCARREGADA","ENTREGUE").contains(estado)?Dados.id(r,"destino_id"):Dados.id(r,"origem_id");
            if(estado.equals("EXTRAVIADA")) Dados.exigir(aeroporto==Dados.id(r,"origem_id") || aeroporto==Dados.id(r,"destino_id"),"Aeroporto fora do trecho.");
            else Dados.exigir(aeroporto==esperado,"Evento em aeroporto incorreto.");
            if(Set.of("DESCARREGADA","ENTREGUE").contains(estado)) {
                var v=Sql.registro(c,"SELECT * FROM voo WHERE id=?",Dados.id(i,"voo_id"));
                Dados.exigir(v.get("chegada_real")!=null && !Dados.instante(v,"chegada_real").isAfter(relogio.instant()),"Aeronave ainda nao chegou.");
            }
            if(estado.equals("CARREGADA")) Dados.exigir(financeiro.recebido(c,Dados.id(i,"reserva_id")).compareTo(financeiro.devido(c,Dados.id(i,"reserva_id")))>=0,"Excesso de bagagem nao pago.");
            evento(c,s,Dados.id(b,"id"),aeroporto,estado,nota);
            if(estado.equals("RETIRADA")) reservas.solicitarDevolucao(c,Dados.id(i,"reserva_id"),Dados.dinheiro(b,"taxa_excesso"),"retirada:"+Dados.id(b,"id"),"Retirada de bagagem");
            auditoria.registrar(c,s.usuarioId(),"RASTREAR_BAGAGEM","bagagem",Dados.id(b,"id"));return null;
        });
    }

    public long embarcar(Sessao s,String cartao,long alocacao) {
        return banco.transacaoRepetivel(c->{
            autorizacao.exigirAtendimento(c,s);var inicial=Sql.registro(c,"SELECT item_id FROM check_in WHERE codigo_cartao=?",cartao);var i=BloqueiosJdbc.item(c,Dados.id(inicial,"item_id"));
            var ci=Sql.registro(c,"SELECT * FROM check_in WHERE codigo_cartao=? AND cancelado_em IS NULL",cartao);var v=Sql.registro(c,"SELECT * FROM voo WHERE id=?",Dados.id(i,"voo_id"));
            Dados.exigir("EMBARQUE".equals(v.get("situacao")),"Voo nao esta em embarque.");janela(v,"embarque_abre","embarque_fecha");
            Dados.exigir("CONFIRMADO".equals(i.get("situacao")),"Passagem ja utilizada ou invalida.");
            Sql.registro(c,"SELECT id FROM bilhete WHERE id=? AND situacao='EMITIDO'",Dados.id(ci,"bilhete_id"));
            Sql.registro(c,"SELECT id FROM ocupacao_assento WHERE id=? AND liberada_em IS NULL",Dados.id(ci,"ocupacao_id"));
            var a=Sql.registro(c,"""
                    SELECT a.* FROM alocacao_recurso a JOIN recurso_aeroportuario r ON r.id=a.recurso_id JOIN voo v ON v.id=a.voo_id JOIN rota rt ON rt.id=v.rota_id
                    WHERE a.id=? AND a.voo_id=? AND a.ativa AND a.finalidade='PARTIDA' AND r.tipo='PORTAO' AND r.situacao='DISPONIVEL' AND r.aeroporto_id=rt.origem_id
                    """,alocacao,Dados.id(i,"voo_id"));
            Dados.exigir(!relogio.instant().isBefore(Dados.instante(a,"inicio")) && relogio.instant().isBefore(Dados.instante(a,"fim")),"Alocacao fora de horario.");
            Dados.exigir(Sql.listar(c,"SELECT id FROM bagagem WHERE checkin_id=? AND situacao NOT IN ('CARREGADA','RETIRADA')",r->r.getLong(1),Dados.id(ci,"id")).isEmpty(),"Bagagem ainda nao carregada ou extraviada.");
            Dados.exigir(financeiro.recebido(c,Dados.id(i,"reserva_id")).compareTo(financeiro.devido(c,Dados.id(i,"reserva_id")))>=0,"Reserva com saldo pendente.");
            long id=Sql.inserir(c,"INSERT INTO embarque(checkin_id,voo_id,alocacao_id,usuario_id,realizado_em) VALUES (?,?,?,?,?) RETURNING id",Dados.id(ci,"id"),Dados.id(i,"voo_id"),alocacao,s.usuarioId(),relogio.instant());
            Sql.executar(c,"UPDATE item_reserva SET situacao='UTILIZADO' WHERE id=?",Dados.id(i,"id"));Sql.executar(c,"UPDATE bilhete SET situacao='UTILIZADO' WHERE id=?",Dados.id(ci,"bilhete_id"));
            reservas.recalcular(c,Dados.id(i,"reserva_id"));auditoria.registrar(c,s.usuarioId(),"EMBARCAR","embarque",id);return id;
        });
    }

    private void evento(Connection c,Sessao s,long bagagem,long aeroporto,String estado,String nota) throws SQLException {
        Sql.executar(c,"UPDATE bagagem SET situacao=? WHERE id=?",estado,bagagem);
        Sql.executar(c,"INSERT INTO evento_bagagem(bagagem_id,aeroporto_id,usuario_id,situacao,ocorrido_em,observacao) VALUES (?,?,?,?,?,?)",bagagem,aeroporto,s.usuarioId(),estado,relogio.instant(),nota);
    }

    private void janela(Map<String,Object> voo,String inicio,String fim) {
        Instant agora=relogio.instant();Dados.exigir(!agora.isBefore(ReservaJdbc.janela(voo,inicio)) && agora.isBefore(ReservaJdbc.janela(voo,fim)),"Fora da janela de atendimento.");
    }
}
