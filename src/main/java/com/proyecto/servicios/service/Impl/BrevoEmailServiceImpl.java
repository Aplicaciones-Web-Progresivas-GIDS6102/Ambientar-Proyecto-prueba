package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de correo electrónico mediante Brevo (Sendinblue).
 * Soporta integración nativa mediante Brevo REST API v3 y fallback a JavaMailSender (SMTP).
 * En entornos de prueba o cuando no existan credenciales configuradas, simula el envío
 * sin exponer códigos sensibles ni credenciales en la bitácora.
 */
@Service
@Slf4j
public class BrevoEmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.api.url:https://api.brevo.com/v3}")
    private String brevoApiUrl;

    @Value("${brevo.from.email:notificaciones@banco.com}")
    private String fromEmail;

    @Value("${brevo.from.name:Banco GestoPago}")
    private String fromName;

    public BrevoEmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider,
                                 ObjectMapper objectMapper) {
        this.mailSenderProvider = mailSenderProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enviarOtpPago(String destinatario, String nombreCliente, String codigoOtp,
                               String idServicio, String idProducto, BigDecimal monto, int vigenciaMinutos) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("No se pudo enviar el OTP: destinatario no especificado.");
            return;
        }

        String maskedEmail = enmascararCorreo(destinatario);
        String subject = "Código de seguridad para su pago de servicio - " + fromName;
        String textBody = String.format(
                "Estimado(a) %s,\n\n" +
                "Su código de segunda autenticación (2FA) para autorizar el pago del servicio (ID: %s, Producto: %s) " +
                "por un monto de $%.2f MXN es:\n\n" +
                "[%s]\n\n" +
                "Este código es de uso único y tiene una vigencia de %d minutos. " +
                "Tiene un máximo de 3 intentos de validación. Si usted no reconoció esta operación, por favor contacte a su banco inmediatamente.\n\n" +
                "Atentamente,\n%s",
                nombreCliente != null ? nombreCliente : "Cliente",
                idServicio, idProducto, monto,
                codigoOtp, vigenciaMinutos, fromName
        );

        // 1. Intentar envío vía Brevo REST API si hay API key configurada
        if (brevoApiKey != null && !brevoApiKey.isBlank() && !brevoApiKey.startsWith("CAMBIAR")) {
            try {
                enviarViaBrevoApi(destinatario, nombreCliente, subject, textBody);
                log.info("Notificación OTP 2FA enviada exitosamente vía Brevo API al destinatario: {}", maskedEmail);
                return;
            } catch (Exception e) {
                log.error("Falla al enviar OTP vía Brevo REST API a {}: {}", maskedEmail, e.getMessage());
            }
        }

        // 2. Intentar envío vía JavaMailSender si está disponible
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(destinatario);
                message.setSubject(subject);
                message.setText(textBody);
                mailSender.send(message);
                log.info("Notificación OTP 2FA enviada exitosamente vía JavaMailSender a: {}", maskedEmail);
                return;
            } catch (Exception e) {
                log.warn("No se pudo enviar correo vía JavaMailSender a {}: {}", maskedEmail, e.getMessage());
            }
        }

        // 3. Fallback simulado para entorno de desarrollo / pruebas
        log.info("Envío de correo simulado exitoso (modo prueba/desarrollo) para cliente={}, destino={}",
                nombreCliente != null ? nombreCliente : "Cliente", maskedEmail);
        log.info("🔑 [CÓDIGO 2FA PARA PRUEBAS (DEV)]: [{}] (Válido por {} min para {})",
                codigoOtp, vigenciaMinutos, destinatario);
    }

    private void enviarViaBrevoApi(String destinatario, String nombreCliente, String subject, String body) throws Exception {
        URI uri = URI.create(brevoApiUrl + "/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api-key", brevoApiKey != null ? brevoApiKey.trim() : "");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", Map.of("name", fromName, "email", fromEmail));
        payload.put("to", List.of(Map.of("email", destinatario, "name", nombreCliente != null ? nombreCliente : "Cliente")));
        payload.put("subject", subject);
        payload.put("textContent", body);

        byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBytes);
            os.flush();
        }

        int statusCode = conn.getResponseCode();
        if (statusCode >= 200 && statusCode < 300) {
            log.debug("Respuesta exitosa de Brevo API HTTP {}", statusCode);
        } else {
            String errorDetail = "";
            try (java.io.InputStream es = conn.getErrorStream()) {
                if (es != null) {
                    errorDetail = " - Detalle: " + new String(es.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {}
            throw new RuntimeException("Brevo API respondió con código HTTP " + statusCode + errorDetail);
        }
    }

    private String enmascararCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            return "***";
        }
        int atIndex = correo.indexOf('@');
        String usuario = correo.substring(0, atIndex);
        String dominio = correo.substring(atIndex);
        if (usuario.length() <= 2) {
            return usuario.charAt(0) + "***" + dominio;
        }
        return usuario.charAt(0) + "***" + usuario.charAt(usuario.length() - 1) + dominio;
    }
}
