package com.minierp.integracion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base de las pruebas de integración: levanta la aplicación completa contra un PostgreSQL 16 real
 * en Docker, creado con las migraciones de supabase/migrations (las mismas de producción).
 * Hibernate corre con ddl-auto=validate, igual que el perfil prod: si una entidad no coincide
 * con la migración, el contexto no arranca y todas las pruebas fallan.
 *
 * El contenedor se crea una sola vez y lo comparten todas las clases *IT (patrón singleton).
 * Cada prueba crea sus propios productos con códigos únicos, así no depende del orden de ejecución.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.default_schema=minierp",
                "spring.jpa.show-sql=false",
                "spring.h2.console.enabled=false",
                "app.seed.enabled=false",
                // AdminInicial crea este administrador al arrancar (la tabla de usuarios empieza vacía)
                "app.admin.email=" + IntegracionBase.ADMIN_EMAIL,
                "app.admin.password=" + IntegracionBase.ADMIN_CLAVE
        })
@AutoConfigureMockMvc
public abstract class IntegracionBase {

    static final String ADMIN_EMAIL = "admin-it@minierp.test";
    static final String ADMIN_CLAVE = "AdminIT12345";

    /** Token del administrador de prueba; se obtiene una vez (el contexto y la base se comparten). */
    private static String tokenAdmin;

    /** Ruta relativa al módulo backend (directorio de trabajo de Maven). */
    private static final Path MIGRACIONES = Path.of("..", "..", "supabase", "migrations");

    static final PostgreSQLContainer<?> POSTGRES = crearContenedor();

