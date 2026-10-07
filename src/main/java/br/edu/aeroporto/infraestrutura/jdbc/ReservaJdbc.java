package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.*;

public final class ReservaJdbc {
    public long reservar(
            Connection c,
            long usuario,
            long comprador,
            List<PedidoTrecho> trechos,
            Instant agora,
            int minutos)
            throws SQLException {
        List<Long> voos = new ArrayList<>();
        for (PedidoTrecho trecho : trechos) voos.add(trecho.voo());
        BloqueiosJdbc.voos(c, voos);
        Sql.registro(c, "SELECT id FROM pessoa WHERE id=? AND ativo FOR SHARE", comprador);
        ArrayList<Map<String, Object>> tarifas = new ArrayList<Map<String, Object>>();
        ArrayList<Long> assentos = new ArrayList<Long>();
        HashSet<Long> selecionados = new HashSet<Long>();
        HashMap<Long, Map<String, Object>> percurso = new HashMap<Long, Map<String, Object>>();
        HashSet<String> ordens = new HashSet<String>();
        Instant prazo = agora.plus(Duration.ofMinutes(minutos));
        List<PedidoTrecho> ordenados = ordenarTrechos(trechos);
        for (PedidoTrecho pedido : ordenados) {
            Dados.exigir(
                    ordens.add(pedido.passageiro() + ":" + pedido.ordem()),
                    "Ordem de trecho repetida.");
            Map<String, Object> voo =
                    Sql.registro(
                            c,
                            "SELECT v.*,r.origem_id,r.destino_id FROM voo v JOIN rota r ON r.id=v.rota_id WHERE v.id=?",
                            pedido.voo());
            Dados.exigir(
                    Set.of("PROGRAMADO", "CHECKIN_ABERTO").contains(voo.get("situacao")),
                    "Voo nao esta disponivel para venda.");
            Instant limite = janela(voo, "checkin_fecha");
            Dados.exigir(agora.isBefore(limite), "Venda encerrada para este voo.");
            if (limite.isBefore(prazo)) prazo = limite;
            Map<String, Object> anterior = percurso.put(pedido.passageiro(), voo);
            if (anterior != null) {
                Dados.exigir(
                        Dados.id(anterior, "destino_id") == Dados.id(voo, "origem_id"),
                        "Conexao sem continuidade.");
                Dados.exigir(
                        !PlanejamentoJdbc.efetivo(anterior, "chegada")
                                .plus(Duration.ofMinutes(60))
                                .isAfter(PlanejamentoJdbc.efetivo(voo, "partida")),
                        "Conexao exige ao menos 60 minutos.");
            }
            Map<String, Object> pessoa =
                    Sql.registro(
                            c,
                            "SELECT p.*,pa.observacoes_assistencia FROM pessoa p JOIN passageiro pa ON pa.pessoa_id=p.id WHERE p.id=? AND p.ativo AND pa.ativo FOR SHARE OF p,pa",
                            pedido.passageiro());
            Map<String, Object> tarifa =
                    Sql.registro(
                            c,
                            "SELECT * FROM tarifa_voo WHERE id=? AND voo_id=? AND ativa",
                            pedido.tarifa(),
                            pedido.voo());
            Dados.exigir(
                    pedido.desconto()
                                    .compareTo(
                                            Dados.dinheiro(tarifa, "valor_base")
                                                    .multiply(new BigDecimal("0.10")))
                            <= 0,
                    "Desconto maximo da simulacao: 10% do valor base.");
            List<Map<String, Object>> livres =
                    Sql.listar(
                            c,
                            """
                    SELECT i.*,a.saida_emergencia FROM inventario_assento_voo i JOIN assento_aeronave a ON a.id=i.assento_id
                    WHERE i.voo_id=? AND i.classe=? AND NOT i.bloqueado AND (?::varchar IS NULL OR i.codigo=?)
                      AND NOT EXISTS (SELECT 1 FROM ocupacao_assento o WHERE o.inventario_id=i.id AND o.liberada_em IS NULL)
                    ORDER BY a.fila,a.coluna
                    """,
                            pedido.voo(),
                            tarifa.get("classe"),
                            pedido.assento(),
                            pedido.assento());
            Long escolhido = null;
            for (Map<String, Object> assento : livres) {
                long id = Dados.id(assento, "id");
                if (selecionados.contains(id)) continue;
                if (Boolean.TRUE.equals(assento.get("saida_emergencia"))) {
                    int idade =
                            Period.between(
                                            ((java.sql.Date) pessoa.get("nascimento"))
                                                    .toLocalDate(),
                                            PlanejamentoJdbc.efetivo(voo, "partida")
                                                    .atZone(ZoneOffset.UTC)
                                                    .toLocalDate())
                                    .getYears();
                    if (idade < 16
                            || idade > 65
                            || (pessoa.get("observacoes_assistencia") != null
                                    && !pessoa.get("observacoes_assistencia").toString().isBlank()))
                        continue;
                }
                escolhido = id;
                break;
            }
            Dados.exigir(escolhido != null, "Assento indisponivel, bloqueado ou incompativel.");
            selecionados.add(escolhido);
            assentos.add(escolhido);
            tarifas.add(tarifa);
        }
        // Ordena uma copia: a lista original acompanha a ordem dos itens e tarifas.
        List<Long> assentosOrdenados = new ArrayList<>(assentos);
        Collections.sort(assentosOrdenados);
        for (long assento : assentosOrdenados)
            Sql.registro(c, "SELECT id FROM inventario_assento_voo WHERE id=? FOR UPDATE", assento);
        long reserva =
                Sql.inserir(
                        c,
                        "INSERT INTO reserva(localizador,comprador_id,usuario_id,criada_em,expira_em) VALUES (?,?,?,?,?) RETURNING id",
                        Dados.codigo("R", 12),
                        comprador,
                        usuario,
                        agora,
                        prazo);
        for (int indice = 0; indice < ordenados.size(); indice++) {
            PedidoTrecho pedido = ordenados.get(indice);
            Map<String, Object> tarifa = tarifas.get(indice);
            long item = novoItem(c, reserva, pedido, tarifa);
            Sql.executar(
                    c,
                    "INSERT INTO ocupacao_assento(item_id,voo_id,inventario_id,criada_em) VALUES (?,?,?,?)",
                    item,
                    pedido.voo(),
                    assentos.get(indice),
                    agora);
        }
        return reserva;
    }

