-- SISTEMA DE AEROPORTO: script único de criação para PostgreSQL/DBeaver.
-- Abra conectado a sistema_aeroporto, selecione TODO o arquivo e use Ctrl+Enter.
-- Não executar em banco com tabelas do projeto já existentes.
-- Não apaga dados; toda a criação é atômica.
DO $instalar_aeroporto$
DECLARE
    etapa TEXT;
BEGIN
    IF current_database() <> 'sistema_aeroporto' THEN
        RAISE EXCEPTION 'Conecte ao banco sistema_aeroporto. Banco atual: %', current_database();
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_catalog.pg_class c
        JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'aeroporto' AND c.relkind IN ('r','p','v','m','f','S')
    ) THEN
        RAISE EXCEPTION 'O schema aeroporto já contém objetos. Instalação interrompida para preservar os dados.';
    END IF;

    etapa := '1: CREATE EXTENSION IF NOT EXISTS';
    EXECUTE $ddl_1$
CREATE EXTENSION IF NOT EXISTS btree_gist;
$ddl_1$;

    etapa := '2: CREATE SCHEMA IF NOT EXISTS';
    EXECUTE $ddl_2$
CREATE SCHEMA IF NOT EXISTS aeroporto;
$ddl_2$;

    etapa := '3: SET LOCAL search_path TO aeroporto,';
    EXECUTE $ddl_3$
SET LOCAL search_path TO aeroporto, public;
$ddl_3$;

    etapa := '4: CREATE TABLE pais ( id';
    EXECUTE $ddl_4$
CREATE TABLE pais (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo CHAR(2) NOT NULL UNIQUE CHECK (codigo ~ '^[A-Z]{2}$'),
    nome VARCHAR(100) NOT NULL CHECK (btrim(nome) <> '')
);
$ddl_4$;

    etapa := '5: CREATE TABLE cidade ( id';
    EXECUTE $ddl_5$
CREATE TABLE cidade (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pais_id BIGINT NOT NULL REFERENCES pais(id),
    nome VARCHAR(120) NOT NULL,
    regiao VARCHAR(100) NOT NULL DEFAULT '',
    UNIQUE (pais_id, nome, regiao)
);
$ddl_5$;

    etapa := '6: CREATE TABLE aeroporto ( id';
    EXECUTE $ddl_6$
CREATE TABLE aeroporto (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cidade_id BIGINT NOT NULL REFERENCES cidade(id),
    codigo_iata CHAR(3) UNIQUE CHECK (codigo_iata ~ '^[A-Z]{3}$'),
    codigo_icao CHAR(4) NOT NULL UNIQUE CHECK (codigo_icao ~ '^[A-Z]{4}$'),
    nome VARCHAR(160) NOT NULL,
    fuso_horario VARCHAR(80) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
$ddl_6$;

    etapa := '7: CREATE TABLE terminal ( id';
    EXECUTE $ddl_7$
CREATE TABLE terminal (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aeroporto_id BIGINT NOT NULL REFERENCES aeroporto(id),
    codigo VARCHAR(15) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (aeroporto_id, codigo), UNIQUE (id, aeroporto_id)
);
$ddl_7$;

    etapa := '8: CREATE TABLE recurso_aeroportuario ( id';
    EXECUTE $ddl_8$
CREATE TABLE recurso_aeroportuario (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aeroporto_id BIGINT NOT NULL REFERENCES aeroporto(id),
    terminal_id BIGINT,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('PORTAO','PISTA','POSICAO','ESTEIRA','BALCAO')),
    codigo VARCHAR(20) NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL'
        CHECK (situacao IN ('DISPONIVEL','INTERDITADO','MANUTENCAO')),
    comprimento_m NUMERIC(8,2) CHECK (comprimento_m > 0),
    envergadura_max_m NUMERIC(6,2) CHECK (envergadura_max_m > 0),
    FOREIGN KEY (terminal_id, aeroporto_id) REFERENCES terminal(id, aeroporto_id),
    UNIQUE (aeroporto_id, tipo, codigo),
    CHECK (tipo <> 'PISTA' OR comprimento_m IS NOT NULL),
    CHECK (tipo <> 'PORTAO' OR terminal_id IS NOT NULL)
);
$ddl_8$;

    etapa := '9: CREATE TABLE companhia_aerea ( id';
    EXECUTE $ddl_9$
