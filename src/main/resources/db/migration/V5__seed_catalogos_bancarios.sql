-- =============================================================================
-- MIGRACIÓN V5: DATOS INICIALES DE CATÁLOGOS BANCARIOS (PAÍSES, ESTADO CIVIL, ESTATUS CUENTA)
-- =============================================================================

INSERT INTO paises (nombre, codigo_iso, activo)
VALUES ('MÉXICO', 'MEX', true)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO estado_civil (descripcion, activo)
VALUES 
    ('SOLTERO(A)', true),
    ('CASADO(A)', true),
    ('DIVORCIADO(A)', true),
    ('VIUDO(A)', true),
    ('UNIÓN LIBRE', true)
ON CONFLICT (descripcion) DO NOTHING;

INSERT INTO estatus_cuenta (nombre, descripcion, activo)
VALUES 
    ('ACTIVA', 'Cuenta Activa', true),
    ('BLOQUEADA', 'Cuenta Bloqueada', true),
    ('CANCELADA', 'Cuenta Cancelada', true)
ON CONFLICT (nombre) DO NOTHING;
