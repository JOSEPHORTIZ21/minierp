package com.minierp.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Criterios de aceptación de docs/specs/autenticacion.md contra PostgreSQL real. */
class AutenticacionIT extends IntegracionBase {

    private static final String CLAVE = "Clave12345";

    @Autowired
    private JwtEncoder jwtEncoder;

    /** Crea un usuario y devuelve {id, email}. */
    private JsonNode crearUsuario(String rol) {
        String email = unico("a").toLowerCase() + "@minierp.test";
        return post("/api/usuarios", Map.of("nombre", "Prueba", "email", email, "password", CLAVE, "rol", rol), 201);
    }

    private JsonNode login(String email, String clave, int status) {
        return llamarComo(null, HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", clave), status);
    }

    private static Map<String, Object> actualizacion(String rol, boolean activo) {
        return Map.of("nombre", "Prueba", "rol", rol, "activo", activo);
    }

    @Test
    void c1_loginValido_devuelveTokenYUsuarioSinClave() {
        JsonNode u = crearUsuario("CAJERO");

        JsonNode r = login(u.get("email").asText(), CLAVE, 200);

        assertFalse(r.get("token").asText().isBlank());
        Duration vigencia = Duration.between(Instant.now(), Instant.parse(r.get("expiraEn").asText()));
        assertTrue(vigencia.toMinutes() > 470 && vigencia.toMinutes() <= 480, "vigencia " + vigencia);
        assertEquals("CAJERO", r.get("usuario").get("rol").asText());
        assertFalse(r.toString().toLowerCase().contains("clave"), r.toString());
        assertFalse(r.toString().toLowerCase().contains("password"), r.toString());

        JsonNode yo = llamarComo(r.get("token").asText(), HttpMethod.GET, "/api/auth/yo", null, 200);
        assertEquals(u.get("id").asLong(), yo.get("id").asLong());
    }

    @Test
    void c2_elCorreoNoDistingueMayusculasNiEspacios() {
        String email = crearUsuario("CAJERO").get("email").asText();

        login("  " + email.toUpperCase() + " ", CLAVE, 200);
    }

    @Test
    void c3_credencialesInvalidas_mismoMensajeSiElCorreoExisteONo() {
        String email = crearUsuario("CAJERO").get("email").asText();

        String claveMala = login(email, "Incorrecta1", 401).get("mensaje").asText();
        String correoMalo = login("noexiste-" + email, CLAVE, 401).get("mensaje").asText();

        assertEquals(claveMala, correoMalo);
    }

    @Test
    void c4_usuarioDesactivado_noEntraYSuTokenDejaDeServir() {
        JsonNode u = crearUsuario("CAJERO");
        String token = login(u.get("email").asText(), CLAVE, 200).get("token").asText();
        llamarComo(token, HttpMethod.GET, "/api/productos", null, 200);

        put("/api/usuarios/" + u.get("id").asLong(), actualizacion("CAJERO", false), 200);

        llamarComo(token, HttpMethod.GET, "/api/productos", null, 401);
        login(u.get("email").asText(), CLAVE, 401);
    }

    @Test
    void c5_trasCincoFallos_bloqueaInclusoConLaClaveCorrecta() throws Exception {
        String email = crearUsuario("CAJERO").get("email").asText();
        for (int i = 0; i < 5; i++) {
            login(email, "Incorrecta1", 401);
        }

        MvcResult r = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}")).andReturn();

        assertEquals(429, r.getResponse().getStatus());
        assertNotNull(r.getResponse().getHeader("Retry-After"));
    }

    @Test
    void c6_sinSesionValida_responde401() {
        llamarComo(null, HttpMethod.GET, "/api/productos", null, 401);
        llamarComo(null, HttpMethod.POST, "/api/ventas", Map.of("items", List.of()), 401);
        llamarComo("no-es-un-jwt", HttpMethod.GET, "/api/productos", null, 401);

        String valido = tokenAdmin();
        String alterado = valido.substring(0, valido.length() - 4) + (valido.endsWith("AAAA") ? "BBBB" : "AAAA");
        llamarComo(alterado, HttpMethod.GET, "/api/productos", null, 401);

        llamarComo(firmar("minierp", Instant.now().minusSeconds(60)), HttpMethod.GET, "/api/productos", null, 401);
        llamarComo(firmar("otro-sistema", Instant.now().plusSeconds(3600)), HttpMethod.GET, "/api/productos", null, 401);
        llamarComo(firmarConOtraClave(), HttpMethod.GET, "/api/productos", null, 401);

        JsonNode error = llamarComo(null, HttpMethod.GET, "/api/dashboard", null, 401);
        assertEquals("/api/dashboard", error.get("ruta").asText());

        assertEquals("ok", llamarComo(null, HttpMethod.GET, "/api/salud", null, 200).get("estado").asText());
    }

