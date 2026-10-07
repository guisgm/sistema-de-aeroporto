# Operacao Java/PostgreSQL

## Escopo implementado

A entrada dos novos fluxos e o terminal Java. O AeroHub React/PostgreSQL usa o schema independente aerohub e está documentado em `06_aplicacao_web.md`. A lista foi implementada sobre as 41 tabelas PostgreSQL existentes, sem criar tabelas equivalentes. O historico automatico de voos, as FKs compostas, as exclusoes de agendas e os indices parciais de ocupacao foram preservados.

`Main` monta os servicos e injeta as dependencias. `MenuPrincipal` conserva as opcoes 1 a 7; `MenuOperacional` conecta as opcoes 8 a 18 aos novos servicos. `CadastroJdbc` usa uma lista fechada de tabelas, campos e tipos em `TipoCadastro`: nomes digitados nunca viram identificadores SQL. Os servicos comerciais compartilham a mesma conexao JDBC para cada transacao.

## Configuracao local

O arquivo `config/application.properties` esta preparado com campos vazios e e ignorado pelo Git. Informe a URL JDBC, o usuario e a senha reais. Exemplo de formato de URL, sem credencial: `jdbc:postgresql://localhost:5432/sistema_aeroporto`. Variaveis `AEROPORTO_DB_URL`, `AEROPORTO_DB_USUARIO`, `AEROPORTO_DB_SENHA` e `AEROPORTO_FUSO` prevalecem sobre o arquivo.

As credenciais do banco do usuario nao estavam no repositorio nem nas variaveis disponiveis. O PostgreSQL usado nos testes e separado. Seus dados ficticios e seu modo de autenticacao nao configuram o banco do usuario.

