package br.edu.aeroporto.dominio;

import br.edu.aeroporto.excecao.RegraNegocioException;

import java.time.LocalDate;
import java.util.Locale;

public final class Validacao {
    private Validacao() {}

    public static String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank())
            throw new RegraNegocioException(campo + " é obrigatório.");
        String resultado = valor.strip();
        if (resultado.length() > maximo)
            throw new RegraNegocioException(campo + " excede " + maximo + " caracteres.");
        for (int i = 0; i < resultado.length(); i++) {
            if (Character.isISOControl(resultado.charAt(i))) {
                throw new RegraNegocioException(campo + " contém caracteres de controle.");
            }
        }
        return resultado;
    }

    public static LocalDate nascimento(LocalDate data, LocalDate hoje) {
        if (data == null || data.isAfter(hoje))
            throw new RegraNegocioException(
                    "Informe uma data de nascimento válida, que não esteja no futuro.");
        return data;
    }

    public static String login(String valor) {
        return texto(valor, "Login", 60).toLowerCase(Locale.ROOT);
    }

    public static String cpf(String valor) {
        if (valor == null || valor.isBlank()) return "";
        String numero = valor.replaceAll("[.\\-\\s]", "");
        if (!numero.matches("[0-9]{11}")) {
            throw new RegraNegocioException("CPF inválido.");
        }
        boolean todosIguais = true;
        for (int i = 1; i < numero.length(); i++) {
            if (numero.charAt(i) != numero.charAt(0)) {
                todosIguais = false;
                break;
            }
        }
        if (todosIguais) throw new RegraNegocioException("CPF inválido.");
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (numero.charAt(i) - '0') * (tamanho + 1 - i);
            int digito = 11 - soma % 11;
            if (digito >= 10) digito = 0;
            if (digito != numero.charAt(tamanho) - '0')
                throw new RegraNegocioException("CPF inválido.");
        }
        return numero;
    }
}
