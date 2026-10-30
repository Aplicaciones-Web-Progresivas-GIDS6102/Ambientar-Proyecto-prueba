-- Tabla de verificaciones OTP (2FA) para pagos de servicios GestoPago.
-- Almacena el hash BCrypt del OTP y los datos de la operación vinculada.
CREATE TABLE IF NOT EXISTS gestopago_otp_verifications (
    id                  BIGSERIAL       PRIMARY KEY,
    cliente_id          BIGINT          NOT NULL,
    cuenta_id           BIGINT,
    id_servicio         VARCHAR(50),
    id_producto         VARCHAR(50),
    monto               NUMERIC(15,2),
    operacion_hash      VARCHAR(64),
    otp_hash            VARCHAR(60)     NOT NULL,
    expira_en           TIMESTAMP       NOT NULL,
    intentos            INT             NOT NULL DEFAULT 0,
    consumido           BOOLEAN         NOT NULL DEFAULT FALSE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Índice para búsquedas rápidas del OTP activo por cliente
CREATE INDEX IF NOT EXISTS idx_gestopago_otp_cliente ON gestopago_otp_verifications (cliente_id, consumido);

-- Índice para validación de la operación específica
CREATE INDEX IF NOT EXISTS idx_gestopago_otp_operacion ON gestopago_otp_verifications (cliente_id, operacion_hash, consumido);
