# Como os tópicos da disciplina entram no projeto Java

Fonte: arquivo “Relatorio tópicos PDP” fornecido pelo aluno, análise dos slides Boas-vindas e aulas 02 a 05. O relatório contém conceitos ensinados, checklist e sugestões de backend. A presença de um tópico nos slides não prova, por si só, que cada método citado seja obrigatório na entrega. A lista exata de avaliação deve ser conferida com o enunciado do professor.

O pedido do aluno é uma aplicação de aeroporto em Java pelo terminal. Portanto, os exemplos de produtos e REST do relatório são adaptados ao domínio. As tabelas abaixo preservam o plano original de cobertura; a seção "Cobertura executada" relaciona a implementação atual e não presume que cada API citada no material seja requisito obrigatório.

Na simplificação de 07/10/2026, a escrita do código foi adaptada às aulas: `var`, `record`, pipelines de Stream, pattern matching de `instanceof` e regras de `switch` com seta foram substituídos. Consultas repetidas agora usam métodos comuns em `Sql`. PostgreSQL, autorização e transações permanecem. Detalhes, exemplos e resultados: [11_simplificacao_java.md](11_simplificacao_java.md).

## Orientação a objetos e arquitetura

| Conteúdo | Aplicação concreta planejada |
|---|---|
| Classe, objeto, atributos e métodos | Classes Passageiro, Funcionario, Aeronave, Voo, Reserva, ItemReserva, Bagagem; métodos reservar, confirmar, cancelar, embarcar |
| Abstração e classe abstrata | Pessoa como base de identificação; abstração de persistência por interfaces |
| Encapsulamento e acesso | Campos private; alteração de estado somente por métodos que validam regras; construtores válidos |
| Herança, superclasse e subclasse | Pessoa → Passageiro / Funcionario; ExcecaoNegocio → ReservaExpiradaException e AssentoIndisponivelException |
| Polimorfismo e interfaces | ExportadorRelatorio com ExportadorTxt e ExportadorCsv; mesma chamada produzirá formatos diferentes |
| Overriding | @Override em exportadores e implementações de repositórios; toString legível nas entidades |
| Overloading | Consultar voo por id ou por companhia/número/instante; exportar relatório com destino padrão ou explícito |
| Object, equals, hashCode, toString | Igualdade por identidade persistida quando disponível; documentar regra para objeto ainda sem id; uso seguro em coleções |
| Construtores e parâmetros | Inicializar entidades com dados mínimos; passar ids e dados entre menu, serviço e repositório |
| Coesão, acoplamento e extensibilidade | Separar serviços de reserva, check-in, operação e financeiro; serviços dependem de interfaces de repositório |
| Pacotes e organização | br.edu.aeroporto.cli, dominio, servico, repositorio, infraestrutura.jdbc, excecao, relatorio |

## Fundamentos, sintaxe e controle de fluxo

| Conteúdo | Aplicação concreta planejada |
|---|---|
| Algoritmo, programa e automação | Sequência reservar → pagar → emitir → check-in → embarcar; expiração de pendências |
| main, static e import | Classe principal inicia configuração, conexão e menu; utilitário estático de normalização quando fizer sentido |
| Tipos primitivos e referências | boolean em disponibilidade, int em opção do menu, Long em ids, BigDecimal em dinheiro, LocalDate/OffsetDateTime nas datas |
| Operadores aritméticos, comparação, lógicos e unários | Calcular tarifa/desconto, conferir prazos, combinar validações, incrementar contadores de relatório |
| Scanner | Entrada do terminal; preferir nextLine e conversões explícitas para evitar quebra de leitura |
| if, else if, else e ternário | Validar reserva; classificar situação de pagamento; mostrar “Sim” ou “Não” em campos booleanos |
| switch, break e default | Menu principal e menus por módulo; rejeição de opção desconhecida; enum de estados |
| while e do while | Repetir menu até a saída; ler arquivo linha por linha |
| for e for-each | Percorrer mapa de assentos, itens de reserva e registros do relatório |
| break e continue | Encerrar busca quando encontra registro; pular linha vazia de importação |
| Compilador, bytecode, JVM, JDK/JRE, portabilidade | Documentar compilação/execução do programa e configuração externa; distribuir JAR com dependências adequadas |
| Memória, segurança da linguagem e bibliotecas | Usar APIs da JDK; não carregar arquivos grandes integralmente; tratar recursos externos |
| Multithread e concorrência | Expiração periódica é uma possível demonstração posterior; regras de banco já consideram múltiplos operadores. Não criar threads sem necessidade funcional |
| História e edições Java ME/SE/EE | Conteúdo conceitual para apresentação; programa usa Java SE. Não é necessário criar tabela para isso |

