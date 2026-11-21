package com.minierp.dto.venta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * El frontend solo envía qué productos y cuántos.
 * Los precios y el IVA los calcula el backend: nunca se confía en montos que vienen del cliente.
 */
public record VentaRequest(
        /** Opcional: si es nulo, la venta es a consumidor final. */
        Long clienteId,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<@Valid ItemVentaRequest> items
) {
}
