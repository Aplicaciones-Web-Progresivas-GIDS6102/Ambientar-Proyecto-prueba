package com.proyecto.servicios.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA que representa la tabla de catálogo 'paises'.
 */
@Entity
@Table(name = "paises")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Column(name = "codigo_iso", nullable = false, unique = true)
    private String codigoIso;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
