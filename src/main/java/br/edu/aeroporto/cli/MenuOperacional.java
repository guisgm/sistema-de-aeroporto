package br.edu.aeroporto.cli;

import br.edu.aeroporto.dominio.*;
import br.edu.aeroporto.dto.TipoRelatorio;
import br.edu.aeroporto.servico.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

public final class MenuOperacional {
    private final Terminal t;
    private final Sessao s;
    private final PassageiroServico passageiros;
    private final CadastroServico cadastros;
    private final AdministracaoServico acesso;
    private final PlanejamentoServico planejamento;
    private final ReservaServico reservas;
    private final FinanceiroServico financeiro;
    private final AtendimentoServico atendimento;
    private final OperacaoServico operacao;
    private final VooServico voos;
    private final RelatoriosCompletosServico relatorios;
    private final ArquivosServico arquivos;
    private final DadosFicticiosServico demo;

    public MenuOperacional(Terminal t,Sessao s,PassageiroServico passageiros,CadastroServico cadastros,AdministracaoServico acesso,
            PlanejamentoServico planejamento,ReservaServico reservas,FinanceiroServico financeiro,AtendimentoServico atendimento,
            OperacaoServico operacao,VooServico voos,RelatoriosCompletosServico relatorios,ArquivosServico arquivos,DadosFicticiosServico demo) {
        this.t=t;this.s=s;this.passageiros=passageiros;this.cadastros=cadastros;this.acesso=acesso;this.planejamento=planejamento;
        this.reservas=reservas;this.financeiro=financeiro;this.atendimento=atendimento;this.operacao=operacao;this.voos=voos;
        this.relatorios=relatorios;this.arquivos=arquivos;this.demo=demo;
    }

    public void executar(int opcao) throws IOException {
        switch(opcao) {
            case 8 -> passageiros();case 9 -> cadastros();case 10 -> acesso();case 11 -> planejamento();
            case 12 -> reservas();case 13 -> financeiro();case 14 -> atendimento();case 15 -> operacao();
            case 16 -> relatorios();case 17 -> arquivos();case 18 -> { String chave=t.ler("Cenario (vazio: PRINCIPAL)");System.out.println(demo.criar(s,chave.isBlank()?"PRINCIPAL":chave)); }
            default -> throw new IllegalArgumentException("Modulo desconhecido.");
        }
    }

    private void passageiros() {
        int opcao=t.inteiro("1 Editar/inativar / 2 Vincular pessoa existente",1,2);
        if(opcao==2) { System.out.println(passageiros.vincular(s,t.id("Pessoa"),t.ler("Assistencia")));return; }
        System.out.println(passageiros.editar(s,t.id("Passageiro"),t.ler("Nome"),t.data("Nascimento"),t.idOpcional("Pais de nacionalidade"),t.ler("Assistencia"),t.sim("Papel de passageiro ativo")));
    }

    private void cadastros() {
        TipoCadastro tipo=escolher(TipoCadastro.values());int opcao=t.inteiro("1 Listar / 2 Cadastrar / 3 Editar/inativar",1,3);
        if(opcao==1) { mostrar(cadastros.listar(s,tipo,t.inteiro("Pagina",1,100000)));return; }
        Long id=opcao==3?t.id("Id do cadastro"):null;var valores=new LinkedHashMap<String,Object>();
        for(var campo:tipo.campos()) {
            if(campo.nome().equals(tipo.chave()) && id!=null) { valores.put(campo.nome(),id);continue; }
            valores.put(campo.nome(),campo.converter(t.ler(campo.nome()+" ["+campo.tipo()+"]"+(campo.opcional()?" opcional":""))));
        }
        System.out.println("Cadastro salvo: "+cadastros.salvar(s,tipo,id,valores));
    }

    private Set<String> perfis() { return new HashSet<>(Arrays.asList(t.ler("Perfis separados por virgula: ADMINISTRADOR,ATENDIMENTO,OPERACAO,CONSULTA").toUpperCase(Locale.ROOT).replace(" ","").split(","))); }

