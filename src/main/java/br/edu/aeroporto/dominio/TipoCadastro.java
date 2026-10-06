package br.edu.aeroporto.dominio;

import br.edu.aeroporto.excecao.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/** Lista fechada dos cadastros; nomes de tabelas/colunas nunca vem da entrada. */
public enum TipoCadastro {
    PAIS("pais", "id", "codigo:s2 nome:s100"),
    CIDADE("cidade", "id", "pais_id:i nome:s120 regiao:s100?"),
    AEROPORTO("aeroporto", "id", "cidade_id:i codigo_iata:s3? codigo_icao:s4 nome:s160 fuso_horario:s80 ativo:b"),
    TERMINAL("terminal", "id", "aeroporto_id:i codigo:s15 nome:s100 ativo:b"),
    RECURSO("recurso_aeroportuario", "id", "aeroporto_id:i terminal_id:i? tipo:PORTAO,PISTA,POSICAO,ESTEIRA,BALCAO codigo:s20 situacao:DISPONIVEL,INTERDITADO,MANUTENCAO comprimento_m:n? envergadura_max_m:n?"),
    COMPANHIA("companhia_aerea", "id", "pais_id:i codigo_icao:s3 codigo_iata:s2? nome:s150 ativo:b"),
    PESSOA("pessoa", "id", "nacionalidade_id:i? nome:s160 nascimento:d ativo:b"),
    DOCUMENTO("documento_pessoa", "id", "pessoa_id:i pais_emissor_id:i tipo:CPF,RG,PASSAPORTE,OUTRO numero:s40 validade:d?"),
    CONTATO("contato_pessoa", "id", "pessoa_id:i tipo:EMAIL,TELEFONE valor:s160 principal:b"),
    CARGO("cargo", "id", "nome:s80 area:ADMINISTRACAO,ATENDIMENTO,TRIPULACAO,MANUTENCAO,OPERACAO,SEGURANCA ativo:b"),
    FUNCIONARIO("funcionario", "pessoa_id", "pessoa_id:i cargo_id:i aeroporto_base_id:i companhia_id:i? matricula:s30 admissao:d desligamento:d?"),
    MODELO("modelo_aeronave", "id", "fabricante:s100 nome:s80 envergadura_m:n comprimento_pista_min_m:n ativo:b"),
    AERONAVE("aeronave", "id", "modelo_id:i companhia_id:i matricula:s15 situacao:ATIVA,INATIVA,MANUTENCAO fabricacao_ano:a"),
    ASSENTO("assento_aeronave", "id", "aeronave_id:i codigo:s6 fila:a coluna:s2 classe:ECONOMICA,EXECUTIVA,PRIMEIRA saida_emergencia:b ativo:b"),
    ROTA("rota", "id", "origem_id:i destino_id:i distancia_km:n ativa:b");

    private final String tabela;
    private final String chave;
    private final List<Campo> campos;

    TipoCadastro(String tabela, String chave, String definicao) {
        this.tabela = tabela;
        this.chave = chave;
        campos = Arrays.stream(definicao.split(" ")).map(item -> {
            String[] partes = item.split(":", 2);
            return new Campo(partes[0], partes[1].replace("?", ""), partes[1].endsWith("?"));
        }).toList();
    }

    public String tabela() { return tabela; }
    public String chave() { return chave; }
    public List<Campo> campos() { return campos; }

    public record Campo(String nome, String tipo, boolean opcional) {
        public Object converter(String entrada) {
            if ((entrada == null || entrada.isBlank()) && opcional) return null;
            String texto = Validacao.texto(entrada, nome, 200);
            try {
                if (tipo.equals("i")) { long id = Long.parseLong(texto); Dados.exigir(id > 0, "Id deve ser positivo."); return id; }
                if (tipo.equals("a")) { int valor = Integer.parseInt(texto); Dados.exigir(valor > 0 && valor <= 32767, "Numero fora do intervalo."); return valor; }
                if (tipo.equals("n")) return Dados.valor(new BigDecimal(texto), true);
                if (tipo.equals("d")) return LocalDate.parse(texto);
                if (tipo.equals("b")) {
                    Dados.exigir(texto.equalsIgnoreCase("true") || texto.equalsIgnoreCase("false"), "Use true ou false.");
                    return Boolean.parseBoolean(texto);
                }
                if (tipo.startsWith("s")) return Validacao.texto(texto, nome, Integer.parseInt(tipo.substring(1)));
                texto = texto.toUpperCase(java.util.Locale.ROOT);
                Dados.exigir(Arrays.asList(tipo.split(",")).contains(texto), "Valor invalido para " + nome + ": " + tipo);
                return texto;
            } catch (NumberFormatException | java.time.DateTimeException erro) {
                throw new RegraNegocioException("Formato invalido para " + nome + ".");
            }
        }
    }
}
