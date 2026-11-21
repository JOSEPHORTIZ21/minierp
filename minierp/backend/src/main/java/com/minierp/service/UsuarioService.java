package com.minierp.service;

import com.minierp.dto.PageResponse;
import com.minierp.dto.usuario.RestablecerClaveRequest;
import com.minierp.dto.usuario.UsuarioActualizarRequest;
import com.minierp.dto.usuario.UsuarioRequest;
import com.minierp.dto.usuario.UsuarioResponse;
import com.minierp.entity.Usuario;
import com.minierp.entity.enums.Rol;
import com.minierp.exception.ConflictoException;
import com.minierp.exception.RecursoNoEncontradoException;
import com.minierp.exception.ReglaNegocioException;
import com.minierp.repository.UsuarioRepository;
import com.minierp.seguridad.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Gestión de usuarios (solo ADMIN). Los usuarios no se borran: se desactivan. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(Pageable pageable) {
        return PageResponse.of(usuarioRepository.findAll(pageable).map(UsuarioResponse::from));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String email = AutenticacionService.normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe un usuario con el correo " + email);
        }
        Usuario usuario = Usuario.builder()
                .email(email)
                .nombre(request.nombre().trim())
                .claveHash(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .activo(true)
                .creadoEn(LocalDateTime.now())
                .build();
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Cambia nombre, rol y estado. Siempre debe quedar al menos un ADMIN activo, y un administrador
     * no puede quitarse a sí mismo el rol ni desactivarse (evita quedarse fuera del sistema).
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioActualizarRequest request) {
        Usuario usuario = buscar(id);
        boolean dejaDeSerAdminActivo = usuario.getRol() == Rol.ADMIN && usuario.getActivo()
                && (request.rol() != Rol.ADMIN || !request.activo());
        if (dejaDeSerAdminActivo) {
            if (usuario.getId().equals(UsuarioActual.id())) {
                throw new ReglaNegocioException("No puedes quitarte el rol de administrador ni desactivar tu propia cuenta");
            }
            // Bloquea a todos los administradores activos: dos cambios simultáneos se atienden en orden
            if (usuarioRepository.bloquearActivosConRol(Rol.ADMIN).size() <= 1) {
                throw new ReglaNegocioException("Debe quedar al menos un administrador activo");
            }
        }
        usuario.setNombre(request.nombre().trim());
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo());
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public void restablecerClave(Long id, RestablecerClaveRequest request) {
        buscar(id).setClaveHash(passwordEncoder.encode(request.password()));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el usuario", id));
    }
}
