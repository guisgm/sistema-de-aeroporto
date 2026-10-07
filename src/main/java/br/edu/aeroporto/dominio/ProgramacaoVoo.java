package br.edu.aeroporto.dominio;

import java.time.Instant;

public final class ProgramacaoVoo {
    private final long companhia;
    private final long rota;
    private final long aeronave;
    private final String numero;
    private final Instant partida;
    private final Instant chegada;
    private final Instant checkinAbre;
    private final Instant checkinFecha;
    private final Instant embarqueAbre;
    private final Instant embarqueFecha;

    public ProgramacaoVoo(
            long companhia,
            long rota,
            long aeronave,
            String numero,
            Instant partida,
            Instant chegada,
            Instant checkinAbre,
            Instant checkinFecha,
            Instant embarqueAbre,
            Instant embarqueFecha) {
        Dados.exigir(companhia > 0 && rota > 0 && aeronave > 0, "Cadastros do voo invalidos.");
        numero = Validacao.texto(numero, "Numero do voo", 8);
        Dados.exigir(numero.matches("[A-Z0-9]{1,8}"), "Numero de voo invalido.");
        validarHorarios(partida, chegada, checkinAbre, checkinFecha, embarqueAbre, embarqueFecha);
        partida = partida.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        chegada = chegada.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        checkinAbre = checkinAbre.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        checkinFecha = checkinFecha.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        embarqueAbre = embarqueAbre.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        embarqueFecha = embarqueFecha.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        validarHorarios(partida, chegada, checkinAbre, checkinFecha, embarqueAbre, embarqueFecha);
        this.companhia = companhia;
        this.rota = rota;
        this.aeronave = aeronave;
        this.numero = numero;
        this.partida = partida;
        this.chegada = chegada;
        this.checkinAbre = checkinAbre;
        this.checkinFecha = checkinFecha;
        this.embarqueAbre = embarqueAbre;
        this.embarqueFecha = embarqueFecha;
    }

    public long companhia() {
        return companhia;
    }

    public long rota() {
        return rota;
    }

    public long aeronave() {
        return aeronave;
    }

    public String numero() {
        return numero;
    }

    public Instant partida() {
        return partida;
    }

    public Instant chegada() {
        return chegada;
    }

    public Instant checkinAbre() {
        return checkinAbre;
    }

    public Instant checkinFecha() {
        return checkinFecha;
    }

    public Instant embarqueAbre() {
        return embarqueAbre;
    }

    public Instant embarqueFecha() {
        return embarqueFecha;
    }

    public static void validarHorarios(
            Instant partida, Instant chegada, Instant ca, Instant cf, Instant ea, Instant ef) {
        Dados.exigir(
                partida != null
                        && chegada != null
                        && ca != null
                        && cf != null
                        && ea != null
                        && ef != null,
                "Informe todos os horarios.");
        Dados.exigir(
                chegada.isAfter(partida)
                        && ca.isBefore(cf)
                        && !cf.isAfter(ef)
                        && ea.isBefore(ef)
                        && !ef.isAfter(partida),
                "Janelas e horarios do voo invalidos.");
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof ProgramacaoVoo)) return false;
        ProgramacaoVoo outro = (ProgramacaoVoo) objeto;
        return companhia == outro.companhia
                && rota == outro.rota
                && aeronave == outro.aeronave
                && java.util.Objects.equals(numero, outro.numero)
                && java.util.Objects.equals(partida, outro.partida)
                && java.util.Objects.equals(chegada, outro.chegada)
                && java.util.Objects.equals(checkinAbre, outro.checkinAbre)
                && java.util.Objects.equals(checkinFecha, outro.checkinFecha)
                && java.util.Objects.equals(embarqueAbre, outro.embarqueAbre)
                && java.util.Objects.equals(embarqueFecha, outro.embarqueFecha);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                companhia,
                rota,
                aeronave,
                numero,
                partida,
                chegada,
                checkinAbre,
                checkinFecha,
                embarqueAbre,
                embarqueFecha);
    }

    @Override
    public String toString() {
        return "ProgramacaoVoo[companhia="
                + companhia
                + ", rota="
                + rota
                + ", aeronave="
                + aeronave
                + ", numero="
                + numero
                + ", partida="
                + partida
                + ", chegada="
                + chegada
                + ", checkinAbre="
                + checkinAbre
                + ", checkinFecha="
                + checkinFecha
                + ", embarqueAbre="
                + embarqueAbre
                + ", embarqueFecha="
                + embarqueFecha
                + "]";
    }
}
