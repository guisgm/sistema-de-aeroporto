package br.edu.aeroporto.dominio;

public enum SituacaoVoo {
    PROGRAMADO,
    CHECKIN_ABERTO,
    EMBARQUE,
    EM_VOO,
    CONCLUIDO,
    CANCELADO;

    public boolean permiteTransicaoPara(SituacaoVoo proxima) {
        if (proxima == null || proxima == this) return false;
        if (proxima == CANCELADO)
            return this == PROGRAMADO || this == CHECKIN_ABERTO || this == EMBARQUE;
        switch (this) {
            case PROGRAMADO:
                return proxima == CHECKIN_ABERTO;
            case CHECKIN_ABERTO:
                return proxima == EMBARQUE;
            case EMBARQUE:
                return proxima == EM_VOO;
            case EM_VOO:
                return proxima == CONCLUIDO;
            case CONCLUIDO:
            case CANCELADO:
                return false;
            default:
                return false;
        }
    }
}
