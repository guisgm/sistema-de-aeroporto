package br.edu.aeroporto.infraestrutura.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public final class AuditoriaJdbc {
    public void registrar(
            Connection conexao, long usuarioId, String acao, String entidade, long registroId)
            throws SQLException {
        Sql.executar(
                conexao,
                """
                INSERT INTO evento_auditoria(usuario_id, acao, entidade, registro_id)
                VALUES (?, ?, ?, ?)
                """,
                usuarioId,
                acao,
                entidade,
                registroId);
    }

    public void definirAutorDoVoo(Connection conexao, long usuarioId, String motivo)
            throws SQLException {
        Sql.unicoTexto(
                conexao,
                "SELECT set_config('aeroporto.usuario_id', ?, true)",
                Long.toString(usuarioId));
        Sql.unicoTexto(conexao, "SELECT set_config('aeroporto.motivo', ?, true)", motivo);
    }
}
