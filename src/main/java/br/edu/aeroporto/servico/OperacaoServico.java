package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class OperacaoServico {
    private final BancoDados banco;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;
    private final PlanejamentoJdbc planejamento = new PlanejamentoJdbc();

    public OperacaoServico(
            BancoDados banco, AutorizacaoJdbc autorizacao, AuditoriaJdbc auditoria, Clock relogio) {
        this.banco = banco;
        this.autorizacao = autorizacao;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public long alocar(
            Sessao s, long voo, long recurso, String finalidade, Instant inicio, Instant fim) {
        return banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    planejamento.serializar(c);
                    BloqueiosJdbc.voo(c, voo);
                    OperacaoJdbc.validarRecurso(c, voo, recurso, finalidade, inicio, fim);
                    long id =
                            Sql.inserir(
                                    c,
                                    "INSERT INTO alocacao_recurso(recurso_id,voo_id,finalidade,inicio,fim) VALUES (?,?,?,?,?) RETURNING id",
                                    recurso,
                                    voo,
                                    finalidade,
                                    inicio,
                                    fim);
                    auditoria.registrar(c, s.usuarioId(), "ALOCAR_RECURSO", "alocacao_recurso", id);
                    return id;
                });
    }

    public long interditar(Sessao s, long recurso, Instant inicio, Instant fim, String motivo) {
        Dados.exigir(fim.isAfter(inicio), "Intervalo invalido.");
        String razao = Validacao.texto(motivo, "Motivo", 1000);
        return banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    planejamento.serializar(c);
                    Sql.registro(
                            c,
                            "SELECT id FROM recurso_aeroportuario WHERE id=? FOR UPDATE",
                            recurso);
                    long id =
                            Sql.inserir(
                                    c,
                                    "INSERT INTO alocacao_recurso(recurso_id,finalidade,inicio,fim,observacao) VALUES (?,'INTERDICAO',?,?,?) RETURNING id",
                                    recurso,
                                    inicio,
                                    fim,
                                    razao);
                    auditoria.registrar(
                            c, s.usuarioId(), "INTERDITAR_RECURSO", "alocacao_recurso", id);
                    return id;
                });
    }

    public void liberarAlocacao(Sessao s, long id) {
        banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    planejamento.serializar(c);
                    Map<String, Object> a =
                            Sql.registro(c, "SELECT * FROM alocacao_recurso WHERE id=?", id);
                    if (a.get("voo_id") != null) {
                        Map<String, Object> v = BloqueiosJdbc.voo(c, Dados.id(a, "voo_id"));
                        Dados.exigir(
                                !"EMBARQUE".equals(v.get("situacao")),
                                "Nao libere o recurso durante embarque.");
                    }
                    Sql.executar(c, "UPDATE alocacao_recurso SET ativa=false WHERE id=?", id);
                    auditoria.registrar(
                            c, s.usuarioId(), "LIBERAR_RECURSO", "alocacao_recurso", id);
                    return null;
                });
    }

    public long habilitar(
            Sessao s,
            long funcionario,
            long modelo,
            String funcao,
            String licenca,
            LocalDate validade) {
        Dados.exigir(
                Set.of("COMANDANTE", "COPILOTO", "COMISSARIO").contains(funcao)
                        && validade != null
                        && !validade.isBefore(LocalDate.now(relogio)),
                "Habilitacao invalida.");
        String numero = Validacao.texto(licenca, "Licenca", 40);
        return banco.transacao(
                c -> {
                    autorizacao.exigirAdministrador(c, s);
                    planejamento.serializar(c);
                    long id =
                            Sql.inserir(
                                    c,
                                    """
                    INSERT INTO habilitacao_tripulante(funcionario_id,modelo_id,funcao,numero_licenca,validade) VALUES (?,?,?,?,?)
                    ON CONFLICT (funcionario_id,modelo_id,funcao) DO UPDATE SET numero_licenca=EXCLUDED.numero_licenca,validade=EXCLUDED.validade RETURNING id
                    """,
                                    funcionario,
                                    modelo,
                                    funcao,
                                    numero,
                                    validade);
                    for (long voo :
                            Sql.listarNumeros(
                                    c,
                                    "SELECT DISTINCT voo_id FROM escala_funcionario WHERE funcionario_id=? AND ativa AND voo_id IS NOT NULL AND fim>CURRENT_TIMESTAMP",
                                    funcionario)) OperacaoJdbc.validarEscalasDoVoo(c, voo);
                    auditoria.registrar(
                            c, s.usuarioId(), "HABILITAR_TRIPULANTE", "habilitacao_tripulante", id);
                    return id;
                });
    }

    public long escalar(
            Sessao s,
            long funcionario,
            long aeroporto,
            Long voo,
            String funcao,
            Instant inicio,
            Instant fim) {
        Dados.exigir(
                Set.of(
                                "COMANDANTE",
                                "COPILOTO",
                                "COMISSARIO",
                                "ATENDIMENTO",
                                "SOLO",
                                "MANUTENCAO",
                                "SEGURANCA")
                        .contains(funcao),
                "Funcao invalida.");
        return banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    planejamento.serializar(c);
                    if (voo != null) BloqueiosJdbc.voo(c, voo);
                    HashMap<String, Object> dados = new java.util.HashMap<String, Object>();
                    dados.put("funcionario_id", funcionario);
                    dados.put("aeroporto_id", aeroporto);
                    dados.put("voo_id", voo);
                    dados.put("funcao", funcao);
                    dados.put("inicio", inicio);
                    dados.put("fim", fim);
                    OperacaoJdbc.validarEscala(c, dados, null);
                    long id =
                            Sql.inserir(
                                    c,
                                    "INSERT INTO escala_funcionario(funcionario_id,aeroporto_id,voo_id,funcao,inicio,fim) VALUES (?,?,?,?,?,?) RETURNING id",
                                    funcionario,
                                    aeroporto,
                                    voo,
                                    funcao,
                                    inicio,
                                    fim);
                    auditoria.registrar(
                            c, s.usuarioId(), "ESCALAR_FUNCIONARIO", "escala_funcionario", id);
                    return id;
                });
    }

    public void liberarEscala(Sessao s, long id) {
        banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    planejamento.serializar(c);
                    Map<String, Object> e =
                            Sql.registro(c, "SELECT * FROM escala_funcionario WHERE id=?", id);
                    if (e.get("voo_id") != null) {
                        Map<String, Object> v = BloqueiosJdbc.voo(c, Dados.id(e, "voo_id"));
                        Dados.exigir(
                                Set.of("PROGRAMADO", "CHECKIN_ABERTO", "CANCELADO", "CONCLUIDO")
                                        .contains(v.get("situacao")),
                                "Equipe em operacao.");
                    }
                    Sql.executar(c, "UPDATE escala_funcionario SET ativa=false WHERE id=?", id);
                    auditoria.registrar(
                            c, s.usuarioId(), "LIBERAR_ESCALA", "escala_funcionario", id);
                    return null;
                });
    }

    public long manutencao(
            Sessao s,
            long aeronave,
            long responsavel,
            String tipo,
            String descricao,
            Instant inicio,
            Instant fim,
            BigDecimal custo) {
        Dados.exigir(
                Set.of("PREVENTIVA", "CORRETIVA", "INSPECAO").contains(tipo) && fim.isAfter(inicio),
                "Manutencao invalida.");
        String texto = Validacao.texto(descricao, "Descricao", 2000);
        BigDecimal valor = Dados.valor(custo, false);
        return banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    planejamento.serializar(c);
                    Sql.registro(
                            c,
                            "SELECT id FROM aeronave WHERE id=? AND situacao<>'INATIVA' FOR UPDATE",
                            aeronave);
                    funcionarioAtivo(c, responsavel);
                    long id =
                            Sql.inserir(
                                    c,
                                    "INSERT INTO manutencao_aeronave(aeronave_id,responsavel_id,tipo,descricao,custo) VALUES (?,?,?,?,?) RETURNING id",
                                    aeronave,
                                    responsavel,
                                    tipo,
                                    texto,
                                    valor);
                    Sql.executar(
                            c,
                            "INSERT INTO agenda_aeronave(aeronave_id,manutencao_id,inicio,fim) VALUES (?,?,?,?)",
                            aeronave,
                            id,
                            inicio,
                            fim);
                    auditoria.registrar(
                            c, s.usuarioId(), "AGENDAR_MANUTENCAO", "manutencao_aeronave", id);
                    return id;
                });
    }

    public void situacaoManutencao(Sessao s, long id, String estado) {
        banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    planejamento.serializar(c);
                    Map<String, Object> m =
                            Sql.registro(
                                    c,
                                    "SELECT * FROM manutencao_aeronave WHERE id=? FOR UPDATE",
                                    id);
                    String antes = Dados.texto(m, "situacao");
                    Dados.exigir(
                            (antes.equals("AGENDADA")
                                            && Set.of("EM_EXECUCAO", "CANCELADA").contains(estado))
                                    || (antes.equals("EM_EXECUCAO") && estado.equals("CONCLUIDA")),
                            "Transicao de manutencao invalida.");
                    Map<String, Object> a =
                            Sql.registro(
                                    c, "SELECT * FROM agenda_aeronave WHERE manutencao_id=?", id);
                    if (estado.equals("EM_EXECUCAO")) {
                        Dados.exigir(
                                !relogio.instant().isBefore(Dados.instante(a, "inicio"))
                                        && relogio.instant().isBefore(Dados.instante(a, "fim")),
                                "Fora da janela de manutencao.");
                        Sql.executar(
                                c,
                                "UPDATE manutencao_aeronave SET inicio_real=?,situacao=? WHERE id=?",
                                relogio.instant(),
                                estado,
                                id);
                        Sql.executar(
                                c,
                                "UPDATE aeronave SET situacao='MANUTENCAO' WHERE id=?",
                                Dados.id(m, "aeronave_id"));
                    } else {
                        if (estado.equals("CONCLUIDA")) {
                            Dados.exigir(
                                    !relogio.instant().isBefore(Dados.instante(m, "inicio_real")),
                                    "Fim real invalido.");
                            Sql.executar(
                                    c,
                                    "UPDATE agenda_aeronave SET fim=GREATEST(fim,?) WHERE manutencao_id=? AND ativa",
                                    relogio.instant(),
                                    id);
                        }
                        Sql.executar(
                                c,
                                "UPDATE manutencao_aeronave SET situacao=?,fim_real=? WHERE id=?",
                                estado,
                                estado.equals("CONCLUIDA") ? relogio.instant() : null,
                                id);
                        Sql.executar(
                                c,
                                "UPDATE agenda_aeronave SET ativa=false WHERE manutencao_id=?",
                                id);
                        Sql.executar(
                                c,
                                "UPDATE aeronave SET situacao='ATIVA' WHERE id=? AND situacao='MANUTENCAO'",
                                Dados.id(m, "aeronave_id"));
                    }
                    auditoria.registrar(
                            c, s.usuarioId(), "SITUACAO_MANUTENCAO", "manutencao_aeronave", id);
                    return null;
                });
    }

    public long servicoSolo(
            Sessao s,
            long voo,
            long aeroporto,
            long responsavel,
            String tipo,
            BigDecimal quantidade,
            String unidade,
            BigDecimal custo) {
        Dados.exigir(
                Set.of("ABASTECIMENTO", "LIMPEZA", "CATERING", "BAGAGEM", "REBOQUE").contains(tipo),
                "Tipo de servico invalido.");
        BigDecimal valor = Dados.valor(custo, false);
        BigDecimal medida = Dados.valor(quantidade, false);
        String un = Validacao.texto(unidade, "Unidade", 20);
        return banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    planejamento.serializar(c);
                    Map<String, Object> v = BloqueiosJdbc.voo(c, voo);
                    Dados.exigir(
                            !Set.of("EM_VOO", "CONCLUIDO", "CANCELADO").contains(v.get("situacao")),
                            "Voo nao admite servico.");
                    Map<String, Object> rota =
                            Sql.registro(
                                    c,
                                    "SELECT origem_id,destino_id FROM rota WHERE id=?",
                                    Dados.id(v, "rota_id"));
                    Dados.exigir(
                            aeroporto == Dados.id(rota, "origem_id")
                                    || aeroporto == Dados.id(rota, "destino_id"),
                            "Servico fora do trecho.");
                    funcionarioAtivo(c, responsavel);
                    long id =
                            Sql.inserir(
                                    c,
                                    "INSERT INTO servico_solo(voo_id,aeroporto_id,responsavel_id,tipo,quantidade,unidade,custo) VALUES (?,?,?,?,?,?,?) RETURNING id",
                                    voo,
                                    aeroporto,
                                    responsavel,
                                    tipo,
                                    medida,
                                    un,
                                    valor);
                    auditoria.registrar(c, s.usuarioId(), "CRIAR_SERVICO_SOLO", "servico_solo", id);
                    return id;
                });
    }

    public void situacaoSolo(Sessao s, long id, String estado) {
        banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    planejamento.serializar(c);
                    Map<String, Object> inicial =
                            Sql.registro(c, "SELECT voo_id FROM servico_solo WHERE id=?", id);
                    BloqueiosJdbc.voo(c, Dados.id(inicial, "voo_id"));
                    Map<String, Object> solo =
                            Sql.registro(c, "SELECT * FROM servico_solo WHERE id=? FOR UPDATE", id);
                    String antes = Dados.texto(solo, "situacao");
                    Dados.exigir(
                            (antes.equals("PENDENTE")
                                            && Set.of("EM_EXECUCAO", "CANCELADO").contains(estado))
                                    || (antes.equals("EM_EXECUCAO")
                                            && Set.of("CONCLUIDO", "CANCELADO").contains(estado)),
                            "Transicao de servico invalida.");
                    Sql.executar(
                            c,
                            "UPDATE servico_solo SET situacao=?,inicio=COALESCE(inicio,?),fim=? WHERE id=?",
                            estado,
                            relogio.instant(),
                            Set.of("CONCLUIDO", "CANCELADO").contains(estado)
                                    ? relogio.instant()
                                    : null,
                            id);
                    auditoria.registrar(
                            c, s.usuarioId(), "SITUACAO_SERVICO_SOLO", "servico_solo", id);
                    return null;
                });
    }

    public long ocorrencia(
            Sessao s, long aeroporto, Long voo, String tipo, String gravidade, String descricao) {
        Dados.exigir(
                Set.of("ATRASO", "METEOROLOGIA", "SEGURANCA", "TECNICA", "OUTRA").contains(tipo)
                        && Set.of("BAIXA", "MEDIA", "ALTA").contains(gravidade),
                "Ocorrencia invalida.");
        String texto = Validacao.texto(descricao, "Descricao", 2000);
        return banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    if (voo != null) {
                        Map<String, Object> v = BloqueiosJdbc.voo(c, voo);
                        Map<String, Object> r =
                                Sql.registro(
                                        c,
                                        "SELECT origem_id,destino_id FROM rota WHERE id=?",
                                        Dados.id(v, "rota_id"));
                        Dados.exigir(
                                aeroporto == Dados.id(r, "origem_id")
                                        || aeroporto == Dados.id(r, "destino_id"),
                                "Ocorrencia fora do trecho.");
                    }
                    long id =
                            Sql.inserir(
                                    c,
                                    "INSERT INTO ocorrencia_operacional(aeroporto_id,voo_id,usuario_id,tipo,gravidade,descricao) VALUES (?,?,?,?,?,?) RETURNING id",
                                    aeroporto,
                                    voo,
                                    s.usuarioId(),
                                    tipo,
                                    gravidade,
                                    texto);
                    auditoria.registrar(
                            c, s.usuarioId(), "ABRIR_OCORRENCIA", "ocorrencia_operacional", id);
                    return id;
                });
    }

    public void encerrarOcorrencia(Sessao s, long id) {
        banco.transacao(
                c -> {
                    autorizacao.exigirOperacao(c, s);
                    Dados.exigir(
                            Sql.executar(
                                            c,
                                            "UPDATE ocorrencia_operacional SET encerrada_em=? WHERE id=? AND encerrada_em IS NULL",
                                            relogio.instant(),
                                            id)
                                    == 1,
                            "Ocorrencia ja encerrada ou inexistente.");
                    auditoria.registrar(
                            c, s.usuarioId(), "ENCERRAR_OCORRENCIA", "ocorrencia_operacional", id);
                    return null;
                });
    }

    private void funcionarioAtivo(java.sql.Connection c, long id) throws java.sql.SQLException {
        Sql.registro(
                c,
                "SELECT f.pessoa_id FROM funcionario f JOIN pessoa p ON p.id=f.pessoa_id WHERE f.pessoa_id=? AND p.ativo AND f.admissao<=CURRENT_DATE AND (f.desligamento IS NULL OR f.desligamento>CURRENT_DATE)",
                id);
    }
}
