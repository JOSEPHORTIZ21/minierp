package com.minierp.dto.usuario;

import com.minierp.entity.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de un usuario (solo ADMIN). */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String email,

        @Clave
        String password,

        @NotNull(message = "El rol es obligatorio")
        Rol rol
) {
}
