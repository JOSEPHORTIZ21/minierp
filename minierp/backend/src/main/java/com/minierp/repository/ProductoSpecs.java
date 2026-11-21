package com.minierp.repository;

import com.minierp.entity.Producto;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros dinámicos para el listado de productos.
 * Cada método devuelve null cuando el filtro no aplica, y Spring Data lo ignora.
 */
public final class ProductoSpecs {

    private ProductoSpecs() {
    }

    public static Specification<Producto> soloActivos(boolean incluirInactivos) {
        return incluirInactivos ? null : (root, query, cb) -> cb.isTrue(root.get("activo"));
    }

    public static Specification<Producto> busqueda(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String patron = "%" + texto.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nombre")), patron),
                cb.like(cb.lower(root.get("codigo")), patron));
    }

    public static Specification<Producto> deCategoria(Long categoriaId) {
        return categoriaId == null ? null
                : (root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    public static Specification<Producto> conStockBajo(boolean aplicar) {
        return !aplicar ? null
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.<Integer>get("stock"), root.<Integer>get("stockMinimo"));
    }
}