    private void acesso() {
        switch(t.inteiro("1 Usuarios / 2 Criar usuario / 3 Acesso e perfis / 4 Minha senha",1,4)) {
            case 1 -> mostrar(acesso.usuarios(s));
            case 2 -> { long funcionario=t.id("Funcionario");String login=t.ler("Login");Set<String> perfis=perfis();System.out.println("Usuario: "+acesso.criarUsuario(s,funcionario,login,t.senha("Senha inicial"),perfis)); }
            case 3 -> acesso.acesso(s,t.id("Usuario"),t.sim("Acesso ativo"),perfis());
            case 4 -> acesso.alterarSenha(s,t.senha("Senha atual"),t.senha("Nova senha"));
        }
    }

    private ProgramacaoVoo programacao() {
        long companhia=t.id("Companhia"),rota=t.id("Rota"),aeronave=t.id("Aeronave");String numero=t.ler("Numero do voo");
        return new ProgramacaoVoo(companhia,rota,aeronave,numero,t.instante("Partida prevista"),t.instante("Chegada prevista"),t.instante("Check-in abre"),t.instante("Check-in fecha"),t.instante("Embarque abre"),t.instante("Embarque fecha"));
    }

    private void planejamento() {
        switch(t.inteiro("1 Programar / 2 Tarifa / 3 Bloquear assento / 4 Reprogramar / 5 Trocar aeronave / 6 Horarios / 7 Situacao",1,7)) {
            case 1 -> System.out.println("Voo: "+planejamento.programar(s,programacao()));
            case 2 -> {
                long voo=t.id("Voo");Long id=t.idOpcional("Tarifa existente");
                var tarifa=new Tarifa(t.ler("Codigo"),t.ler("Classe: ECONOMICA,EXECUTIVA,PRIMEIRA").toUpperCase(Locale.ROOT),t.decimal("Valor base"),t.decimal("Taxa de embarque"),t.decimal("Franquia kg"),t.inteiro("Pecas",0,10),t.sim("Permite cancelar"),t.decimal("Multa"));
                System.out.println("Tarifa: "+planejamento.tarifa(s,voo,id,tarifa,t.sim("Tarifa ativa")));
            }
            case 3 -> planejamento.bloquearAssento(s,t.id("Inventario do assento"),t.sim("Bloqueado"));
            case 4 -> { long voo=t.id("Voo");planejamento.reprogramar(s,voo,programacao(),t.ler("Motivo")); }
            case 5 -> planejamento.trocarAeronave(s,t.id("Voo"),t.id("Nova aeronave"),t.ler("Motivo"));
            case 6 -> planejamento.horarios(s,t.id("Voo"),t.instante("Partida"),t.instante("Chegada"),t.sim("Horarios reais"),t.ler("Motivo"));
            case 7 -> voos.transicao(s,t.id("Voo"),escolher(SituacaoVoo.values()),t.ler("Motivo"));
        }
    }

    private PedidoTrecho trecho() { return new PedidoTrecho(t.id("Passageiro"),t.id("Voo"),t.id("Tarifa"),t.inteiro("Ordem do trecho",1,32767),t.ler("Assento (vazio: automatico)"),t.decimal("Desconto")); }

    private void reservas() {
        switch(t.inteiro("1 Nova reserva / 2 Cancelar / 3 Remarcar / 4 Expirar pendencias",1,4)) {
            case 1 -> rascunho();
            case 2 -> reservas.cancelar(s,t.id("Reserva"),t.idOpcional("Item (vazio: toda reserva)"),t.ler("Motivo"));
            case 3 -> { long item=t.id("Item anterior");System.out.println("Nova reserva: "+reservas.remarcar(s,item,trecho(),t.inteiro("Prazo em minutos",1,120))); }
            case 4 -> System.out.println("Reservas expiradas: "+reservas.expirar(s));
        }
    }

