package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public final class PlanejamentoJdbc {
    public void serializar(Connection c) throws SQLException {
        Sql.unico(c,"SELECT pg_advisory_xact_lock(hashtext('aeroporto.operacao'))",r->true);
    }

    public void validarFrota(Connection c,long aeronave,long companhia,long rota,Instant partida,Instant chegada,Long ignorar) throws SQLException {
        var a=Sql.registro(c,"SELECT * FROM aeronave WHERE id=? AND situacao='ATIVA' FOR UPDATE",aeronave);
        Dados.exigir(Dados.id(a,"companhia_id")==companhia,"A aeronave deve pertencer a companhia do voo.");
        Sql.registro(c,"SELECT id FROM companhia_aerea WHERE id=? AND ativo",companhia);
        Sql.registro(c,"SELECT id FROM modelo_aeronave WHERE id=? AND ativo",Dados.id(a,"modelo_id"));
        var r=Sql.registro(c,"SELECT r.* FROM rota r JOIN aeroporto o ON o.id=r.origem_id JOIN aeroporto d ON d.id=r.destino_id WHERE r.id=? AND r.ativa AND o.ativo AND d.ativo",rota);
        var anterior=Sql.unico(c,"""
                SELECT r.destino_id,COALESCE(v.chegada_real,v.chegada_estimada,v.chegada_prevista) AS chegada
                FROM voo v JOIN rota r ON r.id=v.rota_id WHERE v.aeronave_id=? AND v.situacao<>'CANCELADO'
                  AND v.id<>COALESCE(?,0) AND COALESCE(v.partida_real,v.partida_estimada,v.partida_prevista)<?
                ORDER BY COALESCE(v.partida_real,v.partida_estimada,v.partida_prevista) DESC LIMIT 1
                """,Sql::linha,aeronave,ignorar,partida);
        if(anterior.isPresent()) {
            Dados.exigir(Dados.id(anterior.get(),"destino_id")==Dados.id(r,"origem_id"),"Aeronave nao chega a origem deste voo.");
            Dados.exigir(!Dados.instante(anterior.get(),"chegada").plus(Duration.ofMinutes(75)).isAfter(partida),"Margem entre voos insuficiente.");
        }
        var proximo=Sql.unico(c,"""
                SELECT r.origem_id,COALESCE(v.partida_real,v.partida_estimada,v.partida_prevista) AS partida
                FROM voo v JOIN rota r ON r.id=v.rota_id WHERE v.aeronave_id=? AND v.situacao<>'CANCELADO'
                  AND v.id<>COALESCE(?,0) AND COALESCE(v.partida_real,v.partida_estimada,v.partida_prevista)>=?
                ORDER BY COALESCE(v.partida_real,v.partida_estimada,v.partida_prevista) LIMIT 1
                """,Sql::linha,aeronave,ignorar,partida);
        if(proximo.isPresent()) {
            Dados.exigir(Dados.id(proximo.get(),"origem_id")==Dados.id(r,"destino_id"),"Proximo voo inicia em outro aeroporto.");
            Dados.exigir(!chegada.plus(Duration.ofMinutes(75)).isAfter(Dados.instante(proximo.get(),"partida")),"Margem antes do proximo voo insuficiente.");
        }
    }

    public long programar(Connection c,ProgramacaoVoo v) throws SQLException {
        validarFrota(c,v.aeronave(),v.companhia(),v.rota(),v.partida(),v.chegada(),null);
        long id=Sql.inserir(c,"""
                INSERT INTO voo(companhia_id,rota_id,aeronave_id,numero,partida_prevista,chegada_prevista,
                  checkin_abre,checkin_fecha,embarque_abre,embarque_fecha) VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id
                """,v.companhia(),v.rota(),v.aeronave(),v.numero(),v.partida(),v.chegada(),v.checkinAbre(),v.checkinFecha(),v.embarqueAbre(),v.embarqueFecha());
        Sql.executar(c,"INSERT INTO agenda_aeronave(aeronave_id,voo_id,inicio,fim) VALUES (?,?,?,?)",v.aeronave(),id,v.partida().minus(Duration.ofMinutes(45)),v.chegada().plus(Duration.ofMinutes(30)));
        inventario(c,id,v.aeronave());
        return id;
    }

    public void inventario(Connection c,long voo,long aeronave) throws SQLException {
        int quantidade=Sql.executar(c,"""
                INSERT INTO inventario_assento_voo(voo_id,aeronave_id,assento_id,codigo,classe)
                SELECT ?,aeronave_id,id,codigo,classe FROM assento_aeronave WHERE aeronave_id=? AND ativo ORDER BY fila,coluna
                """,voo,aeronave);
        Dados.exigir(quantidade>0,"Configure pelo menos um assento fisico antes de programar.");
    }

    public long tarifa(Connection c,long voo,Long id,Tarifa t,boolean ativa) throws SQLException {
        if(id!=null && !Sql.listar(c,"SELECT id FROM item_reserva WHERE tarifa_id=?",r->r.getLong(1),id).isEmpty()) {
            Dados.exigir(t.classe().equals(Sql.registro(c,"SELECT classe FROM tarifa_voo WHERE id=? AND voo_id=?",id,voo).get("classe")),"Classe vendida nao pode ser alterada.");
        }
        if(id==null) return Sql.inserir(c,"""
                INSERT INTO tarifa_voo(voo_id,codigo,classe,valor_base,taxa_embarque,franquia_bagagem_kg,limite_pecas,permite_cancelar,multa_cancelamento,ativa)
                VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id
                """,voo,t.codigo(),t.classe(),t.base(),t.taxa(),t.franquia(),t.pecas(),t.permiteCancelar(),t.multa(),ativa);
        Dados.exigir(Sql.executar(c,"""
                UPDATE tarifa_voo SET codigo=?,classe=?,valor_base=?,taxa_embarque=?,franquia_bagagem_kg=?,limite_pecas=?,permite_cancelar=?,multa_cancelamento=?,ativa=? WHERE id=? AND voo_id=?
                """,t.codigo(),t.classe(),t.base(),t.taxa(),t.franquia(),t.pecas(),t.permiteCancelar(),t.multa(),ativa,id,voo)==1,"Tarifa nao encontrada.");
        return id;
    }

    public void agendas(Connection c,Map<String,Object> anterior,Instant partida,Instant chegada) throws SQLException {
        long voo=Dados.id(anterior,"id");
        Instant velhaPartida=efetivo(anterior,"partida"),velhaChegada=efetivo(anterior,"chegada");
        Duration deltaPartida=Duration.between(velhaPartida,partida),deltaChegada=Duration.between(velhaChegada,chegada);
        Sql.executar(c,"UPDATE agenda_aeronave SET inicio=?,fim=? WHERE voo_id=? AND ativa",partida.minus(Duration.ofMinutes(45)),chegada.plus(Duration.ofMinutes(30)),voo);
        for(var alocacao:Sql.listar(c,"SELECT * FROM alocacao_recurso WHERE voo_id=? AND ativa ORDER BY id FOR UPDATE",Sql::linha,voo)) {
            Duration delta="CHEGADA".equals(alocacao.get("finalidade"))?deltaChegada:deltaPartida;
            Sql.executar(c,"UPDATE alocacao_recurso SET inicio=?,fim=? WHERE id=?",Dados.instante(alocacao,"inicio").plus(delta),Dados.instante(alocacao,"fim").plus(delta),Dados.id(alocacao,"id"));
        }
        for(var escala:Sql.listar(c,"SELECT * FROM escala_funcionario WHERE voo_id=? AND ativa ORDER BY id FOR UPDATE",Sql::linha,voo)) {
            Sql.executar(c,"UPDATE escala_funcionario SET inicio=?,fim=? WHERE id=?",Dados.instante(escala,"inicio").plus(deltaPartida),Dados.instante(escala,"fim").plus(deltaChegada),Dados.id(escala,"id"));
        }
        for(var solo:Sql.listar(c,"SELECT * FROM servico_solo WHERE voo_id=? AND situacao='PENDENTE' AND inicio IS NOT NULL ORDER BY id",Sql::linha,voo)) {
            Sql.executar(c,"UPDATE servico_solo SET inicio=?,fim=? WHERE id=?",Dados.instante(solo,"inicio").plus(deltaPartida),solo.get("fim")==null?null:Dados.instante(solo,"fim").plus(deltaPartida),Dados.id(solo,"id"));
        }
    }

    public static Instant efetivo(Map<String,Object> voo,String prefixo) {
        for(String sufixo:new String[]{"_real","_estimada","_prevista"}) if(voo.get(prefixo+sufixo)!=null) return Dados.instante(voo,prefixo+sufixo);
        throw new IllegalArgumentException("Horario inexistente.");
    }
}
