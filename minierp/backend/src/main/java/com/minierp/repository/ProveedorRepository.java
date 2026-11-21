package com.minierp.repository;

import com.minierp.entity.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    boolean existsByNit(String nit);

    boolean existsByNitAndIdNot(String nit, Long id);

    Page<Proveedor> findByRazonSocialContainingIgnoreCaseOrNitContaining(String razonSocial, String nit, Pageable pageable);
}
