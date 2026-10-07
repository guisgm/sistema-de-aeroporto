package br.edu.aeroporto.repositorio;

import br.edu.aeroporto.dominio.Sessao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface UsuarioRepositorio {
    final class Credenciais {
        private final Sessao sessao;
        private final String senhaHash;

        public Credenciais(Sessao sessao, String senhaHash) {
            this.sessao = sessao;
            this.senhaHash = senhaHash;
        }

        public Sessao sessao() {
            return sessao;
        }

        public String senhaHash() {
            return senhaHash;
        }

        @Override
        public String toString() {
            return "Credenciais[usuarioId=" + sessao.usuarioId() + "]";
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) return true;
            if (!(objeto instanceof Credenciais)) return false;
            Credenciais outro = (Credenciais) objeto;
            return java.util.Objects.equals(sessao, outro.sessao)
                    && java.util.Objects.equals(senhaHash, outro.senhaHash);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(sessao, senhaHash);
        }
    }

    Optional<Credenciais> buscarAtivo(Connection conexao, String login) throws SQLException;

    boolean existeUsuario(Connection conexao) throws SQLException;
}
