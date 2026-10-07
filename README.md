# Sistema de aeroporto — Projeto de Programação

Sistema com aplicação web operacional AeroHub e base Java 21 pelo terminal, com banco PostgreSQL e documentação da modelagem.

## Executar o AeroHub Web

A aplicação web oferece painel operacional, gestão de voos, passageiros, reservas, check-in, cartão de embarque, companhias, aeronaves, terminais, portões, auditoria e relatórios CSV. Usa React, API local e PostgreSQL. Java e web usam apenas PostgreSQL, com schemas distintos (`aeroporto` e `aerohub`); seus cadastros ainda são independentes.

Com Node.js 22.13 ou superior instalado:

```powershell
npm.cmd install
# Copie o exemplo e preencha db.url, db.usuario e db.senha se ainda nao configurou.
if (!(Test-Path config/application.properties)) { Copy-Item config/application.properties.example config/application.properties }
# Ativa os dados ficticios e os acessos de demonstracao.
$env:AEROHUB_DEMO = 'true'
npm.cmd run dev
```

Abra **http://localhost:5173** e selecione um perfil nos acessos de simulação. Para login manual, use `admin@aerohub.local`, `operador@aerohub.local` ou `atendente@aerohub.local`, com senha `AeroHub@2026!`.

Os dados ficam no schema `aerohub` do PostgreSQL configurado em `config/application.properties`. A API também aceita `DATABASE_URL` ou `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` e `PGPASSWORD`. O banco deve existir e o usuário precisa poder criar o schema. `AEROHUB_DEMO=true` habilita o provisionamento fictício; sem essa opção, nenhum usuário de demonstração é criado. A base inicial contém 144 voos fictícios, 4 companhias, 20 aeronaves, 3 terminais, 12 portões e 24 passageiros. Os dados são criados uma vez e preservados nas execuções seguintes.

Para o pacote compilado, execute `npm.cmd run build` e depois `npm.cmd start`. Verifique as regras com `npm.cmd test` e os fluxos no Chrome com `npm.cmd run test:e2e`.

A arquitetura, os perfis, as regras, os comandos e os limites da simulação estão em [docs/06_aplicacao_web.md](docs/06_aplicacao_web.md).

## Executar a base Java

O Java foi simplificado com base nas aulas 02 a 05: tipos explícitos, classes com construtores, laços e `switch` tradicional. As consultas repetidas foram centralizadas. Veja o que mudou, o roteiro de estudo e os limites dessa simplificação em [docs/11_simplificacao_java.md](docs/11_simplificacao_java.md).

Preencha `config/application.properties` com sua senha e os dados da conexão PostgreSQL. As credenciais reais não estão no Git. Em banco novo, instale `sql/criar_banco.sql`; em banco existente, não reexecute o instalador. Aplique a migração aditiva antes de abrir o menu:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\executar.ps1 -Migrar
```

No primeiro acesso, somente se ainda não houver usuário:

```powershell
.\executar.ps1 -Inicializar
```

Esse comando cria o primeiro administrador e seu aeroporto-base; use-o uma vez. Depois, abra o sistema:

```powershell
.\executar.ps1
```

A aplicação Java oferece administração dos cadastros e usuários, programação de voos, tarifas, inventário, agendas, reservas individuais/grupos/conexões, pagamentos simulados, bilhetes, expiração, cancelamento, remarcação e reembolsos. Também conecta check-in, cartões, bagagens, embarque, tripulação, recursos, manutenção, serviços de solo, ocorrências, relatórios completos, importação e cópia verificada de arquivos ao PostgreSQL. A web usa o schema `aerohub` e o Java usa `aeroporto`, no mesmo servidor PostgreSQL quando configurados assim; os modelos e os dados continuam independentes.

Os scripts compilam com javac e baixam o driver JDBC oficial, dispensando instalação de Maven. Para ajuda sem conectar ao banco, execute `.\executar.ps1 -Ajuda`. Ao receber o projeto em outro computador, copie `config/application.properties.example` para `config/application.properties` e preencha. O arquivo local de credenciais fica fora do pacote e do Git.

Os scripts procuram o JDK no PATH, JAVA_HOME e `.jdks`, compilando para Java 21. A execução foi verificada com JDK 21 e PostgreSQL 18.4 em ambiente isolado. O banco real do usuário ainda depende de configuração. Guia: [operação Java](docs/08_operacao_java.md); evidências e pendências dos 88 itens: [verificação](docs/07_verificacao_lista.md); demonstração: [roteiro](docs/09_roteiro_apresentacao.md).

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\testar-java.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\testar-postgres.ps1 -Tudo
```