Os scripts procuram javac no PATH, depois em JAVA_HOME e em `.jdks` do usuario. Compilam com `--release 21`. O driver continua pgJDBC 42.7.13, com a verificacao SHA-256 ja existente. Nao mudam o PATH global nem a politica de execucao permanente do Windows.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\executar.ps1 -Diagnostico
powershell -NoProfile -ExecutionPolicy Bypass -File .\executar.ps1 -Migrar
powershell -NoProfile -ExecutionPolicy Bypass -File .\executar.ps1 -Inicializar
powershell -NoProfile -ExecutionPolicy Bypass -File .\executar.ps1
```

Execute `-Inicializar` somente se nao houver operador. O servico recusa uma segunda inicializacao e nao redefine senhas. O primeiro administrador nao tem senha padrao.

## Migracao

Em banco novo, use primeiro `sql/criar_banco.sql`. Em banco existente, preserve a instalacao e execute somente `sql/migracoes/001_cadastros_ativos.sql` ou `-Migrar`.

A migracao adiciona `passageiro.ativo`, `cargo.ativo`, `modelo_aeronave.ativo` e `rota.ativa`, todos com valor inicial verdadeiro. Nao remove tabelas, linhas ou chaves. A inativacao do papel passageiro nao desliga automaticamente o funcionario que possui a mesma pessoa. Desligamento de funcionario usa a data de desligamento existente. A migracao e repetivel e protegida por bloqueio consultivo; foi testada em banco com registros.

## Menus

| Opcao | Fluxos |
|---|---|
| 8 | Editar/inativar passageiro; vincular o papel a uma pessoa existente |
| 9 | Paises, cidades, aeroportos, terminais, recursos, companhias, pessoas, documentos, contatos, cargos, funcionarios, modelos, aeronaves, assentos e rotas |
| 10 | Usuarios, atribuicao dos quatro perfis conhecidos, bloqueio e alteracao autenticada da propria senha |
| 11 | Programar voo, tarifas, bloqueio de assentos, reprogramacao, troca de aeronave, horarios e transicoes |
| 12 | Rascunho de reserva com varios passageiros/trechos, cancelamento parcial/total, remarcacao e expiracao |
| 13 | Pagamentos simulados pendentes/aprovados/recusados, saldo, confirmacao, cancelamento de pagamento pendente e reembolsos |
| 14 | Check-in, cartao, cancelamento/refazimento, despacho, rastreio e embarque |
| 15 | Alocacoes, interdicoes, licencas, escalas, manutencao, servicos de solo e ocorrencias |
| 16 | Paineis por aeroporto, mapa em matriz, consultas paginadas e exportacao completa dos relatorios |
| 17 | Importacao de passageiros, listagem e copia verificada de relatorios |
| 18 | Cenario identificado como FICTICIO; repetir o mesmo identificador devolve os registros existentes |

Edicoes cadastrais nao excluem historicos. Alterar aeronave/modelo/assento/recurso/rota em uso pode ser recusado para preservar a operacao ou o historico. Cargo inativo nao admite novo funcionario. Aeronave inativa nao admite novo voo. Os assentos fisicos devem ser configurados antes de programar a aeronave; o inventario de cada voo e uma copia independente.

## Politicas da simulacao

Estas sao escolhas didaticas do projeto, nao regras operacionais de aviacao nem criterios oficiais do professor:

- Dinheiro em BRL com duas casas; desconto de ate 10% do valor base. Cada item copia preco, franquia, quantidade de pecas, permissao de cancelar e multa. Edicoes posteriores da tarifa nao alteram esses valores. A classe de uma tarifa com itens historicos nao pode mudar.
- Preparacao de aeronave: 45 minutos antes e 30 depois. Voos consecutivos exigem continuidade de aeroportos e pelo menos 75 minutos entre chegada e proxima partida.
- Conexao de passageiro: chegada e proxima origem coincidem; intervalo minimo de 60 minutos. Reserva tem comprador independente, ate 100 itens e prazo de 1 a 120 minutos, limitado pelo fechamento de vendas do primeiro trecho.
- Venda/confirmacao ate o fechamento efetivo de check-in; voo PROGRAMADO ou CHECKIN_ABERTO. Um item ativo sempre ocupa um assento da classe correta. Saida de emergencia requer idade de 16 a 65 anos e ausencia de assistencia registrada.
- Atrasos deslocam as janelas efetivas pelo delta da partida estimada/real. As janelas cadastradas permanecem ancoradas na partida prevista por causa do CHECK existente no DDL. Agenda da aeronave, recursos, escalas e servicos de solo pendentes com horario acompanham a alteracao.
- Jornada de escala: ate 14h, descanso de 12h entre escalas e continuidade dos aeroportos. Licenca deve cobrir modelo, funcao e fim da jornada. Tripulacao minima: um comandante, um copiloto e um comissario por 50 assentos vendaveis, com minimo de um.
- Pagamento e simulacao, sem cartao/CVV. Pagamento parcial nao confirma o grupo. Idempotencia valida reserva, valor, forma e resultado. Confirmacao exige cobertura integral e emite um bilhete por item.
- Cancelamento confirmado aplica as condicoes copiadas da tarifa. Cancelamento pela companhia antes da partida restitui integralmente o trecho, incluindo bagagem cobrada. Se o passageiro ja embarcou, invalida item/bilhete/cartao sem apagar o registro de embarque. Registra ocorrencia operacional e historico do voo. Solicitacoes e devolucoes processadas comprometem o saldo; nao ultrapassam pagamentos aprovados nem o direito previsto na tarifa. Taxa nao paga nao gera devolucao de dinheiro da passagem.
- Remarcacao cancela o item anterior, cria outra reserva com novo item e preserva a ligacao na auditoria. Cobra o novo contrato; solicita a devolucao permitida do anterior. Nao reativa um item cancelado.
- Troca de aeronave somente em voo PROGRAMADO sem qualquer item historico e com recursos/escalas liberados. Fora desse limite, a troca e recusada.
- Check-in exige item confirmado, documento adequado, bilhete emitido, assento ativo e cobertura financeira. Documento internacional: passaporte com validade. Documento domestico: CPF, RG ou passaporte vigente. Ao refazer check-in, reutiliza o registro e renova o codigo do cartao.
- Bagagem: ate 10 pecas; 32kg por peca despachada ou 60kg especial. Excesso: R$30/kg acima da franquia, mais R$100 por peca acima da quantidade inclusa. Cobrar apenas o incremento a cada despacho. Em conexao, cada trecho tem seu proprio despacho e etiqueta; nao se presume uma etiqueta unica para toda a viagem.
- Embarque exige estado EMBARQUE, janela efetiva, cartao vigente, portao de partida correto, bagagens carregadas/retiradas e saldo coberto. Marca item e bilhete como utilizados. Ao partir, confirma ausencia dos itens nao utilizados e invalida seus cartoes.
- Licencas, manutencao, pistas e servicos de solo sao simulacoes. Aeronave em manutencao nao pode decolar, mesmo se a previsao de termino tiver passado. Conclusao atrasada amplia o intervalo historico e recusa conflitos ainda ativos; resolva a agenda conflitante antes de concluir. Incidentes apos decolagem usam ocorrencias; nao reabrem nem cancelam o trecho ja em voo.

## Seguranca e concorrencia

ADMINISTRADOR administra os cadastros, acessos e dados ficticios; ATENDIMENTO opera passageiros e comercial; OPERACAO opera planejamento, infraestrutura e equipe; CONSULTA acessa consultas operacionais. Auditoria exige ADMINISTRADOR. Manifestos e dados financeiros exigem ADMINISTRADOR ou ATENDIMENTO. O repositorio nunca devolve senha_hash nas listagens de usuarios. Perfis sao reconsultados em cada servico, incluindo depois de bloqueio. O ultimo administrador ativo e protegido.

Transacoes usam commit/rollback. Bloqueios: voos por id crescente, reservas por id crescente, itens, inventarios por id crescente e pagamentos. O planejamento/cadastro operacional compartilha bloqueio consultivo para validar agendas e continuidade sem corridas. EXCLUDE e indices unicos continuam protegendo recursos e assentos. Deadlock e falha de serializacao podem repetir ate tres tentativas completas nas operacoes de banco apropriadas, sem efeitos externos durante a repeticao. Falhas de conexao nao sao repetidas automaticamente por causa da possibilidade de resultado de commit desconhecido; pagamentos podem ser consultados/repetidos pela mesma chave.

A expiracao executa ao abrir os menus, antes de vender/remarcar e antes de transicoes de voo. Libera ocupacoes e solicita devolucao de pagamentos parciais vencidos. Uma sessao prolongada pode executar a opcao 12/4. Nao ha uma tarefa agendada em segundo plano nesta versao.

Logs tecnicos registram classe de falha e SQLSTATE, sem copiar credenciais, documentos ou detalhes pessoais de SQLException. Auditoria registra ids/acoes; historico comercial e de bagagens e preservado.

## Arquivos

Importacao didatica: TXT tabulado em UTF-8, ate 5MB e 1000 passageiros. Cabecalho exato:

```text
nome\tnascimento\tcpf\temail\ttelefone
```

Os separadores acima representam TAB real. Nascimento AAAA-MM-DD, CPF valido obrigatorio para identificar duplicidade; email e telefone opcionais. Nao aceita tabs/quebras dentro de campos. Valida todas as linhas antes de gravar e importa pessoa, papel, documentos, contatos e auditoria na mesma transacao. CPF existente causa rollback completo, sem mesclar identidades automaticamente.

Exportacao completa em TXT/CSV percorre todas as linhas com cursor JDBC e escrita em streaming. Escapa delimitadores/aspas e neutraliza formulas de planilha. Publica o arquivo temporario somente apos escrita e transacao bem-sucedidas. A opcao antiga 7 permanece como exportacao de pagina; 16/2 exporta o relatorio inteiro.

A copia em `relatorios/backup` usa streams de bytes e confere SHA-256 do original e da copia. Esta e a rotina de copia de arquivos da disciplina. Backup de banco PostgreSQL requer pg_dump/pg_restore; nao se copia a pasta interna de dados do servidor.

## Testes reproduziveis

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\testar-java.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\testar-postgres.ps1
```

