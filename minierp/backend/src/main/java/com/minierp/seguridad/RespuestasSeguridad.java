package com.minierp.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minierp.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Los rechazos de Spring Security (401 y 403) ocurren antes de llegar a los controladores,
 * así que GlobalExceptionHandler no los ve. Aquí se responden con el mismo formato ErrorResponse.
 */
@Component
public class RespuestasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespuestasSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401: sin token, o token inválido, vencido o de un usuario desactivado. */
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        boolean traiaToken = req.getHeader(HttpHeaders.AUTHORIZATION) != null;
        res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(req, res, HttpStatus.UNAUTHORIZED, traiaToken
                ? "Tu sesión expiró o ya no es válida. Inicia sesión de nuevo"
                : "Debes iniciar sesión para continuar");
    }

    /** 403: usuario autenticado cuyo rol no permite la operación. */
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        escribir(req, res, HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción");
    }

    private void escribir(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String mensaje) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(res.getOutputStream(), new ErrorResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), mensaje, req.getRequestURI(), null));
    }
}
