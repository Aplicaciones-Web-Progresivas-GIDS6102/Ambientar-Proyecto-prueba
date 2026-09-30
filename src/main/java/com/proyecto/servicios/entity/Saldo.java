package com.proyecto.servicios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa el Saldo asociado a una Cuenta Bancaria en la tabla 'saldos'.
 * Utiliza BigDecimal para garantizar la precisión decimal exacta.
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
    @JsonIgnoreProperties({"saldo", "cliente"})
    @ToString.Exclude
    private Cuenta cuenta;

    @Column(name = "saldo_disponible", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoDisponible;

    @Column(name = "saldo_contable", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoContable;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void onSave() {
        if (moneda == null) moneda = "MXN";
        if (saldoDisponible == null) saldoDisponible = BigDecimal.ZERO;
        if (saldoContable == null) saldoContable = BigDecimal.ZERO;
        fechaActualizacion = LocalDateTime.now();
    }
}
