package com.minierp.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Criterios de aceptación de docs/specs/compras.md contra PostgreSQL real. */
class ComprasIT extends IntegracionBase {

    private static Map<String, Object> itemCompra(long productoId, int cantidad, String costo) {
        return Map.of("productoId", productoId, "cantidad", cantidad, "costoUnitario", new BigDecimal(costo));
    }

    private static Map<String, Object> compra(long proveedorId, String factura, Object... items) {
        return Map.of("proveedorId", proveedorId, "numeroFactura", factura, "items", List.of(items));
    }

    @Test
    void c1_compraValida_sumaStockActualizaCostoYRegistraKardex() {
        long p = crearProducto(5, "1800", "2500", "5");
        long proveedor = crearProveedor();
        String factura = unico("fac").toLowerCase();

        JsonNode c = post("/api/compras", compra(proveedor, factura, itemCompra(p, 10, "2000")), 201);

        assertEquals(factura.toUpperCase(), c.get("numeroFactura").asText());
        assertMonto("20000", c.get("subtotal"));
        assertMonto("1000", c.get("totalIva"));
        assertMonto("21000", c.get("total"));

        JsonNode producto = get("/api/productos/" + p, 200);
        assertEquals(15, producto.get("stock").asInt());
        assertMonto("2000", producto.get("precioCompra"));

        JsonNode k = kardex(p);
        JsonNode ultimo = k.get(k.size() - 1);
        assertEquals("ENTRADA", ultimo.get("tipo").asText());
        assertEquals("COMPRA", ultimo.get("concepto").asText());
        assertEquals(factura.toUpperCase(), ultimo.get("referencia").asText());
        assertEquals(5, ultimo.get("stockAnterior").asInt());
        assertEquals(15, ultimo.get("stockResultante").asInt());
    }

    @Test
    void c2_facturaRepetidaDelMismoProveedor_responde409SinSumarStock() {
        long p = crearProducto(0, "100", "200", "0");
        long proveedor = crearProveedor();
        String factura = unico("F");
        post("/api/compras", compra(proveedor, factura, itemCompra(p, 1, "100")), 201);

        post("/api/compras", compra(proveedor, factura.toLowerCase(), itemCompra(p, 1, "100")), 409);
        assertEquals(1, stock(p));

        // El mismo número con otro proveedor sí es válido
        post("/api/compras", compra(crearProveedor(), factura, itemCompra(p, 1, "100")), 201);
        assertEquals(2, stock(p));
    }

    @Test
    void c3_productoRepetidoEnLaCompra_responde422() {
        long p = crearProducto(0, "100", "200", "0");

        post("/api/compras", compra(crearProveedor(), unico("F"),
                itemCompra(p, 1, "100"), itemCompra(p, 2, "100")), 422);

        assertEquals(0, stock(p));
    }

    @Test
    void c4_compraAtomica_unProductoInexistenteRevierteTodo() {
        long p = crearProducto(3, "100", "200", "0");
        int movimientos = kardex(p).size();

        post("/api/compras", compra(crearProveedor(), unico("F"),
                itemCompra(p, 5, "100"), itemCompra(999_999, 1, "100")), 404);

        assertEquals(3, stock(p));
        assertEquals(movimientos, kardex(p).size());
    }

    @Test
    void c5_validacionDeLaPeticion() {
        long p = crearProducto(0, "100", "200", "0");
        long proveedor = crearProveedor();

        JsonNode e = post("/api/compras", Map.of("items", List.of()), 400).get("errores");
        assertTrue(e.has("proveedorId") && e.has("numeroFactura") && e.has("items"), e.toString());
        assertTrue(post("/api/compras", compra(proveedor, "F-1", itemCompra(p, 1, "1.005")), 400)
                .get("errores").has("items[0].costoUnitario"));
        assertTrue(post("/api/compras", compra(proveedor, "F-1", itemCompra(p, 0, "1")), 400)
                .get("errores").has("items[0].cantidad"));
        assertEquals(0, stock(p));
    }

    @Test
    void c6_proveedorInexistente_responde404() {
        long p = crearProducto(0, "100", "200", "0");

        post("/api/compras", compra(999_999, unico("F"), itemCompra(p, 1, "100")), 404);

        assertEquals(0, stock(p));
    }

    @Test
    void c7_detalleConservaElCostoYElIvaDelMomento() {
        long p = crearProducto(0, "100", "200", "19");
        long compraId = post("/api/compras", compra(crearProveedor(), unico("F"), itemCompra(p, 2, "150")), 201)
                .get("id").asLong();

        JsonNode detalle = get("/api/compras/" + compraId, 200).get("detalles").get(0);

        assertMonto("150", detalle.get("costoUnitario"));
        assertMonto("19", detalle.get("porcentajeIva"));
        assertMonto("300", detalle.get("subtotal"));
        assertMonto("57", detalle.get("valorIva"));
        get("/api/compras/999999", 404);
    }
}
