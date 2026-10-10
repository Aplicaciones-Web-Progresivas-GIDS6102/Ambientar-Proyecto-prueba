package com.proyecto.servicios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa a un Cliente (Persona Física) en la tabla 'clientes'.
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "segundo_nombre")
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "curp", nullable = false, unique = true)
    private String curp;

    @Column(name = "rfc", nullable = false, unique = true)
    private String rfc;

    @Column(name = "sexo", nullable = false)
    private String sexo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nacionalidad_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Pais nacionalidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_civil_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private EstadoCivil estadoCivil;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "telefono_movil", nullable = false)
    private String telefonoMovil;

    @Column(name = "telefono_alternativo")
    private String telefonoAlternativo;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    // Relaciones JPA
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    @JsonIgnoreProperties("cliente")
    @ToString.Exclude
    private Domicilio domicilio;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    @JsonIgnoreProperties("cliente")
    @ToString.Exclude
    private InformacionLaboral informacionLaboral;

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("cliente")
    @Builder.Default
    @ToString.Exclude
    private List<Cuenta> cuentas = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("cliente")
    @Builder.Default
    @ToString.Exclude
    private List<DatosBiometricos> datosBiometricos = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (activo == null) activo = true;
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