CREATE TABLE companhia_aerea (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pais_id BIGINT NOT NULL REFERENCES pais(id),
    codigo_icao CHAR(3) NOT NULL UNIQUE CHECK (codigo_icao ~ '^[A-Z0-9]{3}$'),
    codigo_iata CHAR(2) UNIQUE CHECK (codigo_iata ~ '^[A-Z0-9]{2}$'),
    nome VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
$ddl_9$;

    etapa := '10: CREATE TABLE pessoa ( id';
    EXECUTE $ddl_10$
CREATE TABLE pessoa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nacionalidade_id BIGINT REFERENCES pais(id),
    nome VARCHAR(160) NOT NULL CHECK (btrim(nome) <> ''),
    nascimento DATE NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
$ddl_10$;

    etapa := '11: CREATE TABLE documento_pessoa ( id';
    EXECUTE $ddl_11$
CREATE TABLE documento_pessoa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pessoa_id BIGINT NOT NULL REFERENCES pessoa(id),
    pais_emissor_id BIGINT NOT NULL REFERENCES pais(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CPF','RG','PASSAPORTE','OUTRO')),
    numero VARCHAR(40) NOT NULL CHECK (btrim(numero) <> ''),
    validade DATE,
    UNIQUE (pais_emissor_id, tipo, numero)
);
$ddl_11$;

    etapa := '12: CREATE TABLE contato_pessoa ( id';
    EXECUTE $ddl_12$
CREATE TABLE contato_pessoa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pessoa_id BIGINT NOT NULL REFERENCES pessoa(id),
    tipo VARCHAR(15) NOT NULL CHECK (tipo IN ('EMAIL','TELEFONE')),
    valor VARCHAR(160) NOT NULL CHECK (btrim(valor) <> ''),
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (pessoa_id, tipo, valor)
);
$ddl_12$;

    etapa := '13: CREATE UNIQUE INDEX uq_contato_principal ON';
    EXECUTE $ddl_13$
CREATE UNIQUE INDEX uq_contato_principal ON contato_pessoa(pessoa_id, tipo) WHERE principal;
$ddl_13$;

    etapa := '14: CREATE TABLE passageiro ( pessoa_id';
    EXECUTE $ddl_14$
CREATE TABLE passageiro (
    pessoa_id BIGINT PRIMARY KEY REFERENCES pessoa(id),
    codigo_cliente VARCHAR(30) NOT NULL UNIQUE,
    observacoes_assistencia TEXT
);
$ddl_14$;

    etapa := '15: CREATE TABLE cargo ( id';
    EXECUTE $ddl_15$
CREATE TABLE cargo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(80) NOT NULL UNIQUE,
    area VARCHAR(25) NOT NULL CHECK (area IN ('ADMINISTRACAO','ATENDIMENTO','TRIPULACAO','MANUTENCAO','OPERACAO','SEGURANCA'))
);
$ddl_15$;

    etapa := '16: CREATE TABLE funcionario ( pessoa_id';
    EXECUTE $ddl_16$
CREATE TABLE funcionario (
    pessoa_id BIGINT PRIMARY KEY REFERENCES pessoa(id),
    cargo_id BIGINT NOT NULL REFERENCES cargo(id),
    aeroporto_base_id BIGINT NOT NULL REFERENCES aeroporto(id),
    companhia_id BIGINT REFERENCES companhia_aerea(id),
    matricula VARCHAR(30) NOT NULL UNIQUE,
    admissao DATE NOT NULL,
    desligamento DATE,
    CHECK (desligamento IS NULL OR desligamento >= admissao)
);
$ddl_16$;

    etapa := '17: CREATE TABLE modelo_aeronave ( id';
    EXECUTE $ddl_17$
CREATE TABLE modelo_aeronave (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fabricante VARCHAR(100) NOT NULL,
    nome VARCHAR(80) NOT NULL,
    envergadura_m NUMERIC(6,2) NOT NULL CHECK (envergadura_m > 0),
    comprimento_pista_min_m NUMERIC(8,2) NOT NULL CHECK (comprimento_pista_min_m > 0),
    UNIQUE (fabricante, nome)
);
$ddl_17$;

    etapa := '18: CREATE TABLE habilitacao_tripulante ( id';
    EXECUTE $ddl_18$
CREATE TABLE habilitacao_tripulante (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionario(pessoa_id),
    modelo_id BIGINT NOT NULL REFERENCES modelo_aeronave(id),
    funcao VARCHAR(20) NOT NULL CHECK (funcao IN ('COMANDANTE','COPILOTO','COMISSARIO')),
    numero_licenca VARCHAR(40) NOT NULL,
    validade DATE NOT NULL,
    UNIQUE (funcionario_id, modelo_id, funcao)
);
$ddl_18$;

    etapa := '19: CREATE TABLE usuario_sistema ( id';
    EXECUTE $ddl_19$
CREATE TABLE usuario_sistema (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    funcionario_id BIGINT NOT NULL UNIQUE REFERENCES funcionario(pessoa_id),
    login VARCHAR(60) NOT NULL CHECK (login = lower(login) AND btrim(login) <> ''),
    senha_hash TEXT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (login)
);
$ddl_19$;

    etapa := '20: CREATE TABLE perfil_acesso ( id';
    EXECUTE $ddl_20$
CREATE TABLE perfil_acesso (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(40) NOT NULL UNIQUE,
    descricao TEXT NOT NULL
);
$ddl_20$;

    etapa := '21: CREATE TABLE usuario_perfil ( usuario_id';
    EXECUTE $ddl_21$
CREATE TABLE usuario_perfil (
    usuario_id BIGINT NOT NULL REFERENCES usuario_sistema(id),
    perfil_id BIGINT NOT NULL REFERENCES perfil_acesso(id),
    PRIMARY KEY (usuario_id, perfil_id)
);
$ddl_21$;

    etapa := '22: CREATE TABLE aeronave ( id';
    EXECUTE $ddl_22$
