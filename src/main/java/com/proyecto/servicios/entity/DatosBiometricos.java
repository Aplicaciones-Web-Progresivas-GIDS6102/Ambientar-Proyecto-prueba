package com.proyecto.servicios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA que representa la información biométrica asociada al cliente en la tabla 'datos_biometricos'.
 */
@Entity
@Table(name = "datos_biometricos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatosBiometricos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"datosBiometricos", "domicilio", "cuentas", "informacionLaboral", "usuario"})
    @ToString.Exclude
    private Cliente cliente;
}
