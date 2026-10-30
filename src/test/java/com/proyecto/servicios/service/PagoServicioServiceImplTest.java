package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.EstatusCuenta;
import com.proyecto.servicios.entity.Saldo;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioValidado;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoServicioServiceImplTest {

    private static final String USERNAME = "usuario1";
    private static final String CUENTA = "4000123456";

    @Mock private CuentaRepository cuentaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private GestoPagoProductoRepository productoRepository;
    @Mock private ObjectProvider<SegundoFactorService> segundoFactorProvider;
    @Mock private ObjectProvider<SentTxGateway> gatewayProvider;
    @Mock private SegundoFactorService segundoFactor;
    @Mock private SentTxGateway gateway;

    private PagoServicioServiceImpl service;
    private Cliente cliente;
    private Cuenta cuenta;
    private Usuario usuario;
    private GestoPagoProducto producto;

    @BeforeEach
    void setUp() {
        service = new PagoServicioServiceImpl(cuentaRepository, usuarioRepository, productoRepository,
                segundoFactorProvider, gatewayProvider);

        cliente = new Cliente();
        cliente.setId(10L);
        cliente.setActivo(true);

        cuenta = Cuenta.builder().id(1L).numeroCuenta(CUENTA).cliente(cliente)
                .estatusCuenta(EstatusCuenta.builder().nombre("ACTIVA").activo(true).build())
                .build();
        cuenta.setSaldo(Saldo.builder().cuenta(cuenta).saldoDisponible(new BigDecimal("500.00")).build());

        usuario = Usuario.builder().username(USERNAME).cliente(cliente).activo(true).bloqueado(false).build();

        producto = GestoPagoProducto.builder().idServicio("2284").idProducto("14302").precio(100.0).build();

        when(cuentaRepository.findByNumeroCuenta(CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(productoRepository.findFirstByIdServicioAndIdProducto("2284", "14302")).thenReturn(Optional.of(producto));
        when(segundoFactorProvider.getIfAvailable()).thenReturn(segundoFactor);
        when(gatewayProvider.getIfAvailable()).thenReturn(gateway);
        when(segundoFactor.validarYConsumir(anyLong(), anyString())).thenReturn(true);
        when(segundoFactor.validarYConsumir(anyLong(), any(), any(), any(), any(), anyString())).thenReturn(true);
        when(gateway.enviar(any())).thenReturn(PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.APLICADO).upc("UPC1").build());
    }

    private PagoServicioRequest request() {
        return PagoServicioRequest.builder().numCuenta(CUENTA).idServicio("2284").idProducto("14302")
                .monto(new BigDecimal("100.00")).telefono("5512345678").codigo2FA("907531").build();
    }

    @Test
    void pagoValido_validaTodoYEnviaAlProveedorSinCodigo2FA() {
        PagoServicioResponse r = service.pagarServicio(request(), USERNAME);

        assertThat(r.getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
        ArgumentCaptor<PagoServicioValidado> captor = ArgumentCaptor.forClass(PagoServicioValidado.class);
        verify(gateway).enviar(captor.capture());
        assertThat(captor.getValue().monto()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().toString()).doesNotContain("907531");
        // El 2FA se valida antes de llamar al proveedor
        var orden = inOrder(segundoFactor, gateway);
        orden.verify(segundoFactor).validarYConsumir(eq(10L), eq(1L), eq("2284"), eq("14302"), eq(new BigDecimal("100.00")), eq("907531"));
        orden.verify(gateway).enviar(any());
    }

    @Test
    void pagoValido_descuentaSaldo() {
        service.pagarServicio(request(), USERNAME);
        // Saldo inicial 500.00 - 100.00 = 400.00
        assertThat(cuenta.getSaldo().getSaldoDisponible()).isEqualByComparingTo("400.00");
    }

    @Test
    void pagoPendiente_noModificaSaldo() {
        when(gateway.enviar(any())).thenReturn(PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.PENDIENTE_CONFIRMACION).upc("UPC1").build());
        service.pagarServicio(request(), USERNAME);
        assertThat(cuenta.getSaldo().getSaldoDisponible()).isEqualByComparingTo("500.00");
    }

    @Test
    void pagoRechazado_noModificaSaldo() {
        when(gateway.enviar(any())).thenReturn(PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.RECHAZADO).upc("UPC1").build());
        service.pagarServicio(request(), USERNAME);
        assertThat(cuenta.getSaldo().getSaldoDisponible()).isEqualByComparingTo("500.00");
    }

    @Test
    void cuentaInexistente() {
        when(cuentaRepository.findByNumeroCuenta(CUENTA)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(CuentaNoEncontradaException.class);
        verifyNoInteractions(gateway, segundoFactor);
    }

    @Test
    void cuentaAjena() {
        Cliente otro = new Cliente();
        otro.setId(99L);
        usuario.setCliente(otro);
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(CuentaNoAutorizadaException.class);
        verifyNoInteractions(gateway, segundoFactor);
    }

    @Test
    void usuarioDesconocido() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pagarServicio(request(), "otro")).isInstanceOf(CuentaNoAutorizadaException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void usuarioBloqueado() {
        usuario.setBloqueado(true);
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(CuentaNoAutorizadaException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void clienteInactivo() {
        cliente.setActivo(false);
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(CuentaNoAutorizadaException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void cuentaBloqueada() {
        cuenta.getEstatusCuenta().setNombre("BLOQUEADA");
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(CuentaNoAutorizadaException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void servicioInexistente() {
        when(productoRepository.findFirstByIdServicioAndIdProducto(anyString(), anyString())).thenReturn(Optional.empty());
        when(productoRepository.existsByIdServicio("2284")).thenReturn(false);
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME))
                .isInstanceOf(ProductoNoValidoException.class).hasMessageContaining("servicio indicado no existe");
        verifyNoInteractions(gateway);
    }

    @Test
    void productoNoCorrespondeAlServicio() {
        when(productoRepository.findFirstByIdServicioAndIdProducto(anyString(), anyString())).thenReturn(Optional.empty());
        when(productoRepository.existsByIdServicio("2284")).thenReturn(true);
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME))
                .isInstanceOf(ProductoNoValidoException.class).hasMessageContaining("no corresponde");
        verifyNoInteractions(gateway);
    }

    @Test
    void montoDistintoAlPrecioFijo() {
        PagoServicioRequest r = request();
        r.setMonto(new BigDecimal("50.00"));
        assertThatThrownBy(() -> service.pagarServicio(r, USERNAME)).isInstanceOf(ProductoNoValidoException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void productoImporteVariable_aceptaMonto() {
        producto.setPrecio(0.0);
        PagoServicioRequest r = request();
        r.setMonto(new BigDecimal("237.50"));
        assertThat(service.pagarServicio(r, USERNAME).getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
    }

    @Test
    void saldoInsuficiente_noLlamaProveedorNi2FA() {
        cuenta.getSaldo().setSaldoDisponible(new BigDecimal("99.99"));
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(SaldoInsuficienteException.class);
        verifyNoInteractions(gateway, segundoFactor);
    }

    @Test
    void codigo2FAIncorrecto() {
        when(segundoFactor.validarYConsumir(anyLong(), anyString())).thenReturn(false);
        when(segundoFactor.validarYConsumir(anyLong(), any(), any(), any(), any(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(SegundoFactorInvalidoException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void sin2FAImplementado_responde503SinLlamarProveedor() {
        when(segundoFactorProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(IntegracionNoDisponibleException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void sinGateway_noConsumeCodigo2FA() {
        when(gatewayProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(IntegracionNoDisponibleException.class);
        verifyNoInteractions(segundoFactor);
    }

    @Test
    void resultadoPendiente_seDevuelveSinMarcarComoAplicado() {
        when(gateway.enviar(any())).thenReturn(PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.PENDIENTE_CONFIRMACION).upc("UPC1").build());
        assertThat(service.pagarServicio(request(), USERNAME).getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
        verify(gateway, times(1)).enviar(any());
    }

    @Test
    void respuestaVaciaDelProveedor_noSeReintenta() {
        when(gateway.enviar(any())).thenReturn(null);
        assertThatThrownBy(() -> service.pagarServicio(request(), USERNAME)).isInstanceOf(IntegracionNoDisponibleException.class);
        verify(gateway, times(1)).enviar(any());
    }
}
