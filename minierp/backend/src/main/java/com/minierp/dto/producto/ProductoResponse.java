package com.minierp.dto.producto;

import com.minierp.entity.Producto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        BigDecimal porcentajeIva,
        Integer stock,
        Integer stockMinimo,
        Boolean activo,
        boolean stockBajo,
        Long categoriaId,
        String categoriaNombre
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
                p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(),
                p.getPrecioCompra(), p.getPrecioVenta(), p.getPorcentajeIva(),
                p.getStock(), p.getStockMinimo(), p.getActivo(),
                p.getStock() <= p.getStockMinimo(),
                p.getCategoria().getId(), p.getCategoria().getNombre());
    }
}