## Arrays, matrizes e coleções

| Conteúdo | Aplicação concreta planejada |
|---|---|
| Array, tamanho fixo, índice zero e inicialização | Vetor das opções do menu; acessar e percorrer por índice validado |
| Array como parâmetro e cópia | Método que formata cabeçalhos de relatório; Arrays.copyOf preserva vetor original |
| Matriz / multidimensional | Exibição do mapa de assentos em String[][] por fila e coluna; posições inexistentes são vazias, pois aeronaves podem ter mapas irregulares |
| Arrays.toString e deepToString | Diagnóstico do mapa e das opções em modo de desenvolvimento |
| Arrays.sort, equals/deepEquals, fill/setAll | Ordenar cópia de códigos de assento; comparar mapas antes de uma edição; inicializar células com símbolo de vazio |
| List/ArrayList e add/addAll | Montar lista de voos e combinar trechos de ida/volta |
| get, set, remove, contains, size, isEmpty | Editar seleção de passageiros e trechos no rascunho da reserva antes da gravação |
| clear, indexOf, lastIndexOf, subList | Limpar rascunho, localizar seleção e paginar resultados; usar somente quando adequado ao fluxo |
| Collections.sort, max, min, reverse, shuffle | Ordenar relatórios; encontrar maior/menor atraso; inverter apresentação. shuffle pode gerar seleção aleatória apenas em massa fictícia de demonstração |

O banco continua relacional: não gravar um array Java serializado no lugar de linhas de assentos ou itens. Arrays são adequados a estruturas fixas de apresentação; ArrayList a conjuntos que crescem no programa. A ordem persistida dos trechos é o campo ordem_trecho.

## Strings

| Conteúdo | Aplicação concreta planejada |
|---|---|
| equals e equalsIgnoreCase | Comparar localizadores/opções sem usar == para conteúdo |
| startsWith, endsWith e contains | Filtro de nome de passageiro, código do voo e seleção de arquivos TXT/CSV |
| strip, isBlank, length, charAt, substring, indexOf/lastIndexOf | Validar entradas, tratar prefixos de códigos e separar nome/extensão de arquivo |
| replace e replaceAll | Remover pontuação de CPF/telefone e normalizar identificadores; preservar número de passaporte conforme regra do emissor |
| toUpperCase/toLowerCase e Locale.ROOT | Normalizar localizadores, códigos IATA/ICAO e login |
| StringBuilder, format/formatted | Construir relatórios e alinhar colunas no terminal |
| split, join e StringJoiner | Importar arquivo didático delimitado; montar linha de relatório. CSV com aspas e delimitadores em campos exige parser adequado |
| String.valueOf, parseInt/Double/Boolean | Conversão de ids/opções e campos simples; valores monetários usam new BigDecimal(texto), nunca double |
| NumberFormatException | Entrada numérica inválida exibe mensagem e repete leitura |
| Text blocks | SQL legível em repositórios; consultas parametrizadas com PreparedStatement |
| Imutabilidade, memória e concatenação | Explicar String imutável; StringBuilder em loops; não depender de interning para igualdade |

