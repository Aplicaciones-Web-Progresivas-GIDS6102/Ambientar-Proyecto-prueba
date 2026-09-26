package com.proyecto.servicios.scheduler;

import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import com.proyecto.servicios.service.GestoPagoProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Programador de tareas (Cron) para la sincronización del catálogo de productos de GestoPago a las 6:00 AM.
 */
@Component
@Slf4j
public class GestoPagoProductoScheduler {

    private final GestoPagoProductoService gestoPagoProductoService;

    public GestoPagoProductoScheduler(GestoPagoProductoService gestoPagoProductoService) {
        this.gestoPagoProductoService = gestoPagoProductoService;
    }

    @Scheduled(cron = "${gestopago.cron.productos:0 0 6 * * ?}", zone = "America/Mexico_City")
    public void ejecutarSincronizacionDiaria6AM() {
        log.info("========== [CRON 6:00 AM] INICIANDO SINCRONIZACIÓN AUTOMÁTICA DIARIA DE PRODUCTOS GESTOPAGO ==========");
        try {
            ConsultaProductosResponse response = gestoPagoProductoService.sincronizarProductos();
            log.info("[CRON 6:00 AM] Sincronización diaria completada. Código: {}, Productos: {}",
                    response.getCodigo(), response.getProductos() != null ? response.getProductos().size() : 0);
        } catch (Exception e) {
            log.error("[CRON 6:00 AM] Error en Cron diario: {}", e.getMessage(), e);
        }
        log.info("========== [CRON 6:00 AM] FIN DE SINCRONIZACIÓN AUTOMÁTICA DE PRODUCTOS ==========");
    }
}
