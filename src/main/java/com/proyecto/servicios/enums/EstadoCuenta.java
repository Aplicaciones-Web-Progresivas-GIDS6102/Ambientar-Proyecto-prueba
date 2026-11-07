package com.proyecto.servicios.enums;

/**
 * Enum que representa el estado de una cuenta bancaria.
 * Coincide con la restricción CHECK de PostgreSQL (ACTIVA, INACTIVA, BLOQUEADA, CANCELADA).
 */
public enum EstadoCuenta {
    ACTIVA,
    INACTIVA,
    BLOQUEADA,
    CANCELADA
}
