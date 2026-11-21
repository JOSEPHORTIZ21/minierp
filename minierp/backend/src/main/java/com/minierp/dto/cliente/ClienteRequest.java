package com.minierp.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 20, message = "Máximo 20 caracteres")
        String documento,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombre,

        @Size(max = 20, message = "Máximo 20 caracteres")
        String telefono,

        @Email(message = "El correo no es válido")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String email,

        @Size(max = 200, message = "Máximo 200 caracteres")
        String direccion
) {
}