CREATE TABLE aeronave (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    modelo_id BIGINT NOT NULL REFERENCES modelo_aeronave(id),
    companhia_id BIGINT NOT NULL REFERENCES companhia_aerea(id),
    matricula VARCHAR(15) NOT NULL UNIQUE,
    situacao VARCHAR(20) NOT NULL DEFAULT 'ATIVA' CHECK (situacao IN ('ATIVA','INATIVA','MANUTENCAO')),
    fabricacao_ano SMALLINT NOT NULL CHECK (fabricacao_ano BETWEEN 1900 AND 2200)
);
$ddl_22$;

    etapa := '23: CREATE TABLE assento_aeronave ( id';
    EXECUTE $ddl_23$
CREATE TABLE assento_aeronave (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aeronave_id BIGINT NOT NULL REFERENCES aeronave(id),
    codigo VARCHAR(6) NOT NULL,
    fila SMALLINT NOT NULL CHECK (fila > 0),
    coluna VARCHAR(2) NOT NULL,
    classe VARCHAR(20) NOT NULL CHECK (classe IN ('ECONOMICA','EXECUTIVA','PRIMEIRA')),
    saida_emergencia BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (aeronave_id, codigo), UNIQUE (aeronave_id, fila, coluna), UNIQUE (id, aeronave_id)
);
$ddl_23$;

    etapa := '24: CREATE TABLE rota ( id';
    EXECUTE $ddl_24$
CREATE TABLE rota (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    origem_id BIGINT NOT NULL REFERENCES aeroporto(id),
    destino_id BIGINT NOT NULL REFERENCES aeroporto(id),
    distancia_km NUMERIC(10,2) NOT NULL CHECK (distancia_km > 0),
    CHECK (origem_id <> destino_id), UNIQUE (origem_id, destino_id)
);
$ddl_24$;

    etapa := '25: CREATE TABLE voo ( id';
    EXECUTE $ddl_25$
CREATE TABLE voo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    companhia_id BIGINT NOT NULL REFERENCES companhia_aerea(id),
    rota_id BIGINT NOT NULL REFERENCES rota(id),
    aeronave_id BIGINT NOT NULL REFERENCES aeronave(id),
    numero VARCHAR(8) NOT NULL,
    partida_prevista TIMESTAMPTZ NOT NULL,
    chegada_prevista TIMESTAMPTZ NOT NULL,
    partida_estimada TIMESTAMPTZ,
    chegada_estimada TIMESTAMPTZ,
    partida_real TIMESTAMPTZ,
    chegada_real TIMESTAMPTZ,
    checkin_abre TIMESTAMPTZ NOT NULL,
    checkin_fecha TIMESTAMPTZ NOT NULL,
    embarque_abre TIMESTAMPTZ NOT NULL,
    embarque_fecha TIMESTAMPTZ NOT NULL,
    situacao VARCHAR(25) NOT NULL DEFAULT 'PROGRAMADO'
        CHECK (situacao IN ('PROGRAMADO','CHECKIN_ABERTO','EMBARQUE','EM_VOO','CONCLUIDO','CANCELADO')),
    motivo_atraso TEXT,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (companhia_id, numero, partida_prevista), UNIQUE (id, aeronave_id),
    CHECK (chegada_prevista > partida_prevista),
    CHECK (chegada_estimada IS NULL OR partida_estimada IS NULL OR chegada_estimada > partida_estimada),
    CHECK (chegada_real IS NULL OR (partida_real IS NOT NULL AND chegada_real > partida_real)),
    CHECK (checkin_abre < checkin_fecha AND checkin_fecha <= embarque_fecha),
    CHECK (embarque_abre < embarque_fecha AND embarque_fecha <= partida_prevista)
);
$ddl_25$;

    etapa := '26: CREATE TABLE inventario_assento_voo ( id';
    EXECUTE $ddl_26$
CREATE TABLE inventario_assento_voo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voo_id BIGINT NOT NULL,
    aeronave_id BIGINT NOT NULL,
    assento_id BIGINT NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    classe VARCHAR(20) NOT NULL CHECK (classe IN ('ECONOMICA','EXECUTIVA','PRIMEIRA')),
    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (voo_id, aeronave_id) REFERENCES voo(id, aeronave_id),
    FOREIGN KEY (assento_id, aeronave_id) REFERENCES assento_aeronave(id, aeronave_id),
    UNIQUE (voo_id, codigo), UNIQUE (voo_id, assento_id), UNIQUE (id, voo_id)
);
$ddl_26$;

    etapa := '27: CREATE TABLE manutencao_aeronave ( id';
    EXECUTE $ddl_27$
CREATE TABLE manutencao_aeronave (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aeronave_id BIGINT NOT NULL REFERENCES aeronave(id),
    responsavel_id BIGINT NOT NULL REFERENCES funcionario(pessoa_id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('PREVENTIVA','CORRETIVA','INSPECAO')),
    descricao TEXT NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'AGENDADA'
        CHECK (situacao IN ('AGENDADA','EM_EXECUCAO','CONCLUIDA','CANCELADA')),
    inicio_real TIMESTAMPTZ,
    fim_real TIMESTAMPTZ,
    custo NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (custo >= 0),
    UNIQUE (id, aeronave_id),
    CHECK (fim_real IS NULL OR (inicio_real IS NOT NULL AND fim_real >= inicio_real))
);
$ddl_27$;

    etapa := '28: CREATE TABLE agenda_aeronave ( id';
    EXECUTE $ddl_28$
