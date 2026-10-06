package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

public final class OperacaoJdbc {
    public static void validarEscalasDoVoo(Connection c,long voo) throws SQLException {
        for(var escala:Sql.listar(c,"SELECT * FROM escala_funcionario WHERE voo_id=? AND ativa ORDER BY funcionario_id",Sql::linha,voo)) {
            validarEscala(c,escala,Dados.id(escala,"id"));
        }
    }

    public static void validarEscala(Connection c,Map<String,Object> escala,Long ignorar) throws SQLException {
        long funcionario=Dados.id(escala,"funcionario_id"),aeroporto=Dados.id(escala,"aeroporto_id");
        Instant inicio=Dados.instante(escala,"inicio"),fim=Dados.instante(escala,"fim");
        String funcao=Dados.texto(escala,"funcao");
        Dados.exigir(fim.isAfter(inicio) && Duration.between(inicio,fim).compareTo(Duration.ofHours(14))<=0,"Jornada invalida ou superior a 14h.");
        var f=Sql.registro(c,"SELECT f.* FROM funcionario f JOIN pessoa p ON p.id=f.pessoa_id WHERE f.pessoa_id=? AND p.ativo",funcionario);
        LocalDate dia=inicio.atZone(ZoneId.of(Sql.registro(c,"SELECT fuso_horario FROM aeroporto WHERE id=? AND ativo",aeroporto).get("fuso_horario").toString())).toLocalDate();
        LocalDate ultimo=fim.atZone(ZoneId.of("UTC")).toLocalDate();
        LocalDate admissao=((java.sql.Date)f.get("admissao")).toLocalDate();
        Dados.exigir(!admissao.isAfter(dia) && (f.get("desligamento")==null || ((java.sql.Date)f.get("desligamento")).toLocalDate().isAfter(ultimo)),"Funcionario nao esta contratado durante a escala.");
        long destino=aeroporto;
        if(escala.get("voo_id")!=null) {
            var voo=Sql.registro(c,"SELECT v.*,r.origem_id,r.destino_id,a.modelo_id FROM voo v JOIN rota r ON r.id=v.rota_id JOIN aeronave a ON a.id=v.aeronave_id WHERE v.id=?",Dados.id(escala,"voo_id"));
            Dados.exigir(!Set.of("CANCELADO","CONCLUIDO").contains(voo.get("situacao")),"Voo encerrado.");
            Dados.exigir(aeroporto==Dados.id(voo,"origem_id"),"Escala deve iniciar na origem.");
            if(Set.of("COMANDANTE","COPILOTO","COMISSARIO").contains(funcao)) {
                Dados.exigir(!inicio.isAfter(PlanejamentoJdbc.efetivo(voo,"partida").minus(Duration.ofMinutes(45))) && !fim.isBefore(PlanejamentoJdbc.efetivo(voo,"chegada").plus(Duration.ofMinutes(30))),"Escala nao cobre preparacao e chegada.");
                Dados.exigir(!Sql.listar(c,"SELECT id FROM habilitacao_tripulante WHERE funcionario_id=? AND modelo_id=? AND funcao=? AND validade>=?",r->r.getLong(1),funcionario,Dados.id(voo,"modelo_id"),funcao,ultimo).isEmpty(),"Habilitacao ausente ou vencida.");
                destino=Dados.id(voo,"destino_id");
            }
        } else Dados.exigir(!Set.of("COMANDANTE","COPILOTO","COMISSARIO").contains(funcao),"Tripulante requer voo.");
        var anterior=Sql.unico(c,"""
                SELECT e.*,CASE WHEN e.funcao IN ('COMANDANTE','COPILOTO','COMISSARIO') THEN r.destino_id ELSE e.aeroporto_id END AS destino
                FROM escala_funcionario e LEFT JOIN voo v ON v.id=e.voo_id LEFT JOIN rota r ON r.id=v.rota_id
                WHERE e.funcionario_id=? AND (e.ativa OR v.situacao='CONCLUIDO') AND e.id<>COALESCE(?,0) AND e.inicio<? ORDER BY e.inicio DESC LIMIT 1
                """,Sql::linha,funcionario,ignorar,inicio);
        if(anterior.isPresent()) {
            Dados.exigir(!Dados.instante(anterior.get(),"fim").plus(Duration.ofHours(12)).isAfter(inicio),"Descanso minimo de 12h nao atendido.");
            Dados.exigir(Dados.id(anterior.get(),"destino")==aeroporto,"Funcionario esta em outro aeroporto.");
        } else Dados.exigir(Dados.id(f,"aeroporto_base_id")==aeroporto,"Primeira escala deve iniciar na base do funcionario.");
        var proxima=Sql.unico(c,"SELECT * FROM escala_funcionario WHERE funcionario_id=? AND ativa AND id<>COALESCE(?,0) AND inicio>=? ORDER BY inicio LIMIT 1",Sql::linha,funcionario,ignorar,inicio);
        if(proxima.isPresent()) {
            Dados.exigir(!fim.plus(Duration.ofHours(12)).isAfter(Dados.instante(proxima.get(),"inicio")),"Descanso ate proxima jornada insuficiente.");
            Dados.exigir(destino==Dados.id(proxima.get(),"aeroporto_id"),"Proxima escala em outro aeroporto.");
        }
    }