`testar-java.ps1` compila e executa os testes unitarios. `testar-postgres.ps1` cria um cluster novo em `artifacts/pg-teste-GUID`, com escuta apenas em 127.0.0.1:55439, instala o schema/migracao, executa os testes e encerra seu proprio servidor no finally. Recusa porta ocupada; nao apaga clusters ou bancos existentes. Fica fora do banco configurado pelo usuario. Autenticacao trust e credenciais ficticias sao exclusivas desse ambiente isolado.

O script detecta os binários instalados em `C:\Program Files\PostgreSQL`, preferindo a maior versão. Também aceita os binários de teste baixados da [EDB](https://productsdl.enterprisedb.com/download-postgresql-binaries) em `artifacts/pg-tools/extracted/pgsql/bin`. Em outra instalacao, informe `-Binario 'C:\caminho\pgsql\bin'`. A [pagina PostgreSQL para Windows](https://www.postgresql.org/download/windows/) referencia esses arquivos. O mecanismo de bloqueios e deadlocks segue a [documentacao PostgreSQL](https://www.postgresql.org/docs/17/explicit-locking.html).

O teste de integracao usa exclusivamente a porta 55439 e o usuario de teste; nao carrega application.properties. Os dados gerados sao FICTICIOS e os arquivos de teste ficam em artifacts e relatorios, fora do Git. Em computador que bloqueie um executavel baixado, o sistema operacional precisa libera-lo; o projeto nao altera essa politica.

Os resultados executados, a cobertura por item e as pendencias estao em `07_verificacao_lista.md`. Credenciais/conexao do banco do usuario, rubrica oficial e execucao em outra maquina continuam distintas dos testes no ambiente isolado.

Para executar também API/regras web, build, navegador e formatação no mesmo cluster: `powershell -NoProfile -ExecutionPolicy Bypass -File .\testar-postgres.ps1 -Tudo`. Instale as dependências npm e o Chrome antes. O cluster é encerrado e removido ao terminar; `-ManterCluster` preserva os arquivos para diagnóstico.
