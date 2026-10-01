package br.edu.aeroporto.infraestrutura.jdbc;

import br.edu.aeroporto.dominio.PrimeiroAcesso;
import br.edu.aeroporto.excecao.RegraNegocioException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public final class InicializacaoJdbc {
    public long criarAdministrador(Connection conexao, PrimeiroAcesso dados, String senhaHash,
                                   LocalDate hoje, ZoneId fuso) throws SQLException {
        // Serializa o primeiro cadastro mesmo com dois terminais abertos.
        Sql.unico(conexao, "SELECT pg_advisory_xact_lock(hashtext('aeroporto.primeiro_acesso'))", linha -> true);
        boolean existe = Sql.unico(conexao, "SELECT EXISTS (SELECT 1 FROM usuario_sistema)", linha -> linha.getBoolean(1)).orElse(false);
        if (existe) throw new RegraNegocioException("O primeiro acesso já foi criado. Use o login existente.");

        Sql.executar(conexao, "INSERT INTO pais(codigo, nome) VALUES ('BR', 'Brasil') ON CONFLICT (codigo) DO NOTHING");
        long pais = Sql.unico(conexao, "SELECT id FROM pais WHERE codigo = 'BR'", linha -> linha.getLong(1)).orElseThrow();
        Sql.executar(conexao, """
                INSERT INTO cidade(pais_id, nome, regiao) VALUES (?, ?, ?)
                ON CONFLICT (pais_id, nome, regiao) DO NOTHING
                """, pais, dados.cidade(), dados.regiao());
        long cidade = Sql.unico(conexao, "SELECT id FROM cidade WHERE pais_id = ? AND nome = ? AND regiao = ?",
                linha -> linha.getLong(1), pais, dados.cidade(), dados.regiao()).orElseThrow();
        Sql.executar(conexao, """
                INSERT INTO aeroporto(cidade_id, codigo_icao, nome, fuso_horario) VALUES (?, ?, ?, ?)
                ON CONFLICT (codigo_icao) DO NOTHING
                """, cidade, dados.icao(), dados.aeroporto(), fuso.getId());
        long aeroporto = Sql.unico(conexao, "SELECT id FROM aeroporto WHERE codigo_icao = ? AND ativo",
                linha -> linha.getLong(1), dados.icao()).orElseThrow(() -> new RegraNegocioException("O aeroporto está inativo."));
        Sql.executar(conexao, """
                INSERT INTO cargo(nome, area) VALUES ('Administrador do sistema', 'ADMINISTRACAO')
                ON CONFLICT (nome) DO NOTHING
                """);
        long cargo = Sql.unico(conexao, "SELECT id FROM cargo WHERE nome = 'Administrador do sistema'",
                linha -> linha.getLong(1)).orElseThrow();
        long pessoa = Sql.inserir(conexao, "INSERT INTO pessoa(nome, nascimento) VALUES (?, ?) RETURNING id",
                dados.nome(), dados.nascimento());
        String matricula = "F" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Sql.executar(conexao, """
                INSERT INTO funcionario(pessoa_id, cargo_id, aeroporto_base_id, matricula, admissao)
                VALUES (?, ?, ?, ?, ?)
                """, pessoa, cargo, aeroporto, matricula, hoje);
        long usuario = Sql.inserir(conexao, """
                INSERT INTO usuario_sistema(funcionario_id, login, senha_hash) VALUES (?, ?, ?) RETURNING id
                """, pessoa, dados.login(), senhaHash);
        Sql.executar(conexao, """
                INSERT INTO perfil_acesso(nome, descricao)
                VALUES ('ADMINISTRADOR', 'Administração do sistema'),
                       ('ATENDIMENTO', 'Cadastro e atendimento a passageiros'),
                       ('OPERACAO', 'Operação e consulta de voos'),
                       ('CONSULTA', 'Consulta de informações')
                ON CONFLICT (nome) DO NOTHING
                """);
        Sql.executar(conexao, """
                INSERT INTO usuario_perfil(usuario_id, perfil_id)
                SELECT ?, id FROM perfil_acesso WHERE nome = 'ADMINISTRADOR'
                """, usuario);
        return usuario;
    }
}
