package com.minierp.service;

import com.minierp.dto.dashboard.DashboardResponse;
import com.minierp.dto.dashboard.TopProductoResponse;
import com.minierp.dto.dashboard.VentaDiariaResponse;
import com.minierp.dto.producto.ProductoResponse;
import com.minierp.entity.Venta;
import com.minierp.entity.enums.EstadoVenta;
import com.minierp.repository.DetalleVentaRepository;
import com.minierp.repository.ProductoRepository;
import com.minierp.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public DashboardResponse obtenerResumen() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime finHoy = hoy.atTime(LocalTime.MAX);
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        EstadoVenta completada = EstadoVenta.COMPLETADA;

        BigDecimal ventasHoy = ventaRepository.sumarTotal(completada, hoy.atStartOfDay(), finHoy);
        BigDecimal ventasMes = ventaRepository.sumarTotal(completada, inicioMes, finHoy);
        long numeroVentasMes = ventaRepository.countByEstadoAndFechaBetween(completada, inicioMes, finHoy);
        BigDecimal ticketPromedio = numeroVentasMes == 0 ? BigDecimal.ZERO
                : ventasMes.divide(BigDecimal.valueOf(numeroVentasMes), 2, RoundingMode.HALF_UP);

        // Ventas de los últimos 7 días (incluye días sin ventas en cero, para que la gráfica no tenga huecos)
        LocalDate desde = hoy.minusDays(6);
        Map<LocalDate, List<Venta>> porDia = ventaRepository
                .findByEstadoAndFechaBetween(completada, desde.atStartOfDay(), finHoy).stream()
                .collect(Collectors.groupingBy(v -> v.getFecha().toLocalDate()));
        List<VentaDiariaResponse> ultimos7Dias = IntStream.rangeClosed(0, 6)
                .mapToObj(i -> desde.plusDays(i))
                .map(dia -> {
                    List<Venta> ventasDia = porDia.getOrDefault(dia, List.of());
                    BigDecimal total = ventasDia.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new VentaDiariaResponse(dia, total, ventasDia.size());
                })
                .toList();

        List<TopProductoResponse> top = detalleVentaRepository.topProductos(completada, inicioMes, finHoy, PageRequest.of(0, 5));
        List<ProductoResponse> alertas = productoRepository.findStockBajo().stream()
                .limit(5)
                .map(ProductoResponse::from)
                .toList();

        return new DashboardResponse(ventasHoy, ventasMes, numeroVentasMes, ticketPromedio,
                productoRepository.contarStockBajo(), productoRepository.calcularValorInventario(),
                ultimos7Dias, top, alertas);
    }
}
