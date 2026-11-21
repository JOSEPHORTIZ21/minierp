package com.minierp.repository;

import com.minierp.entity.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Override
    @EntityGraph(attributePaths = "proveedor")
    Page<Compra> findAll(Pageable pageable);

    boolean existsByProveedorId(Long proveedorId);

    boolean existsByProveedorIdAndNumeroFacturaIgnoreCase(Long proveedorId, String numeroFactura);
}