CREATE TABLE agenda_aeronave (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aeronave_id BIGINT NOT NULL REFERENCES aeronave(id),
    voo_id BIGINT UNIQUE,
    manutencao_id BIGINT UNIQUE,
    inicio TIMESTAMPTZ NOT NULL,
    fim TIMESTAMPTZ NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (voo_id, aeronave_id) REFERENCES voo(id, aeronave_id),
    FOREIGN KEY (manutencao_id, aeronave_id) REFERENCES manutencao_aeronave(id, aeronave_id),
    CHECK ((voo_id IS NOT NULL) <> (manutencao_id IS NOT NULL)), CHECK (fim > inicio),
    EXCLUDE USING gist (aeronave_id WITH =, tstzrange(inicio, fim, '[)') WITH &&) WHERE (ativa)
);
$ddl_28$;

    etapa := '29: CREATE TABLE alocacao_recurso ( id';
    EXECUTE $ddl_29$
CREATE TABLE alocacao_recurso (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recurso_id BIGINT NOT NULL REFERENCES recurso_aeroportuario(id),
    voo_id BIGINT REFERENCES voo(id),
    finalidade VARCHAR(25) NOT NULL CHECK (finalidade IN ('PARTIDA','CHEGADA','APOIO','INTERDICAO')),
    inicio TIMESTAMPTZ NOT NULL,
    fim TIMESTAMPTZ NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    observacao TEXT,
    UNIQUE (id, voo_id),
    CHECK (fim > inicio), CHECK ((finalidade = 'INTERDICAO') = (voo_id IS NULL)),
    EXCLUDE USING gist (recurso_id WITH =, tstzrange(inicio, fim, '[)') WITH &&) WHERE (ativa)
);
$ddl_29$;

    etapa := '30: CREATE TABLE escala_funcionario ( id';
    EXECUTE $ddl_30$
CREATE TABLE escala_funcionario (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionario(pessoa_id),
    aeroporto_id BIGINT NOT NULL REFERENCES aeroporto(id),
    voo_id BIGINT REFERENCES voo(id),
    funcao VARCHAR(30) NOT NULL CHECK (funcao IN ('COMANDANTE','COPILOTO','COMISSARIO','ATENDIMENTO','SOLO','MANUTENCAO','SEGURANCA')),
    inicio TIMESTAMPTZ NOT NULL,
    fim TIMESTAMPTZ NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (fim > inicio),
    CHECK (funcao NOT IN ('COMANDANTE','COPILOTO','COMISSARIO') OR voo_id IS NOT NULL),
    EXCLUDE USING gist (funcionario_id WITH =, tstzrange(inicio, fim, '[)') WITH &&) WHERE (ativa)
);
$ddl_30$;

    etapa := '31: CREATE TABLE tarifa_voo ( id';
    EXECUTE $ddl_31$
CREATE TABLE tarifa_voo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voo_id BIGINT NOT NULL REFERENCES voo(id),
    codigo VARCHAR(20) NOT NULL,
    classe VARCHAR(20) NOT NULL CHECK (classe IN ('ECONOMICA','EXECUTIVA','PRIMEIRA')),
    valor_base NUMERIC(14,2) NOT NULL CHECK (valor_base >= 0),
    taxa_embarque NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (taxa_embarque >= 0),
    franquia_bagagem_kg NUMERIC(6,2) NOT NULL DEFAULT 0 CHECK (franquia_bagagem_kg >= 0),
    limite_pecas SMALLINT NOT NULL DEFAULT 0 CHECK (limite_pecas >= 0),
    permite_cancelar BOOLEAN NOT NULL,
    multa_cancelamento NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (multa_cancelamento >= 0),
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (voo_id, codigo), UNIQUE (id, voo_id)
);
$ddl_31$;

    etapa := '32: CREATE TABLE reserva ( id';
    EXECUTE $ddl_32$
CREATE TABLE reserva (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    localizador VARCHAR(12) NOT NULL UNIQUE,
    comprador_id BIGINT NOT NULL REFERENCES pessoa(id),
    usuario_id BIGINT NOT NULL REFERENCES usuario_sistema(id),
    situacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CHECK (situacao IN ('PENDENTE','CONFIRMADA','PARCIAL_CANCELADA','CANCELADA','EXPIRADA','FINALIZADA')),
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em TIMESTAMPTZ NOT NULL,
    CHECK (expira_em > criada_em)
);
$ddl_32$;

    etapa := '33: CREATE TABLE item_reserva ( id';
    EXECUTE $ddl_33$
