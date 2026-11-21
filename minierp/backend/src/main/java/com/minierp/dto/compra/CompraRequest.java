package com.minierp.dto.compra;

import com.minierp.dto.Limites;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompraRequest(
        @NotNull(message = "El proveedor es obligatorio")
        Long proveedorId,

        @NotBlank(message = "El número de factura es obligatorio")
        @Size(max = 40, message = "Máximo 40 caracteres")
        String numeroFactura,

        @NotEmpty(message = "La compra debe tener al menos un producto")
        @Size(max = Limites.ITEMS_MAXIMOS, message = "Máximo " + Limites.ITEMS_MAXIMOS + " productos por compra")
        List<@Valid ItemCompraRequest> items
) {
}
