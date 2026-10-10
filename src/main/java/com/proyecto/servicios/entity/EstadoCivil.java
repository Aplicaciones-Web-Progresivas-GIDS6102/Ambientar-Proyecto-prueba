package com.proyecto.servicios.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA que representa la tabla de catálogo 'estado_civil'.
 */
@Entity
@Table(name = "estado_civil")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCivil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "descripcion", nullable = false, unique = true)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
