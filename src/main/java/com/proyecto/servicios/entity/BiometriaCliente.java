package com.proyecto.servicios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.proyecto.servicios.enums.TipoBiometria;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa las Muestras Biométricas del cliente en la tabla 'biometria_clientes'.
 */
@Entity
@Table(name = "biometria_clientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometriaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"biometrias", "domicilio", "cuentas", "usuario"})
    @ToString.Exclude
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_biometria", nullable = false, length = 30)
    private TipoBiometria tipoBiometria;

    @Column(name = "plantilla_biometrica")
    private byte[] plantillaBiometrica;

    @Column(name = "vector_caracteristicas", columnDefinition = "TEXT")
    private String vectorCaracteristicas;

    @Column(name = "hash_biometrico", length = 64)
    private String hashBiometrico;

    @Column(name = "algoritmo", length = 50)
    private String algoritmo;

    @Column(name = "puntuacion_calidad", precision = 5, scale = 2)
    private BigDecimal puntuacionCalidad;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void onCreate() {
        if (activo == null) activo = true;
        fechaRegistro = LocalDateTime.now();
    }
}
