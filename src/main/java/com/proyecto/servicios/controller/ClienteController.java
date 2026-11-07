package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ClienteBajaRequestDTO;
import com.proyecto.servicios.dto.ClienteFiltroRequestDTO;
import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.service.ClienteService;
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
 * Controlador REST para la administración de clientes/usuarios.
 * Conserva exclusivamente tres funcionalidades:
 * 1. Registro de usuarios (POST).
 * 2. Consulta mediante filtros combinables (POST /busquedas).
 * 3. Borrado lógico de usuarios (PATCH /baja).
 * Cumple con todas las restricciones de arquitectura: sin @PathVariable, sin @RequestParam,
 * recepción exclusiva mediante Request DTO y @RequestBody con validación Bean Validation (@Valid),
 * sin exposición de entidades JPA y sin lógica de persistencia o try-catch en el controlador.
 */
@RestController
@RequestMapping({"/clientes", "/api/v1/clientes"})
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para registro, consulta mediante filtros combinables y borrado lógico")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(
            summary = "Registrar Cliente (Onboarding)",
            description = "Crea un nuevo cliente con sus datos validados y genera automáticamente su cuenta bancaria inicial."
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponseDTO> crearCliente(@Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crearCliente(request));
    }

    @Operation(
            summary = "Consultar Clientes con Filtros Combinables",
            description = "Obtiene los clientes activos según los criterios combinables enviados en el cuerpo de la petición. Excluye registros dados de baja."
    )
    @PostMapping(value = "/busquedas", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponseDTO>> buscarClientes(@Valid @RequestBody(required = false) ClienteFiltroRequestDTO filtro) {
        return ResponseEntity.ok(clienteService.buscarClientes(filtro));
    }

    @Operation(
            summary = "Baja Lógica de Cliente",
            description = "Marca al cliente como inactivo en el sistema y desactiva sus cuentas asociadas, preservando el registro físico."
    )
    @PatchMapping(value = "/baja", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponseDTO> darDeBaja(@Valid @RequestBody ClienteBajaRequestDTO request) {
        return ResponseEntity.ok(clienteService.darDeBaja(request));
    }
}
