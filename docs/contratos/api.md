# Contrato de la API REST

- **Base:** `http://localhost:8080/api` en desarrollo · `https://<servicio>.onrender.com/api` en producción.
- **Formato:** JSON en UTF-8. Fechas `LocalDateTime` como `2026-11-21T10:15:30`; fechas de filtro como `2026-11-21`. Montos como número decimal con 2 decimales.
- **Autenticación:** token JWT en la cabecera `Authorization: Bearer <token>`, obtenido en `POST /auth/login` (vigencia de 8 horas). Todas las rutas lo exigen salvo `POST /auth/login` y `GET /salud`. El rol se verifica en cada petición contra la base. Ver [autenticación y usuarios](#autenticación-y-usuarios) y la [spec](../specs/autenticacion.md).
- **CORS:** solo los orígenes de `APP_CORS_ALLOWED_ORIGINS`, con las cabeceras `Authorization` y `Content-Type`.
- **Documentación interactiva:** `/swagger-ui.html` (generada a partir del código; este documento es el contrato acordado).

## Convenciones

### Paginación
Los listados paginados aceptan `page` (desde 0), `size` (máximo **100**; valores mayores se recortan a 100) y `sort=campo,asc|desc`. Responden:

```json
{ "content": [ ... ], "page": 0, "size": 10, "totalElements": 42, "totalPages": 5, "first": true, "last": false }
```

### Errores
Todos los errores usan el mismo cuerpo:

```json
{
  "timestamp": "2026-11-21T10:15:30",
  "status": 422,
  "error": "Unprocessable Entity",
  "mensaje": "Stock insuficiente para 'Azúcar 1 kg': disponible 6, solicitado 10",
  "ruta": "/api/ventas",
  "errores": { "items[0].cantidad": "La cantidad debe ser mayor a cero" }
}
```
`errores` solo aparece en los 400 de validación (campo → mensaje).

| Código | Cuándo | Aplica a |
|---|---|---|
| 400 | Validación de campos (`errores` presente), JSON mal formado, parámetro con tipo inválido, campo de `sort` inexistente | Todas las operaciones con cuerpo o parámetros |
| 404 | El recurso del `{id}` (o uno referenciado en el cuerpo) no existe; ruta inexistente | Operaciones con `{id}` o con ids en el cuerpo |
| 401 | Sin token, token inválido, vencido, firmado por otro sistema o de un usuario desactivado; en el login, credenciales incorrectas (mismo mensaje si el correo no existe) | Todas salvo `GET /salud` |
| 403 | El rol del usuario no permite la operación | Ver la columna **Rol** de cada sección |
| 405 | Método HTTP no soportado en la ruta | Todas |
| 409 | Duplicado de un campo único, o borrado de un registro en uso | Crear/editar/eliminar catálogos, registrar compra |
| 422 | Regla de negocio incumplida (stock, precios, estado) | Productos, ventas, compras, ajustes |
| 429 | 5 intentos de login fallidos con el mismo correo; cabecera `Retry-After` con los segundos de espera | `POST /auth/login` |
| 500 | Error no controlado. El mensaje es genérico (`Error interno del servidor`); el detalle solo queda en el log | Todas |

Los errores 400 y 405 aplican a todas las operaciones y no se repiten en las tablas siguientes.

## Autenticación y usuarios

| Operación | Rol | Respuesta | Errores |
|---|---|---|---|
| `GET /salud` | Público | 200 `{estado: "ok"}` (health check de Render) | — |
| `POST /auth/login` `{email, password}` | Público | 200 `{token, expiraEn, usuario}` | 401 credenciales · 429 demasiados intentos |
| `GET /auth/yo` | Cualquiera | 200 `Usuario` con el rol vigente | — |
| `POST /auth/cambiar-clave` `{actual, nueva}` | Cualquiera | 204 | 422 actual incorrecta o igual a la nueva |
| `GET /usuarios?page=&size=&sort=` | ADMIN | 200 `Page<Usuario>` | — |
| `GET /usuarios/{id}` | ADMIN | 200 `Usuario` | 404 |
| `POST /usuarios` `{nombre, email, password, rol}` | ADMIN | 201 `Usuario` | 409 correo repetido |
| `PUT /usuarios/{id}` `{nombre, rol, activo}` | ADMIN | 200 `Usuario` | 404 · 422 quitarse el rol o desactivarse a sí mismo · 422 dejar el sistema sin administrador activo |
| `PUT /usuarios/{id}/clave` `{password}` | ADMIN | 204 | 404 |

`Usuario = {id, email, nombre, rol: ADMIN|CAJERO, activo, creadoEn}`: nunca incluye la contraseña ni su hash. El correo se guarda en minúsculas y no se edita. Contraseñas: de 8 a 72 caracteres, con al menos una letra y un número (si no, 400 con el error en el campo).

**Rol** en las secciones siguientes: **ADMIN** y **CAJERO** pueden `GET` productos (incluido el kardex), categorías, clientes y ventas, y `POST` ventas y clientes. Todo lo demás es solo **ADMIN** (403 para el CAJERO): dashboard, compras, proveedores, escritura de productos y categorías, ajustes, edición y borrado de clientes, anulación de ventas y usuarios.

## Dashboard

| Operación | Respuesta | Errores |
|---|---|---|
| `GET /dashboard` | 200 `{ventasHoy, ventasMes, numeroVentasMes, ticketPromedioMes, productosStockBajo, valorInventario, ventasUltimos7Dias[{fecha,total,cantidad}], topProductos[{productoId,nombre,cantidadVendida,totalVendido}], alertasStock[Producto]}` | — |

Solo cuenta ventas `COMPLETADA`. Los 7 días incluyen los días sin ventas en cero. Muestra hasta 5 elementos en top y alertas.

## Categorías

`Categoria = {id, nombre, descripcion}` · Request: `nombre` (obligatorio, ≤80), `descripcion` (≤255).

| Operación | Respuesta | Errores |
|---|---|---|
| `GET /categorias` | 200 `Categoria[]` ordenadas por nombre (sin paginar) | — |
| `GET /categorias/{id}` | 200 `Categoria` | 404 |
| `POST /categorias` | 201 `Categoria` | 409 nombre repetido (sin distinguir mayúsculas) |
| `PUT /categorias/{id}` | 200 `Categoria` | 404 · 409 nombre repetido |
| `DELETE /categorias/{id}` | 204 | 404 · 409 tiene productos |

## Clientes

`Cliente = {id, documento, nombre, telefono, email, direccion}` · Request: `documento` (obligatorio, ≤20), `nombre` (obligatorio, ≤120), `telefono` (≤20), `email` (formato email, ≤120), `direccion` (≤200).

| Operación | Respuesta | Errores |
|---|---|---|
| `GET /clientes?buscar=&page=&size=&sort=` | 200 `Page<Cliente>`; `buscar` filtra por nombre o documento; orden por defecto `nombre` | — |
| `GET /clientes/{id}` | 200 `Cliente` | 404 |
| `POST /clientes` | 201 `Cliente` | 409 documento repetido |
| `PUT /clientes/{id}` | 200 `Cliente` | 404 · 409 documento repetido |
| `DELETE /clientes/{id}` | 204 | 404 · 409 tiene ventas |

## Proveedores

`Proveedor = {id, nit, razonSocial, telefono, email, direccion}` · Request: `nit` (obligatorio, ≤20), `razonSocial` (obligatorio, ≤150), `telefono` (≤20), `email` (formato email, ≤120), `direccion` (≤200).

| Operación | Respuesta | Errores |
|---|---|---|
| `GET /proveedores?buscar=&page=&size=&sort=` | 200 `Page<Proveedor>`; `buscar` filtra por razón social o NIT; orden por defecto `razonSocial` | — |
| `GET /proveedores/{id}` | 200 `Proveedor` | 404 |
| `POST /proveedores` | 201 `Proveedor` | 409 NIT repetido |
| `PUT /proveedores/{id}` | 200 `Proveedor` | 404 · 409 NIT repetido |
| `DELETE /proveedores/{id}` | 204 | 404 · 409 tiene compras |

## Productos e inventario

`Producto = {id, codigo, nombre, descripcion, precioCompra, precioVenta, porcentajeIva, stock, stockMinimo, activo, stockBajo, categoriaId, categoriaNombre}`

Request de producto:

| Campo | Regla |
|---|---|
| `codigo` | obligatorio, ≤30; se guarda en mayúsculas |
| `nombre` | obligatorio, ≤120 |
| `descripcion` | ≤255 |
| `precioCompra`, `precioVenta` | obligatorios, > 0, máximo 12 enteros y 2 decimales; `precioVenta ≥ precioCompra` |
| `porcentajeIva` | obligatorio, entre 0 y 100, máximo 2 decimales |
| `stockInicial` | 0 a 100 000; solo se usa al crear |
| `stockMinimo` | obligatorio, 0 a 100 000 |
| `categoriaId` | obligatorio |

| Operación | Respuesta | Errores |
|---|---|---|
| `GET /productos?buscar=&categoriaId=&stockBajo=&incluirInactivos=&page=&size=&sort=` | 200 `Page<Producto>`. Por defecto solo activos; `buscar` filtra por nombre o código | — |
| `GET /productos/{id}` | 200 `Producto` | 404 |
| `POST /productos` | 201 `Producto`. Si `stockInicial > 0`, crea un movimiento `INVENTARIO_INICIAL` | 404 categoría · 409 código repetido · 422 precio de venta menor al de compra |
| `PUT /productos/{id}` | 200 `Producto`. No modifica `stock` | 404 producto o categoría · 409 código repetido · 422 precio de venta menor al de compra |
| `DELETE /productos/{id}` | 204. Borrado lógico (`activo=false`) | 404 |
| `PATCH /productos/{id}/activar` | 200 `Producto` | 404 |
| `GET /productos/{id}/kardex` | 200 `Movimiento[]` en orden cronológico | 404 |
| `POST /productos/{id}/ajustes` | 201 `Movimiento`. Body `{tipo: ENTRADA\|SALIDA, cantidad: 1..100000, observacion: obligatorio ≤255}` | 404 · 422 salida mayor que el stock |

`Movimiento = {id, fecha, tipo, concepto, cantidad, stockAnterior, stockResultante, referencia, observacion}`

## Ventas

`VentaResumen = {id, numero, fecha, clienteNombre, subtotal, totalIva, total, estado}`
`Venta = VentaResumen + {clienteId, clienteDocumento, detalles[{id, productoId, productoCodigo, productoNombre, cantidad, precioUnitario, porcentajeIva, subtotal, valorIva, total}]}`

Request:
```json
{ "clienteId": 1, "items": [ { "productoId": 1, "cantidad": 3 } ] }
```
`clienteId` es opcional (nulo = consumidor final, documento `222222222222`). `items`: de 1 a 100 elementos; `cantidad`: de 1 a 100 000. Los productos repetidos se suman. El cliente no envía precios.

| Operación | Respuesta | Errores |
|---|---|---|
| `GET /ventas?desde=&hasta=&page=&size=&sort=` | 200 `Page<VentaResumen>`; orden por defecto `fecha,desc` | — |
| `GET /ventas/{id}` | 200 `Venta` | 404 |
| `POST /ventas` | 201 `Venta` con número `FV-000001`; descuenta stock y registra movimientos `VENTA` | 404 cliente o producto · 422 stock insuficiente · 422 producto inactivo |
| `PATCH /ventas/{id}/anular` | 200 `Venta` con `estado=ANULADA`; devuelve stock con movimientos `ANULACION_VENTA` | 404 · 422 ya estaba anulada |

## Compras

`CompraResumen = {id, numeroFactura, fecha, proveedorNombre, subtotal, totalIva, total}`
`Compra = CompraResumen + {proveedorId, proveedorNit, detalles[{id, productoId, productoCodigo, productoNombre, cantidad, costoUnitario, porcentajeIva, subtotal, valorIva, total}]}`

Request:
```json
{ "proveedorId": 1, "numeroFactura": "FAC-123", "items": [ { "productoId": 1, "cantidad": 10, "costoUnitario": 1800 } ] }
```
`numeroFactura`: obligatorio, ≤40, se guarda en mayúsculas. `items`: de 1 a 100 elementos, sin productos repetidos; `cantidad`: de 1 a 100 000; `costoUnitario`: > 0, máximo 12 enteros y 2 decimales.

| Operación | Respuesta | Errores |
|---|---|---|
| `GET /compras?page=&size=&sort=` | 200 `Page<CompraResumen>`; orden por defecto `fecha,desc` | — |
| `GET /compras/{id}` | 200 `Compra` | 404 |
| `POST /compras` | 201 `Compra`; suma stock (movimientos `COMPRA`) y actualiza `precioCompra` de cada producto | 404 proveedor o producto · 409 factura repetida para el proveedor · 422 producto repetido |
