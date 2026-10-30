package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de una transacción de pago de servicio GestoPago.
 * Almacena el UPC generado y el estado para correlación con confirmTx.
 */
@Entity
@Table(name = "gestopago_transacciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GestoPagoTransaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "id_servicio", nullable = false, length = 50)
    private String idServicio;

    @Column(name = "id_producto", nullable = false, length = 50)
    private String idProducto;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", nullable = false, length = 10)
    private String moneda;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "referencia", length = 50)
    private String referencia;

    @Column(name = "upc", nullable = false, unique = true, length = 100)
    private String upc;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "codigo_respuesta", length = 10)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", columnDefinition = "TEXT")
    private String mensajeRespuesta;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (estado == null) estado = "PENDIENTE";
        if (moneda == null) moneda = "MXN";
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