## Exceções, arquivos e persistência

| Conteúdo | Aplicação concreta planejada |
|---|---|
| throw, throws e exceções personalizadas | ReservaExpiradaException, AssentoIndisponivelException, PersistenciaException, ImportacaoException |
| try/catch e múltiplos catch | Diferenciar NumberFormatException, IOException e SQLException; mensagens úteis no terminal |
| Checked/unchecked e propagação | IOException/SQLException são tratadas na infraestrutura e convertidas com causa preservada quando necessário; regras de domínio têm exceções próprias |
| finally e try-with-resources | Scanner centralizado e fechamento de arquivos, ResultSet, PreparedStatement e conexão; justificar preferência por fechamento automático |
| Não usar exceção como fluxo habitual | Menu verifica opção e campos vazios antes de chamar operação; ausência em busca retorna Optional quando apropriado |
| FileReader/FileWriter e BufferedReader/BufferedWriter | Importação e exportação de TXT em UTF-8; se demonstrar FileReader/Writer, usar construtores com charset explícito |
| Files, Path/Paths, read/writeString, readAllLines | Configuração e arquivos pequenos; relatório grande é processado em streaming |
| InputStream/OutputStream e Reader/Writer | Cópia de arquivo de exportação e leitura textual; distinguir bytes de caracteres |
| Append | Log textual local de execução com acréscimo de linhas |
| File, exists, isFile/isDirectory, length, canRead/canWrite | Validar entrada de importação, tamanho e diretório de saída; ainda tratar erro de I/O após a checagem |
| Listagem de arquivos e diretórios | Menu lista relatórios e arquivos de importação disponíveis |
| Charset UTF-8 | Preservar nomes e descrições com acentos |
| Backup/cópia e configuração | Copiar arquivos de exportação; backup real do PostgreSQL requer ferramenta do banco e não se faz copiando sua pasta de dados |
| JDBC, conexão, SQLException e fechamento | Repositórios PostgreSQL; PreparedStatement; transações com commit/rollback e fechamento correto |
| Logs e eventos | evento_auditoria para operações; historico_voo para alterações; eventos de bagagem; logger local para falhas técnicas |

## Aplicação pelo terminal

Estrutura prevista: `Main → Menu → Serviço → Interface de Repositório → Repositório JDBC → PostgreSQL`.

O menu apresenta informações e lê escolhas. O serviço aplica regras, autorização e transações. O repositório executa SQL parametrizado. Relatórios são componentes separados. Um serviço que altera várias tabelas compartilha a mesma conexão/transação entre os repositórios envolvidos.

JPA, Servlet, Web Services, REST e GUI aparecem como conteúdos ou sugestões no relatório. JDBC atende a persistência desta proposta. API HTTP e interface gráfica não foram incluídas no escopo solicitado. Se o enunciado formal exigir essas tecnologias, haverá uma etapa adicional; não é uma exigência comprovada somente pela análise dos slides.

## Checklist de entrega desta etapa

- [x] Modelagem conceitual e lógica do domínio.
- [x] Script físico PostgreSQL com tabelas, restrições, índices e visões.
- [x] Diagramas e dicionário de dados.
- [x] Regras e limites de cada operação.
- [x] Plano para aplicar os tópicos no Java.
- [x] Execução do script em PostgreSQL 18.4 isolado e verificação das restrições; banco real do usuário pendente.
- [x] Base Java com menus, repositórios e serviços; escopo atual documentado em `05_base_java.md`.
- [x] Implementação dos demais cadastros e fluxos comerciais/operacionais Java, com testes integrados.
- [ ] Demonstração prática dos tópicos obrigatórios conforme a avaliação do professor.

## Cobertura executada

