package br.edu.aeroporto.infraestrutura.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface OperacaoSql<T> {
    T executar(Connection conexao) throws SQLException;
}
