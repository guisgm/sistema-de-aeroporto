# Dicionário de dados — PostgreSQL

Este dicionário cobre as 41 tabelas do schema `aeroporto` e reproduz as definições do script `sql/criar_banco.sql`.

PK = chave primária; FK = chave estrangeira. Campos PK são obrigatórios mesmo quando a declaração não repete NOT NULL. Campos opcionais aceitam NULL. Uma PK composta exige todos os seus componentes.

Todas as referências usam NO ACTION por padrão. BIGINT gerado por identity é atribuído pelo banco. NUMERIC monetário usa BRL. TIMESTAMPTZ representa instante; a exibição utiliza o fuso do aeroporto.

A coluna “Definição física” contém tipo, valores padrão e restrições locais. As restrições compostas são apresentadas após cada tabela. Restrições de negócio entre entidades são detalhadas na modelagem.

## 1. pais

País e código internacional usado na localização e nos documentos.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `codigo` | Sim | `CHAR(2) NOT NULL UNIQUE CHECK (codigo ~ '^[A-Z]{2}$')` | Código dentro do contexto do cadastro. |
| `nome` | Sim | `VARCHAR(100) NOT NULL CHECK (btrim(nome) <> '')` | Nome descritivo; não é identificador único de pessoa. |

## 2. cidade

Cidade e região dentro de um país.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `pais_id` | Sim | `BIGINT NOT NULL REFERENCES pais(id)` | País ao qual o registro pertence. |
| `nome` | Sim | `VARCHAR(120) NOT NULL` | Nome descritivo; não é identificador único de pessoa. |
| `regiao` | Sim | `VARCHAR(100) NOT NULL DEFAULT ''` | Estado/província/região, ou texto vazio quando não aplicável. |

Restrições compostas ou da tabela:

```sql
UNIQUE (pais_id, nome, regiao)
```

## 3. aeroporto

Aeroporto, códigos de identificação e fuso de exibição.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `cidade_id` | Sim | `BIGINT NOT NULL REFERENCES cidade(id)` | Cidade do aeroporto. |
| `codigo_iata` | Não | `CHAR(3) UNIQUE CHECK (codigo_iata ~ '^[A-Z]{3}$')` | Identificador IATA normalizado em letras maiúsculas. |
| `codigo_icao` | Sim | `CHAR(4) NOT NULL UNIQUE CHECK (codigo_icao ~ '^[A-Z]{4}$')` | Identificador ICAO normalizado. |
| `nome` | Sim | `VARCHAR(160) NOT NULL` | Nome descritivo; não é identificador único de pessoa. |
| `fuso_horario` | Sim | `VARCHAR(80) NOT NULL` | Identificador IANA, por exemplo America/Sao_Paulo; validado no Java. |
| `ativo` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Inativação cadastral; não apagar registros operacionais referenciados. |

## 4. terminal

Terminal de um aeroporto.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `aeroporto_id` | Sim | `BIGINT NOT NULL REFERENCES aeroporto(id)` | Aeroporto ao qual se refere o registro. |
| `codigo` | Sim | `VARCHAR(15) NOT NULL` | Código dentro do contexto do cadastro. |
| `nome` | Sim | `VARCHAR(100) NOT NULL` | Nome descritivo; não é identificador único de pessoa. |
| `ativo` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Inativação cadastral; não apagar registros operacionais referenciados. |

Restrições compostas ou da tabela:

```sql
UNIQUE (aeroporto_id, codigo),
UNIQUE (id, aeroporto_id)
```

## 5. recurso_aeroportuario

Portão, pista, posição de aeronave, esteira ou balcão.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `aeroporto_id` | Sim | `BIGINT NOT NULL REFERENCES aeroporto(id)` | Aeroporto ao qual se refere o registro. |
| `terminal_id` | Não | `BIGINT` | Terminal associado, quando aplicável. |
| `tipo` | Sim | `VARCHAR(20) NOT NULL CHECK (tipo IN ('PORTAO','PISTA','POSICAO','ESTEIRA','BALCAO'))` | Categoria controlada pelos valores do CHECK. |
| `codigo` | Sim | `VARCHAR(20) NOT NULL` | Código dentro do contexto do cadastro. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL'         CHECK (situacao IN ('DISPONIVEL','INTERDITADO','MANUTENCAO'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `comprimento_m` | Não | `NUMERIC(8,2) CHECK (comprimento_m > 0)` | Comprimento da pista em metros; obrigatório para recurso PISTA. |
| `envergadura_max_m` | Não | `NUMERIC(6,2) CHECK (envergadura_max_m > 0)` | Limite de envergadura aceito pelo recurso, se aplicável. |

Restrições compostas ou da tabela:

```sql
FOREIGN KEY (terminal_id, aeroporto_id) REFERENCES terminal(id, aeroporto_id),
UNIQUE (aeroporto_id, tipo, codigo),
CHECK (tipo <> 'PISTA' OR comprimento_m IS NOT NULL),
CHECK (tipo <> 'PORTAO' OR terminal_id IS NOT NULL)
```

## 6. companhia_aerea

Empresa que opera voos ou possui aeronaves.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `pais_id` | Sim | `BIGINT NOT NULL REFERENCES pais(id)` | País ao qual o registro pertence. |
| `codigo_icao` | Sim | `CHAR(3) NOT NULL UNIQUE CHECK (codigo_icao ~ '^[A-Z0-9]{3}$')` | Identificador ICAO normalizado. |
| `codigo_iata` | Não | `CHAR(2) UNIQUE CHECK (codigo_iata ~ '^[A-Z0-9]{2}$')` | Identificador IATA normalizado em letras maiúsculas. |
| `nome` | Sim | `VARCHAR(150) NOT NULL` | Nome descritivo; não é identificador único de pessoa. |
| `ativo` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Inativação cadastral; não apagar registros operacionais referenciados. |

## 7. pessoa

