package com.minierp.dto.usuario;

import com.minierp.entity.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edición de un usuario (solo ADMIN). El correo no se cambia: es la identidad del usuario. */
public record UsuarioActualizarRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombre,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @NotNull(message = "Indica si el usuario está activo")
        Boolean activo
) {
}
