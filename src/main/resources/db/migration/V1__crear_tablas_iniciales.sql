CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE departamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE municipio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL,
    departamento_id UUID NOT NULL REFERENCES departamento(id)
);

CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    telefono VARCHAR(50),
    documento VARCHAR(15) UNIQUE NOT NULL,
    municipio_id UUID NOT NULL REFERENCES municipio(id),
    rol VARCHAR(50) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE otp (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    expiracion TIMESTAMP NOT NULL,
    intentos INTEGER DEFAULT 0,
    estado VARCHAR(50) NOT NULL
);

CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_documento ON usuario(documento);
CREATE INDEX idx_otp_email_estado ON otp(email, estado);
CREATE INDEX idx_municipio_departamento ON municipio(departamento_id);

-- Seed: Departamentos de Colombia
INSERT INTO departamento (id, nombre) VALUES
    ('a1b2c3d4-0001-4000-8000-000000000001', 'Amazonas'),
    ('a1b2c3d4-0002-4000-8000-000000000002', 'Antioquia'),
    ('a1b2c3d4-0003-4000-8000-000000000003', 'Arauca'),
    ('a1b2c3d4-0004-4000-8000-000000000004', 'Atlántico'),
    ('a1b2c3d4-0005-4000-8000-000000000005', 'Bolívar'),
    ('a1b2c3d4-0006-4000-8000-000000000006', 'Boyacá'),
    ('a1b2c3d4-0007-4000-8000-000000000007', 'Caldas'),
    ('a1b2c3d4-0008-4000-8000-000000000008', 'Casanare'),
    ('a1b2c3d4-0009-4000-8000-000000000009', 'Cundinamarca'),
    ('a1b2c3d4-000a-4000-8000-00000000000a', 'Bogotá D.C.');

-- Seed: Algunos municipios de Colombia
-- Amazonas
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-0001-4000-8000-000000000011', 'Leticia', 'a1b2c3d4-0001-4000-8000-000000000001'),
    ('b1c2d3e4-0002-4000-8000-000000000012', 'Puerto Nariño', 'a1b2c3d4-0001-4000-8000-000000000001');

-- Antioquia
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-0003-4000-8000-000000000013', 'Medellín', 'a1b2c3d4-0002-4000-8000-000000000002'),
    ('b1c2d3e4-0004-4000-8000-000000000014', 'Envigado', 'a1b2c3d4-0002-4000-8000-000000000002'),
    ('b1c2d3e4-0005-4000-8000-000000000015', 'Rionegro', 'a1b2c3d4-0002-4000-8000-000000000002');

-- Arauca
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-0006-4000-8000-000000000016', 'Arauca', 'a1b2c3d4-0003-4000-8000-000000000003'),
    ('b1c2d3e4-0007-4000-8000-000000000017', 'Saravena', 'a1b2c3d4-0003-4000-8000-000000000003');

-- Atlántico
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-0008-4000-8000-000000000018', 'Barranquilla', 'a1b2c3d4-0004-4000-8000-000000000004'),
    ('b1c2d3e4-0009-4000-8000-000000000019', 'Soledad', 'a1b2c3d4-0004-4000-8000-000000000004');

-- Bolívar
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-000a-4000-8000-00000000001a', 'Cartagena', 'a1b2c3d4-0005-4000-8000-000000000005'),
    ('b1c2d3e4-000b-4000-8000-00000000001b', 'Magangué', 'a1b2c3d4-0005-4000-8000-000000000005');

-- Boyacá
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-000c-4000-8000-00000000001c', 'Tunja', 'a1b2c3d4-0006-4000-8000-000000000006'),
    ('b1c2d3e4-000d-4000-8000-00000000001d', 'Duitama', 'a1b2c3d4-0006-4000-8000-000000000006');

-- Caldas
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-000e-4000-8000-00000000001e', 'Manizales', 'a1b2c3d4-0007-4000-8000-000000000007'),
    ('b1c2d3e4-000f-4000-8000-00000000001f', 'La Dorada', 'a1b2c3d4-0007-4000-8000-000000000007');

-- Casanare
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-0010-4000-8000-000000000020', 'Yopal', 'a1b2c3d4-0008-4000-8000-000000000008'),
    ('b1c2d3e4-0011-4000-8000-000000000021', 'Aguazul', 'a1b2c3d4-0008-4000-8000-000000000008');

-- Cundinamarca
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-0012-4000-8000-000000000022', 'Soacha', 'a1b2c3d4-0009-4000-8000-000000000009'),
    ('b1c2d3e4-0013-4000-8000-000000000023', 'Zipaquirá', 'a1b2c3d4-0009-4000-8000-000000000009');

-- Bogotá D.C.
INSERT INTO municipio (id, nombre, departamento_id) VALUES
    ('b1c2d3e4-0014-4000-8000-000000000024', 'Bogotá', 'a1b2c3d4-000a-4000-8000-00000000000a');
