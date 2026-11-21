package com.minierp.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Criterios de aceptación de docs/specs/productos-inventario.md contra PostgreSQL real. */
class ProductosInventarioIT extends IntegracionBase {

    private Map<String, Object> producto(String codigo, String compra, String venta, String iva, Integer stockInicial) {
        Map<String, Object> body = new HashMap<>();
        body.put("codigo", codigo);
        body.put("nombre", "Producto " + codigo);
        body.put("precioCompra", new BigDecimal(compra));
        body.put("precioVenta", new BigDecimal(venta));
        body.put("porcentajeIva", new BigDecimal(iva));
        body.put("stockInicial", stockInicial);
        body.put("stockMinimo", 2);
        body.put("categoriaId", categoriaId());
        return body;
    }

    private static Map<String, Object> ajuste(String tipo, int cantidad, String observacion) {
        Map<String, Object> body = new HashMap<>();
        body.put("tipo", tipo);
        body.put("cantidad", cantidad);
        body.put("observacion", observacion);
        return body;
    }

    @Test
    void c1_crearConStockInicial_guardaCodigoEnMayusculasYMovimientoInicial() {
        String codigo = unico("abc").toLowerCase();

        JsonNode p = post("/api/productos", producto(codigo, "100", "150", "19", 20), 201);

        assertEquals(codigo.toUpperCase(), p.get("codigo").asText());
        assertEquals(20, p.get("stock").asInt());
        JsonNode k = kardex(p.get("id").asLong());
        assertEquals(1, k.size());
        assertEquals("INVENTARIO_INICIAL", k.get(0).get("concepto").asText());
        assertEquals(0, k.get(0).get("stockAnterior").asInt());
        assertEquals(20, k.get(0).get("stockResultante").asInt());
    }

    @Test
    void c2_crearSinStockInicial_quedaEnCeroSinMovimientos() {
        JsonNode p = post("/api/productos", producto(unico("P"), "100", "150", "0", null), 201);

        assertEquals(0, p.get("stock").asInt());
        assertEquals(0, kardex(p.get("id").asLong()).size());
    }

    @Test
    void c3_codigoRepetido_responde409SinDistinguirMayusculas() {
        String codigo = unico("DUP");
        post("/api/productos", producto(codigo, "100", "150", "0", 0), 201);

        post("/api/productos", producto(codigo.toLowerCase(), "100", "150", "0", 0), 409);

        long otro = post("/api/productos", producto(unico("P"), "100", "150", "0", 0), 201).get("id").asLong();
        put("/api/productos/" + otro, producto(codigo, "100", "150", "0", 0), 409);
    }

    @Test
    void c4_reglasDePrecios() {
        post("/api/productos", producto(unico("P"), "200", "150", "0", 0), 422);
        assertTrue(post("/api/productos", producto(unico("P"), "100", "150.555", "0", 0), 400)
                .get("errores").has("precioVenta"));
        assertTrue(post("/api/productos", producto(unico("P"), "100", "150", "101", 0), 400)
                .get("errores").has("porcentajeIva"));
        assertTrue(post("/api/productos", producto(unico("P"), "0", "150", "0", 0), 400)
                .get("errores").has("precioCompra"));
    }

    @Test
    void c5_editarNoCambiaElStock() {
        String codigo = unico("P");
        long id = post("/api/productos", producto(codigo, "100", "150", "0", 7), 201).get("id").asLong();
        int movimientos = kardex(id).size();

        JsonNode editado = put("/api/productos/" + id, producto(codigo, "110", "160", "5", 999), 200);

        assertEquals(7, editado.get("stock").asInt());
        assertMonto("160", editado.get("precioVenta"));
        assertEquals(movimientos, kardex(id).size());
    }

