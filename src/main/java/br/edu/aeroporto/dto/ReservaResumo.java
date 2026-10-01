package br.edu.aeroporto.dto;

import br.edu.aeroporto.dominio.SituacaoReserva;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ReservaResumo(long id, String localizador, SituacaoReserva situacao, OffsetDateTime expiraEm,
                            List<Trecho> trechos) {
    public ReservaResumo { trechos = List.copyOf(trechos); }

    public record Trecho(long id, String passageiro, String voo, int ordem,
                        String situacao, String assento, BigDecimal valor) {}

    public BigDecimal totalOriginalPassagens() {
        return trechos.stream().map(Trecho::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
