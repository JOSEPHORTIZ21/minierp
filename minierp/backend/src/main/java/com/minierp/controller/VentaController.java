package com.minierp.controller;

import com.minierp.dto.PageResponse;
import com.minierp.dto.venta.VentaRequest;
import com.minierp.dto.venta.VentaResponse;
import com.minierp.dto.venta.VentaResumenResponse;
import com.minierp.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    /** Ej: GET /api/ventas?desde=2026-11-01&hasta=2026-11-30 */
    @GetMapping
    public PageResponse<VentaResumenResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return ventaService.listar(desde, hasta, pageable);
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return ventaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse registrar(@Valid @RequestBody VentaRequest request) {
        return ventaService.registrar(request);
    }

    @PatchMapping("/{id}/anular")
    public VentaResponse anular(@PathVariable Long id) {
        return ventaService.anular(id);
    }
}