    @Test
    void c6_borradoLogicoYReactivacion() {
        String codigo = unico("BORRAR");
        long id = crearProducto(codigo, 5, 0, "100", "150", "0");
        long ventaId = vender(id, 1).get("id").asLong();

        delete("/api/productos/" + id, 204);

        assertFalse(get("/api/productos/" + id, 200).get("activo").asBoolean());
        assertEquals(0, get("/api/productos?buscar=" + codigo, 200).get("totalElements").asInt());
        assertEquals(1, get("/api/productos?incluirInactivos=true&buscar=" + codigo, 200).get("totalElements").asInt());
        get("/api/ventas/" + ventaId, 200);

        assertTrue(patch("/api/productos/" + id + "/activar", 200).get("activo").asBoolean());
        assertEquals(1, get("/api/productos?buscar=" + codigo, 200).get("totalElements").asInt());
    }

    @Test
    void c7_filtrosDelListado() {
        String prefijo = unico("FIL");
        long bajo = crearProducto(prefijo + "-B", 1, 5, "100", "150", "0");
        long normal = crearProducto(prefijo + "-N", 10, 5, "100", "150", "0");

        JsonNode conStockBajo = get("/api/productos?stockBajo=true&buscar=" + prefijo, 200).get("content");
        assertEquals(1, conStockBajo.size());
        assertEquals(bajo, conStockBajo.get(0).get("id").asLong());
        assertTrue(conStockBajo.get(0).get("stockBajo").asBoolean());

        assertEquals(2, get("/api/productos?buscar=" + prefijo.toLowerCase(), 200).get("totalElements").asInt());
        long categoria = get("/api/productos/" + normal, 200).get("categoriaId").asLong();
        assertEquals(2, get("/api/productos?categoriaId=" + categoria + "&buscar=" + prefijo, 200)
                .get("totalElements").asInt());
    }

    @Test
    void c8_c9_c10_ajustesDeInventario() {
        long id = crearProducto(3, "100", "150", "0");

        JsonNode entrada = post("/api/productos/" + id + "/ajustes", ajuste("ENTRADA", 5, "Conteo físico"), 201);
        assertEquals("AJUSTE", entrada.get("concepto").asText());
        assertEquals(8, stock(id));

        post("/api/productos/" + id + "/ajustes", ajuste("SALIDA", 9, "Dañado"), 422);
        assertEquals(8, stock(id));

        assertTrue(post("/api/productos/" + id + "/ajustes", ajuste("SALIDA", 1, null), 400)
                .get("errores").has("observacion"));
        assertTrue(post("/api/productos/" + id + "/ajustes", ajuste("ENTRADA", 100_001, "x"), 400)
                .get("errores").has("cantidad"));
        post("/api/productos/999999/ajustes", ajuste("ENTRADA", 1, "x"), 404);
    }

    @Test
    void c11_kardexCoherenteConElStock() {
        long id = crearProducto(10, "100", "150", "0");
        vender(id, 3);
        post("/api/productos/" + id + "/ajustes", ajuste("ENTRADA", 4, "Conteo"), 201);
        long ventaId = vender(id, 2).get("id").asLong();
        patch("/api/ventas/" + ventaId + "/anular", 200);

        JsonNode k = kardex(id);
        for (JsonNode m : k) {
            int signo = "ENTRADA".equals(m.get("tipo").asText()) ? 1 : -1;
            assertEquals(m.get("stockAnterior").asInt() + signo * m.get("cantidad").asInt(),
                    m.get("stockResultante").asInt(), m.toString());
        }
        for (int i = 1; i < k.size(); i++) {
            assertEquals(k.get(i - 1).get("stockResultante").asInt(), k.get(i).get("stockAnterior").asInt());
        }
        assertEquals(k.get(k.size() - 1).get("stockResultante").asInt(), stock(id));
        assertEquals(11, stock(id));
    }

    @Test
    void c12_paginacionAcotadaYOrdenInvalido() {
        assertEquals(100, get("/api/productos?size=5000", 200).get("size").asInt());
        JsonNode error = get("/api/productos?sort=noExiste", 400);
        assertTrue(error.get("mensaje").asText().contains("noExiste"), error.toString());
    }

    @Test
    void productoInexistente_responde404() {
        get("/api/productos/999999", 404);
        get("/api/productos/999999/kardex", 404);
        delete("/api/productos/999999", 404);
    }
}
