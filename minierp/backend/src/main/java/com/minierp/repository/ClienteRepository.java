package com.minierp.repository;

import com.minierp.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, Long id);

    Page<Cliente> findByNombreContainingIgnoreCaseOrDocumentoContaining(String nombre, String documento, Pageable pageable);
}
