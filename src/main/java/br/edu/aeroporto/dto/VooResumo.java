package br.edu.aeroporto.dto;

import br.edu.aeroporto.dominio.SituacaoVoo;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class VooResumo {
    private final long id;
    private final String companhia;
    private final String numero;
    private final String origem;
    private final String destino;
    private final ZoneId fusoOrigem;
    private final ZoneId fusoDestino;
    private final OffsetDateTime partida;
    private final OffsetDateTime chegada;
    private final OffsetDateTime partidaEstimada;
    private final OffsetDateTime partidaReal;
    private final SituacaoVoo situacao;

    public VooResumo(
            long id,
            String companhia,
            String numero,
            String origem,
            String destino,
            ZoneId fusoOrigem,
            ZoneId fusoDestino,
            OffsetDateTime partida,
            OffsetDateTime chegada,
            OffsetDateTime partidaEstimada,
            OffsetDateTime partidaReal,
            SituacaoVoo situacao) {
        this.id = id;
        this.companhia = companhia;
        this.numero = numero;
        this.origem = origem;
        this.destino = destino;
        this.fusoOrigem = fusoOrigem;
        this.fusoDestino = fusoDestino;
        this.partida = partida;
        this.chegada = chegada;
        this.partidaEstimada = partidaEstimada;
        this.partidaReal = partidaReal;
        this.situacao = situacao;
    }

    public long id() {
        return id;
    }

    public String companhia() {
        return companhia;
    }

    public String numero() {
        return numero;
    }

    public String origem() {
        return origem;
    }

    public String destino() {
        return destino;
    }

    public ZoneId fusoOrigem() {
        return fusoOrigem;
    }

    public ZoneId fusoDestino() {
        return fusoDestino;
    }

    public OffsetDateTime partida() {
        return partida;
    }

    public OffsetDateTime chegada() {
        return chegada;
    }

    public OffsetDateTime partidaEstimada() {
        return partidaEstimada;
    }

    public OffsetDateTime partidaReal() {
        return partidaReal;
    }

    public SituacaoVoo situacao() {
        return situacao;
    }

    public boolean atrasado() {
        OffsetDateTime referencia = partidaReal != null ? partidaReal : partidaEstimada;
        return referencia != null && referencia.isAfter(partida);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof VooResumo)) return false;
        VooResumo outro = (VooResumo) objeto;
        return id == outro.id
                && java.util.Objects.equals(companhia, outro.companhia)
                && java.util.Objects.equals(numero, outro.numero)
                && java.util.Objects.equals(origem, outro.origem)
                && java.util.Objects.equals(destino, outro.destino)
                && java.util.Objects.equals(fusoOrigem, outro.fusoOrigem)
                && java.util.Objects.equals(fusoDestino, outro.fusoDestino)
                && java.util.Objects.equals(partida, outro.partida)
                && java.util.Objects.equals(chegada, outro.chegada)
                && java.util.Objects.equals(partidaEstimada, outro.partidaEstimada)
                && java.util.Objects.equals(partidaReal, outro.partidaReal)
                && java.util.Objects.equals(situacao, outro.situacao);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                id,
                companhia,
                numero,
                origem,
                destino,
                fusoOrigem,
                fusoDestino,
                partida,
                chegada,
                partidaEstimada,
                partidaReal,
                situacao);
    }

    @Override
    public String toString() {
        return "VooResumo[id="
                + id
                + ", companhia="
                + companhia
                + ", numero="
                + numero
                + ", origem="
                + origem
                + ", destino="
                + destino
                + ", fusoOrigem="
                + fusoOrigem
                + ", fusoDestino="
                + fusoDestino
                + ", partida="
                + partida
                + ", chegada="
                + chegada
                + ", partidaEstimada="
                + partidaEstimada
                + ", partidaReal="
                + partidaReal
                + ", situacao="
                + situacao
                + "]";
    }
}
