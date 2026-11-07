package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.gestopago.OtpVerification;
import com.proyecto.servicios.exception.OtpExpiradoException;
import com.proyecto.servicios.exception.OtpIntentosAgotadosException;
import com.proyecto.servicios.exception.SegundoFactorInvalidoException;
import com.proyecto.servicios.repositorys.gestopago.OtpVerificationRepository;
import com.proyecto.servicios.service.EmailService;
import com.proyecto.servicios.service.SegundoFactorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de segunda autenticación (2FA) por correo electrónico vía Brevo.
 * Reglas de seguridad:
 * 1. Código numérico aleatorio de 6 dígitos generado con SecureRandom.
 * 2. Hash BCrypt persistido en base de datos; nunca se almacena en texto plano.
 * 3. Máximo de 3 intentos de validación por código.
 * 4. Vigencia configurable (5 minutos por defecto).
 * 5. Vinculación unívoca con la operación (cuenta, servicio, producto, monto).
 * 6. Invalidación inmediata tras consumo exitoso o agotamiento de intentos.
 */
@Service
@Slf4j
public class SegundoFactorServiceImpl implements SegundoFactorService {

    private static final int MAX_INTENTOS = 3;

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    @Value("${brevo.otp.validity-minutes:5}")
    private int vigenciaMinutos = 5;

    public SegundoFactorServiceImpl(OtpVerificationRepository otpRepository,
                                    EmailService emailService,
                                    PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public int solicitarOtp(Long clienteId, String correo, String nombreCliente, Long cuentaId,
                            String idServicio, String idProducto, BigDecimal monto) {
        if (clienteId == null) {
            throw new IllegalArgumentException("El clienteId es obligatorio para generar el OTP.");
        }
        if (correo == null || correo.isBlank()) {
            throw new SegundoFactorInvalidoException("El cliente no cuenta con correo electrónico registrado para 2FA.");
        }

        // 1. Invalidar OTPs previos no consumidos del cliente
        List<OtpVerification> previos = otpRepository.findAllByClienteIdAndConsumedFalse(clienteId);
        for (OtpVerification prev : previos) {
            prev.setConsumed(true);
            otpRepository.save(prev);
        }

        // 2. Generar código seguro de 6 dígitos
        int codigoNum = 100000 + secureRandom.nextInt(900000);
        String codigoTexto = String.valueOf(codigoNum);

        // 3. Hashear con BCrypt
        String otpHash = passwordEncoder.encode(codigoTexto);

        // 4. Calcular hash unívoco de la operación
        String operacionHash = calcularHashOperacion(cuentaId, idServicio, idProducto, monto);

        // 5. Persistir verificación en base de datos
        OtpVerification otp = OtpVerification.builder()
                .clienteId(clienteId)
                .cuentaId(cuentaId)
                .idServicio(idServicio)
                .idProducto(idProducto)
                .monto(monto)
                .operacionHash(operacionHash)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(vigenciaMinutos))
                .attempts(0)
                .consumed(false)
                .build();
        otpRepository.save(otp);

        log.info("Código OTP 2FA generado exitosamente para clienteId={}. Vigencia={} minutos.", clienteId, vigenciaMinutos);

        // 6. Enviar notificación por correo vía Brevo
        emailService.enviarOtpPago(correo, nombreCliente, codigoTexto, idServicio, idProducto, monto, vigenciaMinutos);

        return vigenciaMinutos * 60;
    }

    @Override
    public boolean validarYConsumir(Long clienteId, Long cuentaId, String idServicio, String idProducto,
                                     BigDecimal monto, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new SegundoFactorInvalidoException("El código de segunda autenticación es obligatorio.");
        }

        String operacionHash = calcularHashOperacion(cuentaId, idServicio, idProducto, monto);

        // Buscar primero por cliente y hash de operación específica
        Optional<OtpVerification> otpOpt = otpRepository
                .findTopByClienteIdAndOperacionHashAndConsumedFalseOrderByIdDesc(clienteId, operacionHash);

        if (otpOpt.isEmpty()) {
            // Verificar si tiene un OTP emitido para otra operación
            Optional<OtpVerification> otroOtp = otpRepository.findTopByClienteIdAndConsumedFalseOrderByIdDesc(clienteId);
            if (otroOtp.isPresent() && otroOtp.get().getOperacionHash() != null) {
                log.warn("Intento de uso de OTP en operación distinta a la autorizada. clienteId={}", clienteId);
                throw new SegundoFactorInvalidoException("El código de segunda autenticación pertenece a otra operación de pago y no puede ser reutilizado.");
            }
            throw new SegundoFactorInvalidoException("No existe un código de segunda autenticación activo para esta operación.");
        }

        return procesarValidacion(otpOpt.get(), codigo);
    }

    @Override
    public boolean validarYConsumir(Long clienteId, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new SegundoFactorInvalidoException("El código de segunda autenticación es obligatorio.");
        }

        OtpVerification otp = otpRepository.findTopByClienteIdAndConsumedFalseOrderByIdDesc(clienteId)
                .orElseThrow(() -> new SegundoFactorInvalidoException("No existe un código de segunda autenticación activo para este cliente."));

        return procesarValidacion(otp, codigo);
    }

    private boolean procesarValidacion(OtpVerification otp, String codigo) {
        LocalDateTime ahora = LocalDateTime.now();

        // 1. Validar vigencia
        if (ahora.isAfter(otp.getExpiresAt())) {
            otp.setConsumed(true);
            otpRepository.save(otp);
            log.warn("Código OTP 2FA expirado para clienteId={}", otp.getClienteId());
            throw new OtpExpiradoException("El código de segunda autenticación ha expirado. Solicite uno nuevo.");
        }

        // 2. Validar intentos previos
        if (otp.getAttempts() >= MAX_INTENTOS) {
            otp.setConsumed(true);
            otpRepository.save(otp);
            log.warn("Código OTP 2FA bloqueado por intentos agotados para clienteId={}", otp.getClienteId());
            throw new OtpIntentosAgotadosException("Se ha superado el número máximo de intentos permitidos (3). Solicite un nuevo código.");
        }

        // 3. Validar coincidencia de hash BCrypt
        if (!passwordEncoder.matches(codigo, otp.getOtpHash())) {
            int nuevosIntentos = otp.getAttempts() + 1;
            otp.setAttempts(nuevosIntentos);
            if (nuevosIntentos >= MAX_INTENTOS) {
                otp.setConsumed(true);
                otpRepository.save(otp);
                log.warn("Tercer intento fallido de OTP 2FA. Código invalidado para clienteId={}", otp.getClienteId());
                throw new OtpIntentosAgotadosException("Código de segunda autenticación incorrecto. Se han agotado los 3 intentos permitidos.");
            }
            otpRepository.save(otp);
            log.warn("Intento fallido de OTP 2FA ({}/3) para clienteId={}", nuevosIntentos, otp.getClienteId());
            throw new SegundoFactorInvalidoException("Código de segunda autenticación incorrecto. Intento " + nuevosIntentos + " de " + MAX_INTENTOS + ".");
        }

        // 4. Código válido: consumir inmediatamente para uso único
        otp.setConsumed(true);
        otpRepository.save(otp);
        log.info("Código OTP 2FA validado y consumido con éxito para clienteId={}", otp.getClienteId());
        return true;
    }

    public static String calcularHashOperacion(Long cuentaId, String idServicio, String idProducto, BigDecimal monto) {
        if (cuentaId == null || idServicio == null || idProducto == null || monto == null) {
            return null;
        }
        String data = cuentaId + ":" + idServicio + ":" + idProducto + ":" + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(data.hashCode());
        }
    }
}
