# Bitácora de uso de IA

Herramienta: Claude Code (modelo Claude Opus 5.5), en la app de escritorio, sobre la rama `entregables-unidad`.
Fecha: 2026-11-21.

## Prompts clave

| # | Prompt (resumido) | Resultado |
|---|---|---|
| 1 | Analizar el repositorio y generar un `CLAUDE.md` para futuras sesiones (comandos, arquitectura, invariantes) | `CLAUDE.md` con comandos de Maven/npm, perfiles de base de datos e invariantes del stock |
| 2 | Crear los entregables de la unidad: modelo de datos, contrato de la API, specs, migración con RLS y semilla, skill `backend-seguro`, sección Backend, front con datos reales, auditoría, bitácora | El resto de esta rama, un commit por tarea |
| 3 | Decisión de arquitectura (pregunta de la IA): ¿el front habla con Supabase o solo con el backend? → **Solo backend** | RLS en todas las tablas **sin políticas**; el front sigue usando la API de Spring |
| 4 | Decisión de flujo (pregunta de la IA): ¿quién hace los commits? → **La IA, en una rama, sin push** | Rama `entregables-unidad`; el despliegue queda a cargo del equipo |
| 5 | "¿Pruebas de integración o e2e, cuál es mejor y por qué?" → integración primero; luego "Sí, integración" | 47 pruebas `*IT` con Testcontainers y la migración real, una por criterio de las specs. Encontraron un hallazgo nuevo (A14, zona horaria) |

Instrucciones internas que guiaron el trabajo y que conviene reutilizar como prompts:
- "Antes de documentar, lee las entidades, los servicios y los DTO completos; no documentes de memoria."
- "Verifica cada hallazgo con una petición real antes y después de corregirlo."
- "Prueba la migración en un PostgreSQL desechable en Docker y arranca el backend con `ddl-auto=validate` contra ella."

## Fugas encontradas en el backend y cómo se cerraron

Detalle completo en [`docs/auditoria.md`](docs/auditoria.md). No se sabe con certeza qué partes del código original generó una IA. Estas son las fugas típicas de código generado que aparecieron:

| Fuga | Cómo se detectó | Cierre |
|---|---|---|
| Condición de carrera en la anulación: el `if (estado == ANULADA)` sin bloqueo devolvía el stock dos veces | Script con 4 `PATCH /anular` en paralelo: stock 40 → 41 | `SELECT … FOR UPDATE` sobre la venta (`findByIdParaActualizar`) y `VentaServiceTest` |
| Validación "feliz": `@Positive` sin `@Max`, montos sin `@Digits`, listas sin `@Size` | Sondas con `2147483647`, `2.555`, 10^15 y 500 ítems | Clase `Limites` y anotaciones en todos los `*Request`; `LimitesValidacionTest` |
| Desborde de `int` al sumar stock | La sonda del ajuste solo se frenó por el `CHECK` de PostgreSQL | `Math.addExact` en `InventarioService` y prueba unitaria |
| Manejador `catch (Exception)` que convertía 404 y 405 en 500 | Sondas a una ruta inexistente, a un método no soportado y a un `sort` inválido | Manejadores específicos en `GlobalExceptionHandler` |
| Esquema en manos de `ddl-auto=update` en producción, sin RLS | Revisión de `application-prod.properties` | Migración versionada con RLS y `ddl-auto=validate` |
| Archivo de base de datos versionado | `git ls-files` | `.gitignore` y `git rm --cached` |
| Dependencias que no soportan el JDK instalado (Lombok, Byte Buddy) | `mvn test` falló con JDK 25 | Versiones actualizadas y Lombok en `annotationProcessorPaths` |

Se decidió **no** cerrar en esta unidad la falta de autenticación (A1) ni el rate limiting (A7); su justificación está en la auditoría.

## Errores que cometió la IA en esta sesión y cómo se corrigieron

| Error | Cómo se notó | Corrección |
|---|---|---|
| Creyó que subir la versión de Lombok bastaba para compilar con JDK 25 | El build siguió fallando | Además declaró Lombok como procesador de anotaciones y actualizó Byte Buddy para Mockito |
| Documentó campos del dashboard con nombres inventados (`cantidad`, `total`) | Al contrastar con `TopProductoResponse` | Corregido a `cantidadVendida` y `totalVendido` en `docs/contratos/api.md` |
| El borrado de `data/minierp.mv.db` quedó en el commit de otra corrección (A2), porque el archivo ya estaba en el índice | Al revisar `git log --diff-filter=D` | Rehizo los commits de la auditoría (rama local, sin push) para que cada uno tenga solo sus archivos |
| Agregó `.idea/` al `.gitignore` aunque esa carpeta ya está versionada (no tendría efecto) | Al revisar `git ls-files .idea` | Quitó la línea; versionar o no `.idea` queda a decisión del equipo |
| La migración revocaba permisos a `anon`/`authenticated`, roles que no existen en un PostgreSQL local | Al pensar en cómo probarla fuera de Supabase | Bloque `do $$ … if exists (pg_roles)`; se probó en los dos escenarios (con y sin los roles) |

## Lecciones
- Pedir siempre evidencia ejecutable (una petición, una prueba) de cada hallazgo, antes y después de corregirlo.
- Las reglas de negocio con estado ("si no está anulada…") necesitan un bloqueo o una restricción en la base; si no, una prueba concurrente las rompe.
- Revisar lo que la IA pone en cada commit (`git show --stat`), no solo el mensaje.
