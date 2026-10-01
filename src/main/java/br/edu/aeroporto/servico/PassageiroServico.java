package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.CadastroPassageiro;
import br.edu.aeroporto.dominio.Passageiro;
import br.edu.aeroporto.dominio.Sessao;
import br.edu.aeroporto.dominio.Validacao;
import br.edu.aeroporto.excecao.RegraNegocioException;
import br.edu.aeroporto.infraestrutura.jdbc.AuditoriaJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.AutorizacaoJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.BancoDados;
import br.edu.aeroporto.repositorio.PassageiroRepositorio;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class PassageiroServico {
    private final BancoDados banco;
    private final PassageiroRepositorio repositorio;
    private final AuditoriaJdbc auditoria;
    private final AutorizacaoJdbc autorizacao;
    private final Clock relogio;

    public PassageiroServico(BancoDados banco, PassageiroRepositorio repositorio,
                             AuditoriaJdbc auditoria, AutorizacaoJdbc autorizacao, Clock relogio) {
        this.banco = banco;
        this.repositorio = repositorio;
        this.auditoria = auditoria;
        this.autorizacao = autorizacao;
        this.relogio = relogio;
    }

    public Passageiro cadastrar(Sessao sessao, CadastroPassageiro cadastro) {
        Validacao.nascimento(cadastro.nascimento(), LocalDate.now(relogio));
        return banco.transacao(conexao -> {
            autorizacao.exigirAtendimento(conexao, sessao);
            Passageiro passageiro = repositorio.cadastrar(conexao, cadastro);
            auditoria.registrar(conexao, sessao.usuarioId(), "CADASTRAR_PASSAGEIRO", "passageiro", passageiro.id());
            return passageiro;
        });
    }

    public Passageiro buscar(Sessao sessao, long id) {
        if (id <= 0) throw new RegraNegocioException("O id deve ser positivo.");
        return banco.consultar(conexao -> {
            autorizacao.exigirConsulta(conexao, sessao);
            return repositorio.buscar(conexao, id);
        })
                .orElseThrow(() -> new RegraNegocioException("Passageiro não encontrado."));
    }

    public List<Passageiro> buscar(Sessao sessao, String nome, int pagina) {
        if (pagina < 1 || pagina > 100_000) throw new RegraNegocioException("Página fora do intervalo permitido.");
        if (nome == null || nome.length() > 160) throw new RegraNegocioException("Filtro de nome inválido.");
        return banco.consultar(conexao -> {
            autorizacao.exigirConsulta(conexao, sessao);
            return repositorio.buscar(conexao, nome.strip(), 20, (pagina - 1) * 20);
        });
    }
}
