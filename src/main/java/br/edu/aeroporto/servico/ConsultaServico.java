package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.Sessao;
import br.edu.aeroporto.dominio.Validacao;
import br.edu.aeroporto.dto.AssentoResumo;
import br.edu.aeroporto.dto.ReservaResumo;
import br.edu.aeroporto.dto.VooResumo;
import br.edu.aeroporto.excecao.RegraNegocioException;
import br.edu.aeroporto.infraestrutura.jdbc.AutorizacaoJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.BancoDados;
import br.edu.aeroporto.repositorio.ConsultasRepositorio;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

public final class ConsultaServico {
    private final BancoDados banco;
    private final ConsultasRepositorio consultas;
    private final AutorizacaoJdbc autorizacao;

    public ConsultaServico(BancoDados banco, ConsultasRepositorio consultas, AutorizacaoJdbc autorizacao) {
        this.banco = banco;
        this.consultas = consultas;
        this.autorizacao = autorizacao;
    }

    public List<VooResumo> voos(Sessao sessao, LocalDate dia, ZoneId fuso, int pagina) {
        if (dia == null || fuso == null || pagina < 1 || pagina > 100_000) throw new RegraNegocioException("Data, fuso ou página inválidos.");
        return banco.consultar(conexao -> {
            autorizacao.exigirConsulta(conexao, sessao);
            return consultas.voos(conexao, dia.atStartOfDay(fuso).toInstant(),
                    dia.plusDays(1).atStartOfDay(fuso).toInstant(), pagina);
        });
    }

    public List<AssentoResumo> assentos(Sessao sessao, long vooId) {
        if (vooId <= 0) throw new RegraNegocioException("O id do voo deve ser positivo.");
        return banco.consultar(conexao -> {
            autorizacao.exigirConsulta(conexao, sessao);
            return consultas.assentos(conexao, vooId);
        });
    }

    public ReservaResumo reserva(Sessao sessao, String localizador) {
        String codigo = Validacao.texto(localizador, "Localizador", 12).toUpperCase(Locale.ROOT);
        return banco.consultar(conexao -> {
            autorizacao.exigirConsulta(conexao, sessao);
            return consultas.reserva(conexao, codigo);
        }).orElseThrow(() -> new RegraNegocioException("Reserva não encontrada."));
    }
}
