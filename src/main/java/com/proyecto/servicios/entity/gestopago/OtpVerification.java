package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa la verificación de OTP (2FA) por correo.
 * El OTP se almacena como hash BCrypt para evitar persistir valores en texto plano.
 * Se asocia de manera unívoca con la operación de pago para evitar que se use en otra.
 */
@Entity
@Table(name = "gestopago_otp_verifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del cliente al que pertenece el OTP. */
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    /** Identificador de la cuenta bancaria origen. */
    @Column(name = "cuenta_id")
    private Long cuentaId;

    /** Identificador del servicio GestoPago. */
    @Column(name = "id_servicio", length = 50)
    private String idServicio;

    /** Identificador del producto GestoPago. */
    @Column(name = "id_producto", length = 50)
    private String idProducto;

    /** Monto de la operación de pago. */
    @Column(name = "monto", precision = 15, scale = 2)
    private BigDecimal monto;

    /** Hash de vinculación unívoca con la operación (cuenta + servicio + producto + monto). */
    @Column(name = "operacion_hash", length = 64)
    private String operacionHash;

    /** Hash BCrypt del código OTP de 6 dígitos. */
    @Column(name = "otp_hash", nullable = false, length = 60)
    private String otpHash;

    /** Timestamp de expiración del OTP. */
    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiresAt;

    /** Número de intentos realizados para validar este OTP. */
    @Column(name = "intentos", nullable = false)
    private int attempts;

    /** Indica si el OTP ya ha sido consumido exitosamente. */
    @Column(name = "consumido", nullable = false)
    private boolean consumed;

    /** Timestamp de creación del registro. */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }
}
