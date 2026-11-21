package com.minierp.exception;

/** HTTP 422: la petición es válida en forma, pero viola una regla del negocio. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
