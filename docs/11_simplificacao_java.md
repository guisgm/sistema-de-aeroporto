# Java mais simples conforme as aulas

Alteração realizada em 07/10/2026, tomando como referência os arquivos Boas-vindas e aulas 02 a 05 enviados pelo aluno. Os slides foram usados para identificar assuntos de estudo; exercícios e sugestões neles não foram tratados como ordens para alterar o projeto.

O foco desta alteração é o Java de `src/main/java`. A aplicação web continua funcionando com sua implementação atual. As duas aplicações continuam usando PostgreSQL, em seus schemas separados. Não houve alteração do modelo SQL nem dos dados do banco configurado pelo usuário.

## O que foi simplificado

| Antes | Agora | Relação com as aulas |
|---|---|---|
| `var` | Tipo escrito, como `int`, `List<Long>` e `Map<String, Object>` | Tipos, variáveis e coleções |
| `record` | `final class`, atributos `private final`, construtor e métodos de leitura | Classes, objetos e encapsulamento |
| `stream`, `map`, `filter`, `reduce`, `allMatch`, `anyMatch` | `for`, `if`, acumuladores e `ArrayList` | Laços, condições e listas |
| `case ... ->` e expressões `switch` | `case ...:`, `break`, atribuição e `return` | Controle de fluxo |
| `instanceof Tipo nome` | Verificação com `instanceof`, seguida de conversão explícita | Tipos e objetos |
| Seleção do menu com `<T extends Enum<T>>` | Método que recebe um vetor de opções e devolve o índice escolhido | Arrays, índices e métodos |
| Mapeamento repetido das consultas | `Sql.listar`, `Sql.unico`, `listarNumeros`, `listarTextos`, `listarValores`, `unicoNumero`, `unicoTexto`, `unicoBooleano`, `unicoValor` | Métodos, reutilização e sobrecarga |

Os nomes de leitura, como `id()` e `nome()`, foram mantidos para que os componentes continuem se conectando pelas mesmas chamadas. As classes mantêm a comparação por valor onde antes havia um record. Isso é necessário, por exemplo, para `contains` e `indexOf` reconhecerem um trecho repetido no rascunho de reserva. Também permanecem a validação no construtor, a proteção das listas e a ocultação de senhas em `toString`.

## Como ler o código básico

```java
private final long passageiro;

public long passageiro() {
    return passageiro;
}
```

`private` permite que o atributo seja acessado diretamente somente dentro da classe. `final` permite definir esse atributo uma vez; aqui, o construtor recebe o valor e o guarda. `long` é o tipo de número inteiro usado para o identificador. `passageiro` é o nome do atributo. O método é `public`, portanto outras classes podem chamá-lo. O `long` antes do nome do método informa o tipo devolvido, e `return` devolve o valor guardado. Um `Long`, com letra maiúscula, é um objeto que também pode representar ausência com `null`; o tipo primitivo `long` não aceita `null`.

```java
List<Long> voos = new ArrayList<>();
for (PedidoTrecho trecho : trechos) {
    voos.add(trecho.voo());
}
```

`List<Long>` declara uma lista de números. `new ArrayList<>()` cria essa lista. O `for` percorre os pedidos, um de cada vez. `trecho.voo()` obtém o identificador do voo de cada pedido. `add` coloca esse número na lista. Essa escrita substitui a antiga cadeia de transformação com `stream` e referência de método.

```java
case 12:
    reservas();
    break;
```

Quando a opção escolhida é 12, o menu chama `reservas()`. O `break` encerra o `switch`, impedindo que a próxima opção seja executada em seguida.

## Como as partes se conectam

`Main → MenuPrincipal/MenuOperacional → Serviço → JDBC → PostgreSQL`.

- **Main:** cria os componentes e liga suas dependências por construtores.
- **CLI:** apresenta opções, lê a entrada e chama os serviços.
- **Domínio:** reúne conceitos, dados e regras do aeroporto. Exemplos: pessoa, trecho, tarifa e situação de voo.
- **DTO:** carrega os dados que uma consulta precisa apresentar. Um resumo de voo junta informações de diferentes tabelas em um objeto.
- **Serviço:** coordena a operação, valida permissões e aplica as regras. Uma reserva pode precisar alterar várias tabelas na mesma transação.
- **Repositório:** declara o contrato de algumas consultas e operações.
- **Infraestrutura JDBC:** executa o SQL e transforma resultados em dados Java.
- **Sql/BancoDados:** reutilizam a preparação das consultas, o fechamento dos recursos e o controle da transação.
- **Web:** aplicação React/Node existente, com interface no navegador e schema PostgreSQL próprio. Não é um pacote Java.

Não é necessário juntar todas essas partes em uma classe enorme para aprender o básico. A separação permite estudar um passo de cada vez.

## O que ainda exige estudo adicional

