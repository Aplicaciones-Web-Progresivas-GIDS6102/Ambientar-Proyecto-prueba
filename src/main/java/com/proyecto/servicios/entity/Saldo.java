package com.proyecto.servicios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa el Saldo Monetario de la cuenta en la tabla 'saldos'.
 */
@Entity
@Table(name = "saldos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false, unique = true)
    @JsonIgnoreProperties("saldo")
    @ToString.Exclude
    private Cuenta cuenta;

    @Column(name = "saldo_disponible", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoDisponible = BigDecimal.ZERO;

    @Column(name = "saldo_contable", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoContable = BigDecimal.ZERO;

    @Column(name = "moneda", nullable = false)
    @Builder.Default
    private String moneda = "MXN";

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void onSaveOrUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
