package com.minierp.dto.compra;

import com.minierp.dto.Limites;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ItemCompraRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        @Max(value = Limites.CANTIDAD_MAXIMA, message = "La cantidad no puede superar " + Limites.CANTIDAD_MAXIMA)
        Integer cantidad,

        @NotNull(message = "El costo unitario es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El costo debe ser mayor a cero")
        @Digits(integer = Limites.MONTO_ENTEROS, fraction = Limites.MONTO_DECIMALES, message = Limites.MENSAJE_MONTO)
        BigDecimal costoUnitario
) {
}