    private List<PedidoTrecho> ordenarTrechos(List<PedidoTrecho> trechos) {
        List<PedidoTrecho> ordenados = new ArrayList<>(trechos);
        // Ordenacao por insercao: primeiro passageiro, depois ordem do trecho.
        for (int i = 1; i < ordenados.size(); i++) {
            PedidoTrecho atual = ordenados.get(i);
            int posicao = i;
            while (posicao > 0) {
                PedidoTrecho anterior = ordenados.get(posicao - 1);
                boolean vemDepois =
                        anterior.passageiro() > atual.passageiro()
                                || (anterior.passageiro() == atual.passageiro()
                                        && anterior.ordem() > atual.ordem());
                if (!vemDepois) break;
                ordenados.set(posicao, anterior);
                posicao--;
            }
            ordenados.set(posicao, atual);
        }
        return ordenados;
    }

    public long novoItem(Connection c, long reserva, PedidoTrecho p, Map<String, Object> tarifa)
            throws SQLException {
        return Sql.inserir(
                c,
                """
                INSERT INTO item_reserva(reserva_id,passageiro_id,voo_id,tarifa_id,ordem_trecho,valor_base,taxa_embarque,desconto,franquia_bagagem_kg,limite_pecas,permite_cancelar,multa_cancelamento)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id
                """,
                reserva,
                p.passageiro(),
                p.voo(),
                p.tarifa(),
                p.ordem(),
                tarifa.get("valor_base"),
                tarifa.get("taxa_embarque"),
                p.desconto(),
                tarifa.get("franquia_bagagem_kg"),
                tarifa.get("limite_pecas"),
                tarifa.get("permite_cancelar"),
                tarifa.get("multa_cancelamento"));
    }

