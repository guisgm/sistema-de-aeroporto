package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.Sessao;
import br.edu.aeroporto.excecao.RegraNegocioException;
import java.sql.Connection;
import java.sql.SQLException;

public final class AutorizacaoJdbc {
    public void exigirConsulta(Connection conexao, Sessao sessao) throws SQLException {
        exigir(conexao, sessao, true);
    }

    public void exigirAtendimento(Connection conexao, Sessao sessao) throws SQLException {
        exigir(conexao, sessao, false);
    }

    public void exigirAdministrador(Connection conexao, Sessao sessao) throws SQLException {
        exigirPerfil(conexao, sessao, "ADMINISTRADOR", "ADMINISTRADOR");
    }

    public void exigirOperacao(Connection conexao, Sessao sessao) throws SQLException {
        exigirPerfil(conexao, sessao, "ADMINISTRADOR", "OPERACAO");
    }

    private void exigirPerfil(Connection conexao, Sessao sessao, String primeiro, String segundo) throws SQLException {
        if (sessao == null) throw new RegraNegocioException("Entre no sistema antes de continuar.");
        boolean permitido = Sql.unico(conexao, """
                SELECT EXISTS (
                  SELECT 1 FROM usuario_sistema u JOIN funcionario f ON f.pessoa_id=u.funcionario_id
                  JOIN pessoa p ON p.id=f.pessoa_id JOIN usuario_perfil up ON up.usuario_id=u.id
                  JOIN perfil_acesso pa ON pa.id=up.perfil_id
                  WHERE u.id=? AND u.ativo AND p.ativo AND f.admissao<=CURRENT_DATE
                    AND (f.desligamento IS NULL OR f.desligamento>CURRENT_DATE) AND pa.nome IN (?,?)
                )
                """, linha -> linha.getBoolean(1), sessao.usuarioId(), primeiro, segundo).orElse(false);
        if (!permitido) throw new RegraNegocioException("Usuario inativo ou sem permissao para esta operacao.");
    }

    private void exigir(Connection conexao, Sessao sessao, boolean apenasConsulta) throws SQLException {
        if (sessao == null) throw new RegraNegocioException("Entre no sistema antes de continuar.");
        boolean permitido = Sql.unico(conexao, """
                SELECT EXISTS (
                    SELECT 1 FROM usuario_sistema u
                    JOIN funcionario f ON f.pessoa_id = u.funcionario_id
                    JOIN pessoa p ON p.id = f.pessoa_id
                    JOIN usuario_perfil up ON up.usuario_id = u.id
                    JOIN perfil_acesso pa ON pa.id = up.perfil_id
                    WHERE u.id = ? AND u.ativo AND p.ativo
                      AND f.admissao <= CURRENT_DATE
                      AND (f.desligamento IS NULL OR f.desligamento > CURRENT_DATE)
                      AND pa.nome IN ('ADMINISTRADOR', 'ATENDIMENTO', 'OPERACAO', 'CONSULTA')
                      AND (? OR pa.nome IN ('ADMINISTRADOR', 'ATENDIMENTO'))
                )
                """, linha -> linha.getBoolean(1), sessao.usuarioId(), apenasConsulta).orElse(false);
        if (!permitido) throw new RegraNegocioException("Usuário inativo ou sem permissão para esta operação.");
    }
}
