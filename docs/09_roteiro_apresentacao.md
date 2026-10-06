# Apresentacao e demonstracao

Material preparado para apresentacao oral, em oito partes. O enunciado/rubrica oficial ainda precisa ser confrontado com esta entrega. Nao ha afirmacao de execucao em outro computador.

## 1. Sistema e escopo

Mostrar o terminal Java com PostgreSQL e a modelagem de 41 tabelas. Explicar que um voo e uma ocorrencia datada, uma reserva pode conter grupos/conexoes, e pagamentos sao simulados.

## 2. Orientacao a objetos

Abrir Pessoa, Passageiro e Funcionario: classe abstrata, heranca, encapsulamento, identidade comum e sobrescrita. Mostrar ExportadorVoos e implementacoes TXT/CSV para polimorfismo. Mostrar os records de programacao, tarifa e pedidos de trechos e a separacao entre menu, servico e persistencia.

## 3. Arrays, matrizes, colecoes e strings

Opcao 16/4: matriz String[][] de um mapa irregular, ordenacao das colunas, busca por indice e copias defensivas. Opcao 12/1: ArrayList de passageiros/trechos com adicao, edicao, remocao e limpeza antes de gravar. Opcao 16: inversao de resultados e minimo/maximo/mediana dos atrasos. Mostrar normalizacao de CPF/login, texto SQL, split do arquivo tabulado e StringJoiner na exportacao.

## 4. Persistencia e consistencia

Mostrar a transacao que reserva grupo, copia tarifa e ocupa os assentos. Mostrar FKs compostas, indices parciais e EXCLUDE. Explicar ordem de bloqueios, rollback e repeticao completa apos deadlock. Mostrar auditoria e historico de voo, sem abrir arquivos de credenciais.

## 5. Demonstracao comercial

Depois de configurar o banco real e aplicar a migracao:

1. Entrar como administrador existente; inicializar somente se ainda nao houver usuario.
2. Opcao 18: escolher identificador DEMO01; anotar ids devolvidos de passageiro, documento, voo, tarifa e alocacao. Repetir DEMO01 para demonstrar ausencia de duplicacao. Para outra demonstracao futura, escolher outro identificador.
3. Opcao 16/4: exibir o mapa irregular de cinco assentos.
4. Opcao 12/1: comprador = passageiro do cenario, adicionar um trecho com voo/tarifa devolvidos e assento automatico; gravar com prazo de 15 minutos.
5. Opcao 6: consultar localizador, ou 16/1 VENDAS e MANIFESTO para obter ids de reserva e item.
6. Opcao 13/1: registrar PIX simulado APROVADO de R$120 com chave unica. Repetir a mesma chave e dados para demonstrar idempotencia.
7. Opcao 11/7: abrir CHECKIN_ABERTO, dentro da janela ja vigente do cenario.
8. Opcao 14/1: realizar check-in com item/documento do cenario; 14/2 exibe o cartao.
9. Opcao 14/4: despachar 25kg. Opcao 13/3 mostra o excesso de R$60; pagar com outra chave, usando 13/1.
10. Como administrador ou operador, registrar INSPECIONADA e CARREGADA em 14/5 no aeroporto de origem (consultar 16/3 ou 9/AEROPORTO para o id).
11. Aguardar a janela de embarque real; 11/7 altera para EMBARQUE. A demo ja possui equipe licenciada e portao alocado. Opcao 14/6 usa o codigo do cartao e o id da alocacao devolvido.

O cenario nasce com partida duas horas a frente: check-in ja aberto por janela, embarque inicia 40 minutos antes da partida. Nao alterar o relogio do computador para encurtar a demonstracao. Para uma apresentacao curta, usar os testes de integracao, que injetam um Clock de simulacao e percorrem todas as etapas sem espera real.

## 6. Cancelamentos, falhas e operacao

Demonstrar cancelamento parcial de uma reserva separada, a multa copiada, solicitacao/processamento de reembolso e liberacao do assento. Mostrar remarcacao com item anterior preservado, expiracao e passageiro ausente no fechamento. Mostrar o teste de duas vendas do mesmo assento e o teste de deadlock. Opcao 15 demonstra agendas, manutencao e ocorrencias.

## 7. Arquivos e excecoes

Opcao 16/2: exportar MANIFESTO ou PAGAMENTOS completo em CSV. Opcao 17/2 lista arquivos e 17/3 copia um relatorio com verificacao SHA-256. Para importacao, preparar TXT com TAB real e nomes FICTICIOS; demonstrar arquivo valido e arquivo cujo segundo registro falha, com rollback do primeiro. Mostrar FileReader/FileWriter em UTF-8, BufferedReader/Writer, InputStream/OutputStream, try-with-resources e finally.

## 8. Evidencias e pendencias

Abrir `07_verificacao_lista.md`, os comandos de testes e seus resultados. Explicar a diferenca entre PostgreSQL isolado de teste e banco do usuario. Os slides aulas 02 a 05 foram localizados e os conceitos preservados; conformidade com a rubrica oficial depende do material completo. Configuracao no banco do usuario e portabilidade em outra maquina devem ser verificadas antes da entrega definitiva.
