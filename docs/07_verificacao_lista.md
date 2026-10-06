# Verificacao da lista solicitada

Data: 2026-10-06. Este registro distingue implementacao de verificacao real.

## Estado inicial

- Git: main, commit ab686e0, sem modificacoes locais; envio anterior bloqueado por falta de permissao.
- Web/SQLite: 22 testes aprovados antes de editar.
- Java: compilacao --release 21 e --ajuda aprovadas usando JDK 26 localizado em .jdks; JDK fora do PATH.
- PostgreSQL do usuario: sem arquivo local de credenciais, variaveis ou servidor identificado. Nenhuma senha foi lida ou criada.
- Materiais: slides Boas-vindas e aulas 02 a 05 localizados em Documents/Projeto de programacao. Enunciado e rubrica oficial nao localizados no repositorio.
- SQL: 41 tabelas, exclusoes de agendas, unicidade parcial de assentos e historico de voo ja existentes; preservados.

## Requisitos

V = implementado e exercitado no ambiente indicado; P = parcial (implementacao, verificacao especifica ou validacao externa pendente); N = ainda nao implementado; D = recurso/informacao indisponivel. V nao significa teste exaustivo de todos os limites. A classificacao inicial foi preservada; a evidencia atual mostra comportamento observado ou o que falta.

Estado atual: 72 V, 13 P, 3 D. A entrega nao esta integralmente concluida.

