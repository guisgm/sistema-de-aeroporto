package br.edu.aeroporto.dominio;

import java.time.LocalDate;

public final class Passageiro extends Pessoa {
    private final String codigoCliente;

    public Passageiro(
            long id, String nome, LocalDate nascimento, boolean ativo, String codigoCliente) {
        super(id, nome, nascimento, ativo);
        this.codigoCliente = Validacao.texto(codigoCliente, "Código do cliente", 30);
    }

    public String codigoCliente() {
        return codigoCliente;
    }

    @Override
    public String identificacao() {
        return codigoCliente;
    }
}
