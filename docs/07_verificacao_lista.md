# Verificacao da lista solicitada

Data: 2026-10-07. Este registro distingue implementacao de verificacao real.

## Ambiente e escopo desta rodada

- Node.js 24.18, JDK 21.0.10 e PostgreSQL 18.4 instalados no Windows.
- SQLite removido do código, dos testes, dos comandos e da configuração web.
- PostgreSQL único motor: schema `aeroporto` para Java (41 tabelas), `aerohub` para web (9 tabelas). Os cadastros dos dois modelos permanecem independentes.
- Testes executados em cluster isolado na porta 55439, sem alterar os serviços locais existentes.
- Configuração local sem senha preenchida; validação no banco real ainda depende das credenciais.
- Enunciado/rubrica oficial não disponível no repositório; execução em outro computador não realizada.

## Requisitos

V = implementado e exercitado no ambiente indicado; P = parcial (implementacao, verificacao especifica ou validacao externa pendente); N = ainda nao implementado; D = recurso/informacao indisponivel. V nao significa teste exaustivo de todos os limites. A classificacao inicial foi preservada; a evidencia atual mostra comportamento observado ou o que falta.

Estado atual: 81 V, 4 P, 3 D. A entrega nao esta integralmente concluida.

| ID | Requisito | Inicial | Atual | Evidencia atual / falta |
|---|---|---|---|---|
| 01 | Configurar as credenciais locais do PostgreSQL. | D | D | Arquivo local ignorado preparado com campos vazios. Credenciais reais nao estao disponiveis; nenhuma foi inventada. |
| 02 | Validar conexão, primeiro administrador e login. | P | P | Conexao real PostgreSQL 18.4, primeiro admin, segundo acesso recusado e login pelo terminal aprovados no cluster isolado. Banco do usuario ainda sem configuracao. |
| 03 | Validar cadastro de passageiros, consultas, auditoria e exportação no banco. | P | P | Passageiros, consultas, auditoria e exportacao aprovados no PostgreSQL isolado. Repeticao no banco do usuario depende das credenciais. |
| 04 | Conferir o enunciado e os critérios oficiais de avaliação do professor. | D | D | Slides e Relatorio topicos PDP encontrados e relidos; enunciado/rubrica oficial nao localizado. Nao ha declaracao de conformidade oficial. |
| 05 | Implementar edição e inativação de passageiros. | N | V | PassageiroServico + CadastroServico; IntegracaoTeste.cadastros: edicao, dupla identidade e inativacao do papel sem desligar funcionario. |
| 06 | Completar documentos, contatos, nacionalidade e necessidades de assistência. | N | V | PassageiroServico + CadastroServico; IntegracaoTeste.cadastros: edicao, dupla identidade e inativacao do papel sem desligar funcionario. |
| 07 | Permitir que uma pessoa existente tenha os papéis de passageiro e funcionário. | N | V | PassageiroServico + CadastroServico; IntegracaoTeste.cadastros: edicao, dupla identidade e inativacao do papel sem desligar funcionario. |
| 08 | Implementar cadastro, edição e inativação de funcionários e cargos. | N | V | CadastroServico + AdministracaoServico; cadastros/administracaoEImportacao: cargo/funcionario inativados, perfis, bloqueio, ultima conta administrativa e troca de senha. |
| 09 | Implementar administração de usuários, perfis, alteração de senha e bloqueio de acesso. | N | V | CadastroServico + AdministracaoServico; cadastros/administracaoEImportacao: cargo/funcionario inativados, perfis, bloqueio, ultima conta administrativa e troca de senha. |
| 10 | Implementar cadastros de países, cidades, aeroportos e terminais. | N | V | IntegracaoTeste.cadastrosRestantes: país cadastrado, editado e consultado pelo menu real; cidades/aeroportos/terminais persistidos nos serviços. |
| 11 | Implementar cadastros de portões, pistas, posições, esteiras e balcões. | N | V | TipoCadastro/CadastroServico/CadastroJdbc; cadastros/cenariosAdicionais: localidades, recursos, frota, modelos, assentos e rotas realmente persistidos. |
| 12 | Implementar cadastros de companhias aéreas e modelos de aeronave. | N | V | TipoCadastro/CadastroServico/CadastroJdbc; cadastros/cenariosAdicionais: localidades, recursos, frota, modelos, assentos e rotas realmente persistidos. |
| 13 | Implementar cadastro, edição e inativação de aeronaves. | N | V | regrasComplementares: edição do ano, inativação preservando assentos e rejeição de programação de aeronave inativa aprovadas. |
| 14 | Implementar configuração dos assentos físicos das aeronaves. | N | V | TipoCadastro/CadastroServico/CadastroJdbc; cadastros/cenariosAdicionais: localidades, recursos, frota, modelos, assentos e rotas realmente persistidos. |
| 15 | Implementar cadastro e manutenção das rotas. | N | V | cadastrosRestantes: edição de distância, rejeição de inativação com voos ativos, inativação após cancelamento preservando histórico e rejeição de nova programação aprovadas. |
| 16 | Implementar cadastro de ocorrências de voo. | N | V | OperacaoServico.ocorrencia/encerrarOcorrencia e VooServico; ocorrencias abertas/encerradas e cancelamento automatico exercitados. |
| 17 | Definir horários previstos e janelas de check-in e embarque. | N | V | PlanejamentoServico/PlanejamentoJdbc + VooServico/OperacaoServico; inventario, tarifas, margens, agendas, reprogramacao, troca sem vendas, horarios reais/estimados e transicoes exercitados. |
| 18 | Gerar o inventário de assentos de cada voo. | N | V | PlanejamentoServico/PlanejamentoJdbc + VooServico/OperacaoServico; inventario, tarifas, margens, agendas, reprogramacao, troca sem vendas, horarios reais/estimados e transicoes exercitados. |
| 19 | Implementar tarifas, classes, descontos e condições comerciais. | N | V | PlanejamentoServico/PlanejamentoJdbc + VooServico/OperacaoServico; inventario, tarifas, margens, agendas, reprogramacao, troca sem vendas, horarios reais/estimados e transicoes exercitados. |
| 20 | Criar a agenda da aeronave, incluindo margens de preparação. | N | V | PlanejamentoServico/PlanejamentoJdbc + VooServico/OperacaoServico; inventario, tarifas, margens, agendas, reprogramacao, troca sem vendas, horarios reais/estimados e transicoes exercitados. |
| 21 | Alocar portões, pistas e outros recursos. | N | V | PlanejamentoServico/PlanejamentoJdbc + VooServico/OperacaoServico; inventario, tarifas, margens, agendas, reprogramacao, troca sem vendas, horarios reais/estimados e transicoes exercitados. |
| 22 | Implementar atualização de horários estimados e reais. | N | V | PlanejamentoServico/PlanejamentoJdbc + VooServico/OperacaoServico; inventario, tarifas, margens, agendas, reprogramacao, troca sem vendas, horarios reais/estimados e transicoes exercitados. |
| 23 | Implementar as transições de situação do voo. | N | V | PlanejamentoServico/PlanejamentoJdbc + VooServico/OperacaoServico; inventario, tarifas, margens, agendas, reprogramacao, troca sem vendas, horarios reais/estimados e transicoes exercitados. |
| 24 | Implementar reprogramação, cancelamento e troca de aeronave dentro dos limites documentados. | N | V | PlanejamentoServico/PlanejamentoJdbc + VooServico/OperacaoServico; inventario, tarifas, margens, agendas, reprogramacao, troca sem vendas, horarios reais/estimados e transicoes exercitados. |
| 25 | Atualizar todas as agendas quando houver mudanças no voo. | N | V | PlanejamentoServico/PlanejamentoJdbc + VooServico/OperacaoServico; inventario, tarifas, margens, agendas, reprogramacao, troca sem vendas, horarios reais/estimados e transicoes exercitados. |
| 26 | Implementar reservas individuais, de grupos e com conexões. | N | V | Grupo e conexões nos serviços; testarRascunho executou adicionar/listar/editar/remover/limpar/gravar pelo terminal e conferiu os itens finais persistidos. |
| 27 | Vincular comprador, passageiros e trechos corretamente. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 28 | Copiar preços e condições da tarifa para os itens vendidos. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 29 | Implementar escolha ou atribuição automática de assento. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 30 | Validar disponibilidade, bloqueio e classe do assento. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 31 | Definir o prazo de confirmação da reserva. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 32 | Implementar expiração e liberação de assentos ao iniciar o programa e antes de vender. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 33 | Implementar cancelamento de um item ou da reserva inteira. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 34 | Recalcular a situação da reserva após alterações nos itens. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 35 | Implementar remarcação preservando itens e bilhetes anteriores. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 36 | Implementar pagamentos simulados e suas situações. | N | V | regrasComplementares: pagamento pendente não emite bilhete, cancelamento mantém histórico e aprovação posterior reutiliza a chave idempotente; aprovado/recusado/parcial também exercitados. |
| 37 | Validar cobertura financeira antes de confirmar a reserva. | N | V | FinanceiroServico/FinanceiroJdbc + ReservaJdbc; comercial/concorrencia/cancelamentos: parcial, cobertura, bilhetes, idempotencia simultanea, multas, devolucoes e excesso. |
| 38 | Emitir bilhetes após a confirmação. | N | V | FinanceiroServico/FinanceiroJdbc + ReservaJdbc; comercial/concorrencia/cancelamentos: parcial, cobertura, bilhetes, idempotencia simultanea, multas, devolucoes e excesso. |
| 39 | Impedir duplicação de operações com chaves de idempotência. | N | V | FinanceiroServico/FinanceiroJdbc + ReservaJdbc; comercial/concorrencia/cancelamentos: parcial, cobertura, bilhetes, idempotencia simultanea, multas, devolucoes e excesso. |
| 40 | Definir e aplicar multas e condições de cancelamento. | N | V | FinanceiroServico/FinanceiroJdbc + ReservaJdbc; comercial/concorrencia/cancelamentos: parcial, cobertura, bilhetes, idempotencia simultanea, multas, devolucoes e excesso. |
| 41 | Implementar solicitações e processamento de reembolsos. | N | V | FinanceiroServico/FinanceiroJdbc + ReservaJdbc; comercial/concorrencia/cancelamentos: parcial, cobertura, bilhetes, idempotencia simultanea, multas, devolucoes e excesso. |
| 42 | Impedir devoluções superiores ao valor pago. | N | V | FinanceiroServico/FinanceiroJdbc + ReservaJdbc; comercial/concorrencia/cancelamentos: parcial, cobertura, bilhetes, idempotencia simultanea, multas, devolucoes e excesso. |
| 43 | Implementar pagamentos complementares, como excesso de bagagem. | N | V | FinanceiroServico/FinanceiroJdbc + ReservaJdbc; comercial/concorrencia/cancelamentos: parcial, cobertura, bilhetes, idempotencia simultanea, multas, devolucoes e excesso. |
| 44 | Implementar check-in com validação de prazo, documento, bilhete e assento. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 45 | Gerar o cartão de embarque. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 46 | Implementar cancelamento e realização novamente do check-in. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 47 | Implementar despacho, pesagem e etiquetagem de bagagens. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 48 | Definir limites de peças, franquia e cobrança de excesso. | N | V | Franquia e excesso por peso, peça adicional (R$100) e rejeição de peça de 33kg aprovados em regrasComplementares. |
| 49 | Implementar situações e histórico de rastreio das bagagens. | P | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 50 | Tratar retirada, extravio e despacho em conexões. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 51 | Implementar embarque com validação de cartão, portão e horário. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 52 | Atualizar item e bilhete quando a passagem for utilizada. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 53 | Identificar passageiros que não compareceram. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 54 | Implementar habilitações de tripulantes e validade das licenças. | N | V | OperacaoServico/OperacaoJdbc + PlanejamentoJdbc; cadastros/cenariosAdicionais: licencas, equipe, gate/pista, interdicao, manutencao, cinco servicos de solo e ocorrencias. |
| 55 | Implementar escalas e atribuição de funções. | N | V | OperacaoServico/OperacaoJdbc + PlanejamentoJdbc; cadastros/cenariosAdicionais: licencas, equipe, gate/pista, interdicao, manutencao, cinco servicos de solo e ocorrencias. |
| 56 | Definir a quantidade mínima de tripulantes. | N | V | Equipe mínima válida permitiu embarque; ausência de tripulação impediu abertura em regrasComplementares. |
| 57 | Validar descanso, deslocamento e conflitos de jornada. | N | V | regrasComplementares: base inicial, descanso inferior a 12h, deslocamento incompatível e jornada superior a 14h rejeitados; jornada válida persistida. |
| 58 | Validar continuidade dos deslocamentos das aeronaves. | N | V | regrasComplementares: continuidade ida/volta, intervalo inferior a 75min e inserção antes do voo futuro exercitados. |
| 59 | Validar disponibilidade, localização e compatibilidade dos recursos. | N | V | OperacaoServico/OperacaoJdbc + PlanejamentoJdbc; cadastros/cenariosAdicionais: licencas, equipe, gate/pista, interdicao, manutencao, cinco servicos de solo e ocorrencias. |
| 60 | Implementar interdições de recursos. | N | V | OperacaoServico/OperacaoJdbc + PlanejamentoJdbc; cadastros/cenariosAdicionais: licencas, equipe, gate/pista, interdicao, manutencao, cinco servicos de solo e ocorrencias. |
| 61 | Implementar manutenção preventiva, corretiva e inspeções. | N | V | OperacaoServico/OperacaoJdbc + PlanejamentoJdbc; cadastros/cenariosAdicionais: licencas, equipe, gate/pista, interdicao, manutencao, cinco servicos de solo e ocorrencias. |
| 62 | Implementar abastecimento, limpeza, catering, bagagens e reboque. | N | V | OperacaoServico/OperacaoJdbc + PlanejamentoJdbc; cadastros/cenariosAdicionais: licencas, equipe, gate/pista, interdicao, manutencao, cinco servicos de solo e ocorrencias. |
| 63 | Implementar registro e encerramento de ocorrências operacionais. | N | V | OperacaoServico/OperacaoJdbc + PlanejamentoJdbc; cadastros/cenariosAdicionais: licencas, equipe, gate/pista, interdicao, manutencao, cinco servicos de solo e ocorrencias. |
| 64 | Completar painéis de partidas e chegadas por aeroporto. | N | V | RelatoriosCompletosServico + TipoRelatorio; todos os 16 tipos consultados/exportados; paineis, mapa, rastreio, atrasos e CSV de pagamentos com mais de 20 linhas. |
| 65 | Consultar portões, agendas, escalas e históricos. | P | V | RelatoriosCompletosServico + TipoRelatorio; todos os 16 tipos consultados/exportados; paineis, mapa, rastreio, atrasos e CSV de pagamentos com mais de 20 linhas. |
| 66 | Completar manifesto de passageiros, embarcados e ausentes. | N | V | RelatoriosCompletosServico + TipoRelatorio; todos os 16 tipos consultados/exportados; paineis, mapa, rastreio, atrasos e CSV de pagamentos com mais de 20 linhas. |
| 67 | Implementar consulta de bagagem por etiqueta. | N | V | RelatoriosCompletosServico + TipoRelatorio; todos os 16 tipos consultados/exportados; paineis, mapa, rastreio, atrasos e CSV de pagamentos com mais de 20 linhas. |
| 68 | Implementar relatórios de ocupação e atrasos. | N | V | RelatoriosCompletosServico + TipoRelatorio; todos os 16 tipos consultados/exportados; paineis, mapa, rastreio, atrasos e CSV de pagamentos com mais de 20 linhas. |
| 69 | Implementar relatórios de vendas, pagamentos e devoluções. | N | V | RelatoriosCompletosServico + TipoRelatorio; todos os 16 tipos consultados/exportados; paineis, mapa, rastreio, atrasos e CSV de pagamentos com mais de 20 linhas. |
| 70 | Ampliar a exportação para relatórios completos, além da página atual. | P | V | RelatoriosCompletosServico + TipoRelatorio; todos os 16 tipos consultados/exportados; paineis, mapa, rastreio, atrasos e CSV de pagamentos com mais de 20 linhas. |
| 71 | Implementar importação de dados com validação. | N | V | ArquivosServico; arquivos/administracaoEImportacao: importacao valida, erro na segunda linha com rollback e copia identica verificada por hash e Files.mismatch. |
| 72 | Implementar a rotina de cópia/backup de arquivos prevista na disciplina. | N | V | ArquivosServico; arquivos/administracaoEImportacao: importacao valida, erro na segunda linha com rollback e copia identica verificada por hash e Files.mismatch. |
| 73 | Aplicar permissões em todos os novos serviços. | P | V | AutorizacaoJdbc/AuditoriaJdbc/BancoDados/BloqueiosJdbc; permissao recusada, sessao bloqueada, autor do historico, rollback, vendas/pagamentos simultaneos e deadlock real exercitados. |
| 74 | Registrar auditoria das novas operações. | P | V | AutorizacaoJdbc/AuditoriaJdbc/BancoDados/BloqueiosJdbc; permissao recusada, sessao bloqueada, autor do historico, rollback, vendas/pagamentos simultaneos e deadlock real exercitados. |
| 75 | Garantir transações completas nos fluxos que alteram várias tabelas. | P | V | AutorizacaoJdbc/AuditoriaJdbc/BancoDados/BloqueiosJdbc; permissao recusada, sessao bloqueada, autor do historico, rollback, vendas/pagamentos simultaneos e deadlock real exercitados. |
| 76 | Aplicar a ordem de bloqueios prevista na modelagem. | N | V | AutorizacaoJdbc/AuditoriaJdbc/BancoDados/BloqueiosJdbc; permissao recusada, sessao bloqueada, autor do historico, rollback, vendas/pagamentos simultaneos e deadlock real exercitados. |
| 77 | Tratar operações simultâneas, conflitos e deadlocks. | N | V | AutorizacaoJdbc/AuditoriaJdbc/BancoDados/BloqueiosJdbc; permissao recusada, sessao bloqueada, autor do historico, rollback, vendas/pagamentos simultaneos e deadlock real exercitados. |
| 78 | Tratar falhas de conexão sem deixar dados parcialmente gravados. | N | V | BancoDados faz rollback e nao repete SQLSTATE de conexao. SQLException 08006 foi injetada apos gravacao: nada parcial persistiu. Nao foi simulado corte de rede no instante de commit; resultado desconhecido exige consulta por chave. |
| 79 | Preservar o histórico em cancelamentos e inativações. | P | V | AutorizacaoJdbc/AuditoriaJdbc/BancoDados/BloqueiosJdbc; permissao recusada, sessao bloqueada, autor do historico, rollback, vendas/pagamentos simultaneos e deadlock real exercitados. |
| 80 | Evitar informações pessoais e credenciais nos logs. | P | V | Main/PersistenciaException e listagem AdministracaoServico: logs apenas com classe/SQLSTATE, sem senha/documento/detalhes SQL; hashes nao sao selecionados na administracao. |
| 81 | Preparar dados fictícios para demonstração. | N | V | DadosFicticiosServico e menu 18; cenario FICTICIO com passageiro/documento, voo/tarifa, assentos, equipe e gate; repetir cenario devolve ids existentes. |
| 82 | Verificar regras, restrições, permissões e operações concorrentes. | P | V | IntegracaoTeste: fluxo completo real, restricoes, operacoes simultaneas, expiracao, cancelamento, remarcacao, reembolso, ausencia, falha JDBC injetada e deadlock PostgreSQL. |
| 83 | Verificar o fluxo completo: cadastro → voo → reserva → pagamento → check-in → embarque. | N | V | IntegracaoTeste: fluxo completo real, restricoes, operacoes simultaneas, expiracao, cancelamento, remarcacao, reembolso, ausencia, falha JDBC injetada e deadlock PostgreSQL. |
| 84 | Verificar expiração, cancelamento, reembolso, ausência e falhas. | N | V | IntegracaoTeste: fluxo completo real, restricoes, operacoes simultaneas, expiracao, cancelamento, remarcacao, reembolso, ausencia, falha JDBC injetada e deadlock PostgreSQL. |
| 85 | Implementar os conteúdos restantes de arrays, matrizes, coleções, strings e arquivos exigidos pelo professor. | P | P | Matriz/vetores, Arrays, ArrayList, Collections, strings e arquivos em uso funcional; fontes e testes em docs/03. Rubrica ausente; APIs sem obrigatoriedade comprovada nao foram inventadas. |
| 86 | Atualizar os MDs conforme cada funcionalidade for concluída. | N | V | README e guias Java/web, tópicos, verificação e checklist atualizados; modelagem e diagramas preservados. |
| 87 | Validar a execução em outro computador. | D | D | Nao houve acesso nem execucao em outra maquina. Scripts de configuracao/testes e instrucoes preparados, mas portabilidade externa nao foi declarada. |
| 88 | Preparar a apresentação e o roteiro de demonstração. | N | P | Apresentacao oral em oito partes e demonstracao documentadas em docs/09; nao foi criado PPTX nem validado formato/tempo segundo uma rubrica oficial. |