    private void rascunho() {
        long comprador=t.id("Pessoa compradora");var itens=new ArrayList<PedidoTrecho>();
        while(true) {
            switch(t.inteiro("1 Adicionar / 2 Editar / 3 Remover / 4 Listar / 5 Limpar / 6 Gravar / 0 Voltar",0,6)) {
                case 0 -> { return; }
                case 1 -> {
                    Dados.exigir(itens.size()<100,"Limite de 100 itens.");var pedido=trecho();
                    Dados.exigir(!itens.contains(pedido),"Trecho repetido na posicao "+(itens.indexOf(pedido)+1)+".");itens.add(pedido);
                }
                case 2 -> { Dados.exigir(!itens.isEmpty(),"Sem itens.");int indice=t.inteiro("Posicao",1,itens.size())-1;System.out.println(itens.get(indice));itens.set(indice,trecho()); }
                case 3 -> { Dados.exigir(!itens.isEmpty(),"Sem itens.");itens.remove(t.inteiro("Posicao",1,itens.size())-1); }
                case 4 -> {
                    if(itens.isEmpty()) { System.out.println("Sem itens.");break; }
                    int pagina=t.inteiro("Pagina",1,(itens.size()+19)/20),inicio=(pagina-1)*20;
                    var parte=itens.subList(inicio,Math.min(inicio+20,itens.size()));
                    for(int i=0;i<parte.size();i++) System.out.println((inicio+i+1)+" | "+parte.get(i));
                }
                case 5 -> itens.clear();
                case 6 -> { System.out.println("Reserva: "+reservas.reservar(s,comprador,itens,t.inteiro("Prazo em minutos",1,120)));return; }
            }
        }
    }

    private void financeiro() {
        switch(t.inteiro("1 Pagamento simulado / 2 Confirmar / 3 Saldo / 4 Solicitar reembolso / 5 Processar reembolso / 6 Cancelar pagamento pendente",1,6)) {
            case 1 -> System.out.println("Pagamento: "+financeiro.pagar(s,t.id("Reserva"),t.ler("Chave de idempotencia"),t.ler("Forma: PIX,CARTAO,DINHEIRO").toUpperCase(Locale.ROOT),t.decimal("Valor"),t.ler("Resultado: PENDENTE,APROVADO,RECUSADO").toUpperCase(Locale.ROOT)));
            case 2 -> financeiro.confirmar(s,t.id("Reserva"));
            case 3 -> System.out.println("Saldo: "+financeiro.saldo(s,t.id("Reserva")));
            case 4 -> System.out.println("Reembolso: "+financeiro.solicitarReembolso(s,t.id("Pagamento"),t.decimal("Valor"),t.ler("Chave"),t.ler("Motivo")));
            case 5 -> financeiro.processarReembolso(s,t.id("Reembolso"),t.sim("Aprovar"));
            case 6 -> financeiro.cancelarPagamento(s,t.id("Pagamento"));
        }
    }

    private void atendimento() {
        switch(t.inteiro("1 Check-in / 2 Cartao / 3 Cancelar check-in / 4 Despachar / 5 Evento da bagagem / 6 Embarcar / 7 Rastreio",1,7)) {
            case 1 -> System.out.println("Check-in: "+atendimento.checkin(s,t.id("Item"),t.id("Documento")));
            case 2 -> System.out.println(atendimento.cartao(s,t.id("Item")));
            case 3 -> atendimento.cancelarCheckin(s,t.id("Item"));
            case 4 -> System.out.println("Bagagem: "+atendimento.despachar(s,t.id("Item"),t.ler("Etiqueta (vazio: gerar)"),t.decimal("Peso kg"),t.ler("Categoria: DESPACHADA,ESPECIAL").toUpperCase(Locale.ROOT)));
            case 5 -> atendimento.rastrear(s,t.ler("Etiqueta"),t.id("Aeroporto"),t.ler("Situacao").toUpperCase(Locale.ROOT),t.ler("Observacao"));
            case 6 -> System.out.println("Embarque: "+atendimento.embarcar(s,t.ler("Codigo do cartao"),t.id("Alocacao do portao")));
            case 7 -> mostrar(relatorios.bagagem(s,t.ler("Etiqueta")));
        }
    }

