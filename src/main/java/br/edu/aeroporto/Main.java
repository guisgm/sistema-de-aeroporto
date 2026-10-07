package br.edu.aeroporto;

import br.edu.aeroporto.cli.MenuOperacional;
import br.edu.aeroporto.cli.MenuPrincipal;
import br.edu.aeroporto.cli.Terminal;
import br.edu.aeroporto.config.Configuracao;
import br.edu.aeroporto.dominio.PrimeiroAcesso;
import br.edu.aeroporto.dominio.Sessao;
import br.edu.aeroporto.excecao.PersistenciaException;
import br.edu.aeroporto.excecao.RegraNegocioException;
import br.edu.aeroporto.infraestrutura.jdbc.AuditoriaJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.AutorizacaoJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.BancoDados;
import br.edu.aeroporto.infraestrutura.jdbc.CadastroJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.ConsultasJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.InicializacaoJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.PassageiroJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.PlanejamentoJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.ReservaJdbc;
import br.edu.aeroporto.infraestrutura.jdbc.Sql;
import br.edu.aeroporto.infraestrutura.jdbc.UsuarioJdbc;
import br.edu.aeroporto.servico.AdministracaoServico;
import br.edu.aeroporto.servico.ArquivosServico;
import br.edu.aeroporto.servico.AtendimentoServico;
import br.edu.aeroporto.servico.AutenticacaoServico;
import br.edu.aeroporto.servico.CadastroServico;
import br.edu.aeroporto.servico.ConsultaServico;
import br.edu.aeroporto.servico.DadosFicticiosServico;
import br.edu.aeroporto.servico.FinanceiroServico;
import br.edu.aeroporto.servico.InicializacaoServico;
import br.edu.aeroporto.servico.OperacaoServico;
import br.edu.aeroporto.servico.PassageiroServico;
import br.edu.aeroporto.servico.PlanejamentoServico;
import br.edu.aeroporto.servico.RelatorioServico;
import br.edu.aeroporto.servico.RelatoriosCompletosServico;
import br.edu.aeroporto.servico.ReservaServico;
import br.edu.aeroporto.servico.VooServico;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() {}

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--ajuda")) {
            System.out.println(
                    """
                    Sistema de aeroporto — Java 21 / PostgreSQL
                    .\\executar.ps1 -Inicializar  Cria o primeiro administrador e seu aeroporto-base no Brasil.
                    .\\executar.ps1              Abre o login e os menus.
                    Configuração: config/application.properties (copie o .example e preencha).
                    O banco deve ter a estrutura criada por sql/criar_banco.sql.
                    .\\executar.ps1 -Migrar       Aplica a migracao aditiva sem recriar tabelas.
                    .\\executar.ps1 -Diagnostico  Confere conexao, schema e existencia de usuarios.
                    Cadastros, voos, reservas, pagamentos simulados, check-in, bagagens, embarque e operacao.
                    """);
            return;
        }
        if (args.length > 1
                || args.length == 1
                        && !java.util.Set.of("--inicializar", "--migrar", "--diagnostico")
                                .contains(args[0])) {
            System.err.println("Argumento desconhecido. Use --ajuda.");
            System.exit(1);
        }
        configurarLog();
        try (Terminal terminal = new Terminal()) {
            Configuracao configuracao = Configuracao.carregar();
            BancoDados banco = new BancoDados(configuracao);
            AuditoriaJdbc auditoria = new AuditoriaJdbc();
            AutorizacaoJdbc autorizacao = new AutorizacaoJdbc();
            Clock relogio = Clock.system(configuracao.fuso());
            if (args.length == 1 && args[0].equals("--migrar")) {
                String migracao =
                        Files.readString(Path.of("sql/migracoes/001_cadastros_ativos.sql"));
                banco.transacao(
                        c -> {
                            long pessoas =
                                    Sql.unicoNumero(c, "SELECT COUNT(*) FROM pessoa").orElse(0L);
                            long reservasExistentes =
                                    Sql.unicoNumero(c, "SELECT COUNT(*) FROM reserva").orElse(0L);
                            try (Statement comando = c.createStatement()) {
                                comando.execute(migracao);
                            }
                            System.out.println(
                                    "Migracao aditiva aplicada. Pessoas preservadas: "
                                            + pessoas
                                            + "; reservas: "
                                            + reservasExistentes);
                            return null;
                        });
                return;
            }
            if (args.length == 1 && args[0].equals("--diagnostico")) {
                banco.consultar(
                        c -> {
                            System.out.println("Conexao PostgreSQL estabelecida.");
                            System.out.println(
                                    "Tabelas: "
                                            + Sql.unicoNumero(
                                                            c,
                                                            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='aeroporto' AND table_type='BASE TABLE'")
                                                    .orElse(0L));
                            System.out.println(
                                    "Usuarios existentes: "
                                            + Sql.unicoNumero(
                                                            c,
                                                            "SELECT COUNT(*) FROM usuario_sistema")
                                                    .orElse(0L));
                            System.out.println(
                                    "Migracao aplicada: "
                                            + Sql.unicoBooleano(
                                                            c,
                                                            "SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='aeroporto' AND table_name='passageiro' AND column_name='ativo')")
                                                    .orElse(false));
                            return null;
                        });
                return;
            }
            AutenticacaoServico autenticacao =
                    new AutenticacaoServico(banco, new UsuarioJdbc(), auditoria);
            boolean migracaoAplicada =
                    banco.consultar(
                            c ->
                                    Sql.unicoBooleano(
                                                    c,
                                                    "SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='aeroporto' AND table_name='passageiro' AND column_name='ativo')")
                                            .orElse(false));
            if (!migracaoAplicada)
                throw new RegraNegocioException(
                        "Aplique a migracao aditiva com .\\executar.ps1 -Migrar antes de entrar.");
            if (args.length == 1) {
                if (autenticacao.existeUsuario())
                    throw new RegraNegocioException(
                            "O primeiro acesso já existe. Execute sem -Inicializar.");
                inicializar(
                        terminal,
                        new InicializacaoServico(
                                banco, new InicializacaoJdbc(), auditoria, relogio));
                return;
            }
            if (!autenticacao.existeUsuario()) {
                System.out.println(
                        "Nenhum operador cadastrado. Execute .\\executar.ps1 -Inicializar para criar o primeiro acesso.");
                return;
            }
            Sessao sessao = entrar(terminal, autenticacao);
            if (sessao == null) return;
            System.out.println("Operador: " + sessao.funcionario().nome());
            PassageiroServico passageiros =
                    new PassageiroServico(
                            banco, new PassageiroJdbc(), auditoria, autorizacao, relogio);
            ConsultaServico consultas =
                    new ConsultaServico(banco, new ConsultasJdbc(), autorizacao);
            ReservaServico reservas =
                    new ReservaServico(banco, new ReservaJdbc(), autorizacao, auditoria, relogio);
            reservas.expirarPendencias();
            MenuOperacional operacional =
                    new MenuOperacional(
                            terminal,
                            sessao,
                            passageiros,
                            new CadastroServico(
                                    banco, new CadastroJdbc(), autorizacao, auditoria, relogio),
                            new AdministracaoServico(banco, autorizacao, auditoria),
                            new PlanejamentoServico(
                                    banco, new PlanejamentoJdbc(), autorizacao, auditoria, relogio),
                            reservas,
                            new FinanceiroServico(banco, autorizacao, auditoria, relogio),
                            new AtendimentoServico(banco, autorizacao, auditoria, relogio),
                            new OperacaoServico(banco, autorizacao, auditoria, relogio),
                            new VooServico(banco, autorizacao, auditoria, relogio),
                            new RelatoriosCompletosServico(banco, autorizacao, auditoria),
                            new ArquivosServico(banco, autorizacao, auditoria, relogio),
                            new DadosFicticiosServico(banco, autorizacao, auditoria, relogio));
            new MenuPrincipal(
                            terminal,
                            sessao,
                            passageiros,
                            consultas,
                            new RelatorioServico(),
                            configuracao.fuso(),
                            operacional)
                    .executar();
        } catch (Terminal.FimDaEntrada fim) {
            System.out.println("Entrada encerrada.");
        } catch (RegraNegocioException | IllegalArgumentException | DateTimeException erro) {
            System.err.println(erro.getMessage());
            System.exit(1);
        } catch (PersistenciaException erro) {
            System.err.println(erro.getMessage());
            if (erro.getCause() instanceof SQLException) {
                SQLException sql = (SQLException) erro.getCause();
                LOG.log(Level.SEVERE, "Falha na inicialização. SQLSTATE={0}", sql.getSQLState());
            }
            System.exit(1);
        } catch (IOException erro) {
            System.err.println(
                    "Não foi possível ler a configuração. Confira o arquivo e as permissões.");
            LOG.log(Level.SEVERE, "Falha de configuração: {0}", erro.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static Sessao entrar(Terminal terminal, AutenticacaoServico autenticacao) {
        for (int tentativa = 0; tentativa < 3; tentativa++) {
            String login = terminal.ler("Login");
            try {
                return autenticacao.entrar(login, terminal.senha("Senha"));
            } catch (RegraNegocioException erro) {
                System.out.println(erro.getMessage());
            }
        }
        System.out.println("Limite de tentativas desta execução atingido.");
        return null;
    }

    private static void inicializar(Terminal terminal, InicializacaoServico inicializacao) {
        System.out.println(
                "PRIMEIRO ACESSO — criação do administrador e de seu aeroporto-base no Brasil.");
        PrimeiroAcesso dados =
                new PrimeiroAcesso(
                        terminal.ler("Nome do administrador"),
                        terminal.data("Nascimento"),
                        terminal.ler("Login"),
                        terminal.ler("Cidade do aeroporto"),
                        terminal.ler("Estado/região"),
                        terminal.ler("Nome do aeroporto"),
                        terminal.ler("Código ICAO do aeroporto (4 letras)"));
        inicializacao.criar(dados, terminal.senha("Senha (mínimo 12 caracteres)"));
        System.out.println(
                "Primeiro acesso criado. Execute novamente sem -Inicializar para entrar.");
    }

    private static void configurarLog() {
        try {
            Files.createDirectories(Path.of("logs"));
            FileHandler arquivo = new FileHandler("logs/sistema-%g.log", 1_000_000, 3, true);
            arquivo.setEncoding("UTF-8");
            arquivo.setFormatter(new SimpleFormatter());
            Logger.getLogger("br.edu.aeroporto").addHandler(arquivo);
        } catch (IOException | SecurityException erro) {
            System.err.println(
                    "Log em arquivo indisponível; mensagens técnicas serão exibidas no terminal.");
        }
    }
}
