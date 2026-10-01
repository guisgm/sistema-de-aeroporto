package br.edu.aeroporto.dto;

import br.edu.aeroporto.dominio.SituacaoVoo;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record VooResumo(long id, String companhia, String numero, String origem, String destino,
                        ZoneId fusoOrigem, ZoneId fusoDestino, OffsetDateTime partida,
                        OffsetDateTime chegada, OffsetDateTime partidaEstimada,
                        OffsetDateTime partidaReal, SituacaoVoo situacao) {
    public boolean atrasado() {
        OffsetDateTime referencia = partidaReal != null ? partidaReal : partidaEstimada;
        return referencia != null && referencia.isAfter(partida);
    }
}
