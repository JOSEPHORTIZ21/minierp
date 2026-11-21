package com.minierp.dto.inventario;

import com.minierp.entity.MovimientoInventario;
import com.minierp.entity.enums.ConceptoMovimiento;
import com.minierp.entity.enums.TipoMovimiento;

import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        LocalDateTime fecha,
        TipoMovimiento tipo,
        ConceptoMovimiento concepto,
        Integer cantidad,
        Integer stockAnterior,
        Integer stockResultante,
        String referencia,
        String observacion
) {
    public static MovimientoResponse from(MovimientoInventario m) {
        return new MovimientoResponse(m.getId(), m.getFecha(), m.getTipo(), m.getConcepto(), m.getCantidad(),
                m.getStockAnterior(), m.getStockResultante(), m.getReferencia(), m.getObservacion());
    }
}
