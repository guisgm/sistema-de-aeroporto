package br.edu.aeroporto.dominio;

public enum SituacaoVoo {
    PROGRAMADO, CHECKIN_ABERTO, EMBARQUE, EM_VOO, CONCLUIDO, CANCELADO;

    public boolean permiteTransicaoPara(SituacaoVoo proxima) {
        if (proxima == null || proxima == this) return false;
        if (proxima == CANCELADO) return this == PROGRAMADO || this == CHECKIN_ABERTO || this == EMBARQUE;
        return switch (this) {
            case PROGRAMADO -> proxima == CHECKIN_ABERTO;
            case CHECKIN_ABERTO -> proxima == EMBARQUE;
            case EMBARQUE -> proxima == EM_VOO;
            case EM_VOO -> proxima == CONCLUIDO;
            case CONCLUIDO, CANCELADO -> false;
        };
    }
}
