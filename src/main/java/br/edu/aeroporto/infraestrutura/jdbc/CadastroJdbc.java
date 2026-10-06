package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.TipoCadastro;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CadastroJdbc {
    public long salvar(Connection c, TipoCadastro tipo, Long id, Map<String, Object> valores) throws SQLException {
        var nomes = tipo.campos().stream().map(TipoCadastro.Campo::nome)
                .filter(nome -> id == null || !nome.equals(tipo.chave())).toList();
        var parametros = new ArrayList<Object>();
        nomes.forEach(nome -> parametros.add(valores.get(nome)));
        if (id == null) {
            return Sql.inserir(c, "INSERT INTO " + tipo.tabela() + "(" + String.join(",", nomes) + ") VALUES ("
                    + nomes.stream().map(n -> "?").collect(Collectors.joining(",")) + ") RETURNING " + tipo.chave(), parametros.toArray());
        }
        parametros.add(id);
        int alterados = Sql.executar(c, "UPDATE " + tipo.tabela() + " SET "
                + nomes.stream().map(n -> n + "=?").collect(Collectors.joining(",")) + " WHERE " + tipo.chave() + "=?", parametros.toArray());
        br.edu.aeroporto.dominio.Dados.exigir(alterados == 1, "Cadastro nao encontrado.");
        return id;
    }

    public List<Map<String, Object>> listar(Connection c, TipoCadastro tipo, int pagina) throws SQLException {
        return Sql.listar(c, "SELECT * FROM " + tipo.tabela() + " ORDER BY " + tipo.chave() + " LIMIT 20 OFFSET ?", Sql::linha, (pagina-1)*20);
    }

    public Map<String, Object> buscar(Connection c, TipoCadastro tipo, long id) throws SQLException {
        return Sql.registro(c, "SELECT * FROM " + tipo.tabela() + " WHERE " + tipo.chave() + "=? FOR UPDATE", id);
    }
}
