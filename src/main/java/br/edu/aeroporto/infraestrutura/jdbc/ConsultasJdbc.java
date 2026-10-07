package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.SituacaoReserva;
import br.edu.aeroporto.dominio.SituacaoVoo;
import br.edu.aeroporto.dto.AssentoResumo;
import br.edu.aeroporto.dto.ReservaResumo;
import br.edu.aeroporto.dto.VooResumo;
import br.edu.aeroporto.repositorio.ConsultasRepositorio;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public final class ConsultasJdbc implements ConsultasRepositorio {
    @Override
    public List<VooResumo> voos(Connection conexao, Instant inicio, Instant fim, int pagina)
            throws SQLException {
        return Sql.listar(
                conexao,
                """
                SELECT v.id, c.codigo_icao AS companhia, v.numero,
                       ao.codigo_icao AS origem, ad.codigo_icao AS destino,
                       ao.fuso_horario AS fuso_origem, ad.fuso_horario AS fuso_destino,
                       v.partida_prevista, v.chegada_prevista, v.partida_estimada, v.partida_real, v.situacao
                FROM voo v JOIN companhia_aerea c ON c.id = v.companhia_id
                JOIN rota r ON r.id = v.rota_id JOIN aeroporto ao ON ao.id = r.origem_id
                JOIN aeroporto ad ON ad.id = r.destino_id
                WHERE v.partida_prevista >= ? AND v.partida_prevista < ?
                ORDER BY v.partida_prevista, v.id LIMIT 20 OFFSET ?
                """,
                linha ->
                        new VooResumo(
                                linha.getLong("id"),
                                linha.getString("companhia"),
                                linha.getString("numero"),
                                linha.getString("origem"),
                                linha.getString("destino"),
                                ZoneId.of(linha.getString("fuso_origem")),
                                ZoneId.of(linha.getString("fuso_destino")),
                                linha.getObject("partida_prevista", OffsetDateTime.class),
                                linha.getObject("chegada_prevista", OffsetDateTime.class),
                                linha.getObject("partida_estimada", OffsetDateTime.class),
                                linha.getObject("partida_real", OffsetDateTime.class),
                                SituacaoVoo.valueOf(linha.getString("situacao"))),
                inicio,
                fim,
                (pagina - 1) * 20);
    }

    @Override
    public List<AssentoResumo> assentos(Connection conexao, long vooId) throws SQLException {
        return Sql.listar(
                conexao,
                """
                SELECT i.codigo, i.classe, i.bloqueado,
                       EXISTS (SELECT 1 FROM ocupacao_assento o
                               WHERE o.inventario_id = i.id AND o.liberada_em IS NULL) AS ocupado
                FROM inventario_assento_voo i JOIN assento_aeronave a ON a.id = i.assento_id
                WHERE i.voo_id = ? ORDER BY a.fila, a.coluna
                """,
                linha ->
                        new AssentoResumo(
                                linha.getString("codigo"),
                                linha.getString("classe"),
                                linha.getBoolean("bloqueado"),
                                linha.getBoolean("ocupado")),
                vooId);
    }

    @Override
    public Optional<ReservaResumo> reserva(Connection conexao, String localizador)
            throws SQLException {
        return Sql.unico(
                conexao,
                "SELECT id, localizador, situacao, expira_em FROM reserva WHERE localizador = ?",
                linha -> {
                    long reservaId = linha.getLong("id");
                    List<ReservaResumo.Trecho> trechos =
                            Sql.listar(
                                    conexao,
                                    """
                    SELECT i.id, p.nome, c.codigo_icao || v.numero AS voo, i.ordem_trecho,
                           i.situacao, a.codigo AS assento, i.valor_base + i.taxa_embarque - i.desconto AS valor
                    FROM item_reserva i JOIN pessoa p ON p.id = i.passageiro_id
                    JOIN voo v ON v.id = i.voo_id JOIN companhia_aerea c ON c.id = v.companhia_id
                    LEFT JOIN ocupacao_assento o ON o.item_id = i.id AND o.liberada_em IS NULL
                    LEFT JOIN inventario_assento_voo a ON a.id = o.inventario_id
                    WHERE i.reserva_id = ? ORDER BY p.nome, i.passageiro_id, i.ordem_trecho
                    """,
                                    item ->
                                            new ReservaResumo.Trecho(
                                                    item.getLong("id"),
                                                    item.getString("nome"),
                                                    item.getString("voo"),
                                                    item.getInt("ordem_trecho"),
                                                    item.getString("situacao"),
                                                    item.getString("assento"),
                                                    item.getBigDecimal("valor")),
                                    reservaId);
                    return new ReservaResumo(
                            reservaId,
                            linha.getString("localizador"),
                            SituacaoReserva.valueOf(linha.getString("situacao")),
                            linha.getObject("expira_em", OffsetDateTime.class),
                            trechos);
                },
                localizador);
    }
}
