package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import com.proyecto.servicios.service.GestoPagoProductoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para exponer la consulta de lista de productos de GestoPago.
**/
@RestController
@RequestMapping("/api/v1/gestopago")
@Slf4j
public class GestoPagoProductoController {

    private final GestoPagoProductoService gestoPagoProductoService;

    public GestoPagoProductoController(GestoPagoProductoService gestoPagoProductoService) {
        this.gestoPagoProductoService = gestoPagoProductoService;
    }

    @PostMapping(
            value = "/productos",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConsultaProductosResponse> consultarProductos(@Valid @RequestBody ConsultaProductosRequest request) {
        log.info("Petición recibida en Controller para consultar productos de GestoPago.");
        
        ConsultaProductosResponse response = gestoPagoProductoService.obtenerProductos(request);
        
        // Retorna HTTP 200 OK siempre con la estructura de respuesta validada
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