O segundo comando detecta os binários PostgreSQL instalados no Windows (ou aceita `-Binario`). Com `-Tudo`, executa testes Java, API/regras web, build, navegador e formatação em um cluster isolado na porta 55439, e encerra e remove esse cluster ao terminar. `-ManterCluster` preserva os arquivos para diagnóstico. Não utiliza o banco configurado em `application.properties`. Instale as dependências npm antes; o navegador exige Chrome instalado.

## Arquivos

- `sql/web.sql`: estrutura PostgreSQL da aplicação web, aplicada automaticamente no schema exclusivo.
- `sql/criar_banco.sql`: instalador original das 41 tabelas, preservado.
- `sql/migracoes/001_cadastros_ativos.sql`: migração repetível que adiciona quatro indicadores de atividade, sem apagar registros.
- `docs/01_modelagem.md`: escopo, entidades, relacionamentos, regras e fluxos.
- `docs/02_dicionario_dados.md`: atributos, tipos, obrigatoriedade, chaves e restrições.
- `docs/03_topicos_disciplina.md`: aplicação dos conteúdos da disciplina no Java.
- `docs/04_diagramas.md`: diagramas entidade-relacionamento por módulo.
- `docs/05_base_java.md`: execução e organização do código Java.
- `docs/07_verificacao_lista.md`: classificação e evidências de cada requisito, resultados e limitações reais.
- `docs/08_operacao_java.md`: configuração, migração, menus, regras e testes isolados.
- `docs/09_roteiro_apresentacao.md`: apresentação oral e demonstração.
- `docs/11_simplificacao_java.md`: simplificação conforme as aulas, exemplos básicos, comparação e testes.
- `src/main/java`: código da aplicação, organizado em pacotes.
- `pom.xml`: projeto Maven para abrir na IDE.
- `compilar.ps1` e `executar.ps1`: compilação e execução pelo PowerShell.
- `testar-java.ps1` e `testar-postgres.ps1`: testes unitários e integração em PostgreSQL isolado.
- `config/application.properties.example`: exemplo de configuração sem senha.

## Executar no DBeaver

O PostgreSQL precisa estar funcionando e o banco `sistema_aeroporto` já deve existir. Para um ambiente novo, conecte ao banco `postgres`, deixe Auto-commit ativado e execute separadamente:

```sql
CREATE DATABASE sistema_aeroporto;
```

Depois:

1. Conecte ao banco `sistema_aeroporto`.
2. Abra `sql/criar_banco.sql` no editor SQL.
3. Deixe Auto-commit ativado.
4. Clique no código, pressione Ctrl+A e depois Ctrl+Enter.
5. Aguarde a mensagem `Estrutura criada: 41 tabelas no schema aeroporto.`

O arquivo contém um único comando DO, sem diretivas de psql nem BEGIN/COMMIT de controle de transação. O BEGIN/END dentro do bloco faz parte da linguagem PL/pgSQL. Toda a criação é atômica: se falhar, informa a etapa e desfaz as mudanças desse comando.

O script não apaga dados. Aceita um schema aeroporto vazio, mas interrompe a instalação se já houver objetos do projeto. Não é migração nem recriação de banco existente. A extensão btree_gist requer permissão de instalação.

## Conferir

Em outra aba SQL, execute:

```sql
SELECT COUNT(*) AS quantidade_tabelas
FROM information_schema.tables
WHERE table_schema = 'aeroporto'
  AND table_type = 'BASE TABLE';
```

O resultado esperado é 41. Atualize a conexão e expanda `Schemas → aeroporto → Tabelas`. A instalação cria a estrutura sem dados de demonstração.