| Conteúdo | Uso funcional no código atual | Verificação |
|---|---|---|
| POO, herança, encapsulamento, equals/hashCode | Pessoa, Passageiro e Funcionario preservados; mesma identidade nos dois papéis | UnidadeTeste e IntegracaoTeste.cadastros |
| Interfaces, overriding e sobrecarga | Repositórios e exportadores originais; PassageiroServico.buscar por id/nome; criação de cenário com identificador opcional | Compilação e consultas/arquivos no PostgreSQL |
| Scanner, conversões, switch e loops | Terminal, MenuPrincipal e MenuOperacional; reserva montada antes de gravar | Login e navegação por entrada real do terminal |
| Vetores e matriz irregular | MapaAssentos: int[], String[], String[][]; posições inexistentes permanecem vazias | UnidadeTeste verifica dimensões, estados e isolamento de cópias; mapa real no terminal |
| Arrays.binarySearch/fill/copyOf/equals/deepEquals e Collections.sort | Laços montam os vetores de fila/coluna e copiam as linhas da matriz; listas ordenadas, preenchimento de espaços e proteção do mapa; limpeza de senhas preservada | UnidadeTeste e consulta integrada de assentos |
| ArrayList e generics | Rascunho com add/get/set/remove/contains/indexOf/size/isEmpty/clear/subList; combinação de ids com addAll | testarRascunho aprovado: adicionar/listar/editar/remover/limpar/gravar pelo terminal, com conferência no PostgreSQL |
| Collections.sort/min/max | Resumo de atrasos calcula menor, maior e mediana sem alterar dados persistidos | IntegracaoTeste verifica atraso real e relatórios |
| Collections.shuffle | Distribuição determinística de inserção dos assentos na massa FICTICIA | Criação e repetição idempotente da massa no PostgreSQL |
| Collections.reverse e Arrays.toString | Ordem inversa opcional de consultas e cabeçalho de colunas do mapa no terminal | Compilação e execução do mapa; opção de inversão ainda sem teste individual |
| Strings, regex e StringBuilder | CPF/contatos, códigos, condições comerciais, cabeçalho TAB, seleção de extensões; Locale.ROOT; StringJoiner nos relatórios | UnidadeTeste, importações válidas/inválidas e exportação |
| Exceções, causa, finally e recursos | RegraNegocioException/PersistenciaException; BancoDados e ArquivosServico; fechamento JDBC/streams; finally do servidor de teste | Rollback por falha injetada, restrições e deadlock real |
| FileReader/BufferedReader e UTF-8 | Importação TXT tabulada, limitada e transacional | Importação válida e falha na segunda linha sem gravação parcial |
| FileWriter/BufferedWriter | Relatórios completos TXT/CSV em streaming; exportadores anteriores preservados | Todos os tipos consultados/exportados; CSV com mais de 20 linhas |
| File/Path/Files e streams de bytes | Validação/listagem de arquivos; cópia em relatorios/backup, SHA-256 verificado | Files.mismatch entre original e cópia |
| Concorrência | ExecutorService/CountDownLatch nos testes, bloqueios e transações PostgreSQL nos serviços | Venda simultânea do mesmo assento, pagamento com mesma chave e deadlock real |

Os slides Boas-vindas e aulas 02 a 05 foram relidos, incluindo o XML textual dos PPTX. O relatório PDP foi preservado como fonte de sugestões, não como enunciado oficial. Métodos como ArrayList.lastIndexOf e Arrays.deepToString não foram adicionados apenas para constarem no código: se forem obrigatórios na rubrica, será necessário confirmar o uso exigido. GUI/eventos, REST/JPA e empacotamento JAR também não têm obrigatoriedade comprovada para esta entrega pelo terminal. A simulação web anterior não é evidência de cumprimento de uma eventual exigência de GUI Java.

Não foi localizada a rubrica oficial, portanto o último item do checklist permanece pendente. O roteiro oral e os exemplos estão em [09_roteiro_apresentacao.md](09_roteiro_apresentacao.md); a relação completa dos requisitos está em [07_verificacao_lista.md](07_verificacao_lista.md).
