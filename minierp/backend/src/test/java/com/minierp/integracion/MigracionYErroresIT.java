package com.minierp.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La migración de Supabase es compatible con las entidades (el contexto arrancó con ddl-auto=validate),
 * deja RLS activo sin políticas y carga la semilla. Además, el formato común de errores de la API.
 */
class MigracionYErroresIT extends IntegracionBase {

    private static final List<String> TABLAS = List.of("categorias", "productos", "clientes", "proveedores",
            "ventas", "detalles_venta", "compras", "detalles_compra", "movimientos_inventario");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migracion_activaRlsEnTodasLasTablasSinPoliticas() {
        List<Map<String, Object>> filas = jdbc.queryForList(
                "select relname, relrowsecurity from pg_class " +
                "where relnamespace = 'minierp'::regnamespace and relkind = 'r'");
        assertEquals(TABLAS.size(), filas.size(), "tablas en el esquema minierp: " + filas);
        for (Map<String, Object> fila : filas) {
            assertTrue(TABLAS.contains((String) fila.get("relname")), "tabla inesperada " + fila.get("relname"));
            assertEquals(Boolean.TRUE, fila.get("relrowsecurity"), "RLS desactivado en " + fila.get("relname"));
        }
        Integer politicas = jdbc.queryForObject(
                "select count(*) from pg_policies where schemaname = 'minierp'", Integer.class);
        assertEquals(0, politicas, "no debe haber políticas: el acceso público queda denegado");
    }

    @Test
    void migracion_cargaLaSemillaConSuKardex() {
        JsonNode arroz = get("/api/productos?buscar=ABA-001", 200).get("content").get(0);
        assertEquals("Arroz blanco 500 g", arroz.get("nombre").asText());
        // 120 iniciales - 4 (FV-000001) - 2 (FV-000003)
        assertEquals(114, arroz.get("stock").asInt());

        JsonNode kardex = kardex(arroz.get("id").asLong());
        assertEquals("INVENTARIO_INICIAL", kardex.get(0).get("concepto").asText());
        assertEquals(kardex.get(kardex.size() - 1).get("stockResultante").asInt(), 114);
    }

    @Test
    void migracion_restriccionesDeLaBaseRespaldanLasReglas() {
        // Aunque un error en el servicio dejara pasar un stock negativo, la base lo rechaza (R2)
        Long id = jdbc.queryForObject("select id from minierp.productos where codigo = 'ABA-002'", Long.class);
        assertThrows(Exception.class, () -> jdbc.update("update minierp.productos set stock = -1 where id = ?", id));
    }

    @Test
    void rutaInexistente_responde404ConElFormatoComun() {
        JsonNode error = get("/api/no-existe", 404);
        assertEquals(404, error.get("status").asInt());
        assertEquals("/api/no-existe", error.get("ruta").asText());
        assertTrue(error.hasNonNull("mensaje"));
    }

    @Test
    void metodoNoSoportado_responde405() {
        llamar(HttpMethod.DELETE, "/api/ventas", null, 405);
    }

    @Test
    void jsonMalFormado_responde400() {
        JsonNode error = post("/api/categorias", "{ esto no es json", 400);
        assertEquals("El cuerpo de la petición no es un JSON válido", error.get("mensaje").asText());
    }

    @Test
    void parametroConTipoInvalido_responde400() {
        get("/api/productos/abc", 400);
        get("/api/ventas?desde=ayer", 400);
    }
}
