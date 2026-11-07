package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.BiometriaClienteDTO;
import com.proyecto.servicios.service.BiometriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el registro y consulta de metadatos biométricos de clientes.
 */
@RestController
@RequestMapping("/biometria")
@RequiredArgsConstructor
@Tag(name = "Biometría", description = "Endpoints para registro y administración no sensible de datos biométricos de clientes")
public class BiometriaController {

    private final BiometriaService biometriaService;

    @Operation(summary = "Registrar Muestra Biométrica", description = "Registra metadatos de una muestra biométrica (huella, facial, iris, etc.) asocidada a un cliente.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BiometriaClienteDTO> registrarBiometria(@Valid @RequestBody BiometriaClienteDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(biometriaService.registrarBiometria(dto));
    }

    @Operation(summary = "Consultar Biometría por Cliente", description = "Obtiene los registros biométricos del cliente sin exponer plantillas sensibles.")
    @GetMapping(value = "/cliente/{clienteId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BiometriaClienteDTO>> obtenerBiometriasPorCliente(@PathVariable("clienteId") Long clienteId) {
        return ResponseEntity.ok(biometriaService.obtenerBiometriasPorCliente(clienteId));
    }

    @Operation(summary = "Desactivar Registro Biométrico", description = "Inactiva la muestra biométrica seleccionada.")
    @PutMapping(value = "/{id}/desactivar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BiometriaClienteDTO> desactivarBiometria(@PathVariable("id") Long id) {
        return ResponseEntity.ok(biometriaService.desactivarBiometria(id));
    }
}
