# Sistema de aeroporto — Projeto de Programação

Sistema com aplicação web operacional AeroHub e base Java 21 pelo terminal, com banco PostgreSQL e documentação da modelagem.

## Executar o AeroHub Web

A aplicação web oferece painel operacional, gestão de voos, passageiros, reservas, check-in, cartão de embarque, companhias, aeronaves, terminais, portões, auditoria e relatórios CSV. Usa React, API local e SQLite persistente. Funciona como simulação independente da base Java/PostgreSQL.

Com Node.js 22.13 ou superior instalado:

```powershell
npm.cmd install
npm.cmd run dev
```

Abra **http://localhost:5173** e selecione um perfil nos acessos de simulação. Para login manual, use `admin@aerohub.local`, `operador@aerohub.local` ou `atendente@aerohub.local`, com senha `AeroHub@2026!`.

Os dados ficam em `data/aerohub.sqlite`. A base inicial contém 144 voos fictícios, 4 companhias, 20 aeronaves, 3 terminais, 12 portões e 24 passageiros. Os dados são criados uma vez e preservados nas execuções seguintes.

Para o pacote compilado, execute `npm.cmd run build` e depois `npm.cmd start`. Verifique as regras com `npm.cmd test` e os fluxos no Chrome com `npm.cmd run test:e2e`.

A arquitetura, os perfis, as regras, os comandos e os limites da simulação estão em [docs/06_aplicacao_web.md](docs/06_aplicacao_web.md).

## Executar a base Java

Preencha `config/application.properties` com sua senha e os dados da conexão PostgreSQL. No terminal, dentro desta pasta:

```powershell
.\executar.ps1 -Inicializar
```

Esse comando cria o primeiro administrador e seu aeroporto-base; use-o uma vez. Depois, abra o sistema:

```powershell
.\executar.ps1
```

A base oferece login com perfis, cadastro/busca de passageiros, consultas de voos/assentos/reservas e exportação de uma página de voos em TXT/CSV. O banco precisa ter a estrutura já criada. Vendas, pagamentos, expiração, check-in e demais fluxos operacionais são os próximos incrementos.

Os scripts compilam com javac e baixam o driver JDBC oficial, dispensando instalação de Maven. Para ajuda sem conectar ao banco, execute `.\executar.ps1 -Ajuda`. Ao receber o projeto em outro computador, copie `config/application.properties.example` para `config/application.properties` e preencha. O arquivo local de credenciais fica fora do pacote e do Git.

O guia detalhado da arquitetura, dos perfis e das próximas etapas está em `docs/05_base_java.md`.

## Arquivos

- `sql/criar_banco.sql`: único script de criação das 41 tabelas, schema, extensão, restrições, índices, visões e histórico automático de voos.
- `docs/01_modelagem.md`: escopo, entidades, relacionamentos, regras e fluxos.
- `docs/02_dicionario_dados.md`: atributos, tipos, obrigatoriedade, chaves e restrições.
- `docs/03_topicos_disciplina.md`: aplicação dos conteúdos da disciplina no Java.
- `docs/04_diagramas.md`: diagramas entidade-relacionamento por módulo.
- `docs/05_base_java.md`: execução, organização do código, funcionalidades disponíveis e continuidade.
- `src/main/java`: código da aplicação, organizado em pacotes.
- `pom.xml`: projeto Maven para abrir na IDE.
- `compilar.ps1` e `executar.ps1`: compilação e execução pelo PowerShell.
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
