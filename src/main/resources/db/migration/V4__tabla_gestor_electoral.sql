CREATE TABLE gestor_electoral (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuario(id),
    alcance_operacion VARCHAR(50) NOT NULL,
    CONSTRAINT chk_gestor_alcance CHECK (alcance_operacion IN ('NACIONAL', 'DEPARTAMENTAL', 'MUNICIPAL'))
);

INSERT INTO gestor_electoral (usuario_id, alcance_operacion)
SELECT id, alcance_operacion
FROM usuario
WHERE rol = 'GESTOR_ELECTORAL' AND alcance_operacion IS NOT NULL;

ALTER TABLE usuario DROP CONSTRAINT IF EXISTS chk_alcance;
ALTER TABLE usuario DROP COLUMN IF EXISTS alcance_operacion;
