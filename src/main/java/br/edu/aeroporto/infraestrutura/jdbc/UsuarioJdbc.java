package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.Funcionario;
import br.edu.aeroporto.dominio.Sessao;
import br.edu.aeroporto.repositorio.UsuarioRepositorio;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;

public final class UsuarioJdbc implements UsuarioRepositorio {
    @Override
    public Optional<Credenciais> buscarAtivo(Connection conexao, String login) throws SQLException {
        return Sql.unico(conexao, """
                SELECT u.id, u.senha_hash, f.pessoa_id, f.matricula, p.nome, p.nascimento, p.ativo
                FROM usuario_sistema u JOIN funcionario f ON f.pessoa_id = u.funcionario_id
                JOIN pessoa p ON p.id = f.pessoa_id
                WHERE u.login = ? AND u.ativo AND p.ativo
                  AND f.admissao <= CURRENT_DATE
                  AND (f.desligamento IS NULL OR f.desligamento > CURRENT_DATE)
                """, linha -> {
            long usuarioId = linha.getLong("id");
            var funcionario = new Funcionario(linha.getLong("pessoa_id"), linha.getString("nome"),
                    linha.getObject("nascimento", LocalDate.class), linha.getBoolean("ativo"), linha.getString("matricula"));
            var perfis = new HashSet<>(Sql.listar(conexao, """
                    SELECT p.nome FROM usuario_perfil up JOIN perfil_acesso p ON p.id = up.perfil_id
                    WHERE up.usuario_id = ?
                    """, perfil -> perfil.getString(1), usuarioId));
            return new Credenciais(new Sessao(usuarioId, funcionario, perfis), linha.getString("senha_hash"));
        }, login);
    }

    @Override
    public boolean existeUsuario(Connection conexao) throws SQLException {
        return Sql.unico(conexao, "SELECT EXISTS (SELECT 1 FROM usuario_sistema)", linha -> linha.getBoolean(1)).orElse(false);
    }
}
