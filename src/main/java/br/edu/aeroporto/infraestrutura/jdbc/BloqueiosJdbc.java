package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.Dados;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class BloqueiosJdbc {
    private BloqueiosJdbc() {}

    public static Map<String, Object> voo(Connection c, long id) throws SQLException {
        return Sql.registro(c, "SELECT * FROM voo WHERE id=? FOR UPDATE", id);
    }

    public static void voos(Connection c, Collection<Long> ids) throws SQLException {
        for (long id : ids.stream().distinct().sorted().toList()) voo(c, id);
    }

    public static Map<String, Object> reserva(Connection c, long id) throws SQLException {
        List<Long> voos = Sql.listar(c, "SELECT DISTINCT voo_id FROM item_reserva WHERE reserva_id=? ORDER BY voo_id", r -> r.getLong(1), id);
        voos(c, voos);
        var reserva = Sql.registro(c, "SELECT * FROM reserva WHERE id=? FOR UPDATE", id);
        Sql.listar(c, "SELECT id FROM item_reserva WHERE reserva_id=? ORDER BY id FOR UPDATE", r -> r.getLong(1), id);
        return reserva;
    }

    public static Map<String, Object> item(Connection c, long id) throws SQLException {
        var inicial = Sql.registro(c, "SELECT reserva_id FROM item_reserva WHERE id=?", id);
        reserva(c, Dados.id(inicial, "reserva_id"));
        return Sql.registro(c, "SELECT * FROM item_reserva WHERE id=? FOR UPDATE", id);
    }
}
