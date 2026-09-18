package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(name = "gestoPagoProductClient", url = "${gestopago.products.url}")
public interface GestoPagoProductClient {
    // Consume el endpoint GET /sistema/service/getProductList.do del proveedor externo.
    //@param bearerToken Encabezado de autorización "Bearer <token>" inyectado desde la configuración o servicio.

    @GetMapping(
            value = "/sistema/service/getProductList.do",
            produces = MediaType.APPLICATION_XML_VALUE
    )
    GestoPagoProductXmlResponse getProductList(
            @RequestHeader("Authorization") String bearerToken
    );
}
