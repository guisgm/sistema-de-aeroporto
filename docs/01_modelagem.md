# Modelagem do banco de dados do sistema de aeroporto

## 1. Objetivo e limites do domínio

Simular, pelo terminal de um programa Java, o atendimento a passageiros e o planejamento e acompanhamento de voos. O projeto permite cadastros, reservas, pagamento simulado, emissão de bilhetes, check-in, despacho de bagagens, embarque, cancelamento, consultas e relatórios. A operação também inclui infraestrutura, funcionários, tripulação, manutenção e serviços em solo.

O modelo atende a múltiplos aeroportos e companhias. Uma execução pode administrar um aeroporto principal e usar os demais como origem e destino. Um voo representa um trecho em uma data concreta; um número de voo pode se repetir em outras datas. Conexões são vários itens da mesma reserva.

Não se pretende reproduzir todas as operações de um aeroporto real. Carga comercial, alfândega, imigração, concessões de lojas, estacionamento de automóveis, folha de pagamento, controle de tráfego aéreo e contabilidade fiscal ficam fora desta versão. Podem ser módulos futuros. Pistas, abastecimento e licenças são simulações didáticas, sem protocolos operacionais reais.

## 2. Módulos e entidades

| Módulo | Entidades | Finalidade |
|---|---|---|
| Localização | pais, cidade, aeroporto, terminal | Origem, destino e infraestrutura |
| Infraestrutura | recurso_aeroportuario, alocacao_recurso | Portões, pistas, posições, esteiras, balcões e bloqueios por horário |
| Pessoas | pessoa, documento_pessoa, contato_pessoa, passageiro | Identificação, documentos, contatos e assistência |
| Equipe | cargo, funcionario, habilitacao_tripulante, escala_funcionario | Funcionários, licenças e jornada alocada |
| Acesso | usuario_sistema, perfil_acesso, usuario_perfil | Operadores do programa e seus perfis |
| Frota | companhia_aerea, modelo_aeronave, aeronave, assento_aeronave | Companhias, modelos e configuração física de assentos |
| Planejamento | rota, voo, agenda_aeronave, inventario_assento_voo | Trechos, horários e capacidade vendável |
| Comercial | tarifa_voo, reserva, item_reserva, ocupacao_assento, bilhete | Tarifas, grupos de passageiros, conexões, assentos e passagem emitida |
| Financeiro | pagamento, reembolso | Recebimentos e devoluções simulados |
| Atendimento | check_in, bagagem, evento_bagagem, embarque | Cartão de embarque e rastreio das malas |
| Operação | manutencao_aeronave, servico_solo, ocorrencia_operacional | Manutenção, tarefas e ocorrências |
| Rastreabilidade | historico_voo, evento_auditoria | Histórico e ações dos operadores |

## 3. Principais escolhas de modelagem

### Pessoa, passageiro e funcionário

`pessoa` concentra nome, nascimento e nacionalidade. `passageiro` e `funcionario` usam `pessoa_id` como chave primária e estrangeira: cada papel pertence a exatamente uma pessoa. A mesma pessoa pode ser passageiro e funcionário. Documentos e contatos são relações 1:N; CPF não é obrigatório para estrangeiros e não é usado como chave primária.

No Java, essa estrutura permite uma classe abstrata `Pessoa` e subclasses `Passageiro` e `Funcionario` como demonstração de herança. O banco também admite os dois papéis simultâneos; para esse caso, a aplicação pode manter duas instâncias de papel com a mesma identidade persistida. O modelo relacional não exige que todas as classes usem herança.

### Rota, voo e companhia

`rota` é origem → destino. A rota inversa é outra linha. `voo` é uma ocorrência com companhia, número, aeronave e horários completos. Não há tabela de dias da semana porque esta versão cadastra ocorrências; se houver repetição semanal, o Java gera várias ocorrências ou uma futura tabela de programação recorrente.

O modelo permite aeronaves de outra companhia em um voo, para simular cessão/arrendamento. Se o trabalho não incluir essa situação, o Java deve exigir que as companhias coincidam. Não há compartilhamento de códigos nesta versão.

### Infraestrutura e agenda

Portão, pista, posição, esteira e balcão são tipos de `recurso_aeroportuario`. Os campos opcionais de comprimento e envergadura servem apenas aos tipos aplicáveis. Esse cadastro comum permite um único mecanismo de reserva de horários e de interdição.

`alocacao_recurso` representa ocupação do recurso por voo ou bloqueio por interdição. A situação cadastral indica disponibilidade geral; a ocupação em um horário é calculada na agenda. Não existe um campo permanente “ocupado” no recurso.

