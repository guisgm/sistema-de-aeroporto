package br.edu.aeroporto.dominio;

import java.math.BigDecimal;
import java.util.Set;

public final class Tarifa {
    private final String codigo;
    private final String classe;
    private final BigDecimal base;
    private final BigDecimal taxa;
    private final BigDecimal franquia;
    private final int pecas;
    private final boolean permiteCancelar;
    private final BigDecimal multa;

    public Tarifa(
            String codigo,
            String classe,
            BigDecimal base,
            BigDecimal taxa,
            BigDecimal franquia,
            int pecas,
            boolean permiteCancelar,
            BigDecimal multa) {
        codigo = Validacao.texto(codigo, "Codigo da tarifa", 20);
        Dados.exigir(
                Set.of("ECONOMICA", "EXECUTIVA", "PRIMEIRA").contains(classe), "Classe invalida.");
        base = Dados.valor(base, false);
        taxa = Dados.valor(taxa, false);
        franquia = Dados.valor(franquia, false);
        multa = Dados.valor(multa, false);
        Dados.exigir(pecas >= 0 && pecas <= 10, "Limite de pecas invalido.");
        Dados.exigir(multa.compareTo(base.add(taxa)) <= 0, "Multa supera a tarifa.");
        this.codigo = codigo;
        this.classe = classe;
        this.base = base;
        this.taxa = taxa;
        this.franquia = franquia;
        this.pecas = pecas;
        this.permiteCancelar = permiteCancelar;
        this.multa = multa;
    }

    public String codigo() {
        return codigo;
    }

    public String classe() {
        return classe;
    }

    public BigDecimal base() {
        return base;
    }

    public BigDecimal taxa() {
        return taxa;
    }

    public BigDecimal franquia() {
        return franquia;
    }

    public int pecas() {
        return pecas;
    }

    public boolean permiteCancelar() {
        return permiteCancelar;
    }

    public BigDecimal multa() {
        return multa;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof Tarifa)) return false;
        Tarifa outro = (Tarifa) objeto;
        return java.util.Objects.equals(codigo, outro.codigo)
                && java.util.Objects.equals(classe, outro.classe)
                && java.util.Objects.equals(base, outro.base)
                && java.util.Objects.equals(taxa, outro.taxa)
                && java.util.Objects.equals(franquia, outro.franquia)
                && pecas == outro.pecas
                && permiteCancelar == outro.permiteCancelar
                && java.util.Objects.equals(multa, outro.multa);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                codigo, classe, base, taxa, franquia, pecas, permiteCancelar, multa);
    }

    @Override
    public String toString() {
        return "Tarifa[codigo="
                + codigo
                + ", classe="
                + classe
                + ", base="
                + base
                + ", taxa="
                + taxa
                + ", franquia="
                + franquia
                + ", pecas="
                + pecas
                + ", permiteCancelar="
                + permiteCancelar
                + ", multa="
                + multa
                + "]";
    }
}
