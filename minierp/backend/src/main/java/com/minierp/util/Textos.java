package com.minierp.util;

public final class Textos {

    private Textos() {
    }

    /** Recorta espacios y convierte cadenas vacías en null, para no guardar "" en la base de datos. */
    public static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
