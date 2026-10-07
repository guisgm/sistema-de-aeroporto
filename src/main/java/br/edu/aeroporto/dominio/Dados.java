package br.edu.aeroporto.dominio;

import br.edu.aeroporto.excecao.RegraNegocioException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public final class Dados {
    private Dados() {}

    public static long id(Map<String, Object> dados, String campo) {
        Object valor = dados.get(campo);
        if (!(valor instanceof Number)) throw new RegraNegocioException("Id invalido: " + campo);
        Number numero = (Number) valor;
        if (numero.longValue() <= 0) throw new RegraNegocioException("Id invalido: " + campo);
        return numero.longValue();
    }

    public static String texto(Map<String, Object> dados, String campo) {
        return String.valueOf(dados.get(campo));
    }

    public static BigDecimal dinheiro(Map<String, Object> dados, String campo) {
        Object valor = dados.get(campo);
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    public static BigDecimal valor(BigDecimal valor, boolean positivo) {
        if (valor == null
                || valor.scale() > 2
                || valor.precision() > 14
                || valor.signum() < (positivo ? 1 : 0)) {
            throw new RegraNegocioException(
                    "Valor monetario invalido; use ate duas casas decimais.");
        }
        return valor.setScale(2);
    }

    public static Instant instante(Map<String, Object> dados, String campo) {
        Object valor = dados.get(campo);
        if (valor instanceof OffsetDateTime) return ((OffsetDateTime) valor).toInstant();
        if (valor instanceof Timestamp) return ((Timestamp) valor).toInstant();
        if (valor instanceof Instant)
            return ((Instant) valor).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        throw new RegraNegocioException("Horario nao informado: " + campo);
    }

    public static void exigir(boolean condicao, String mensagem) {
        if (!condicao) throw new RegraNegocioException(mensagem);
    }

    public static String codigo(String prefixo, int tamanho) {
        return prefixo
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, tamanho - prefixo.length())
                        .toUpperCase(java.util.Locale.ROOT);
    }
}