| ID | Requisito | Inicial | Atual | Evidencia atual / falta |
|---|---|---|---|---|
| 01 | Configurar as credenciais locais do PostgreSQL. | D | D | Arquivo local ignorado preparado com campos vazios. Credenciais reais nao estao disponiveis; nenhuma foi inventada. |
| 02 | Validar conexão, primeiro administrador e login. | P | P | Conexao real PostgreSQL 17.11, primeiro admin, segundo acesso recusado e login pelo terminal aprovados no cluster isolado. Banco do usuario ainda sem configuracao. |
| 03 | Validar cadastro de passageiros, consultas, auditoria e exportação no banco. | P | P | Passageiros, consultas, auditoria e exportacao aprovados no PostgreSQL isolado. Repeticao no banco do usuario depende das credenciais. |
| 04 | Conferir o enunciado e os critérios oficiais de avaliação do professor. | D | D | Slides e Relatorio topicos PDP encontrados e relidos; enunciado/rubrica oficial nao localizado. Nao ha declaracao de conformidade oficial. |
| 05 | Implementar edição e inativação de passageiros. | N | V | PassageiroServico + CadastroServico; IntegracaoTeste.cadastros: edicao, dupla identidade e inativacao do papel sem desligar funcionario. |
| 06 | Completar documentos, contatos, nacionalidade e necessidades de assistência. | N | V | PassageiroServico + CadastroServico; IntegracaoTeste.cadastros: edicao, dupla identidade e inativacao do papel sem desligar funcionario. |
| 07 | Permitir que uma pessoa existente tenha os papéis de passageiro e funcionário. | N | V | PassageiroServico + CadastroServico; IntegracaoTeste.cadastros: edicao, dupla identidade e inativacao do papel sem desligar funcionario. |
| 08 | Implementar cadastro, edição e inativação de funcionários e cargos. | N | V | CadastroServico + AdministracaoServico; cadastros/administracaoEImportacao: cargo/funcionario inativados, perfis, bloqueio, ultima conta administrativa e troca de senha. |
| 09 | Implementar administração de usuários, perfis, alteração de senha e bloqueio de acesso. | N | V | CadastroServico + AdministracaoServico; cadastros/administracaoEImportacao: cargo/funcionario inativados, perfis, bloqueio, ultima conta administrativa e troca de senha. |
| 10 | Implementar cadastros de países, cidades, aeroportos e terminais. | N | P | Cidades/aeroportos/terminais criados pelo servico nos testes; PAIS usa o mesmo CRUD fechado, mas cadastro/edicao de pais pelo menu ainda sem verificacao especifica. |
| 11 | Implementar cadastros de portões, pistas, posições, esteiras e balcões. | N | V | TipoCadastro/CadastroServico/CadastroJdbc; cadastros/cenariosAdicionais: localidades, recursos, frota, modelos, assentos e rotas realmente persistidos. |
| 12 | Implementar cadastros de companhias aéreas e modelos de aeronave. | N | V | TipoCadastro/CadastroServico/CadastroJdbc; cadastros/cenariosAdicionais: localidades, recursos, frota, modelos, assentos e rotas realmente persistidos. |
| 13 | Implementar cadastro, edição e inativação de aeronaves. | N | P | Criacao, inventario e limites de uso da aeronave aprovados. Teste adicional de edicao/inativacao foi escrito em regrasComplementares, mas nao executado apos bloqueio de aprovacao. |
| 14 | Implementar configuração dos assentos físicos das aeronaves. | N | V | TipoCadastro/CadastroServico/CadastroJdbc; cadastros/cenariosAdicionais: localidades, recursos, frota, modelos, assentos e rotas realmente persistidos. |
| 15 | Implementar cadastro e manutenção das rotas. | N | P | Rotas de ida/volta e planejamento aprovados. Edicao/inativacao e limites historicos implementados, ainda sem teste comportamental especifico dessas edicoes. |
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
| 26 | Implementar reservas individuais, de grupos e com conexões. | N | P | Reservas individuais, quatro itens/grupo e conexoes aprovados nos servicos. TestarRascunho acrescenta gravacao com edicao/limpeza pelo terminal, ainda nao executado. |
| 27 | Vincular comprador, passageiros e trechos corretamente. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 28 | Copiar preços e condições da tarifa para os itens vendidos. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 29 | Implementar escolha ou atribuição automática de assento. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 30 | Validar disponibilidade, bloqueio e classe do assento. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 31 | Definir o prazo de confirmação da reserva. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 32 | Implementar expiração e liberação de assentos ao iniciar o programa e antes de vender. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 33 | Implementar cancelamento de um item ou da reserva inteira. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 34 | Recalcular a situação da reserva após alterações nos itens. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 35 | Implementar remarcação preservando itens e bilhetes anteriores. | N | V | ReservaServico/ReservaJdbc; comercial/cenariosAdicionais: grupo de quatro itens, conexoes, comprador separado, snapshot, classe/bloqueio, expiracao, cancelamento parcial e remarcacao. |
| 36 | Implementar pagamentos simulados e suas situações. | N | P | APROVADO/RECUSADO e pagamento parcial aprovados. Casos novos de PENDENTE, aprovacao posterior e cancelamento pendente aguardam execucao de regrasComplementares. |
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
| 48 | Definir limites de peças, franquia e cobrança de excesso. | N | P | Franquia e cobranca por peso aprovadas (25kg gera R$60). Teste novo de peca extra e rejeicao de 33kg foi escrito, ainda nao executado. |
| 49 | Implementar situações e histórico de rastreio das bagagens. | P | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 50 | Tratar retirada, extravio e despacho em conexões. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 51 | Implementar embarque com validação de cartão, portão e horário. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 52 | Atualizar item e bilhete quando a passagem for utilizada. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 53 | Identificar passageiros que não compareceram. | N | V | AtendimentoServico + VooServico; atendimento/bagagensEmConexao/cancelamentoAposEmbarque: documento, cartao, refazer check-in, etiquetas, excesso, extravio, retirada, embarque e ausencia. |
| 54 | Implementar habilitações de tripulantes e validade das licenças. | N | V | OperacaoServico/OperacaoJdbc + PlanejamentoJdbc; cadastros/cenariosAdicionais: licencas, equipe, gate/pista, interdicao, manutencao, cinco servicos de solo e ocorrencias. |
| 55 | Implementar escalas e atribuição de funções. | N | V | OperacaoServico/OperacaoJdbc + PlanejamentoJdbc; cadastros/cenariosAdicionais: licencas, equipe, gate/pista, interdicao, manutencao, cinco servicos de solo e ocorrencias. |
| 56 | Definir a quantidade mínima de tripulantes. | N | P | Equipe minima valida permitiu embarque no fluxo completo. Rejeicao por equipe ausente foi acrescentada em regrasComplementares, ainda sem execucao. |
| 57 | Validar descanso, deslocamento e conflitos de jornada. | N | P | Licencas, escalas e conflito foram exercitados. Testes direcionados de descanso de 12h, base, deslocamento e jornada >14h aguardam a rodada complementar. |
| 58 | Validar continuidade dos deslocamentos das aeronaves. | N | P | Continuidade de ida/volta e conflitos usados no planejamento. Rejeicoes especificas de deslocamento incorreto, intervalo <75min e insercao antes do proximo voo aguardam execucao complementar. |
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
| 86 | Atualizar os MDs conforme cada funcionalidade for concluída. | N | V | README e docs/02,03,05,07,08,09 atualizados; modelagem, diagramas e doc web preservados. |
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

## Verificacoes executadas