    @Test
    void c7_permisosDelCajero() {
        String cajero = nuevoUsuario("CAJERO");
        long producto = crearProducto(5, "100", "150", "0");
        long cliente = crearCliente();
        long ventaId = vender(producto, 1).get("id").asLong();

        // Permitido
        llamarComo(cajero, HttpMethod.GET, "/api/productos", null, 200);
        llamarComo(cajero, HttpMethod.GET, "/api/productos/" + producto + "/kardex", null, 200);
        llamarComo(cajero, HttpMethod.GET, "/api/categorias", null, 200);
        llamarComo(cajero, HttpMethod.GET, "/api/clientes", null, 200);
        llamarComo(cajero, HttpMethod.GET, "/api/ventas/" + ventaId, null, 200);
        llamarComo(cajero, HttpMethod.POST, "/api/ventas", venta(item(producto, 1)), 201);
        llamarComo(cajero, HttpMethod.POST, "/api/clientes",
                Map.of("documento", unico("D").substring(0, 10), "nombre", "Cliente del cajero"), 201);
        llamarComo(cajero, HttpMethod.GET, "/api/auth/yo", null, 200);

        // Prohibido
        Map<String, Object> productoNuevo = Map.of("codigo", unico("P"), "nombre", "x", "precioCompra", BigDecimal.ONE,
                "precioVenta", BigDecimal.TEN, "porcentajeIva", BigDecimal.ZERO, "stockMinimo", 0, "categoriaId", categoriaId());
        List<Object[]> prohibidas = List.of(
                new Object[]{HttpMethod.GET, "/api/dashboard", null},
                new Object[]{HttpMethod.POST, "/api/productos", productoNuevo},
                new Object[]{HttpMethod.DELETE, "/api/productos/" + producto, null},
                new Object[]{HttpMethod.POST, "/api/productos/" + producto + "/ajustes",
                        Map.of("tipo", "ENTRADA", "cantidad", 1, "observacion", "x")},
                new Object[]{HttpMethod.POST, "/api/categorias", Map.of("nombre", unico("C"))},
                new Object[]{HttpMethod.PUT, "/api/clientes/" + cliente, Map.of("documento", "1", "nombre", "x")},
                new Object[]{HttpMethod.DELETE, "/api/clientes/" + cliente, null},
                new Object[]{HttpMethod.GET, "/api/compras", null},
                new Object[]{HttpMethod.GET, "/api/proveedores", null},
                new Object[]{HttpMethod.PATCH, "/api/ventas/" + ventaId + "/anular", null},
                new Object[]{HttpMethod.GET, "/api/usuarios", null},
                new Object[]{HttpMethod.POST, "/api/usuarios",
                        Map.of("nombre", "x", "email", "x@minierp.test", "password", CLAVE, "rol", "ADMIN")});
        for (Object[] p : prohibidas) {
            JsonNode error = llamarComo(cajero, (HttpMethod) p[0], (String) p[1], p[2], 403);
            assertEquals(403, error.get("status").asInt());
        }
        assertEquals("COMPLETADA", get("/api/ventas/" + ventaId, 200).get("estado").asText(), "la venta no se anuló");
        // 5 iniciales - 1 (venta del admin) - 1 (venta del cajero); el ajuste prohibido no sumó nada
        assertEquals(3, stock(producto), "el ajuste prohibido no cambió el stock");
    }

    @Test
    void c8_elRolVigenteSeAplicaSinVolverAIniciarSesion() {
        JsonNode u = crearUsuario("CAJERO");
        String token = login(u.get("email").asText(), CLAVE, 200).get("token").asText();
        llamarComo(token, HttpMethod.GET, "/api/dashboard", null, 403);

        put("/api/usuarios/" + u.get("id").asLong(), actualizacion("ADMIN", true), 200);
        llamarComo(token, HttpMethod.GET, "/api/dashboard", null, 200);

        put("/api/usuarios/" + u.get("id").asLong(), actualizacion("CAJERO", true), 200);
        llamarComo(token, HttpMethod.GET, "/api/dashboard", null, 403);
    }

