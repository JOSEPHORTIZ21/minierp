package com.minierp.dto.auth;

import com.minierp.dto.usuario.Clave;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarClaveRequest(
        @NotBlank(message = "Escribe tu contraseña actual")
        @Size(max = 72, message = "Máximo 72 caracteres")
        String actual,

        @Clave
        String nueva
) {
}
