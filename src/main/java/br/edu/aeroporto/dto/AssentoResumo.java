package br.edu.aeroporto.dto;

public final class AssentoResumo {
    private final String codigo;
    private final String classe;
    private final boolean bloqueado;
    private final boolean ocupado;

    public AssentoResumo(String codigo, String classe, boolean bloqueado, boolean ocupado) {
        this.codigo = codigo;
        this.classe = classe;
        this.bloqueado = bloqueado;
        this.ocupado = ocupado;
    }

    public String codigo() {
        return codigo;
    }

    public String classe() {
        return classe;
    }

    public boolean bloqueado() {
        return bloqueado;
    }

    public boolean ocupado() {
        return ocupado;
    }

    public boolean livre() {
        return !bloqueado && !ocupado;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof AssentoResumo)) return false;
        AssentoResumo outro = (AssentoResumo) objeto;
        return java.util.Objects.equals(codigo, outro.codigo)
                && java.util.Objects.equals(classe, outro.classe)
                && bloqueado == outro.bloqueado
                && ocupado == outro.ocupado;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(codigo, classe, bloqueado, ocupado);
    }

    @Override
    public String toString() {
        return "AssentoResumo[codigo="
                + codigo
                + ", classe="
                + classe
                + ", bloqueado="
                + bloqueado
                + ", ocupado="
                + ocupado
                + "]";
    }
}
