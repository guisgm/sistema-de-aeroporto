package br.edu.aeroporto.dominio;

import java.time.LocalDate;

public final class Funcionario extends Pessoa {
    private final String matricula;

    public Funcionario(long id, String nome, LocalDate nascimento, boolean ativo, String matricula) {
        super(id, nome, nascimento, ativo);
        this.matricula = Validacao.texto(matricula, "Matrícula", 30);
    }

    @Override
    public String identificacao() { return matricula; }
}
