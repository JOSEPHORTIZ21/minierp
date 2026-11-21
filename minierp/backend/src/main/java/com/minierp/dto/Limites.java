package com.minierp.dto;

/**
 * Topes de validación compartidos por los DTO de entrada.
 * La API es pública (sin autenticación), así que cada valor que llega del cliente debe estar acotado.
 */
public final class Limites {

    /** Unidades por renglón o ajuste. Con 100 renglones, la suma cabe de sobra en un Integer. */
    public static final long CANTIDAD_MAXIMA = 100_000;

    /** Renglones por venta o compra. */
    public static final int ITEMS_MAXIMOS = 100;

    /** Montos en numeric(14,2): 12 enteros y 2 decimales, sin redondeos silenciosos. */
    public static final int MONTO_ENTEROS = 12;
    public static final int MONTO_DECIMALES = 2;
    public static final String MENSAJE_MONTO = "Máximo 12 dígitos enteros y 2 decimales";

    private Limites() {
    }
}
