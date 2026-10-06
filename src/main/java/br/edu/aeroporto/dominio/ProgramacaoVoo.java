package br.edu.aeroporto.dominio;

import java.time.Instant;

public record ProgramacaoVoo(long companhia,long rota,long aeronave,String numero,Instant partida,Instant chegada,
                             Instant checkinAbre,Instant checkinFecha,Instant embarqueAbre,Instant embarqueFecha) {
    public ProgramacaoVoo {
        Dados.exigir(companhia>0 && rota>0 && aeronave>0,"Cadastros do voo invalidos.");
        numero=Validacao.texto(numero,"Numero do voo",8);
        Dados.exigir(numero.matches("[A-Z0-9]{1,8}"),"Numero de voo invalido.");
        validarHorarios(partida,chegada,checkinAbre,checkinFecha,embarqueAbre,embarqueFecha);
        partida=partida.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        chegada=chegada.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        checkinAbre=checkinAbre.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        checkinFecha=checkinFecha.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        embarqueAbre=embarqueAbre.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        embarqueFecha=embarqueFecha.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        validarHorarios(partida,chegada,checkinAbre,checkinFecha,embarqueAbre,embarqueFecha);
    }

    public static void validarHorarios(Instant partida,Instant chegada,Instant ca,Instant cf,Instant ea,Instant ef) {
        Dados.exigir(partida!=null && chegada!=null && ca!=null && cf!=null && ea!=null && ef!=null,"Informe todos os horarios.");
        Dados.exigir(chegada.isAfter(partida) && ca.isBefore(cf) && !cf.isAfter(ef) && ea.isBefore(ef) && !ef.isAfter(partida),"Janelas e horarios do voo invalidos.");
    }
}
