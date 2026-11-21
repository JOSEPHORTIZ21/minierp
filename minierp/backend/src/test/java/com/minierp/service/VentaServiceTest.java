package com.minierp.service;

import com.minierp.entity.DetalleVenta;
import com.minierp.entity.Producto;
import com.minierp.entity.Venta;
import com.minierp.entity.enums.ConceptoMovimiento;
import com.minierp.entity.enums.EstadoVenta;
import com.minierp.exception.RecursoNoEncontradoException;
import com.minierp.exception.ReglaNegocioException;
import com.minierp.repository.ProductoRepository;
import com.minierp.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Anulación de ventas: la venta se lee con bloqueo y solo se anula una vez. */
@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private VentaService ventaService;

    private Venta venta(EstadoVenta estado, Producto producto) {
        Venta v = Venta.builder().id(7L).numero("FV-000007").fecha(LocalDateTime.now()).estado(estado)
                .subtotal(BigDecimal.TEN).totalIva(BigDecimal.ZERO).total(BigDecimal.TEN).build();
        v.agregarDetalle(DetalleVenta.builder().producto(producto).cantidad(3)
                .precioUnitario(BigDecimal.ONE).porcentajeIva(BigDecimal.ZERO)
                .subtotal(BigDecimal.TEN).valorIva(BigDecimal.ZERO).total(BigDecimal.TEN).build());
        return v;
    }

    @Test
    void anular_bloqueaLaVentaYDevuelveElStock() {
        Producto producto = Producto.builder().id(1L).nombre("Arroz").stock(5).stockMinimo(0).build();
        Venta v = venta(EstadoVenta.COMPLETADA, producto);
        when(ventaRepository.findByIdParaActualizar(7L)).thenReturn(Optional.of(v));
        when(productoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(producto));

        ventaService.anular(7L);

        assertEquals(EstadoVenta.ANULADA, v.getEstado());
        verify(ventaRepository).findByIdParaActualizar(7L);
        verify(ventaRepository, never()).findById(anyLong());
        verify(inventarioService).registrarEntrada(producto, 3, ConceptoMovimiento.ANULACION_VENTA,
                "FV-000007", "Anulación de la venta");
    }

    @Test
    void anular_ventaYaAnulada_noDevuelveStock() {
        Producto producto = Producto.builder().id(1L).nombre("Arroz").stock(5).stockMinimo(0).build();
        when(ventaRepository.findByIdParaActualizar(7L)).thenReturn(Optional.of(venta(EstadoVenta.ANULADA, producto)));

        assertThrows(ReglaNegocioException.class, () -> ventaService.anular(7L));
        verifyNoInteractions(inventarioService, productoRepository);
    }

    @Test
    void anular_ventaInexistente_lanza404() {
        when(ventaRepository.findByIdParaActualizar(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> ventaService.anular(99L));
        verify(inventarioService, never()).registrarEntrada(any(), anyInt(), any(), any(), any());
    }
}