    private void operacao() {
        switch(t.inteiro("1 Alocar / 2 Interditar / 3 Liberar recurso / 4 Habilitacao / 5 Escalar / 6 Liberar escala / 7 Manutencao / 8 Situacao manutencao / 9 Solo / 10 Situacao solo / 11 Ocorrencia / 12 Encerrar ocorrencia",1,12)) {
            case 1 -> System.out.println("Alocacao: "+operacao.alocar(s,t.id("Voo"),t.id("Recurso"),t.ler("Finalidade: PARTIDA,CHEGADA,APOIO").toUpperCase(Locale.ROOT),t.instante("Inicio"),t.instante("Fim")));
            case 2 -> System.out.println("Interdicao: "+operacao.interditar(s,t.id("Recurso"),t.instante("Inicio"),t.instante("Fim"),t.ler("Motivo")));
            case 3 -> operacao.liberarAlocacao(s,t.id("Alocacao"));
            case 4 -> System.out.println("Habilitacao: "+operacao.habilitar(s,t.id("Funcionario"),t.id("Modelo"),t.ler("Funcao").toUpperCase(Locale.ROOT),t.ler("Licenca"),t.data("Validade")));
            case 5 -> System.out.println("Escala: "+operacao.escalar(s,t.id("Funcionario"),t.id("Aeroporto"),t.idOpcional("Voo"),t.ler("Funcao").toUpperCase(Locale.ROOT),t.instante("Inicio"),t.instante("Fim")));
            case 6 -> operacao.liberarEscala(s,t.id("Escala"));
            case 7 -> System.out.println("Manutencao: "+operacao.manutencao(s,t.id("Aeronave"),t.id("Responsavel"),t.ler("Tipo: PREVENTIVA,CORRETIVA,INSPECAO").toUpperCase(Locale.ROOT),t.ler("Descricao"),t.instante("Inicio"),t.instante("Fim"),t.decimal("Custo")));
            case 8 -> operacao.situacaoManutencao(s,t.id("Manutencao"),t.ler("Situacao").toUpperCase(Locale.ROOT));
            case 9 -> System.out.println("Servico: "+operacao.servicoSolo(s,t.id("Voo"),t.id("Aeroporto"),t.id("Responsavel"),t.ler("Tipo").toUpperCase(Locale.ROOT),t.decimal("Quantidade"),t.ler("Unidade"),t.decimal("Custo")));
            case 10 -> operacao.situacaoSolo(s,t.id("Servico"),t.ler("Situacao").toUpperCase(Locale.ROOT));
            case 11 -> System.out.println("Ocorrencia: "+operacao.ocorrencia(s,t.id("Aeroporto"),t.idOpcional("Voo"),t.ler("Tipo").toUpperCase(Locale.ROOT),t.ler("Gravidade").toUpperCase(Locale.ROOT),t.ler("Descricao")));
            case 12 -> operacao.encerrarOcorrencia(s,t.id("Ocorrencia"));
        }
    }

    private void relatorios() throws IOException {
        switch(t.inteiro("1 Consultar / 2 Exportar completo / 3 Painel / 4 Mapa / 5 Estatisticas atrasos",1,5)) {
            case 1 -> { TipoRelatorio tipo=escolher(TipoRelatorio.values());var linhas=new ArrayList<>(relatorios.consultar(s,tipo,t.inteiro("Pagina",1,100000)));if(t.sim("Ordem inversa")) Collections.reverse(linhas);mostrar(linhas); }
            case 2 -> System.out.println(relatorios.exportar(s,escolher(TipoRelatorio.values()),t.sim("CSV")));
            case 3 -> mostrar(relatorios.painel(s,t.id("Aeroporto"),t.data("Dia"),t.sim("Partidas")));
            case 4 -> { var mapa=relatorios.mapa(s,t.id("Voo"));System.out.println(Arrays.toString(mapa.colunas()));int[] filas=mapa.filas();String[][] celulas=mapa.celulas();for(int i=0;i<filas.length;i++) System.out.println(filas[i]+" | "+String.join(" | ",celulas[i])); }
            case 5 -> System.out.println(relatorios.resumoAtrasos(s));
        }
    }

    private void arquivos() throws IOException {
        switch(t.inteiro("1 Importar passageiros TXT/TSV / 2 Listar relatorios / 3 Copiar relatorio",1,3)) {
            case 1 -> System.out.println("Importados: "+arquivos.importarPassageiros(s,Path.of(t.ler("Caminho do arquivo"))));
            case 2 -> arquivos.listar(s).forEach(System.out::println);
            case 3 -> System.out.println(arquivos.copiarRelatorio(s,Path.of(t.ler("Caminho do relatorio"))));
        }
    }

    private <T extends Enum<T>> T escolher(T[] opcoes) { for(int i=0;i<opcoes.length;i++) System.out.println((i+1)+" | "+opcoes[i].name());return opcoes[t.inteiro("Opcao",1,opcoes.length)-1]; }
    private void mostrar(List<Map<String,Object>> linhas) { if(linhas.isEmpty()) System.out.println("Nenhum registro.");linhas.forEach(System.out::println); }
}
