package br.edu.aeroporto.servico;

import br.edu.aeroporto.dto.VooResumo;
import br.edu.aeroporto.relatorio.ExportadorVoos;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class RelatorioServico {
    public Path exportar(List<VooResumo> voos, ExportadorVoos exportador) throws IOException {
        Path pasta = Path.of("relatorios").toAbsolutePath().normalize();
        Files.createDirectories(pasta);
        Path temporario = Files.createTempFile(pasta, "voos-", ".tmp");
        Path destino = pasta.resolve("voos-" + UUID.randomUUID() + "." + exportador.extensao());
        try {
            try (BufferedWriter arquivo = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                exportador.escrever(arquivo, voos);
            }
            return Files.move(temporario, destino);
        } catch (IOException | RuntimeException erro) {
            try { Files.deleteIfExists(temporario); } catch (IOException limpeza) { erro.addSuppressed(limpeza); }
            throw erro;
        }
    }
}
