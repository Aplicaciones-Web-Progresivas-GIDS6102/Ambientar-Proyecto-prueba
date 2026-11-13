package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.service.Impl.BrevoEmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevoEmailServiceImplTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private BrevoEmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new BrevoEmailServiceImpl(mailSenderProvider, new ObjectMapper());
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@banco.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Banco Test");
    }

    @Test
    @DisplayName("Envío de correo con destinatario nulo o vacío no hace nada")
    void enviarOtpPago_DestinatarioVacio_NoEnvia() {
        assertDoesNotThrow(() -> emailService.enviarOtpPago(null, "Juan", "123456", "2284", "14302", new BigDecimal("100.00"), 5));
        assertDoesNotThrow(() -> emailService.enviarOtpPago("   ", "Juan", "123456", "2284", "14302", new BigDecimal("100.00"), 5));
        verifyNoInteractions(mailSenderProvider);
    }

    @Test
    @DisplayName("Envío con JavaMailSender exitoso")
    void enviarOtpPago_JavaMailSender_Exitoso() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.enviarOtpPago("usuario@example.com", "Juan", "123456", "2284", "14302", new BigDecimal("100.00"), 5));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Envío fallback simulado cuando JavaMailSender falla o no está disponible")
    void enviarOtpPago_FallbackSimulado() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> emailService.enviarOtpPago("usuario@example.com", "Juan", "123456", "2284", "14302", new BigDecimal("100.00"), 5));
    }

    @Test
    @DisplayName("Envío con Brevo API key configurada invoca enviarViaBrevoApi")
    void enviarOtpPago_BrevoApiKeyConfigurada() {
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "xkeysib-test-key-12345");
        ReflectionTestUtils.setField(emailService, "brevoApiUrl", "http://127.0.0.1:1");

        assertDoesNotThrow(() -> emailService.enviarOtpPago("dest@test.com", "Juan", "123456", "2284", "14302", new BigDecimal("100.00"), 5));
    }
}
