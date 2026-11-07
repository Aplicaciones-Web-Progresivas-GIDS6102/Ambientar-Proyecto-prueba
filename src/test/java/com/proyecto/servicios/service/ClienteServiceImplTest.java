package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.ClienteBajaRequestDTO;
import com.proyecto.servicios.dto.ClienteFiltroRequestDTO;
import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.dto.DomicilioDTO;
import com.proyecto.servicios.entity.*;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.repositorys.*;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

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
    private PaisRepository paisRepository;

    @Mock
    private EstadoCivilRepository estadoCivilRepository;

    @Mock
    private EstatusCuentaRepository estatusCuentaRepository;

    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteRequestDTO requestDTO;
    private Cliente clienteMock;
    private Domicilio domicilioMock;
    private Cuenta cuentaMock;
    private Pais paisMock;
    private EstadoCivil estadoCivilMock;
    private EstatusCuenta estatusCuentaMock;

    @BeforeEach
    void setUp() {
        paisMock = Pais.builder().id(1L).nombre("MÉXICO").codigoIso("MEX").activo(true).build();
        estadoCivilMock = EstadoCivil.builder().id(1L).descripcion("SOLTERO").activo(true).build();
        estatusCuentaMock = EstatusCuenta.builder().id(1L).nombre("ACTIVA").descripcion("Cuenta Activa").activo(true).build();

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
                .sexo("M")
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
                .pais(paisMock)
                .build();

        clienteMock = Cliente.builder()
                .id(10L)
                .nombre("CARLOS")
                .apellidoPaterno("HERNANDEZ")
                .apellidoMaterno("GOMEZ")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .curp("HERC900515HMCRGR01")
                .rfc("HERC900515XX1")
                .sexo("M")
                .nacionalidad(paisMock)
                .estadoCivil(estadoCivilMock)
                .correo("carlos.hernandez@email.com")
                .telefonoMovil("5512345678")
                .activo(true)
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
                .estatusCuenta(estatusCuentaMock)
                .saldo(Saldo.builder().saldoDisponible(BigDecimal.ZERO).saldoContable(BigDecimal.ZERO).moneda("MXN").build())
                .build();
    }

    // ==========================================
    // 1. PRUEBAS DE REGISTRO DE USUARIOS
    // ==========================================

    @Test
    @DisplayName("Debe registrar exitosamente a un nuevo cliente y crear automáticamente su cuenta bancaria")
    void crearCliente_Exitoso() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoIgnoreCase(anyString())).thenReturn(false);
        when(paisRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.of(paisMock));
        when(estadoCivilRepository.findByDescripcionIgnoreCase(anyString())).thenReturn(Optional.of(estadoCivilMock));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteMock);
        when(cuentaService.crearCuentaParaCliente(any(Cliente.class))).thenReturn(cuentaMock);

        ClienteResponseDTO response = clienteService.crearCliente(requestDTO);

        assertNotNull(response);
        assertEquals("CARLOS", response.getNombre());
        assertEquals("HERC900515HMCRGR01", response.getCurp());
        assertEquals("HERC900515XX1", response.getRfc());
        assertTrue(response.getActivo());

        verify(clienteRepository, times(1)).save(any(Cliente.class));
        verify(cuentaService, times(1)).crearCuentaParaCliente(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe rechazar el registro si el cliente es menor de 18 años")
    void crearCliente_MenorDeEdad_LanzaExcepcion() {
        requestDTO.setFechaNacimiento(LocalDate.now().minusYears(17));

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> clienteService.crearCliente(requestDTO));
        assertTrue(ex.getMessage().contains("mayor de edad"));
        verify(clienteRepository, never()).save(any());
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
    @DisplayName("Debe lanzar ClienteYaExisteException si el correo ya existe (insensible a mayúsculas)")
    void crearCliente_CorreoDuplicado_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoIgnoreCase(requestDTO.getCorreo().toLowerCase())).thenReturn(true);

        assertThrows(ClienteYaExisteException.class, () -> clienteService.crearCliente(requestDTO));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Integridad transaccional: Excepción en creación de cuenta debe propagarse impidiendo onboarding incompleto")
    void crearCliente_FalloCuenta_PropagaExcepcion() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoIgnoreCase(anyString())).thenReturn(false);
        when(paisRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.of(paisMock));
        when(estadoCivilRepository.findByDescripcionIgnoreCase(anyString())).thenReturn(Optional.of(estadoCivilMock));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteMock);
        when(cuentaService.crearCuentaParaCliente(any(Cliente.class)))
                .thenThrow(new RuntimeException("Fallo en base de datos al crear saldo"));

        assertThrows(RuntimeException.class, () -> clienteService.crearCliente(requestDTO));
    }

    // ==========================================
    // 2. PRUEBAS DE CONSULTA POR FILTROS
    // ==========================================

    @Test
    @DisplayName("Debe consultar clientes con DTO de filtros combinables")
    void buscarClientes_ConFiltroDTO_Exitoso() {
        ClienteFiltroRequestDTO filtro = ClienteFiltroRequestDTO.builder()
                .nombre("CARLOS")
                .correo("carlos@email.com")
                .build();

        when(clienteRepository.findAll(any(Specification.class))).thenReturn(List.of(clienteMock));

        List<ClienteResponseDTO> resultados = clienteService.buscarClientes(filtro);

        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals("CARLOS", resultados.get(0).getNombre());
        verify(clienteRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Debe consultar clientes sin filtros devolviendo todos los activos")
    void buscarClientes_SinFiltros_Exitoso() {
        when(clienteRepository.findAll(any(Specification.class))).thenReturn(List.of(clienteMock));

        List<ClienteResponseDTO> resultados = clienteService.buscarClientes((ClienteFiltroRequestDTO) null);

        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        verify(clienteRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Debe ejecutar sobrecarga de búsqueda por (nombre, correo) delegando correctamente")
    void buscarClientes_SobrecargaNombreCorreo_Exitoso() {
        when(clienteRepository.findAll(any(Specification.class))).thenReturn(List.of(clienteMock));

        List<ClienteResponseDTO> resultados = clienteService.buscarClientes("CARLOS", "carlos@email.com");

        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals("CARLOS", resultados.get(0).getNombre());
        verify(clienteRepository, times(1)).findAll(any(Specification.class));
    }

    // ==========================================
    // 3. PRUEBAS DE BORRADO LÓGICO
    // ==========================================

    @Test
    @DisplayName("Debe realizar la baja lógica del cliente y desactivar sus cuentas asociadas sin eliminar el registro físico")
    void darDeBaja_ClienteActivo_Exitoso() {
        ClienteBajaRequestDTO request = ClienteBajaRequestDTO.builder()
                .id(10L)
                .motivo("Cancelación voluntaria")
                .build();

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cuentaRepository.findByClienteId(10L)).thenReturn(List.of(cuentaMock));
        when(estatusCuentaRepository.findByNombreIgnoreCase("INACTIVA"))
                .thenReturn(Optional.of(EstatusCuenta.builder().nombre("INACTIVA").build()));

        ClienteResponseDTO response = clienteService.darDeBaja(request);

        assertNotNull(response);
        assertFalse(response.getActivo());
        assertNotNull(response.getFechaBaja());

        ArgumentCaptor<Cliente> clienteCaptor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository, times(1)).save(clienteCaptor.capture());
        assertFalse(clienteCaptor.getValue().getActivo());
        assertNotNull(clienteCaptor.getValue().getFechaBaja());

        assertEquals("INACTIVA", cuentaMock.getEstatusCuenta().getNombre());
        verify(cuentaRepository, times(1)).save(cuentaMock);
        verify(clienteRepository, never()).delete(any(Cliente.class));
        verify(clienteRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Debe lanzar ClienteNoEncontradoException cuando el ID no existe en la base de datos")
    void darDeBaja_ClienteInexistente_Lanza404() {
        ClienteBajaRequestDTO request = ClienteBajaRequestDTO.builder().id(999L).build();
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.darDeBaja(request));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar ClienteYaEliminadoException cuando el cliente ya se encontraba dado de baja")
    void darDeBaja_ClienteYaEliminado_Lanza409() {
        clienteMock.setActivo(false);
        clienteMock.setFechaBaja(LocalDateTime.now().minusDays(1));

        ClienteBajaRequestDTO request = ClienteBajaRequestDTO.builder().id(10L).build();
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));

        assertThrows(ClienteYaEliminadoException.class, () -> clienteService.darDeBaja(request));
        verify(clienteRepository, never()).save(any());
    }
}
