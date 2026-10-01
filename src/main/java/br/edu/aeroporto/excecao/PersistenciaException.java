package br.edu.aeroporto.excecao;

import java.sql.SQLException;

public final class PersistenciaException extends RuntimeException {
    public PersistenciaException(SQLException causa) {
        super(mensagem(causa.getSQLState()), causa);
    }

    private static String mensagem(String estado) {
        if (estado == null) return "Não foi possível acessar o banco de dados.";
        return switch (estado) {
            case "23505" -> "Já existe um registro com essa identificação.";
            case "23503" -> "Um cadastro referenciado não existe ou ainda está em uso.";
            case "23514", "23502" -> "Os dados não atendem às regras do banco.";
            case "23P01" -> "Há conflito de horário na agenda.";
            case "40001", "40P01" -> "A operação concorrente foi interrompida. Tente novamente.";
            case "42P01", "3F000" -> "A estrutura do banco não está pronta. Execute sql/criar_banco.sql.";
            case "28P01", "28000" -> "Confira o usuário e a senha da conexão PostgreSQL.";
            default -> estado.startsWith("08")
                    ? "Não foi possível conectar ao PostgreSQL. Confira o serviço e a configuração."
                    : "Não foi possível concluir a operação no banco. Consulte o log técnico.";
        };
    }
}
