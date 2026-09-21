package com.proyecto.servicios.scheduler;

import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import com.proyecto.servicios.service.GestoPagoProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


 // Programador de tareas (Cron) para la sincronización del catálogo de productos de GestoPago.
 // Automatiza la ejecución diaria a las 6:00 AM de la actualización del catálogo de productos
 // Utiliza la anotación `@Scheduled` de Spring con la expresión Cron "0 0 6 * * ?":
 // Si el proveedor responde con HTTP 200 OK, borra los registros antiguos en la BD PostgreSQL, inserta los nuevos y actualiza Redis.

@Component
@Slf4j
public class GestoPagoProductoScheduler {

    private final GestoPagoProductoService gestoPagoProductoService;

    public GestoPagoProductoScheduler(GestoPagoProductoService gestoPagoProductoService) {
        this.gestoPagoProductoService = gestoPagoProductoService;
    }

    // Invoca el proceso de sincronización automática con el proveedor externo.

    // Invoca `sincronizarProductos()`. Registra en logs el inicio, resultado y número de productos procesados.

    @Scheduled(cron = "${gestopago.cron.productos:0 0 6 * * ?}", zone = "America/Mexico_City")
    public void ejecutarSincronizacionDiaria6AM() {
        log.info("========== [CRON 6:00 AM] INICIANDO SINCRONIZACIÓN AUTOMÁTICA DIARIA DE PRODUCTOS GESTOPAGO ==========");

        try {
            ConsultaProductosResponse response = gestoPagoProductoService.sincronizarProductos();

            if ("01".equals(response.getCodigo())) {
                log.info("[CRON 6:00 AM] Sincronización diaria completada CON ÉXITO. Total productos sincronizados: {}",
                        response.getProductos() != null ? response.getProductos().size() : 0);
            } else {
                log.warn("[CRON 6:00 AM] Sincronización diaria finalizada con código de advertencia/falla: {} - Mensaje: {}",
                        response.getCodigo(), response.getMensaje());
            }

        } catch (Exception e) {
            log.error("[CRON 6:00 AM] Error crítico en la ejecución del Cron diario de productos: {}", e.getMessage(), e);
        }

        log.info("========== [CRON 6:00 AM] FIN DE SINCRONIZACIÓN AUTOMÁTICA DE PRODUCTOS ==========");
    }
}
