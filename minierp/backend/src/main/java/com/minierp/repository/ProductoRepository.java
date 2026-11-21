package com.minierp.repository;

import com.minierp.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    boolean existsByCategoriaId(Long categoriaId);

    /** Trae la categoría en la misma consulta para evitar el problema N+1. */
    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findAll(Specification<Producto> spec, Pageable pageable);

    /**
     * Bloquea la fila (SELECT ... FOR UPDATE) mientras dura la transacción.
     * Evita que dos ventas simultáneas vendan el mismo stock dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdParaActualizar(@Param("id") Long id);

    @Query("select p from Producto p join fetch p.categoria " +
           "where p.activo = true and p.stock <= p.stockMinimo order by p.stock asc")
    List<Producto> findStockBajo();

    @Query("select count(p) from Producto p where p.activo = true and p.stock <= p.stockMinimo")
    long contarStockBajo();

    @Query("select coalesce(sum(p.stock * p.precioCompra), 0) from Producto p where p.activo = true")
    BigDecimal calcularValorInventario();
}
