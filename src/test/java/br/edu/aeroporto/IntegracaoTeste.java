package br.edu.aeroporto;

import br.edu.aeroporto.config.Configuracao;
import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.dto.TipoRelatorio;
import br.edu.aeroporto.excecao.*;
import br.edu.aeroporto.infraestrutura.jdbc.*;
import br.edu.aeroporto.servico.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Usa exclusivamente o cluster isolado de artifacts na porta 55439; nunca a configuracao local do usuario. */
public final class IntegracaoTeste {
    private static final class Relogio extends Clock {
        private Instant instante=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return instante;}
        void definir(Instant valor){instante=valor;}
    }
    private static final Relogio relogio=new Relogio();
    private static final BancoDados banco=new BancoDados(new Configuracao("jdbc:postgresql://127.0.0.1:55439/sistema_aeroporto","aeroporto_teste","",ZoneId.of("America/Sao_Paulo")));
    private static final AutorizacaoJdbc autorizacao=new AutorizacaoJdbc();
    private static final AuditoriaJdbc auditoria=new AuditoriaJdbc();
    private static final CadastroServico cadastros=new CadastroServico(banco,new CadastroJdbc(),autorizacao,auditoria,relogio);
    private static final PassageiroServico passageiros=new PassageiroServico(banco,new PassageiroJdbc(),auditoria,autorizacao,relogio);
    private static final ReservaServico reservas=new ReservaServico(banco,new ReservaJdbc(),autorizacao,auditoria,relogio);
    private static final PlanejamentoServico planejamento=new PlanejamentoServico(banco,new PlanejamentoJdbc(),autorizacao,auditoria,relogio);
    private static final FinanceiroServico financeiro=new FinanceiroServico(banco,autorizacao,auditoria,relogio);
    private static final AtendimentoServico atendimento=new AtendimentoServico(banco,autorizacao,auditoria,relogio);
    private static final OperacaoServico operacao=new OperacaoServico(banco,autorizacao,auditoria,relogio);
    private static final VooServico voos=new VooServico(banco,autorizacao,auditoria,relogio);
    private static final AdministracaoServico administracao=new AdministracaoServico(banco,autorizacao,auditoria);
    private static final RelatoriosCompletosServico relatorios=new RelatoriosCompletosServico(banco,autorizacao,auditoria);
    private static Sessao admin;
    private static int verificacoes;
    private static final String sufixo=UUID.randomUUID().toString().substring(0,8);
    private static long pais,origem,destino,companhia,modelo,aeronave,rota,voo,tarifa,portao,documento,passageiro,outroPassageiro;
    private static Instant partida,chegada;
    private static int numeroVoo;

    public static void main(String[] args) throws Exception {
        inicializar();cadastros();comercial();atendimento();arquivos();concorrencia();cancelamentos();cenariosAdicionais();regrasComplementares();administracaoEImportacao();
        System.out.println(verificacoes+" verificacoes PostgreSQL aprovadas; banco isolado preservado.");
    }

    private static void inicializar() {
        var autenticacao=new AutenticacaoServico(banco,new UsuarioJdbc(),auditoria);
        var inicializacao=new InicializacaoServico(banco,new InicializacaoJdbc(),auditoria,relogio);
        var dados=new PrimeiroAcesso("Administrador FICTICIO",LocalDate.of(1980,1,1),"teste_admin","Cidade FICTICIA","Teste","Aeroporto FICTICIO","XINI");
        if(!autenticacao.existeUsuario()) inicializacao.criar(dados,"SenhaFicticia!2026".toCharArray());
        admin=autenticacao.entrar("teste_admin","SenhaFicticia!2026".toCharArray());
        verificar(admin.perfis().contains("ADMINISTRADOR"),"Primeiro administrador e login");
        rejeitar(()->inicializacao.criar(dados,"SenhaFicticia!2026".toCharArray()),"Inicializacao nao duplica admin");
        rejeitar(()->autenticacao.entrar("teste_admin","incorreta".toCharArray()),"Login invalido");
        verificar(contar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='aeroporto' AND table_type='BASE TABLE'")==41,"41 tabelas preservadas");
    }

    private static void cadastros() {
        pais=banco.transacao(c->{Sql.executar(c,"INSERT INTO pais(codigo,nome) VALUES ('ZZ','Pais FICTICIO') ON CONFLICT DO NOTHING");return Dados.id(Sql.registro(c,"SELECT id FROM pais WHERE codigo='ZZ'"),"id");});
        long cidade=cad(TipoCadastro.CIDADE,"pais_id",pais,"nome","Cidade FICTICIA "+sufixo,"regiao","");
        origem=cad(TipoCadastro.AEROPORTO,"cidade_id",cidade,"codigo_icao",icao(),"nome","Origem FICTICIA","fuso_horario","UTC");
        destino=cad(TipoCadastro.AEROPORTO,"cidade_id",cidade,"codigo_icao",icao(),"nome","Destino FICTICIO","fuso_horario","UTC");
        long terminal=cad(TipoCadastro.TERMINAL,"aeroporto_id",origem,"codigo","T1","nome","Terminal FICTICIO");
        portao=cad(TipoCadastro.RECURSO,"aeroporto_id",origem,"terminal_id",terminal,"tipo","PORTAO","codigo","P1","situacao","DISPONIVEL","envergadura_max_m","40");
        companhia=cad(TipoCadastro.COMPANHIA,"pais_id",pais,"codigo_icao",icao().substring(1),"nome","Companhia FICTICIA "+sufixo);
        modelo=cad(TipoCadastro.MODELO,"fabricante","FICTICIO","nome",sufixo,"envergadura_m","30","comprimento_pista_min_m","1500");
        aeronave=cad(TipoCadastro.AERONAVE,"modelo_id",modelo,"companhia_id",companhia,"matricula","PT-"+sufixo,"situacao","ATIVA","fabricacao_ano",2025);
        for(int fila=1;fila<=3;fila++) for(String coluna:new String[]{"A","B"}) cad(TipoCadastro.ASSENTO,"aeronave_id",aeronave,"codigo",fila+coluna,"fila",fila,"coluna",coluna,"classe","ECONOMICA","saida_emergencia",false);
        rota=cad(TipoCadastro.ROTA,"origem_id",origem,"destino_id",destino,"distancia_km","1000");
        passageiro=cad(TipoCadastro.PESSOA,"nome","Passageiro FICTICIO "+sufixo,"nascimento","1990-01-01","nacionalidade_id",pais);
        passageiros.vincular(admin,passageiro,"Assistencia ficticia");passageiros.vincular(admin,passageiro,"Assistencia ficticia");
        verificar(contar("SELECT COUNT(*) FROM passageiro WHERE pessoa_id="+passageiro)==1,"Vinculo nao duplica pessoa");
        documento=cad(TipoCadastro.DOCUMENTO,"pessoa_id",passageiro,"pais_emissor_id",pais,"tipo","PASSAPORTE","numero","FICTICIO-"+sufixo,"validade","2099-01-01");
        cad(TipoCadastro.CONTATO,"pessoa_id",passageiro,"tipo","EMAIL","valor",sufixo+"@example.invalid","principal",true);
        passageiros.editar(admin,passageiro,"Passageiro EDITADO FICTICIO "+sufixo,LocalDate.of(1990,1,1),pais,"",true);
        verificar(passageiros.buscar(admin,passageiro).nome().contains("EDITADO"),"Edicao de passageiro");
        outroPassageiro=cad(TipoCadastro.PESSOA,"nome","Outro FICTICIO "+sufixo,"nascimento","1991-01-01");passageiros.vincular(admin,outroPassageiro,"");
        long cargo=cad(TipoCadastro.CARGO,"nome","Tripulacao FICTICIA "+sufixo,"area","TRIPULACAO");
        cad(TipoCadastro.FUNCIONARIO,"pessoa_id",passageiro,"cargo_id",cargo,"aeroporto_base_id",origem,"matricula","DUAL-"+sufixo,"admissao",LocalDate.now().minusDays(1));
        passageiros.editar(admin,passageiro,"Passageiro EDITADO FICTICIO "+sufixo,LocalDate.of(1990,1,1),pais,"",false);
        verificar(contar("SELECT COUNT(*) FROM funcionario f JOIN pessoa p ON p.id=f.pessoa_id WHERE f.pessoa_id="+passageiro+" AND p.ativo")==1,"Inativacao de papel passageiro preserva funcionario");
        passageiros.editar(admin,passageiro,"Passageiro EDITADO FICTICIO "+sufixo,LocalDate.of(1990,1,1),pais,"",true);
        long consulta=administracao.criarUsuario(admin,passageiro,"consulta_"+sufixo,"SenhaFicticia!2026".toCharArray(),Set.of("CONSULTA"));
        var leitor=new AutenticacaoServico(banco,new UsuarioJdbc(),auditoria).entrar("consulta_"+sufixo,"SenhaFicticia!2026".toCharArray());
        rejeitar(()->cadastros.salvar(leitor,TipoCadastro.PAIS,null,mapa("codigo","XY","nome","Proibido")),"Permissoes revalidadas no servico");
        administracao.acesso(admin,consulta,false,Set.of("CONSULTA"));rejeitar(()->relatorios.consultar(leitor,TipoRelatorio.OCUPACAO,1),"Sessao bloqueada perde acesso");
        rejeitar(()->administracao.acesso(admin,admin.usuarioId(),false,Set.of("ADMINISTRADOR")),"Ultimo administrador preservado");
        partida=relogio.instant().plus(Duration.ofHours(3));chegada=partida.plus(Duration.ofHours(2));
        voo=planejamento.programar(admin,programacao(aeronave,partida,chegada));
        tarifa=planejamento.tarifa(admin,voo,null,new Tarifa("FICTICIA","ECONOMICA",new BigDecimal("100"),BigDecimal.ZERO,new BigDecimal("23"),1,true,new BigDecimal("10")),true);
        verificar(contar("SELECT COUNT(*) FROM inventario_assento_voo WHERE voo_id="+voo)==6,"Inventario gerado");
        verificar(contar("SELECT COUNT(*) FROM agenda_aeronave WHERE voo_id="+voo)==1,"Agenda gerada");
        rejeitar(()->planejamento.programar(admin,programacao(aeronave,partida.plusSeconds(60),chegada.plusSeconds(60))),"Conflito e continuidade da frota");
        verificar(contar("SELECT COUNT(*) FROM voo WHERE aeronave_id="+aeronave)==1,"Planejamento rejeitado nao grava parcialmente");
        for(String funcao:new String[]{"COMANDANTE","COPILOTO","COMISSARIO"}) {
            long pessoa=cad(TipoCadastro.PESSOA,"nome",funcao+" FICTICIO "+sufixo,"nascimento","1985-01-01");
            cad(TipoCadastro.FUNCIONARIO,"pessoa_id",pessoa,"cargo_id",cargo,"aeroporto_base_id",origem,"matricula",funcao+"-"+sufixo,"admissao",LocalDate.now().minusDays(1));
            operacao.habilitar(admin,pessoa,modelo,funcao,"FICTICIA-"+sufixo,LocalDate.now().plusYears(1));
            operacao.escalar(admin,pessoa,origem,voo,funcao,partida.minus(Duration.ofMinutes(45)),chegada.plus(Duration.ofMinutes(30)));
            rejeitar(()->operacao.escalar(admin,pessoa,origem,voo,funcao,partida,chegada),"Escala conflitante ou incompleta rejeitada");
        }
        verificar(relatorios.mapa(admin,voo).celulas().length==3,"Mapa matriz integrado ao banco");
    }

    private static void comercial() throws Exception {
        long reserva=reservas.reservar(admin,passageiro,List.of(new PedidoTrecho(passageiro,voo,tarifa,1,"1A",BigDecimal.ZERO)),15);
        long item=numero("SELECT id FROM item_reserva WHERE reserva_id="+reserva);
        verificar(numero("SELECT valor_base FROM item_reserva WHERE id="+item)==100,"Preco copiado");
        long pagamento=financeiro.pagar(admin,reserva,"p-"+sufixo,"PIX",new BigDecimal("100"),"APROVADO");
        verificar(financeiro.pagar(admin,reserva,"p-"+sufixo,"PIX",new BigDecimal("100"),"APROVADO")==pagamento,"Pagamento idempotente");
        rejeitar(()->financeiro.pagar(admin,reserva,"p-"+sufixo,"PIX",new BigDecimal("90"),"APROVADO"),"Idempotencia valida parametros");
        verificar(contar("SELECT COUNT(*) FROM bilhete WHERE item_id="+item)==1,"Bilhete emitido apos cobertura");
        rejeitar(()->planejamento.trocarAeronave(admin,voo,aeronave,"Teste"),"Troca preserva itens historicos");
        long outro=reservas.reservar(admin,outroPassageiro,List.of(new PedidoTrecho(outroPassageiro,voo,tarifa,1,"1B",BigDecimal.ZERO)),1);
        financeiro.pagar(admin,outro,"parcial-"+sufixo,"PIX",new BigDecimal("50"),"APROVADO");
        verificar(contar("SELECT COUNT(*) FROM bilhete b JOIN item_reserva i ON i.id=b.item_id WHERE i.reserva_id="+outro)==0,"Pagamento parcial nao emite bilhete");
        Instant anterior=relogio.instant();relogio.definir(anterior.plusSeconds(61));verificar(reservas.expirarPendencias()>=1,"Expiracao executada");
        verificar(contar("SELECT COUNT(*) FROM ocupacao_assento o JOIN item_reserva i ON i.id=o.item_id WHERE i.reserva_id="+outro+" AND o.liberada_em IS NULL")==0,"Expiracao libera assento");
        verificar(numero("SELECT SUM(valor) FROM reembolso WHERE reserva_id="+outro)==50,"Pagamento parcial expirado gera devolucao");
        relogio.definir(anterior);
    }

    private static void atendimento() {
        long item=numero("SELECT i.id FROM item_reserva i WHERE i.voo_id="+voo+" AND i.passageiro_id="+passageiro);
        rejeitar(()->atendimento.checkin(admin,item,documento),"Estado de voo bloqueia check-in");
        voos.transicao(admin,voo,SituacaoVoo.CHECKIN_ABERTO,"Teste");
        long ci=atendimento.checkin(admin,item,documento);atendimento.cancelarCheckin(admin,item);
        verificar(atendimento.checkin(admin,item,documento)==ci,"Refaz check-in no mesmo registro");
        long mala=atendimento.despachar(admin,item,"M-"+sufixo,new BigDecimal("25"),"DESPACHADA");
        verificar(numero("SELECT taxa_excesso FROM bagagem WHERE id="+mala)==60,"Excesso calculado");
        verificar(atendimento.despachar(admin,item,"M-"+sufixo,new BigDecimal("25"),"DESPACHADA")==mala,"Etiqueta nao duplica despacho");
        atendimento.rastrear(admin,"M-"+sufixo,origem,"INSPECIONADA","Teste");
        rejeitar(()->atendimento.rastrear(admin,"M-"+sufixo,origem,"CARREGADA","Teste"),"Excesso nao pago bloqueia carga");
        long reserva=numero("SELECT reserva_id FROM item_reserva WHERE id="+item);financeiro.pagar(admin,reserva,"bag-"+sufixo,"PIX",new BigDecimal("60"),"APROVADO");
        atendimento.rastrear(admin,"M-"+sufixo,origem,"CARREGADA","Teste");
        long alocacao=operacao.alocar(admin,voo,portao,"PARTIDA",partida.minus(Duration.ofMinutes(45)),partida);
        rejeitar(()->operacao.interditar(admin,portao,partida.minus(Duration.ofMinutes(30)),partida,"Teste"),"Interdicao conflitante rejeitada");
        relogio.definir(partida.minus(Duration.ofMinutes(35)));voos.transicao(admin,voo,SituacaoVoo.EMBARQUE,"Teste");
        String codigo=atendimento.cartao(admin,item).get("codigo_cartao").toString();atendimento.embarcar(admin,codigo,alocacao);
        verificar(contar("SELECT COUNT(*) FROM bilhete WHERE item_id="+item+" AND situacao='UTILIZADO'")==1,"Embarque consome bilhete");
        rejeitar(()->atendimento.embarcar(admin,codigo,alocacao),"Embarque nao se repete");
        relogio.definir(partida);voos.transicao(admin,voo,SituacaoVoo.EM_VOO,"Teste");
        relogio.definir(chegada.plusSeconds(1));planejamento.horarios(admin,voo,partida,chegada,true,"Chegada ficticia");voos.transicao(admin,voo,SituacaoVoo.CONCLUIDO,"Teste");
        atendimento.rastrear(admin,"M-"+sufixo,destino,"DESCARREGADA","Teste");atendimento.rastrear(admin,"M-"+sufixo,destino,"ENTREGUE","Teste");
        verificar(relatorios.bagagem(admin,"M-"+sufixo).size()==5,"Historico de bagagem completo");
        verificar(contar("SELECT COUNT(*) FROM historico_voo WHERE voo_id="+voo+" AND usuario_id IS NULL")==0,"Historico de voo tem autor");
    }

    private static void arquivos() throws Exception {
        var arquivos=new ArquivosServico(banco,autorizacao,auditoria,relogio);
        Path csv=relatorios.exportar(admin,TipoRelatorio.MANIFESTO,true);
        Path copia=arquivos.copiarRelatorio(admin,csv);verificar(Files.mismatch(csv,copia)==-1,"Backup de arquivo verificado");
        Path entrada=Path.of("artifacts","importacao-"+sufixo+".txt");
        Files.writeString(entrada,"nome\tnascimento\tcpf\temail\ttelefone\nFICTICIO\t1990-01-01\t52998224725\tficticio@example.invalid\t\nINVALIDO\t2099-01-01\t123\t\t\n");
        long antes=contar("SELECT COUNT(*) FROM pessoa");rejeitar(()->{try{arquivos.importarPassageiros(admin,entrada);}catch(java.io.IOException e){throw new RuntimeException(e);}},"Importacao invalida rejeitada");
        verificar(contar("SELECT COUNT(*) FROM pessoa")==antes,"Importacao nao grava parcialmente");
        verificar(!arquivos.listar(admin).isEmpty(),"Arquivos disponiveis no menu");
    }

    private static void concorrencia() throws Exception {
        // Nova aeronave para evitar conflito com a localizacao da aeronave concluida.
        long aviao=cad(TipoCadastro.AERONAVE,"modelo_id",modelo,"companhia_id",companhia,"matricula","PC-"+sufixo,"situacao","ATIVA","fabricacao_ano",2025);
        cad(TipoCadastro.ASSENTO,"aeronave_id",aviao,"codigo","1A","fila",1,"coluna","A","classe","ECONOMICA","saida_emergencia",false);
        Instant inicio=relogio.instant().plus(Duration.ofHours(3));long flight=planejamento.programar(admin,programacao(aviao,inicio,inicio.plus(Duration.ofHours(2))));
        long fare=planejamento.tarifa(admin,flight,null,new Tarifa("TESTE","ECONOMICA",new BigDecimal("100"),BigDecimal.ZERO,BigDecimal.ZERO,0,true,BigDecimal.ZERO),true);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var inicioSimultaneo=new CountDownLatch(1);var tarefas=new ArrayList<Future<Boolean>>();
            for(long pessoa:new long[]{passageiro,outroPassageiro}) tarefas.add(executor.submit(()->{inicioSimultaneo.await();try{reservas.reservar(admin,pessoa,List.of(new PedidoTrecho(pessoa,flight,fare,1,"1A",BigDecimal.ZERO)),15);return true;}catch(RegraNegocioException | PersistenciaException esperado){return false;}}));
            inicioSimultaneo.countDown();int sucessos=0;for(var tarefa:tarefas) if(tarefa.get(40,TimeUnit.SECONDS)) sucessos++;
            verificar(sucessos==1,"Dois operadores nao vendem o mesmo assento");
        }
        verificar(contar("SELECT COUNT(*) FROM item_reserva WHERE voo_id="+flight)==1,"Concorrencia nao cria item orfao");
        long reserva=numero("SELECT reserva_id FROM item_reserva WHERE voo_id="+flight);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->financeiro.pagar(admin,reserva,"conc-"+sufixo,"PIX",new BigDecimal("100"),"APROVADO"));
            var b=executor.submit(()->financeiro.pagar(admin,reserva,"conc-"+sufixo,"PIX",new BigDecimal("100"),"APROVADO"));
            verificar(a.get(40,TimeUnit.SECONDS).equals(b.get(40,TimeUnit.SECONDS)),"Pagamento simultaneo idempotente");
        }
        reservas.cancelar(admin,reserva,null,"Cancelamento FICTICIO");long refund=numero("SELECT id FROM reembolso WHERE reserva_id="+reserva);
        financeiro.processarReembolso(admin,refund,true);financeiro.processarReembolso(admin,refund,true);
        verificar(numero("SELECT SUM(valor) FROM reembolso WHERE reserva_id="+reserva+" AND situacao='PROCESSADO'")==100,"Reembolso nao se repete");
        rejeitar(()->financeiro.solicitarReembolso(admin,numero("SELECT id FROM pagamento WHERE reserva_id="+reserva),new BigDecimal("1"),"excedente-"+sufixo,"Teste"),"Reembolso acima do pago rejeitado");
        verificar(contar("SELECT COUNT(*) FROM ocupacao_assento o JOIN item_reserva i ON i.id=o.item_id WHERE i.reserva_id="+reserva+" AND o.liberada_em IS NULL")==0,"Cancelamento libera assento");
    }

    private static void cancelamentos() {
        long aviao=cad(TipoCadastro.AERONAVE,"modelo_id",modelo,"companhia_id",companhia,"matricula","PV-"+sufixo,"situacao","ATIVA","fabricacao_ano",2025);
        cad(TipoCadastro.ASSENTO,"aeronave_id",aviao,"codigo","1A","fila",1,"coluna","A","classe","ECONOMICA","saida_emergencia",false);
        Instant inicio=relogio.instant().plus(Duration.ofHours(3));long flight=planejamento.programar(admin,programacao(aviao,inicio,inicio.plus(Duration.ofHours(2))));
        long fare=planejamento.tarifa(admin,flight,null,new Tarifa("TESTE","ECONOMICA",new BigDecimal("100"),BigDecimal.ZERO,BigDecimal.ZERO,0,false,new BigDecimal("50")),true);
        long reserva=reservas.reservar(admin,outroPassageiro,List.of(new PedidoTrecho(outroPassageiro,flight,fare,1,null,BigDecimal.ZERO)),15);financeiro.pagar(admin,reserva,"cancelvoo-"+sufixo,"PIX",new BigDecimal("100"),"APROVADO");
        rejeitar(()->reservas.cancelar(admin,reserva,null,"Teste"),"Tarifa nao cancelavel respeitada");
        voos.transicao(admin,flight,SituacaoVoo.CANCELADO,"Cancelamento pela companhia FICTICIA");
        verificar(numero("SELECT SUM(valor) FROM reembolso WHERE reserva_id="+reserva)==100,"Cancelamento de voo restitui integralmente");
        verificar(contar("SELECT COUNT(*) FROM agenda_aeronave WHERE voo_id="+flight+" AND ativa")==0,"Cancelamento libera agendas");
        verificar(contar("SELECT COUNT(*) FROM bilhete b JOIN item_reserva i ON i.id=b.item_id WHERE i.reserva_id="+reserva+" AND b.situacao='CANCELADO'")==1,"Cancelamento preserva bilhete anterior");
    }

    private static void cenariosAdicionais() throws Exception {
        Instant base=relogio.instant();
        long aviao=novoAviao("GX",6);
        Instant inicio=base.plus(Duration.ofHours(3)),fim=inicio.plus(Duration.ofHours(2));
        long primeiro=planejamento.programar(admin,programacao(aviao,inicio,fim));
        long reversa=cad(TipoCadastro.ROTA,"origem_id",destino,"destino_id",origem,"distancia_km","1000");
        Instant inicioVolta=fim.plus(Duration.ofHours(2)),fimVolta=inicioVolta.plus(Duration.ofHours(2));
        var p=programacao(aviao,inicioVolta,fimVolta);
        long segundo=planejamento.programar(admin,new ProgramacaoVoo(companhia,reversa,aviao,p.numero(),p.partida(),p.chegada(),p.checkinAbre(),p.checkinFecha(),p.embarqueAbre(),p.embarqueFecha()));
        long fare=planejamento.tarifa(admin,primeiro,null,new Tarifa("GRUPO","ECONOMICA",new BigDecimal("100"),BigDecimal.ZERO,new BigDecimal("23"),1,true,new BigDecimal("10")),true);
        long volta=planejamento.tarifa(admin,segundo,null,new Tarifa("GRUPO","ECONOMICA",new BigDecimal("100"),BigDecimal.ZERO,new BigDecimal("23"),1,true,new BigDecimal("10")),true);
        long comprador=cad(TipoCadastro.PESSOA,"nome","Comprador que nao viaja FICTICIO "+sufixo,"nascimento","1980-01-01");
        var grupo=List.of(new PedidoTrecho(passageiro,primeiro,fare,1,null,BigDecimal.ZERO),new PedidoTrecho(passageiro,segundo,volta,2,null,BigDecimal.ZERO),new PedidoTrecho(outroPassageiro,primeiro,fare,1,null,BigDecimal.ZERO),new PedidoTrecho(outroPassageiro,segundo,volta,2,null,BigDecimal.ZERO));
        long reserva=reservas.reservar(admin,comprador,grupo,15);
        verificar(numero("SELECT comprador_id FROM reserva WHERE id="+reserva)==comprador && contar("SELECT COUNT(*) FROM item_reserva WHERE reserva_id="+reserva)==4,"Grupo e conexoes preservam comprador e passageiros");
        financeiro.pagar(admin,reserva,"grupo-"+sufixo,"PIX",new BigDecimal("400"),"APROVADO");
        long item=numero("SELECT MIN(id) FROM item_reserva WHERE reserva_id="+reserva);
        reservas.cancelar(admin,reserva,item,"Parcial FICTICIO");
        verificar(texto("SELECT situacao FROM reserva WHERE id="+reserva).equals("PARCIAL_CANCELADA"),"Cancelamento parcial recalcula cabecalho");
        verificar(numero("SELECT SUM(valor) FROM reembolso WHERE reserva_id="+reserva)==90,"Multa vendida aplicada");
        long reembolso=numero("SELECT id FROM reembolso WHERE reserva_id="+reserva);financeiro.processarReembolso(admin,reembolso,false);
        rejeitar(()->financeiro.solicitarReembolso(admin,numero("SELECT id FROM pagamento WHERE reserva_id="+reserva),new BigDecimal("91"),"multa-"+sufixo,"Teste"),"Solicitacao manual nao ignora multa");
        long pedido=financeiro.solicitarReembolso(admin,numero("SELECT id FROM pagamento WHERE reserva_id="+reserva),new BigDecimal("90"),"manual-"+sufixo,"Teste");financeiro.processarReembolso(admin,pedido,true);
        long pendente=reservas.reservar(admin,passageiro,List.of(new PedidoTrecho(passageiro,primeiro,fare,1,null,BigDecimal.ZERO)),15);
        long novoItem=numero("SELECT id FROM item_reserva WHERE reserva_id="+pendente);
        long remarcada=reservas.remarcar(admin,novoItem,new PedidoTrecho(passageiro,primeiro,fare,1,null,BigDecimal.ZERO),15);
        verificar(texto("SELECT situacao FROM item_reserva WHERE id="+novoItem).equals("CANCELADO") && remarcada!=pendente,"Remarcacao preserva item e reserva anteriores");
        reservas.cancelar(admin,remarcada,null,"Teste");
        long quantidade=contar("SELECT COUNT(*) FROM reserva");
        rejeitar(()->reservas.reservar(admin,passageiro,List.of(new PedidoTrecho(passageiro,primeiro,fare,1,"3A",BigDecimal.ZERO),new PedidoTrecho(passageiro,primeiro,fare,2,"3B",BigDecimal.ZERO)),15),"Duplicidade de passageiro falha durante gravacao");
        verificar(contar("SELECT COUNT(*) FROM reserva")==quantidade,"Falha no segundo item desfaz toda a transacao");
        rejeitar(()->reservas.reservar(admin,passageiro,List.of(new PedidoTrecho(passageiro,primeiro,fare,1,null,new BigDecimal("11"))),15),"Desconto fora da politica rejeitado");
        long inventario=numero("SELECT id FROM inventario_assento_voo WHERE voo_id="+primeiro+" AND codigo='3A'");planejamento.bloquearAssento(admin,inventario,true);
        rejeitar(()->reservas.reservar(admin,passageiro,List.of(new PedidoTrecho(passageiro,primeiro,fare,1,"3A",BigDecimal.ZERO)),15),"Assento bloqueado nao vendido");planejamento.bloquearAssento(admin,inventario,false);
        long executiva=planejamento.tarifa(admin,primeiro,null,new Tarifa("EXECUTIVA","EXECUTIVA",new BigDecimal("200"),BigDecimal.ZERO,BigDecimal.ZERO,0,true,BigDecimal.ZERO),true);
        rejeitar(()->reservas.reservar(admin,passageiro,List.of(new PedidoTrecho(passageiro,primeiro,executiva,1,"3A",BigDecimal.ZERO)),15),"Classe de assento incompativel rejeitada");
        planejamento.tarifa(admin,primeiro,fare,new Tarifa("GRUPO","ECONOMICA",new BigDecimal("200"),BigDecimal.ZERO,new BigDecimal("23"),1,true,new BigDecimal("20")),true);
        verificar(numero("SELECT valor_base FROM item_reserva WHERE id="+item)==100,"Edicao de tarifa preserva preco e condicoes vendidos");
        long livre=novoAviao("TX",2);long semVendas=planejamento.programar(admin,programacao(livre,base.plus(Duration.ofHours(15)),base.plus(Duration.ofHours(17))));
        long troca=novoAviao("TY",3);planejamento.trocarAeronave(admin,semVendas,troca,"Teste sem vendas");
        verificar(contar("SELECT COUNT(*) FROM inventario_assento_voo WHERE voo_id="+semVendas)==3,"Troca sem vendas recria inventario compativel");
        var atual=banco.consultar(c->Sql.registro(c,"SELECT * FROM voo WHERE id=?",semVendas));
        Instant novaPartida=Dados.instante(atual,"partida_prevista").plus(Duration.ofHours(1)),novaChegada=Dados.instante(atual,"chegada_prevista").plus(Duration.ofHours(1));
        planejamento.reprogramar(admin,semVendas,new ProgramacaoVoo(companhia,rota,troca,atual.get("numero").toString(),novaPartida,novaChegada,Dados.instante(atual,"checkin_abre").plus(Duration.ofHours(1)),Dados.instante(atual,"checkin_fecha").plus(Duration.ofHours(1)),Dados.instante(atual,"embarque_abre").plus(Duration.ofHours(1)),Dados.instante(atual,"embarque_fecha").plus(Duration.ofHours(1))),"Teste");
        long alocacao=operacao.alocar(admin,semVendas,portao,"PARTIDA",novaPartida.minus(Duration.ofMinutes(45)),novaPartida);
        planejamento.horarios(admin,semVendas,novaPartida.plus(Duration.ofMinutes(20)),novaChegada.plus(Duration.ofMinutes(20)),false,"Atraso FICTICIO");
        verificar(banco.consultar(c->Dados.instante(Sql.registro(c,"SELECT inicio FROM alocacao_recurso WHERE id=?",alocacao),"inicio")).equals(novaPartida.minus(Duration.ofMinutes(25))),"Atraso atualiza agenda de recurso");
        verificar(banco.consultar(c->Dados.instante(Sql.registro(c,"SELECT inicio FROM agenda_aeronave WHERE voo_id=?",semVendas),"inicio")).equals(novaPartida.minus(Duration.ofMinutes(25))),"Atraso atualiza agenda de aeronave");
        verificar(relatorios.resumoAtrasos(admin).get("maximo").compareTo(new BigDecimal("20"))>=0,"Relatorio de atrasos calculado");
        long pista=cad(TipoCadastro.RECURSO,"aeroporto_id",origem,"tipo","PISTA","codigo","CURTA","situacao","DISPONIVEL","comprimento_m","500");
        rejeitar(()->operacao.alocar(admin,semVendas,pista,"PARTIDA",novaPartida,novaPartida.plusSeconds(60)),"Pista incompativel rejeitada");
        for(String tipo:new String[]{"POSICAO","ESTEIRA","BALCAO"}) cad(TipoCadastro.RECURSO,"aeroporto_id",origem,"tipo",tipo,"codigo","FICTICIO","situacao","DISPONIVEL");
        long impedimento=operacao.interditar(admin,pista,base,base.plusSeconds(60),"Interdicao FICTICIA");operacao.liberarAlocacao(admin,impedimento);
        verificar(contar("SELECT COUNT(*) FROM alocacao_recurso WHERE id="+impedimento+" AND NOT ativa")==1,"Interdicao encerrada preserva registro");
        long manutencao=operacao.manutencao(admin,novoAviao("MX",1),admin.funcionario().id(),"INSPECAO","FICTICIA",base.minusSeconds(60),base.plusSeconds(60),BigDecimal.ZERO);
        operacao.situacaoManutencao(admin,manutencao,"EM_EXECUCAO");operacao.situacaoManutencao(admin,manutencao,"CONCLUIDA");
        verificar(texto("SELECT situacao FROM manutencao_aeronave WHERE id="+manutencao).equals("CONCLUIDA"),"Manutencao completa preserva historico");
        long antes=contar("SELECT COUNT(*) FROM manutencao_aeronave");
        rejeitar(()->operacao.manutencao(admin,troca,admin.funcionario().id(),"PREVENTIVA","FICTICIA",novaPartida,novaChegada,BigDecimal.ZERO),"Manutencao nao sobrepoe voo");verificar(contar("SELECT COUNT(*) FROM manutencao_aeronave")==antes,"Manutencao rejeitada desfaz registros");
        for(String tipo:new String[]{"ABASTECIMENTO","LIMPEZA","CATERING","BAGAGEM","REBOQUE"}) {
            long solo=operacao.servicoSolo(admin,semVendas,origem,admin.funcionario().id(),tipo,BigDecimal.ONE,"un",BigDecimal.ZERO);
            operacao.situacaoSolo(admin,solo,"EM_EXECUCAO");operacao.situacaoSolo(admin,solo,"CONCLUIDO");
        }
        verificar(contar("SELECT COUNT(*) FROM servico_solo WHERE voo_id="+semVendas+" AND situacao='CONCLUIDO'")==5,"Cinco tipos de servico de solo integrados");
        long ocorrencia=operacao.ocorrencia(admin,origem,semVendas,"TECNICA","BAIXA","FICTICIA");operacao.encerrarOcorrencia(admin,ocorrencia);
        verificar(contar("SELECT COUNT(*) FROM ocorrencia_operacional WHERE id="+ocorrencia+" AND encerrada_em IS NOT NULL")==1,"Ocorrencia aberta e encerrada");
        for(TipoRelatorio tipo:TipoRelatorio.values()) { relatorios.consultar(admin,tipo,1);relatorios.exportar(admin,tipo,true); }
        verificar(!relatorios.painel(admin,origem,inicio.atZone(ZoneOffset.UTC).toLocalDate(),true).isEmpty(),"Painel de partidas por aeroporto");
        verificar(!relatorios.painel(admin,destino,fim.atZone(ZoneOffset.UTC).toLocalDate(),false).isEmpty(),"Painel de chegadas por aeroporto");
        bagagensEmConexao(reserva,primeiro,segundo,inicioVolta,base);
        var demo=new DadosFicticiosServico(banco,autorizacao,auditoria,relogio);var dados=demo.criar(admin,sufixo);
        verificar(dados.equals(demo.criar(admin,sufixo)),"Demonstracao nao duplica dados");
        long demoVoo=Dados.id(dados,"voo"),demoPax=Dados.id(dados,"passageiro"),demoTarifa=Dados.id(dados,"tarifa");
        long demoReserva=reservas.reservar(admin,demoPax,List.of(new PedidoTrecho(demoPax,demoVoo,demoTarifa,1,null,BigDecimal.ZERO)),15);
        for(int i=0;i<25;i++) financeiro.pagar(admin,demoReserva,"recusa-"+sufixo+"-"+i,"CARTAO",new BigDecimal("120"),"RECUSADO");
        Path csv=relatorios.exportar(admin,TipoRelatorio.PAGAMENTOS,true);
        verificar(Files.readAllLines(csv).size()==contar("SELECT COUNT(*) FROM pagamento")+1,"Exportacao completa ultrapassa primeira pagina");
        financeiro.pagar(admin,demoReserva,"demo-"+sufixo,"PIX",new BigDecimal("120"),"APROVADO");
        long demoItem=numero("SELECT id FROM item_reserva WHERE reserva_id="+demoReserva);
        voos.transicao(admin,demoVoo,SituacaoVoo.CHECKIN_ABERTO,"Teste");atendimento.checkin(admin,demoItem,Dados.id(dados,"documento"));
        long mala=atendimento.despachar(admin,demoItem,"CANC-"+sufixo,new BigDecimal("25"),"DESPACHADA");atendimento.cancelarCheckin(admin,demoItem);
        verificar(contar("SELECT COUNT(*) FROM reembolso WHERE reserva_id="+demoReserva)==0,"Cancelar excesso nao pago nao devolve dinheiro da passagem");
        atendimento.checkin(admin,demoItem,Dados.id(dados,"documento"));
        Instant saida=banco.consultar(c->Dados.instante(Sql.registro(c,"SELECT partida_prevista FROM voo WHERE id=?",demoVoo),"partida_prevista"));
        relogio.definir(saida.minus(Duration.ofMinutes(30)));voos.transicao(admin,demoVoo,SituacaoVoo.EMBARQUE,"Teste");
        relogio.definir(saida);voos.transicao(admin,demoVoo,SituacaoVoo.EM_VOO,"Teste");
        verificar(texto("SELECT situacao FROM item_reserva WHERE id="+demoItem).equals("NAO_COMPARECEU"),"Fechamento identifica passageiro ausente");
        cancelamentoAposEmbarque();
        falhasEDeadlock(primeiro,segundo);
        testarTerminal();
    }

    private static void bagagensEmConexao(long reserva,long primeiro,long segundo,Instant volta,Instant base) {
        long doc=cad(TipoCadastro.DOCUMENTO,"pessoa_id",outroPassageiro,"pais_emissor_id",pais,"tipo","PASSAPORTE","numero","CONEXAO-"+sufixo,"validade","2099-01-01");
        long itemId=numero("SELECT id FROM item_reserva WHERE reserva_id="+reserva+" AND passageiro_id="+outroPassageiro+" AND voo_id="+primeiro);
        long segundoItem=numero("SELECT id FROM item_reserva WHERE reserva_id="+reserva+" AND passageiro_id="+outroPassageiro+" AND voo_id="+segundo);
        voos.transicao(admin,primeiro,SituacaoVoo.CHECKIN_ABERTO,"Conexao FICTICIA");atendimento.checkin(admin,itemId,doc);
        String primeiraTag="CON1-"+sufixo;long primeiraMala=atendimento.despachar(admin,itemId,primeiraTag,new BigDecimal("22"),"DESPACHADA");
        atendimento.rastrear(admin,primeiraTag,origem,"EXTRAVIADA","Extravio FICTICIO");atendimento.rastrear(admin,primeiraTag,origem,"RETIRADA","Recuperada FICTICIA");
        verificar(texto("SELECT situacao FROM bagagem WHERE id="+primeiraMala).equals("RETIRADA") && relatorios.bagagem(admin,primeiraTag).size()==3,"Extravio e retirada preservam rastreio");
        relogio.definir(volta.minus(Duration.ofHours(2)));voos.transicao(admin,segundo,SituacaoVoo.CHECKIN_ABERTO,"Conexao FICTICIA");atendimento.checkin(admin,segundoItem,doc);
        long segundaMala=atendimento.despachar(admin,segundoItem,"CON2-"+sufixo,new BigDecimal("22"),"DESPACHADA");
        verificar(numero("SELECT voo_id FROM bagagem b JOIN check_in ci ON ci.id=b.checkin_id WHERE b.id="+segundaMala)==segundo && segundaMala!=primeiraMala,"Despacho em conexao recebe nova etiqueta e vinculo ao trecho");
        relogio.definir(base);
    }

    private static void cancelamentoAposEmbarque() {
        var dados=new DadosFicticiosServico(banco,autorizacao,auditoria,relogio).criar(admin,"C"+sufixo.substring(1));
        long flight=Dados.id(dados,"voo"),pax=Dados.id(dados,"passageiro");
        long reserva=reservas.reservar(admin,pax,List.of(new PedidoTrecho(pax,flight,Dados.id(dados,"tarifa"),1,null,BigDecimal.ZERO)),15);
        financeiro.pagar(admin,reserva,"emb-cancel-"+sufixo,"PIX",new BigDecimal("120"),"APROVADO");
        long item=numero("SELECT id FROM item_reserva WHERE reserva_id="+reserva);
        voos.transicao(admin,flight,SituacaoVoo.CHECKIN_ABERTO,"Teste");atendimento.checkin(admin,item,Dados.id(dados,"documento"));
        Instant saida=banco.consultar(c->Dados.instante(Sql.registro(c,"SELECT partida_prevista FROM voo WHERE id=?",flight),"partida_prevista"));
        long aviao=numero("SELECT aeronave_id FROM voo WHERE id="+flight);
        long manutencao=operacao.manutencao(admin,aviao,admin.funcionario().id(),"CORRETIVA","Atraso FICTICIO",relogio.instant().minusSeconds(60),relogio.instant().plusSeconds(60),BigDecimal.ZERO);
        operacao.situacaoManutencao(admin,manutencao,"EM_EXECUCAO");
        relogio.definir(saida.minus(Duration.ofMinutes(30)));voos.transicao(admin,flight,SituacaoVoo.EMBARQUE,"Teste");
        atendimento.embarcar(admin,atendimento.cartao(admin,item).get("codigo_cartao").toString(),Dados.id(dados,"alocacao"));
        rejeitar(()->reservas.cancelar(admin,reserva,null,"Teste"),"Passageiro nao cancela item ja embarcado");
        relogio.definir(saida.minus(Duration.ofMinutes(10)));
        rejeitar(()->voos.transicao(admin,flight,SituacaoVoo.EM_VOO,"Teste"),"Manutencao em execucao impede decolagem");
        rejeitar(()->operacao.situacaoManutencao(admin,manutencao,"CONCLUIDA"),"Manutencao atrasada nao oculta conflito com agenda de voo");
        voos.transicao(admin,flight,SituacaoVoo.CANCELADO,"Cancelamento apos embarque FICTICIO");
        verificar(texto("SELECT situacao FROM item_reserva WHERE id="+item).equals("CANCELADO") && texto("SELECT situacao FROM bilhete WHERE item_id="+item).equals("CANCELADO"),"Cancelamento antes da partida invalida passagem ja embarcada");
        verificar(contar("SELECT COUNT(*) FROM embarque e JOIN check_in ci ON ci.id=e.checkin_id WHERE ci.item_id="+item)==1,"Cancelamento preserva registro de embarque");
        verificar(numero("SELECT SUM(valor) FROM reembolso WHERE reserva_id="+reserva)==120 && contar("SELECT COUNT(*) FROM ocorrencia_operacional WHERE voo_id="+flight)==1,"Cancelamento gera devolucao integral e ocorrencia");
        operacao.situacaoManutencao(admin,manutencao,"CONCLUIDA");
        verificar(texto("SELECT situacao FROM aeronave WHERE id="+aviao).equals("ATIVA"),"Manutencao conclui apos resolucao do conflito");
    }

    private static void falhasEDeadlock(long primeiro,long segundo) throws Exception {
        long antes=contar("SELECT COUNT(*) FROM evento_auditoria");
        rejeitar(()->banco.transacao(c->{Sql.executar(c,"INSERT INTO evento_auditoria(acao,entidade) VALUES ('TESTE_ROLLBACK','teste')");throw new java.sql.SQLException("Falha injetada de conexao","08006");}),"Falha JDBC e traduzida");
        verificar(contar("SELECT COUNT(*) FROM evento_auditoria")==antes,"Falha JDBC desfaz alteracoes anteriores");
        var barreira=new CountDownLatch(2);var tentativas=new java.util.concurrent.atomic.AtomicInteger();
        try(var executor=Executors.newFixedThreadPool(2)) {
            var tarefas=new ArrayList<Future<Boolean>>();
            for(boolean ordem:new boolean[]{true,false}) {
                var rodada=new java.util.concurrent.atomic.AtomicInteger();
                tarefas.add(executor.submit(()->banco.transacaoRepetivel(c->{
                    int atual=rodada.incrementAndGet();tentativas.incrementAndGet();
                    Sql.registro(c,"SELECT id FROM voo WHERE id=? FOR UPDATE",ordem?primeiro:segundo);
                    if(atual==1) { barreira.countDown();try{if(!barreira.await(10,TimeUnit.SECONDS)) throw new AssertionError("Barreira de deadlock");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);} }
                    Sql.registro(c,"SELECT id FROM voo WHERE id=? FOR UPDATE",ordem?segundo:primeiro);return true;
                })));
            }
            for(var tarefa:tarefas) verificar(tarefa.get(40,TimeUnit.SECONDS),"Transacao recuperada de deadlock real");
        }
        verificar(tentativas.get()>=3,"Deadlock provocou repeticao da transacao completa");
    }

    private static void testarTerminal() throws Exception {
        var processo=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"-Dfile.encoding=UTF-8","-cp","target/classes;lib/*","br.edu.aeroporto.Main");
        processo.environment().put("AEROPORTO_DB_URL","jdbc:postgresql://127.0.0.1:55439/sistema_aeroporto");processo.environment().put("AEROPORTO_DB_USUARIO","aeroporto_teste");processo.environment().put("AEROPORTO_DB_SENHA","FICTICIO-cluster-trust");
        processo.redirectErrorStream(true);var filho=processo.start();
        try(var entrada=filho.outputWriter()) { entrada.write("teste_admin\nSenhaFicticia!2026\n16\n4\n"+voo+"\n17\n2\n0\n"); }
        String saida= new String(filho.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        verificar(filho.waitFor(30,TimeUnit.SECONDS) && filho.exitValue()==0 && saida.contains("Importacao") && saida.contains("1A:O"),"Login, menus e matriz executados por entrada real do terminal");
    }

    private static long novoAviao(String prefixo,int capacidade) {
        long id=cad(TipoCadastro.AERONAVE,"modelo_id",modelo,"companhia_id",companhia,"matricula",prefixo+"-"+sufixo,"situacao","ATIVA","fabricacao_ano",2025);
        for(int i=0;i<capacidade;i++) { int fila=i/2+1;String coluna=i%2==0?"A":"B";cad(TipoCadastro.ASSENTO,"aeronave_id",id,"codigo",fila+coluna,"fila",fila,"coluna",coluna,"classe","ECONOMICA","saida_emergencia",false); }return id;
    }

    private static void regrasComplementares() throws Exception {
        Instant base=relogio.instant();
        long cargo=cad(TipoCadastro.CARGO,"nome","Equipe SOLO FICTICIA "+sufixo,"area","OPERACAO");
        long pessoa=cad(TipoCadastro.PESSOA,"nome","Equipe SOLO FICTICIA "+sufixo,"nascimento","1985-01-01");
        cad(TipoCadastro.FUNCIONARIO,"pessoa_id",pessoa,"cargo_id",cargo,"aeroporto_base_id",origem,"matricula","SOLO-"+sufixo,"admissao",LocalDate.now().minusDays(1));
        Instant inicio=base.plus(Duration.ofHours(2)),fim=inicio.plus(Duration.ofHours(2));
        rejeitar(()->operacao.escalar(admin,pessoa,destino,null,"SOLO",inicio,fim),"Primeira jornada inicia na base do funcionario");
        operacao.escalar(admin,pessoa,origem,null,"SOLO",inicio,fim);
        rejeitar(()->operacao.escalar(admin,pessoa,origem,null,"SOLO",fim.plus(Duration.ofHours(11)),fim.plus(Duration.ofHours(13))),"Descanso inferior a 12h rejeitado");
        rejeitar(()->operacao.escalar(admin,pessoa,destino,null,"SOLO",fim.plus(Duration.ofHours(13)),fim.plus(Duration.ofHours(15))),"Jornada seguinte exige deslocamento continuo");
        operacao.escalar(admin,pessoa,origem,null,"SOLO",fim.plus(Duration.ofHours(13)),fim.plus(Duration.ofHours(15)));
        verificar(contar("SELECT COUNT(*) FROM escala_funcionario WHERE funcionario_id="+pessoa)==2,"Jornadas com descanso e local corretos persistidas");
        rejeitar(()->operacao.escalar(admin,pessoa,origem,null,"SOLO",fim.plus(Duration.ofHours(30)),fim.plus(Duration.ofHours(45))),"Jornada superior a 14h rejeitada");
        long aviao=novoAviao("RV",2);Instant partida=base.plus(Duration.ofHours(5)),chegada=partida.plus(Duration.ofHours(2));
        long primeiro=planejamento.programar(admin,programacao(aviao,partida,chegada));
        rejeitar(()->planejamento.programar(admin,programacao(aviao,chegada.plus(Duration.ofHours(3)),chegada.plus(Duration.ofHours(5)))),"Aeronave nao reaparece na origem sem deslocamento");
        long reversa=numero("SELECT id FROM rota WHERE origem_id="+destino+" AND destino_id="+origem+" LIMIT 1");
        var curto=programacao(aviao,chegada.plus(Duration.ofMinutes(60)),chegada.plus(Duration.ofHours(3)));
        rejeitar(()->planejamento.programar(admin,new ProgramacaoVoo(companhia,reversa,aviao,curto.numero(),curto.partida(),curto.chegada(),curto.checkinAbre(),curto.checkinFecha(),curto.embarqueAbre(),curto.embarqueFecha())),"Intervalo da aeronave inferior a 75 minutos rejeitado");
        var retorno=programacao(aviao,chegada.plus(Duration.ofHours(3)),chegada.plus(Duration.ofHours(5)));
        long segundo=planejamento.programar(admin,new ProgramacaoVoo(companhia,reversa,aviao,retorno.numero(),retorno.partida(),retorno.chegada(),retorno.checkinAbre(),retorno.checkinFecha(),retorno.embarqueAbre(),retorno.embarqueFecha()));
        verificar(contar("SELECT COUNT(*) FROM voo WHERE aeronave_id="+aviao)==2,"Aeronave conserva continuidade em ida e volta");
        rejeitar(()->planejamento.programar(admin,programacao(aviao,base.plusSeconds(60),base.plus(Duration.ofHours(2)))),"Insercao antes de voo futuro respeita proximo aeroporto");
        long tarifa=planejamento.tarifa(admin,primeiro,null,new Tarifa("COMPLEMENTAR","ECONOMICA",new BigDecimal("100"),BigDecimal.ZERO,new BigDecimal("23"),1,true,BigDecimal.ZERO),true);
        long reserva=reservas.reservar(admin,passageiro,List.of(new PedidoTrecho(passageiro,primeiro,tarifa,1,null,BigDecimal.ZERO)),15);
        long pendente=financeiro.pagar(admin,reserva,"pend-"+sufixo,"PIX",new BigDecimal("100"),"PENDENTE");
        verificar(contar("SELECT COUNT(*) FROM bilhete b JOIN item_reserva i ON i.id=b.item_id WHERE i.reserva_id="+reserva)==0,"Pagamento pendente nao confirma nem emite bilhete");
        long cancelar=financeiro.pagar(admin,reserva,"pend-cancel-"+sufixo,"PIX",BigDecimal.ONE,"PENDENTE");financeiro.cancelarPagamento(admin,cancelar);
        verificar(texto("SELECT situacao FROM pagamento WHERE id="+cancelar).equals("CANCELADO"),"Pagamento pendente cancelado preserva registro");
        verificar(financeiro.pagar(admin,reserva,"pend-"+sufixo,"PIX",new BigDecimal("100"),"APROVADO")==pendente,"Aprovacao reutiliza pagamento pendente idempotente");
        rejeitar(()->planejamento.tarifa(admin,primeiro,tarifa,new Tarifa("COMPLEMENTAR","EXECUTIVA",new BigDecimal("100"),BigDecimal.ZERO,new BigDecimal("23"),1,true,BigDecimal.ZERO),true),"Classe de tarifa vendida preservada");
        relogio.definir(partida.minus(Duration.ofHours(3)));voos.transicao(admin,primeiro,SituacaoVoo.CHECKIN_ABERTO,"Teste");
        long item=numero("SELECT id FROM item_reserva WHERE reserva_id="+reserva);atendimento.checkin(admin,item,documento);
        atendimento.despachar(admin,item,"PEC1-"+sufixo,new BigDecimal("22"),"DESPACHADA");
        long segunda=atendimento.despachar(admin,item,"PEC2-"+sufixo,BigDecimal.ONE,"DESPACHADA");
        verificar(numero("SELECT taxa_excesso FROM bagagem WHERE id="+segunda)==100,"Peca adicional cobra taxa sem duplicar franquia de peso");
        rejeitar(()->atendimento.despachar(admin,item,"PESO-"+sufixo,new BigDecimal("33"),"DESPACHADA"),"Limite de peso por peca despachada respeitado");
        relogio.definir(partida.minus(Duration.ofMinutes(30)));
        rejeitar(()->voos.transicao(admin,primeiro,SituacaoVoo.EMBARQUE,"Teste"),"Tripulacao minima ausente impede abertura de embarque");
        voos.transicao(admin,primeiro,SituacaoVoo.CANCELADO,"Teste");
        long livre=novoAviao("IN",1);var valores=new LinkedHashMap<>(banco.consultar(c->Sql.registro(c,"SELECT modelo_id,companhia_id,matricula,situacao,fabricacao_ano FROM aeronave WHERE id=?",livre)));
        valores.put("fabricacao_ano",2024);cadastros.salvar(admin,TipoCadastro.AERONAVE,livre,valores);valores.put("situacao","INATIVA");cadastros.salvar(admin,TipoCadastro.AERONAVE,livre,valores);
        verificar(numero("SELECT fabricacao_ano FROM aeronave WHERE id="+livre)==2024 && contar("SELECT COUNT(*) FROM assento_aeronave WHERE aeronave_id="+livre)==1,"Edicao e inativacao da aeronave preservam configuracao");
        rejeitar(()->planejamento.programar(admin,programacao(livre,relogio.instant().plus(Duration.ofHours(3)),relogio.instant().plus(Duration.ofHours(5)))),"Aeronave inativa nao admite voo");
        long vazio=novoAviao("RZ",2);Instant proxima=relogio.instant().plus(Duration.ofHours(3));long flight=planejamento.programar(admin,programacao(vazio,proxima,proxima.plus(Duration.ofHours(2))));
        long fare=planejamento.tarifa(admin,flight,null,new Tarifa("RASCUNHO","ECONOMICA",new BigDecimal("100"),BigDecimal.ZERO,BigDecimal.ZERO,0,true,BigDecimal.ZERO),true);
        testarRascunho(flight,fare);
    }

    private static void testarRascunho(long voo,long tarifa) throws Exception {
        long antes=contar("SELECT COUNT(*) FROM reserva");
        String trecho=passageiro+"\n"+voo+"\n"+tarifa+"\n1\n\n0\n";
        String outro=outroPassageiro+"\n"+voo+"\n"+tarifa+"\n1\n\n0\n";
        var processo=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"-Dfile.encoding=UTF-8","-cp","target/classes;lib/*","br.edu.aeroporto.Main");
        processo.environment().put("AEROPORTO_DB_URL","jdbc:postgresql://127.0.0.1:55439/sistema_aeroporto");processo.environment().put("AEROPORTO_DB_USUARIO","aeroporto_teste");processo.environment().put("AEROPORTO_DB_SENHA","FICTICIO-cluster-trust");
        processo.redirectErrorStream(true);var filho=processo.start();
        try(var entrada=filho.outputWriter()) { entrada.write("teste_admin\nSenhaFicticia!2026\n12\n1\n"+passageiro+"\n1\n"+trecho+"4\n1\n2\n1\n"+outro+"3\n1\n1\n"+trecho+"5\n1\n"+outro+"6\n15\n0\n"); }
        String saida=new String(filho.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        verificar(filho.waitFor(30,TimeUnit.SECONDS) && filho.exitValue()==0 && saida.contains("Reserva: ") && contar("SELECT COUNT(*) FROM reserva")==antes+1,"ArrayList: adicionar, listar pagina, editar, remover, limpar e gravar pelo terminal");
        verificar(contar("SELECT COUNT(*) FROM item_reserva WHERE voo_id="+voo+" AND passageiro_id="+outroPassageiro)==1,"Rascunho grava somente o trecho final selecionado");
    }

    private static void administracaoEImportacao() throws Exception {
        long usuario=numero("SELECT id FROM usuario_sistema WHERE funcionario_id="+passageiro);
        administracao.acesso(admin,usuario,true,Set.of("CONSULTA"));
        var auth=new AutenticacaoServico(banco,new UsuarioJdbc(),auditoria);var sessao=auth.entrar("consulta_"+sufixo,"SenhaFicticia!2026".toCharArray());
        administracao.alterarSenha(sessao,"SenhaFicticia!2026".toCharArray(),"NovaSenhaFicticia!2026".toCharArray());
        rejeitar(()->auth.entrar("consulta_"+sufixo,"SenhaFicticia!2026".toCharArray()),"Senha antiga nao autentica apos alteracao");
        verificar(auth.entrar("consulta_"+sufixo,"NovaSenhaFicticia!2026".toCharArray()).usuarioId()==usuario,"Alteracao de senha autenticada");
        rejeitar(()->{try{relatorios.exportar(sessao,TipoRelatorio.MANIFESTO,true);}catch(java.io.IOException e){throw new RuntimeException(e);}},"Perfil consulta nao exporta manifesto privado");
        long cargo=cad(TipoCadastro.CARGO,"nome","Cargo encerrado FICTICIO "+sufixo,"area","ATENDIMENTO");
        var cargos=new LinkedHashMap<>(banco.consultar(c->Sql.registro(c,"SELECT nome,area,ativo FROM cargo WHERE id=?",cargo)));cargos.put("ativo",false);cadastros.salvar(admin,TipoCadastro.CARGO,cargo,cargos);
        verificar(contar("SELECT COUNT(*) FROM cargo WHERE id="+cargo+" AND NOT ativo")==1,"Cargo editado e inativado sem exclusao");
        long pessoa=cad(TipoCadastro.PESSOA,"nome","Funcionario desligado FICTICIO "+sufixo,"nascimento","1980-01-01");
        cargos.put("ativo",true);cadastros.salvar(admin,TipoCadastro.CARGO,cargo,cargos);
        var funcionario=mapa("pessoa_id",pessoa,"cargo_id",cargo,"aeroporto_base_id",origem,"companhia_id",null,"matricula","END-"+sufixo,"admissao",LocalDate.now().minusDays(1),"desligamento",null);
        cadastros.salvar(admin,TipoCadastro.FUNCIONARIO,null,funcionario);funcionario.put("desligamento",LocalDate.now());cadastros.salvar(admin,TipoCadastro.FUNCIONARIO,pessoa,funcionario);
        verificar(contar("SELECT COUNT(*) FROM funcionario WHERE pessoa_id="+pessoa+" AND desligamento IS NOT NULL")==1,"Funcionario inativado por desligamento preserva identidade");
        var arquivos=new ArquivosServico(banco,autorizacao,auditoria,relogio);
        Path entrada=Path.of("artifacts","valida-"+sufixo+".txt");String cpf=cpf();
        Files.writeString(entrada,"nome\tnascimento\tcpf\temail\ttelefone\nFICTICIO IMPORTADO\t1990-01-01\t"+cpf+"\timportado@example.invalid\t\n");
        verificar(arquivos.importarPassageiros(admin,entrada)==1,"Importacao valida persiste passageiro e auditoria");
        long antes=contar("SELECT COUNT(*) FROM pessoa");
        Files.writeString(entrada,"nome\tnascimento\tcpf\temail\ttelefone\nNOVO FICTICIO\t1990-01-01\t"+cpf()+"\t\t\nDUPLICADO FICTICIO\t1990-01-01\t"+cpf+"\t\t\n");
        rejeitar(()->{try{arquivos.importarPassageiros(admin,entrada);}catch(java.io.IOException e){throw new RuntimeException(e);}},"Importacao com documento existente falha no banco");
        verificar(contar("SELECT COUNT(*) FROM pessoa")==antes,"Importacao com falha no segundo registro reverte o primeiro");
        long antesDaMigracao=contar("SELECT COUNT(*) FROM pessoa");
        banco.transacao(c->{try(var st=c.createStatement()){st.execute(Files.readString(Path.of("sql/migracoes/001_cadastros_ativos.sql")));}catch(java.io.IOException e){throw new RuntimeException(e);}return null;});
        verificar(contar("SELECT COUNT(*) FROM pessoa")==antesDaMigracao,"Migracao repetida preserva dados existentes");
    }

    private static String cpf() {
        var r=new java.security.SecureRandom();var codigo=new StringBuilder();for(int i=0;i<9;i++) codigo.append(r.nextInt(10));
        for(int tamanho=9;tamanho<=10;tamanho++){int soma=0;for(int i=0;i<tamanho;i++) soma+=(codigo.charAt(i)-'0')*(tamanho+1-i);int digito=11-soma%11;codigo.append(digito>=10?0:digito);}return codigo.toString();
    }

    private static String texto(String sql) { return banco.consultar(c->Sql.unico(c,sql,r->r.getString(1)).orElse("")); }

    private static ProgramacaoVoo programacao(long aviao,Instant inicio,Instant fim) { return new ProgramacaoVoo(companhia,rota,aviao,sufixo.substring(0,5).toUpperCase(Locale.ROOT)+String.format("%03d",numeroVoo++),inicio,fim,inicio.minus(Duration.ofHours(4)),inicio.minus(Duration.ofMinutes(45)),inicio.minus(Duration.ofMinutes(40)),inicio.minus(Duration.ofMinutes(10))); }
    private static String icao() {
        while(true) {
            StringBuilder b=new StringBuilder("X");new java.security.SecureRandom().ints(3,0,26).forEach(n->b.append((char)('A'+n)));String codigo=b.toString();
            boolean usado=banco.consultar(c->!Sql.listar(c,"SELECT id FROM aeroporto WHERE codigo_icao=? UNION ALL SELECT id FROM companhia_aerea WHERE codigo_icao=?",r->r.getLong(1),codigo,codigo.substring(1)).isEmpty());
            if(!usado) return codigo;
        }
    }
    private static long cad(TipoCadastro tipo,Object... valores) {
        var dados=new LinkedHashMap<String,Object>();for(var campo:tipo.campos()) dados.put(campo.nome(),campo.tipo().equals("b")?true:null);dados.putAll(mapa(valores));return cadastros.salvar(admin,tipo,null,dados);
    }
    private static Map<String,Object> mapa(Object... valores) { var dados=new LinkedHashMap<String,Object>();for(int i=0;i<valores.length;i+=2) dados.put(valores[i].toString(),valores[i+1]);return dados; }
    private static long contar(String sql) { return numero(sql); }
    private static long numero(String sql) { return banco.consultar(c->Sql.unico(c,sql,r->r.getLong(1)).orElse(0L)); }
    private static void verificar(boolean condicao,String nome) { verificacoes++;if(!condicao) throw new AssertionError(nome);System.out.println("OK "+nome); }
    private static void rejeitar(Runnable acao,String nome) { try{acao.run();}catch(RegraNegocioException | PersistenciaException esperado){verificar(true,nome);return;}throw new AssertionError(nome); }
}
