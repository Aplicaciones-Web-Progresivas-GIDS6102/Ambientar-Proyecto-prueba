package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.service.Impl.BrevoEmailServiceImpl;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

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
    private HttpServer mockHttpServer;
    private int mockPort;

    @BeforeEach
    void setUp() {
        emailService = new BrevoEmailServiceImpl(mailSenderProvider, new ObjectMapper());
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@banco.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Banco Test");
    }

    @AfterEach
    void tearDown() {
        if (mockHttpServer != null) {
            mockHttpServer.stop(0);
        }
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
    @DisplayName("Envío con JavaMailSender lanza excepción y recurre a fallback")
    void enviarOtpPago_JavaMailSender_Falla() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.enviarOtpPago("usuario@example.com", "Juan", "123456", "2284", "14302", new BigDecimal("100.00"), 5));
    }

    @Test
    @DisplayName("Envío fallback simulado cuando JavaMailSender no está disponible")
    void enviarOtpPago_FallbackSimulado() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> emailService.enviarOtpPago("ab@example.com", null, "123456", "2284", "14302", new BigDecimal("100.00"), 5));
    }

    @Test
    @DisplayName("Envío con correo inválido para probar rama de enmascaramiento")
    void enviarOtpPago_EnmascararCorreoInvalido() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> emailService.enviarOtpPago("correo-sin-arroba", "Cliente", "123456", "2284", "14302", new BigDecimal("100.00"), 5));
    }

    @Test
    @DisplayName("Envío vía Brevo API exitoso (HTTP 201)")
    void enviarOtpPago_BrevoApi_Exitoso() throws IOException {
        mockHttpServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockHttpServer.getAddress().getPort();
        mockHttpServer.createContext("/smtp/email", exchange -> {
            byte[] response = "{\"messageId\":\"msg-12345\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        mockHttpServer.start();

        ReflectionTestUtils.setField(emailService, "brevoApiKey", "xkeysib-real-looking-key");
        ReflectionTestUtils.setField(emailService, "brevoApiUrl", "http://localhost:" + mockPort);

        assertDoesNotThrow(() -> emailService.enviarOtpPago("cliente@example.com", "Juan Perez", "654321", "2284", "14302", new BigDecimal("250.00"), 5));
    }

    @Test
    @DisplayName("Envío vía Brevo API con error HTTP 400 y detalle de error")
    void enviarOtpPago_BrevoApi_ErrorConDetalle() throws IOException {
        mockHttpServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockHttpServer.getAddress().getPort();
        mockHttpServer.createContext("/smtp/email", exchange -> {
            byte[] response = "{\"code\":\"invalid_parameter\",\"message\":\"bad email\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        mockHttpServer.start();

        ReflectionTestUtils.setField(emailService, "brevoApiKey", "xkeysib-real-looking-key");
        ReflectionTestUtils.setField(emailService, "brevoApiUrl", "http://localhost:" + mockPort);

        assertDoesNotThrow(() -> emailService.enviarOtpPago("cliente@example.com", null, "654321", "2284", "14302", new BigDecimal("250.00"), 5));
    }

    @Test
    @DisplayName("Envío con Brevo API key dummy (inicia con CAMBIAR) se salta Brevo API")
    void enviarOtpPago_BrevoApiKeyDummy_SeSalta() {
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "CAMBIAR_POR_API_KEY_REAL");
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> emailService.enviarOtpPago("cliente@test.com", "Cliente", "123456", "2284", "14302", new BigDecimal("100.00"), 5));
    }
}