CREATE TABLE item_reserva (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id BIGINT NOT NULL REFERENCES reserva(id),
    passageiro_id BIGINT NOT NULL REFERENCES passageiro(pessoa_id),
    voo_id BIGINT NOT NULL REFERENCES voo(id),
    tarifa_id BIGINT NOT NULL,
    ordem_trecho SMALLINT NOT NULL CHECK (ordem_trecho > 0),
    situacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CHECK (situacao IN ('PENDENTE','CONFIRMADO','CANCELADO','EXPIRADO','UTILIZADO','NAO_COMPARECEU')),
    valor_base NUMERIC(14,2) NOT NULL CHECK (valor_base >= 0),
    taxa_embarque NUMERIC(14,2) NOT NULL CHECK (taxa_embarque >= 0),
    desconto NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (desconto >= 0),
    franquia_bagagem_kg NUMERIC(6,2) NOT NULL CHECK (franquia_bagagem_kg >= 0),
    limite_pecas SMALLINT NOT NULL CHECK (limite_pecas >= 0),
    permite_cancelar BOOLEAN NOT NULL,
    multa_cancelamento NUMERIC(14,2) NOT NULL CHECK (multa_cancelamento >= 0),
    cancelado_em TIMESTAMPTZ,
    motivo_cancelamento TEXT,
    FOREIGN KEY (tarifa_id, voo_id) REFERENCES tarifa_voo(id, voo_id),
    UNIQUE (reserva_id, passageiro_id, ordem_trecho), UNIQUE (id, voo_id),
    CHECK (desconto <= valor_base + taxa_embarque),
    CHECK ((situacao = 'CANCELADO') = (cancelado_em IS NOT NULL))
);
$ddl_33$;

    etapa := '34: CREATE UNIQUE INDEX uq_passageiro_voo_ativo ON';
    EXECUTE $ddl_34$
CREATE UNIQUE INDEX uq_passageiro_voo_ativo ON item_reserva(passageiro_id, voo_id)
    WHERE situacao IN ('PENDENTE','CONFIRMADO','UTILIZADO','NAO_COMPARECEU');
$ddl_34$;

    etapa := '35: CREATE TABLE ocupacao_assento ( id';
    EXECUTE $ddl_35$
CREATE TABLE ocupacao_assento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id BIGINT NOT NULL,
    voo_id BIGINT NOT NULL,
    inventario_id BIGINT NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    liberada_em TIMESTAMPTZ,
    FOREIGN KEY (item_id, voo_id) REFERENCES item_reserva(id, voo_id),
    FOREIGN KEY (inventario_id, voo_id) REFERENCES inventario_assento_voo(id, voo_id),
    UNIQUE (id, item_id, voo_id),
    CHECK (liberada_em IS NULL OR liberada_em >= criada_em)
);
$ddl_35$;

    etapa := '36: CREATE UNIQUE INDEX uq_assento_ocupado ON';
    EXECUTE $ddl_36$
CREATE UNIQUE INDEX uq_assento_ocupado ON ocupacao_assento(inventario_id) WHERE liberada_em IS NULL;
$ddl_36$;

    etapa := '37: CREATE UNIQUE INDEX uq_item_assento ON';
    EXECUTE $ddl_37$
CREATE UNIQUE INDEX uq_item_assento ON ocupacao_assento(item_id) WHERE liberada_em IS NULL;
$ddl_37$;

    etapa := '38: CREATE TABLE bilhete ( id';
    EXECUTE $ddl_38$
CREATE TABLE bilhete (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id BIGINT NOT NULL UNIQUE REFERENCES item_reserva(id),
    numero VARCHAR(20) NOT NULL UNIQUE,
    emitido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    situacao VARCHAR(20) NOT NULL DEFAULT 'EMITIDO' CHECK (situacao IN ('EMITIDO','UTILIZADO','CANCELADO')),
    UNIQUE (id, item_id)
);
$ddl_38$;

    etapa := '39: CREATE TABLE pagamento ( id';
    EXECUTE $ddl_39$
CREATE TABLE pagamento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id BIGINT NOT NULL REFERENCES reserva(id),
    chave_idempotencia VARCHAR(80) NOT NULL UNIQUE,
    forma VARCHAR(20) NOT NULL CHECK (forma IN ('PIX','CARTAO','DINHEIRO')),
    valor NUMERIC(14,2) NOT NULL CHECK (valor > 0),
    situacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' CHECK (situacao IN ('PENDENTE','APROVADO','RECUSADO','CANCELADO')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processado_em TIMESTAMPTZ,
    referencia_externa VARCHAR(120),
    UNIQUE (id, reserva_id)
);
$ddl_39$;

    etapa := '40: CREATE TABLE reembolso ( id';
    EXECUTE $ddl_40$
CREATE TABLE reembolso (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pagamento_id BIGINT NOT NULL,
    reserva_id BIGINT NOT NULL REFERENCES reserva(id),
    valor NUMERIC(14,2) NOT NULL CHECK (valor > 0),
    motivo TEXT NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'SOLICITADO' CHECK (situacao IN ('SOLICITADO','PROCESSADO','RECUSADO')),
    chave_idempotencia VARCHAR(80) NOT NULL UNIQUE,
    solicitado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processado_em TIMESTAMPTZ,
    FOREIGN KEY (pagamento_id, reserva_id) REFERENCES pagamento(id, reserva_id)
);
$ddl_40$;

    etapa := '41: CREATE TABLE check_in ( id';
    EXECUTE $ddl_41$
CREATE TABLE check_in (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id BIGINT NOT NULL UNIQUE,
    voo_id BIGINT NOT NULL,
    bilhete_id BIGINT NOT NULL UNIQUE,
    ocupacao_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL REFERENCES usuario_sistema(id),
    codigo_cartao VARCHAR(40) NOT NULL UNIQUE,
    realizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelado_em TIMESTAMPTZ,
    FOREIGN KEY (item_id, voo_id) REFERENCES item_reserva(id, voo_id),
    FOREIGN KEY (bilhete_id, item_id) REFERENCES bilhete(id, item_id),
    FOREIGN KEY (ocupacao_id, item_id, voo_id) REFERENCES ocupacao_assento(id, item_id, voo_id),
    UNIQUE (id, voo_id),
    CHECK (cancelado_em IS NULL OR cancelado_em >= realizado_em)
);
$ddl_41$;

    etapa := '42: CREATE TABLE bagagem ( id';
    EXECUTE $ddl_42$
