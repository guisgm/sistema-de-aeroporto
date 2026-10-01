# Base Java — execução e continuidade

## O que foi entregue

Uma aplicação de terminal Java 21, com JDBC PostgreSQL, configuração externa, primeiro acesso, autenticação, perfis, cadastro e busca de passageiros, consultas de voos/assentos/reservas e exportação de uma página de voos em TXT ou CSV. O driver JDBC é a única dependência de execução fora da JDK.

Esta é a base do programa. Não implementa ainda vendas, emissão de bilhetes, pagamentos, expiração, cancelamento, check-in, bagagens, embarque, planejamento de frota, escalas ou manutenção. Nenhuma opção do menu promete executar essas operações. As consultas a voos e reservas leem registros já existentes; a inicialização não inventa voos ou reservas.

## Preparar e executar

O computador já tem JDK 21. O banco sistema_aeroporto precisa conter a estrutura criada pelo script único `sql/criar_banco.sql`. Não reexecute a criação se as tabelas já existirem.

1. Abra a pasta do sistema no terminal PowerShell.
2. Preencha `config/application.properties` com a senha e os dados da sua conexão PostgreSQL. O arquivo local foi preparado a partir do exemplo e fica fora do Git e do ZIP de entrega. Em outro computador, copie `config/application.properties.example` para `config/application.properties` antes de preencher.
3. Execute o primeiro acesso uma vez:

```powershell
.\executar.ps1 -Inicializar
```

4. Informe os dados do administrador e do aeroporto-base. A inicialização cadastra Brasil, cidade, aeroporto, cargo, pessoa, funcionário, usuário e perfis em uma única transação, preservando cadastros equivalentes já existentes. A senha precisa ter 12 a 128 caracteres. Ela não tem valor padrão.
5. Execute o menu:

```powershell
.\executar.ps1
```

Para mostrar instruções sem conectar ao banco:

```powershell
.\executar.ps1 -Ajuda
```

