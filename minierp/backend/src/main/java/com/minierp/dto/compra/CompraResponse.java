package com.minierp.dto.compra;

import com.minierp.entity.Compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResponse(
        Long id,
        String numeroFactura,
        LocalDateTime fecha,
        Long proveedorId,
        String proveedorNombre,
        String proveedorNit,
        BigDecimal subtotal,
        BigDecimal totalIva,
        BigDecimal total,
        List<DetalleCompraResponse> detalles
) {
    public static CompraResponse from(Compra c) {
        return new CompraResponse(c.getId(), c.getNumeroFactura(), c.getFecha(),
                c.getProveedor().getId(), c.getProveedor().getRazonSocial(), c.getProveedor().getNit(),
                c.getSubtotal(), c.getTotalIva(), c.getTotal(),
                c.getDetalles().stream().map(DetalleCompraResponse::from).toList());
    }
}
