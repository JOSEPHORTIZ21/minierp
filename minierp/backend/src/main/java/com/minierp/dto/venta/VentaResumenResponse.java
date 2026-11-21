package com.minierp.dto.venta;

import com.minierp.entity.Venta;
import com.minierp.entity.enums.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Versión liviana para listados (sin detalles). */
public record VentaResumenResponse(
        Long id,
        String numero,
        LocalDateTime fecha,
        String clienteNombre,
        BigDecimal subtotal,
        BigDecimal totalIva,
        BigDecimal total,
        EstadoVenta estado
) {
    public static VentaResumenResponse from(Venta v) {
        return new VentaResumenResponse(v.getId(), v.getNumero(), v.getFecha(),
                v.getCliente() == null ? VentaResponse.NOMBRE_CONSUMIDOR_FINAL : v.getCliente().getNombre(),
                v.getSubtotal(), v.getTotalIva(), v.getTotal(), v.getEstado());
    }
}
