package com.minierp.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VentaDiariaResponse(LocalDate fecha, BigDecimal total, long cantidad) {
}
