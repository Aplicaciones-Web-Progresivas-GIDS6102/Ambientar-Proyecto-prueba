package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para el Onboarding y Administración de Clientes (Personas Físicas).
 * Soporta las rutas /clientes y /api/v1/clientes.
 */
@RestController
@RequestMapping({"/clientes", "/api/v1/clientes"})
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para el Onboarding completo, consultas, actualización y baja lógica de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Registrar Cliente (Onboarding)", description = "Crea un nuevo cliente con su domicilio, genera automáticamente su cuenta bancaria y saldo inicial.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponseDTO> crearCliente(@Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crearCliente(request));
    }

    @Operation(summary = "Listar Clientes Activos", description = "Obtiene la lista de clientes registrados que se encuentran activos en la plataforma.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponseDTO>> obtenerClientesActivos() {
        return ResponseEntity.ok(clienteService.obtenerClientesActivos());
    }

    @Operation(summary = "Obtener Cliente por ID", description = "Busca un cliente por su ID numérico primario.")
    @GetMapping(value = "/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponseDTO> obtenerClientePorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(clienteService.obtenerClientePorId(id));
    }

    @Operation(summary = "Actualizar Cliente por ID", description = "Actualiza los datos personales y de domicilio del cliente. No permite modificar CURP ni RFC.")
    @PutMapping(value = "/{id:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @PathVariable("id") Long id,
            @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @Operation(summary = "Baja Lógica de Cliente", description = "Marca al cliente y sus cuentas como inactivos sin eliminar físicamente los registros.")
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable("id") Long id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar Cliente por CURP", description = "Obtiene los detalles del cliente filtrando por su clave CURP.")
    @GetMapping(value = {"/curp/{curp}", "/busquedas/curp/{curp}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponseDTO> buscarPorCurp(@PathVariable("curp") String curp) {
        return ResponseEntity.ok(clienteService.buscarPorCurp(curp));
    }

    @Operation(summary = "Buscar Cliente por RFC", description = "Obtiene los detalles del cliente filtrando por su clave RFC.")
    @GetMapping(value = {"/rfc/{rfc}", "/busquedas/rfc/{rfc}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponseDTO> buscarPorRfc(@PathVariable("rfc") String rfc) {
        return ResponseEntity.ok(clienteService.buscarPorRfc(rfc));
    }

    @Operation(summary = "Buscar Cliente por Correo", description = "Obtiene los detalles del cliente filtrando por su correo electrónico.")
    @GetMapping(value = {"/email/{email}", "/correo/{email}", "/busquedas/correo"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponseDTO> buscarPorCorreo(@PathVariable(value = "email", required = false) String pathEmail,
                                                             @RequestParam(value = "correo", required = false) String paramCorreo) {
        String correo = (pathEmail != null && !pathEmail.isBlank()) ? pathEmail : paramCorreo;
        return ResponseEntity.ok(clienteService.buscarPorCorreo(correo));
    }

    @Operation(summary = "Buscar Clientes por Rango de Fechas", description = "Obtiene la lista de clientes registrados dentro del rango de fechas especificado.")
    @GetMapping(value = "/busquedas/fecha-creacion", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponseDTO>> buscarPorFechaCreacion(
            @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(clienteService.buscarPorFechaCreacion(fechaInicio, fechaFin));
    }
}
