-- =============================================================================
-- MIGRACIÓN V3: CORRECCIONES Y AJUSTES ESTRUCTURALES DEL SISTEMA BANCARIO
-- =============================================================================
-- Ajusta las restricciones en PostgreSQL según los requisitos del proyecto:
-- 1. ingreso_mensual > 0 (impide 0 y valores negativos).
-- 2. movil exactamente 10 dígitos (REGEX '^[0-9]{10}$').
-- 3. codigo_postal exactamente 5 dígitos (REGEX '^[0-9]{5}$').
-- 4. curp exactamente 18 caracteres.
-- 5. rfc de 12 o 13 caracteres.
-- 6. correo máximo 100 caracteres.
-- 7. Eliminación de índices redundantes sobre columnas UNIQUE.
-- =============================================================================

-- 1. Ajustes en la tabla clientes
ALTER TABLE clientes
    ALTER COLUMN correo TYPE VARCHAR(100),
    ALTER COLUMN movil TYPE VARCHAR(10);

-- Actualizar restricción de ingreso mensual (> 0)
ALTER TABLE clientes
    DROP CONSTRAINT IF EXISTS chk_clientes_ingreso,
    ADD CONSTRAINT chk_clientes_ingreso CHECK (ingreso_mensual > 0);

-- Restricción para móvil de exactamente 10 dígitos
ALTER TABLE clientes
    ADD CONSTRAINT chk_clientes_movil CHECK (movil ~ '^[0-9]{10}$');

-- Restricción para CURP de exactamente 18 caracteres
ALTER TABLE clientes
    ADD CONSTRAINT chk_clientes_curp_len CHECK (LENGTH(curp) = 18);

-- Restricción para RFC de 12 o 13 caracteres
ALTER TABLE clientes
    ADD CONSTRAINT chk_clientes_rfc_len CHECK (LENGTH(rfc) IN (12, 13));

-- 2. Ajustes en la tabla domicilios
ALTER TABLE domicilios
    ALTER COLUMN codigo_postal TYPE VARCHAR(5);

-- Restricción para Código Postal de exactamente 5 dígitos
ALTER TABLE domicilios
    ADD CONSTRAINT chk_domicilios_cp CHECK (codigo_postal ~ '^[0-9]{5}$');

-- 3. Eliminación de índices redundantes (PostgreSQL genera implícitamente un índice único por cada CONSTRAINT UNIQUE)
DROP INDEX IF EXISTS idx_clientes_curp;
DROP INDEX IF EXISTS idx_clientes_rfc;
DROP INDEX IF EXISTS idx_clientes_correo;
DROP INDEX IF EXISTS idx_cuentas_numero_cuenta;
DROP INDEX IF EXISTS idx_usuarios_username;
DROP INDEX IF EXISTS idx_sesiones_token_sesion;

-- Recrear índice de clientes activos simplificado
DROP INDEX IF EXISTS idx_clientes_activo;
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo);
