package com.minierp.exception;

/** HTTP 409: duplicados o registros que no se pueden eliminar por estar en uso. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
