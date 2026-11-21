package com.minierp.repository;

import com.minierp.dto.dashboard.TopProductoResponse;
import com.minierp.entity.DetalleVenta;
import com.minierp.entity.enums.EstadoVenta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    /** Productos más vendidos en un rango de fechas (el Pageable limita cuántos). */
    @Query("""
            select new com.minierp.dto.dashboard.TopProductoResponse(p.id, p.nombre, sum(d.cantidad), sum(d.total))
            from DetalleVenta d join d.producto p join d.venta v
            where v.estado = :estado and v.fecha between :desde and :hasta
            group by p.id, p.nombre
            order by sum(d.cantidad) desc
            """)
    List<TopProductoResponse> topProductos(@Param("estado") EstadoVenta estado,
                                           @Param("desde") LocalDateTime desde,
                                           @Param("hasta") LocalDateTime hasta,
                                           Pageable pageable);
}
