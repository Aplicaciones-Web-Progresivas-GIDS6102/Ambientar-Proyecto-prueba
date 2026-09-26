package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para la persistencia de productos GestoPago en PostgreSQL.
 */
@Entity
@Table(name = "gestopago_productos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GestoPagoProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "servicio", length = 100)
    private String servicio;

    @Column(name = "producto", length = 150)
    private String producto;

    @Column(name = "id_servicio", length = 50)
    private String idServicio;

    @Column(name = "id_producto", length = 50)
    private String idProducto;

    @Column(name = "id_cat_tipo_servicio", length = 50)
    private String idCatTipoServicio;

    @Column(name = "tipo_front", length = 50)
    private String tipoFront;

    @Column(name = "has_digito_verificador")
    private Boolean hasDigitoVerificador;

    @Column(name = "precio")
    private Double precio;

    @Column(name = "show_ayuda")
    private Boolean showAyuda;

    @Column(name = "tipo_referencia", length = 50)
    private String tipoReferencia;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void onSave() {
        if (fechaActualizacion == null) {
            fechaActualizacion = LocalDateTime.now();
        }
    }
}
