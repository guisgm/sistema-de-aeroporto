package br.edu.aeroporto.infraestrutura.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/** Centraliza recursos JDBC; as consultas e regras continuam nos seus módulos. */
public final class Sql {
    private Sql() {}

    public static Map<String, Object> linha(ResultSet resultado) throws SQLException {
        var valores = new LinkedHashMap<String, Object>();
        var metadados = resultado.getMetaData();
        for (int coluna = 1; coluna <= metadados.getColumnCount(); coluna++) {
            valores.put(metadados.getColumnLabel(coluna), resultado.getObject(coluna));
        }
        return Collections.unmodifiableMap(valores);
    }

    public static Map<String, Object> registro(Connection conexao, String consulta, Object... parametros) throws SQLException {
        return unico(conexao, consulta, Sql::linha, parametros)
                .orElseThrow(() -> new br.edu.aeroporto.excecao.RegraNegocioException("Registro nao encontrado."));
    }

    @FunctionalInterface
    public interface Mapeador<T> {
        T mapear(ResultSet linha) throws SQLException;
    }

    public static <T> List<T> listar(Connection conexao, String sql, Mapeador<T> mapeador,
                                    Object... parametros) throws SQLException {
        try (PreparedStatement comando = preparar(conexao, sql, parametros);
             ResultSet linhas = comando.executeQuery()) {
            List<T> resultado = new ArrayList<>();
            while (linhas.next()) resultado.add(mapeador.mapear(linhas));
            return List.copyOf(resultado);
        }
    }

    public static <T> Optional<T> unico(Connection conexao, String sql, Mapeador<T> mapeador,
                                         Object... parametros) throws SQLException {
        try (PreparedStatement comando = preparar(conexao, sql, parametros);
             ResultSet linhas = comando.executeQuery()) {
            if (!linhas.next()) return Optional.empty();
            T resultado = mapeador.mapear(linhas);
            if (linhas.next()) throw new SQLException("A consulta esperava no máximo uma linha.");
            return Optional.of(resultado);
        }
    }

    public static long inserir(Connection conexao, String sql, Object... parametros) throws SQLException {
        return unico(conexao, sql, linha -> linha.getLong(1), parametros)
                .orElseThrow(() -> new SQLException("A inserção não retornou o identificador."));
    }

    public static int executar(Connection conexao, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement comando = preparar(conexao, sql, parametros)) {
            return comando.executeUpdate();
        }
    }

    private static PreparedStatement preparar(Connection conexao, String sql, Object... parametros)
            throws SQLException {
        PreparedStatement comando = conexao.prepareStatement(sql);
        try {
            comando.setQueryTimeout(30);
            for (int i = 0; i < parametros.length; i++) {
                Object valor = parametros[i];
                if (valor instanceof Enum<?> estado) valor = estado.name();
                if (valor instanceof Instant instante) valor = instante.truncatedTo(java.time.temporal.ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
                comando.setObject(i + 1, valor);
            }
            return comando;
        } catch (SQLException erro) {
            try { comando.close(); } catch (SQLException fechamento) { erro.addSuppressed(fechamento); }
            throw erro;
        }
    }
}
