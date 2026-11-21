package com.minierp.config;

import com.minierp.dto.categoria.CategoriaRequest;
import com.minierp.dto.cliente.ClienteRequest;
import com.minierp.dto.producto.ProductoRequest;
import com.minierp.dto.producto.ProductoResponse;
import com.minierp.dto.proveedor.ProveedorRequest;
import com.minierp.dto.venta.ItemVentaRequest;
import com.minierp.dto.venta.VentaRequest;
import com.minierp.dto.usuario.UsuarioRequest;
import com.minierp.entity.enums.Rol;
import com.minierp.repository.CategoriaRepository;
import com.minierp.repository.UsuarioRepository;
import com.minierp.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carga datos de ejemplo la primera vez que arranca la aplicación (base vacía).
 * Usa los mismos servicios que la API, así los datos pasan por todas las reglas de negocio.
 * Para desactivarlo: app.seed.enabled=false
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final String CAJERO_DEMO = "cajero@minierp.local";

    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final ProveedorService proveedorService;
    private final VentaService ventaService;

    @Override
    public void run(String... args) {
        // Usuario de prueba con rol CAJERO (solo desarrollo: en prod app.seed.enabled=false).
        // El ADMIN de desarrollo lo crea AdminInicial con app.admin.* de application.properties.
        if (!usuarioRepository.existsByEmail(CAJERO_DEMO)) {
            usuarioService.crear(new UsuarioRequest("Cajero de prueba", CAJERO_DEMO, "Cajero12345", Rol.CAJERO));
            log.info("Usuario de prueba creado: {}", CAJERO_DEMO);
        }

        if (categoriaRepository.count() > 0) {
            return;
        }
        log.info("Base de datos vacía: cargando datos de ejemplo...");

        Long abarrotes = categoriaService.crear(new CategoriaRequest("Abarrotes", "Granos, aceites y despensa")).id();
        Long bebidas = categoriaService.crear(new CategoriaRequest("Bebidas", "Agua, jugos y gaseosas")).id();
        Long aseo = categoriaService.crear(new CategoriaRequest("Aseo", "Limpieza del hogar")).id();
        Long papeleria = categoriaService.crear(new CategoriaRequest("Papelería", "Útiles escolares y de oficina")).id();

        ProductoResponse arroz = producto("ABA-001", "Arroz blanco 500 g", "1800", "2500", "0", 120, 30, abarrotes);
        producto("ABA-002", "Aceite vegetal 1 L", "7200", "9500", "5", 40, 10, abarrotes);
        ProductoResponse cafe = producto("ABA-003", "Café molido 500 g", "11000", "14900", "5", 25, 8, abarrotes);
        producto("ABA-004", "Azúcar 1 kg", "3600", "4800", "5", 6, 15, abarrotes);           // stock bajo a propósito
        ProductoResponse agua = producto("BEB-001", "Agua sin gas 600 ml", "900", "1800", "19", 200, 50, bebidas);
        producto("BEB-002", "Jugo de naranja 1 L", "3800", "5500", "19", 4, 10, bebidas);  // stock bajo a propósito
        ProductoResponse detergente = producto("ASE-001", "Detergente en polvo 1 kg", "8200", "11500", "19", 35, 10, aseo);
        producto("ASE-002", "Jabón de loza 500 g", "3100", "4500", "19", 60, 15, aseo);
        ProductoResponse cuaderno = producto("PAP-001", "Cuaderno cuadriculado 100 hojas", "2900", "4500", "19", 80, 20, papeleria);
        producto("PAP-002", "Bolígrafo negro", "600", "1200", "19", 150, 40, papeleria);

        Long laura = clienteService.crear(new ClienteRequest("1121000001", "Laura Martínez", "3101234567",
                "laura@example.com", "Barrio Centro")).id();
        clienteService.crear(new ClienteRequest("900123456", "Distribuidora El Llano S.A.S.", "6086712345",
                "compras@elllano.example.com", "Km 5 vía Puerto López"));

        proveedorService.crear(new ProveedorRequest("900555111-2", "Mayorista Andino S.A.S.", "6017001122",
                "ventas@mayoristaandino.example.com", "Bogotá"));
        proveedorService.crear(new ProveedorRequest("901222333-4", "Aseo Total Ltda.", "6045556677",
                "pedidos@aseototal.example.com", "Medellín"));

        // Algunas ventas para que el dashboard no arranque vacío
        ventaService.registrar(new VentaRequest(laura, List.of(
                new ItemVentaRequest(arroz.id(), 4), new ItemVentaRequest(cafe.id(), 1))));
        ventaService.registrar(new VentaRequest(null, List.of(
                new ItemVentaRequest(agua.id(), 6), new ItemVentaRequest(cuaderno.id(), 3))));
        ventaService.registrar(new VentaRequest(null, List.of(
                new ItemVentaRequest(detergente.id(), 2), new ItemVentaRequest(arroz.id(), 2))));

        log.info("Datos de ejemplo cargados.");
    }

    private ProductoResponse producto(String codigo, String nombre, String compra, String venta, String iva,
                                      int stock, int minimo, Long categoriaId) {
        return productoService.crear(new ProductoRequest(codigo, nombre, null, new BigDecimal(compra),
                new BigDecimal(venta), new BigDecimal(iva), stock, minimo, categoriaId));
    }
}
