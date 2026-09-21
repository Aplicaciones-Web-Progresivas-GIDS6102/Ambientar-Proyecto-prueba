package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un producto de GestoPago en la base de datos PostgreSQL.
 * 
 * ¿QUÉ HACE?
 * Sirve como modelo relacional persistente para guardar los productos recuperados del servicio web externo.
 * Permite tener una copia local limpia de la lista de productos que sirve como respaldo (fallback) en modo offline
 * cuando el servicio externo no está disponible o no hay conexión a Internet.
 * 
 * ¿CÓMO LO HACE?
 * Mapea la tabla 'gestopago_productos' en PostgreSQL. Los datos se actualizan sobrescribiendo los registros anteriores
 * únicamente cuando se recibe una respuesta exitosa (HTTP 200) del servicio externo.
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