`agenda_aeronave` reúne voo e manutenção. Cada linha tem exatamente uma dessas origens. Isso evita agendar manutenção e voo em tabelas independentes sem detectar o conflito entre eles. O intervalo de voo deve incluir a preparação e o tempo posterior necessários; as margens são definidas pelo projeto, por exemplo 45 minutos antes e 30 minutos depois.

As agendas usam intervalos `[início, fim)`: uma alocação pode começar no instante em que outra termina. Restrições de exclusão PostgreSQL impedem sobreposição entre registros ativos do mesmo avião, recurso ou funcionário. A criação e a atualização dessas agendas continuam sendo responsabilidade da aplicação.

### Assentos e capacidade

`assento_aeronave` é o mapa físico. `inventario_assento_voo` copia os assentos utilizáveis para a ocorrência do voo. A capacidade vendável é o número de assentos não bloqueados desse inventário; não há outro contador de capacidade que possa divergir.

Cada item ativo precisa ocupar um assento durante a reserva, mesmo antes do check-in. Se o cliente não escolher, o sistema atribui um provisoriamente. `ocupacao_assento` conserva os registros anteriores com `liberada_em`; dois índices únicos parciais impedem ocupações ativas duplicadas para o mesmo assento ou item. Chaves compostas garantem que item, inventário e ocupação sejam do mesmo voo, e que o assento físico pertença à aeronave do voo.

Expiração não é automática por passagem do tempo. O Java executa uma rotina ao iniciar o programa e antes de operações comerciais: expira reservas pendentes vencidas e libera suas ocupações na mesma transação. Em execução prolongada, pode repetir periodicamente. Enquanto não houver essa rotina, a reserva vencida ainda bloqueia o assento.

### Reservas, conexões e bilhetes

Uma reserva tem comprador e pode conter vários passageiros e vários trechos. Um item corresponde a um passageiro em um voo. O comprador pode não viajar. A ordem do trecho é única por passageiro dentro da reserva.

Cada item tem tarifa e cópia dos valores e condições vendidos. Mudanças futuras na tarifa não alteram contratos anteriores. Um item pode ter um bilhete; emissão só ocorre após confirmação. Remarcação cria novo item/bilhete e cancela o anterior, mantendo o histórico. Um item cancelado não é reativado. A compra posterior para o mesmo voo gera novo item.

Todos os valores monetários desta versão são em reais (BRL), com duas casas decimais. Tarifas não armazenam câmbio ou múltiplas moedas. Pagamentos são simulados; não se armazenam números de cartão, CVV ou credenciais financeiras.

### Bagagens e check-in

O cartão de embarque pertence ao check-in de um único item/bilhete/voo. O modelo prevê no máximo um registro de check-in por item; se for cancelado e depois refeito, o Java atualiza esse mesmo registro, registrando as ações na auditoria. Ao trocar assento, atualiza também a referência à ocupação atual.

Uma bagagem é uma peça despachada em um trecho, identificada por etiqueta única. Em conexão, o próximo trecho cria outro registro de despacho e etiqueta; não há rastreio único da peça entre trechos nesta versão. Bagagem de cabine não integra o despacho. `evento_bagagem` preserva as mudanças de situação, enquanto `bagagem.situacao` guarda o estado atual.

O valor das passagens é separado de taxas extras de bagagem. `vw_total_reserva` calcula apenas os itens de passagem. Para cobrar bagagem, somar as taxas das bagagens vigentes da reserva em consulta separada. O pagamento pode ser complementar e continua associado à reserva. A aplicação deve manter a política de devolução dessas taxas no cancelamento.

## 4. Cardinalidades

