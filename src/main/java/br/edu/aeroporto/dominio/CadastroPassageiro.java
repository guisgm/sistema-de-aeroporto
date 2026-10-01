package br.edu.aeroporto.dominio;

import br.edu.aeroporto.excecao.RegraNegocioException;
import java.time.LocalDate;
import java.util.Locale;

public record CadastroPassageiro(String nome, LocalDate nascimento, String cpf, String email, String telefone) {
    public CadastroPassageiro {
        nome = Validacao.texto(nome, "Nome", 160);
        cpf = Validacao.cpf(cpf);
        email = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        telefone = telefone == null ? "" : telefone.strip().replaceAll("[()\\-\\s]", "");
        if (!email.isEmpty() && (email.length() > 160 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) {
            throw new RegraNegocioException("E-mail inválido.");
        }
        if (!telefone.isEmpty() && !telefone.matches("\\+?[0-9]{8,15}")) {
            throw new RegraNegocioException("Telefone inválido: use de 8 a 15 dígitos, com + opcional.");
        }
    }
}
