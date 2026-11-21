package com.minierp.exception;

/** HTTP 429: demasiados intentos fallidos de inicio de sesión. */
public class DemasiadosIntentosException extends RuntimeException {

    private final long segundosEspera;

    public DemasiadosIntentosException(long segundosEspera) {
        super("Demasiados intentos fallidos. Intenta de nuevo en " + Math.max(1, (segundosEspera + 59) / 60) + " minuto(s)");
        this.segundosEspera = segundosEspera;
    }

    public long getSegundosEspera() {
        return segundosEspera;
    }
}
