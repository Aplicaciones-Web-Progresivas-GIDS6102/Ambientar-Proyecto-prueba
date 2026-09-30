package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.dto.DomicilioDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Domicilio;
import com.proyecto.servicios.entity.Saldo;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteRequestDTO requestDTO;
    private Cliente clienteMock;
    private Domicilio domicilioMock;
    private Cuenta cuentaMock;

    @BeforeEach
    void setUp() {
        DomicilioDTO domicilioDTO = DomicilioDTO.builder()
                .calle("AV. REFORMA")
                .numeroExterior("123")
                .colonia("CENTRO")
                .municipio("CUAUHTEMOC")
                .estado("CDMX")
                .codigoPostal("06000")
                .pais("MÉXICO")
                .build();

        requestDTO = ClienteRequestDTO.builder()
                .nombre("CARLOS")
                .apellidoPaterno("HERNANDEZ")
                .apellidoMaterno("GOMEZ")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .curp("HERC900515HMCRGR01")
                .rfc("HERC900515XX1")
                .sexo(Sexo.M)
                .correo("carlos.hernandez@email.com")
                .movil("5512345678")
                .ingresoMensual(new BigDecimal("25000.00"))
                .domicilio(domicilioDTO)
                .build();

        domicilioMock = Domicilio.builder()
                .id(1L)
                .calle("AV. REFORMA")
                .numeroExterior("123")
                .colonia("CENTRO")
                .municipio("CUAUHTEMOC")
                .estado("CDMX")
                .codigoPostal("06000")
                .pais("MÉXICO")
                .build();

        clienteMock = Cliente.builder()
                .id(10L)
                .nombre("CARLOS")
                .apellidoPaterno("HERNANDEZ")
                .apellidoMaterno("GOMEZ")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .curp("HERC900515HMCRGR01")
                .rfc("HERC900515XX1")
                .sexo(Sexo.M)
                .nacionalidad("MEXICANA")
                .correo("carlos.hernandez@email.com")
                .movil("5512345678")
                .ingresoMensual(new BigDecimal("25000.00"))
                .activo(true)
                .eliminado(false)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .domicilio(domicilioMock)
                .cuentas(new ArrayList<>())
                .build();
        domicilioMock.setCliente(clienteMock);

        cuentaMock = Cuenta.builder()
                .id(100L)
                .cliente(clienteMock)
                .numeroCuenta("4000123456")
                .clabe("012180400012345601")
                .tipoCuenta("DEBITO")
                .estado(EstadoCuenta.ACTIVA)
                .activo(true)
                .saldo(Saldo.builder().saldoDisponible(BigDecimal.ZERO).saldoContable(BigDecimal.ZERO).moneda("MXN").build())
                .build();
    }

    @Test
    @DisplayName("Debe registrar exitosamente a un nuevo cliente y crear automáticamente su cuenta bancaria")
    void crearCliente_Exitoso() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteMock);
        when(cuentaService.crearCuentaParaCliente(any(Cliente.class))).thenReturn(cuentaMock);

        ClienteResponseDTO response = clienteService.crearCliente(requestDTO);

        assertNotNull(response);
        assertEquals("CARLOS", response.getNombre());
        assertEquals("HERC900515HMCRGR01", response.getCurp());
        assertEquals("HERC900515XX1", response.getRfc());
        assertTrue(response.getActivo());
        assertFalse(response.getEliminado());

        verify(clienteRepository, times(1)).save(any(Cliente.class));
        verify(cuentaService, times(1)).crearCuentaParaCliente(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe lanzar CurpDuplicadaException si la CURP ya existe")
    void crearCliente_CurpDuplicada_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(requestDTO.getCurp())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> clienteService.crearCliente(requestDTO));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar RfcDuplicadoException si el RFC ya existe")
    void crearCliente_RfcDuplicado_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(requestDTO.getRfc())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> clienteService.crearCliente(requestDTO));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar ClienteYaExisteException si el correo ya existe")
    void crearCliente_CorreoDuplicado_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(requestDTO.getCorreo())).thenReturn(true);

        assertThrows(ClienteYaExisteException.class, () -> clienteService.crearCliente(requestDTO));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe obtener un cliente por ID exitosamente")
    void obtenerClientePorId_Exitoso() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));

        ClienteResponseDTO response = clienteService.obtenerClientePorId(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("CARLOS", response.getNombre());
    }

    @Test
    @DisplayName("Debe lanzar ClienteNoEncontradoException cuando el ID no existe")
    void obtenerClientePorId_Inexistente_LanzaExcepcion() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerClientePorId(99L));
    }

    @Test
    @DisplayName("Debe lanzar ValidacionNegocioException al intentar modificar la CURP en actualización")
    void actualizarCliente_IntentoModificarCurp_LanzaExcepcion() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));

        requestDTO.setCurp("MODIFICADA12345678");

        assertThrows(ValidacionNegocioException.class, () -> clienteService.actualizarCliente(10L, requestDTO));
    }

    @Test
    @DisplayName("Debe lanzar ValidacionNegocioException al intentar modificar el RFC en actualización")
    void actualizarCliente_IntentoModificarRfc_LanzaExcepcion() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));

        requestDTO.setRfc("MODIF1234567");

        assertThrows(ValidacionNegocioException.class, () -> clienteService.actualizarCliente(10L, requestDTO));
    }

    @Test
    @DisplayName("Debe realizar la baja lógica del cliente y desactivar sus cuentas asociadas")
    void eliminarCliente_BajaLogica_Exitoso() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));
        when(cuentaRepository.findByClienteId(10L)).thenReturn(List.of(cuentaMock));

        clienteService.eliminarCliente(10L);

        assertFalse(clienteMock.getActivo());
        assertTrue(clienteMock.getEliminado());
        assertNotNull(clienteMock.getFechaBaja());

        assertFalse(cuentaMock.getActivo());
        assertEquals(EstadoCuenta.INACTIVA, cuentaMock.getEstado());

        verify(clienteRepository, times(1)).save(clienteMock);
        verify(cuentaRepository, times(1)).save(cuentaMock);
    }

    @Test
    @DisplayName("Debe buscar clientes por rango de fechas de creación válido")
    void buscarPorFechaCreacion_RangoValido_Exitoso() {
        LocalDateTime inicio = LocalDateTime.now().minusDays(5);
        LocalDateTime fin = LocalDateTime.now();

        when(clienteRepository.findByActivoTrueAndFechaCreacionBetween(inicio, fin)).thenReturn(List.of(clienteMock));

        List<ClienteResponseDTO> resultados = clienteService.buscarPorFechaCreacion(inicio, fin);

        assertNotNull(resultados);
        assertEquals(1, resultados.size());
    }

    @Test
    @DisplayName("Debe lanzar ValidacionNegocioException si la fecha inicio es posterior a fecha fin")
    void buscarPorFechaCreacion_RangoInvalido_LanzaExcepcion() {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fin = LocalDateTime.now().minusDays(5);

        assertThrows(ValidacionNegocioException.class, () -> clienteService.buscarPorFechaCreacion(inicio, fin));
    }

    @Test
    @DisplayName("Integridad transaccional: Excepción en creación de cuenta debe propagarse impidiendo onboarding incompleto")
    void crearCliente_FalloCuenta_PropagaExcepcion() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteMock);
        when(cuentaService.crearCuentaParaCliente(any(Cliente.class)))
                .thenThrow(new RuntimeException("Fallo en base de datos al crear saldo"));

        assertThrows(RuntimeException.class, () -> clienteService.crearCliente(requestDTO));
    }
}
