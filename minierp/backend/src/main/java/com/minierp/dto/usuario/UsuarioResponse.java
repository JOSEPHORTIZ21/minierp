package com.minierp.dto.usuario;

import com.minierp.entity.Usuario;
import com.minierp.entity.enums.Rol;

import java.time.LocalDateTime;

/** Nunca incluye el hash de la contraseña. */
public record UsuarioResponse(Long id, String email, String nombre, Rol rol, Boolean activo, LocalDateTime creadoEn) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getNombre(), u.getRol(), u.getActivo(), u.getCreadoEn());
    }
}
