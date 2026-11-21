package com.minierp.service;

import com.minierp.entity.MovimientoInventario;
import com.minierp.entity.Producto;
import com.minierp.entity.enums.ConceptoMovimiento;
import com.minierp.entity.enums.TipoMovimiento;
import com.minierp.exception.ReglaNegocioException;
import com.minierp.exception.StockInsuficienteException;
import com.minierp.repository.MovimientoInventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas unitarias: no necesitan base de datos ni levantar Spring. */
@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @InjectMocks
    private InventarioService inventarioService;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = Producto.builder().id(1L).codigo("ABA-001").nombre("Arroz").stock(10).stockMinimo(2).build();
    }

    @Test
    void registrarSalida_descuentaStockYGuardaMovimiento() {
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoInventario mov = inventarioService.registrarSalida(producto, 4, ConceptoMovimiento.VENTA, "FV-000001", null);

        assertEquals(6, producto.getStock());
        assertEquals(TipoMovimiento.SALIDA, mov.getTipo());
        assertEquals(10, mov.getStockAnterior());
        assertEquals(6, mov.getStockResultante());
        assertEquals("FV-000001", mov.getReferencia());
    }

    @Test
    void registrarSalida_sinStockSuficiente_lanzaExcepcionYNoModificaNada() {
        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.registrarSalida(producto, 11, ConceptoMovimiento.VENTA, "FV-000002", null));

        assertEquals(10, producto.getStock());
        verifyNoInteractions(movimientoRepository);
    }

    @Test
    void registrarEntrada_sumaStock() {
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoInventario mov = inventarioService.registrarEntrada(producto, 15, ConceptoMovimiento.COMPRA, "FC-123", null);

        assertEquals(25, producto.getStock());
        assertEquals(TipoMovimiento.ENTRADA, mov.getTipo());
    }

    @Test
    void cantidadCeroOMenor_noSePermite() {
        assertThrows(ReglaNegocioException.class,
                () -> inventarioService.registrarEntrada(producto, 0, ConceptoMovimiento.AJUSTE, "AJUSTE", "x"));
    }
}
