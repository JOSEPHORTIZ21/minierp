package com.minierp.dto.usuario;

/** El ADMIN asigna una contraseña nueva a otro usuario (por ejemplo, si la olvidó). */
public record RestablecerClaveRequest(
        @Clave
        String password
) {
}
