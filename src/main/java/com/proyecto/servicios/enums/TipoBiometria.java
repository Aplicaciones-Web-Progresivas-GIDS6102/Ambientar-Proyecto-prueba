package com.proyecto.servicios.enums;

/**
 * Enum que representa los tipos de muestras biométricas soportadas.
 * Coincide con la restricción CHECK de PostgreSQL (HUELLA_DACTILAR, RECONOCIMIENTO_FACIAL, IRIS, PATRON_VOZ).
 */
public enum TipoBiometria {
    HUELLA_DACTILAR,
    RECONOCIMIENTO_FACIAL,
    IRIS,
    PATRON_VOZ
}
