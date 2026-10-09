package com.proyecto.servicios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad JPA que representa la tabla 'informacion_laboral' (Relación 1:1 con 'clientes').
 */
@Entity
@Table(name = "informacion_laboral")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformacionLaboral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"informacionLaboral", "domicilio", "cuentas", "usuario", "datosBiometricos"})
    @ToString.Exclude
    private Cliente cliente;

    @Column(name = "ocupacion", nullable = false)
    private String ocupacion;

    @Column(name = "empresa", nullable = false)
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal ingresoMensual;
}
