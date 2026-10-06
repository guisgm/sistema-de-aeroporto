package br.edu.aeroporto.repositorio;

import br.edu.aeroporto.dominio.CadastroPassageiro;
import br.edu.aeroporto.dominio.Passageiro;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface PassageiroRepositorio {
    Passageiro vincular(java.sql.Connection conexao, long pessoaId, String assistencia) throws java.sql.SQLException;
    Passageiro editar(java.sql.Connection conexao, long id, String nome, java.time.LocalDate nascimento,
                      Long nacionalidade, String assistencia, boolean ativo) throws java.sql.SQLException;
    Passageiro cadastrar(Connection conexao, CadastroPassageiro cadastro) throws SQLException;
    Optional<Passageiro> buscar(Connection conexao, long id) throws SQLException;
    List<Passageiro> buscar(Connection conexao, String nome, int limite, int deslocamento) throws SQLException;
}
