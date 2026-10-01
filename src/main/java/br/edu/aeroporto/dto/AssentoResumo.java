package br.edu.aeroporto.dto;

public record AssentoResumo(String codigo, String classe, boolean bloqueado, boolean ocupado) {
    public boolean livre() { return !bloqueado && !ocupado; }
}
