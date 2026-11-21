package com.minierp.dto.venta;

import com.minierp.dto.Limites;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * El frontend solo envía qué productos y cuántos.
 * Los precios y el IVA los calcula el backend: nunca se confía en montos que vienen del cliente.
 */
public record VentaRequest(
        /** Opcional: si es nulo, la venta es a consumidor final. */
        Long clienteId,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        @Size(max = Limites.ITEMS_MAXIMOS, message = "Máximo " + Limites.ITEMS_MAXIMOS + " productos por venta")
        List<@Valid ItemVentaRequest> items
) {
}
