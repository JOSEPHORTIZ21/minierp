package com.minierp.dto.venta;

import com.minierp.entity.DetalleVenta;

import java.math.BigDecimal;

public record DetalleVentaResponse(
        Long id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal porcentajeIva,
        BigDecimal subtotal,
        BigDecimal valorIva,
        BigDecimal total
) {
    public static DetalleVentaResponse from(DetalleVenta d) {
        return new DetalleVentaResponse(d.getId(), d.getProducto().getId(), d.getProducto().getCodigo(),
                d.getProducto().getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getPorcentajeIva(),
                d.getSubtotal(), d.getValorIva(), d.getTotal());
    }
}
