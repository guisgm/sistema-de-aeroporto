package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.dto.TipoRelatorio;
import br.edu.aeroporto.infraestrutura.jdbc.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class RelatoriosCompletosServico {
    private final BancoDados banco;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;

    public RelatoriosCompletosServico(
            BancoDados banco, AutorizacaoJdbc autorizacao, AuditoriaJdbc auditoria) {
        this.banco = banco;
        this.autorizacao = autorizacao;
        this.auditoria = auditoria;
    }

    private void permitir(Connection c, Sessao s, TipoRelatorio tipo) throws SQLException {
        if (tipo == TipoRelatorio.AUDITORIA) autorizacao.exigirAdministrador(c, s);
        else if (tipo.restrito()) autorizacao.exigirAtendimento(c, s);
        else autorizacao.exigirConsulta(c, s);
    }

    public List<Map<String, Object>> consultar(Sessao s, TipoRelatorio tipo, int pagina) {
        Dados.exigir(pagina >= 1 && pagina <= 100000, "Pagina invalida.");
        return banco.consultar(
                c -> {
                    permitir(c, s, tipo);
                    return Sql.listar(
                            c,
                            "SELECT * FROM ("
                                    + tipo.sql().strip()
                                    + ") relatorio LIMIT 20 OFFSET ?",
                            (pagina - 1) * 20);
                });
    }

    public List<Map<String, Object>> painel(
            Sessao s, long aeroporto, LocalDate data, boolean partidas) {
        return banco.consultar(
                c -> {
                    autorizacao.exigirConsulta(c, s);
                    Map<String, Object> a =
                            Sql.registro(
                                    c, "SELECT fuso_horario FROM aeroporto WHERE id=?", aeroporto);
                    ZoneId fuso = ZoneId.of(a.get("fuso_horario").toString());
                    Instant inicio = data.atStartOfDay(fuso).toInstant(),
                            fim = data.plusDays(1).atStartOfDay(fuso).toInstant();
                    String campo = partidas ? "partida_prevista" : "chegada_prevista",
                            lado = partidas ? "origem_id" : "destino_id";
                    return Sql.listar(
                            c,
                            "SELECT v.*,o.codigo_icao AS origem,d.codigo_icao AS destino FROM voo v JOIN rota r ON r.id=v.rota_id JOIN aeroporto o ON o.id=r.origem_id JOIN aeroporto d ON d.id=r.destino_id WHERE r."
                                    + lado
                                    + "=? AND v."
                                    + campo
                                    + ">=? AND v."
                                    + campo
                                    + "<? ORDER BY v."
                                    + campo
                                    + ",v.id",
                            aeroporto,
                            inicio,
                            fim);
                });
    }

    public MapaAssentos mapa(Sessao s, long voo) {
        return banco.consultar(
                c -> {
                    autorizacao.exigirConsulta(c, s);
                    return new MapaAssentos(
                            Sql.listar(
                                    c,
                                    """
                SELECT i.codigo,a.fila,a.coluna,i.bloqueado,EXISTS (SELECT 1 FROM ocupacao_assento o WHERE o.inventario_id=i.id AND o.liberada_em IS NULL) AS ocupado
                FROM inventario_assento_voo i JOIN assento_aeronave a ON a.id=i.assento_id WHERE i.voo_id=? ORDER BY a.fila,a.coluna
                """,
                                    voo));
                });
    }

    public List<Map<String, Object>> bagagem(Sessao s, String etiqueta) {
        String tag = Validacao.texto(etiqueta, "Etiqueta", 30);
        return banco.consultar(
                c -> {
                    autorizacao.exigirAtendimento(c, s);
                    return Sql.listar(
                            c,
                            "SELECT b.etiqueta,b.situacao AS atual,e.aeroporto_id,e.situacao,e.ocorrido_em,e.observacao FROM bagagem b JOIN evento_bagagem e ON e.bagagem_id=b.id WHERE b.etiqueta=? ORDER BY e.ocorrido_em,e.id",
                            tag);
                });
    }

    public Map<String, java.math.BigDecimal> resumoAtrasos(Sessao s) {
        return banco.consultar(
                c -> {
                    autorizacao.exigirConsulta(c, s);
                    ArrayList<BigDecimal> valores =
                            new ArrayList<>(
                                    Sql.listarValores(
                                            c,
                                            "SELECT GREATEST(0,EXTRACT(EPOCH FROM(COALESCE(partida_real,partida_estimada,partida_prevista)-partida_prevista))/60) FROM voo WHERE situacao<>'CANCELADO'"));
                    if (valores.isEmpty()) return Map.of();
                    Collections.sort(valores);
                    BigDecimal maximo = Collections.max(valores);
                    BigDecimal minimo = Collections.min(valores);
                    BigDecimal mediana =
                            valores.size() % 2 == 1
                                    ? valores.get(valores.size() / 2)
                                    : valores.get(valores.size() / 2 - 1)
                                            .add(valores.get(valores.size() / 2))
                                            .divide(new java.math.BigDecimal("2"));
                    return Map.of("minimo", minimo, "maximo", maximo, "mediana", mediana);
                });
    }

    public Path exportar(Sessao s, TipoRelatorio tipo, boolean csv) throws IOException {
        Path pasta = Path.of("relatorios").toAbsolutePath().normalize();
        Files.createDirectories(pasta);
        Path temporario = Files.createTempFile(pasta, "relatorio-", ".tmp"),
                destino =
                        pasta.resolve(
                                tipo.name().toLowerCase(Locale.ROOT)
                                        + "-"
                                        + UUID.randomUUID()
                                        + (csv ? ".csv" : ".txt"));
        try {
            banco.transacao(
                    c -> {
                        permitir(c, s, tipo);
                        try (BufferedWriter writer =
                                        new BufferedWriter(
                                                new FileWriter(
                                                        temporario.toFile(),
                                                        StandardCharsets.UTF_8));
                                PreparedStatement statement = c.prepareStatement(tipo.sql())) {
                            statement.setFetchSize(500);
                            statement.setQueryTimeout(30);
                            try (ResultSet r = statement.executeQuery()) {
                                ResultSetMetaData meta = r.getMetaData();
                                String[] campos = new String[meta.getColumnCount()];
                                for (int i = 0; i < campos.length; i++)
                                    campos[i] = meta.getColumnLabel(i + 1);
                                escrever(writer, campos, csv);
                                while (r.next()) {
                                    for (int i = 0; i < campos.length; i++)
                                        campos[i] =
                                                r.getObject(i + 1) == null
                                                        ? ""
                                                        : r.getObject(i + 1).toString();
                                    escrever(writer, campos, csv);
                                }
                            }
                        } catch (IOException erro) {
                            throw new UncheckedIOException(erro);
                        }
                        auditoria.registrar(c, s.usuarioId(), "EXPORTAR_RELATORIO", tipo.name(), 0);
                        return null;
                    });
            return Files.move(temporario, destino);
        } catch (RuntimeException | IOException erro) {
            try {
                Files.deleteIfExists(temporario);
            } catch (IOException limpeza) {
                erro.addSuppressed(limpeza);
            }
            if (erro instanceof UncheckedIOException)
                throw ((UncheckedIOException) erro).getCause();
            throw erro;
        }
    }

    private void escrever(BufferedWriter writer, String[] campos, boolean csv) throws IOException {
        StringJoiner linha = new StringJoiner(csv ? ";" : " | ");
        for (String original : campos) {
            String valor = original.replace('\r', ' ').replace('\n', ' ');
            if (csv) {
                if (valor.stripLeading().matches("^[=+@-].*")) valor = "'" + valor;
                valor = "\"" + valor.replace("\"", "\"\"") + "\"";
            }
            linha.add(valor);
        }
        writer.write(linha.toString());
        writer.newLine();
    }
}
