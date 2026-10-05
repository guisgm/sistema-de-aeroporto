# AeroHub: aplicacao web operacional

## Escopo e arquitetura

AeroHub e uma simulacao funcional de operacoes aeroportuarias para a base GRU/SBGR.
A interface React conversa com uma API Express no mesmo servidor. O SQLite local
persiste os dados em `data/aerohub.sqlite`, com chaves estrangeiras, indices e
transacoes. A aplicacao Java/PostgreSQL original permanece independente: esta
versao web nao consulta nem modifica seu banco PostgreSQL.

Requisitos: Node.js 22.13 ou superior; ambiente verificado com Node.js 24.11.
O projeto usa a [API SQLite nativa do Node.js](https://nodejs.org/api/sqlite.html).
No Node.js 24.11, o aviso de recurso experimental do SQLite e esperado.

## Executar

No PowerShell, a partir da raiz do projeto:

```powershell
npm.cmd install
npm.cmd run dev
```

Abra `http://localhost:5173`. API e interface usam a mesma origem. Para executar
o pacote compilado:

```powershell
npm.cmd run build
npm.cmd start
```

Outra porta, caso 5173 esteja ocupada:

```powershell
$env:PORT = '5174'
npm.cmd start
```

O primeiro inicio cria 4 companhias, 20 aeronaves, 3 terminais, 12 portoes,
144 voos e 24 passageiros com reservas ficticias. Os voos cobrem os seis dias
anteriores, o dia da inicializacao e o seguinte. A base nao e recriada nem
atualizada automaticamente a cada abertura. Depois deste periodo, cadastre voos
com novas datas ou consulte a programacao historica no seletor de data.
Os status sao alterados pela equipe; nao ha integracao com rastreamento de voos.
Para avancar o horario dos voos originais de demonstracao, use `npm.cmd run demo:sync`.
O comando ajusta apenas os voos ficticios do dia inicial com versao 1; preserva
voos cadastrados ou editados por usuarios e registra a sincronizacao na auditoria.

## Acessos de demonstracao

Na tela de acesso, os botoes de simulacao autenticam cada perfil. Para login manual,
a senha dos tres usuarios ficticios e `AeroHub@2026!`.

| Perfil | E-mail | Permissoes de alteracao |
| --- | --- | --- |
| Administrador | admin@aerohub.local | Todos os cadastros, operacoes e check-in |
| Operador | operador@aerohub.local | Voos, aeronaves e portoes |
| Atendente | atendente@aerohub.local | Passageiros, reservas e check-in |

Todos consultam o painel e os recursos operacionais. O operador recebe documentos
mascarados e nao recebe contatos nem datas de nascimento de passageiros. A exportacao
de passageiros exige perfil de administrador ou atendente e omite documentos.

## Fluxos implementados

- Voos: cadastro, edicao, status, cancelamento com motivo e confirmacao, realocacao
  de portao e aeronave, filtros por data, operacao, status, companhia, terminal e destino.
- Passageiros: cadastro, edicao, CPF com verificacao dos digitos, passaporte com
  formato validado, dados obrigatorios, reservas vinculadas e busca por documento.
- Reservas: localizador, mapa de assentos com ocupacao real, exclusividade de assento,
  exclusividade de passageiro por voo e cancelamento com liberacao do assento.
- Check-in: janela de 48 horas antes da partida, verificacao do estado do voo,
  emissao de cartao de embarque e impressao pelo navegador, inclusive em PDF.
- Companhias: cadastro, edicao, frota, historico e taxas de atraso e cancelamento.
- Aeronaves: matricula, modelo, capacidade, manutencao, notas e indisponibilidade.
- Terminais e portoes: cadastro, edicao, bloqueio e programacao por data.
- Painel: totais calculados, partidas/chegadas, programacao, portoes alocados,
  alertas, distribuicao por horario e acesso aos detalhes.
- Busca global: voos, passageiros e companhias. Relatorios CSV de voos, contatos de
  passageiros, companhias e auditoria. Historico pesquisavel com os 300 eventos mais recentes.

## Regras de integridade

O servidor aplica todas as regras, independentemente da interface:

- Cada portao exige intervalo de 90 minutos entre movimentos. A nova previsao de
  um voo atrasado entra no calculo; horario previsto isoladamente nao determina conflitos.
- Uma aeronave so recebe outro voo depois da duracao do voo anterior e 60 minutos
  de preparacao. Aeronaves em manutencao e portoes bloqueados nao recebem voos ativos.
- A companhia da aeronave deve coincidir com a do voo. Um extremo da rota deve ser GRU.
- Bloquear um portao, indisponibilizar uma aeronave ou trocar sua companhia exige
  remover suas alocacoes ativas futuras. Reduzir capacidade nao pode eliminar assentos reservados.
- Voos encerrados e cancelados nao sao reabertos. Cancelar um voo cancela suas reservas
  na mesma transacao. Voos em manutencao nao aceitam reservas nem check-in.
- Reservas e check-in estao disponiveis apenas para partidas futuras. A reserva verifica
  capacidade, assento livre e ausencia de outra reserva ativa do mesmo passageiro no voo.
- Atualizacoes exigem a versao atual do registro para evitar sobrescritas entre usuarios.
- SQL utiliza parametros vinculados. CSV neutraliza formulas de planilha.

O mapa considera fileiras de seis assentos, adequado a simulacao; modelos comerciais
reais podem usar configuracoes diferentes. Os validadores de documentos verificam
formato/digitos, sem consulta a orgaos emissores.

## Seguranca local

Senhas armazenadas com scrypt e salt aleatorio. Sessoes de oito horas usam tokens
aleatorios em cookies HttpOnly/SameSite=Strict. Mutacoes exigem token CSRF, mesma origem
e autorizacao no servidor. O acesso tem limite de tentativas. Formularios usam schema Zod.
As sessoes ficam na memoria e expiram quando o servidor reinicia.

Os usuarios e senhas publicos de demonstracao sao exclusivos deste ambiente de
simulacao, que escuta em `127.0.0.1` por padrao. Antes de qualquer uso com dados reais,
substitua o provisionamento de demonstracao por contas individuais e remova os botoes
de acesso rapido. Uma implantacao externa tambem exige HTTPS, `COOKIE_SECURE=true`,
gestao de sessoes compartilhada, backups e integracoes operacionais apropriadas.
Nao e um sistema homologado para controlar um aeroporto real.

## Codigo e verificacao

| Caminho | Responsabilidade |
| --- | --- |
| web/App.jsx | Sessao, navegacao e coordenacao dos fluxos |
| web/Dashboard.jsx | Painel operacional |
| web/Views.jsx | Tabelas, cadastros e relatorios |
| web/forms.jsx | Formularios, reservas, detalhes e cartao |
| web/components.jsx | Controles e componentes compartilhados |
| web/model.js | Formatacao e indicadores derivados |
| server/database.mjs | Schema relacional, transacoes e dados de simulacao |
| server/operations.mjs | Autorizacao, regras de negocio e auditoria |
| server/validation.mjs | Schemas e validacao de documentos |
| server/app.mjs | Rotas, sessoes e protecoes da API |
| tests/ | Testes de regras, API e fluxos de navegador |

```powershell
npm.cmd test
npm.cmd run test:e2e
npm.cmd run format:check
```

Os testes de navegador usam Google Chrome instalado, compilam a interface e executam
um servidor isolado na porta 5199, com banco separado em `artifacts/`. Capturas ficam
em `artifacts/` e falhas geram traces em `test-results/`. Nenhum teste escreve na base
operacional `data/aerohub.sqlite`. `scripts/visual-check.mjs` verifica a instancia em
5173 nas larguras 1440, 1280, 768, 390 e 320 e registra eventuais transbordamentos.

Fontes DM Sans/Manrope e icones Lucide sao distribuidos localmente. A fotografia
ilustrativa de aeroporto vem do [Unsplash](https://images.unsplash.com/photo-1580285198593-af9f402c676a)
e nao representa necessariamente Guarulhos. A interface compilada nao depende de CDNs.
