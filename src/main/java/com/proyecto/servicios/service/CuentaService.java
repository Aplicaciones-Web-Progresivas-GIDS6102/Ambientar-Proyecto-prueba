package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.CuentaResponseDTO;
import com.proyecto.servicios.dto.SaldoDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;

import java.util.List;

/**
 * Interfaz de servicio para operaciones de Cuentas Bancarias.
 */
public interface CuentaService {

    CuentaResponseDTO obtenerCuentaPorNumero(String numeroCuenta);

    SaldoDTO obtenerSaldo(String numeroCuenta);

    List<CuentaResponseDTO> obtenerCuentasDeCliente(Long clienteId);

    List<CuentaResponseDTO> obtenerCuentasActivas();

    Cuenta crearCuentaParaCliente(Cliente cliente);
}
