package br.edu.aeroporto.dominio;

import java.math.BigDecimal;

public final class PedidoTrecho {
    private final long passageiro;
    private final long voo;
    private final long tarifa;
    private final int ordem;
    private final String assento;
    private final BigDecimal desconto;

    public PedidoTrecho(
            long passageiro,
            long voo,
            long tarifa,
            int ordem,
            String assento,
            BigDecimal desconto) {
        Dados.exigir(
                passageiro > 0 && voo > 0 && tarifa > 0 && ordem > 0 && ordem <= 32767,
                "Trecho invalido.");
        assento =
                assento == null || assento.isBlank()
                        ? null
                        : Validacao.texto(assento, "Assento", 6).toUpperCase(java.util.Locale.ROOT);
        desconto = Dados.valor(desconto, false);
        this.passageiro = passageiro;
        this.voo = voo;
        this.tarifa = tarifa;
        this.ordem = ordem;
        this.assento = assento;
        this.desconto = desconto;
    }

    public long passageiro() {
        return passageiro;
    }

    public long voo() {
        return voo;
    }

    public long tarifa() {
        return tarifa;
    }

    public int ordem() {
        return ordem;
    }

    public String assento() {
        return assento;
    }

    public BigDecimal desconto() {
        return desconto;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof PedidoTrecho)) return false;
        PedidoTrecho outro = (PedidoTrecho) objeto;
        return passageiro == outro.passageiro
                && voo == outro.voo
                && tarifa == outro.tarifa
                && ordem == outro.ordem
                && java.util.Objects.equals(assento, outro.assento)
                && java.util.Objects.equals(desconto, outro.desconto);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(passageiro, voo, tarifa, ordem, assento, desconto);
    }

    @Override
    public String toString() {
        return "PedidoTrecho[passageiro="
                + passageiro
                + ", voo="
                + voo
                + ", tarifa="
                + tarifa
                + ", ordem="
                + ordem
                + ", assento="
                + assento
                + ", desconto="
                + desconto
                + "]";
    }
}