Se o PowerShell bloquear scripts, execute somente para esse processo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\executar.ps1 -Inicializar
```

Não é necessário instalar Maven para executar pelos scripts. `compilar.ps1` usa javac, baixa o driver oficial pgJDBC 42.7.13 se necessário e confere o SHA-256 fixado. O driver foi conferido no site do pgJDBC e no Maven Central. O JAR fica em lib e não é enviado no pacote; a primeira execução em outro computador precisa de internet. O pom.xml também permite abrir o projeto como Maven na IDE.

Variáveis de ambiente AEROPORTO_DB_URL, AEROPORTO_DB_USUARIO, AEROPORTO_DB_SENHA e AEROPORTO_FUSO têm prioridade sobre o arquivo. Execute os scripts a partir da pasta do sistema; eles ajustam o diretório de trabalho automaticamente. Na IDE, configure a pasta do projeto como diretório de trabalho e br.edu.aeroporto.Main como classe principal.

## Organização do código

```text
src/main/java/br/edu/aeroporto/
├── Main.java                  montagem das dependências e início
├── cli/                       leitura e apresentação no terminal
├── config/                    configuração externa
├── dominio/                   identificação, validação, estados e sessão
├── dto/                       resultados imutáveis de consultas
├── servico/                   casos de uso e limites das transações
├── repositorio/               contratos das consultas e da persistência
├── infraestrutura/jdbc/       SQL, conexão, autorização e auditoria
├── seguranca/                 geração e verificação do hash de senha
├── relatorio/                 exportadores TXT e CSV
└── excecao/                   erros de domínio e persistência
```

`Main` injeta as dependências pelos construtores. O menu depende de serviços e não contém SQL. Os repositórios têm contratos úteis para permitir outra implementação futuramente. A conexão JDBC é passada explicitamente entre componentes da mesma operação; esta base não cria um framework de persistência ou uma abstração CRUD genérica para todas as 41 tabelas.

### Domínio e dados

Pessoa é uma classe abstrata com estado encapsulado e imutável. Passageiro e Funcionario sobrescrevem identificacao, mantendo o id comum de pessoa. Uma mesma pessoa nos dois papéis é igual por id; não há objetos de domínio com id provisório. Os pedidos de cadastro usam record separado da entidade persistida.

SituacaoVoo e SituacaoReserva são enums que coincidem com o banco. SituacaoVoo inclui a regra de transição prevista no MD, mas ainda não há serviço que altere o voo. Essa regra será utilizada junto às validações operacionais, às agendas e ao histórico.

Resultados de consulta usam records. Dinheiro é BigDecimal; datas civis são LocalDate; TIMESTAMPTZ é lido como OffsetDateTime. O painel usa o fuso da configuração para filtrar um dia inteiro e os fusos dos aeroportos para exibir os horários. A conexão também recebe esse fuso para os cálculos de CURRENT_DATE. Datas inválidas de entrada são tratadas no terminal; nascimento futuro é rejeitado no serviço.

### Persistência e transações

BancoDados abre e fecha cada conexão e centraliza commit/rollback. Consultas usam uma transação somente de leitura com snapshot consistente (REPEATABLE READ). Escritas usam o isolamento padrão READ COMMITTED; futuras operações comerciais precisam dos bloqueios de linhas previstos na modelagem.

Sql centraliza PreparedStatement, bind de valores e mapeamento. Nenhuma entrada é concatenada ao SQL. Os recursos usam try-with-resources. Erros SQL são convertidos em mensagens adequadas, com causa preservada. Rollback que falhar é anexado como erro suprimido, preservando a falha original.

O cadastro de passageiro grava pessoa, papel de passageiro, CPF opcional, contatos e auditoria juntos. Qualquer falha desfaz tudo. O CPF é normalizado e seus dígitos verificadores são conferidos. A pessoa pode existir sem CPF, nacionalidade ou contatos, como permite o modelo. Nacionalidade, assistência e outros documentos serão acrescentados em uma etapa posterior.

A busca de passageiros é paginada em 20 registros; nomes são filtros literais, sem tratar % ou _ como curingas. Consultas de voos também usam 20 registros por página. A consulta de uma reserva usa duas consultas fixas, e não uma consulta por passageiro/trecho. O total apresentado é o preço original dos itens, incluindo cancelados; não representa saldo financeiro e não inclui bagagens.

### Acesso, auditoria e arquivos

O primeiro acesso só funciona quando não há usuário cadastrado. Um bloqueio consultivo do PostgreSQL serializa inicializações simultâneas. Não há usuário/senha pré-fixados. Senhas usam PBKDF2-HMAC-SHA256, sal aleatório e comparação constante. Entrada oculta funciona no console real; consoles de algumas IDEs não permitem ocultar caracteres e avisam o operador.

ADMINISTRADOR e ATENDIMENTO podem cadastrar passageiros. ADMINISTRADOR, ATENDIMENTO, OPERACAO e CONSULTA podem acessar as consultas atuais, inclusive passageiros e reservas. Perfis adicionais cadastrados no banco exigem política correspondente no Java. A cada operação, autorização consulta novamente o usuário ativo, vínculo e perfis; ela não depende de esconder itens do menu.

Auditoria registra login, primeiro acesso e cadastro de passageiro. O helper definirAutorDoVoo prepara o autor/motivo da trigger existente para futuros serviços de voo. Logs técnicos ficam em logs com rotação e registram tipo de falha/SQLSTATE, sem copiar senha, CPF ou detalhes pessoais da exceção SQL.

ExportadorVoos tem implementações TXT e CSV para demonstrar polimorfismo real. O relatório contém somente a página consultada, não todos os voos do período. A escrita usa BufferedWriter em UTF-8, um arquivo temporário e publicação após concluir a escrita. O CSV protege aspas, delimitadores e fórmulas de planilha. O diretório relatorios é fixado pela aplicação.

## Conteúdos já demonstrados e conteúdos futuros

| Conteúdo | Exemplo na base |
|---|---|
| Abstração, herança e encapsulamento | Pessoa, Passageiro e Funcionario |
| Polimorfismo e overriding | ExportadorVoos, ExportadorTxt, ExportadorCsv; identificacao dos papéis |
| Interface e baixo acoplamento | PassageiroRepositorio, UsuarioRepositorio, ConsultasRepositorio |
| Sobrecarga | PassageiroServico.buscar por id ou nome/página |
| Enum e switch | Estados e escolha das opções de menu |
| List, Set, generics e Optional | Resultados, perfis, ausência de registro, helpers JDBC |
| Loops e condicionais | Menus, CPF, listagem e exportação |
| Strings e regex | strip, isBlank, replaceAll, matches, split, formatação e normalização |
| Arrays e métodos utilitários | Vetor de argumentos, hash/sal, Arrays.fill para limpar senhas |
| Stream | Soma das passagens e validações simples |
| Exceções e recursos | Exceções específicas, causa preservada, finally e try-with-resources |
| Arquivos e UTF-8 | Configuração, logs, TXT e CSV |
| Banco | JDBC, PreparedStatement, transações, paginação e auditoria |

Matriz do mapa de assentos, importação, mini backup, outros métodos de Arrays/Collections e funcionalidades operacionais ainda não foram implementados. O MD dos tópicos permanece como plano de cobertura integral. Não há métodos vazios para fingir a conclusão desses conteúdos.

## Próximos incrementos

1. Cadastros administrativos de companhias, modelos, aeronaves, assentos e rotas; edição/inativação de pessoas e documentos adicionais.
2. Programação de voo: agenda da aeronave, inventário de assentos, janelas e tarifas na mesma transação.
3. Reserva: um item por passageiro/trecho, snapshots de tarifa e uma ocupação ativa por item; expiração explícita ao iniciar e antes de vender.
4. Pagamento simulado e emissão de bilhete, com cobertura integral e idempotência.
5. Cancelamentos e reembolsos, preservando histórico e respeitando os bloqueios e saldos definidos.
6. Check-in, bagagens, embarque, recursos, tripulação, manutenção e serviços em solo.

O SQL e as regras dos MDs continuam sendo o contrato de persistência. Novos serviços devem bloquear voos → reservas → itens → inventários → pagamentos, sempre por id crescente dentro de cada grupo, e compartilhar a mesma conexão JDBC entre repositórios. Nenhum serviço deve salvar um estado confirmado sem as entidades dependentes exigidas pelo modelo.

## Validação desta entrega

A compilação foi concluída com o JDK 21 disponível e a execução de executar.ps1 -Ajuda terminou com sucesso. Essa opção é independente do banco. Conexão, primeiro acesso e fluxos com PostgreSQL ainda precisam ser exercitados após preencher as credenciais locais; a entrega não utiliza nem lê a senha salva no DBeaver.

Referências: [pgJDBC e versões do driver](https://jdbc.postgresql.org/download/) e [Maven Compiler Plugin](https://maven.apache.org/plugins/maven-compiler-plugin/).
