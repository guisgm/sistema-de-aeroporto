package br.edu.aeroporto.dto;

import br.edu.aeroporto.dominio.SituacaoReserva;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class ReservaResumo {
    private final long id;
    private final String localizador;
    private final SituacaoReserva situacao;
    private final OffsetDateTime expiraEm;
    private final List<Trecho> trechos;

    public ReservaResumo(
            long id,
            String localizador,
            SituacaoReserva situacao,
            OffsetDateTime expiraEm,
            List<Trecho> trechos) {
        trechos = List.copyOf(trechos);
        this.id = id;
        this.localizador = localizador;
        this.situacao = situacao;
        this.expiraEm = expiraEm;
        this.trechos = trechos;
    }

    public long id() {
        return id;
    }

    public String localizador() {
        return localizador;
    }

    public SituacaoReserva situacao() {
        return situacao;
    }

    public OffsetDateTime expiraEm() {
        return expiraEm;
    }

    public List<Trecho> trechos() {
        return trechos;
    }

    public static final class Trecho {
        private final long id;
        private final String passageiro;
        private final String voo;
        private final int ordem;
        private final String situacao;
        private final String assento;
        private final BigDecimal valor;

        public Trecho(
                long id,
                String passageiro,
                String voo,
                int ordem,
                String situacao,
                String assento,
                BigDecimal valor) {
            this.id = id;
            this.passageiro = passageiro;
            this.voo = voo;
            this.ordem = ordem;
            this.situacao = situacao;
            this.assento = assento;
            this.valor = valor;
        }

        public long id() {
            return id;
        }

        public String passageiro() {
            return passageiro;
        }

        public String voo() {
            return voo;
        }

        public int ordem() {
            return ordem;
        }

        public String situacao() {
            return situacao;
        }

        public String assento() {
            return assento;
        }

        public BigDecimal valor() {
            return valor;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) return true;
            if (!(objeto instanceof Trecho)) return false;
            Trecho outro = (Trecho) objeto;
            return id == outro.id
                    && java.util.Objects.equals(passageiro, outro.passageiro)
                    && java.util.Objects.equals(voo, outro.voo)
                    && ordem == outro.ordem
                    && java.util.Objects.equals(situacao, outro.situacao)
                    && java.util.Objects.equals(assento, outro.assento)
                    && java.util.Objects.equals(valor, outro.valor);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, passageiro, voo, ordem, situacao, assento, valor);
        }

        @Override
        public String toString() {
            return "Trecho[id="
                    + id
                    + ", passageiro="
                    + passageiro
                    + ", voo="
                    + voo
                    + ", ordem="
                    + ordem
                    + ", situacao="
                    + situacao
                    + ", assento="
                    + assento
                    + ", valor="
                    + valor
                    + "]";
        }
    }

    public BigDecimal totalOriginalPassagens() {
        BigDecimal total = BigDecimal.ZERO;
        for (Trecho trecho : trechos) total = total.add(trecho.valor());
        return total;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof ReservaResumo)) return false;
        ReservaResumo outro = (ReservaResumo) objeto;
        return id == outro.id
                && java.util.Objects.equals(localizador, outro.localizador)
                && java.util.Objects.equals(situacao, outro.situacao)
                && java.util.Objects.equals(expiraEm, outro.expiraEm)
                && java.util.Objects.equals(trechos, outro.trechos);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, localizador, situacao, expiraEm, trechos);
    }

    @Override
    public String toString() {
        return "ReservaResumo[id="
                + id
                + ", localizador="
                + localizador
                + ", situacao="
                + situacao
                + ", expiraEm="
                + expiraEm
                + ", trechos="
                + trechos
                + "]";
    }
}
