CREATE TABLE gestor_electoral (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuario(id),
    alcance_operacion VARCHAR(50) NOT NULL,
    CONSTRAINT chk_gestor_alcance CHECK (alcance_operacion IN ('NACIONAL', 'DEPARTAMENTAL', 'MUNICIPAL'))
);