    public static Instant janela(Map<String, Object> voo, String campo) {
        Duration atraso =
                Duration.between(
                        Dados.instante(voo, "partida_prevista"),
                        PlanejamentoJdbc.efetivo(voo, "partida"));
        return Dados.instante(voo, campo).plus(atraso);
    }

    public void recalcular(Connection c, long reserva) throws SQLException {
        List<String> estados =
                Sql.listarTextos(
                        c, "SELECT situacao FROM item_reserva WHERE reserva_id=?", reserva);
        String estado;
        boolean todosExpirados = true;
        boolean todosCanceladosOuExpirados = true;
        boolean algumCanceladoOuExpirado = false;
        boolean todosFinalizados = true;
        boolean algumPendente = false;
        for (String situacao : estados) {
            boolean expirado = situacao.equals("EXPIRADO");
            boolean canceladoOuExpirado = situacao.equals("CANCELADO") || expirado;
            todosExpirados = todosExpirados && expirado;
            todosCanceladosOuExpirados = todosCanceladosOuExpirados && canceladoOuExpirado;
            algumCanceladoOuExpirado = algumCanceladoOuExpirado || canceladoOuExpirado;
            todosFinalizados =
                    todosFinalizados
                            && (situacao.equals("UTILIZADO") || situacao.equals("NAO_COMPARECEU"));
            algumPendente = algumPendente || situacao.equals("PENDENTE");
        }
        if (todosExpirados) estado = "EXPIRADA";
        else if (todosCanceladosOuExpirados) estado = "CANCELADA";
        else if (algumCanceladoOuExpirado) estado = "PARCIAL_CANCELADA";
        else if (todosFinalizados) estado = "FINALIZADA";
        else if (algumPendente) estado = "PENDENTE";
        else estado = "CONFIRMADA";
        Sql.executar(c, "UPDATE reserva SET situacao=? WHERE id=?", estado, reserva);
    }

    public BigDecimal cancelarItem(
            Connection c,
            Map<String, Object> item,
            Instant agora,
            String motivo,
            boolean integral,
            long usuario)
            throws SQLException {
        String estado = Dados.texto(item, "situacao");
        if (Set.of("CANCELADO", "EXPIRADO").contains(estado)) return BigDecimal.ZERO;
        boolean embarcadoAntesDaPartida =
                integral
                        && estado.equals("UTILIZADO")
                        && Set.of("PROGRAMADO", "CHECKIN_ABERTO", "EMBARQUE")
                                .contains(
                                        Sql.registro(
                                                        c,
                                                        "SELECT situacao FROM voo WHERE id=?",
                                                        Dados.id(item, "voo_id"))
                                                .get("situacao"));
        Dados.exigir(
                Set.of("PENDENTE", "CONFIRMADO").contains(estado) || embarcadoAntesDaPartida,
                "Item utilizado ou ausente nao pode ser cancelado.");
        if (!integral && estado.equals("CONFIRMADO"))
            Dados.exigir(
                    Boolean.TRUE.equals(item.get("permite_cancelar")),
                    "Tarifa nao permite cancelamento.");
        long id = Dados.id(item, "id");
        BigDecimal total =
                Dados.dinheiro(item, "valor_base")
                        .add(Dados.dinheiro(item, "taxa_embarque"))
                        .subtract(Dados.dinheiro(item, "desconto"));
        BigDecimal devolucao =
                integral || estado.equals("PENDENTE")
                        ? total
                        : total.subtract(Dados.dinheiro(item, "multa_cancelamento"))
                                .max(BigDecimal.ZERO);
        BigDecimal bagagens =
                Sql.unicoValor(
                                c,
                                "SELECT COALESCE(SUM(b.taxa_excesso),0) FROM bagagem b JOIN check_in ci ON ci.id=b.checkin_id WHERE ci.item_id=? AND b.situacao<>'RETIRADA'",
                                id)
                        .orElse(BigDecimal.ZERO);
        Sql.executar(
                c,
                "UPDATE item_reserva SET situacao='CANCELADO',cancelado_em=?,motivo_cancelamento=? WHERE id=?",
                agora,
                motivo,
                id);
        invalidar(c, id, agora, usuario);
        return devolucao.add(bagagens);
    }

