package br.edu.aeroporto.dominio;

import br.edu.aeroporto.excecao.RegraNegocioException;

import java.time.LocalDate;
import java.util.Locale;

public final class CadastroPassageiro {
    private final String nome;
    private final LocalDate nascimento;
    private final String cpf;
    private final String email;
    private final String telefone;

    public CadastroPassageiro(
            String nome, LocalDate nascimento, String cpf, String email, String telefone) {
        nome = Validacao.texto(nome, "Nome", 160);
        cpf = Validacao.cpf(cpf);
        email = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        telefone = telefone == null ? "" : telefone.strip().replaceAll("[()\\-\\s]", "");
        if (!email.isEmpty()
                && (email.length() > 160 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) {
            throw new RegraNegocioException("E-mail inválido.");
        }
        if (!telefone.isEmpty() && !telefone.matches("\\+?[0-9]{8,15}")) {
            throw new RegraNegocioException(
                    "Telefone inválido: use de 8 a 15 dígitos, com + opcional.");
        }
        this.nome = nome;
        this.nascimento = nascimento;
        this.cpf = cpf;
        this.email = email;
        this.telefone = telefone;
    }

    public String nome() {
        return nome;
    }

    public LocalDate nascimento() {
        return nascimento;
    }

    public String cpf() {
        return cpf;
    }

    public String email() {
        return email;
    }

    public String telefone() {
        return telefone;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof CadastroPassageiro)) return false;
        CadastroPassageiro outro = (CadastroPassageiro) objeto;
        return java.util.Objects.equals(nome, outro.nome)
                && java.util.Objects.equals(nascimento, outro.nascimento)
                && java.util.Objects.equals(cpf, outro.cpf)
                && java.util.Objects.equals(email, outro.email)
                && java.util.Objects.equals(telefone, outro.telefone);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(nome, nascimento, cpf, email, telefone);
    }

    @Override
    public String toString() {
        return "CadastroPassageiro[nome="
                + nome
                + ", nascimento="
                + nascimento
                + ", cpf="
                + cpf
                + ", email="
                + email
                + ", telefone="
                + telefone
                + "]";
    }
}
