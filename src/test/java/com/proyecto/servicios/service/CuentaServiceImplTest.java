package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.CuentaResponseDTO;
import com.proyecto.servicios.dto.SaldoDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Saldo;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.SaldoRepository;
import com.proyecto.servicios.service.Impl.CuentaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private SaldoRepository saldoRepository;

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    private Cliente clienteMock;
    private Cuenta cuentaMock;
    private Saldo saldoMock;

    @BeforeEach
    void setUp() {
        clienteMock = Cliente.builder()
                .id(1L)
                .nombre("JUAN")
                .apellidoPaterno("PEREZ")
                .apellidoMaterno("LOPEZ")
                .build();

        saldoMock = Saldo.builder()
                .id(10L)
                .saldoDisponible(new BigDecimal("0.00"))
                .saldoContable(new BigDecimal("0.00"))
                .moneda("MXN")
                .build();

        cuentaMock = Cuenta.builder()
                .id(100L)
                .cliente(clienteMock)
                .numeroCuenta("4000123456")
                .clabe("012180400012345601")
                .tipoCuenta("DEBITO")
                .estado(EstadoCuenta.ACTIVA)
                .activo(true)
                .saldo(saldoMock)
                .build();
        saldoMock.setCuenta(cuentaMock);
    }

    @Test
    @DisplayName("Debe crear automáticamente una cuenta con estado ACTIVA y saldo inicial en cero")
    void crearCuentaParaCliente_Exitoso() {
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.existsByClabe(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cuenta cuentaCreada = cuentaService.crearCuentaParaCliente(clienteMock);

        assertNotNull(cuentaCreada);
        assertEquals(clienteMock, cuentaCreada.getCliente());
        assertEquals(EstadoCuenta.ACTIVA, cuentaCreada.getEstado());
        assertTrue(cuentaCreada.getActivo());
        assertEquals("DEBITO", cuentaCreada.getTipoCuenta());
        assertNotNull(cuentaCreada.getSaldo());
        assertEquals(0, BigDecimal.ZERO.compareTo(cuentaCreada.getSaldo().getSaldoDisponible()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cuentaCreada.getSaldo().getSaldoContable()));
        assertEquals("MXN", cuentaCreada.getSaldo().getMoneda());

        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
    }

    @Test
    @DisplayName("Debe buscar cuenta por número de cuenta de forma exitosa")
    void obtenerCuentaPorNumero_Exitoso() {
        when(cuentaRepository.findByNumeroCuenta("4000123456")).thenReturn(Optional.of(cuentaMock));

        CuentaResponseDTO response = cuentaService.obtenerCuentaPorNumero("4000123456");

        assertNotNull(response);
        assertEquals("4000123456", response.getNumeroCuenta());
        assertEquals("012180400012345601", response.getClabe());
        assertEquals(EstadoCuenta.ACTIVA, response.getEstado());
    }

    @Test
    @DisplayName("Debe lanzar CuentaNoEncontradaException si el número de cuenta no existe")
    void obtenerCuentaPorNumero_Inexistente_LanzaExcepcion() {
        when(cuentaRepository.findByNumeroCuenta("9999999999")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.obtenerCuentaPorNumero("9999999999"));
    }

    @Test
    @DisplayName("Debe obtener el saldo de una cuenta existente utilizando BigDecimal")
    void obtenerSaldo_Exitoso() {
        when(cuentaRepository.findByNumeroCuenta("4000123456")).thenReturn(Optional.of(cuentaMock));
        when(saldoRepository.findByCuentaId(100L)).thenReturn(Optional.of(saldoMock));

        SaldoDTO saldoDTO = cuentaService.obtenerSaldo("4000123456");

        assertNotNull(saldoDTO);
        assertEquals(0, BigDecimal.ZERO.compareTo(saldoDTO.getSaldoDisponible()));
        assertEquals(0, BigDecimal.ZERO.compareTo(saldoDTO.getSaldoContable()));
        assertEquals("MXN", saldoDTO.getMoneda());
    }

    @Test
    @DisplayName("Debe obtener las cuentas activas")
    void obtenerCuentasActivas_Exitoso() {
        when(cuentaRepository.findByActivoTrue()).thenReturn(List.of(cuentaMock));

        List<CuentaResponseDTO> resultado = cuentaService.obtenerCuentasActivas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getActivo());
    }
}
