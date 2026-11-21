package com.minierp.service;

import com.minierp.dto.PageResponse;
import com.minierp.dto.venta.ItemVentaRequest;
import com.minierp.dto.venta.VentaRequest;
import com.minierp.dto.venta.VentaResponse;
import com.minierp.dto.venta.VentaResumenResponse;
import com.minierp.entity.DetalleVenta;
import com.minierp.entity.Producto;
import com.minierp.entity.Venta;
import com.minierp.entity.enums.ConceptoMovimiento;
import com.minierp.entity.enums.EstadoVenta;
import com.minierp.exception.RecursoNoEncontradoException;
import com.minierp.exception.ReglaNegocioException;
import com.minierp.repository.ProductoRepository;
import com.minierp.repository.VentaRepository;
import com.minierp.util.Montos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteService clienteService;
    private final InventarioService inventarioService;

    @Transactional(readOnly = true)
    public PageResponse<VentaResumenResponse> listar(LocalDate desde, LocalDate hasta, Pageable pageable) {
        Page<Venta> pagina;
        if (desde == null && hasta == null) {
            pagina = ventaRepository.findAll(pageable);
        } else {
            LocalDateTime inicio = (desde != null ? desde : LocalDate.of(2000, 1, 1)).atStartOfDay();
            LocalDateTime fin = (hasta != null ? hasta : LocalDate.now()).atTime(LocalTime.MAX);
            pagina = ventaRepository.findByFechaBetween(inicio, fin, pageable);
        }
        return PageResponse.of(pagina.map(VentaResumenResponse::from));
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id) {
        return VentaResponse.from(buscar(id));
    }

    /**
     * Registra una venta de forma atómica: si un solo producto no tiene stock,
     * la transacción se revierte completa y no se descuenta nada.
     */
    @Transactional
    public VentaResponse registrar(VentaRequest request) {
        // 1. Agrupar productos repetidos y ordenarlos por id.
        //    El orden fijo evita bloqueos mutuos (deadlocks) entre ventas simultáneas.
        Map<Long, Integer> cantidades = new TreeMap<>();
        for (ItemVentaRequest item : request.items()) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        // 2. Crear la cabecera para obtener el id y generar el consecutivo
        Venta venta = Venta.builder()
                .fecha(LocalDateTime.now())
                .cliente(request.clienteId() == null ? null : clienteService.buscar(request.clienteId()))
                .estado(EstadoVenta.COMPLETADA)
                .subtotal(BigDecimal.ZERO)
                .totalIva(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();
        venta = ventaRepository.save(venta);
        venta.setNumero(String.format("FV-%06d", venta.getId()));

        // 3. Procesar cada producto: bloquear fila, validar, descontar stock y calcular valores
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> item : cantidades.entrySet()) {
            Long productoId = item.getKey();
            int cantidad = item.getValue();

            Producto producto = productoRepository.findByIdParaActualizar(productoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("el producto", productoId));
            if (!producto.getActivo()) {
                throw new ReglaNegocioException("El producto '" + producto.getNombre() + "' está inactivo y no se puede vender");
            }

            inventarioService.registrarSalida(producto, cantidad, ConceptoMovimiento.VENTA, venta.getNumero(), null);

            BigDecimal sub = Montos.multiplicar(producto.getPrecioVenta(), cantidad);
            BigDecimal iva = Montos.calcularIva(sub, producto.getPorcentajeIva());
            venta.agregarDetalle(DetalleVenta.builder()
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(producto.getPrecioVenta())
                    .porcentajeIva(producto.getPorcentajeIva())
                    .subtotal(sub)
                    .valorIva(iva)
                    .total(sub.add(iva))
                    .build());

            subtotal = subtotal.add(sub);
            totalIva = totalIva.add(iva);
        }

        // 4. Totales
        venta.setSubtotal(subtotal);
        venta.setTotalIva(totalIva);
        venta.setTotal(subtotal.add(totalIva));
        ventaRepository.flush(); // fuerza los INSERT para que los detalles tengan id en la respuesta
        return VentaResponse.from(venta);
    }

    /** Anula la venta y devuelve las unidades al inventario. */
    @Transactional
    public VentaResponse anular(Long id) {
        // Bloquea la venta: sin esto, dos anulaciones simultáneas pasaban la validación de estado
        // y devolvían el stock dos veces.
        Venta venta = ventaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("la venta", id));
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ReglaNegocioException("La venta " + venta.getNumero() + " ya está anulada");
        }
        venta.getDetalles().stream()
                .sorted(Comparator.comparing(d -> d.getProducto().getId()))
                .forEach(detalle -> {
                    Producto producto = productoRepository.findByIdParaActualizar(detalle.getProducto().getId())
                            .orElseThrow(() -> new RecursoNoEncontradoException("el producto", detalle.getProducto().getId()));
                    inventarioService.registrarEntrada(producto, detalle.getCantidad(), ConceptoMovimiento.ANULACION_VENTA,
                            venta.getNumero(), "Anulación de la venta");
                });
        venta.setEstado(EstadoVenta.ANULADA);
        return VentaResponse.from(venta);
    }

    private Venta buscar(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("la venta", id));
    }
}