## Evidencias no codigo

- Entrada: [Main](../src/main/java/br/edu/aeroporto/Main.java), [MenuOperacional](../src/main/java/br/edu/aeroporto/cli/MenuOperacional.java), [Terminal](../src/main/java/br/edu/aeroporto/cli/Terminal.java).
- Cadastros/acesso: [CadastroServico](../src/main/java/br/edu/aeroporto/servico/CadastroServico.java), [PassageiroServico](../src/main/java/br/edu/aeroporto/servico/PassageiroServico.java), [AdministracaoServico](../src/main/java/br/edu/aeroporto/servico/AdministracaoServico.java), [TipoCadastro](../src/main/java/br/edu/aeroporto/dominio/TipoCadastro.java).
- Voos/operacao: [PlanejamentoServico](../src/main/java/br/edu/aeroporto/servico/PlanejamentoServico.java), [VooServico](../src/main/java/br/edu/aeroporto/servico/VooServico.java), [OperacaoServico](../src/main/java/br/edu/aeroporto/servico/OperacaoServico.java), [OperacaoJdbc](../src/main/java/br/edu/aeroporto/infraestrutura/jdbc/OperacaoJdbc.java).
- Comercial: [ReservaServico](../src/main/java/br/edu/aeroporto/servico/ReservaServico.java), [ReservaJdbc](../src/main/java/br/edu/aeroporto/infraestrutura/jdbc/ReservaJdbc.java), [FinanceiroServico](../src/main/java/br/edu/aeroporto/servico/FinanceiroServico.java), [AtendimentoServico](../src/main/java/br/edu/aeroporto/servico/AtendimentoServico.java).
- Relatorios/arquivos: [RelatoriosCompletosServico](../src/main/java/br/edu/aeroporto/servico/RelatoriosCompletosServico.java), [ArquivosServico](../src/main/java/br/edu/aeroporto/servico/ArquivosServico.java), [MapaAssentos](../src/main/java/br/edu/aeroporto/dominio/MapaAssentos.java), [DadosFicticiosServico](../src/main/java/br/edu/aeroporto/servico/DadosFicticiosServico.java).
- Consistencia: [BancoDados](../src/main/java/br/edu/aeroporto/infraestrutura/jdbc/BancoDados.java), [BloqueiosJdbc](../src/main/java/br/edu/aeroporto/infraestrutura/jdbc/BloqueiosJdbc.java), [AutorizacaoJdbc](../src/main/java/br/edu/aeroporto/infraestrutura/jdbc/AutorizacaoJdbc.java), [migracao](../sql/migracoes/001_cadastros_ativos.sql).
- Testes: [UnidadeTeste](../src/test/java/br/edu/aeroporto/UnidadeTeste.java), [IntegracaoTeste](../src/test/java/br/edu/aeroporto/IntegracaoTeste.java), [testar-postgres.ps1](../testar-postgres.ps1).

