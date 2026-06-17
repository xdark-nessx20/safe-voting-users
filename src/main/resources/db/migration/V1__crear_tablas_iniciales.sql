CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE departamentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE municipios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL,
    departamento_id UUID NOT NULL REFERENCES departamentos(id)
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    telefono VARCHAR(50),
    documento VARCHAR(15) UNIQUE NOT NULL,
    municipio_id UUID NOT NULL REFERENCES municipios(id),
    rol VARCHAR(50) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    expiracion TIMESTAMP NOT NULL,
    intentos INTEGER DEFAULT 0,
    estado VARCHAR(50) NOT NULL
);

CREATE INDEX idx_usuario_email ON usuarios(email);
CREATE INDEX idx_usuario_documento ON usuarios(documento);
CREATE INDEX idx_otp_email_estado ON otps(email, estado);
CREATE INDEX idx_municipio_departamento ON municipios(departamento_id);
