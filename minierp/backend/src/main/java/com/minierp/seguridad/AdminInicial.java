package com.minierp.seguridad;

import com.minierp.dto.usuario.UsuarioRequest;
import com.minierp.entity.enums.Rol;
import com.minierp.repository.UsuarioRepository;
import com.minierp.service.UsuarioService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Crea el primer administrador al arrancar, solo si la tabla de usuarios está vacía.
 * Los datos vienen de app.admin.email / app.admin.password (ADMIN_EMAIL / ADMIN_PASSWORD en producción),
 * así la contraseña nunca queda escrita en el código ni en la migración.
 * Después, ese administrador crea a los demás usuarios desde la pantalla de Usuarios.
 */
@Slf4j
@Component
@Order(0)
public class AdminInicial implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final Validator validator;
    private final String email;
    private final String password;

    public AdminInicial(UsuarioRepository usuarioRepository, UsuarioService usuarioService, Validator validator,
                        @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.validator = validator;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("No hay usuarios y no se definieron ADMIN_EMAIL / ADMIN_PASSWORD: nadie podrá iniciar sesión");
            return;
        }
        UsuarioRequest request = new UsuarioRequest("Administrador", email, password, Rol.ADMIN);
        Set<ConstraintViolation<UsuarioRequest>> errores = validator.validate(request);
        if (!errores.isEmpty()) {
            // Fallar al arrancar es mejor que crear un administrador con una contraseña débil
            throw new IllegalStateException("ADMIN_EMAIL / ADMIN_PASSWORD no son válidos: " + errores.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage()).collect(Collectors.joining("; ")));
        }
        usuarioService.crear(request);
        log.info("Administrador inicial creado: {}", request.email().trim().toLowerCase());
    }
}
