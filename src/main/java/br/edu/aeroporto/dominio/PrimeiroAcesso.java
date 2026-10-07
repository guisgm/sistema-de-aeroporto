package br.edu.aeroporto.dominio;

import br.edu.aeroporto.excecao.RegraNegocioException;

import java.time.LocalDate;
import java.util.Locale;

public final class PrimeiroAcesso {
    private final String nome;
    private final LocalDate nascimento;
    private final String login;
    private final String cidade;
    private final String regiao;
    private final String aeroporto;
    private final String icao;

    public PrimeiroAcesso(
            String nome,
            LocalDate nascimento,
            String login,
            String cidade,
            String regiao,
            String aeroporto,
            String icao) {
        nome = Validacao.texto(nome, "Nome", 160);
        login = Validacao.login(login);
        cidade = Validacao.texto(cidade, "Cidade", 120);
        regiao = Validacao.texto(regiao, "Estado/região", 100);
        aeroporto = Validacao.texto(aeroporto, "Aeroporto", 160);
        icao = Validacao.texto(icao, "ICAO", 4).toUpperCase(Locale.ROOT);
        if (!icao.matches("[A-Z]{4}"))
            throw new RegraNegocioException("ICAO deve conter quatro letras.");
        this.nome = nome;
        this.nascimento = nascimento;
        this.login = login;
        this.cidade = cidade;
        this.regiao = regiao;
        this.aeroporto = aeroporto;
        this.icao = icao;
    }

    public String nome() {
        return nome;
    }

    public LocalDate nascimento() {
        return nascimento;
    }

    public String login() {
        return login;
    }

    public String cidade() {
        return cidade;
    }

    public String regiao() {
        return regiao;
    }

    public String aeroporto() {
        return aeroporto;
    }

    public String icao() {
        return icao;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof PrimeiroAcesso)) return false;
        PrimeiroAcesso outro = (PrimeiroAcesso) objeto;
        return java.util.Objects.equals(nome, outro.nome)
                && java.util.Objects.equals(nascimento, outro.nascimento)
                && java.util.Objects.equals(login, outro.login)
                && java.util.Objects.equals(cidade, outro.cidade)
                && java.util.Objects.equals(regiao, outro.regiao)
                && java.util.Objects.equals(aeroporto, outro.aeroporto)
                && java.util.Objects.equals(icao, outro.icao);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(nome, nascimento, login, cidade, regiao, aeroporto, icao);
    }

    @Override
    public String toString() {
        return "PrimeiroAcesso[nome="
                + nome
                + ", nascimento="
                + nascimento
                + ", login="
                + login
                + ", cidade="
                + cidade
                + ", regiao="
                + regiao
                + ", aeroporto="
                + aeroporto
                + ", icao="
                + icao
                + "]";
    }
}
