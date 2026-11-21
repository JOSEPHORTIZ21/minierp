# Reporte de auditoría del backend

- **Fecha:** 2026-11-21
- **Alcance:** `minierp/backend` (Spring Boot), configuración de perfiles, migración de Supabase y archivos versionados. El frontend se revisó en lo que afecta a la API (tamaños de página, secretos en `.env`, `dangerouslySetInnerHTML`).
- **Método:** revisión del código guiada por la skill `backend-seguro`, sondas HTTP contra el backend con el perfil `prod` sobre PostgreSQL 16 en Docker (con la migración aplicada) antes y después de corregir, y pruebas unitarias.
- **Resultado:** 13 hallazgos: 9 corregidos y 4 justificados. `mvn test`: 13 pruebas en verde con JDK 21 y JDK 25.

## Resumen

| ID | Hallazgo | Severidad | Estado |
|---|---|---|---|
| A1 | La API no tiene autenticación ni autorización | Alta | Justificado |
| A2 | Dos anulaciones simultáneas devuelven el stock dos veces | Alta | Corregido |
| A3 | Esquema de producción sin migración ni RLS (`ddl-auto=update`) | Alta | Corregido |
| A4 | Cantidades sin tope: desbordan el `Integer` del stock | Media | Corregido |
| A5 | Montos sin control de dígitos: redondeo silencioso y desborde de `numeric(14,2)` | Media | Corregido |
| A6 | Listas y páginas sin tope razonable (ventas de 500 ítems, páginas de 2000) | Media | Corregido |
| A7 | Sin límite de peticiones (rate limiting) | Media | Justificado |
| A8 | Errores de Spring MVC y `sort` inválido responden 500 | Baja | Corregido |
| A9 | Base H2 local versionada en git | Baja | Corregido |
| A10 | El proyecto no compila con JDK 23 o superior | Baja | Corregido |
| A11 | Swagger UI público en producción | Baja | Justificado |
| A12 | Credenciales `root/root` en `application-mysql.properties` y `docker-compose.yml` | Baja | Justificado |
| A13 | Faltaban pruebas de la anulación y de los límites de entrada | Baja | Corregido |

## Detalle

### A1 · Sin autenticación — Justificado
Cualquiera que conozca la URL de Render puede listar clientes (con teléfono y correo), registrar ventas o anular facturas. CORS no protege: solo lo aplica el navegador, y `curl` lo ignora.
**Justificación:** la autenticación no forma parte del alcance de esta unidad, y agregarla exige pantallas de login, manejo de sesión en el front y roles. Mientras tanto, el daño queda acotado por: RLS y un esquema no expuesto (la base solo es accesible a través del backend), validación estricta de cada entrada (A4–A6), reglas de negocio en el servidor (el cliente no puede fijar precios, totales ni stock) y un kardex que deja rastro de cada cambio de inventario.
**Plan:** Spring Security con JWT y roles `ADMIN`/`CAJERO` (ya figura en el README como siguiente paso). Hasta entonces, no cargar datos personales reales en producción.

### A2 · Doble anulación concurrente — Corregido
`VentaService.anular` leía la venta sin bloqueo. Dos peticiones simultáneas pasaban la validación `estado != ANULADA` y ambas devolvían las unidades.
**Evidencia (antes):** 4 `PATCH /anular` en paralelo sobre la misma venta → respuestas `200 422 200 422`, stock de 40 → 41 (debía quedar igual tras vender 1 y anular).
**Corrección:** `VentaRepository.findByIdParaActualizar` (`PESSIMISTIC_WRITE`) en `anular`. **Después:** 5 de 5 intentos `200 422 422 422`, stock sin cambio. Prueba: `VentaServiceTest`.

### A3 · Esquema sin migración ni RLS — Corregido
En `prod`, Hibernate creaba y alteraba las tablas (`ddl-auto=update`): sin restricciones `CHECK`, sin RLS y con cambios de esquema no revisados en cada despliegue.
**Corrección:** `supabase/migrations/20261121000000_esquema_inicial.sql` crea las 9 tablas con `CHECK`, `UNIQUE` e índices, activa RLS en todas sin políticas, revoca `anon`/`authenticated` y carga la semilla. El perfil `prod` pasa a `ddl-auto=validate` y `app.seed.enabled=false`.
**Verificación:** con el rol `anon`, antes del `grant` → `permission denied for schema minierp`; tras un `grant` explícito → `SELECT` devuelve 0 filas e `INSERT` falla con `violates row-level security policy`. El backend arranca con `validate` y opera normalmente. La migración se puede ejecutar dos veces sin error.

### A4 · Desborde de cantidades — Corregido
`POST /api/productos/{id}/ajustes` con `cantidad: 2147483647` sumaba en `int` y el resultado se volvía negativo. En PostgreSQL lo frenó el `CHECK (stock >= 0)` de la migración (409 genérico). En H2 y MySQL, donde Hibernate crea las tablas sin esa restricción, el stock quedaría negativo.
**Corrección:** `@Max(100000)` en todas las cantidades y en el stock inicial y mínimo (`dto/Limites`), y `Math.addExact`/`subtractExact` en `InventarioService`, que responde 422. **Después:** 400 con el error en el campo `cantidad`.

