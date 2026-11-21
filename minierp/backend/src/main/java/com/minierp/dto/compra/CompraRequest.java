package com.minierp.dto.compra;

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
        List<@Valid ItemCompraRequest> items
) {
}
