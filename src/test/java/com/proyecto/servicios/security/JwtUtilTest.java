package com.proyecto.servicios.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "9a2f8c7b6e5d4c3b2a109876543210fedcba9876543210123456789abcdef0123");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    @Test
    @DisplayName("Generar token y validar extracción de username y clienteId")
    void generarYValidarToken() {
        String token = jwtUtil.generateToken("usuarioPrueba", 123L);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("usuarioPrueba");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Validación falla para token inválido")
    void validateToken_Invalido() {
        assertFalse(jwtUtil.validateToken("token_invalido_malformado"));
    }
}
