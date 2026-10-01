package br.edu.aeroporto.dominio;

import java.util.Set;

public record Sessao(long usuarioId, Funcionario funcionario, Set<String> perfis) {
    public Sessao {
        perfis = Set.copyOf(perfis);
    }

}