## Preservacao dos materiais

Slides Boas-vindas e aulas 02 a 05 encontrados em Documents/Projeto de programacao. Aula 02: POO, heranca, polimorfismo, Java, Scanner, condicionais e repeticao. Aula 03: arrays, matrizes, Arrays e ArrayList. Aula 04: strings e conversoes. Aula 05: excecoes, finally, try-with-resources, UTF-8, arquivos, copia e backup. Foram relidos os textos XML dos PPTX e o Relatorio topicos PDP. O slide de avaliacoes nao forneceu rubrica textual; nao foi usado para inventar pesos ou criterios. A cobertura concreta e seus limites estao em [03_topicos_disciplina.md](03_topicos_disciplina.md).

## Verificações executadas nesta etapa

Comando: `powershell -NoProfile -ExecutionPolicy Bypass -File .\testar-postgres.ps1 -Tudo`.

| Verificação | Resultado |
|---|---|
| Compilação Java, JDK 21 | Aprovada |
| UnidadeTeste | 12 verificações aprovadas |
| IntegracaoTeste, PostgreSQL 18.4 | 129 verificações aprovadas; 41 tabelas Java |
| API e regras web, PostgreSQL | 27 testes aprovados |
| Build Vite | Aprovado |
| Playwright/Chrome | 4 testes aprovados (navegação, busca/exportação, passageiro/reserva/check-in/cartão, voo/edição/histórico, permissões e mobile) |
| Formatação | Aprovada |
| git diff --check | Aprovado |

