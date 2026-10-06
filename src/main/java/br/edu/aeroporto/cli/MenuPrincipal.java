package br.edu.aeroporto.cli;

import br.edu.aeroporto.dominio.CadastroPassageiro;
import br.edu.aeroporto.dominio.Passageiro;
import br.edu.aeroporto.dominio.Sessao;
import br.edu.aeroporto.dto.VooResumo;
import br.edu.aeroporto.excecao.PersistenciaException;
import br.edu.aeroporto.excecao.RegraNegocioException;
import br.edu.aeroporto.relatorio.ExportadorCsv;
import br.edu.aeroporto.relatorio.ExportadorTxt;
import br.edu.aeroporto.relatorio.ExportadorVoos;
import br.edu.aeroporto.servico.ConsultaServico;
import br.edu.aeroporto.servico.PassageiroServico;
import br.edu.aeroporto.servico.RelatorioServico;
import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MenuPrincipal {
    private static final Logger LOG = Logger.getLogger(MenuPrincipal.class.getName());
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm XXX");
    private final Terminal terminal;
    private final Sessao sessao;
    private final PassageiroServico passageiros;
    private final ConsultaServico consultas;
    private final RelatorioServico relatorios;
    private final ZoneId fuso;
    private final MenuOperacional operacional;

    public MenuPrincipal(Terminal terminal, Sessao sessao, PassageiroServico passageiros,
                         ConsultaServico consultas, RelatorioServico relatorios, ZoneId fuso, MenuOperacional operacional) {
        this.terminal = terminal;
        this.sessao = sessao;
        this.passageiros = passageiros;
        this.consultas = consultas;
        this.relatorios = relatorios;
        this.fuso = fuso;
        this.operacional=operacional;
    }

    public void executar() {
        boolean continuar = true;
        do {
            System.out.println("""

                    SISTEMA DE AEROPORTO
                    1 — Cadastrar passageiro
                    2 — Buscar passageiros por nome
                    3 — Consultar passageiro por id
                    4 — Consultar voos por data
                    5 — Consultar mapa de assentos
                    6 — Consultar reserva por localizador
                    7 — Exportar página de voos (TXT/CSV)
                    8 — Editar passageiros e vincular papeis
                    9 — Cadastros administrativos
                    10 — Usuarios, acesso e senha
                    11 — Planejamento e situacao de voos
                    12 — Reservas, conexoes e remarcacao
                    13 — Pagamentos e reembolsos simulados
                    14 — Check-in, bagagens e embarque
                    15 — Equipe, recursos e operacao
                    16 — Paineis e relatorios completos
                    17 — Importacao e copia de arquivos
                    18 — Dados FICTICIOS de demonstracao
                    0 — Sair
                    """);
            try {
                int opcaoAtual=terminal.inteiro("Opção",0,18);
                switch (opcaoAtual) {
                    case 0 -> continuar = false;
                    case 1 -> cadastrarPassageiro();
                    case 2 -> buscarPassageiros();
                    case 3 -> mostrar(passageiros.buscar(sessao, terminal.id("Id do passageiro")));
                    case 4 -> mostrarVoos(consultarVoos());
                    case 5 -> mostrarAssentos();
                    case 6 -> mostrarReserva();
                    case 7 -> exportarVoos();
                    default -> operacional.executar(opcaoAtual);
                }
            } catch (RegraNegocioException erro) {
                System.out.println(erro.getMessage());
            } catch (IllegalArgumentException | java.time.DateTimeException erro) {
                System.out.println("Formato de entrada invalido. Confira o campo informado.");
            } catch (PersistenciaException erro) {
                System.out.println(erro.getMessage());
                if (erro.getCause() instanceof SQLException sql) {
                    LOG.log(Level.WARNING, "Falha JDBC no menu. SQLSTATE={0}", sql.getSQLState());
                }
            } catch (IOException erro) {
                System.out.println("Não foi possível acessar o arquivo. Confira a pasta e as permissões.");
                LOG.log(Level.WARNING, "Falha de exportação: {0}", erro.getClass().getSimpleName());
            }
        } while (continuar);
    }

    private void cadastrarPassageiro() {
        var cadastro = new CadastroPassageiro(terminal.ler("Nome"), terminal.data("Nascimento"),
                terminal.ler("CPF (opcional)"), terminal.ler("E-mail (opcional)"), terminal.ler("Telefone (opcional)"));
        Passageiro salvo = passageiros.cadastrar(sessao, cadastro);
        System.out.println("Passageiro cadastrado com id " + salvo.id() + ".");
        mostrar(salvo);
    }

    private void buscarPassageiros() {
        String nome = terminal.ler("Parte do nome (vazio para listar)");
        int pagina = terminal.inteiro("Página (20 registros por página)", 1, 100_000);
        List<Passageiro> resultado = passageiros.buscar(sessao, nome, pagina);
        if (resultado.isEmpty()) System.out.println("Nenhum passageiro nesta página.");
        resultado.forEach(MenuPrincipal::mostrar);
    }

    private static void mostrar(Passageiro passageiro) {
        System.out.printf("%d | %s | %s | Nascimento: %s | %s%n", passageiro.id(),
                passageiro.codigoCliente(), passageiro.nome(), passageiro.nascimento(), passageiro.ativa() ? "Ativo" : "Inativo");
    }

    private List<VooResumo> consultarVoos() {
        System.out.println("O filtro usa o dia de partida no fuso " + fuso + ". Exibição usa o fuso de cada aeroporto.");
        return consultas.voos(sessao, terminal.data("Dia de partida"), fuso,
                terminal.inteiro("Página (20 registros por página)", 1, 100_000));
    }

    private static void mostrarVoos(List<VooResumo> voos) {
        if (voos.isEmpty()) System.out.println("Nenhum voo nesta página.");
        for (VooResumo voo : voos) {
            System.out.printf("%d | %s%s | %s → %s | Partida: %s | Chegada: %s | %s%s%n", voo.id(),
                    voo.companhia(), voo.numero(), voo.origem(), voo.destino(),
                    DATA.format(voo.partida().atZoneSameInstant(voo.fusoOrigem())),
                    DATA.format(voo.chegada().atZoneSameInstant(voo.fusoDestino())),
                    voo.situacao(), voo.atrasado() ? " — ATRASADO" : "");
        }
    }

    private void mostrarAssentos() {
        var assentos = consultas.assentos(sessao, terminal.id("Id do voo"));
        if (assentos.isEmpty()) System.out.println("Voo inexistente ou sem inventário de assentos.");
        for (var assento : assentos) {
            String situacao = assento.bloqueado() ? "Bloqueado" : assento.ocupado() ? "Ocupado" : "Livre";
            System.out.printf("%s | %s | %s%n", assento.codigo(), assento.classe(), situacao);
        }
    }

    private void mostrarReserva() {
        var reserva = consultas.reserva(sessao, terminal.ler("Localizador"));
        System.out.printf("Reserva %s | %s | Expira: %s%n", reserva.localizador(), reserva.situacao(),
                DATA.format(reserva.expiraEm().atZoneSameInstant(fuso)));
        for (var trecho : reserva.trechos()) {
            System.out.printf("%s | Trecho %d | %s | %s | Assento: %s | R$ %s%n", trecho.passageiro(),
                    trecho.ordem(), trecho.voo(), trecho.situacao(), trecho.assento() == null ? "—" : trecho.assento(), trecho.valor().toPlainString());
        }
        System.out.println("Total original das passagens, incluindo itens cancelados: R$ " + reserva.totalOriginalPassagens().toPlainString());
    }

    private void exportarVoos() throws IOException {
        List<VooResumo> voos = consultarVoos();
        if (voos.isEmpty()) throw new RegraNegocioException("Não há voos nesta página para exportar.");
        ExportadorVoos exportador = switch (terminal.inteiro("Formato: 1 TXT / 2 CSV", 1, 2)) {
            case 1 -> new ExportadorTxt();
            case 2 -> new ExportadorCsv();
            default -> throw new IllegalStateException("Formato inválido.");
        };
        System.out.println("Relatório salvo em " + relatorios.exportar(voos, exportador));
    }
}
