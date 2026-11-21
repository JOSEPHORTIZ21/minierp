package com.minierp.controller;

import com.minierp.dto.PageResponse;
import com.minierp.dto.inventario.AjusteInventarioRequest;
import com.minierp.dto.inventario.MovimientoResponse;
import com.minierp.dto.producto.ProductoRequest;
import com.minierp.dto.producto.ProductoResponse;
import com.minierp.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    /** Ej: GET /api/productos?buscar=arroz&categoriaId=1&stockBajo=true&page=0&size=10&sort=nombre */
    @GetMapping
    public PageResponse<ProductoResponse> listar(@RequestParam(required = false) String buscar,
                                                 @RequestParam(required = false) Long categoriaId,
                                                 @RequestParam(defaultValue = "false") boolean stockBajo,
                                                 @RequestParam(defaultValue = "false") boolean incluirInactivos,
                                                 @PageableDefault(size = 10, sort = "nombre") Pageable pageable) {
        return productoService.listar(buscar, categoriaId, stockBajo, incluirInactivos, pageable);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    /** Desactiva (borrado lógico). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public ProductoResponse activar(@PathVariable Long id) {
        return productoService.activar(id);
    }

    @GetMapping("/{id}/kardex")
    public List<MovimientoResponse> kardex(@PathVariable Long id) {
        return productoService.kardex(id);
    }

    @PostMapping("/{id}/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse ajustar(@PathVariable Long id, @Valid @RequestBody AjusteInventarioRequest request) {
        return productoService.ajustarInventario(id, request);
    }
}
