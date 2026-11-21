---
name: backend-seguro
description: Lista de verificación de seguridad e integridad para cambios en el backend Spring Boot del Mini-ERP (minierp/backend) y en la migración de Supabase. Úsala antes de crear o modificar un endpoint, DTO, servicio, repositorio, entidad, tabla o configuración de perfiles, y al auditar el backend.
---

# Backend seguro (Mini-ERP)

Contexto: el navegador solo habla con la API de Spring Boot; Spring Boot es el único cliente de la base (Supabase PostgreSQL, esquema `minierp`, RLS activo sin políticas). La API **no tiene autenticación** (hallazgo A1 de `docs/auditoria.md`), así que cada endpoint debe ser seguro aunque lo invoque cualquiera.

Antes de dar por terminado un cambio en el backend, recorre esta lista y corrige lo que no cumpla.

## 1. Entrada (DTO y controlador)
- [ ] El controlador recibe un `record *Request` con `@Valid @RequestBody`, nunca una entidad.
- [ ] Cada `String` tiene `@Size(max = …)` igual al `length` de la columna; los obligatorios llevan `@NotBlank`.
- [ ] Cada cantidad entera tiene `@Positive`/`@PositiveOrZero` **y** `@Max(100_000)` (evita desbordar `Integer` en el stock).
- [ ] Cada monto tiene `@DecimalMin` y `@Digits(integer = 12, fraction = 2)` (cabe en `numeric(14,2)`, sin redondeos silenciosos).
- [ ] Cada lista lleva `@NotEmpty`, `@Size(max = 100)` y `List<@Valid …>`.
- [ ] El cliente nunca envía precios, totales, IVA, stock, estado ni números consecutivos: los calcula el servicio.
- [ ] Los listados usan `Pageable` (tamaño máximo global de 100 en `application.properties`) o una consulta acotada; nada de `findAll()` sin límite sobre tablas que crecen.

## 2. Reglas de negocio (servicio)
- [ ] La regla vive en el servicio, no en el controlador ni solo en el frontend.
- [ ] Cambios de stock **solo** por `InventarioService.registrarEntrada/registrarSalida` dentro de un `@Transactional` del servicio que llama.
- [ ] Toda fila que se lee para luego modificarla según su estado (stock, estado de venta) se carga con un método `…ParaActualizar` (`@Lock(PESSIMISTIC_WRITE)`). Si se bloquean varias filas, en orden de id.
- [ ] Las comprobaciones de unicidad del servicio (409) tienen su `unique` en la base; las de rango (422/400) tienen su `check` en la migración.
- [ ] Los errores se lanzan con las excepciones de `com.minierp.exception` (404/409/422). Nunca `ResponseStatusException` ni `RuntimeException` genérica.
- [ ] Los mensajes de error son para el usuario: sin SQL, nombres de tablas, trazas ni datos de otros registros.

## 3. Persistencia
- [ ] Consultas con parámetros (`:param`, derivadas o `Specification`); nunca concatenar texto del usuario en JPQL/SQL.
- [ ] `sort` del cliente: un campo inexistente debe responder 400 (lo maneja `GlobalExceptionHandler`); no expongas ordenamiento por relaciones o campos sensibles.
- [ ] Toda entidad o columna nueva va **también** en una migración nueva en `supabase/migrations/` (`YYYYMMDDHHMMSS_descripcion.sql`) con: tipos iguales a los que genera Hibernate, `check`/`unique` de las reglas, índices de las FK y `alter table … enable row level security`. En producción `ddl-auto=validate` no crea nada: si falta la migración, el backend no arranca.
- [ ] Nunca crees políticas RLS para `anon` ni `authenticated`, ni muevas tablas al esquema `public`.

## 4. Configuración y secretos
- [ ] Credenciales y orígenes solo por variables de entorno en `application-prod.properties` (`${DB_URL}`, …). Nada de secretos en archivos versionados, `.env.local` incluido.
- [ ] La consola H2 y `show-sql` quedan desactivados en `prod`.
- [ ] CORS: solo `/api/**`, solo los orígenes de `app.cors.allowed-origins`; nunca `*`.
- [ ] Los logs de error no imprimen cuerpos de petición ni datos personales.

## 5. Verificación
- [ ] `mvn test` en verde (con JDK 17–25).
- [ ] Si cambiaste un contrato: actualiza `docs/contratos/api.md`, la spec en `docs/specs/` y `frontend/src/lib/types.ts`.
- [ ] Prueba manual de los errores del endpoint nuevo: cuerpo vacío (400), id inexistente (404) y la regla de negocio (409/422).
