package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.Dados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class FinanceiroJdbc {
    public BigDecimal devido(Connection c, long reserva) throws SQLException {
        BigDecimal passagens =
                Sql.unicoValor(
                                c,
                                "SELECT COALESCE(SUM(valor_base+taxa_embarque-desconto),0) FROM item_reserva WHERE reserva_id=? AND situacao NOT IN ('CANCELADO','EXPIRADO')",
                                reserva)
                        .orElse(BigDecimal.ZERO);
        BigDecimal bagagens =
                Sql.unicoValor(
                                c,
                                """
                SELECT COALESCE(SUM(b.taxa_excesso),0) FROM bagagem b JOIN check_in ci ON ci.id=b.checkin_id
                JOIN item_reserva i ON i.id=ci.item_id WHERE i.reserva_id=? AND b.situacao<>'RETIRADA' AND i.situacao NOT IN ('CANCELADO','EXPIRADO')
                """,
                                reserva)
                        .orElse(BigDecimal.ZERO);
        return passagens.add(bagagens);
    }

    public BigDecimal recebido(Connection c, long reserva) throws SQLException {
        BigDecimal pagos =
                Sql.unicoValor(
                                c,
                                "SELECT COALESCE(SUM(valor),0) FROM pagamento WHERE reserva_id=? AND situacao='APROVADO'",
                                reserva)
                        .orElse(BigDecimal.ZERO);
        BigDecimal devolvidos =
                Sql.unicoValor(
                                c,
                                "SELECT COALESCE(SUM(valor),0) FROM reembolso WHERE reserva_id=? AND situacao IN ('SOLICITADO','PROCESSADO')",
                                reserva)
                        .orElse(BigDecimal.ZERO);
        return pagos.subtract(devolvidos);
    }

    public void confirmar(Connection c, long reserva, Instant agora) throws SQLException {
        Map<String, Object> r = Sql.registro(c, "SELECT * FROM reserva WHERE id=?", reserva);
        List<Map<String, Object>> pendentes =
                Sql.listar(
                        c,
                        "SELECT * FROM item_reserva WHERE reserva_id=? AND situacao='PENDENTE' ORDER BY id",
                        reserva);
        Dados.exigir(!pendentes.isEmpty(), "Nao ha itens pendentes para confirmar.");
        Dados.exigir(Dados.instante(r, "expira_em").isAfter(agora), "Reserva vencida.");
        Dados.exigir(
                recebido(c, reserva).compareTo(devido(c, reserva)) >= 0,
                "Pagamento insuficiente para confirmar todos os itens.");
        for (Map<String, Object> i : pendentes) {
            long id = Dados.id(i, "id");
            Map<String, Object> v =
                    Sql.registro(c, "SELECT * FROM voo WHERE id=?", Dados.id(i, "voo_id"));
            Dados.exigir(
                    java.util.Set.of("PROGRAMADO", "CHECKIN_ABERTO").contains(v.get("situacao"))
                            && agora.isBefore(ReservaJdbc.janela(v, "checkin_fecha")),
                    "Voo indisponivel para confirmacao.");
            Sql.registro(
                    c,
                    "SELECT id FROM ocupacao_assento WHERE item_id=? AND liberada_em IS NULL",
                    id);
            Sql.executar(c, "UPDATE item_reserva SET situacao='CONFIRMADO' WHERE id=?", id);
            Sql.executar(
                    c,
                    "INSERT INTO bilhete(item_id,numero,emitido_em) VALUES (?,?,?)",
                    id,
                    Dados.codigo("B", 20),
                    agora);
        }
        new ReservaJdbc().recalcular(c, reserva);
    }
}