    public void invalidar(Connection c, long item, Instant agora, Long usuario)
            throws SQLException {
        Sql.executar(
                c,
                "UPDATE ocupacao_assento SET liberada_em=? WHERE item_id=? AND liberada_em IS NULL",
                agora,
                item);
        Sql.executar(
                c,
                "UPDATE bilhete SET situacao='CANCELADO' WHERE item_id=? AND situacao IN ('EMITIDO','UTILIZADO')",
                item);
        Sql.executar(
                c,
                "UPDATE check_in SET cancelado_em=? WHERE item_id=? AND cancelado_em IS NULL",
                agora,
                item);
        for (Map<String, Object> b :
                Sql.listar(
                        c,
                        "SELECT b.id,r.origem_id FROM bagagem b JOIN check_in ci ON ci.id=b.checkin_id JOIN voo v ON v.id=ci.voo_id JOIN rota r ON r.id=v.rota_id WHERE ci.item_id=? AND b.situacao NOT IN ('RETIRADA','ENTREGUE') ORDER BY b.id FOR UPDATE OF b",
                        item)) {
            Sql.executar(c, "UPDATE bagagem SET situacao='RETIRADA' WHERE id=?", Dados.id(b, "id"));
            if (usuario != null)
                Sql.executar(
                        c,
                        "INSERT INTO evento_bagagem(bagagem_id,aeroporto_id,usuario_id,situacao,ocorrido_em,observacao) VALUES (?,?,?,'RETIRADA',?,'Cancelamento')",
                        Dados.id(b, "id"),
                        Dados.id(b, "origem_id"),
                        usuario,
                        agora);
        }
    }

    public void solicitarDevolucao(
            Connection c, long reserva, BigDecimal valor, String chave, String motivo)
            throws SQLException {
        FinanceiroJdbc financeiro = new FinanceiroJdbc();
        BigDecimal livre =
                financeiro
                        .recebido(c, reserva)
                        .subtract(financeiro.devido(c, reserva))
                        .max(BigDecimal.ZERO);
        BigDecimal restante = valor.min(livre);
        for (Map<String, Object> p :
                Sql.listar(
                        c,
                        """
                SELECT p.*,p.valor-COALESCE((SELECT SUM(r.valor) FROM reembolso r WHERE r.pagamento_id=p.id AND r.situacao IN ('SOLICITADO','PROCESSADO')),0) AS disponivel
                FROM pagamento p WHERE p.reserva_id=? AND p.situacao='APROVADO' ORDER BY p.id FOR UPDATE
                """,
                        reserva)) {
            BigDecimal parte = restante.min(Dados.dinheiro(p, "disponivel"));
            if (parte.signum() <= 0) continue;
            Sql.executar(
                    c,
                    "INSERT INTO reembolso(pagamento_id,reserva_id,valor,motivo,chave_idempotencia) VALUES (?,?,?,?,?) ON CONFLICT (chave_idempotencia) DO NOTHING",
                    Dados.id(p, "id"),
                    reserva,
                    parte,
                    motivo,
                    chave + ":" + Dados.id(p, "id"));
            restante = restante.subtract(parte);
            if (restante.signum() == 0) break;
        }
    }

