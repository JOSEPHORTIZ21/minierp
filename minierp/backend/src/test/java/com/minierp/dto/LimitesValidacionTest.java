package com.minierp.dto;

import com.minierp.dto.compra.ItemCompraRequest;
import com.minierp.dto.producto.ProductoRequest;
import com.minierp.dto.venta.ItemVentaRequest;
import com.minierp.dto.venta.VentaRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Los topes de los DTO: la API es pública, así que ningún valor del cliente llega sin acotar. */
class LimitesValidacionTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<String> camposInvalidos(Object dto) {
        Set<ConstraintViolation<Object>> violaciones = validator.validate(dto);
        return violaciones.stream().map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void venta_conMasDe100Items_esInvalida() {
        List<ItemVentaRequest> items = Collections.nCopies(101, new ItemVentaRequest(1L, 1));
        assertTrue(camposInvalidos(new VentaRequest(null, items)).contains("items"));
    }

    @Test
    void venta_con100Items_esValida() {
        List<ItemVentaRequest> items = Collections.nCopies(100, new ItemVentaRequest(1L, 1));
        assertTrue(camposInvalidos(new VentaRequest(null, items)).isEmpty());
    }

    @Test
    void cantidad_porEncimaDelTope_esInvalida() {
        assertTrue(camposInvalidos(new ItemVentaRequest(1L, Integer.MAX_VALUE)).contains("cantidad"));
        assertTrue(camposInvalidos(new ItemVentaRequest(1L, 100_000)).isEmpty());
    }

    @Test
    void costo_conTresDecimales_esInvalido() {
        assertTrue(camposInvalidos(new ItemCompraRequest(1L, 1, new BigDecimal("1.005"))).contains("costoUnitario"));
        assertTrue(camposInvalidos(new ItemCompraRequest(1L, 1, new BigDecimal("1.05"))).isEmpty());
    }

    @Test
    void precio_queNoCabeEnNumeric14_2_esInvalido() {
        ProductoRequest request = new ProductoRequest("T-1", "Prueba", null, BigDecimal.ONE,
                new BigDecimal("1000000000000"), BigDecimal.ZERO, 0, 0, 1L);
        assertTrue(camposInvalidos(request).contains("precioVenta"));
    }
}