Identidade comum aos papéis de passageiro, funcionário e comprador.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `nacionalidade_id` | Não | `BIGINT REFERENCES pais(id)` | País de nacionalidade, se informado. |
| `nome` | Sim | `VARCHAR(160) NOT NULL CHECK (btrim(nome) <> '')` | Nome descritivo; não é identificador único de pessoa. |
| `nascimento` | Sim | `DATE NOT NULL` | Data civil de nascimento; serviço rejeita data futura. |
| `criado_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de criação do registro. |
| `ativo` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Inativação cadastral; não apagar registros operacionais referenciados. |

## 8. documento_pessoa

Um documento de identificação de uma pessoa.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `pessoa_id` | Sim | `BIGINT NOT NULL REFERENCES pessoa(id)` | Identidade da pessoa; nas tabelas de papel também é a chave primária. |
| `pais_emissor_id` | Sim | `BIGINT NOT NULL REFERENCES pais(id)` | País que emitiu o documento. |
| `tipo` | Sim | `VARCHAR(20) NOT NULL CHECK (tipo IN ('CPF','RG','PASSAPORTE','OUTRO'))` | Categoria controlada pelos valores do CHECK. |
| `numero` | Sim | `VARCHAR(40) NOT NULL CHECK (btrim(numero) <> '')` | Número/código do documento, licença ou operação conforme a entidade. |
| `validade` | Não | `DATE` | Data civil de validade, quando aplicável. |

Restrições compostas ou da tabela:

```sql
UNIQUE (pais_emissor_id, tipo, numero)
```

## 9. contato_pessoa

Um e-mail ou telefone; no máximo um principal por tipo/pessoa.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `pessoa_id` | Sim | `BIGINT NOT NULL REFERENCES pessoa(id)` | Identidade da pessoa; nas tabelas de papel também é a chave primária. |
| `tipo` | Sim | `VARCHAR(15) NOT NULL CHECK (tipo IN ('EMAIL','TELEFONE'))` | Categoria controlada pelos valores do CHECK. |
| `valor` | Sim | `VARCHAR(160) NOT NULL CHECK (btrim(valor) <> '')` | E-mail ou telefone conforme o tipo de contato. |
| `principal` | Sim | `BOOLEAN NOT NULL DEFAULT FALSE` | Marca o contato preferido dentro do tipo. |

Restrições compostas ou da tabela:

```sql
UNIQUE (pessoa_id, tipo, valor)
```

## 10. passageiro

Papel de cliente que pode viajar em um voo.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `pessoa_id` | Sim | `BIGINT PRIMARY KEY REFERENCES pessoa(id)` | Identidade da pessoa; nas tabelas de papel também é a chave primária. |
| `codigo_cliente` | Sim | `VARCHAR(30) NOT NULL UNIQUE` | Identificador público do cadastro de passageiro. |
| `observacoes_assistencia` | Não | `TEXT` | Necessidades de assistência voluntariamente informadas; limitar informação sensível. |

## 11. cargo

Cargo e área funcional do funcionário.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `nome` | Sim | `VARCHAR(80) NOT NULL UNIQUE` | Nome descritivo; não é identificador único de pessoa. |
| `area` | Sim | `VARCHAR(25) NOT NULL CHECK (area IN ('ADMINISTRACAO','ATENDIMENTO','TRIPULACAO','MANUTENCAO','OPERACAO','SEGURANCA'))` | Área de atuação do cargo. |

## 12. funcionario

Papel de pessoa empregada no aeroporto ou companhia.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `pessoa_id` | Sim | `BIGINT PRIMARY KEY REFERENCES pessoa(id)` | Identidade da pessoa; nas tabelas de papel também é a chave primária. |
| `cargo_id` | Sim | `BIGINT NOT NULL REFERENCES cargo(id)` | Cargo cadastrado. |
| `aeroporto_base_id` | Sim | `BIGINT NOT NULL REFERENCES aeroporto(id)` | Aeroporto de lotação do funcionário. |
| `companhia_id` | Não | `BIGINT REFERENCES companhia_aerea(id)` | Companhia associada ao voo, aeronave ou vínculo de trabalho. |
| `matricula` | Sim | `VARCHAR(30) NOT NULL UNIQUE` | Identificação da aeronave ou do empregado conforme a tabela. |
| `admissao` | Sim | `DATE NOT NULL` | Data de início do vínculo de trabalho. |
| `desligamento` | Não | `DATE` | Data de fim do vínculo, se encerrado. |

Restrições compostas ou da tabela:

```sql
CHECK (desligamento IS NULL OR desligamento >= admissao)
```

## 13. modelo_aeronave

Modelo técnico com dimensões e requisito didático de pista.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `fabricante` | Sim | `VARCHAR(100) NOT NULL` | Fabricante do modelo de aeronave. |
| `nome` | Sim | `VARCHAR(80) NOT NULL` | Nome descritivo; não é identificador único de pessoa. |
| `envergadura_m` | Sim | `NUMERIC(6,2) NOT NULL CHECK (envergadura_m > 0)` | Envergadura do modelo em metros. |
| `comprimento_pista_min_m` | Sim | `NUMERIC(8,2) NOT NULL CHECK (comprimento_pista_min_m > 0)` | Mínimo didático de pista; não representa cálculo aeronáutico real. |

Restrições compostas ou da tabela:

```sql
UNIQUE (fabricante, nome)
```

## 14. habilitacao_tripulante

Licença do funcionário para função em determinado modelo.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `funcionario_id` | Sim | `BIGINT NOT NULL REFERENCES funcionario(pessoa_id)` | Pessoa com papel de funcionário. |
| `modelo_id` | Sim | `BIGINT NOT NULL REFERENCES modelo_aeronave(id)` | Modelo técnico da aeronave. |
| `funcao` | Sim | `VARCHAR(20) NOT NULL CHECK (funcao IN ('COMANDANTE','COPILOTO','COMISSARIO'))` | Função da habilitação ou da escala. |
| `numero_licenca` | Sim | `VARCHAR(40) NOT NULL` | Número da licença de tripulante. |
| `validade` | Sim | `DATE NOT NULL` | Data civil de validade, quando aplicável. |

Restrições compostas ou da tabela:

```sql
UNIQUE (funcionario_id, modelo_id, funcao)
```

## 15. usuario_sistema

Conta de um funcionário para operar os menus.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `funcionario_id` | Sim | `BIGINT NOT NULL UNIQUE REFERENCES funcionario(pessoa_id)` | Pessoa com papel de funcionário. |
| `login` | Sim | `VARCHAR(60) NOT NULL CHECK (login = lower(login) AND btrim(login) <> '')` | Nome de acesso normalizado em minúsculas. |
| `senha_hash` | Sim | `TEXT NOT NULL` | Hash de senha; jamais senha em texto puro. |
| `ativo` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Inativação cadastral; não apagar registros operacionais referenciados. |
| `criado_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de criação do registro. |