| Relação | Cardinalidade e regra |
|---|---|
| País → cidade → aeroporto → terminal | 1:N em cada etapa; cada filho tem um pai |
| Aeroporto → recurso | 1:N; terminal do recurso é opcional, mas obrigatório para portão |
| Pessoa → passageiro / funcionário | 1:0..1 para cada papel |
| Pessoa → documento / contato | 1:N; pessoa recém-cadastrada pode ainda não ter esses registros |
| Cargo → funcionário | 1:N |
| Funcionário → usuário | 1:0..1 |
| Usuário ↔ perfil | N:N por usuario_perfil |
| Funcionário ↔ modelo de aeronave | N:N por habilitacao_tripulante, também diferenciada por função |
| Modelo / companhia → aeronave | 1:N |
| Aeronave → assento físico | 1:N |
| Aeroporto → rota | 1:N nos papéis origem e destino |
| Rota / companhia / aeronave → voo | 1:N |
| Voo → inventário / tarifas / escalas / alocações | 1:N |
| Voo / manutenção → agenda aeronave | 1:0..1 no DDL; aplicação exige uma agenda ativa quando programados |
| Recurso → alocação | 1:N sem sobreposição ativa |
| Funcionário → escala | 1:N sem sobreposição ativa |
| Reserva → item | 1:N; aplicação exige pelo menos um item |
| Passageiro / voo / tarifa → item | 1:N |
| Item → ocupação | 1:N histórico, no máximo uma ocupação ativa |
| Inventário de assento → ocupação | 1:N histórico, no máximo uma ocupação ativa |
| Item → bilhete → check-in → embarque | 1:0..1 em cada etapa |
| Check-in → bagagem → evento | 1:N em cada etapa |
| Reserva → pagamento → reembolso | 1:N em cada etapa |
| Voo → histórico / serviço de solo / ocorrência | 1:N |

## 5. Regras de integridade: quem garante cada uma

“Banco” significa restrição ou trigger presente no SQL. “Java” significa regra que cabe ao serviço, dentro de transação. Esta tabela descreve o sistema final; as regras já implementadas na base estão identificadas em `05_base_java.md`. A validação do menu não substitui o serviço.

| Regra | Responsável |
|---|---|
| Identificadores, referências, unicidade de documentos, matrícula, localizador e bilhete | Banco |
| Origem diferente do destino; valores não negativos; chegada depois da partida | Banco |
| Terminal do recurso pertence ao mesmo aeroporto | Banco |
| Assento pertence à aeronave e ao voo corretos | Banco, por FKs compostas |
| Item, tarifa, check-in, bilhete, ocupação e embarque pertencem ao mesmo trecho | Banco, por FKs compostas |
| Passageiro não tem dois itens ativos no mesmo voo | Banco, índice parcial |
| Um assento não tem duas ocupações ativas; item não tem dois assentos ativos | Banco, índices parciais |
| Agendas ativas do mesmo avião, funcionário ou recurso não se sobrepõem | Banco, EXCLUDE |
| Toda criação e alteração de voo registra histórico | Banco, trigger |
| Voo programado e manutenção agendada têm agenda correspondente | Java |
| Todas as agendas acompanham reprogramações e atrasos | Java |
| Cadastro ativo, funcionário contratado, documento válido, nascimento não futuro e fuso válido | Java |
| Origem/destino internacional exige documentos adequados à simulação | Java |
| Portão e pista estão no aeroporto do trecho; tipo e dimensões são compatíveis | Java |
| Alocação de embarque é de portão, finalidade PARTIDA, ativa e válida no instante | Java |
| Tripulação tem habilitação válida para modelo e função; quantidade mínima definida no projeto | Java |
| Descanso entre jornadas, deslocamento entre aeroportos e continuidade da frota | Java |
| Assento não bloqueado, classe compatível com tarifa e código do inventário consistente | Java |
| Item confirmado tem bilhete emitido e reserva financeiramente coberta | Java |
| Valores vendidos são cópias da tarifa e não mudam depois da confirmação | Java |
| Soma dos pagamentos aprovados e reembolsos processados respeita os valores devidos | Java com bloqueio da reserva/pagamento |
| Pagamento recusado não confirma reserva; chaves idempotentes evitam repetição da mesma operação | Java + unicidade no banco |
| Cancelamento respeita tarifa; calcula multa e devolução; libera assento e invalida bilhete/check-in | Java |
| Prazo de reserva é cumprido; expiração libera assentos | Java, rotina explícita |
| Check-in só com item confirmado, bilhete emitido, ocupação ativa e janela válida | Java |
| Embarque só com check-in vigente, portão correto e janela válida | Java |
| Franquia, excesso de bagagem, exigências de assento em saída de emergência e assistência | Java |
| Alterar situação da mala também insere evento na mesma transação | Java |
| Cancelar voo cancela itens e libera todas as agendas; restituições são registradas | Java |
| Alterações comerciais e de acesso deixam auditoria | Java |

Os CHECKs com NULL permitem campos opcionais; regras entre várias tabelas são deliberadamente atribuídas ao serviço, para evitar CHECKs que consultem outras tabelas. Escritas diretas no banco podem deixar estados de negócio incompletos. O aplicativo deve centralizar operações nos serviços e usar um usuário de banco com permissões compatíveis.

## 6. Estados e transições

