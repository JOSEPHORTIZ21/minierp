package com.minierp.seguridad;

import com.minierp.entity.Usuario;
import com.minierp.repository.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * Convierte un token ya verificado (firma, expiración, emisor) en la identidad de la petición.
 * Consulta el usuario en cada petición: si fue desactivado o eliminado, el token deja de servir aunque
 * no haya expirado, y el rol que se aplica es el actual, no el que tenía al iniciar sesión.
 *
 * No es un @Component a propósito: Spring MVC registraría cualquier bean Converter como conversor de parámetros.
 */
class ConvertidorTokenUsuario implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    ConvertidorTokenUsuario(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long id;
        try {
            id = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new InvalidBearerTokenException("Token sin usuario válido");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .filter(Usuario::getActivo)
                .orElseThrow(() -> new InvalidBearerTokenException("El usuario no existe o está desactivado"));
        return new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())),
                usuario.getId().toString());
    }
}
