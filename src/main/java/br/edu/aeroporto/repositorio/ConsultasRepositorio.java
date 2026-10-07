package br.edu.aeroporto.repositorio;

import br.edu.aeroporto.dto.AssentoResumo;
import br.edu.aeroporto.dto.ReservaResumo;
import br.edu.aeroporto.dto.VooResumo;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConsultasRepositorio {
    List<VooResumo> voos(Connection conexao, Instant inicio, Instant fim, int pagina)
            throws SQLException;

    List<AssentoResumo> assentos(Connection conexao, long vooId) throws SQLException;

    Optional<ReservaResumo> reserva(Connection conexao, String localizador) throws SQLException;
}
