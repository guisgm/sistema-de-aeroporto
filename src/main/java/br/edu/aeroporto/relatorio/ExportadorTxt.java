package br.edu.aeroporto.relatorio;

import br.edu.aeroporto.dto.VooResumo;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExportadorTxt implements ExportadorVoos {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm XXX");

    @Override
    public String extensao() { return "txt"; }

    @Override
    public void escrever(BufferedWriter arquivo, List<VooResumo> voos) throws IOException {
        arquivo.write("RELATÓRIO DE VOOS — página consultada");
        arquivo.newLine();
        for (VooResumo voo : voos) {
            arquivo.write("%d | %s%s | %s → %s | %s | %s%n".formatted(voo.id(), voo.companhia(), voo.numero(),
                    voo.origem(), voo.destino(), DATA.format(voo.partida().atZoneSameInstant(voo.fusoOrigem())), voo.situacao()));
        }
        arquivo.write("Total de registros nesta página: " + voos.size());
        arquivo.newLine();
    }
}
