-- =============================================================================
-- MIGRACIÓN V2: ESTRUCTURA DEL SISTEMA BANCARIO Y ONBOARDING DE CLIENTES
-- =============================================================================
-- Respeto de la versión V1 preexistente (gestopago_tokens).
-- Define tablas, relaciones, restricciones e índices para:
-- 1. Clientes (Personas Físicas)
-- 2. Domicilios
-- 3. Cuentas Bancarias
-- 4. Saldos
-- 5. Usuarios / Credenciales
-- 6. Sesiones de Usuario
-- 7. Biometría de Clientes
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. TABLA: clientes
-- Representa a las personas físicas registradas en el sistema.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clientes (
    id                      BIGSERIAL PRIMARY KEY,
    nombre                  TEXT NOT NULL,
    segundo_nombre          TEXT,
    apellido_paterno        TEXT NOT NULL,
    apellido_materno        TEXT NOT NULL,
    fecha_nacimiento        DATE NOT NULL,
    curp                    VARCHAR(18) NOT NULL,
    rfc                     VARCHAR(13) NOT NULL,
    sexo                    VARCHAR(1) NOT NULL,
    nacionalidad            TEXT NOT NULL DEFAULT 'MEXICANA',
    estado_civil            TEXT,
    correo                  TEXT NOT NULL,
    movil                   VARCHAR(15) NOT NULL,
    telefono_alternativo    VARCHAR(15),
    ocupacion               TEXT,
    empresa                 TEXT,
    ingreso_mensual         NUMERIC(15,2) DEFAULT 0.00,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    eliminado               BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_baja              TIMESTAMP,
    fecha_creacion          TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_clientes_curp UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo),
    CONSTRAINT chk_clientes_sexo CHECK (sexo IN ('M', 'F', 'X')),
    CONSTRAINT chk_clientes_ingreso CHECK (ingreso_mensual >= 0)
);

-- -----------------------------------------------------------------------------
-- 2. TABLA: domicilios
-- Almacena la dirección principal asociada al cliente (Relación 1:1).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS domicilios (
    id                      BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL,
    calle                   TEXT NOT NULL,
    numero_exterior         VARCHAR(20) NOT NULL,
    numero_interior         VARCHAR(20),
    colonia                 TEXT NOT NULL,
    municipio               TEXT NOT NULL,
    estado                  TEXT NOT NULL,
    codigo_postal           VARCHAR(10) NOT NULL,
    pais                    TEXT NOT NULL DEFAULT 'MÉXICO',
    fecha_creacion          TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT uq_domicilios_cliente UNIQUE (cliente_id)
);

-- -----------------------------------------------------------------------------
-- 3. TABLA: cuentas
-- Almacena las cuentas bancarias pertenecientes a los clientes (Relación 1:N).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cuentas (
    id                      BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL,
    numero_cuenta           VARCHAR(20) NOT NULL,
    clabe                   VARCHAR(18) NOT NULL,
    tipo_cuenta             VARCHAR(30) NOT NULL DEFAULT 'DEBITO',
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion          TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT uq_cuentas_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT uq_cuentas_clabe UNIQUE (clabe),
    CONSTRAINT chk_cuentas_estado CHECK (estado IN ('ACTIVA', 'INACTIVA', 'BLOQUEADA', 'CANCELADA'))
);

-- -----------------------------------------------------------------------------
-- 4. TABLA: saldos
-- Representa el saldo monetario actual asociado a la cuenta (Relación 1:1).
-- Usa NUMERIC(15,2) para evitar errores de precisión de punto flotante (Double/Float).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saldos (
    id                      BIGSERIAL PRIMARY KEY,
    cuenta_id               BIGINT NOT NULL,
    saldo_disponible        NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    saldo_contable          NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    moneda                  VARCHAR(3) NOT NULL DEFAULT 'MXN',
    fecha_actualizacion     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_saldos_cuenta FOREIGN KEY (cuenta_id) 
        REFERENCES cuentas(id) ON DELETE CASCADE,
    CONSTRAINT uq_saldos_cuenta UNIQUE (cuenta_id),
    CONSTRAINT chk_saldos_disponible CHECK (saldo_disponible >= 0),
    CONSTRAINT chk_saldos_contable CHECK (saldo_contable >= 0)
);

-- -----------------------------------------------------------------------------
-- 5. TABLA: usuarios
-- Almacena las credenciales de acceso al sistema ligadas a un cliente (Relación 1:1).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id                      BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL,
    username                VARCHAR(50) NOT NULL,
    password_hash           VARCHAR(255) NOT NULL,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    intentos_fallidos       INTEGER NOT NULL DEFAULT 0,
    bloqueado               BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_ultimo_acceso     TIMESTAMP,
    fecha_creacion          TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_usuarios_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT uq_usuarios_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_usuarios_username UNIQUE (username)
);

-- -----------------------------------------------------------------------------
-- 6. TABLA: sesiones
-- Registra las sesiones de usuario activas e históricas para auditoría y control.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sesiones (
    id                      BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT NOT NULL,
    token_sesion            VARCHAR(500) NOT NULL,
    activa                  BOOLEAN NOT NULL DEFAULT TRUE,
    ip_origen               VARCHAR(45),
    user_agent              TEXT,
    fecha_inicio            TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_ultima_actividad  TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_expiracion        TIMESTAMP NOT NULL,

    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (usuario_id) 
        REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uq_sesiones_token UNIQUE (token_sesion)
);

-- -----------------------------------------------------------------------------
-- 7. TABLA: biometria_clientes
-- Registra plantillas y vectores biométricos en formato BYTEA/TEXT con metadatos.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biometria_clientes (
    id                      BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL,
    tipo_biometria          VARCHAR(30) NOT NULL,
    plantilla_biometrica    BYTEA,
    vector_caracteristicas  TEXT,
    hash_biometrico         VARCHAR(64),
    algoritmo               VARCHAR(50),
    puntuacion_calidad      NUMERIC(5,2),
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_biometria_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT chk_biometria_tipo CHECK (tipo_biometria IN ('HUELLA_DACTILAR', 'RECONOCIMIENTO_FACIAL', 'IRIS', 'PATRON_VOZ'))
);

-- =============================================================================
-- ÍNDICES OPTIMIZADOS PARA CONSULTAS DEL SISTEMA BANCARIO
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_clientes_curp ON clientes(curp);
CREATE INDEX IF NOT EXISTS idx_clientes_rfc ON clientes(rfc);
CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes(correo);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo, eliminado);

CREATE INDEX IF NOT EXISTS idx_cuentas_numero_cuenta ON cuentas(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_estado ON cuentas(estado, activo);

CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios(username);
CREATE INDEX IF NOT EXISTS idx_usuarios_cliente_id ON usuarios(cliente_id);

CREATE INDEX IF NOT EXISTS idx_sesiones_token_sesion ON sesiones(token_sesion);
CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_activa ON sesiones(usuario_id, activa);

CREATE INDEX IF NOT EXISTS idx_biometria_cliente_tipo ON biometria_clientes(cliente_id, tipo_biometria);
