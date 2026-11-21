package com.minierp.seguridad;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Id del usuario que hace la petición (el "sub" del token, ya validado por Spring Security). */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No hay un usuario autenticado en esta petición");
        }
        return Long.valueOf(auth.getName());
    }
}
