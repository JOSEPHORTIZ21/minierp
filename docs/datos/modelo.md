# Modelo de datos

Fuente de verdad del esquema: la migración [`supabase/migrations/20261121000000_esquema_inicial.sql`](../../supabase/migrations/20261121000000_esquema_inicial.sql) (producción, PostgreSQL en Supabase, esquema `minierp`). En desarrollo, Hibernate genera las mismas tablas en H2 a partir de las entidades de `com.minierp.entity`.

## Diagrama entidad-relación

```mermaid
erDiagram
    CATEGORIAS ||--o{ PRODUCTOS : agrupa
    PRODUCTOS ||--o{ MOVIMIENTOS_INVENTARIO : "kardex"
    PRODUCTOS ||--o{ DETALLES_VENTA : "se vende en"
    PRODUCTOS ||--o{ DETALLES_COMPRA : "se compra en"
    CLIENTES |o--o{ VENTAS : realiza
    VENTAS ||--|{ DETALLES_VENTA : contiene
    PROVEEDORES ||--o{ COMPRAS : factura
    COMPRAS ||--|{ DETALLES_COMPRA : contiene

    CATEGORIAS {
        bigint id PK
        varchar(80) nombre UK
        varchar(255) descripcion
    }
    PRODUCTOS {
        bigint id PK
        varchar(30) codigo UK
        varchar(120) nombre
        varchar(255) descripcion
        numeric(14_2) precio_compra "último costo, sin IVA"
        numeric(14_2) precio_venta "sin IVA"
        numeric(5_2) porcentaje_iva "0, 5 o 19"
        integer stock ">= 0"
        integer stock_minimo ">= 0"
        boolean activo "borrado lógico"
        bigint categoria_id FK
    }
    CLIENTES {
        bigint id PK
        varchar(20) documento UK "cédula o NIT"
        varchar(120) nombre
        varchar(20) telefono
        varchar(120) email
        varchar(200) direccion
    }
    PROVEEDORES {
        bigint id PK
        varchar(20) nit UK
        varchar(150) razon_social
        varchar(20) telefono
        varchar(120) email
        varchar(200) direccion
    }
    VENTAS {
        bigint id PK
        varchar(20) numero UK "FV-000001"
        timestamp fecha
        bigint cliente_id FK "nulo = consumidor final"
        numeric(14_2) subtotal
        numeric(14_2) total_iva
        numeric(14_2) total
        varchar(15) estado "COMPLETADA | ANULADA"
    }
    DETALLES_VENTA {
        bigint id PK
        bigint venta_id FK
        bigint producto_id FK
        integer cantidad "> 0"
        numeric(14_2) precio_unitario "precio del momento"
        numeric(5_2) porcentaje_iva
        numeric(14_2) subtotal
        numeric(14_2) valor_iva
        numeric(14_2) total
    }
    COMPRAS {
        bigint id PK
        varchar(40) numero_factura "UK con proveedor_id"
        timestamp fecha
        bigint proveedor_id FK
        numeric(14_2) subtotal
        numeric(14_2) total_iva
        numeric(14_2) total
    }
    DETALLES_COMPRA {
        bigint id PK
        bigint compra_id FK
        bigint producto_id FK
        integer cantidad "> 0"
        numeric(14_2) costo_unitario
        numeric(5_2) porcentaje_iva
        numeric(14_2) subtotal
        numeric(14_2) valor_iva
        numeric(14_2) total
    }
    MOVIMIENTOS_INVENTARIO {
        bigint id PK
        bigint producto_id FK
        varchar(10) tipo "ENTRADA | SALIDA"
        varchar(25) concepto "INVENTARIO_INICIAL | COMPRA | VENTA | ANULACION_VENTA | AJUSTE"
        integer cantidad "> 0"
        integer stock_anterior
        integer stock_resultante
        varchar(40) referencia "FV-..., factura, AJUSTE"
        varchar(255) observacion
        timestamp fecha
    }
```

## Tablas

