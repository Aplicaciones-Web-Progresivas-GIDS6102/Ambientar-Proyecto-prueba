package com.proyecto.servicios.scheduler;

import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import com.proyecto.servicios.service.GestoPagoProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el programador Cron de 6:00 AM (GestoPagoProductoScheduler).
 */
@ExtendWith(MockitoExtension.class)
class GestoPagoProductoSchedulerTest {

    @Mock
    private GestoPagoProductoService gestoPagoProductoService;

    @InjectMocks
    private GestoPagoProductoScheduler cronScheduler;

    @Test
    @DisplayName("PRUEBA CRON 6 AM: Debe invocar el servicio de sincronización de productos al ejecutarse el Scheduler")
    void ejecutarSincronizacionDiaria6AM_InvocaServicio() {
        // ARRANGE
        ConsultaProductosResponse responseMock = ConsultaProductosResponse.builder()
                .codigo("01")
                .mensaje("Operación realizada con éxito")
                .productos(new ArrayList<>())
                .build();

        when(gestoPagoProductoService.sincronizarProductos()).thenReturn(responseMock);

        // ACT
        cronScheduler.ejecutarSincronizacionDiaria6AM();

        // ASSERT & VERIFY
        verify(gestoPagoProductoService, times(1)).sincronizarProductos();
    }
}
