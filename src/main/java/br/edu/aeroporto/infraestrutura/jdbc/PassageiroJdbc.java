package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.CadastroPassageiro;
import br.edu.aeroporto.dominio.Passageiro;
import br.edu.aeroporto.excecao.RegraNegocioException;
import br.edu.aeroporto.repositorio.PassageiroRepositorio;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PassageiroJdbc implements PassageiroRepositorio {
    private static final String SELECT =
            """
            SELECT p.id, p.nome, p.nascimento, p.ativo AND pa.ativo AS ativo, pa.codigo_cliente
            FROM pessoa p JOIN passageiro pa ON pa.pessoa_id = p.id
            """;

    @Override
    public Passageiro vincular(Connection c, long pessoaId, String assistencia)
            throws SQLException {
        Sql.registro(c, "SELECT id FROM pessoa WHERE id=? AND ativo FOR UPDATE", pessoaId);
        String codigo =
                "P"
                        + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 20)
                                .toUpperCase(java.util.Locale.ROOT);
        Sql.executar(
                c,
                "INSERT INTO passageiro(pessoa_id,codigo_cliente,observacoes_assistencia) VALUES (?,?,?) ON CONFLICT (pessoa_id) DO NOTHING",
                pessoaId,
                codigo,
                assistencia);
        return buscar(c, pessoaId).orElseThrow();
    }

    @Override
    public Passageiro editar(
            Connection c,
            long id,
            String nome,
            LocalDate nascimento,
            Long nacionalidade,
            String assistencia,
            boolean ativo)
            throws SQLException {
        Sql.registro(c, "SELECT pessoa_id FROM passageiro WHERE pessoa_id=? FOR UPDATE", id);
        Sql.executar(
                c,
                "UPDATE pessoa SET nome=?,nascimento=?,nacionalidade_id=? WHERE id=?",
                nome,
                nascimento,
                nacionalidade,
                id);
        Sql.executar(
                c,
                "UPDATE passageiro SET observacoes_assistencia=?,ativo=? WHERE pessoa_id=?",
                assistencia,
                ativo,
                id);
        return buscar(c, id).orElseThrow();
    }

    @Override
    public Passageiro cadastrar(Connection conexao, CadastroPassageiro cadastro)
            throws SQLException {
        long id =
                Sql.inserir(
                        conexao,
                        "INSERT INTO pessoa(nome, nascimento) VALUES (?, ?) RETURNING id",
                        cadastro.nome(),
                        cadastro.nascimento());
        String codigo =
                "P"
                        + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 20)
                                .toUpperCase(java.util.Locale.ROOT);
        Sql.executar(
                conexao,
                "INSERT INTO passageiro(pessoa_id, codigo_cliente) VALUES (?, ?)",
                id,
                codigo);
        if (!cadastro.cpf().isEmpty()) {
            long brasil =
                    Sql.unicoNumero(conexao, "SELECT id FROM pais WHERE codigo = 'BR'")
                            .orElseThrow(
                                    () ->
                                            new RegraNegocioException(
                                                    "Cadastre o país BR antes de registrar CPF."));
            Sql.executar(
                    conexao,
                    """
                    INSERT INTO documento_pessoa(pessoa_id, pais_emissor_id, tipo, numero)
                    VALUES (?, ?, 'CPF', ?)
                    """,
                    id,
                    brasil,
                    cadastro.cpf());
        }
        salvarContato(conexao, id, "EMAIL", cadastro.email());
        salvarContato(conexao, id, "TELEFONE", cadastro.telefone());
        return new Passageiro(id, cadastro.nome(), cadastro.nascimento(), true, codigo);
    }

    @Override
    public Optional<Passageiro> buscar(Connection conexao, long id) throws SQLException {
        return Sql.unico(conexao, SELECT + " WHERE p.id = ?", PassageiroJdbc::mapear, id);
    }

    @Override
    public List<Passageiro> buscar(Connection conexao, String nome, int limite, int deslocamento)
            throws SQLException {
        // strpos trata o filtro como texto literal, inclusive caracteres % e _.
        return Sql.listar(
                conexao,
                SELECT
                        + """
                WHERE strpos(lower(p.nome), lower(?)) > 0
                ORDER BY p.nome, p.id LIMIT ? OFFSET ?
                """,
                PassageiroJdbc::mapear,
                nome,
                limite,
                deslocamento);
    }

    private static Passageiro mapear(ResultSet linha) throws SQLException {
        return new Passageiro(
                linha.getLong("id"),
                linha.getString("nome"),
                linha.getObject("nascimento", LocalDate.class),
                linha.getBoolean("ativo"),
                linha.getString("codigo_cliente"));
    }

    private static void salvarContato(Connection conexao, long id, String tipo, String valor)
            throws SQLException {
        if (valor.isEmpty()) return;
        Sql.executar(
                conexao,
                """
                INSERT INTO contato_pessoa(pessoa_id, tipo, valor, principal) VALUES (?, ?, ?, TRUE)
                """,
                id,
                tipo,
                valor);
    }
}