    public static void validarTripulacao(Connection c,long voo) throws SQLException {
        validarEscalasDoVoo(c,voo);
        long assentos=Sql.unico(c,"SELECT COUNT(*) FROM inventario_assento_voo WHERE voo_id=? AND NOT bloqueado",r->r.getLong(1),voo).orElse(0L);
        for(String funcao:new String[]{"COMANDANTE","COPILOTO","COMISSARIO"}) {
            long quantidade=Sql.unico(c,"SELECT COUNT(*) FROM escala_funcionario WHERE voo_id=? AND ativa AND funcao=?",r->r.getLong(1),voo,funcao).orElse(0L);
            long minimo=funcao.equals("COMISSARIO")?Math.max(1,(assentos+49)/50):1;
            Dados.exigir(quantidade>=minimo,"Tripulacao insuficiente: "+funcao+" minimo "+minimo);
        }
    }

    public static void validarRecurso(Connection c,long voo,long recurso,String finalidade,Instant inicio,Instant fim) throws SQLException {
        Dados.exigir(Set.of("PARTIDA","CHEGADA","APOIO").contains(finalidade) && fim.isAfter(inicio),"Alocacao invalida.");
        var v=Sql.registro(c,"""
                SELECT v.*,r.origem_id,r.destino_id,m.envergadura_m,m.comprimento_pista_min_m
                FROM voo v JOIN rota r ON r.id=v.rota_id JOIN aeronave a ON a.id=v.aeronave_id JOIN modelo_aeronave m ON m.id=a.modelo_id WHERE v.id=?
                """,voo);
        var r=Sql.registro(c,"SELECT * FROM recurso_aeroportuario WHERE id=? AND situacao='DISPONIVEL'",recurso);
        Dados.exigir(!Set.of("CANCELADO","CONCLUIDO").contains(v.get("situacao")),"Voo encerrado.");
        long aeroporto=finalidade.equals("CHEGADA")?Dados.id(v,"destino_id"):Dados.id(v,"origem_id");
        Dados.exigir(Dados.id(r,"aeroporto_id")==aeroporto,"Recurso em outro aeroporto.");
        Sql.registro(c,"SELECT id FROM aeroporto WHERE id=? AND ativo",aeroporto);
        if(r.get("terminal_id")!=null) Sql.registro(c,"SELECT id FROM terminal WHERE id=? AND ativo",Dados.id(r,"terminal_id"));
        if(r.get("envergadura_max_m")!=null) Dados.exigir(Dados.dinheiro(r,"envergadura_max_m").compareTo(Dados.dinheiro(v,"envergadura_m"))>=0,"Envergadura incompativel.");
        if("PISTA".equals(r.get("tipo"))) Dados.exigir(Dados.dinheiro(r,"comprimento_m").compareTo(Dados.dinheiro(v,"comprimento_pista_min_m"))>=0,"Pista muito curta.");
    }
}
