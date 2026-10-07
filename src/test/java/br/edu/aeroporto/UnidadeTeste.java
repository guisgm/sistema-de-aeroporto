package br.edu.aeroporto;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.seguranca.Senhas;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class UnidadeTeste {
    public static void main(String[] args) {
        assertiva(Validacao.cpf("529.982.247-25").equals("52998224725"), "CPF valido normalizado");
        falha(() -> Validacao.cpf("11111111111"), "CPF repetido rejeitado");
        falha(
                () -> Validacao.nascimento(LocalDate.now().plusDays(1), LocalDate.now()),
                "Nascimento futuro rejeitado");
        falha(() -> Dados.valor(new BigDecimal("1.001"), false), "Dinheiro com 3 casas rejeitado");
        assertiva(
                !SituacaoVoo.CONCLUIDO.permiteTransicaoPara(SituacaoVoo.PROGRAMADO),
                "Voo encerrado nao reabre");
        List<Map<String, Object>> linhas =
                List.of(
                        Map.<String, Object>of(
                                "codigo",
                                "2C",
                                "fila",
                                2,
                                "coluna",
                                "C",
                                "ocupado",
                                true,
                                "bloqueado",
                                false),
                        Map.<String, Object>of(
                                "codigo",
                                "1A",
                                "fila",
                                1,
                                "coluna",
                                "A",
                                "ocupado",
                                false,
                                "bloqueado",
                                false));
        MapaAssentos mapa = new MapaAssentos(linhas);
        assertiva(
                Arrays.deepEquals(mapa.celulas(), new String[][] {{"1A:L", "--"}, {"--", "2C:O"}}),
                "Matriz irregular de assentos");
        String[][] copia = mapa.celulas();
        copia[0][0] = "alterado";
        assertiva(mapa.celulas()[0][0].equals("1A:L"), "Copia defensiva da matriz");
        assertiva(mapa.mesmoMapa(new MapaAssentos(linhas)), "Comparacao de mapas");
        falha(
                () ->
                        TipoCadastro.Campo.class
                                .cast(new TipoCadastro.Campo("ativo", "b", false))
                                .converter("qualquer"),
                "Booleano invalido rejeitado");
        String hash = Senhas.gerar("SenhaFicticia!2026".toCharArray());
        assertiva(Senhas.conferir("SenhaFicticia!2026".toCharArray(), hash), "Senha correta");
        assertiva(!Senhas.conferir("incorreta".toCharArray(), hash), "Senha incorreta rejeitada");
        assertiva(
                !Senhas.conferir("senha".toCharArray(), "hash-invalido"),
                "Hash invalido rejeitado");
        falha(() -> Validacao.cpf("123"), "CPF curto rejeitado sem acessar indice inexistente");
        falha(
                () -> Validacao.texto("Nome\nOutro", "Nome", 100),
                "Controle dentro do texto rejeitado");
        MapaAssentos vazio = new MapaAssentos(List.of());
        assertiva(
                vazio.filas().length == 0
                        && vazio.colunas().length == 0
                        && vazio.celulas().length == 0,
                "Mapa vazio permitido");
        MapaAssentos bloqueado =
                new MapaAssentos(
                        List.of(
                                Map.<String, Object>of(
                                        "codigo",
                                        "1A",
                                        "fila",
                                        1,
                                        "coluna",
                                        "A",
                                        "ocupado",
                                        true,
                                        "bloqueado",
                                        true)));
        assertiva(
                bloqueado.celulas()[0][0].equals("1A:B"), "Bloqueio tem prioridade sobre ocupacao");
        PedidoTrecho primeiro = new PedidoTrecho(1, 2, 3, 1, "1a", BigDecimal.ZERO);
        PedidoTrecho repetido = new PedidoTrecho(1, 2, 3, 1, "1A", new BigDecimal("0.00"));
        ArrayList<PedidoTrecho> rascunho = new ArrayList<>();
        rascunho.add(primeiro);
        assertiva(
                rascunho.contains(repetido) && rascunho.indexOf(repetido) == 0,
                "Rascunho identifica trecho repetido por valor");
        HashSet<PedidoTrecho> conjunto = new HashSet<>();
        conjunto.add(primeiro);
        conjunto.add(repetido);
        assertiva(conjunto.size() == 1, "Igualdade e hash do trecho concordam");
        assertiva(
                !rascunho.contains(new PedidoTrecho(1, 2, 3, 2, "1A", BigDecimal.ZERO)),
                "Outra ordem de trecho permanece diferente");
        br.edu.aeroporto.config.Configuracao config =
                new br.edu.aeroporto.config.Configuracao(
                        "jdbc:postgresql://localhost/teste",
                        "usuario",
                        "segredo-ficticio",
                        ZoneId.of("UTC"));
        assertiva(
                !config.toString().contains("segredo-ficticio"),
                "Configuracao nao revela senha no texto");
        System.out.println("20 verificacoes unitarias Java aprovadas.");
    }

    static void assertiva(boolean condicao, String nome) {
        if (!condicao) throw new AssertionError(nome);
    }

    static void falha(Runnable operacao, String nome) {
        try {
            operacao.run();
        } catch (br.edu.aeroporto.excecao.RegraNegocioException esperado) {
            return;
        }
        throw new AssertionError(nome);
    }
}
