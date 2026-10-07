package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;
import br.edu.aeroporto.seguranca.Senhas;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdministracaoServico {
    private static final Set<String> PERFIS =
            Set.of("ADMINISTRADOR", "ATENDIMENTO", "OPERACAO", "CONSULTA");
    private final BancoDados banco;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;

    public AdministracaoServico(
            BancoDados banco, AutorizacaoJdbc autorizacao, AuditoriaJdbc auditoria) {
        this.banco = banco;
        this.autorizacao = autorizacao;
        this.auditoria = auditoria;
    }

    public List<Map<String, Object>> usuarios(Sessao sessao) {
        return banco.consultar(
                c -> {
                    autorizacao.exigirAdministrador(c, sessao);
                    return Sql.listar(
                            c,
                            """
                    SELECT u.id,u.funcionario_id,u.login,u.ativo,array_agg(p.nome ORDER BY p.nome) AS perfis
                    FROM usuario_sistema u LEFT JOIN usuario_perfil up ON up.usuario_id=u.id
                    LEFT JOIN perfil_acesso p ON p.id=up.perfil_id GROUP BY u.id ORDER BY u.id
                    """);
                });
    }

    public long criarUsuario(
            Sessao sessao, long funcionario, String login, char[] senha, Set<String> perfis) {
        try {
            validarSenha(senha);
            validarPerfis(perfis);
            String normalizado = Validacao.login(login);
            return banco.transacao(
                    c -> {
                        autorizacao.exigirAdministrador(c, sessao);
                        Sql.registro(
                                c,
                                "SELECT f.pessoa_id FROM funcionario f JOIN pessoa p ON p.id=f.pessoa_id WHERE f.pessoa_id=? AND p.ativo AND f.admissao<=CURRENT_DATE AND (f.desligamento IS NULL OR f.desligamento>CURRENT_DATE) FOR UPDATE OF f",
                                funcionario);
                        long id =
                                Sql.inserir(
                                        c,
                                        "INSERT INTO usuario_sistema(funcionario_id,login,senha_hash) VALUES (?,?,?) RETURNING id",
                                        funcionario,
                                        normalizado,
                                        Senhas.gerar(senha));
                        for (String perfil : perfis)
                            Dados.exigir(
                                    Sql.executar(
                                                    c,
                                                    "INSERT INTO usuario_perfil(usuario_id,perfil_id) SELECT ?,id FROM perfil_acesso WHERE nome=?",
                                                    id,
                                                    perfil)
                                            == 1,
                                    "Perfil nao cadastrado.");
                        auditoria.registrar(
                                c, sessao.usuarioId(), "CRIAR_USUARIO", "usuario_sistema", id);
                        return id;
                    });
        } finally {
            if (senha != null) Arrays.fill(senha, '\0');
        }
    }

    public void acesso(Sessao sessao, long usuario, boolean ativo, Set<String> perfis) {
        validarPerfis(perfis);
        banco.transacao(
                c -> {
                    Sql.unico(
                            c,
                            "SELECT pg_advisory_xact_lock(hashtext('aeroporto.acesso'))",
                            r -> true);
                    autorizacao.exigirAdministrador(c, sessao);
                    Sql.registro(
                            c, "SELECT id FROM usuario_sistema WHERE id=? FOR UPDATE", usuario);
                    Sql.executar(
                            c, "UPDATE usuario_sistema SET ativo=? WHERE id=?", ativo, usuario);
                    Sql.executar(c, "DELETE FROM usuario_perfil WHERE usuario_id=?", usuario);
                    for (String perfil : perfis)
                        Dados.exigir(
                                Sql.executar(
                                                c,
                                                "INSERT INTO usuario_perfil(usuario_id,perfil_id) SELECT ?,id FROM perfil_acesso WHERE nome=?",
                                                usuario,
                                                perfil)
                                        == 1,
                                "Perfil nao cadastrado.");
                    Dados.exigir(
                            Sql.unicoBooleano(
                                            c,
                                            """
                    SELECT EXISTS (SELECT 1 FROM usuario_sistema u JOIN usuario_perfil up ON up.usuario_id=u.id
                    JOIN perfil_acesso pa ON pa.id=up.perfil_id JOIN funcionario f ON f.pessoa_id=u.funcionario_id
                    JOIN pessoa p ON p.id=f.pessoa_id WHERE u.ativo AND p.ativo AND pa.nome='ADMINISTRADOR'
                    AND f.admissao<=CURRENT_DATE AND (f.desligamento IS NULL OR f.desligamento>CURRENT_DATE))
                    """)
                                    .orElse(false),
                            "Preserve pelo menos um administrador ativo.");
                    auditoria.registrar(
                            c,
                            sessao.usuarioId(),
                            ativo ? "ALTERAR_PERFIS" : "BLOQUEAR_USUARIO",
                            "usuario_sistema",
                            usuario);
                    return null;
                });
    }

    public void alterarSenha(Sessao sessao, char[] atual, char[] nova) {
        try {
            validarSenha(nova);
            banco.transacao(
                    c -> {
                        autorizacao.exigirConsulta(c, sessao);
                        String hash =
                                Sql.registro(
                                                c,
                                                "SELECT senha_hash FROM usuario_sistema WHERE id=? FOR UPDATE",
                                                sessao.usuarioId())
                                        .get("senha_hash")
                                        .toString();
                        Dados.exigir(Senhas.conferir(atual, hash), "Senha atual incorreta.");
                        Sql.executar(
                                c,
                                "UPDATE usuario_sistema SET senha_hash=? WHERE id=?",
                                Senhas.gerar(nova),
                                sessao.usuarioId());
                        auditoria.registrar(
                                c,
                                sessao.usuarioId(),
                                "ALTERAR_SENHA",
                                "usuario_sistema",
                                sessao.usuarioId());
                        return null;
                    });
        } finally {
            if (atual != null) Arrays.fill(atual, '\0');
            if (nova != null) Arrays.fill(nova, '\0');
        }
    }

    private static void validarSenha(char[] senha) {
        Dados.exigir(
                senha != null && senha.length >= 12 && senha.length <= 128,
                "Senha deve ter de 12 a 128 caracteres.");
    }

    private static void validarPerfis(Set<String> perfis) {
        Dados.exigir(
                perfis != null && !perfis.isEmpty() && PERFIS.containsAll(perfis),
                "Selecione perfis conhecidos: " + PERFIS);
    }
}
