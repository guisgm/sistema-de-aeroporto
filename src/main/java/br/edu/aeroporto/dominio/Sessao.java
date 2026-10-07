package br.edu.aeroporto.dominio;

import java.util.Set;

public final class Sessao {
    private final long usuarioId;
    private final Funcionario funcionario;
    private final Set<String> perfis;

    public Sessao(long usuarioId, Funcionario funcionario, Set<String> perfis) {
        perfis = Set.copyOf(perfis);
        this.usuarioId = usuarioId;
        this.funcionario = funcionario;
        this.perfis = perfis;
    }

    public long usuarioId() {
        return usuarioId;
    }

    public Funcionario funcionario() {
        return funcionario;
    }

    public Set<String> perfis() {
        return perfis;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof Sessao)) return false;
        Sessao outro = (Sessao) objeto;
        return usuarioId == outro.usuarioId
                && java.util.Objects.equals(funcionario, outro.funcionario)
                && java.util.Objects.equals(perfis, outro.perfis);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(usuarioId, funcionario, perfis);
    }

    @Override
    public String toString() {
        return "Sessao[usuarioId="
                + usuarioId
                + ", funcionario="
                + funcionario
                + ", perfis="
                + perfis
                + "]";
    }
}
