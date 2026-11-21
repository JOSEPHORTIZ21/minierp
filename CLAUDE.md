# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Mini-ERP de inventario y facturación (proyecto académico). Todo el código, los nombres y los mensajes están en **español**; mantén esa convención (clases, métodos, campos, mensajes de error, textos de UI).

El código vive en `minierp/`: `minierp/backend` (Spring Boot, puerto 8080) y `minierp/frontend` (Next.js, puerto 3000). [minierp/README.md](minierp/README.md) tiene el modelo de datos, la tabla de reglas de negocio, los endpoints y los pasos de despliegue.

## Comandos

Backend (`minierp/backend`, Java 17, Maven; no hay wrapper `mvnw`):
```bash
mvn spring-boot:run                                        # H2 por defecto
mvn spring-boot:run -Dspring-boot.run.profiles=mysql       # MySQL (docker compose up -d en minierp/)
mvn test                                                   # todas las pruebas
mvn test -Dtest=InventarioServiceTest                      # una clase
mvn test -Dtest=InventarioServiceTest#registrarSalida_descuentaStockYGuardaMovimiento  # un método
mvn package -DskipTests
```
- Swagger: http://localhost:8080/swagger-ui.html · consola H2: http://localhost:8080/h2-console (`jdbc:h2:file:./data/minierp`, usuario `sa`, sin contraseña).

Frontend (`minierp/frontend`, Node 20+):
```bash
cp .env.local.example .env.local   # NEXT_PUBLIC_API_URL=http://localhost:8080/api
npm install
npm run dev
npm run build                      # también sirve como verificación de tipos
```
No hay linter ni pruebas en el frontend.

## Base de datos y perfiles

- **Por defecto (H2 en archivo)**: `./data/minierp` es relativo al directorio de trabajo del proceso. Al ejecutar desde IntelliJ queda en la raíz del repo (`data/minierp.mv.db`, que está versionado en git); con `mvn spring-boot:run` dentro de `backend/` queda en `backend/data/`. Para reiniciar: detener la app y borrar `data/`.
- En H2 y MySQL el esquema lo genera Hibernate (`ddl-auto=update`). En `prod` lo define la migración `supabase/migrations/` y Hibernate solo valida (`ddl-auto=validate`).
- `DataSeeder` carga datos de ejemplo solo si no hay categorías (`app.seed.enabled`).
- **`mysql`**: `application-mysql.properties`.
- **`prod`**: PostgreSQL en Supabase, desplegado en Render con el `Dockerfile` (que activa `prod`). Usa las variables de entorno `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `APP_CORS_ALLOWED_ORIGINS`. Las tablas van al esquema `minierp` (no `public`), con RLS activo y sin políticas; los datos semilla los carga la migración (`app.seed.enabled=false`). Requiere el Session pooler (puerto 5432), no el Transaction pooler. El pool de Hikari está limitado a 5 conexiones. El frontend se despliega en Vercel.
- Los orígenes CORS salen de `app.cors.allowed-origins` (`CorsConfig`, solo para `/api/**`).

## Backend

Documentación de referencia: [docs/datos/modelo.md](docs/datos/modelo.md) (ER y en qué capa vive cada regla), [docs/contratos/api.md](docs/contratos/api.md) (operaciones y errores), [docs/specs/](docs/specs/) (criterios de aceptación del back) y [docs/auditoria.md](docs/auditoria.md). Antes de tocar endpoints, DTO, servicios, entidades o la migración, usa la skill **`backend-seguro`** (`.claude/skills/backend-seguro`).

### Arquitectura (`com.minierp`)

Capas: `controller` → `service` → `repository` → `entity`, con `dto` separados por módulo (`dto/venta`, `dto/producto`, …).
- Los DTO son `record`s. Los `*Response` se construyen con un `static from(Entidad)`; los `*Request` llevan Bean Validation y se reciben con `@Valid`. Los controladores nunca exponen entidades.
- Los listados paginados devuelven `PageResponse<T>` (no `Page` de Spring), y el frontend lo tipa como `Page<T>` en `lib/types.ts`.
- Errores: los servicios lanzan `RecursoNoEncontradoException` (404), `ReglaNegocioException`/`StockInsuficienteException` (422) o `ConflictoException` (409). `GlobalExceptionHandler` convierte todo a `ErrorResponse {status, error, mensaje, ruta, errores?}`, donde `errores` es el mapa campo→mensaje de las validaciones.

### Invariantes que atraviesan varios servicios
- **El stock solo lo modifica `InventarioService`** (`registrarEntrada`/`registrarSalida`), que además crea el `MovimientoInventario` (kardex). Sus métodos son `@Transactional(propagation = MANDATORY)`: hay que llamarlos dentro de una transacción de `VentaService`, `CompraService` o de un ajuste. Nunca hagas `producto.setStock(...)` en otro lugar.
- Para modificar stock, carga el producto con `ProductoRepository.findByIdParaActualizar` (bloqueo `PESSIMISTIC_WRITE`) y procesa los ítems **ordenados por id de producto** para evitar deadlocks.
- Los precios, el IVA y los totales se calculan en el backend; las peticiones de venta y compra solo envían `productoId` y `cantidad`. El detalle guarda el precio del momento.
- Dinero siempre en `BigDecimal` mediante `util/Montos` (escala 2, `HALF_UP`). IVA colombiano: 0, 5 o 19 %.
- Productos con borrado lógico (`activo`). No se pueden borrar categorías, clientes ni proveedores que estén en uso.
- `spring.jpa.open-in-view=false`: carga dentro del servicio todas las relaciones que necesite el DTO (transacciones `readOnly`, `join fetch`), porque fuera de ellos se produce `LazyInitializationException`.

### Seguridad y cambios de esquema
- La API no tiene autenticación: todo endpoint es público. La validación de los DTO (límites de texto, cantidades hasta 100 000, montos `@Digits(12,2)`, listas de hasta 100 ítems) y el tamaño máximo de página (100) son la primera línea de defensa; no los relajes.
- Cualquier entidad o columna nueva necesita una migración nueva en `supabase/migrations/` con RLS activo. Si falta, `prod` no arranca porque `validate` falla.
- Para probar la migración y el perfil `prod` en local: levanta `postgres:16` en Docker, aplica el `.sql` con `psql` y arranca el jar con `SPRING_PROFILES_ACTIVE=prod` y las variables `DB_*` apuntando al contenedor.

### Pruebas
Unitarias con Mockito (`@ExtendWith(MockitoExtension.class)`), sin levantar Spring ni la base de datos.

## Arquitectura del frontend (`minierp/frontend/src`)

- Next.js 15 App Router + React 19 + Tailwind 4 + Recharts. Las páginas son componentes de cliente (`"use client"`) que piden los datos a la API desde el navegador; no hay rutas de API ni obtención de datos en el servidor.
- Toda llamada HTTP pasa por `lib/api.ts` (`api.get/post/put/patch/delete`, `query()` para armar el query string). Lanza `ApiError` con `mensaje` y `errores`; usa `erroresCampos(e)` para mostrar los errores por campo y `mensajeError(e)` (`lib/format.ts`) para el mensaje general.
- `lib/types.ts` replica a mano los DTO del backend: si cambias un `*Response` o `*Request` en Java, actualízalo aquí también.
- `components/ui.tsx` reúne los componentes visuales compartidos (`PageHeader`, `Alerta`, `Campo`, `Paginacion`, `StatCard`, …). `CatalogoTerceros` es la pantalla CRUD genérica que usan clientes y proveedores. `lib/format.ts` formatea moneda COP y fechas.
