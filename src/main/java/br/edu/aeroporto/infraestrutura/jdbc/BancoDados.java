package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.config.Configuracao;
import br.edu.aeroporto.excecao.PersistenciaException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class BancoDados {
    private final Configuracao configuracao;

    public BancoDados(Configuracao configuracao) {
        this.configuracao = configuracao;
    }

    public <T> T consultar(OperacaoSql<T> operacao) {
        return executar(operacao, true);
    }

    public <T> T transacao(OperacaoSql<T> operacao) {
        return executar(operacao, false);
    }

    public <T> T transacaoRepetivel(OperacaoSql<T> operacao) {
        for (int tentativa = 0; ; tentativa++) {
            try {
                return transacao(operacao);
            } catch (PersistenciaException erro) {
                String estado = ((SQLException) erro.getCause()).getSQLState();
                if (tentativa >= 2 || !("40P01".equals(estado) || "40001".equals(estado))) throw erro;
            }
        }
    }

    private <T> T executar(OperacaoSql<T> operacao, boolean somenteLeitura) {
        try (Connection conexao = abrir()) {
            conexao.setReadOnly(somenteLeitura);
            if (somenteLeitura) conexao.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            conexao.setAutoCommit(false);
            try {
                T resultado = operacao.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException erro) {
                try {
                    conexao.rollback();
                } catch (SQLException falhaRollback) {
                    erro.addSuppressed(falhaRollback);
                }
                throw erro;
            }
        } catch (SQLException erro) {
            throw new PersistenciaException(erro);
        }
    }

    private Connection abrir() throws SQLException {
        Properties propriedades = new Properties();
        propriedades.setProperty("user", configuracao.usuario());
        propriedades.setProperty("password", configuracao.senha());
        propriedades.setProperty("currentSchema", "aeroporto,public");
        propriedades.setProperty("ApplicationName", "sistema-aeroporto");
        propriedades.setProperty("connectTimeout", "10");
        propriedades.setProperty("socketTimeout", "60");
        propriedades.setProperty("options", "-c timezone=" + configuracao.fuso().getId());
        return DriverManager.getConnection(configuracao.url(), propriedades);
    }
}