| Tabla | Propósito | Claves e índices |
|---|---|---|
| `categorias` | Agrupa productos | `nombre` único |
| `productos` | Catálogo con precios, IVA y stock actual | `codigo` único · FK `categoria_id` |
| `clientes` | Compradores identificados | `documento` único |
| `proveedores` | Quienes nos facturan | `nit` único |
| `ventas` | Cabecera de la factura de venta | `numero` único · FK `cliente_id` (opcional) |
| `detalles_venta` | Renglones de la venta, con el precio congelado | FK `venta_id`, `producto_id` |
| `compras` | Factura recibida de un proveedor | único (`proveedor_id`, `numero_factura`) |
| `detalles_compra` | Renglones de la compra | FK `compra_id`, `producto_id` |
| `movimientos_inventario` | Kardex: cada cambio de stock con su saldo antes y después | FK `producto_id` · índice (`producto_id`, `fecha`) |

Montos en `numeric(14,2)`, redondeo `HALF_UP` a 2 decimales (`util/Montos`). Fechas en hora local del servidor (`America/Bogota`).

## Reglas de negocio y capa responsable

Capas: **BD** = restricción en PostgreSQL (migración) · **Servicio** = `com.minierp.service` · **DTO** = Bean Validation en `*Request` · **Repositorio** = consulta/bloqueo en `com.minierp.repository` · **Front** = solo experiencia de usuario, nunca la única barrera.

| # | Regla | Capa principal | Refuerzo |
|---|---|---|---|
| R1 | El stock solo cambia por inventario inicial, compra, venta, anulación o ajuste, y cada cambio deja un movimiento en el kardex | Servicio (`InventarioService`, transacción `MANDATORY`) | BD: RLS impide escribir desde la API pública de Supabase |
| R2 | El stock nunca queda negativo | Servicio (`InventarioService.registrarSalida` → 422) | BD: `check (stock >= 0)` |
| R3 | Una venta es atómica: si un producto falla, no se descuenta nada | Servicio (`@Transactional` en `VentaService.registrar`) | — |
| R4 | Dos ventas simultáneas no pueden vender la misma unidad | Repositorio (`findByIdParaActualizar`, `SELECT … FOR UPDATE`) | Servicio: productos en orden de id para evitar deadlocks |
| R5 | Precios, IVA y totales los calcula el backend; el cliente solo envía `productoId` y `cantidad` (y el costo en compras) | DTO (`VentaRequest` sin precios) + Servicio | — |
| R6 | El detalle conserva el precio y el IVA del momento | Servicio (copia a `DetalleVenta`/`DetalleCompra`) | — |
| R7 | Una venta anulada devuelve sus unidades y no se puede anular dos veces | Servicio (`VentaService.anular`, venta bloqueada con `FOR UPDATE`) | BD: `check (estado in (...))` |
| R8 | No se vende un producto inactivo | Servicio (`VentaService.registrar` → 422) | Front: el buscador solo muestra activos |
| R9 | Cada compra suma stock y actualiza el último costo (`precio_compra`) | Servicio (`CompraService.registrar`) | — |
| R10 | Un proveedor no puede repetir número de factura | Servicio (→ 409) | BD: `unique (proveedor_id, numero_factura)` |
| R11 | Un producto no puede repetirse en la misma compra | Servicio (→ 422) | — |
| R12 | El precio de venta no puede ser menor al de compra | Servicio (`ProductoService.validarPrecios` → 422) | BD: `check (precio_venta >= precio_compra)` no se aplica porque una compra puede subir el costo por encima del precio de venta vigente |
| R13 | Código de producto, nombre de categoría, documento de cliente y NIT son únicos | Servicio (→ 409, sin distinguir mayúsculas en código y categoría) | BD: `unique` |
| R14 | Los productos no se borran, se desactivan | Servicio (`ProductoService.desactivar`) | BD: FK sin `on delete cascade` |
| R15 | No se eliminan categorías, clientes ni proveedores con registros asociados | Servicio (→ 409) | BD: FK `on delete restrict` |
| R16 | Cantidades positivas y acotadas; IVA entre 0 y 100; precios > 0 | DTO (`@Positive`, `@Max`, `@DecimalMin/Max`) | BD: `check` equivalentes |
| R17 | Venta sin cliente = consumidor final (documento 222222222222 en la factura) | Servicio/DTO (`VentaResponse`) | Front (factura imprimible) |
| R18 | Los datos solo son accesibles a través de la API de Spring Boot | BD (RLS activo sin políticas para `anon`/`authenticated`, esquema no expuesto) | Infra: credenciales solo en variables de entorno de Render |
