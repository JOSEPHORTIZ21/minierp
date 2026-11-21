package com.minierp.controller;

import com.minierp.dto.PageResponse;
import com.minierp.dto.usuario.RestablecerClaveRequest;
import com.minierp.dto.usuario.UsuarioActualizarRequest;
import com.minierp.dto.usuario.UsuarioRequest;
import com.minierp.dto.usuario.UsuarioResponse;
import com.minierp.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Solo ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public PageResponse<UsuarioResponse> listar(@PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return usuarioService.listar(pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizarRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @PutMapping("/{id}/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecerClave(@PathVariable Long id, @Valid @RequestBody RestablecerClaveRequest request) {
        usuarioService.restablecerClave(id, request);
    }
}
