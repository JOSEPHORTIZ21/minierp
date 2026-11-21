package com.minierp.dto.auth;

import com.minierp.dto.usuario.UsuarioResponse;

import java.time.Instant;

/** El frontend envía el token en cada petición: Authorization: Bearer {token}. */
public record LoginResponse(String token, Instant expiraEn, UsuarioResponse usuario) {
}
