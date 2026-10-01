package br.edu.aeroporto.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Map;
import java.util.Properties;

public record Configuracao(String url, String usuario, String senha, ZoneId fuso) {
    public static Configuracao carregar() throws IOException {
        Properties propriedades = new Properties();
        Path arquivo = Path.of("config", "application.properties");
        if (Files.exists(arquivo)) {
            if (!Files.isRegularFile(arquivo)) throw new IOException("A configuração precisa ser um arquivo.");
            try (Reader reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
                propriedades.load(reader);
            }
        }
        Map<String, String> ambiente = System.getenv();
        String url = valor(propriedades, ambiente, "db.url", "AEROPORTO_DB_URL", "jdbc:postgresql://localhost:5432/sistema_aeroporto");
        String usuario = valor(propriedades, ambiente, "db.usuario", "AEROPORTO_DB_USUARIO", "postgres");
        String senha = valor(propriedades, ambiente, "db.senha", "AEROPORTO_DB_SENHA", "");
        ZoneId fuso = ZoneId.of(valor(propriedades, ambiente, "app.fuso", "AEROPORTO_FUSO", "America/Sao_Paulo"));
        if (!url.startsWith("jdbc:postgresql:") || usuario.isBlank() || senha.isBlank()) {
            throw new IllegalArgumentException("Configure db.url, db.usuario e db.senha em config/application.properties ou nas variáveis de ambiente.");
        }
        return new Configuracao(url, usuario, senha, fuso);
    }

    private static String valor(Properties propriedades, Map<String, String> ambiente,
                                String chave, String variavel, String padrao) {
        return ambiente.getOrDefault(variavel, propriedades.getProperty(chave, padrao));
    }

    @Override
    public String toString() {
        return "Configuracao[fuso=" + fuso + "]";
    }
}
