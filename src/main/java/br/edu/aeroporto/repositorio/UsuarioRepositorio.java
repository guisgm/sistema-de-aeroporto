package br.edu.aeroporto.repositorio;

import br.edu.aeroporto.dominio.Sessao;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface UsuarioRepositorio {
    record Credenciais(Sessao sessao, String senhaHash) {
        @Override
        public String toString() { return "Credenciais[usuarioId=" + sessao.usuarioId() + "]"; }
    }

    Optional<Credenciais> buscarAtivo(Connection conexao, String login) throws SQLException;
    boolean existeUsuario(Connection conexao) throws SQLException;
}