Testes complementares agora executados: pendências de países, frota, rotas, rascunho,
pagamentos, bagagens, equipe, descanso e continuidade. A web inclui testes novos de
persistência entre conexões, duas vendas do mesmo assento, versões concorrentes,
rollback por chave estrangeira e exportação na virada do dia em São Paulo.

A primeira execução encontrou uma colisão aleatória entre identificadores de cenários
fictícios: o prefixo C podia coincidir com o identificador principal. Os testes agora
usam prefixos distintos D/C, mantendo a idempotência de produção. A migração assíncrona
web também foi corrigida conforme a falha observada no teste de check-in. As rodadas
com falha não são contadas como aprovadas. Evidência bruta: `artifacts/validacao-atual.log`.

As transações web usam o mesmo cliente PostgreSQL até commit/rollback e um bloqueio
por schema antes das validações. A criação de dados fictícios é opcional e repetível.
Os testes usam schemas exclusivos e removem somente seus próprios schemas/cluster.
Não foi encontrado arquivo de dados web prévio neste workspace; não houve importação
de dados de outro motor. O schema Java original não foi substituído pelo schema web.

## Limpeza e entrega

Removidos os scripts temporários da conversão. O script de testes encerra e remove
os clusters criados por ele e os schemas web de teste. Mantidos código, testes,
documentação, driver JDBC, dependências e builds necessários à execução. Os relatórios
fictícios e as capturas de teste ficaram nas pastas ignoradas como evidência.
Uma limpeza adicional em lote de pastas geradas foi recusada pela política automática;
esses arquivos não foram apagados. Credenciais e serviços locais foram preservados.

## Pendências externas para encerramento

1. Preencher `config/application.properties` e validar conexão/login/cadastros no banco real.
2. Disponibilizar enunciado/rubrica oficial para confirmar conteúdos, formato e tempo da apresentação.
3. Executar em outro computador e registrar ambiente/resultados.

Apresentação oral e roteiro estão em [09_roteiro_apresentacao.md](09_roteiro_apresentacao.md).
A lista não declara conformidade oficial nem validação em ambiente ao qual não houve acesso.
