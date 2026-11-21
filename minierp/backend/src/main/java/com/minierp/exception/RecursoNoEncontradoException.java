package com.minierp.exception;

/** HTTP 404 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Object id) {
        super("No se encontró " + recurso + " con id " + id);
    }
}
