package com.minierp.dto.producto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 30, message = "Máximo 30 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombre,

        @Size(max = 255, message = "Máximo 255 caracteres")
        String descripcion,

        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "Debe ser mayor a cero")
        BigDecimal precioCompra,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "Debe ser mayor a cero")
        BigDecimal precioVenta,

        @NotNull(message = "El porcentaje de IVA es obligatorio")
        @DecimalMin(value = "0.0", message = "No puede ser negativo")
        @DecimalMax(value = "100.0", message = "No puede superar 100")
        BigDecimal porcentajeIva,

        /** Solo se usa al crear. Al editar se ignora: el stock cambia con compras, ventas y ajustes. */
        @PositiveOrZero(message = "No puede ser negativo")
        Integer stockInicial,

        @NotNull(message = "El stock mínimo es obligatorio")
        @PositiveOrZero(message = "No puede ser negativo")
        Integer stockMinimo,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId
) {
}