### Voo

`PROGRAMADO → CHECKIN_ABERTO → EMBARQUE → EM_VOO → CONCLUIDO`.

`CANCELADO` é destino permitido antes de `EM_VOO`; não há retorno automático a estado anterior. Após decolagem, incidentes entram nas ocorrências e o trecho termina em `CONCLUIDO`. Desvio para outro destino é uma extensão futura; não alterar a rota silenciosamente após o voo partir.

Atraso é uma condição derivada dos horários estimados/reais e pode coexistir com check-in ou embarque. Por isso, não é um estado exclusivo. A visão do painel indica atraso de partida; atraso de chegada pode ser calculado separadamente. Mudanças de horários geram histórico automaticamente.

### Reserva e item

Reserva: `PENDENTE → CONFIRMADA → FINALIZADA`. Reserva pendente pode virar `EXPIRADA` ou `CANCELADA`. Cancelamento de parte dos itens gera `PARCIAL_CANCELADA`; cancelamento de todos gera `CANCELADA`. O Java recalcula o estado do cabeçalho sob bloqueio, incluindo grupos com passageiros diferentes.

Item: `PENDENTE → CONFIRMADO → UTILIZADO` ou `NAO_COMPARECEU`. Item pendente pode virar `EXPIRADO`; pendente ou confirmado pode virar `CANCELADO`. `UTILIZADO` é marcado quando a passagem é efetivamente consumida no embarque, dentro da mesma transação que registra embarque e atualiza bilhete. Ausência é apurada no fechamento do embarque.

### Financeiro e atendimento

Pagamento: `PENDENTE → APROVADO | RECUSADO | CANCELADO`. Pagamento aprovado é preservado; devoluções são linhas em `reembolso`. Não se substitui o valor pago pelo saldo restante.

Reembolso: `SOLICITADO → PROCESSADO | RECUSADO`. Check-in cancelado é indicado por `cancelado_em`. Ocupação liberada tem `liberada_em`. Bagagem percorre recebimento, inspeção, carga, descarga e entrega; extravio ou retirada são caminhos excepcionais documentados.

O DDL limita os valores possíveis; a sequência de transições é validada pelo Java.

## 7. Operações atômicas e concorrência

Todas as operações compostas usam uma conexão JDBC com autoCommit desativado, commit no sucesso e rollback em erro. Uma operação que falha não pode salvar somente parte das mudanças.

Ordem padrão de bloqueios: voos por id crescente → reservas por id crescente → itens → inventários por id crescente → pagamentos. Todos os serviços seguem a mesma ordem para reduzir deadlocks. Quando um fluxo começa por localizar um item, lê seus identificadores primeiro e depois bloqueia nessa ordem, relendo o estado. Não executar chamadas externas enquanto mantém bloqueios.

1. **Programar voo:** validar rota/aeronave; inserir voo, agenda com margens, inventário e tarifas; opcionalmente escalas e recursos. Só disponibilizar venda quando o inventário e as condições comerciais estiverem completos.
2. **Reservar:** bloquear os voos em ordem; expirar pendências vencidas; validar janela de venda; criar cabeçalho e itens com valores vendidos; bloquear inventários selecionados; inserir ocupações; registrar auditoria; commit. Se faltar assento em qualquer trecho, rollback da reserva inteira.
3. **Confirmar pagamento:** bloquear voos envolvidos e reserva; reler prazo e itens; inserir ou localizar pagamento por chave idempotente; registrar aprovação simulada; conferir saldo; confirmar itens e cabeçalho; emitir bilhetes. Pagamentos parciais podem existir, mas a confirmação do grupo ocorre apenas com cobertura integral.
4. **Check-in:** bloquear voo/reserva/item/inventário; validar situação, documento e janela; conferir bilhete/ocupação; criar ou atualizar check-in, bagagens e eventos; registrar eventual pagamento complementar de excesso.
5. **Embarcar:** bloquear voo/reserva/item; validar check-in e alocação; conferir horário e bagagens; inserir embarque; atualizar item e bilhete; registrar auditoria.
6. **Cancelar/expirar:** bloquear voos/reserva/itens; validar política; registrar motivo e instante; liberar ocupações; cancelar bilhete e check-in; registrar retirada de bagagens; recalcular cabeçalho; criar solicitações de reembolso quando aplicável. Expiração aplica-se somente a pendentes e normalmente não tem bilhete/check-in.
7. **Processar reembolso:** bloquear reserva e pagamento; somar devoluções já processadas e solicitações ainda pendentes; não permitir comprometimento acima do pagamento aprovado; processar por chave idempotente; registrar auditoria. A contabilização de solicitações pendentes impede aprovar várias devoluções que, juntas, excedam o pagamento.
8. **Reprogramar voo:** bloquear voo e reservas afetadas; alterar horários e atualizar agenda de avião, escalas e recursos; resolver conflitos ou rollback; preservar horários anteriores no histórico; atualizar janelas comerciais e de atendimento.
9. **Trocar aeronave:** permitido apenas antes de qualquer item de reserva ter sido criado para o voo nesta versão. Bloquear voo; validar ausência de itens e de ocupações históricas; remover inventário antigo sem ocupações, atualizar aeronave/agenda e recriar inventário. Havendo vendas, inclusive já canceladas, recusar e exigir um fluxo futuro de realocação, sem remover histórico.
10. **Cancelar voo:** bloquear voo e todas as reservas afetadas; cancelar itens segundo política; liberar ocupações e agendas; invalidar cartões/bilhetes; tratar bagagens; solicitar reembolsos; registrar ocorrência e auditoria; commit.