CREATE TABLE bagagem (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    checkin_id BIGINT NOT NULL REFERENCES check_in(id),
    etiqueta VARCHAR(30) NOT NULL UNIQUE,
    peso_kg NUMERIC(6,2) NOT NULL CHECK (peso_kg > 0),
    categoria VARCHAR(20) NOT NULL CHECK (categoria IN ('DESPACHADA','ESPECIAL')),
    taxa_excesso NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (taxa_excesso >= 0),
    situacao VARCHAR(20) NOT NULL DEFAULT 'RECEBIDA'
        CHECK (situacao IN ('RECEBIDA','INSPECIONADA','CARREGADA','DESCARREGADA','ENTREGUE','EXTRAVIADA','RETIRADA'))
);
$ddl_42$;

    etapa := '43: CREATE TABLE evento_bagagem ( id';
    EXECUTE $ddl_43$
CREATE TABLE evento_bagagem (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bagagem_id BIGINT NOT NULL REFERENCES bagagem(id),
    aeroporto_id BIGINT NOT NULL REFERENCES aeroporto(id),
    usuario_id BIGINT NOT NULL REFERENCES usuario_sistema(id),
    situacao VARCHAR(20) NOT NULL CHECK (situacao IN ('RECEBIDA','INSPECIONADA','CARREGADA','DESCARREGADA','ENTREGUE','EXTRAVIADA','RETIRADA')),
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacao TEXT
);
$ddl_43$;

    etapa := '44: CREATE TABLE embarque ( id';
    EXECUTE $ddl_44$
CREATE TABLE embarque (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    checkin_id BIGINT NOT NULL UNIQUE,
    voo_id BIGINT NOT NULL,
    alocacao_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL REFERENCES usuario_sistema(id),
    realizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (checkin_id, voo_id) REFERENCES check_in(id, voo_id),
    FOREIGN KEY (alocacao_id, voo_id) REFERENCES alocacao_recurso(id, voo_id)
);
$ddl_44$;

    etapa := '45: CREATE TABLE historico_voo ( id';
    EXECUTE $ddl_45$
CREATE TABLE historico_voo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voo_id BIGINT NOT NULL REFERENCES voo(id),
    usuario_id BIGINT REFERENCES usuario_sistema(id),
    alterado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    situacao_anterior VARCHAR(25),
    situacao_nova VARCHAR(25) NOT NULL,
    dados_anteriores JSONB,
    dados_novos JSONB NOT NULL,
    motivo TEXT
);
$ddl_45$;

    etapa := '46: CREATE TABLE ocorrencia_operacional ( id';
    EXECUTE $ddl_46$
CREATE TABLE ocorrencia_operacional (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aeroporto_id BIGINT NOT NULL REFERENCES aeroporto(id),
    voo_id BIGINT REFERENCES voo(id),
    usuario_id BIGINT NOT NULL REFERENCES usuario_sistema(id),
    tipo VARCHAR(25) NOT NULL CHECK (tipo IN ('ATRASO','METEOROLOGIA','SEGURANCA','TECNICA','OUTRA')),
    descricao TEXT NOT NULL,
    gravidade VARCHAR(10) NOT NULL CHECK (gravidade IN ('BAIXA','MEDIA','ALTA')),
    aberta_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    encerrada_em TIMESTAMPTZ,
    CHECK (encerrada_em IS NULL OR encerrada_em >= aberta_em)
);
$ddl_46$;

    etapa := '47: CREATE TABLE servico_solo ( id';
    EXECUTE $ddl_47$
CREATE TABLE servico_solo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voo_id BIGINT NOT NULL REFERENCES voo(id),
    aeroporto_id BIGINT NOT NULL REFERENCES aeroporto(id),
    responsavel_id BIGINT NOT NULL REFERENCES funcionario(pessoa_id),
    tipo VARCHAR(25) NOT NULL CHECK (tipo IN ('ABASTECIMENTO','LIMPEZA','CATERING','BAGAGEM','REBOQUE')),
    situacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' CHECK (situacao IN ('PENDENTE','EM_EXECUCAO','CONCLUIDO','CANCELADO')),
    inicio TIMESTAMPTZ,
    fim TIMESTAMPTZ,
    quantidade NUMERIC(12,2) CHECK (quantidade >= 0),
    unidade VARCHAR(20),
    custo NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (custo >= 0),
    CHECK (fim IS NULL OR (inicio IS NOT NULL AND fim >= inicio))
);
$ddl_47$;

    etapa := '48: CREATE TABLE evento_auditoria ( id';
    EXECUTE $ddl_48$
CREATE TABLE evento_auditoria (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuario_sistema(id),
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acao VARCHAR(50) NOT NULL,
    entidade VARCHAR(60) NOT NULL,
    registro_id BIGINT,
    detalhes JSONB NOT NULL DEFAULT '{}'::jsonb
);
$ddl_48$;

    etapa := '49: CREATE FUNCTION registrar_historico_voo() RETURNS TRIGGER';
    EXECUTE $ddl_49$
