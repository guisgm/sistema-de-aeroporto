-- Migracao aditiva; preserva registros, PKs, FKs e historicos.
-- Executar conectado ao banco existente, depois de conferir sua configuracao.
DO $migracao$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('aeroporto.migracoes'));
    IF to_regclass('aeroporto.passageiro') IS NULL THEN
        RAISE EXCEPTION 'Instale a estrutura inicial antes das migracoes.';
    END IF;
    ALTER TABLE aeroporto.passageiro ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
    ALTER TABLE aeroporto.cargo ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
    ALTER TABLE aeroporto.modelo_aeronave ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
    ALTER TABLE aeroporto.rota ADD COLUMN IF NOT EXISTS ativa BOOLEAN NOT NULL DEFAULT TRUE;
END;
$migracao$;
