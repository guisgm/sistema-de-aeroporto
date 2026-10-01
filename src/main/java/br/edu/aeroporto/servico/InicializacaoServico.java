package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.PrimeiroAcesso;
import br.edu.aeroporto.dominio.Validacao;
import br.edu.aeroporto.excecao.RegraNegocioException;
import br.edu.aeroporto.infraestrutura.jdbc.AuditoriaJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.BancoDados;
import br.edu.aeroporto.infraestrutura.jdbc.InicializacaoJdbc;
import br.edu.aeroporto.seguranca.Senhas;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;

public final class InicializacaoServico {
    private final BancoDados banco;
    private final InicializacaoJdbc inicializacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;

    public InicializacaoServico(BancoDados banco, InicializacaoJdbc inicializacao, AuditoriaJdbc auditoria, Clock relogio) {
        this.banco = banco;
        this.inicializacao = inicializacao;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public void criar(PrimeiroAcesso dados, char[] senha) {
        try {
            if (senha.length < 12 || senha.length > 128) throw new RegraNegocioException("Use uma senha de 12 a 128 caracteres.");
            LocalDate hoje = LocalDate.now(relogio);
            Validacao.nascimento(dados.nascimento(), hoje);
            String hash = Senhas.gerar(senha);
            banco.transacao(conexao -> {
                long usuario = inicializacao.criarAdministrador(conexao, dados, hash, hoje, relogio.getZone());
                auditoria.registrar(conexao, usuario, "CRIAR_PRIMEIRO_ACESSO", "usuario_sistema", usuario);
                return null;
            });
        } finally {
            Arrays.fill(senha, '\0');
        }
    }
}
