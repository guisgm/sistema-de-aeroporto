package br.edu.aeroporto.dominio;

import java.time.LocalDate;
import java.util.Objects;

public abstract class Pessoa {
    private final long id;
    private final String nome;
    private final LocalDate nascimento;
    private final boolean ativa;

    protected Pessoa(long id, String nome, LocalDate nascimento, boolean ativa) {
        if (id <= 0) throw new IllegalArgumentException("Id persistido deve ser positivo.");
        this.id = id;
        this.nome = Validacao.texto(nome, "Nome", 160);
        this.nascimento = Objects.requireNonNull(nascimento);
        this.ativa = ativa;
    }

    public final long id() {
        return id;
    }

    public final String nome() {
        return nome;
    }

    public final LocalDate nascimento() {
        return nascimento;
    }

    public final boolean ativa() {
        return ativa;
    }

    public abstract String identificacao();

    @Override
    public final boolean equals(Object outro) {
        // A mesma pessoa pode ter os dois papéis; a identidade comum é pessoa.id.
        if (this == outro) return true;
        if (!(outro instanceof Pessoa)) return false;
        Pessoa pessoa = (Pessoa) outro;
        return id == pessoa.id;
    }

    @Override
    public final int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return identificacao() + " — " + nome;
    }
}
