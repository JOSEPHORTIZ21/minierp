package com.minierp.repository;

import com.minierp.entity.Venta;
import com.minierp.entity.enums.EstadoVenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Override
    @EntityGraph(attributePaths = "cliente")
    Page<Venta> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "cliente")
    Page<Venta> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    @Query("select coalesce(sum(v.total), 0) from Venta v " +
           "where v.estado = :estado and v.fecha between :desde and :hasta")
    BigDecimal sumarTotal(@Param("estado") EstadoVenta estado,
                          @Param("desde") LocalDateTime desde,
                          @Param("hasta") LocalDateTime hasta);

    long countByEstadoAndFechaBetween(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta);

    List<Venta> findByEstadoAndFechaBetween(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta);

    boolean existsByClienteId(Long clienteId);

    /** SELECT … FOR UPDATE: dos anulaciones simultáneas de la misma venta se atienden una tras otra. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> findByIdParaActualizar(@Param("id") Long id);
}
