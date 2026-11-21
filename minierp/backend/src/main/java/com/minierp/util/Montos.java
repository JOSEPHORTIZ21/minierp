package com.minierp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculos de dinero. Siempre BigDecimal: double pierde precisión con valores monetarios. */
public final class Montos {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private Montos() {
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal multiplicar(BigDecimal valorUnitario, int cantidad) {
        return redondear(valorUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }

    public static BigDecimal calcularIva(BigDecimal base, BigDecimal porcentaje) {
        return redondear(base.multiply(porcentaje).divide(CIEN, 10, RoundingMode.HALF_UP));
    }
}
