package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.OtpVerification;
import com.proyecto.servicios.exception.OtpExpiradoException;
import com.proyecto.servicios.exception.OtpIntentosAgotadosException;
import com.proyecto.servicios.exception.SegundoFactorInvalidoException;
import com.proyecto.servicios.repositorys.gestopago.OtpVerificationRepository;
import com.proyecto.servicios.service.Impl.SegundoFactorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SegundoFactorServiceImplTest {

    @Mock
    private OtpVerificationRepository otpRepository;

    @Mock
    private EmailService emailService;

    private PasswordEncoder passwordEncoder;
    private SegundoFactorServiceImpl segundoFactorService;

    private static final Long CLIENTE_ID = 10L;
    private static final Long CUENTA_ID = 1L;
    private static final String CORREO = "carlos@banco.com";
    private static final String SERVICIO = "2284";
    private static final String PRODUCTO = "14302";
    private static final BigDecimal MONTO = new BigDecimal("100.00");

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        segundoFactorService = new SegundoFactorServiceImpl(otpRepository, emailService, passwordEncoder);
    }

    @Test
    void solicitarOtp_GeneraCodigoSeguroYEnviaPorCorreo() {
        when(otpRepository.findAllByClienteIdAndConsumedFalse(CLIENTE_ID)).thenReturn(Collections.emptyList());

        int vigencia = segundoFactorService.solicitarOtp(CLIENTE_ID, CORREO, "Carlos", CUENTA_ID, SERVICIO, PRODUCTO, MONTO);

        assertThat(vigencia).isEqualTo(300); // 5 minutos = 300 segundos

        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpRepository).save(captor.capture());
        OtpVerification guardado = captor.getValue();

        assertThat(guardado.getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(guardado.getOperacionHash()).isNotNull();
        assertThat(guardado.getOtpHash()).isNotEqualTo("123456");
        assertThat(guardado.isConsumed()).isFalse();
        assertThat(guardado.getAttempts()).isZero();

        verify(emailService).enviarOtpPago(eq(CORREO), eq("Carlos"), anyString(), eq(SERVICIO), eq(PRODUCTO), eq(MONTO), eq(5));
    }

    @Test
    void validarYConsumir_CodigoCorrecto_RetornaTrueYMarcaConsumido() {
        String codigoTexto = "543210";
        String hash = passwordEncoder.encode(codigoTexto);
        String opHash = SegundoFactorServiceImpl.calcularHashOperacion(CUENTA_ID, SERVICIO, PRODUCTO, MONTO);

        OtpVerification otp = OtpVerification.builder()
                .id(1L)
                .clienteId(CLIENTE_ID)
                .operacionHash(opHash)
                .otpHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .consumed(false)
                .build();

        when(otpRepository.findTopByClienteIdAndOperacionHashAndConsumedFalseOrderByIdDesc(CLIENTE_ID, opHash))
                .thenReturn(Optional.of(otp));

        boolean resultado = segundoFactorService.validarYConsumir(CLIENTE_ID, CUENTA_ID, SERVICIO, PRODUCTO, MONTO, codigoTexto);

        assertThat(resultado).isTrue();
        assertThat(otp.isConsumed()).isTrue();
        verify(otpRepository).save(otp);
    }

    @Test
    void validarYConsumir_CodigoIncorrecto_IncrementaIntentosYLanzaExcepcion() {
        String hash = passwordEncoder.encode("543210");
        String opHash = SegundoFactorServiceImpl.calcularHashOperacion(CUENTA_ID, SERVICIO, PRODUCTO, MONTO);

        OtpVerification otp = OtpVerification.builder()
                .id(1L)
                .clienteId(CLIENTE_ID)
                .operacionHash(opHash)
                .otpHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .consumed(false)
                .build();

        when(otpRepository.findTopByClienteIdAndOperacionHashAndConsumedFalseOrderByIdDesc(CLIENTE_ID, opHash))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> segundoFactorService.validarYConsumir(CLIENTE_ID, CUENTA_ID, SERVICIO, PRODUCTO, MONTO, "000000"))
                .isInstanceOf(SegundoFactorInvalidoException.class)
                .hasMessageContaining("Intento 1 de 3");

        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(otp.isConsumed()).isFalse();
        verify(otpRepository).save(otp);
    }

    @Test
    void validarYConsumir_AgotarTresIntentos_BloqueaOtp() {
        String hash = passwordEncoder.encode("543210");
        String opHash = SegundoFactorServiceImpl.calcularHashOperacion(CUENTA_ID, SERVICIO, PRODUCTO, MONTO);

        OtpVerification otp = OtpVerification.builder()
                .id(1L)
                .clienteId(CLIENTE_ID)
                .operacionHash(opHash)
                .otpHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(2) // Ya llevaba 2 intentos previos
                .consumed(false)
                .build();

        when(otpRepository.findTopByClienteIdAndOperacionHashAndConsumedFalseOrderByIdDesc(CLIENTE_ID, opHash))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> segundoFactorService.validarYConsumir(CLIENTE_ID, CUENTA_ID, SERVICIO, PRODUCTO, MONTO, "000000"))
                .isInstanceOf(OtpIntentosAgotadosException.class)
                .hasMessageContaining("agotado los 3 intentos");

        assertThat(otp.getAttempts()).isEqualTo(3);
        assertThat(otp.isConsumed()).isTrue();
        verify(otpRepository).save(otp);
    }

    @Test
    void validarYConsumir_OtpExpirado_LanzaOtpExpiradoException() {
        String hash = passwordEncoder.encode("543210");
        String opHash = SegundoFactorServiceImpl.calcularHashOperacion(CUENTA_ID, SERVICIO, PRODUCTO, MONTO);

        OtpVerification otp = OtpVerification.builder()
                .id(1L)
                .clienteId(CLIENTE_ID)
                .operacionHash(opHash)
                .otpHash(hash)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Expiró hace 1 minuto
                .attempts(0)
                .consumed(false)
                .build();

        when(otpRepository.findTopByClienteIdAndOperacionHashAndConsumedFalseOrderByIdDesc(CLIENTE_ID, opHash))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> segundoFactorService.validarYConsumir(CLIENTE_ID, CUENTA_ID, SERVICIO, PRODUCTO, MONTO, "543210"))
                .isInstanceOf(OtpExpiradoException.class)
                .hasMessageContaining("ha expirado");

        assertThat(otp.isConsumed()).isTrue();
        verify(otpRepository).save(otp);
    }

    @Test
    void validarYConsumir_OtpDeOtraOperacion_RechazaReutilizacion() {
        String hash = passwordEncoder.encode("543210");
        String opHashOriginal = SegundoFactorServiceImpl.calcularHashOperacion(CUENTA_ID, SERVICIO, PRODUCTO, MONTO);
        String opHashDiferente = SegundoFactorServiceImpl.calcularHashOperacion(CUENTA_ID, "9999", "88888", new BigDecimal("500.00"));

        OtpVerification otpOriginal = OtpVerification.builder()
                .id(1L)
                .clienteId(CLIENTE_ID)
                .operacionHash(opHashOriginal)
                .otpHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .consumed(false)
                .build();

        when(otpRepository.findTopByClienteIdAndOperacionHashAndConsumedFalseOrderByIdDesc(CLIENTE_ID, opHashDiferente))
                .thenReturn(Optional.empty());
        when(otpRepository.findTopByClienteIdAndConsumedFalseOrderByIdDesc(CLIENTE_ID))
                .thenReturn(Optional.of(otpOriginal));

        assertThatThrownBy(() -> segundoFactorService.validarYConsumir(CLIENTE_ID, CUENTA_ID, "9999", "88888", new BigDecimal("500.00"), "543210"))
                .isInstanceOf(SegundoFactorInvalidoException.class)
                .hasMessageContaining("pertenece a otra operación");
    }
}
