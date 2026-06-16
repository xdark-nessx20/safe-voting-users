ALTER TABLE usuario ADD COLUMN IF NOT EXISTS alcance_operacion VARCHAR(50);

ALTER TABLE usuario DROP CONSTRAINT IF EXISTS chk_alcance;
ALTER TABLE usuario ADD CONSTRAINT chk_alcance CHECK (alcance_operacion IN ('NACIONAL', 'DEPARTAMENTAL', 'MUNICIPAL') OR alcance_operacion IS NULL);
