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
        java.util.List<Long> ordenados = new java.util.ArrayList<>();
        for (Long id : ids) {
            if (!ordenados.contains(id)) ordenados.add(id);
        }
        java.util.Collections.sort(ordenados);
        for (long id : ordenados) voo(c, id);
    }

    public static Map<String, Object> reserva(Connection c, long id) throws SQLException {
        List<Long> voos =
                Sql.listarNumeros(
                        c,
                        "SELECT DISTINCT voo_id FROM item_reserva WHERE reserva_id=? ORDER BY voo_id",
                        id);
        voos(c, voos);
        Map<String, Object> reserva =
                Sql.registro(c, "SELECT * FROM reserva WHERE id=? FOR UPDATE", id);
        Sql.listarNumeros(
                c, "SELECT id FROM item_reserva WHERE reserva_id=? ORDER BY id FOR UPDATE", id);
        return reserva;
    }

    public static Map<String, Object> item(Connection c, long id) throws SQLException {
        Map<String, Object> inicial =
                Sql.registro(c, "SELECT reserva_id FROM item_reserva WHERE id=?", id);
        reserva(c, Dados.id(inicial, "reserva_id"));
        return Sql.registro(c, "SELECT * FROM item_reserva WHERE id=? FOR UPDATE", id);
    }
}
