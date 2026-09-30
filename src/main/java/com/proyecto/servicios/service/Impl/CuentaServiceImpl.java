package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.CuentaResponseDTO;
import com.proyecto.servicios.dto.SaldoDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Saldo;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.CuentaMapper;
import com.proyecto.servicios.mapper.SaldoMapper;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.SaldoRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de Cuentas Bancarias.
 * Gestiona la creación de cuentas automáticas, generación de número de cuenta y CLABE,
 * así como consultas de saldos y cuentas activas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;

    private static final String BANCO_PREFIX = "4000";
    private final Random random = new Random();

    @Override
    @Transactional(readOnly = true)
    public CuentaResponseDTO obtenerCuentaPorNumero(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta bancaria N° '" + numeroCuenta + "' no existe."));
        return CuentaMapper.toDTO(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public SaldoDTO obtenerSaldo(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta bancaria N° '" + numeroCuenta + "' no existe."));
        Saldo saldo = saldoRepository.findByCuentaId(cuenta.getId())
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró saldo asociado a la cuenta '" + numeroCuenta + "'."));
        return SaldoMapper.toDTO(saldo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> obtenerCuentasDeCliente(Long clienteId) {
        return cuentaRepository.findByClienteId(clienteId).stream()
                .map(CuentaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> obtenerCuentasActivas() {
        return cuentaRepository.findByActivoTrue().stream()
                .map(CuentaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Cuenta crearCuentaParaCliente(Cliente cliente) {
        log.info("Generando cuenta bancaria automática para el cliente ID={}", cliente.getId());

        String numeroCuenta = generarNumeroCuentaUnico();
        String clabe = generarClabeUnica(numeroCuenta);

        Cuenta cuenta = Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .clabe(clabe)
                .tipoCuenta("DEBITO")
                .estado(EstadoCuenta.ACTIVA)
                .activo(true)
                .build();

        Saldo saldoInicial = Saldo.builder()
                .cuenta(cuenta)
                .saldoDisponible(BigDecimal.ZERO)
                .saldoContable(BigDecimal.ZERO)
                .moneda("MXN")
                .build();

        cuenta.setSaldo(saldoInicial);
        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        log.info("Cuenta bancaria N° {} creada exitosamente con CLABE {}", numeroCuenta, clabe);
        return cuentaGuardada;
    }

    private String generarNumeroCuentaUnico() {
        String numeroCuenta;
        int intentos = 0;
        do {
            long sufijo = 100000L + random.nextInt(900000);
            numeroCuenta = BANCO_PREFIX + sufijo;
            intentos++;
            if (intentos > 100) {
                throw new RuntimeException("No se pudo generar un número de cuenta único tras múltiples intentos.");
            }
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));
        return numeroCuenta;
    }

    private String generarClabeUnica(String numeroCuenta) {
        // Estructura CLABE 18 dígitos: 012 (Banco) + 180 (Plaza) + 10 dígitos (Cuenta) + 2 dígitos control
        String clabeBase = "012180" + numeroCuenta;
        String clabe = clabeBase + "01";
        int intentos = 0;
        while (cuentaRepository.existsByClabe(clabe)) {
            intentos++;
            clabe = clabeBase + String.format("%02d", intentos % 100);
        }
        return clabe;
    }
}
