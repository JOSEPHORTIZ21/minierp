package com.minierp.exception;

public class StockInsuficienteException extends ReglaNegocioException {

    public StockInsuficienteException(String producto, int disponible, int solicitado) {
        super(String.format("Stock insuficiente para '%s': disponible %d, solicitado %d", producto, disponible, solicitado));
    }
}
