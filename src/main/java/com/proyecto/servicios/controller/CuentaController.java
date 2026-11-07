package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.CuentaResponseDTO;
import com.proyecto.servicios.dto.SaldoDTO;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la consulta de Cuentas Bancarias y Saldos.
 */
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas Bancarias", description = "Endpoints para la gestión y consulta de cuentas bancarias y saldos")
public class CuentaController {

    private final CuentaService cuentaService;

    @Operation(summary = "Obtener Cuenta por Número", description = "Busca y retorna los detalles de la cuenta bancaria por su número de cuenta.")
    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponseDTO> obtenerCuentaPorNumero(@PathVariable("numeroCuenta") String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerCuentaPorNumero(numeroCuenta));
    }

    @Operation(summary = "Obtener Saldo de Cuenta", description = "Consulta el saldo disponible y contable de la cuenta bancaria especificada.")
    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaldoDTO> obtenerSaldo(@PathVariable("numeroCuenta") String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerSaldo(numeroCuenta));
    }

    @Operation(summary = "Obtener Cuentas Activas", description = "Lista todas las cuentas bancarias que se encuentran activas en el sistema.")
    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CuentaResponseDTO>> obtenerCuentasActivas() {
        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }

    @Operation(summary = "Obtener Cuentas de Cliente", description = "Obtiene la lista de cuentas pertenecientes al cliente especificado por su ID.")
    @GetMapping(value = "/cliente/{clienteId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CuentaResponseDTO>> obtenerCuentasDeCliente(@PathVariable("clienteId") Long clienteId) {
        return ResponseEntity.ok(cuentaService.obtenerCuentasDeCliente(clienteId));
    }
}
