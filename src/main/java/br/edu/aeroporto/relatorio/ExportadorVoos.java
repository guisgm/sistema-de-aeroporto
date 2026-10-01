package br.edu.aeroporto.relatorio;

import br.edu.aeroporto.dto.VooResumo;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public interface ExportadorVoos {
    String extensao();
    void escrever(BufferedWriter arquivo, List<VooResumo> voos) throws IOException;
}
