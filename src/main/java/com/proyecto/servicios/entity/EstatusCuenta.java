package com.proyecto.servicios.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA que representa la tabla de catálogo 'estatus_cuenta'.
 */
@Entity
@Table(name = "estatus_cuenta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstatusCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
