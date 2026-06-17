CREATE TABLE gestores_electorales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id),
    alcance_operacion VARCHAR(50) NOT NULL,
    CONSTRAINT chk_gestor_alcance CHECK (alcance_operacion IN ('NACIONAL', 'DEPARTAMENTAL', 'MUNICIPAL'))
);
