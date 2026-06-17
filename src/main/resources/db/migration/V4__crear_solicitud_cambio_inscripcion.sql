CREATE TABLE solicitudes_cambio_inscripcion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    municipio_origen_id UUID NOT NULL REFERENCES municipios(id),
    municipio_destino_id UUID NOT NULL REFERENCES municipios(id),
    motivo TEXT NOT NULL,
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo TEXT,
    gestor_id UUID REFERENCES usuarios(id),
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_resolucion TIMESTAMP,
    CONSTRAINT chk_estado_solicitud CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CANCELADA'))
);

CREATE INDEX idx_solicitud_usuario_estado ON solicitudes_cambio_inscripcion(usuario_id, estado);
CREATE INDEX idx_solicitud_estado_destino ON solicitudes_cambio_inscripcion(estado, municipio_destino_id);
