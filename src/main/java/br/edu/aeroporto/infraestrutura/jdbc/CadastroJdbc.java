package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.TipoCadastro;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class CadastroJdbc {
    public long salvar(Connection c, TipoCadastro tipo, Long id, Map<String, Object> valores)
            throws SQLException {
        List<String> nomes = new ArrayList<>();
        ArrayList<Object> parametros = new ArrayList<>();
        StringJoiner marcadores = new StringJoiner(",");
        StringJoiner atribuicoes = new StringJoiner(",");
        for (TipoCadastro.Campo campo : tipo.campos()) {
            String nome = campo.nome();
            if (id != null && nome.equals(tipo.chave())) continue;
            nomes.add(nome);
            parametros.add(valores.get(nome));
            marcadores.add("?");
            atribuicoes.add(nome + "=?");
        }
        if (id == null) {
            return Sql.inserir(
                    c,
                    "INSERT INTO "
                            + tipo.tabela()
                            + "("
                            + String.join(",", nomes)
                            + ") VALUES ("
                            + marcadores
                            + ") RETURNING "
                            + tipo.chave(),
                    parametros.toArray());
        }
        parametros.add(id);
        int alterados =
                Sql.executar(
                        c,
                        "UPDATE "
                                + tipo.tabela()
                                + " SET "
                                + atribuicoes
                                + " WHERE "
                                + tipo.chave()
                                + "=?",
                        parametros.toArray());
        br.edu.aeroporto.dominio.Dados.exigir(alterados == 1, "Cadastro nao encontrado.");
        return id;
    }

    public List<Map<String, Object>> listar(Connection c, TipoCadastro tipo, int pagina)
            throws SQLException {
        return Sql.listar(
                c,
                "SELECT * FROM "
                        + tipo.tabela()
                        + " ORDER BY "
                        + tipo.chave()
                        + " LIMIT 20 OFFSET ?",
                (pagina - 1) * 20);
    }

    public Map<String, Object> buscar(Connection c, TipoCadastro tipo, long id)
            throws SQLException {
        return Sql.registro(
                c,
                "SELECT * FROM " + tipo.tabela() + " WHERE " + tipo.chave() + "=? FOR UPDATE",
                id);
    }
}
