package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import com.proyecto.servicios.service.GestoPagoProductoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


 //Controlador REST para exponer la consulta y sincronización de productos de GestoPago.
 //Expone endpoints HTTP para consultar la lista de productos (aprovechando caché Redis y respaldo offline en BD)
 //y para forzar la sincronización manual si se requiere fuera de la ejecución programada de las 6:00 AM.

@RestController
@RequestMapping("/api/v1/gestopago")
@Slf4j
public class GestoPagoProductoController {

    private final GestoPagoProductoService gestoPagoProductoService;

    public GestoPagoProductoController(GestoPagoProductoService gestoPagoProductoService) {
        this.gestoPagoProductoService = gestoPagoProductoService;
    }


    //Endpoint para consultar los productos. Utiliza Redis Caché y fallback offline en BD PostgreSQL si no hay Internet.

    @PostMapping(
            value = "/productos",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConsultaProductosResponse> consultarProductos(@Valid @RequestBody ConsultaProductosRequest request) {
        log.info("Petición recibida en Controller para consultar productos de GestoPago.");
        ConsultaProductosResponse response = gestoPagoProductoService.obtenerProductos(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(
            value = "/sincronizar",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConsultaProductosResponse> sincronizarProductosManualmente() {
        log.info("Petición de sincronización manual recibida en Controller.");
        ConsultaProductosResponse response = gestoPagoProductoService.sincronizarProductos();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
