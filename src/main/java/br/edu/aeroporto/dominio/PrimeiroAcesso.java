package br.edu.aeroporto.dominio;

import br.edu.aeroporto.excecao.RegraNegocioException;
import java.time.LocalDate;
import java.util.Locale;

public record PrimeiroAcesso(String nome, LocalDate nascimento, String login,
                            String cidade, String regiao, String aeroporto, String icao) {
    public PrimeiroAcesso {
        nome = Validacao.texto(nome, "Nome", 160);
        login = Validacao.login(login);
        cidade = Validacao.texto(cidade, "Cidade", 120);
        regiao = Validacao.texto(regiao, "Estado/região", 100);
        aeroporto = Validacao.texto(aeroporto, "Aeroporto", 160);
        icao = Validacao.texto(icao, "ICAO", 4).toUpperCase(Locale.ROOT);
        if (!icao.matches("[A-Z]{4}")) throw new RegraNegocioException("ICAO deve conter quatro letras.");
    }
}
