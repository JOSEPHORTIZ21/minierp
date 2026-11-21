package com.minierp.seguridad;

import com.minierp.dto.auth.LoginResponse;
import com.minierp.dto.usuario.UsuarioResponse;
import com.minierp.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration vigencia;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.vigencia-horas:8}") long vigenciaHoras) {
        this.encoder = encoder;
        this.vigencia = Duration.ofHours(vigenciaHoras);
    }

    /**
     * El token solo identifica al usuario (sub = id). El rol y si está activo se leen de la base en cada
     * petición (ConvertidorTokenUsuario), así un cambio de rol o una desactivación aplica de inmediato.
     * "nombre" y "rol" van como información para mostrar en el frontend, no para autorizar.
     */
    public LoginResponse emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(vigencia);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISOR)
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim("nombre", usuario.getNombre())
                .claim("rol", usuario.getRol().name())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new LoginResponse(token, expira, UsuarioResponse.from(usuario));
    }
}
