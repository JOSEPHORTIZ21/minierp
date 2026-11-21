package com.minierp.dto.dashboard;

import java.math.BigDecimal;

public record TopProductoResponse(Long productoId, String nombre, Long cantidadVendida, BigDecimal totalVendido) {
}
