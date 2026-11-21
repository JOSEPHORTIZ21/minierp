package com.minierp.dto.compra;

import com.minierp.entity.DetalleCompra;

import java.math.BigDecimal;

public record DetalleCompraResponse(
        Long id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Integer cantidad,
        BigDecimal costoUnitario,
        BigDecimal porcentajeIva,
        BigDecimal subtotal,
        BigDecimal valorIva,
        BigDecimal total
) {
    public static DetalleCompraResponse from(DetalleCompra d) {
        return new DetalleCompraResponse(d.getId(), d.getProducto().getId(), d.getProducto().getCodigo(),
                d.getProducto().getNombre(), d.getCantidad(), d.getCostoUnitario(), d.getPorcentajeIva(),
                d.getSubtotal(), d.getValorIva(), d.getTotal());
    }
}
