package br.edu.aeroporto.servico;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CadastroServico {
    private final BancoDados banco;
    private final CadastroJdbc repositorio;
    private final AutorizacaoJdbc autorizacao;
    private final AuditoriaJdbc auditoria;
    private final Clock relogio;

    public CadastroServico(
            BancoDados banco,
            CadastroJdbc repositorio,
            AutorizacaoJdbc autorizacao,
            AuditoriaJdbc auditoria,
            Clock relogio) {
        this.banco = banco;
        this.repositorio = repositorio;
        this.autorizacao = autorizacao;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public List<Map<String, Object>> listar(Sessao sessao, TipoCadastro tipo, int pagina) {
        Dados.exigir(pagina > 0 && pagina <= 100000, "Pagina invalida.");
        return banco.consultar(
                c -> {
                    permitir(c, sessao, tipo);
                    return repositorio.listar(c, tipo, pagina);
                });
    }

    public long salvar(Sessao sessao, TipoCadastro tipo, Long id, Map<String, Object> entrada) {
        LinkedHashMap<String, Object> valores = new LinkedHashMap<String, Object>();
        for (String nome : entrada.keySet()) {
            boolean conhecido = false;
            for (TipoCadastro.Campo campo : tipo.campos()) {
                if (campo.nome().equals(nome)) {
                    conhecido = true;
                    break;
                }
            }
            Dados.exigir(conhecido, "Campo desconhecido.");
        }
        for (TipoCadastro.Campo campo : tipo.campos()) {
            Object valor = entrada.get(campo.nome());
            valores.put(campo.nome(), campo.converter(valor == null ? null : valor.toString()));
        }
        return banco.transacao(
                c -> {
                    permitir(c, sessao, tipo);
                    Sql.unico(
                            c,
                            "SELECT pg_advisory_xact_lock(hashtext('aeroporto.operacao'))",
                            r -> true);
                    Map<String, Object> anterior =
                            id == null ? Map.of() : repositorio.buscar(c, tipo, id);
                    validar(c, tipo, id, valores, anterior);
                    long salvo = repositorio.salvar(c, tipo, id, valores);
                    auditoria.registrar(
                            c,
                            sessao.usuarioId(),
                            id == null ? "CADASTRAR" : "EDITAR",
                            tipo.tabela(),
                            salvo);
                    return salvo;
                });
    }

    private void permitir(Connection c, Sessao s, TipoCadastro t) throws SQLException {
        if (t == TipoCadastro.PESSOA || t == TipoCadastro.DOCUMENTO || t == TipoCadastro.CONTATO)
            autorizacao.exigirAtendimento(c, s);
        else autorizacao.exigirAdministrador(c, s);
    }

    private void validar(
            Connection c,
            TipoCadastro tipo,
            Long id,
            Map<String, Object> v,
            Map<String, Object> antes)
            throws SQLException {
        switch (tipo) {
            case CIDADE:
                {
                    if (v.get("regiao") == null) v.put("regiao", "");
                    break;
                }
            case PAIS:
                Dados.exigir(
                        Dados.texto(v, "codigo").matches("[A-Z]{2}"),
                        "Codigo de pais: duas letras maiusculas.");
                break;
            case AEROPORTO:
                {
                    ZoneId.of(Dados.texto(v, "fuso_horario"));
                    Dados.exigir(
                            Dados.texto(v, "codigo_icao").matches("[A-Z]{4}"), "ICAO invalido.");
                    if (v.get("codigo_iata") != null)
                        Dados.exigir(
                                Dados.texto(v, "codigo_iata").matches("[A-Z]{3}"),
                                "IATA invalido.");

                    break;
                }
            case PESSOA:
                {
                    Validacao.nascimento((LocalDate) v.get("nascimento"), LocalDate.now(relogio));
                    if (id != null && Boolean.FALSE.equals(v.get("ativo"))) {
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM item_reserva WHERE passageiro_id=? AND situacao IN ('PENDENTE','CONFIRMADO')",
                                        id),
                                "Pessoa tem passagens ativas.");
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM usuario_sistema u JOIN usuario_perfil up ON up.usuario_id=u.id JOIN perfil_acesso p ON p.id=up.perfil_id WHERE u.funcionario_id=? AND u.ativo AND p.nome='ADMINISTRADOR'",
                                        id),
                                "Bloqueie o acesso administrativo antes de inativar a pessoa.");
                    }

                    break;
                }
            case DOCUMENTO:
                {
                    Dados.exigir(
                            id == null || Dados.id(antes, "pessoa_id") == Dados.id(v, "pessoa_id"),
                            "Nao transfira documento entre pessoas.");
                    if ("CPF".equals(v.get("tipo"))) {
                        v.put("numero", Validacao.cpf(Dados.texto(v, "numero")));
                        Dados.exigir(
                                existe(
                                        c,
                                        "SELECT 1 FROM pais WHERE id=? AND codigo='BR'",
                                        Dados.id(v, "pais_emissor_id")),
                                "CPF requer emissor BR.");
                    }
                    if (v.get("validade") != null)
                        Dados.exigir(
                                !((LocalDate) v.get("validade")).isBefore(LocalDate.now(relogio)),
                                "Documento ja vencido.");

                    break;
                }
            case CONTATO:
                {
                    Dados.exigir(
                            id == null || Dados.id(antes, "pessoa_id") == Dados.id(v, "pessoa_id"),
                            "Nao transfira contato.");
                    String valor = Dados.texto(v, "valor");
                    Dados.exigir(
                            "EMAIL".equals(v.get("tipo"))
                                    ? valor.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                                    : valor.matches("\\+?[0-9]{8,15}"),
                            "Contato invalido.");
                    if (Boolean.TRUE.equals(v.get("principal")))
                        Sql.executar(
                                c,
                                "UPDATE contato_pessoa SET principal=false WHERE pessoa_id=? AND tipo=?",
                                Dados.id(v, "pessoa_id"),
                                v.get("tipo"));

                    break;
                }
            case FUNCIONARIO:
                {
                    Dados.exigir(
                            id == null || id == Dados.id(v, "pessoa_id"),
                            "Nao altere a identidade do funcionario.");
                    Dados.exigir(
                            existe(
                                    c,
                                    "SELECT 1 FROM pessoa WHERE id=? AND ativo",
                                    Dados.id(v, "pessoa_id")),
                            "Pessoa inativa.");
                    Dados.exigir(
                            existe(
                                    c,
                                    "SELECT 1 FROM cargo WHERE id=? AND ativo",
                                    Dados.id(v, "cargo_id")),
                            "Cargo inativo.");
                    if (id != null
                            && (!v.get("cargo_id").equals(antes.get("cargo_id"))
                                    || !v.get("aeroporto_base_id")
                                            .equals(antes.get("aeroporto_base_id")))) {
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM escala_funcionario WHERE funcionario_id=? AND ativa AND fim>CURRENT_TIMESTAMP",
                                        id),
                                "Libere as escalas futuras antes de mudar cargo ou base.");
                    }
                    if (v.get("desligamento") != null
                            && !((LocalDate) v.get("desligamento"))
                                    .isAfter(LocalDate.now(relogio))) {
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM escala_funcionario WHERE funcionario_id=? AND ativa AND fim>CURRENT_TIMESTAMP",
                                        Dados.id(v, "pessoa_id")),
                                "Funcionario tem escalas futuras.");
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM usuario_sistema u JOIN usuario_perfil up ON up.usuario_id=u.id JOIN perfil_acesso p ON p.id=up.perfil_id WHERE u.funcionario_id=? AND u.ativo AND p.nome='ADMINISTRADOR'",
                                        Dados.id(v, "pessoa_id")),
                                "Bloqueie o acesso administrativo antes do desligamento.");
                    }

                    break;
                }
            case AERONAVE:
                {
                    if (id != null
                            && (!"ATIVA".equals(v.get("situacao"))
                                    || !v.get("modelo_id").equals(antes.get("modelo_id"))
                                    || !v.get("companhia_id").equals(antes.get("companhia_id")))) {
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM agenda_aeronave WHERE aeronave_id=? AND ativa AND fim>CURRENT_TIMESTAMP",
                                        id),
                                "Libere a agenda da aeronave antes da alteracao.");
                    }

                    break;
                }
            case ASSENTO:
                {
                    Dados.exigir(
                            Dados.texto(v, "coluna").matches("[A-Z]{1,2}")
                                    && Dados.texto(v, "codigo")
                                            .equals(v.get("fila") + Dados.texto(v, "coluna")),
                            "Codigo de assento deve combinar fila e coluna.");
                    if (id != null)
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM inventario_assento_voo WHERE assento_id=?",
                                        id),
                                "Assento ja copiado para voo; preserve o mapa historico.");
                    Dados.exigir(
                            !existe(
                                    c,
                                    "SELECT 1 FROM voo WHERE aeronave_id=? AND situacao NOT IN ('CONCLUIDO','CANCELADO')",
                                    Dados.id(v, "aeronave_id")),
                            "Configure assentos antes de programar voos.");

                    break;
                }
            case ROTA:
                {
                    if (id != null
                            && (!v.get("origem_id").equals(antes.get("origem_id"))
                                    || !v.get("destino_id").equals(antes.get("destino_id"))))
                        Dados.exigir(
                                !existe(c, "SELECT 1 FROM voo WHERE rota_id=?", id),
                                "Rota ja utilizada; cadastre outro trecho para preservar historico.");
                    if (id != null && Boolean.FALSE.equals(v.get("ativa")))
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM voo WHERE rota_id=? AND situacao NOT IN ('CONCLUIDO','CANCELADO')",
                                        id),
                                "Rota tem voos ativos.");

                    break;
                }
            case RECURSO:
                {
                    if (id != null)
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM alocacao_recurso WHERE recurso_id=? AND ativa AND fim>CURRENT_TIMESTAMP",
                                        id),
                                "Recurso tem alocacoes futuras; use interdicao na agenda.");

                    break;
                }
            case MODELO:
                {
                    if (id != null
                            && (!v.get("envergadura_m").equals(antes.get("envergadura_m"))
                                    || !v.get("comprimento_pista_min_m")
                                            .equals(antes.get("comprimento_pista_min_m")))) {
                        Dados.exigir(
                                !existe(
                                        c,
                                        "SELECT 1 FROM agenda_aeronave ag JOIN aeronave a ON a.id=ag.aeronave_id WHERE a.modelo_id=? AND ag.ativa AND ag.fim>CURRENT_TIMESTAMP",
                                        id),
                                "Modelo tem aeronaves em agenda ativa.");
                    }

                    break;
                }
            default:
                {
                    break;
                }
        }
        if (id != null && Boolean.FALSE.equals(v.get("ativo"))) {
            String consulta;
            switch (tipo) {
                case COMPANHIA:
                    consulta =
                            "SELECT 1 FROM voo WHERE companhia_id=? AND situacao NOT IN ('CONCLUIDO','CANCELADO')";
                    break;
                case AEROPORTO:
                    consulta =
                            "SELECT 1 FROM voo v JOIN rota r ON r.id=v.rota_id WHERE ? IN (r.origem_id,r.destino_id) AND v.situacao NOT IN ('CONCLUIDO','CANCELADO')";
                    break;
                case TERMINAL:
                    consulta =
                            "SELECT 1 FROM alocacao_recurso a JOIN recurso_aeroportuario r ON r.id=a.recurso_id WHERE r.terminal_id=? AND a.ativa AND a.fim>CURRENT_TIMESTAMP";
                    break;
                case CARGO:
                    consulta =
                            "SELECT 1 FROM funcionario WHERE cargo_id=? AND (desligamento IS NULL OR desligamento>CURRENT_DATE)";
                    break;
                case MODELO:
                    consulta = "SELECT 1 FROM aeronave WHERE modelo_id=? AND situacao<>'INATIVA'";
                    break;
                default:
                    consulta = null;
                    break;
            }
            if (consulta != null)
                Dados.exigir(!existe(c, consulta, id), "Cadastro tem operacoes ativas.");
        }
    }

    private boolean existe(Connection c, String sql, long id) throws SQLException {
        return !Sql.listar(c, sql, r -> true, id).isEmpty();
    }
}
