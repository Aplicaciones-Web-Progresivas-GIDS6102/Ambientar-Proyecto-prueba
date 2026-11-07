package com.proyecto.servicios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa el registro de Sesión en la tabla 'sesiones'.
 */
@Entity
@Table(name = "sesiones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"sesiones", "cliente"})
    @ToString.Exclude
    private Usuario usuario;

    @Column(name = "token_sesion", nullable = false, unique = true, length = 500)
    private String tokenSesion;

    @Column(name = "activa", nullable = false)
    private Boolean activa;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_ultima_actividad", nullable = false)
    private LocalDateTime fechaUltimaActividad;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @PrePersist
    void onCreate() {
        if (activa == null) activa = true;
        fechaInicio = LocalDateTime.now();
        fechaUltimaActividad = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        fechaUltimaActividad = LocalDateTime.now();
    }
}
