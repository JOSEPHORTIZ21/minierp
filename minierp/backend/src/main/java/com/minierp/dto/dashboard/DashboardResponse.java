package com.minierp.dto.dashboard;

import com.minierp.dto.producto.ProductoResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal ventasHoy,
        BigDecimal ventasMes,
        long numeroVentasMes,
        BigDecimal ticketPromedioMes,
        long productosStockBajo,
        BigDecimal valorInventario,
        List<VentaDiariaResponse> ventasUltimos7Dias,
        List<TopProductoResponse> topProductos,
        List<ProductoResponse> alertasStock
) {
}