-- Histórico automático: configuração LOCAL no começo de cada transação Java.
-- SET LOCAL aeroporto.usuario_id = '1'; SET LOCAL aeroporto.motivo = 'Atraso meteorológico';
CREATE FUNCTION registrar_historico_voo() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO historico_voo(voo_id, usuario_id, situacao_nova, dados_novos, motivo)
        VALUES (NEW.id, NULLIF(current_setting('aeroporto.usuario_id', true), '')::BIGINT,
            NEW.situacao, to_jsonb(NEW), NULLIF(current_setting('aeroporto.motivo', true), ''));
    ELSIF to_jsonb(OLD) IS DISTINCT FROM to_jsonb(NEW) THEN
        INSERT INTO historico_voo(voo_id, usuario_id, situacao_anterior, situacao_nova, dados_anteriores, dados_novos, motivo)
        VALUES (NEW.id, NULLIF(current_setting('aeroporto.usuario_id', true), '')::BIGINT,
            OLD.situacao, NEW.situacao, to_jsonb(OLD), to_jsonb(NEW),
            NULLIF(current_setting('aeroporto.motivo', true), ''));
    END IF;
    RETURN NEW;
END;
$$;
$ddl_49$;

    etapa := '50: CREATE TRIGGER tg_historico_voo AFTER INSERT';
    EXECUTE $ddl_50$
CREATE TRIGGER tg_historico_voo AFTER INSERT OR UPDATE ON voo
    FOR EACH ROW EXECUTE FUNCTION registrar_historico_voo();
$ddl_50$;

    etapa := '51: CREATE INDEX ix_cidade_pais ON cidade(pais_id);';
    EXECUTE $ddl_51$
-- Índices de consulta e de junção (UNIQUE/PK/EXCLUDE já criam seus índices).
CREATE INDEX ix_cidade_pais ON cidade(pais_id);
$ddl_51$;

    etapa := '52: CREATE INDEX ix_aeroporto_cidade ON aeroporto(cidade_id);';
    EXECUTE $ddl_52$
CREATE INDEX ix_aeroporto_cidade ON aeroporto(cidade_id);
$ddl_52$;

    etapa := '53: CREATE INDEX ix_documento_pessoa ON documento_pessoa(pessoa_id);';
    EXECUTE $ddl_53$
CREATE INDEX ix_documento_pessoa ON documento_pessoa(pessoa_id);
$ddl_53$;

    etapa := '54: CREATE INDEX ix_funcionario_cargo ON funcionario(cargo_id);';
    EXECUTE $ddl_54$
CREATE INDEX ix_funcionario_cargo ON funcionario(cargo_id);
$ddl_54$;

    etapa := '55: CREATE INDEX ix_funcionario_base ON funcionario(aeroporto_base_id);';
    EXECUTE $ddl_55$
CREATE INDEX ix_funcionario_base ON funcionario(aeroporto_base_id);
$ddl_55$;

    etapa := '56: CREATE INDEX ix_aeronave_modelo ON aeronave(modelo_id);';
    EXECUTE $ddl_56$
CREATE INDEX ix_aeronave_modelo ON aeronave(modelo_id);
$ddl_56$;

    etapa := '57: CREATE INDEX ix_voo_rota_data ON voo(rota_id,';
    EXECUTE $ddl_57$
CREATE INDEX ix_voo_rota_data ON voo(rota_id, partida_prevista);
$ddl_57$;

    etapa := '58: CREATE INDEX ix_voo_situacao_data ON voo(situacao,';
    EXECUTE $ddl_58$
CREATE INDEX ix_voo_situacao_data ON voo(situacao, partida_prevista);
$ddl_58$;

    etapa := '59: CREATE INDEX ix_voo_aeronave ON voo(aeronave_id);';
    EXECUTE $ddl_59$
CREATE INDEX ix_voo_aeronave ON voo(aeronave_id);
$ddl_59$;

    etapa := '60: CREATE INDEX ix_alocacao_voo ON alocacao_recurso(voo_id);';
    EXECUTE $ddl_60$
CREATE INDEX ix_alocacao_voo ON alocacao_recurso(voo_id);
$ddl_60$;

    etapa := '61: CREATE INDEX ix_escala_voo ON escala_funcionario(voo_id);';
    EXECUTE $ddl_61$
CREATE INDEX ix_escala_voo ON escala_funcionario(voo_id);
$ddl_61$;

    etapa := '62: CREATE INDEX ix_reserva_comprador ON reserva(comprador_id);';
    EXECUTE $ddl_62$
CREATE INDEX ix_reserva_comprador ON reserva(comprador_id);
$ddl_62$;

    etapa := '63: CREATE INDEX ix_reserva_prazo ON reserva(expira_em)';
    EXECUTE $ddl_63$
CREATE INDEX ix_reserva_prazo ON reserva(expira_em) WHERE situacao = 'PENDENTE';
$ddl_63$;

    etapa := '64: CREATE INDEX ix_item_reserva ON item_reserva(reserva_id);';
    EXECUTE $ddl_64$
CREATE INDEX ix_item_reserva ON item_reserva(reserva_id);
$ddl_64$;

    etapa := '65: CREATE INDEX ix_item_voo ON item_reserva(voo_id,';
    EXECUTE $ddl_65$
