package com.minierp.dto.venta;

import com.minierp.entity.Cliente;
import com.minierp.entity.Venta;
import com.minierp.entity.enums.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(
        Long id,
        String numero,
        LocalDateTime fecha,
        Long clienteId,
        String clienteNombre,
        String clienteDocumento,
        BigDecimal subtotal,
        BigDecimal totalIva,
        BigDecimal total,
        EstadoVenta estado,
        List<DetalleVentaResponse> detalles
) {
    /** Documento que usa la DIAN para ventas a consumidor final. */
    public static final String DOCUMENTO_CONSUMIDOR_FINAL = "222222222222";
    public static final String NOMBRE_CONSUMIDOR_FINAL = "Consumidor final";

    public static VentaResponse from(Venta v) {
        Cliente c = v.getCliente();
        return new VentaResponse(
                v.getId(), v.getNumero(), v.getFecha(),
                c == null ? null : c.getId(),
                c == null ? NOMBRE_CONSUMIDOR_FINAL : c.getNombre(),
                c == null ? DOCUMENTO_CONSUMIDOR_FINAL : c.getDocumento(),
                v.getSubtotal(), v.getTotalIva(), v.getTotal(), v.getEstado(),
                v.getDetalles().stream().map(DetalleVentaResponse::from).toList());
    }
}