As restrições únicas e de exclusão protegem contra concorrência mesmo se dois operadores escolherem o mesmo recurso. A aplicação traduz SQLSTATE de duplicidade/conflito em mensagem do terminal e pode repetir operações idempotentes após deadlock. Leitura seguida de escrita sem bloqueio não é suficiente para os saldos financeiros.

## 8. Normalização, tipos e histórico

Cadastros e relações estão organizados para evitar listas em colunas, documentos duplicados e atributos de entidades distintas na mesma tabela. N:N têm tabelas associativas. Valores de tarifa no item e mapa do inventário são snapshots intencionais, preservando condições históricas.

PKs usam BIGINT identity; o Java usa Long. Dinheiro usa NUMERIC e BigDecimal; datas civis usam DATE e LocalDate; instantes usam TIMESTAMPTZ e OffsetDateTime/Instant. Aeroportos guardam identificadores IANA de fuso, validados pelo Java com ZoneId. O programa converte horários para o fuso do aeroporto ao exibir e exige fuso na entrada de horários. Não armazenar apenas hora do dia para um voo.

JSONB é reservado a histórico e auditoria; passageiros, itens e assentos permanecem relacionais. Observações textuais não substituem relações estruturadas.

Exclusão física de dados operacionais não é o fluxo normal. FKs usam NO ACTION por padrão, evitando apagar cadastros referenciados. Cadastros são inativados, operações canceladas e históricos preservados. Dados de identificação devem ser minimizados nos logs; senha contém somente hash adequado, nunca texto puro. Perfis de acesso são verificados no Java e não são roles PostgreSQL.

## 9. Menus e consultas previstos

- Cadastros: passageiros, pessoas, documentos, contatos, funcionários, cargos, companhias, aeronaves, assentos e aeroportos.
- Planejamento: rotas, voos, tarifas, portões, pistas, escalas e manutenção.
- Atendimento: reserva individual/grupo/conexão, pagamento, bilhete, cancelamento, check-in, bagagem e embarque.
- Operação: abrir check-in/embarque, atualizar horários, registrar partida/chegada, serviços em solo, ocorrência e cancelamento de voo.
- Consultas: painel por aeroporto/data, assentos disponíveis, passageiros do voo, reserva por localizador, bagagem por etiqueta, escala, agenda e histórico.
- Relatórios: ocupação, atraso, vendas, devoluções, ausências e bagagens; exportação em TXT/CSV para demonstrar arquivos da disciplina.

## 10. Implementação por etapas

1. Cadastros de pessoas, aeroporto, companhia, frota e rotas.
2. Programação de voos, assentos e tarifas.
3. Reserva, confirmação, bilhete, cancelamento e expiração.
4. Check-in, bagagens e embarque.
5. Agendas de equipe/recursos, manutenção e serviços de solo.
6. Perfis, auditoria, relatórios, importação e exportação.

O modelo já contempla todas essas etapas. A ordem reduz o tamanho de cada incremento do programa, sem exigir recomeçar a modelagem.

## Referências técnicas consultadas

Os intervalos e as restrições de exclusão seguem a documentação oficial de [Range Types](https://www.postgresql.org/docs/current/rangetypes.html). Chaves, CHECKs e referências seguem [Constraints](https://www.postgresql.org/docs/current/ddl-constraints.html). As políticas aeroportuárias e comerciais são decisões didáticas desta modelagem, e não regras operacionais extraídas dessas páginas.