CREATE INDEX ix_item_voo ON item_reserva(voo_id, situacao);
$ddl_65$;

    etapa := '66: CREATE INDEX ix_pagamento_reserva ON pagamento(reserva_id,';
    EXECUTE $ddl_66$
CREATE INDEX ix_pagamento_reserva ON pagamento(reserva_id, situacao);
$ddl_66$;

    etapa := '67: CREATE INDEX ix_reembolso_pagamento ON reembolso(pagamento_id,';
    EXECUTE $ddl_67$
CREATE INDEX ix_reembolso_pagamento ON reembolso(pagamento_id, situacao);
$ddl_67$;

    etapa := '68: CREATE INDEX ix_bagagem_checkin ON bagagem(checkin_id);';
    EXECUTE $ddl_68$
CREATE INDEX ix_bagagem_checkin ON bagagem(checkin_id);
$ddl_68$;

    etapa := '69: CREATE INDEX ix_evento_bagagem ON evento_bagagem(bagagem_id,';
    EXECUTE $ddl_69$
CREATE INDEX ix_evento_bagagem ON evento_bagagem(bagagem_id, ocorrido_em);
$ddl_69$;

    etapa := '70: CREATE INDEX ix_historico_voo ON historico_voo(voo_id,';
    EXECUTE $ddl_70$
CREATE INDEX ix_historico_voo ON historico_voo(voo_id, alterado_em);
$ddl_70$;

    etapa := '71: CREATE INDEX ix_ocorrencia_voo ON ocorrencia_operacional(voo_id);';
    EXECUTE $ddl_71$
CREATE INDEX ix_ocorrencia_voo ON ocorrencia_operacional(voo_id);
$ddl_71$;

    etapa := '72: CREATE INDEX ix_servico_voo ON servico_solo(voo_id);';
    EXECUTE $ddl_72$
CREATE INDEX ix_servico_voo ON servico_solo(voo_id);
$ddl_72$;

    etapa := '73: CREATE INDEX ix_auditoria_entidade ON evento_auditoria(entidade,';
    EXECUTE $ddl_73$
CREATE INDEX ix_auditoria_entidade ON evento_auditoria(entidade, registro_id, ocorrido_em);
$ddl_73$;

    etapa := '74: CREATE VIEW vw_painel_voos AS SELECT';
    EXECUTE $ddl_74$
CREATE VIEW vw_painel_voos AS
SELECT v.id, c.codigo_iata AS companhia, v.numero,
       ao.codigo_iata AS origem, ad.codigo_iata AS destino,
       v.partida_prevista, v.chegada_prevista, v.partida_estimada, v.chegada_estimada,
       v.partida_real, v.chegada_real, v.situacao,
       (COALESCE(v.partida_real, v.partida_estimada, v.partida_prevista) > v.partida_prevista) AS atrasado,
       EXTRACT(EPOCH FROM (COALESCE(v.partida_real, v.partida_estimada, v.partida_prevista) - v.partida_prevista))/60 AS diferenca_partida_min
FROM voo v JOIN companhia_aerea c ON c.id = v.companhia_id
JOIN rota r ON r.id = v.rota_id
JOIN aeroporto ao ON ao.id = r.origem_id JOIN aeroporto ad ON ad.id = r.destino_id;
$ddl_74$;

    etapa := '75: CREATE VIEW vw_disponibilidade_assentos AS SELECT';
    EXECUTE $ddl_75$
CREATE VIEW vw_disponibilidade_assentos AS
SELECT i.voo_id, i.classe, COUNT(*) AS total_assentos,
       COUNT(*) FILTER (WHERE i.bloqueado) AS bloqueados,
       COUNT(*) FILTER (WHERE o.id IS NOT NULL) AS ocupados,
       COUNT(*) FILTER (WHERE NOT i.bloqueado AND o.id IS NULL) AS livres
FROM inventario_assento_voo i LEFT JOIN ocupacao_assento o
ON o.inventario_id = i.id AND o.liberada_em IS NULL
GROUP BY i.voo_id, i.classe;
$ddl_75$;

    etapa := '76: CREATE VIEW vw_total_reserva AS SELECT';
    EXECUTE $ddl_76$
CREATE VIEW vw_total_reserva AS
SELECT r.id, r.localizador, COALESCE(SUM(i.valor_base + i.taxa_embarque - i.desconto), 0) AS total_original,
       COALESCE(SUM(i.valor_base + i.taxa_embarque - i.desconto)
           FILTER (WHERE i.situacao NOT IN ('CANCELADO','EXPIRADO')), 0) AS total_itens_vigentes
FROM reserva r LEFT JOIN item_reserva i ON i.reserva_id = r.id GROUP BY r.id, r.localizador;
$ddl_76$;

    RAISE NOTICE 'Estrutura criada: 41 tabelas no schema aeroporto.';
EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION USING
        MESSAGE = 'Instalação interrompida: ' || SQLERRM,
        DETAIL = 'Etapa: ' || COALESCE(etapa, 'verificação inicial') || '; SQLSTATE original: ' || SQLSTATE,
        HINT = 'Envie a mensagem e o detalhe completos. Nenhuma mudança desta instalação foi mantida.';
END;
$instalar_aeroporto$;
