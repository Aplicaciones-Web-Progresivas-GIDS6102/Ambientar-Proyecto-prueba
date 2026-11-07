-- Script de migración Flyway para la tabla de productos de GestoPago.
-- Almacena una copia de respaldo en PostgreSQL para soporte offline cuando no hay conexión.

CREATE TABLE IF NOT EXISTS gestopago_productos (
    id                      BIGSERIAL PRIMARY KEY,
    servicio                VARCHAR(100)    NOT NULL DEFAULT '',
    producto                VARCHAR(150)    NOT NULL DEFAULT '',
    id_servicio             VARCHAR(50)     NOT NULL DEFAULT '',
    id_producto             VARCHAR(50)     NOT NULL DEFAULT '',
    id_cat_tipo_servicio    VARCHAR(50)     NOT NULL DEFAULT '',
    tipo_front              VARCHAR(50)     NOT NULL DEFAULT '',
    has_digito_verificador  BOOLEAN         NOT NULL DEFAULT FALSE,
    precio                  NUMERIC(10,2)   NOT NULL DEFAULT 0.0,
    show_ayuda              BOOLEAN         NOT NULL DEFAULT FALSE,
    tipo_referencia         VARCHAR(50)     NOT NULL DEFAULT '',
    legend                  TEXT            NOT NULL DEFAULT '',
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW()
);
