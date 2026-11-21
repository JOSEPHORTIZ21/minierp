package com.minierp.service;

import com.minierp.dto.auth.CambiarClaveRequest;
import com.minierp.dto.auth.LoginRequest;
import com.minierp.dto.auth.LoginResponse;
import com.minierp.dto.usuario.UsuarioResponse;
import com.minierp.entity.Usuario;
import com.minierp.exception.CredencialesInvalidasException;
import com.minierp.exception.RecursoNoEncontradoException;
import com.minierp.exception.ReglaNegocioException;
import com.minierp.repository.UsuarioRepository;
import com.minierp.seguridad.IntentosLogin;
import com.minierp.seguridad.TokenService;
import com.minierp.seguridad.UsuarioActual;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final IntentosLogin intentosLogin;

    /** Hash de relleno: si el correo no existe se compara igual, para que la respuesta tarde lo mismo. */
    private final String hashDeRelleno;

    public AutenticacionService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                                TokenService tokenService, IntentosLogin intentosLogin) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.intentosLogin = intentosLogin;
        this.hashDeRelleno = passwordEncoder.encode("relleno-para-tiempo-constante");
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Sin @Transactional a propósito: BCrypt tarda ~100 ms y no debe retener una de las 5 conexiones del pool.
     * Misma respuesta (401) si el correo no existe, si la contraseña es incorrecta o si el usuario está desactivado.
     */
    public LoginResponse login(LoginRequest request) {
        String email = normalizarEmail(request.email());
        intentosLogin.verificar(email);

        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
        boolean claveCorrecta = passwordEncoder.matches(request.password(),
                usuario.map(Usuario::getClaveHash).orElse(hashDeRelleno));

        if (usuario.isEmpty() || !claveCorrecta || !usuario.get().getActivo()) {
            intentosLogin.registrarFallo(email);
            throw new CredencialesInvalidasException();
        }
        intentosLogin.registrarExito(email);
        return tokenService.emitir(usuario.get());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse yo() {
        return UsuarioResponse.from(buscarActual());
    }

    @Transactional
    public void cambiarClave(CambiarClaveRequest request) {
        Usuario usuario = buscarActual();
        if (!passwordEncoder.matches(request.actual(), usuario.getClaveHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        if (request.actual().equals(request.nueva())) {
            throw new ReglaNegocioException("La nueva contraseña debe ser distinta de la actual");
        }
        usuario.setClaveHash(passwordEncoder.encode(request.nueva()));
    }

    private Usuario buscarActual() {
        Long id = UsuarioActual.id();
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el usuario", id));
    }
}
