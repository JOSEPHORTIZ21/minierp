package com.minierp.dto.compra;

import com.minierp.entity.Compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraResumenResponse(
        Long id,
        String numeroFactura,
        LocalDateTime fecha,
        String proveedorNombre,
        BigDecimal subtotal,
        BigDecimal totalIva,
        BigDecimal total
) {
    public static CompraResumenResponse from(Compra c) {
        return new CompraResumenResponse(c.getId(), c.getNumeroFactura(), c.getFecha(),
                c.getProveedor().getRazonSocial(), c.getSubtotal(), c.getTotalIva(), c.getTotal());
    }
}