    private static PostgreSQLContainer<?> crearContenedor() {
        PostgreSQLContainer<?> contenedor = new PostgreSQLContainer<>("postgres:16-alpine");
        // docker-entrypoint-initdb.d ejecuta los .sql en orden alfabético: igual que el nombre con fecha de cada migración
        try (Stream<Path> archivos = Files.list(MIGRACIONES)) {
            List<Path> sqls = archivos.filter(p -> p.toString().endsWith(".sql")).sorted().toList();
            if (sqls.isEmpty()) {
                throw new IllegalStateException("No hay migraciones en " + MIGRACIONES.toAbsolutePath());
            }
            for (Path sql : sqls) {
                contenedor.withCopyFileToContainer(MountableFile.forHostPath(sql),
                        "/docker-entrypoint-initdb.d/" + sql.getFileName());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        contenedor.start();
        return contenedor;
    }

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapperSpring;

    /** Lee los montos como BigDecimal para comparar sin errores de redondeo de double. */
    protected ObjectMapper json() {
        return objectMapperSpring.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    // ------------------------------------------------------------------
    // Llamadas HTTP
    // ------------------------------------------------------------------

    /** Ejecuta la petición como el administrador de prueba. */
    protected JsonNode llamar(HttpMethod metodo, String url, Object cuerpo, int statusEsperado) {
        return llamarComo(tokenAdmin(), metodo, url, cuerpo, statusEsperado);
    }

    /**
     * Ejecuta la petición con el token indicado (null = sin cabecera Authorization),
     * verifica el código HTTP y devuelve el cuerpo (o null si viene vacío).
     */
    protected JsonNode llamarComo(String token, HttpMethod metodo, String url, Object cuerpo, int statusEsperado) {
        try {
            MockHttpServletRequestBuilder peticion = MockMvcRequestBuilders.request(metodo, url);
            if (token != null) {
                peticion.header("Authorization", "Bearer " + token);
            }
            if (cuerpo != null) {
                String texto = cuerpo instanceof String s ? s : objectMapperSpring.writeValueAsString(cuerpo);
                peticion.contentType(MediaType.APPLICATION_JSON).content(texto);
            }
            MvcResult r = mvc.perform(peticion).andReturn();
            String respuesta = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertEquals(statusEsperado, r.getResponse().getStatus(),
                    () -> metodo + " " + url + " respondió " + r.getResponse().getStatus() + ": " + respuesta);
            return respuesta.isBlank() ? null : json().readTree(respuesta);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected JsonNode get(String url, int status) {
        return llamar(HttpMethod.GET, url, null, status);
    }

    protected JsonNode post(String url, Object cuerpo, int status) {
        return llamar(HttpMethod.POST, url, cuerpo, status);
    }

    protected JsonNode put(String url, Object cuerpo, int status) {
        return llamar(HttpMethod.PUT, url, cuerpo, status);
    }

    protected JsonNode patch(String url, int status) {
        return llamar(HttpMethod.PATCH, url, null, status);
    }

    protected JsonNode delete(String url, int status) {
        return llamar(HttpMethod.DELETE, url, null, status);
    }

    // ------------------------------------------------------------------
    // Sesiones
    // ------------------------------------------------------------------

    protected String tokenAdmin() {
        if (tokenAdmin == null) {
            tokenAdmin = iniciarSesion(ADMIN_EMAIL, ADMIN_CLAVE);
        }
        return tokenAdmin;
    }

    protected String iniciarSesion(String email, String clave) {
        return llamarComo(null, HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", clave), 200)
                .get("token").asText();
    }

    /** Crea (como admin) un usuario nuevo con el rol indicado y devuelve su token. */
    protected String nuevoUsuario(String rol) {
        String email = unico("u").toLowerCase() + "@minierp.test";
        post("/api/usuarios", Map.of("nombre", "Usuario " + rol, "email", email, "password", "Clave12345", "rol", rol), 201);
        return iniciarSesion(email, "Clave12345");
    }

    // ------------------------------------------------------------------
    // Datos de prueba
    // ------------------------------------------------------------------

    protected static String unico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    protected long categoriaId() {
        return get("/api/categorias", 200).get(0).get("id").asLong();
    }

    protected long crearProducto(int stockInicial, String precioCompra, String precioVenta, String iva) {
        return crearProducto(unico("IT"), stockInicial, 0, precioCompra, precioVenta, iva);
    }

    protected long crearProducto(String codigo, int stockInicial, int stockMinimo,
                                 String precioCompra, String precioVenta, String iva) {
        Map<String, Object> body = Map.of(
                "codigo", codigo,
                "nombre", "Producto " + codigo,
                "precioCompra", new BigDecimal(precioCompra),
                "precioVenta", new BigDecimal(precioVenta),
                "porcentajeIva", new BigDecimal(iva),
                "stockInicial", stockInicial,
                "stockMinimo", stockMinimo,
                "categoriaId", categoriaId());
        return post("/api/productos", body, 201).get("id").asLong();
    }

    protected int stock(long productoId) {
        return get("/api/productos/" + productoId, 200).get("stock").asInt();
    }

    protected JsonNode kardex(long productoId) {
        return get("/api/productos/" + productoId + "/kardex", 200);
    }

    protected long crearCliente() {
        return post("/api/clientes", Map.of("documento", unico("C").substring(0, 10), "nombre", "Cliente de prueba"), 201)
                .get("id").asLong();
    }

    protected long crearProveedor() {
        return post("/api/proveedores", Map.of("nit", unico("N").substring(0, 10), "razonSocial", "Proveedor de prueba"), 201)
                .get("id").asLong();
    }

    protected static Map<String, Object> item(long productoId, int cantidad) {
        return Map.of("productoId", productoId, "cantidad", cantidad);
    }

    protected static Map<String, Object> venta(Object... items) {
        return Map.of("items", List.of(items));
    }

    protected JsonNode vender(long productoId, int cantidad) {
        return post("/api/ventas", venta(item(productoId, cantidad)), 201);
    }

    protected static void assertMonto(String esperado, JsonNode valor) {
        assertEquals(0, new BigDecimal(esperado).compareTo(valor.decimalValue()),
                () -> "esperado " + esperado + " pero fue " + valor);
    }
}
