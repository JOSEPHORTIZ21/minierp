package com.minierp.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Criterios de aceptación de docs/specs/ventas.md contra PostgreSQL real. */
class VentasIT extends IntegracionBase {

    /** Peticiones HTTP reales (no MockMvc) para que la concurrencia pase por el servidor y su pool de hilos. */
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int puerto;

    private int enviar(String metodo, String ruta, String json) {
        try {
            HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + tokenAdmin())
                    .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(json));
            return HTTP.send(peticion.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void c1_ventaValida_calculaTotalesDescuentaStockYRegistraKardex() {
        long p = crearProducto(10, "500", "1000", "19");

        JsonNode venta = vender(p, 3);

        assertTrue(venta.get("numero").asText().matches("FV-\\d{6}"), venta.get("numero").asText());
        assertEquals("COMPLETADA", venta.get("estado").asText());
        assertMonto("3000", venta.get("subtotal"));
        assertMonto("570", venta.get("totalIva"));
        assertMonto("3570", venta.get("total"));
        assertEquals(7, stock(p));

        JsonNode ultimo = ultimoMovimiento(p);
        assertEquals("SALIDA", ultimo.get("tipo").asText());
        assertEquals("VENTA", ultimo.get("concepto").asText());
        assertEquals(10, ultimo.get("stockAnterior").asInt());
        assertEquals(7, ultimo.get("stockResultante").asInt());
        assertEquals(venta.get("numero").asText(), ultimo.get("referencia").asText());
    }

    @Test
    void c2_preciosEnviadosPorElClienteSeIgnoran() {
        long p = crearProducto(5, "500", "1000", "19");

        JsonNode venta = post("/api/ventas", Map.of("items", List.of(
                Map.of("productoId", p, "cantidad", 1, "precioUnitario", 1, "total", 1))), 201);

        assertMonto("1000", venta.get("detalles").get(0).get("precioUnitario"));
        assertMonto("1190", venta.get("total"));
    }

    @Test
    void c3_ventaAtomica_siUnProductoNoTieneStockNoSeDescuentaNada() {
        long a = crearProducto(10, "500", "1000", "0");
        long b = crearProducto(1, "500", "1000", "0");
        long ventasAntes = totalVentas();
        int kardexA = kardex(a).size();

        JsonNode error = post("/api/ventas", venta(item(a, 2), item(b, 5)), 422);

        assertTrue(error.get("mensaje").asText().contains("Stock insuficiente"), error.toString());
        assertEquals(10, stock(a));
        assertEquals(1, stock(b));
        assertEquals(kardexA, kardex(a).size());
        assertEquals(ventasAntes, totalVentas(), "la venta fallida no debe quedar guardada");
    }

    @Test
    void c4_productosRepetidosSeSumanEnUnSoloRenglon() {
        long p = crearProducto(10, "500", "1000", "0");

        JsonNode venta = post("/api/ventas", venta(item(p, 2), item(p, 3)), 201);

        assertEquals(1, venta.get("detalles").size());
        assertEquals(5, venta.get("detalles").get(0).get("cantidad").asInt());
        assertEquals(5, stock(p));
    }

    @Test
    void c5_sinClienteEsConsumidorFinal() {
        long p = crearProducto(3, "500", "1000", "0");

        JsonNode venta = vender(p, 1);

        assertTrue(venta.get("clienteId").isNull());
        assertEquals("222222222222", venta.get("clienteDocumento").asText());
    }

    @Test
    void ventaConCliente_quedaAsociada() {
        long p = crearProducto(3, "500", "1000", "0");
        long cliente = crearCliente();

        JsonNode venta = post("/api/ventas", Map.of("clienteId", cliente, "items", List.of(item(p, 1))), 201);

        assertEquals(cliente, venta.get("clienteId").asLong());
    }

    @Test
    void c6_productoInactivoNoSeVende() {
        long p = crearProducto(5, "500", "1000", "0");
        delete("/api/productos/" + p, 204);

        post("/api/ventas", venta(item(p, 1)), 422);

        assertEquals(5, stock(p));
    }

    @Test
    void c7_clienteOProductoInexistenteResponde404() {
        long p = crearProducto(5, "500", "1000", "0");

        post("/api/ventas", venta(item(999_999, 1)), 404);
        post("/api/ventas", Map.of("clienteId", 999_999, "items", List.of(item(p, 1))), 404);
        assertEquals(5, stock(p));
    }

    @Test
    void c8_validacionDeLaPeticion() {
        long p = crearProducto(5, "500", "1000", "0");

        assertTrue(post("/api/ventas", Map.of("items", List.of()), 400).get("errores").has("items"));
        assertTrue(post("/api/ventas", Map.of("items", Collections.nCopies(101, item(p, 1))), 400)
                .get("errores").has("items"));
        assertTrue(post("/api/ventas", venta(item(p, 0)), 400).get("errores").has("items[0].cantidad"));
        assertTrue(post("/api/ventas", venta(item(p, 100_001)), 400).get("errores").has("items[0].cantidad"));
        assertEquals(5, stock(p));
    }

    @Test
    void c9_ventasSimultaneasPorLaUltimaUnidad_soloUnaGana() throws Exception {
        long p = crearProducto(1, "500", "1000", "0");

        List<Integer> codigos = enParalelo(6, () ->
                enviar("POST", "/api/ventas", "{\"items\":[{\"productoId\":" + p + ",\"cantidad\":1}]}"));

        assertEquals(1, Collections.frequency(codigos, 201), "códigos: " + codigos);
        assertEquals(5, Collections.frequency(codigos, 422), "códigos: " + codigos);
        assertEquals(0, stock(p));
    }

    @Test
    void c10_anularDevuelveElStockConMovimientoDeAnulacion() {
        long p = crearProducto(10, "500", "1000", "0");
        long ventaId = vender(p, 3).get("id").asLong();

        JsonNode anulada = patch("/api/ventas/" + ventaId + "/anular", 200);

        assertEquals("ANULADA", anulada.get("estado").asText());
        assertEquals(10, stock(p));
        JsonNode ultimo = ultimoMovimiento(p);
        assertEquals("ENTRADA", ultimo.get("tipo").asText());
        assertEquals("ANULACION_VENTA", ultimo.get("concepto").asText());
        assertEquals(3, ultimo.get("cantidad").asInt());
    }

    @Test
    void c11_noSePuedeAnularDosVeces() {
        long p = crearProducto(10, "500", "1000", "0");
        long ventaId = vender(p, 3).get("id").asLong();
        patch("/api/ventas/" + ventaId + "/anular", 200);

        patch("/api/ventas/" + ventaId + "/anular", 422);

        assertEquals(10, stock(p));
    }

    @Test
    void c11_anulacionesSimultaneas_devuelvenElStockUnaSolaVez() throws Exception {
        long p = crearProducto(10, "500", "1000", "0");
        long ventaId = vender(p, 3).get("id").asLong();

        List<Integer> codigos = enParalelo(4, () -> enviar("PATCH", "/api/ventas/" + ventaId + "/anular", null));

        assertEquals(1, Collections.frequency(codigos, 200), "códigos: " + codigos);
        assertEquals(3, Collections.frequency(codigos, 422), "códigos: " + codigos);
        assertEquals(10, stock(p), "el stock debe volver exactamente a 10, no más");
    }

    @Test
    void c12_ventasAnuladasNoSumanEnElDashboard() {
        JsonNode antes = get("/api/dashboard", 200);
        long p = crearProducto(10, "500", "1000", "0");
        long ventaId = vender(p, 2).get("id").asLong();
        patch("/api/ventas/" + ventaId + "/anular", 200);

        JsonNode despues = get("/api/dashboard", 200);

        assertEquals(0, antes.get("ventasHoy").decimalValue().compareTo(despues.get("ventasHoy").decimalValue()));
        assertEquals(antes.get("numeroVentasMes").asLong(), despues.get("numeroVentasMes").asLong());
    }

    @Test
    void c13_listadoFiltraPorRangoDeFechasYOrdenaDeLaMasReciente() {
        long p = crearProducto(5, "500", "1000", "0");
        String numero = vender(p, 1).get("numero").asText();
        LocalDate hoy = LocalDate.now();

        JsonNode deHoy = get("/api/ventas?desde=" + hoy + "&hasta=" + hoy + "&size=100", 200).get("content");
        assertTrue(contieneNumero(deHoy, numero));
        assertEquals(numero, get("/api/ventas?size=1", 200).get("content").get(0).get("numero").asText(),
                "la venta más reciente va primero");

        JsonNode deManana = get("/api/ventas?desde=" + hoy.plusDays(1), 200).get("content");
        assertFalse(contieneNumero(deManana, numero));
    }

    // ------------------------------------------------------------------

    private JsonNode ultimoMovimiento(long productoId) {
        JsonNode k = kardex(productoId);
        return k.get(k.size() - 1);
    }

    private long totalVentas() {
        return get("/api/ventas?size=1", 200).get("totalElements").asLong();
    }

    private static boolean contieneNumero(JsonNode ventas, String numero) {
        for (JsonNode v : ventas) {
            if (numero.equals(v.get("numero").asText())) {
                return true;
            }
        }
        return false;
    }

    /** Lanza la misma petición en N hilos que arrancan a la vez y devuelve los códigos HTTP. */
    private static List<Integer> enParalelo(int hilos, Supplier<Integer> peticion) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return peticion.get();
                }));
            }
            salida.countDown();
            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> f : futuros) {
                codigos.add(f.get(30, TimeUnit.SECONDS));
            }
            return codigos;
        } finally {
            pool.shutdownNow();
        }
    }
}