    public BigDecimal limiteDevolucao(Connection c, long reserva) throws SQLException {
        BigDecimal direito =
                Sql.unicoValor(
                                c,
                                """
                SELECT COALESCE(SUM(CASE WHEN i.situacao='EXPIRADO' THEN i.valor_base+i.taxa_embarque-i.desconto
                    WHEN i.situacao='CANCELADO' THEN GREATEST(0,i.valor_base+i.taxa_embarque-i.desconto-
                        CASE WHEN v.situacao='CANCELADO' OR b.id IS NULL THEN 0 ELSE i.multa_cancelamento END)
                    ELSE 0 END),0)
                FROM item_reserva i JOIN voo v ON v.id=i.voo_id LEFT JOIN bilhete b ON b.item_id=i.id WHERE i.reserva_id=?
                """,
                                reserva)
                        .orElse(BigDecimal.ZERO);
        BigDecimal taxas =
                Sql.unicoValor(
                                c,
                                "SELECT COALESCE(SUM(b.taxa_excesso),0) FROM bagagem b JOIN check_in ci ON ci.id=b.checkin_id JOIN item_reserva i ON i.id=ci.item_id WHERE i.reserva_id=? AND b.situacao='RETIRADA'",
                                reserva)
                        .orElse(BigDecimal.ZERO);
        BigDecimal comprometido =
                Sql.unicoValor(
                                c,
                                "SELECT COALESCE(SUM(valor),0) FROM reembolso WHERE reserva_id=? AND situacao IN ('SOLICITADO','PROCESSADO')",
                                reserva)
                        .orElse(BigDecimal.ZERO);
        return direito.add(taxas).subtract(comprometido).max(BigDecimal.ZERO);
    }

    public int expirar(Connection c, Instant agora) throws SQLException {
        List<Long> ids =
                Sql.listarNumeros(
                        c,
                        "SELECT id FROM reserva WHERE expira_em<=? AND EXISTS (SELECT 1 FROM item_reserva i WHERE i.reserva_id=reserva.id AND i.situacao='PENDENTE') ORDER BY id",
                        agora);
        ArrayList<Long> voos = new ArrayList<Long>();
        for (long id : ids)
            voos.addAll(
                    Sql.listarNumeros(c, "SELECT voo_id FROM item_reserva WHERE reserva_id=?", id));
        BloqueiosJdbc.voos(c, voos);
        int quantidade = 0;
        for (long id : ids) {
            Map<String, Object> reserva = BloqueiosJdbc.reserva(c, id);
            if (Dados.instante(reserva, "expira_em").isAfter(agora)) continue;
            List<Map<String, Object>> itens =
                    Sql.listar(
                            c,
                            "SELECT * FROM item_reserva WHERE reserva_id=? AND situacao='PENDENTE' ORDER BY id",
                            id);
            if (itens.isEmpty()) continue;
            for (Map<String, Object> item : itens) {
                Sql.executar(
                        c,
                        "UPDATE item_reserva SET situacao='EXPIRADO' WHERE id=?",
                        Dados.id(item, "id"));
                invalidar(c, Dados.id(item, "id"), agora, null);
            }
            BigDecimal aprovado =
                    Sql.unicoValor(
                                    c,
                                    "SELECT COALESCE(SUM(valor),0) FROM pagamento WHERE reserva_id=? AND situacao='APROVADO'",
                                    id)
                            .orElse(BigDecimal.ZERO);
            solicitarDevolucao(c, id, aprovado, "exp:" + id, "Expiracao de reserva pendente");
            recalcular(c, id);
            Sql.executar(
                    c,
                    "INSERT INTO evento_auditoria(acao,entidade,registro_id) VALUES ('EXPIRAR_RESERVA','reserva',?)",
                    id);
            quantidade++;
        }
        return quantidade;
    }
}
