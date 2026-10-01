package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.Sessao;
import br.edu.aeroporto.dominio.Validacao;
import br.edu.aeroporto.excecao.RegraNegocioException;
import br.edu.aeroporto.infraestrutura.jdbc.AuditoriaJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.BancoDados;
import br.edu.aeroporto.repositorio.UsuarioRepositorio;
import br.edu.aeroporto.seguranca.Senhas;
import java.util.Arrays;

public final class AutenticacaoServico {
    private final BancoDados banco;
    private final UsuarioRepositorio usuarios;
    private final AuditoriaJdbc auditoria;

    public AutenticacaoServico(BancoDados banco, UsuarioRepositorio usuarios, AuditoriaJdbc auditoria) {
        this.banco = banco;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    public boolean existeUsuario() {
        return banco.consultar(usuarios::existeUsuario);
    }

    public Sessao entrar(String login, char[] senha) {
        try {
            String normalizado = Validacao.login(login);
            var credenciais = banco.consultar(conexao -> usuarios.buscarAtivo(conexao, normalizado));
            if (credenciais.isEmpty() || !Senhas.conferir(senha, credenciais.get().senhaHash())) {
                throw new RegraNegocioException("Login ou senha inválidos.");
            }
            Sessao sessao = credenciais.get().sessao();
            if (sessao.perfis().isEmpty()) throw new RegraNegocioException("O usuário não possui perfil de acesso.");
            banco.transacao(conexao -> {
                auditoria.registrar(conexao, sessao.usuarioId(), "ENTRAR", "usuario_sistema", sessao.usuarioId());
                return null;
            });
            return sessao;
        } finally {
            Arrays.fill(senha, '\0');
        }
    }
}
