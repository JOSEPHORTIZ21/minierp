package com.minierp.dto.venta;

import com.minierp.dto.Limites;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record ItemVentaRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        @Max(value = Limites.CANTIDAD_MAXIMA, message = "La cantidad no puede superar " + Limites.CANTIDAD_MAXIMA)
        Integer cantidad
) {
}
