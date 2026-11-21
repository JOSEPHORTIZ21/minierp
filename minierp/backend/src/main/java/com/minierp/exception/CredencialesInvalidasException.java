package com.minierp.exception;

/** HTTP 401 en el login. El mensaje es el mismo si el correo no existe o si la contraseña es incorrecta. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}