| Comando / ambiente | Resultado observado |
|---|---|
| compilar.ps1, JDK 26 --release 21 | Aprovado; aplicacao compilada para Java 21 |
| executar.ps1 -Ajuda | Aprovado antes da implementacao; independente do banco |
| testar-postgres.ps1, PostgreSQL 17.11 | Cluster novo e schema/migracao instalados; 41 tabelas; 12 verificacoes unitarias e 103 verificacoes PostgreSQL aprovadas; servidor encerrado no finally |
| Integracao CLI | Login, menus, matriz e listagem de arquivos por stdin real aprovados na rodada de 103 verificacoes |
| Concorrencia / falhas | Duas vendas disputaram um assento (uma venceu); mesma chave de pagamento retornou um registro; deadlock real repetiu a transacao integral; falha JDBC injetada desfez gravacao anterior |
| npm.cmd test | 22 testes aprovados, antes e depois das alteracoes Java |
| npm.cmd run build | Aprovado |
| npm.cmd run format:check | Aprovado nos arquivos suportados web/servidor/scripts; nao e formatador Java |
| npm.cmd exec playwright test | 4 testes desktop/mobile e fluxos anteriores aprovados |
| executar.ps1 -Diagnostico, configuracao do usuario | Erro esperado: db.url/db.usuario/db.senha ausentes; nenhuma conexao ou criacao de administrador no banco do usuario |
| git diff --check | Aprovado |

Depois da rodada de 103 verificacoes, foram acrescentados regrasComplementares e testarRascunho: descanso/local/jornada, continuidade da aeronave, pagamento pendente, classe vendida, peca extra/limite de peso, equipe ausente, inativacao da aeronave e rascunho pelo terminal. A nova chamada de testar-postgres.ps1 NAO foi executada: a revisao automatica de aprovacao atingiu limite de uso. Nao foi uma rejeicao por inseguranca e nenhuma tentativa de contornar a aprovacao foi feita. Esses testes ainda precisam de compilacao/execucao; nao entram nos 103 resultados aprovados. O codigo de producao nao foi modificado depois dessa rodada aprovada, apenas testes/documentacao.

Falhas da web preexistentes nao foram observadas nos 22 testes de base. Avisos Node sobre SQLite experimental/cores foram mantidos; nao sao falhas. Nao foi alterada credencial existente nem apagado banco/cluster. Os clusters ficticios de verificacao foram encerrados e seus arquivos permaneceram em artifacts, ignorados pelo Git.

## Arquivos alterados

- Base estendida: Main, MenuPrincipal, Terminal, PassageiroServico/PassageiroJdbc/PassageiroRepositorio, AutenticacaoServico, AutorizacaoJdbc, BancoDados e Sql.
- Novos modulos: MenuOperacional; dominio Dados/TipoCadastro/ProgramacaoVoo/Tarifa/PedidoTrecho/MapaAssentos; TipoRelatorio; JDBC Cadastro/Planejamento/Operacao/Reserva/Financeiro/Bloqueios; servicos Cadastro/Administracao/Planejamento/Voo/Reserva/Financeiro/Atendimento/Operacao/RelatoriosCompletos/Arquivos/DadosFicticios.
- Execucao/testes: compilar.ps1, executar.ps1, scripts/configurar-java.ps1, scripts/PrepararBancoTeste.java, testar-java.ps1, testar-postgres.ps1, UnidadeTeste e IntegracaoTeste.
- Banco: somente sql/migracoes/001_cadastros_ativos.sql. Instalador original e 41 tabelas preservados.
- Documentacao: README e docs/02,03,05,07,08,09. Docs/01,04,06 e aplicacao web anterior preservados.
- Configuracao local: application.properties com campos vazios, ignorado pelo Git. Binarios/JAR, classes, clusters, logs, dados web e relatorios fora do versionamento.

## Pendencias para encerramento

1. Liberar a execucao aprovada ou aguardar a recuperacao do limite da ferramenta e repetir testar-postgres.ps1 para compilar/executar a ampliacao final. Corrigir eventual falha sem enfraquecer regras. Validar tambem os cadastros/edicoes ainda marcados P.
2. Preencher localmente a conexao PostgreSQL real e repetir diagnostico, migracao e validacao, sem enviar senha na conversa nem criar outro administrador se ja houver um.
3. Obter enunciado/rubrica oficial para confirmar APIs, GUI, formato de apresentacao e demais criterios. Slides/sugestoes nao substituem essa conferencia.
4. Executar em outro computador e registrar ambiente/resultados. Scripts preparados nao equivalem a execucao externa.
5. Apresentacao oral e roteiro estao preparados em [09_roteiro_apresentacao.md](09_roteiro_apresentacao.md); nao foi criado PPTX nem confirmado formato/tempo oficial.

O envio anterior ao GitHub foi recusado por falta de permissao da conta no repositorio de destino. No encerramento desta verificacao, as alteracoes desta etapa ainda estavam locais, sem novo commit/push; nao foram apresentadas como publicadas.