### A5 · Montos sin control de dígitos — Corregido
`precioVenta: 2.555` respondía 201 con `2.555`, pero PostgreSQL guardaba `2.56`: la respuesta no coincidía con lo almacenado. Un precio de 10^15 llegaba a la base y fallaba con 409.
**Corrección:** `@Digits(integer = 12, fraction = 2)` en precios y costos, y `@Digits(3, 2)` en el IVA. **Después:** ambos casos responden 400 con el error por campo.

### A6 · Listas y páginas sin tope — Corregido
Una venta con 500 ítems se procesaba completa (500 `SELECT … FOR UPDATE` en una transacción). `?size=5000` devolvía páginas de hasta 2000 elementos (el máximo por defecto de Spring).
**Corrección:** `@Size(max = 100)` en `items` de ventas y compras, y `spring.data.web.pageable.max-page-size=100`. El front pide como máximo `size=100`, así que no se afecta. **Después:** 500 ítems → 400; `size=5000` → página de 100.

### A7 · Sin rate limiting — Justificado
Sin límite de peticiones, un cliente puede saturar el plan gratuito de Render (512 MB) o el pool de 5 conexiones de Supabase.
**Justificación:** depende de A1 (sin identidad no hay límite por usuario), y el plan gratuito de Render no ofrece un WAF. Se mitiga con A6 (cada petición está acotada) y con el pool limitado, que protege a Supabase. Se implementará junto con la autenticación (por ejemplo Bucket4j por usuario o IP).

### A8 · Respuestas 500 indebidas — Corregido
`?sort=noExiste`, una ruta inexistente y `DELETE /api/ventas` respondían 500 y quedaban registrados como errores internos, porque el manejador general capturaba las excepciones de Spring MVC.
**Corrección:** manejadores de `PropertyReferenceException` (400), `NoResourceFoundException` (404), `HttpRequestMethodNotSupportedException` (405), tipo de contenido no soportado (415) y parámetro obligatorio ausente (400). **Después:** 400, 404 y 405 respectivamente, con el formato `ErrorResponse`.

### A9 · Base H2 versionada — Corregido
`data/minierp.mv.db` estaba en git: cualquier dato cargado en desarrollo terminaba en el repositorio.
**Corrección:** `git rm --cached` y `.gitignore` en la raíz con `data/`. El archivo sigue en el historial, pero solo contiene los datos de ejemplo de `DataSeeder` (correos `example.com`); no se reescribe el historial.

### A10 · No compila con JDK 23+ — Corregido
Con el JDK 25 instalado, `mvn test` fallaba con cientos de errores `cannot find symbol` (getters de Lombok). Desde JDK 23, `javac` no ejecuta procesadores de anotaciones que solo estén en el classpath; además, Lombok 1.18.34 y Byte Buddy 1.14 no soportan JDK 25.
**Corrección:** `lombok.version=1.18.42`, `byte-buddy.version=1.17.8` y Lombok declarado en `annotationProcessorPaths` del `maven-compiler-plugin`. Compila y pasa las pruebas con JDK 21 y 25.

### A11 · Swagger público — Justificado
`/swagger-ui.html` describe todos los endpoints en producción.
**Justificación:** solo documenta una API que ya es pública (A1) y se usa en las pruebas de fuego del despliegue. Al implementar A1 se desactivará en `prod` (`springdoc.swagger-ui.enabled=false`).

### A12 · Credenciales de ejemplo — Justificado
`root/root` en el perfil `mysql` y en `docker-compose.yml`.
**Justificación:** son solo para MySQL local o en un contenedor de desarrollo; el perfil `prod` lee las credenciales de variables de entorno, y el historial de git no contiene secretos de Supabase ni de Render (revisado con `git log -p`).

### A13 · Pruebas insuficientes — Corregido
Solo existía `InventarioServiceTest`.
**Corrección:** `VentaServiceTest` (anulación con bloqueo, doble anulación, venta inexistente), `LimitesValidacionTest` (topes de ítems, cantidades y dígitos) y un caso de desborde en `InventarioServiceTest`.

## Controles verificados sin hallazgos
- Consultas parametrizadas: `Specification`, consultas derivadas y JPQL con `:param`; no hay SQL construido con texto del usuario.
- El cliente no envía precios, IVA, totales, stock ni estado en ventas; los campos desconocidos se ignoran.
- Los errores no exponen trazas ni SQL (500 con mensaje genérico; las violaciones de restricción se responden como 409 genérico y el detalle solo va al log).
- CORS: un origen no permitido recibe 403 en el preflight.
- La consola H2 y `show-sql` están desactivados en `prod`.
- Front: no usa `dangerouslySetInnerHTML` y `.env.local` solo contiene la URL pública de la API y está ignorado por git.

## Acción pendiente en producción
El esquema `minierp` de Supabase lo creó Hibernate. Para aplicar la migración completa (con sus `CHECK`), en el SQL Editor de Supabase ejecuta `drop schema minierp cascade;` (solo contiene datos de ejemplo) y luego el archivo de la migración. Después, redesplegar el backend en Render.
