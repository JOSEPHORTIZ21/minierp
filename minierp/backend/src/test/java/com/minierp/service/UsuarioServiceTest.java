package com.minierp.service;

import com.minierp.dto.usuario.UsuarioActualizarRequest;
import com.minierp.entity.Usuario;
import com.minierp.entity.enums.Rol;
import com.minierp.exception.ReglaNegocioException;
import com.minierp.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Regla "nunca sin administrador" (criterio 10 de docs/specs/autenticacion.md). */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private static Usuario usuario(long id, Rol rol) {
        return Usuario.builder().id(id).email("u" + id + "@minierp.test").nombre("U" + id)
                .claveHash("x").rol(rol).activo(true).creadoEn(LocalDateTime.now()).build();
    }

    /** La petición la hace el usuario con id 1. */
    @BeforeEach
    void sesion() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noSePuedeDesactivarAlUltimoAdministradorActivo() {
        Usuario otroAdmin = usuario(2, Rol.ADMIN);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otroAdmin));
        when(usuarioRepository.bloquearActivosConRol(Rol.ADMIN)).thenReturn(List.of(otroAdmin));

        assertThrows(ReglaNegocioException.class, () ->
                usuarioService.actualizar(2L, new UsuarioActualizarRequest("U2", Rol.ADMIN, false)));
        assertTrue(otroAdmin.getActivo());
        assertEquals(Rol.ADMIN, otroAdmin.getRol());
    }

    @Test
    void conDosAdministradores_sePuedeDegradarAUno() {
        Usuario otroAdmin = usuario(2, Rol.ADMIN);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otroAdmin));
        when(usuarioRepository.bloquearActivosConRol(Rol.ADMIN)).thenReturn(List.of(usuario(1, Rol.ADMIN), otroAdmin));

        usuarioService.actualizar(2L, new UsuarioActualizarRequest("U2", Rol.CAJERO, true));

        assertEquals(Rol.CAJERO, otroAdmin.getRol());
    }

    @Test
    void unAdministradorNoPuedeDegradarseASiMismo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1, Rol.ADMIN)));

        assertThrows(ReglaNegocioException.class, () ->
                usuarioService.actualizar(1L, new UsuarioActualizarRequest("U1", Rol.CAJERO, true)));
        verify(usuarioRepository, never()).bloquearActivosConRol(any());
    }

    @Test
    void cambiosQueNoAfectanAdministradores_noBloquean() {
        Usuario cajero = usuario(3, Rol.CAJERO);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(cajero));

        usuarioService.actualizar(3L, new UsuarioActualizarRequest("Nuevo nombre", Rol.CAJERO, false));

        assertFalse(cajero.getActivo());
        verify(usuarioRepository, never()).bloquearActivosConRol(any());
    }
}