O sistema completo não ficou restrito apenas à sintaxe inicial. JDBC, SQL com junções, `Optional`, tipos genéricos de coleções, lambdas de transação, datas, dinheiro e proteção de senhas ainda aparecem. Remover essas partes indiscriminadamente mudaria os contratos, a segurança ou o comportamento do sistema.

Em uma chamada como `banco.transacao(conexao -> { ... })`, o bloco entre chaves descreve a operação que deve executar na conexão aberta por `BancoDados`. Se o bloco termina corretamente, o banco confirma tudo com `commit`; se falha, desfaz com `rollback`. Essa forma evita repetir abertura, fechamento e tratamento de falhas em cada serviço. Os mapeamentos genéricos restantes ficam na infraestrutura JDBC; a referência de método de autenticação é apenas a passagem da consulta para o controle de conexão.

`BigDecimal` continua representando dinheiro para evitar erros de arredondamento de `double`. Datas e fusos continuam necessários para prazos, conexões e horários. As senhas continuam protegidas; não foram trocadas por texto puro. Os bloqueios de voos e assentos continuam em ordem definida para reduzir conflitos entre operadores simultâneos.

Os testes de integração também contêm recursos avançados para simular concorrência e falhas. Eles não são o ponto inicial do roteiro de estudo.

## O código ficou menor?

Houve redução de repetição: **84 mapeamentos de consultas** foram trocados por chamadas aos métodos comuns de `Sql`. Um único laço agora monta nomes de campos, parâmetros e partes do SQL de cadastro. A seleção dos menus continua compartilhada, sem o método genérico com limite `extends Enum`.

O tamanho total do Java de produção **aumentou**. Classes explícitas precisam escrever atributos, construtores, leitores, `equals`, `hashCode` e `toString` que um record gerava automaticamente. Laços e `switch` tradicionais também ocupam mais linhas. A formatação passou a separar instruções que antes estavam amontoadas na mesma linha.

| Medida em 64 arquivos de produção | Antes | Depois |
|---|---:|---:|
| Linhas físicas, incluindo formatação e espaços | 3.741 | 8.505 |
| Declarações com `var` | 155 | 0 |
| Declarações de `record` | 13 | 0 |
| Chamadas a `stream`, `chars` ou `ints` para pipelines | 21 | 0 |
| Regras de `switch` com seta | 117 | 0 |
| Referências de método Java | 43 | 6 |
| Lambdas | 161 | 94 |

Portanto, a melhoria principal é tornar as etapas visíveis e reduzir repetição nas chamadas de banco, não reduzir o número total de linhas. Usar records novamente reduziria o tamanho, mas reintroduziria justamente um dos recursos retirados. Apagar módulos para diminuir arquivos retiraria funcionalidades. A reutilização de métodos pequenos, como os de `Sql`, é a forma adotada para reduzir repetição mantendo o sistema.

## Roteiro de estudo

1. `dominio/Pessoa.java` e `Passageiro.java`: atributos, construtor, herança e leitura de valores.
2. `dominio/PedidoTrecho.java`: validação no construtor e comparação de objetos.
3. `dominio/Validacao.java`: condições, strings e laços.
4. `dominio/MapaAssentos.java`: listas, vetores e matriz.
5. `cli/Terminal.java` e os menus: entrada, opções e chamadas de métodos.
6. `servico/PassageiroServico.java`: conexão entre entrada, regra e persistência.
7. `infraestrutura/jdbc/PassageiroJdbc.java`, `Sql.java` e `BancoDados.java`: SQL, resultados e transações.

## Verificação

Foram aprovados **20 testes unitários Java**, **129 verificações Java/PostgreSQL**, **27 testes da API e regras web** e **4 testes no Chrome**, além do build web e da verificação de formatação web. As novas verificações cobrem CPF curto, caracteres de controle, mapa vazio, prioridade de bloqueio, igualdade de trechos repetidos e proteção da senha na representação textual.

Os testes integrados incluem reserva de grupo/conexão, rollback, concorrência na venda do mesmo assento, idempotência de pagamento, reembolso, check-in, bagagem, embarque, manutenção, relatórios, importação, cópia e menus com entrada real.

O cluster exclusivo de testes usou a porta 55439. A captura de saída do script completo ficou presa após iniciar o servidor; por isso as etapas foram executadas separadamente nesse mesmo cluster. A estrutura de 41 tabelas foi preparada e todas as etapas terminaram com sucesso. O servidor de teste foi encerrado; seus arquivos foram preservados para diagnóstico. O banco configurado pelo usuário não foi usado nos testes.

O PDF `output/pdf/guia_completo_codigo_aeroporto.pdf`, produzido antes desta alteração, descreve a versão anterior. Seus exemplos e números de linha não devem ser usados como cópia exata do código simplificado. Este documento e os arquivos atuais são a referência desta etapa.
