package com.minierp.dto.proveedor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El NIT es obligatorio")
        @Size(max = 20, message = "Máximo 20 caracteres")
        String nit,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "Máximo 150 caracteres")
        String razonSocial,

        @Size(max = 20, message = "Máximo 20 caracteres")
        String telefono,

        @Email(message = "El correo no es válido")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String email,

        @Size(max = 200, message = "Máximo 200 caracteres")
        String direccion
) {
}
