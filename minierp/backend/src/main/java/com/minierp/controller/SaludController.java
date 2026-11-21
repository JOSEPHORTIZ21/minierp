package com.minierp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Público y sin datos: lo usa el Health Check de Render (el resto de la API exige sesión). */
@RestController
public class SaludController {

    @GetMapping("/api/salud")
    public Map<String, String> salud() {
        return Map.of("estado", "ok");
    }
}
