package com.minierp.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Criterios de aceptación de docs/specs/catalogos.md contra PostgreSQL real. */
class CatalogosIT extends IntegracionBase {

    @Test
    void c1_crudDeCategoria() {
        String nombre = unico("Cat");
        long id = post("/api/categorias", Map.of("nombre", nombre), 201).get("id").asLong();

        assertEquals(nombre, get("/api/categorias/" + id, 200).get("nombre").asText());
        assertEquals(nombre + " 2", put("/api/categorias/" + id, Map.of("nombre", nombre + " 2"), 200)
                .get("nombre").asText());
        delete("/api/categorias/" + id, 204);

        get("/api/categorias/" + id, 404);
        put("/api/categorias/" + id, Map.of("nombre", "x"), 404);
        delete("/api/categorias/" + id, 404);
    }

    @Test
    void c2_unicosAlCrearYAlEditar() {
        String nombre = unico("Cat");
        long id = post("/api/categorias", Map.of("nombre", nombre), 201).get("id").asLong();
        post("/api/categorias", Map.of("nombre", nombre.toLowerCase()), 409);
        put("/api/categorias/" + id, Map.of("nombre", nombre, "descripcion", "misma"), 200);

        long cliente = crearCliente();
        String documento = get("/api/clientes/" + cliente, 200).get("documento").asText();
        post("/api/clientes", Map.of("documento", documento, "nombre", "Otro"), 409);
        long otroCliente = crearCliente();
        put("/api/clientes/" + otroCliente, Map.of("documento", documento, "nombre", "Otro"), 409);

        long proveedor = crearProveedor();
        String nit = get("/api/proveedores/" + proveedor, 200).get("nit").asText();
        post("/api/proveedores", Map.of("nit", nit, "razonSocial", "Otro"), 409);
    }

    @Test
    void c3_limpiezaDeTextos() {
        Map<String, Object> body = new HashMap<>();
        body.put("documento", "  " + unico("D").substring(0, 10) + "  ");
        body.put("nombre", "  Ana Pérez  ");
        body.put("telefono", "   ");
        body.put("email", "");
        body.put("direccion", null);

        JsonNode c = post("/api/clientes", body, 201);

        assertEquals("Ana Pérez", c.get("nombre").asText());
        assertFalse(c.get("documento").asText().startsWith(" "));
        assertTrue(c.get("telefono").isNull());
        assertTrue(c.get("email").isNull());
    }

    @Test
    void c4_noSeBorranRegistrosEnUso() {
        long categoria = post("/api/categorias", Map.of("nombre", unico("Cat")), 201).get("id").asLong();
        String codigo = unico("P");
        post("/api/productos", Map.of("codigo", codigo, "nombre", "x", "precioCompra", BigDecimal.ONE,
                "precioVenta", BigDecimal.TEN, "porcentajeIva", BigDecimal.ZERO, "stockInicial", 5,
                "stockMinimo", 0, "categoriaId", categoria), 201);
        delete("/api/categorias/" + categoria, 409);

        long producto = get("/api/productos?buscar=" + codigo, 200).get("content").get(0).get("id").asLong();
        long cliente = crearCliente();
        post("/api/ventas", Map.of("clienteId", cliente, "items", List.of(item(producto, 1))), 201);
        delete("/api/clientes/" + cliente, 409);

        long proveedor = crearProveedor();
        post("/api/compras", Map.of("proveedorId", proveedor, "numeroFactura", unico("F"),
                "items", List.of(Map.of("productoId", producto, "cantidad", 1, "costoUnitario", 1))), 201);
        delete("/api/proveedores/" + proveedor, 409);

        // Sin registros asociados sí se pueden borrar
        delete("/api/clientes/" + crearCliente(), 204);
        delete("/api/proveedores/" + crearProveedor(), 204);
    }

    @Test
    void c5_validacionDeCampos() {
        JsonNode e = post("/api/clientes", Map.of("documento", "", "nombre", "x".repeat(121),
                "email", "no-es-correo"), 400).get("errores");
        assertTrue(e.has("documento") && e.has("nombre") && e.has("email"), e.toString());

        assertTrue(post("/api/proveedores", Map.of("nit", "1"), 400).get("errores").has("razonSocial"));
        assertTrue(post("/api/categorias", Map.of("nombre", " "), 400).get("errores").has("nombre"));
    }

    @Test
    void c6_busquedaPorNombreODocumento() {
        String marca = unico("Busq");
        String documento = unico("9").substring(0, 10);
        post("/api/clientes", Map.of("documento", documento, "nombre", "Cliente " + marca), 201);

        assertEquals(1, get("/api/clientes?buscar=" + marca.toLowerCase(), 200).get("totalElements").asInt());
        assertEquals(1, get("/api/clientes?buscar=" + documento, 200).get("totalElements").asInt());

        String nit = unico("8").substring(0, 10);
        post("/api/proveedores", Map.of("nit", nit, "razonSocial", "Proveedor " + marca), 201);
        assertEquals(1, get("/api/proveedores?buscar=" + marca, 200).get("totalElements").asInt());
        assertEquals(1, get("/api/proveedores?buscar=" + nit, 200).get("totalElements").asInt());
    }

    @Test
    void c7_losErroresNoExponenDetallesInternos() {
        String nombre = unico("Cat");
        post("/api/categorias", Map.of("nombre", nombre), 201);

        for (JsonNode error : List.of(
                post("/api/categorias", Map.of("nombre", nombre), 409),
                get("/api/productos?sort=noExiste", 400),
                get("/api/no-existe", 404))) {
            String texto = error.toString().toLowerCase();
            assertFalse(texto.contains("minierp."), texto);
            assertFalse(texto.contains("sql"), texto);
            assertFalse(texto.contains("exception"), texto);
            assertFalse(error.has("trace"), texto);
        }
    }
}
