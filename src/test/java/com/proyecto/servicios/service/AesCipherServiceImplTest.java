package com.proyecto.servicios.service;

import com.proyecto.servicios.service.Impl.AesCipherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesCipherServiceImplTest {

    private AesCipherServiceImpl cipherService;

    @BeforeEach
    void setUp() {
        cipherService = new AesCipherServiceImpl();
        // 32-byte key (AES-256) and 16-byte IV in Base64
        ReflectionTestUtils.setField(cipherService, "aesKeyBase64", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        ReflectionTestUtils.setField(cipherService, "aesIvBase64", "MDEyMzQ1Njc4OWFiY2RlZg==");
        cipherService.validarConfiguracion();
    }

    @Test
    @DisplayName("Cifrar y descifrar texto plano exitosamente")
    void cifrarYDescifrar_Exitoso() {
        String original = "Mensaje confidencial 12345";
        String cifrado = cipherService.cifrar(original);

        assertThat(cifrado).isNotNull().isNotEqualTo(original);

        String descifrado = cipherService.descifrar(cifrado);
        assertThat(descifrado).isEqualTo(original);
    }

    @Test
    @DisplayName("Cifrar texto nulo lanza excepción")
    void cifrar_Nulo_LanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> cipherService.cifrar(null));
    }

    @Test
    @DisplayName("Descifrar texto nulo lanza excepción")
    void descifrar_Nulo_LanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> cipherService.descifrar(null));
    }
}