    @Test
    void c9_altaDeUsuarios_validaciones() {
        String email = crearUsuario("CAJERO").get("email").asText();

        post("/api/usuarios", Map.of("nombre", "x", "email", email.toUpperCase(), "password", CLAVE, "rol", "CAJERO"), 409);
        for (String debil : List.of("corta1", "SoloLetras", "12345678")) {
            JsonNode e = post("/api/usuarios", Map.of("nombre", "x", "email", unico("d").toLowerCase() + "@minierp.test",
                    "password", debil, "rol", "CAJERO"), 400);
            assertTrue(e.get("errores").has("password"), debil + " → " + e);
        }
        post("/api/usuarios", Map.of("nombre", "x", "email", unico("r").toLowerCase() + "@minierp.test",
                "password", CLAVE, "rol", "GERENTE"), 400);
        assertTrue(post("/api/usuarios", Map.of("nombre", "", "email", "no-es-correo", "password", CLAVE, "rol", "CAJERO"), 400)
                .get("errores").has("email"));
        get("/api/usuarios/999999", 404);
    }

    @Test
    void c10_unAdminNoPuedeQuitarseElRolNiDesactivarse() {
        JsonNode admin = crearUsuario("ADMIN");
        String token = login(admin.get("email").asText(), CLAVE, 200).get("token").asText();
        String ruta = "/api/usuarios/" + admin.get("id").asLong();

        llamarComo(token, HttpMethod.PUT, ruta, actualizacion("CAJERO", true), 422);
        llamarComo(token, HttpMethod.PUT, ruta, actualizacion("ADMIN", false), 422);
        // Otro administrador sí puede hacerlo (quedan al menos dos activos)
        put(ruta, actualizacion("CAJERO", true), 200);
    }

    @Test
    void c11_cambioDeContrasenaPropia() {
        JsonNode u = crearUsuario("CAJERO");
        String email = u.get("email").asText();
        String token = login(email, CLAVE, 200).get("token").asText();

        llamarComo(token, HttpMethod.POST, "/api/auth/cambiar-clave", Map.of("actual", "Incorrecta1", "nueva", "Nueva12345"), 422);
        llamarComo(token, HttpMethod.POST, "/api/auth/cambiar-clave", Map.of("actual", CLAVE, "nueva", CLAVE), 422);
        assertTrue(llamarComo(token, HttpMethod.POST, "/api/auth/cambiar-clave", Map.of("actual", CLAVE, "nueva", "corta"), 400)
                .get("errores").has("nueva"));
        llamarComo(token, HttpMethod.POST, "/api/auth/cambiar-clave", Map.of("actual", CLAVE, "nueva", "Nueva12345"), 204);

        login(email, "Nueva12345", 200);
        login(email, CLAVE, 401);
    }

    @Test
    void c12_elAdminRestableceLaContrasenaDeOtroUsuario() {
        JsonNode u = crearUsuario("CAJERO");

        llamar(HttpMethod.PUT, "/api/usuarios/" + u.get("id").asLong() + "/clave", Map.of("password", "Restablecida1"), 204);

        login(u.get("email").asText(), "Restablecida1", 200);
        login(u.get("email").asText(), CLAVE, 401);
    }

    @Test
    void c13_lasRespuestasNuncaIncluyenLaClave() {
        crearUsuario("CAJERO");

        String listado = get("/api/usuarios?size=100", 200).toString().toLowerCase();

        assertFalse(listado.contains("clave"), listado);
        assertFalse(listado.contains("password"), listado);
        assertFalse(listado.contains("$2a$"), "no debe aparecer ningún hash BCrypt");
    }

    // ------------------------------------------------------------------

    /** Token con la clave real del backend, pero con el emisor y la expiración indicados. */
    private String firmar(String emisor, Instant expira) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(emisor).subject("1")
                .issuedAt(expira.minusSeconds(3600)).expiresAt(expira).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    /** Token bien formado y vigente, pero firmado con otra clave (alguien que no conoce JWT_SECRET). */
    private String firmarConOtraClave() {
        var otraClave = new SecretKeySpec("una-clave-distinta-de-al-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder ajeno = new NimbusJwtEncoder(new ImmutableSecret<>(otraClave));
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("minierp").subject("1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
        return ajeno.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