Restrições compostas ou da tabela:

```sql
UNIQUE (login)
```

## 16. perfil_acesso

Perfil cujas permissões são implementadas nos serviços Java.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `nome` | Sim | `VARCHAR(40) NOT NULL UNIQUE` | Nome descritivo; não é identificador único de pessoa. |
| `descricao` | Sim | `TEXT NOT NULL` | Descrição do cadastro ou atividade. |

## 17. usuario_perfil

Associação de conta a perfil; permite vários perfis.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `usuario_id` | Sim | `BIGINT NOT NULL REFERENCES usuario_sistema(id)` | Operador responsável ou autor da ação. |
| `perfil_id` | Sim | `BIGINT NOT NULL REFERENCES perfil_acesso(id)` | Perfil de acesso associado. |

Restrições compostas ou da tabela:

```sql
PRIMARY KEY (usuario_id, perfil_id)
```

## 18. aeronave

Aeronave física identificada por matrícula.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `modelo_id` | Sim | `BIGINT NOT NULL REFERENCES modelo_aeronave(id)` | Modelo técnico da aeronave. |
| `companhia_id` | Sim | `BIGINT NOT NULL REFERENCES companhia_aerea(id)` | Companhia associada ao voo, aeronave ou vínculo de trabalho. |
| `matricula` | Sim | `VARCHAR(15) NOT NULL UNIQUE` | Identificação da aeronave ou do empregado conforme a tabela. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'ATIVA' CHECK (situacao IN ('ATIVA','INATIVA','MANUTENCAO'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `fabricacao_ano` | Sim | `SMALLINT NOT NULL CHECK (fabricacao_ano BETWEEN 1900 AND 2200)` | Ano de fabricação da aeronave. |

## 19. assento_aeronave

Assento na configuração física de uma aeronave.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `aeronave_id` | Sim | `BIGINT NOT NULL REFERENCES aeronave(id)` | Aeronave física associada. |
| `codigo` | Sim | `VARCHAR(6) NOT NULL` | Código dentro do contexto do cadastro. |
| `fila` | Sim | `SMALLINT NOT NULL CHECK (fila > 0)` | Número físico da fila, maior que zero. |
| `coluna` | Sim | `VARCHAR(2) NOT NULL` | Letra/identificador físico da coluna. |
| `classe` | Sim | `VARCHAR(20) NOT NULL CHECK (classe IN ('ECONOMICA','EXECUTIVA','PRIMEIRA'))` | Cabine comercial: econômica, executiva ou primeira. |
| `saida_emergencia` | Sim | `BOOLEAN NOT NULL DEFAULT FALSE` | Assento com requisitos adicionais de elegibilidade. |
| `ativo` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Inativação cadastral; não apagar registros operacionais referenciados. |

Restrições compostas ou da tabela:

```sql
UNIQUE (aeronave_id, codigo),
UNIQUE (aeronave_id, fila, coluna),
UNIQUE (id, aeronave_id)
```

## 20. rota

Trecho direcional entre dois aeroportos.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `origem_id` | Sim | `BIGINT NOT NULL REFERENCES aeroporto(id)` | Aeroporto de partida da rota. |
| `destino_id` | Sim | `BIGINT NOT NULL REFERENCES aeroporto(id)` | Aeroporto de chegada da rota. |
| `distancia_km` | Sim | `NUMERIC(10,2) NOT NULL CHECK (distancia_km > 0)` | Distância didática em quilômetros. |

Restrições compostas ou da tabela:

```sql
CHECK (origem_id <> destino_id),
UNIQUE (origem_id, destino_id)
```

## 21. voo

Ocorrência de um trecho com companhia, aeronave e instantes completos.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `companhia_id` | Sim | `BIGINT NOT NULL REFERENCES companhia_aerea(id)` | Companhia associada ao voo, aeronave ou vínculo de trabalho. |
| `rota_id` | Sim | `BIGINT NOT NULL REFERENCES rota(id)` | Trecho direcional cadastrado. |
| `aeronave_id` | Sim | `BIGINT NOT NULL REFERENCES aeronave(id)` | Aeronave física associada. |
| `numero` | Sim | `VARCHAR(8) NOT NULL` | Número/código do documento, licença ou operação conforme a entidade. |
| `partida_prevista` | Sim | `TIMESTAMPTZ NOT NULL` | Instante original/planejado de partida, com fuso. |
| `chegada_prevista` | Sim | `TIMESTAMPTZ NOT NULL` | Instante original/planejado de chegada, com fuso. |
| `partida_estimada` | Não | `TIMESTAMPTZ` | Estimativa atual de partida, antes de acontecer. |
| `chegada_estimada` | Não | `TIMESTAMPTZ` | Estimativa atual de chegada. |
| `partida_real` | Não | `TIMESTAMPTZ` | Instante em que a partida aconteceu. |
| `chegada_real` | Não | `TIMESTAMPTZ` | Instante em que a chegada aconteceu. |
| `checkin_abre` | Sim | `TIMESTAMPTZ NOT NULL` | Início da janela de check-in. |
| `checkin_fecha` | Sim | `TIMESTAMPTZ NOT NULL` | Fim exclusivo da janela de check-in. |
| `embarque_abre` | Sim | `TIMESTAMPTZ NOT NULL` | Início da janela de embarque. |
| `embarque_fecha` | Sim | `TIMESTAMPTZ NOT NULL` | Fim exclusivo da janela de embarque. |
| `situacao` | Sim | `VARCHAR(25) NOT NULL DEFAULT 'PROGRAMADO'         CHECK (situacao IN ('PROGRAMADO','CHECKIN_ABERTO','EMBARQUE','EM_VOO','CONCLUIDO','CANCELADO'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `motivo_atraso` | Não | `TEXT` | Justificativa operacional da estimativa alterada. |
| `criado_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de criação do registro. |

Restrições compostas ou da tabela:

```sql
UNIQUE (companhia_id, numero, partida_prevista),
UNIQUE (id, aeronave_id),
CHECK (chegada_prevista > partida_prevista),
CHECK (chegada_estimada IS NULL OR partida_estimada IS NULL OR chegada_estimada > partida_estimada),
CHECK (chegada_real IS NULL OR (partida_real IS NOT NULL AND chegada_real > partida_real)),
CHECK (checkin_abre < checkin_fecha AND checkin_fecha <= embarque_fecha),
CHECK (embarque_abre < embarque_fecha AND embarque_fecha <= partida_prevista)
```

## 22. inventario_assento_voo

Mapa de assentos comercializáveis de uma ocorrência de voo.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `voo_id` | Sim | `BIGINT NOT NULL` | Ocorrência concreta do voo. |
| `aeronave_id` | Sim | `BIGINT NOT NULL` | Aeronave física associada. |
| `assento_id` | Sim | `BIGINT NOT NULL` | Assento do mapa físico da aeronave. |
| `codigo` | Sim | `VARCHAR(6) NOT NULL` | Código dentro do contexto do cadastro. Snapshot do assento ao preparar o voo; validado pelo Java. |
| `classe` | Sim | `VARCHAR(20) NOT NULL CHECK (classe IN ('ECONOMICA','EXECUTIVA','PRIMEIRA'))` | Cabine comercial: econômica, executiva ou primeira. Snapshot do assento ao preparar o voo; validado pelo Java. |
| `bloqueado` | Sim | `BOOLEAN NOT NULL DEFAULT FALSE` | Assento indisponível para venda naquele voo. |

Restrições compostas ou da tabela:

```sql
FOREIGN KEY (voo_id, aeronave_id) REFERENCES voo(id, aeronave_id),
FOREIGN KEY (assento_id, aeronave_id) REFERENCES assento_aeronave(id, aeronave_id),
UNIQUE (voo_id, codigo),
UNIQUE (voo_id, assento_id),
UNIQUE (id, voo_id)
```

## 23. manutencao_aeronave

Ordem de manutenção; planejamento de tempo fica na agenda.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `aeronave_id` | Sim | `BIGINT NOT NULL REFERENCES aeronave(id)` | Aeronave física associada. |
| `responsavel_id` | Sim | `BIGINT NOT NULL REFERENCES funcionario(pessoa_id)` | Funcionário responsável pela manutenção ou tarefa. |
| `tipo` | Sim | `VARCHAR(20) NOT NULL CHECK (tipo IN ('PREVENTIVA','CORRETIVA','INSPECAO'))` | Categoria controlada pelos valores do CHECK. |
| `descricao` | Sim | `TEXT NOT NULL` | Descrição do cadastro ou atividade. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'AGENDADA'         CHECK (situacao IN ('AGENDADA','EM_EXECUCAO','CONCLUIDA','CANCELADA'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `inicio_real` | Não | `TIMESTAMPTZ` | Início efetivo da manutenção. |
| `fim_real` | Não | `TIMESTAMPTZ` | Conclusão efetiva da manutenção. |
| `custo` | Sim | `NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (custo >= 0)` | Custo simulado da atividade, em BRL. |

Restrições compostas ou da tabela:

```sql
UNIQUE (id, aeronave_id),
CHECK (fim_real IS NULL OR (inicio_real IS NOT NULL AND fim_real >= inicio_real))
```

## 24. agenda_aeronave

Intervalo ativo de voo ou manutenção, sem conflito no mesmo avião.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `aeronave_id` | Sim | `BIGINT NOT NULL REFERENCES aeronave(id)` | Aeronave física associada. |
| `voo_id` | Não | `BIGINT UNIQUE` | Ocorrência concreta do voo. |
| `manutencao_id` | Não | `BIGINT UNIQUE` | Ordem de manutenção; alternativa ao voo na agenda. |
| `inicio` | Sim | `TIMESTAMPTZ NOT NULL` | Início de intervalo ou de execução. |
| `fim` | Sim | `TIMESTAMPTZ NOT NULL` | Fim exclusivo da agenda ou conclusão da atividade. |
| `ativa` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Indica que esta alocação ainda participa do controle de conflitos. |

Restrições compostas ou da tabela:

```sql
FOREIGN KEY (voo_id, aeronave_id) REFERENCES voo(id, aeronave_id),
FOREIGN KEY (manutencao_id, aeronave_id) REFERENCES manutencao_aeronave(id, aeronave_id),
CHECK ((voo_id IS NOT NULL) <> (manutencao_id IS NOT NULL)),
CHECK (fim > inicio),
EXCLUDE USING gist (aeronave_id WITH =, tstzrange(inicio, fim, '[)') WITH &&) WHERE (ativa)
```

## 25. alocacao_recurso

Uso de um recurso por um voo ou interdição sem voo.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `recurso_id` | Sim | `BIGINT NOT NULL REFERENCES recurso_aeroportuario(id)` | Recurso cuja agenda será ocupada. |
| `voo_id` | Não | `BIGINT REFERENCES voo(id)` | Ocorrência concreta do voo. |
| `finalidade` | Sim | `VARCHAR(25) NOT NULL CHECK (finalidade IN ('PARTIDA','CHEGADA','APOIO','INTERDICAO'))` | Uso do recurso em partida, chegada, apoio ou interdição. |
| `inicio` | Sim | `TIMESTAMPTZ NOT NULL` | Início de intervalo ou de execução. |
| `fim` | Sim | `TIMESTAMPTZ NOT NULL` | Fim exclusivo da agenda ou conclusão da atividade. |
| `ativa` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Indica que esta alocação ainda participa do controle de conflitos. |
| `observacao` | Não | `TEXT` | Comentário adicional da operação. |

Restrições compostas ou da tabela:

```sql
UNIQUE (id, voo_id),
CHECK (fim > inicio),
CHECK ((finalidade = 'INTERDICAO') = (voo_id IS NULL)),
EXCLUDE USING gist (recurso_id WITH =, tstzrange(inicio, fim, '[)') WITH &&) WHERE (ativa)
```

## 26. escala_funcionario

Intervalo de trabalho alocado, eventualmente vinculado a um voo.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `funcionario_id` | Sim | `BIGINT NOT NULL REFERENCES funcionario(pessoa_id)` | Pessoa com papel de funcionário. |
| `aeroporto_id` | Sim | `BIGINT NOT NULL REFERENCES aeroporto(id)` | Aeroporto ao qual se refere o registro. |
| `voo_id` | Não | `BIGINT REFERENCES voo(id)` | Ocorrência concreta do voo. |
| `funcao` | Sim | `VARCHAR(30) NOT NULL CHECK (funcao IN ('COMANDANTE','COPILOTO','COMISSARIO','ATENDIMENTO','SOLO','MANUTENCAO','SEGURANCA'))` | Função da habilitação ou da escala. |
| `inicio` | Sim | `TIMESTAMPTZ NOT NULL` | Início de intervalo ou de execução. |
| `fim` | Sim | `TIMESTAMPTZ NOT NULL` | Fim exclusivo da agenda ou conclusão da atividade. |
| `ativa` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Indica que esta alocação ainda participa do controle de conflitos. |

Restrições compostas ou da tabela:

```sql
CHECK (fim > inicio),
CHECK (funcao NOT IN ('COMANDANTE','COPILOTO','COMISSARIO') OR voo_id IS NOT NULL),
EXCLUDE USING gist (funcionario_id WITH =, tstzrange(inicio, fim, '[)') WITH &&) WHERE (ativa)
```

## 27. tarifa_voo

Oferta comercial por voo; condições copiadas no momento da reserva.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `voo_id` | Sim | `BIGINT NOT NULL REFERENCES voo(id)` | Ocorrência concreta do voo. |
| `codigo` | Sim | `VARCHAR(20) NOT NULL` | Código dentro do contexto do cadastro. |
| `classe` | Sim | `VARCHAR(20) NOT NULL CHECK (classe IN ('ECONOMICA','EXECUTIVA','PRIMEIRA'))` | Cabine comercial: econômica, executiva ou primeira. |
| `valor_base` | Sim | `NUMERIC(14,2) NOT NULL CHECK (valor_base >= 0)` | Preço básico em BRL; no item, snapshot do preço vendido. |
| `taxa_embarque` | Sim | `NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (taxa_embarque >= 0)` | Taxa em BRL; no item, snapshot da taxa vendida. |
| `franquia_bagagem_kg` | Sim | `NUMERIC(6,2) NOT NULL DEFAULT 0 CHECK (franquia_bagagem_kg >= 0)` | Franquia total de peso por passageiro/trecho, em kg. |
| `limite_pecas` | Sim | `SMALLINT NOT NULL DEFAULT 0 CHECK (limite_pecas >= 0)` | Quantidade máxima de peças incluídas na tarifa. |
| `permite_cancelar` | Sim | `BOOLEAN NOT NULL` | Condição comercial; não impede restituição em cancelamento do próprio voo. |
| `multa_cancelamento` | Sim | `NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (multa_cancelamento >= 0)` | Multa contratada em BRL; aplicação define quando cobrar. |
| `ativa` | Sim | `BOOLEAN NOT NULL DEFAULT TRUE` | Indica que esta alocação ainda participa do controle de conflitos. |

Restrições compostas ou da tabela:

```sql
UNIQUE (voo_id, codigo),
UNIQUE (id, voo_id)
```

## 28. reserva

Cabeçalho do pedido, comprador, localizador e prazo para confirmação.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `localizador` | Sim | `VARCHAR(12) NOT NULL UNIQUE` | Código público único da reserva, normalizado pelo Java. |
| `comprador_id` | Sim | `BIGINT NOT NULL REFERENCES pessoa(id)` | Pessoa que realizou a compra, podendo não viajar. |
| `usuario_id` | Sim | `BIGINT NOT NULL REFERENCES usuario_sistema(id)` | Operador responsável ou autor da ação. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'         CHECK (situacao IN ('PENDENTE','CONFIRMADA','PARCIAL_CANCELADA','CANCELADA','EXPIRADA','FINALIZADA'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `criada_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de criação da reserva ou ocupação. |
| `expira_em` | Sim | `TIMESTAMPTZ NOT NULL` | Prazo de confirmação da reserva; rotina Java realiza a expiração. |

Restrições compostas ou da tabela:

```sql
CHECK (expira_em > criada_em)
```

## 29. item_reserva

Um passageiro em um trecho, com preço e condições contratadas.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `reserva_id` | Sim | `BIGINT NOT NULL REFERENCES reserva(id)` | Cabeçalho comercial ao qual pertence a operação. |
| `passageiro_id` | Sim | `BIGINT NOT NULL REFERENCES passageiro(pessoa_id)` | Pessoa com papel de passageiro que viaja neste item. |
| `voo_id` | Sim | `BIGINT NOT NULL REFERENCES voo(id)` | Ocorrência concreta do voo. |
| `tarifa_id` | Sim | `BIGINT NOT NULL` | Tarifa de referência do mesmo voo. |
| `ordem_trecho` | Sim | `SMALLINT NOT NULL CHECK (ordem_trecho > 0)` | Sequência da viagem de um passageiro dentro da reserva. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'         CHECK (situacao IN ('PENDENTE','CONFIRMADO','CANCELADO','EXPIRADO','UTILIZADO','NAO_COMPARECEU'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `valor_base` | Sim | `NUMERIC(14,2) NOT NULL CHECK (valor_base >= 0)` | Preço básico em BRL; no item, snapshot do preço vendido. |
| `taxa_embarque` | Sim | `NUMERIC(14,2) NOT NULL CHECK (taxa_embarque >= 0)` | Taxa em BRL; no item, snapshot da taxa vendida. |
| `desconto` | Sim | `NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (desconto >= 0)` | Desconto em BRL aplicado ao item. |
| `franquia_bagagem_kg` | Sim | `NUMERIC(6,2) NOT NULL CHECK (franquia_bagagem_kg >= 0)` | Franquia total de peso por passageiro/trecho, em kg. |
| `limite_pecas` | Sim | `SMALLINT NOT NULL CHECK (limite_pecas >= 0)` | Quantidade máxima de peças incluídas na tarifa. |
| `permite_cancelar` | Sim | `BOOLEAN NOT NULL` | Condição comercial; não impede restituição em cancelamento do próprio voo. |
| `multa_cancelamento` | Sim | `NUMERIC(14,2) NOT NULL CHECK (multa_cancelamento >= 0)` | Multa contratada em BRL; aplicação define quando cobrar. |
| `cancelado_em` | Não | `TIMESTAMPTZ` | Instante do cancelamento; NULL enquanto não cancelado. |
| `motivo_cancelamento` | Não | `TEXT` | Justificativa do cancelamento. |

Restrições compostas ou da tabela:

```sql
FOREIGN KEY (tarifa_id, voo_id) REFERENCES tarifa_voo(id, voo_id),
UNIQUE (reserva_id, passageiro_id, ordem_trecho),
UNIQUE (id, voo_id),
CHECK (desconto <= valor_base + taxa_embarque),
CHECK ((situacao = 'CANCELADO') = (cancelado_em IS NOT NULL))
```

## 30. ocupacao_assento

Histórico de atribuição de um assento a um item de reserva.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `item_id` | Sim | `BIGINT NOT NULL` | Trecho de um passageiro dentro da reserva. |
| `voo_id` | Sim | `BIGINT NOT NULL` | Ocorrência concreta do voo. |
| `inventario_id` | Sim | `BIGINT NOT NULL` | Assento comercializável de um voo. |
| `criada_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de criação da reserva ou ocupação. |
| `liberada_em` | Não | `TIMESTAMPTZ` | Fim da ocupação; NULL significa assento ainda ocupado. |

Restrições compostas ou da tabela:

```sql
FOREIGN KEY (item_id, voo_id) REFERENCES item_reserva(id, voo_id),
FOREIGN KEY (inventario_id, voo_id) REFERENCES inventario_assento_voo(id, voo_id),
UNIQUE (id, item_id, voo_id),
CHECK (liberada_em IS NULL OR liberada_em >= criada_em)
```

## 31. bilhete

Documento de passagem emitido para um item confirmado.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `item_id` | Sim | `BIGINT NOT NULL UNIQUE REFERENCES item_reserva(id)` | Trecho de um passageiro dentro da reserva. |
| `numero` | Sim | `VARCHAR(20) NOT NULL UNIQUE` | Número/código do documento, licença ou operação conforme a entidade. |
| `emitido_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante da emissão do bilhete. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'EMITIDO' CHECK (situacao IN ('EMITIDO','UTILIZADO','CANCELADO'))` | Estado atual dentro dos valores admitidos no CHECK. |

Restrições compostas ou da tabela:

```sql
UNIQUE (id, item_id)
```

## 32. pagamento

Tentativa ou recebimento simulado vinculado à reserva.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `reserva_id` | Sim | `BIGINT NOT NULL REFERENCES reserva(id)` | Cabeçalho comercial ao qual pertence a operação. |
| `chave_idempotencia` | Sim | `VARCHAR(80) NOT NULL UNIQUE` | Identifica uma operação para evitar repetição acidental. |
| `forma` | Sim | `VARCHAR(20) NOT NULL CHECK (forma IN ('PIX','CARTAO','DINHEIRO'))` | Meio de pagamento simulado. |
| `valor` | Sim | `NUMERIC(14,2) NOT NULL CHECK (valor > 0)` | Valor financeiro desta operação, em BRL. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' CHECK (situacao IN ('PENDENTE','APROVADO','RECUSADO','CANCELADO'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `criado_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de criação do registro. |
| `processado_em` | Não | `TIMESTAMPTZ` | Instante de processamento financeiro, se concluído. |
| `referencia_externa` | Não | `VARCHAR(120)` | Identificador de simulação/integração; não contém dados de cartão. |

Restrições compostas ou da tabela:

```sql
UNIQUE (id, reserva_id)
```

## 33. reembolso

Devolução solicitada contra um pagamento da mesma reserva.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `pagamento_id` | Sim | `BIGINT NOT NULL` | Pagamento original contra o qual se solicita devolução. |
| `reserva_id` | Sim | `BIGINT NOT NULL REFERENCES reserva(id)` | Cabeçalho comercial ao qual pertence a operação. |
| `valor` | Sim | `NUMERIC(14,2) NOT NULL CHECK (valor > 0)` | Valor financeiro desta operação, em BRL. |
| `motivo` | Sim | `TEXT NOT NULL` | Justificativa da alteração ou devolução. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'SOLICITADO' CHECK (situacao IN ('SOLICITADO','PROCESSADO','RECUSADO'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `chave_idempotencia` | Sim | `VARCHAR(80) NOT NULL UNIQUE` | Identifica uma operação para evitar repetição acidental. |
| `solicitado_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de solicitação da devolução. |
| `processado_em` | Não | `TIMESTAMPTZ` | Instante de processamento financeiro, se concluído. |

Restrições compostas ou da tabela:

```sql
FOREIGN KEY (pagamento_id, reserva_id) REFERENCES pagamento(id, reserva_id)
```

## 34. check_in

Registro do cartão de embarque para um bilhete e uma ocupação.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `item_id` | Sim | `BIGINT NOT NULL UNIQUE` | Trecho de um passageiro dentro da reserva. |
| `voo_id` | Sim | `BIGINT NOT NULL` | Ocorrência concreta do voo. |
| `bilhete_id` | Sim | `BIGINT NOT NULL UNIQUE` | Bilhete emitido para o mesmo item. |
| `ocupacao_id` | Sim | `BIGINT NOT NULL` | Atribuição de assento do mesmo item e voo. |
| `usuario_id` | Sim | `BIGINT NOT NULL REFERENCES usuario_sistema(id)` | Operador responsável ou autor da ação. |
| `codigo_cartao` | Sim | `VARCHAR(40) NOT NULL UNIQUE` | Código público único do cartão de embarque. |
| `realizado_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante efetivo da operação. |
| `cancelado_em` | Não | `TIMESTAMPTZ` | Instante do cancelamento; NULL enquanto não cancelado. |

Restrições compostas ou da tabela:

```sql
FOREIGN KEY (item_id, voo_id) REFERENCES item_reserva(id, voo_id),
FOREIGN KEY (bilhete_id, item_id) REFERENCES bilhete(id, item_id),
FOREIGN KEY (ocupacao_id, item_id, voo_id) REFERENCES ocupacao_assento(id, item_id, voo_id),
UNIQUE (id, voo_id),
CHECK (cancelado_em IS NULL OR cancelado_em >= realizado_em)
```

## 35. bagagem

Uma peça despachada no trecho do check-in.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `checkin_id` | Sim | `BIGINT NOT NULL REFERENCES check_in(id)` | Check-in ao qual pertence bagagem ou embarque. |
| `etiqueta` | Sim | `VARCHAR(30) NOT NULL UNIQUE` | Identificador único da peça despachada no trecho. |
| `peso_kg` | Sim | `NUMERIC(6,2) NOT NULL CHECK (peso_kg > 0)` | Peso medido da peça em quilogramas. |
| `categoria` | Sim | `VARCHAR(20) NOT NULL CHECK (categoria IN ('DESPACHADA','ESPECIAL'))` | Categoria da bagagem despachada. |
| `taxa_excesso` | Sim | `NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (taxa_excesso >= 0)` | Taxa extra de bagagem em BRL, separada da tarifa de passagem. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'RECEBIDA'         CHECK (situacao IN ('RECEBIDA','INSPECIONADA','CARREGADA','DESCARREGADA','ENTREGUE','EXTRAVIADA','RETIRADA'))` | Estado atual dentro dos valores admitidos no CHECK. |

## 36. evento_bagagem

Evento cronológico de rastreio de uma peça.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `bagagem_id` | Sim | `BIGINT NOT NULL REFERENCES bagagem(id)` | Peça rastreada pelo evento. |
| `aeroporto_id` | Sim | `BIGINT NOT NULL REFERENCES aeroporto(id)` | Aeroporto ao qual se refere o registro. |
| `usuario_id` | Sim | `BIGINT NOT NULL REFERENCES usuario_sistema(id)` | Operador responsável ou autor da ação. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL CHECK (situacao IN ('RECEBIDA','INSPECIONADA','CARREGADA','DESCARREGADA','ENTREGUE','EXTRAVIADA','RETIRADA'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `ocorrido_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de ocorrência do evento. |
| `observacao` | Não | `TEXT` | Comentário adicional da operação. |

## 37. embarque

Confirmação de entrada do passageiro em seu voo.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `checkin_id` | Sim | `BIGINT NOT NULL UNIQUE` | Check-in ao qual pertence bagagem ou embarque. |
| `voo_id` | Sim | `BIGINT NOT NULL` | Ocorrência concreta do voo. |
| `alocacao_id` | Sim | `BIGINT NOT NULL` | Alocação do portão, validada pelo serviço no embarque. |
| `usuario_id` | Sim | `BIGINT NOT NULL REFERENCES usuario_sistema(id)` | Operador responsável ou autor da ação. |
| `realizado_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante efetivo da operação. |

Restrições compostas ou da tabela:

```sql
FOREIGN KEY (checkin_id, voo_id) REFERENCES check_in(id, voo_id),
FOREIGN KEY (alocacao_id, voo_id) REFERENCES alocacao_recurso(id, voo_id)
```

## 38. historico_voo

Snapshot automático de cada criação ou alteração de voo.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `voo_id` | Sim | `BIGINT NOT NULL REFERENCES voo(id)` | Ocorrência concreta do voo. |
| `usuario_id` | Não | `BIGINT REFERENCES usuario_sistema(id)` | Operador responsável ou autor da ação. |
| `alterado_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de registro da alteração de voo. |
| `situacao_anterior` | Não | `VARCHAR(25)` | Estado do voo antes da alteração; NULL na criação. |
| `situacao_nova` | Sim | `VARCHAR(25) NOT NULL` | Estado do voo depois da alteração. |
| `dados_anteriores` | Não | `JSONB` | Snapshot JSONB anterior; NULL na criação. |
| `dados_novos` | Sim | `JSONB NOT NULL` | Snapshot JSONB depois da operação. |
| `motivo` | Não | `TEXT` | Justificativa da alteração ou devolução. |

## 39. ocorrencia_operacional

Incidente registrado em aeroporto, opcionalmente ligado a voo.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `aeroporto_id` | Sim | `BIGINT NOT NULL REFERENCES aeroporto(id)` | Aeroporto ao qual se refere o registro. |
| `voo_id` | Não | `BIGINT REFERENCES voo(id)` | Ocorrência concreta do voo. |
| `usuario_id` | Sim | `BIGINT NOT NULL REFERENCES usuario_sistema(id)` | Operador responsável ou autor da ação. |
| `tipo` | Sim | `VARCHAR(25) NOT NULL CHECK (tipo IN ('ATRASO','METEOROLOGIA','SEGURANCA','TECNICA','OUTRA'))` | Categoria controlada pelos valores do CHECK. |
| `descricao` | Sim | `TEXT NOT NULL` | Descrição do cadastro ou atividade. |
| `gravidade` | Sim | `VARCHAR(10) NOT NULL CHECK (gravidade IN ('BAIXA','MEDIA','ALTA'))` | Classificação didática da ocorrência. |
| `aberta_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de abertura da ocorrência. |
| `encerrada_em` | Não | `TIMESTAMPTZ` | Instante do encerramento, se resolvida. |

Restrições compostas ou da tabela:

```sql
CHECK (encerrada_em IS NULL OR encerrada_em >= aberta_em)
```

## 40. servico_solo

Atividade de apoio para um voo no aeroporto.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `voo_id` | Sim | `BIGINT NOT NULL REFERENCES voo(id)` | Ocorrência concreta do voo. |
| `aeroporto_id` | Sim | `BIGINT NOT NULL REFERENCES aeroporto(id)` | Aeroporto ao qual se refere o registro. |
| `responsavel_id` | Sim | `BIGINT NOT NULL REFERENCES funcionario(pessoa_id)` | Funcionário responsável pela manutenção ou tarefa. |
| `tipo` | Sim | `VARCHAR(25) NOT NULL CHECK (tipo IN ('ABASTECIMENTO','LIMPEZA','CATERING','BAGAGEM','REBOQUE'))` | Categoria controlada pelos valores do CHECK. |
| `situacao` | Sim | `VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' CHECK (situacao IN ('PENDENTE','EM_EXECUCAO','CONCLUIDO','CANCELADO'))` | Estado atual dentro dos valores admitidos no CHECK. |
| `inicio` | Não | `TIMESTAMPTZ` | Início de intervalo ou de execução. |
| `fim` | Não | `TIMESTAMPTZ` | Fim exclusivo da agenda ou conclusão da atividade. |
| `quantidade` | Não | `NUMERIC(12,2) CHECK (quantidade >= 0)` | Quantidade física associada ao serviço, se aplicável. |
| `unidade` | Não | `VARCHAR(20)` | Unidade da quantidade, por exemplo litro ou peça. |
| `custo` | Sim | `NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (custo >= 0)` | Custo simulado da atividade, em BRL. |

Restrições compostas ou da tabela:

```sql
CHECK (fim IS NULL OR (inicio IS NOT NULL AND fim >= inicio))
```

## 41. evento_auditoria

Registro de ação realizada no sistema; preenchido pelo Java.

| Campo | Obrigatório | Definição física | Significado |
|---|---|---|---|
| `id` | Sim | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Identificador interno gerado pelo banco. |
| `usuario_id` | Não | `BIGINT REFERENCES usuario_sistema(id)` | Operador responsável ou autor da ação. |
| `ocorrido_em` | Sim | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` | Instante de ocorrência do evento. |
| `acao` | Sim | `VARCHAR(50) NOT NULL` | Operação realizada, por exemplo CANCELAR_RESERVA. |
| `entidade` | Sim | `VARCHAR(60) NOT NULL` | Nome lógico da entidade afetada. |
| `registro_id` | Não | `BIGINT` | Identificador auditado; referência lógica, sem FK para permitir diferentes entidades. |
| `detalhes` | Sim | `JSONB NOT NULL DEFAULT '{}'::jsonb` | Dados complementares mínimos da ação, evitando documentos e segredos. |

## Índices adicionais, histórico e visões

O SQL também contém índices de busca; índices UNIQUE parciais para contato principal, passageiro/voo ativo e ocupação ativa; três restrições EXCLUDE para conflitos de agenda; a função/trigger registrar_historico_voo; e as visões vw_painel_voos, vw_disponibilidade_assentos e vw_total_reserva.

A lista física abaixo conserva as condições dos índices, que não aparecem nas definições locais de colunas:

```sql
CREATE UNIQUE INDEX uq_contato_principal ON contato_pessoa(pessoa_id, tipo) WHERE principal;
CREATE UNIQUE INDEX uq_passageiro_voo_ativo ON item_reserva(passageiro_id, voo_id)
    WHERE situacao IN ('PENDENTE','CONFIRMADO','UTILIZADO','NAO_COMPARECEU');
CREATE UNIQUE INDEX uq_assento_ocupado ON ocupacao_assento(inventario_id) WHERE liberada_em IS NULL;
CREATE UNIQUE INDEX uq_item_assento ON ocupacao_assento(item_id) WHERE liberada_em IS NULL;
CREATE INDEX ix_cidade_pais ON cidade(pais_id);
CREATE INDEX ix_aeroporto_cidade ON aeroporto(cidade_id);
CREATE INDEX ix_documento_pessoa ON documento_pessoa(pessoa_id);
CREATE INDEX ix_funcionario_cargo ON funcionario(cargo_id);
CREATE INDEX ix_funcionario_base ON funcionario(aeroporto_base_id);
CREATE INDEX ix_aeronave_modelo ON aeronave(modelo_id);
CREATE INDEX ix_voo_rota_data ON voo(rota_id, partida_prevista);
CREATE INDEX ix_voo_situacao_data ON voo(situacao, partida_prevista);
CREATE INDEX ix_voo_aeronave ON voo(aeronave_id);
CREATE INDEX ix_alocacao_voo ON alocacao_recurso(voo_id);
CREATE INDEX ix_escala_voo ON escala_funcionario(voo_id);
CREATE INDEX ix_reserva_comprador ON reserva(comprador_id);
CREATE INDEX ix_reserva_prazo ON reserva(expira_em) WHERE situacao = 'PENDENTE';
CREATE INDEX ix_item_reserva ON item_reserva(reserva_id);
CREATE INDEX ix_item_voo ON item_reserva(voo_id, situacao);
CREATE INDEX ix_pagamento_reserva ON pagamento(reserva_id, situacao);
CREATE INDEX ix_reembolso_pagamento ON reembolso(pagamento_id, situacao);
CREATE INDEX ix_bagagem_checkin ON bagagem(checkin_id);
CREATE INDEX ix_evento_bagagem ON evento_bagagem(bagagem_id, ocorrido_em);
CREATE INDEX ix_historico_voo ON historico_voo(voo_id, alterado_em);
CREATE INDEX ix_ocorrencia_voo ON ocorrencia_operacional(voo_id);
CREATE INDEX ix_servico_voo ON servico_solo(voo_id);
CREATE INDEX ix_auditoria_entidade ON evento_auditoria(entidade, registro_id, ocorrido_em);
```

Consulte o script para as definições completas de função, trigger e visões. As visões não armazenam novos registros.
