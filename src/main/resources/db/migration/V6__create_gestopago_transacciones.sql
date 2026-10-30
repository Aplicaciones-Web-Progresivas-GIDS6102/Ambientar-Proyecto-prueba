-- Tabla de transacciones de pagos de servicios GestoPago.
-- Almacena el UPC generado, estado, y datos para correlacionar con confirmTx.
CREATE TABLE IF NOT EXISTS gestopago_transacciones (
    id                  BIGSERIAL       PRIMARY KEY,
    cuenta_id           BIGINT          NOT NULL,
    cliente_id          BIGINT          NOT NULL,
    id_servicio         VARCHAR(50)     NOT NULL,
    id_producto         VARCHAR(50)     NOT NULL,
    monto               NUMERIC(15,2)   NOT NULL,
    moneda              VARCHAR(10)     NOT NULL DEFAULT 'MXN',
    telefono            VARCHAR(20),
    referencia          VARCHAR(50),
    upc                 VARCHAR(100)    NOT NULL,
    estado              VARCHAR(30)     NOT NULL DEFAULT 'PENDIENTE',
    codigo_respuesta    VARCHAR(10),
    mensaje_respuesta   TEXT,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gestopago_tx_upc UNIQUE (upc)
);

-- Índice para búsquedas por cuenta y estado
CREATE INDEX IF NOT EXISTS idx_gestopago_tx_cuenta_estado ON gestopago_transacciones (cuenta_id, estado);

-- Índice para búsquedas por UPC
CREATE INDEX IF NOT EXISTS idx_gestopago_tx_upc ON gestopago_transacciones (upc);
