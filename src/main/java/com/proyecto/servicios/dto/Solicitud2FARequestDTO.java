package com.proyecto.servicios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

import java.math.BigDecimal;

/**
 * DTO para solicitar el código OTP de segunda autenticación (2FA) para una operación de pago.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Solicitud2FARequestDTO {

    @NotBlank(message = "El número de cuenta es obligatorio.")
    @Size(min = 10, max = 20, message = "El número de cuenta debe tener entre 10 y 20 caracteres.")
    @Pattern(regexp = "^\\d+$", message = "El número de cuenta solo debe contener dígitos numéricos.")
    @Schema(description = "Número de cuenta origen", example = "4000123456")
    private String numCuenta;

    @NotBlank(message = "El idServicio es obligatorio.")
    @Schema(description = "Identificador del servicio GestoPago", example = "2284")
    private String idServicio;

    @NotBlank(message = "El idProducto es obligatorio.")
    @Schema(description = "Identificador del producto GestoPago", example = "14302")
    private String idProducto;

    @NotNull(message = "El monto es obligatorio.")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero.")
    @Digits(integer = 13, fraction = 2, message = "El monto admite máximo 13 enteros y 2 decimales.")
    @Schema(description = "Monto monetario de la operación", example = "100.00")
    private BigDecimal monto;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener 10 dígitos numéricos.")
    @Schema(description = "Teléfono de recarga (si aplica)", example = "5512345678")
    private String telefono;

    @Size(max = 50, message = "La referencia admite máximo 50 caracteres.")
    @Schema(description = "Referencia de pago del servicio", example = "REF-987654")
    private String referencia;
}
