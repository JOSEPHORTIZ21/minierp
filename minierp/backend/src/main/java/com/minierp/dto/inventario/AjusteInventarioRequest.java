package com.minierp.dto.inventario;

import com.minierp.entity.enums.TipoMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Ajuste manual: conteo físico, producto dañado, vencido, etc. */
public record AjusteInventarioRequest(
        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimiento tipo,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "Debe ser mayor a cero")
        Integer cantidad,

        @NotBlank(message = "Explica el motivo del ajuste")
        @Size(max = 255, message = "Máximo 255 caracteres")
        String observacion
) {
}
