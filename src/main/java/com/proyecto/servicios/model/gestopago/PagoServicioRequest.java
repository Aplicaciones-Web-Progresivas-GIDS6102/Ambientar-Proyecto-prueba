package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Request interno para el pago de un servicio GestoPago.
 * No contiene campos internos (UPC, token, estado): se generan en el backend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoServicioRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{10,20}$", message = "El número de cuenta debe contener entre 10 y 20 dígitos")
    private String numCuenta;

    @NotBlank(message = "El idServicio es obligatorio")
    @Pattern(regexp = "^\\d{1,10}$", message = "El idServicio debe ser numérico")
    private String idServicio;

    @NotBlank(message = "El idProducto es obligatorio")
    @Pattern(regexp = "^\\d{1,10}$", message = "El idProducto debe ser numérico")
    private String idProducto;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Digits(integer = 13, fraction = 2, message = "El monto admite máximo 13 enteros y 2 decimales")
    private BigDecimal monto;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener 10 dígitos")
    private String telefono;

    @Size(max = 50, message = "La referencia admite máximo 50 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "La referencia solo admite letras, números y guiones")
    private String referencia;

    @NotBlank(message = "El código de segunda autenticación es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código de segunda autenticación debe tener 6 dígitos")
    @ToString.Exclude
    private String codigo2FA;

    @JsonIgnore
    @AssertTrue(message = "Debe indicar teléfono o referencia")
    public boolean isTelefonoOReferenciaPresente() {
        return StringUtils.hasText(telefono) || StringUtils.hasText(referencia);
    }
}
