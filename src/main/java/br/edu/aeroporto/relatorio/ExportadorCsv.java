package br.edu.aeroporto.relatorio;

import br.edu.aeroporto.dto.VooResumo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public final class ExportadorCsv implements ExportadorVoos {
    @Override
    public String extensao() {
        return "csv";
    }

    @Override
    public void escrever(BufferedWriter arquivo, List<VooResumo> voos) throws IOException {
        arquivo.write("id;voo;origem;destino;partida_local;chegada_local;situacao");
        arquivo.newLine();
        for (VooResumo voo : voos) {
            arquivo.write(
                    String.join(
                            ";",
                            Long.toString(voo.id()),
                            campo(voo.companhia() + voo.numero()),
                            campo(voo.origem()),
                            campo(voo.destino()),
                            campo(
                                    voo.partida()
                                            .atZoneSameInstant(voo.fusoOrigem())
                                            .toOffsetDateTime()
                                            .toString()),
                            campo(
                                    voo.chegada()
                                            .atZoneSameInstant(voo.fusoDestino())
                                            .toOffsetDateTime()
                                            .toString()),
                            campo(voo.situacao().name())));
            arquivo.newLine();
        }
    }

    private static String campo(String texto) {
        // Neutraliza fórmulas quando o CSV é aberto em uma planilha.
        String seguro = texto.matches("^[=+@-].*") ? "'" + texto : texto;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }
}
