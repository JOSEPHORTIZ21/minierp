package com.minierp.controller;

import com.minierp.dto.auth.CambiarClaveRequest;
import com.minierp.dto.auth.LoginRequest;
import com.minierp.dto.auth.LoginResponse;
import com.minierp.dto.usuario.UsuarioResponse;
import com.minierp.service.AutenticacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    /** Público. Devuelve el token que el frontend envía después en Authorization: Bearer … */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacionService.login(request);
    }

    /** Datos del usuario de la sesión actual (rol vigente). */
    @GetMapping("/yo")
    public UsuarioResponse yo() {
        return autenticacionService.yo();
    }

    @PostMapping("/cambiar-clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarClave(@Valid @RequestBody CambiarClaveRequest request) {
        autenticacionService.cambiarClave(request);
    }
}
