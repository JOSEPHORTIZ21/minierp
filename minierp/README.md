# Mini-ERP · Inventario y facturación

Proyecto integrador del Diplomado de Desarrollo Web Full Stack.

| Capa | Tecnología |
|---|---|
| Backend | Java 17 · Spring Boot 3.3 · Spring Data JPA · Bean Validation · springdoc (Swagger) |
| Base de datos | H2 (por defecto) · MySQL 8 (perfil `mysql`) |
| Frontend | Next.js 15 (App Router) · React 19 · TypeScript · Tailwind CSS 4 · Recharts |

```
minierp/
├── backend/            API REST (puerto 8080)
├── frontend/           Aplicación web (puerto 3000)
└── docker-compose.yml  MySQL opcional con Docker
```

---

## 1. Cómo ejecutarlo

### Requisitos
- JDK 17 o superior, Maven (o el de tu IDE: IntelliJ, VS Code o STS)
- Node.js 20 o superior
- No necesitas instalar base de datos: por defecto se usa **H2**, que viene incluida.

### Backend
1. Ejecuta:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
2. Las tablas se crean solas en la carpeta `data/`. La primera vez se cargan **datos de ejemplo** (categorías, 10 productos, clientes, proveedores y 3 ventas). Para reiniciar la base desde cero, detén la app y borra la carpeta `data`.
3. Documentación interactiva de la API: **http://localhost:8080/swagger-ui.html**
4. Consola de la base de datos: **http://localhost:8080/h2-console** (JDBC URL `jdbc:h2:file:./data/minierp`, usuario `sa`, sin contraseña).

### Usar MySQL (opcional)
1. Ten MySQL corriendo (XAMPP, servicio de Windows o `docker compose up -d`).
2. Ajusta usuario y contraseña en `application-mysql.properties`.
3. Arranca con el perfil `mysql`:
   - IntelliJ: *Run → Edit Configurations → Active profiles:* `mysql`
   - Consola: `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`

### Frontend
```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```
Abre **http://localhost:3000**

---

## Despliegue en producción

```
Navegador ──▶ Vercel (frontend) ──▶ Render (backend, Docker) ──▶ Supabase (PostgreSQL)
```

### 1. Supabase (base de datos)
1. Crea un proyecto en supabase.com y **guarda la contraseña de la base de datos**.
2. Clic en **Connect** → **Session pooler** (puerto 5432). No uses el Transaction pooler (6543): Hibernate usa prepared statements.
3. Toma de ahí el host, el usuario (`postgres.<project-ref>`) y arma:
   `jdbc:postgresql://<HOST>:5432/postgres?sslmode=require`

Las tablas se crean solas en el esquema `minierp` al primer arranque del backend.

### 2. Render (backend)
1. **New → Web Service** → conecta el repositorio de GitHub.
2. **Root Directory:** `backend` · **Language/Runtime:** Docker · **Instance:** Free.
3. Variables de entorno:

   | Key | Value |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://<HOST>:5432/postgres?sslmode=require` |
   | `DB_USERNAME` | `postgres.<project-ref>` |
   | `DB_PASSWORD` | contraseña de la base de Supabase |
   | `APP_CORS_ALLOWED_ORIGINS` | `https://<tu-app>.vercel.app,http://localhost:3000` |

4. **Health Check Path** (en *Advanced*): `/api/categorias`
5. Al terminar, prueba `https://<tu-servicio>.onrender.com/swagger-ui.html`

> El plan gratuito se apaga tras 15 min sin uso; la primera petición después tarda en responder mientras arranca.

### 3. Vercel (frontend)
1. **Add New → Project** → mismo repositorio → **Root Directory:** `frontend`.
2. Variable `NEXT_PUBLIC_API_URL` = `https://<tu-servicio>.onrender.com/api`
3. Deploy. Si después cambias la variable, haz **Redeploy**.

---

## 2. Modelo de datos

```mermaid
erDiagram
    CATEGORIA ||--o{ PRODUCTO : agrupa
    PRODUCTO ||--o{ MOVIMIENTO_INVENTARIO : "tiene kardex"
    PRODUCTO ||--o{ DETALLE_VENTA : "se vende en"
    PRODUCTO ||--o{ DETALLE_COMPRA : "se compra en"
    CLIENTE |o--o{ VENTA : realiza
    VENTA ||--|{ DETALLE_VENTA : contiene
    PROVEEDOR ||--o{ COMPRA : factura
    COMPRA ||--|{ DETALLE_COMPRA : contiene

    CATEGORIA {
        Long id PK
        String nombre UK
        String descripcion
    }
    PRODUCTO {
        Long id PK
        String codigo UK
        String nombre
        BigDecimal precioCompra
        BigDecimal precioVenta
        BigDecimal porcentajeIva
        Integer stock
        Integer stockMinimo
        Boolean activo
        Long categoria_id FK
    }
    CLIENTE {
        Long id PK
        String documento UK
        String nombre
        String telefono
        String email
    }
    PROVEEDOR {
        Long id PK
        String nit UK
        String razonSocial
        String telefono
        String email
    }
    VENTA {
        Long id PK
        String numero UK
        LocalDateTime fecha
        Long cliente_id FK
        BigDecimal subtotal
        BigDecimal totalIva
        BigDecimal total
        String estado
    }
    DETALLE_VENTA {
        Long id PK
        Long venta_id FK
        Long producto_id FK
        Integer cantidad
        BigDecimal precioUnitario
        BigDecimal valorIva
        BigDecimal total
    }
    COMPRA {
        Long id PK
        String numeroFactura
        LocalDateTime fecha
        Long proveedor_id FK
        BigDecimal total
    }
    DETALLE_COMPRA {
        Long id PK
        Long compra_id FK
        Long producto_id FK
        Integer cantidad
        BigDecimal costoUnitario
        BigDecimal total
    }
    MOVIMIENTO_INVENTARIO {
        Long id PK
        Long producto_id FK
        String tipo
        String concepto
        Integer cantidad
        Integer stockAnterior
        Integer stockResultante
        String referencia
        LocalDateTime fecha
    }
```

> GitHub y VS Code (con extensión Mermaid) dibujan este diagrama automáticamente.

---

## 3. Reglas de negocio implementadas

| Regla | Dónde |
|---|---|
| El stock **solo** cambia por ventas, compras, anulaciones o ajustes, y cada cambio queda en el kardex | `InventarioService` |
| Una venta es atómica: si un producto no tiene stock, no se descuenta nada (`@Transactional`) | `VentaService.registrar` |
| Bloqueo pesimista (`SELECT … FOR UPDATE`) para que dos ventas simultáneas no vendan el mismo stock | `ProductoRepository.findByIdParaActualizar` |
| Los productos se procesan ordenados por id para evitar deadlocks | `VentaService`, `CompraService` |
| Los precios e IVA los calcula el backend; el frontend solo envía producto y cantidad | `VentaRequest` |
| El detalle guarda el precio del momento (si el producto cambia de precio, la factura no cambia) | `DetalleVenta` |
| Anular una venta devuelve las unidades al inventario | `VentaService.anular` |
| Cada compra suma stock y actualiza el último costo del producto | `CompraService` |
| No se repite una factura del mismo proveedor | `CompraService` + restricción única |
| El precio de venta no puede ser menor al de compra | `ProductoService` |
| Borrado lógico de productos (pueden estar en facturas) | `ProductoService.desactivar` |
| No se eliminan categorías, clientes ni proveedores en uso | Servicios respectivos |
| IVA colombiano (0 %, 5 %, 19 %) y consumidor final con documento 222222222222 | `ProductoForm`, `VentaResponse` |

---

## 4. Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/dashboard` | Indicadores, ventas de 7 días, top productos, alertas |
| GET | `/api/productos?buscar=&categoriaId=&stockBajo=&page=&size=&sort=` | Listado con filtros y paginación |
| POST / PUT | `/api/productos`, `/api/productos/{id}` | Crear / editar |
| DELETE | `/api/productos/{id}` | Desactivar (borrado lógico) |
| PATCH | `/api/productos/{id}/activar` | Reactivar |
| GET | `/api/productos/{id}/kardex` | Historial de movimientos |
| POST | `/api/productos/{id}/ajustes` | Ajuste manual de inventario |
| GET / POST | `/api/ventas` | Listar (filtro `desde`, `hasta`) / registrar venta |
| GET | `/api/ventas/{id}` | Factura con detalle |
| PATCH | `/api/ventas/{id}/anular` | Anular y devolver stock |
| GET / POST | `/api/compras` | Listar / registrar compra |
| CRUD | `/api/categorias`, `/api/clientes`, `/api/proveedores` | Catálogos |

Ejemplo para registrar una venta:
```json
POST /api/ventas
{
  "clienteId": 1,
  "items": [
    { "productoId": 1, "cantidad": 3 },
    { "productoId": 5, "cantidad": 2 }
  ]
}
```

Todos los errores llegan con el mismo formato:
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "mensaje": "Stock insuficiente para 'Azúcar 1 kg': disponible 6, solicitado 10",
  "ruta": "/api/ventas"
}
```
Los errores de validación incluyen además `"errores": { "campo": "mensaje" }`, que el frontend muestra debajo de cada input.

---

## 5. Pantallas del frontend

| Ruta | Pantalla |
|---|---|
| `/` | Dashboard con indicadores y gráfica |
| `/ventas/nueva` | Punto de venta (buscador + carrito) |
| `/ventas`, `/ventas/[id]` | Historial y factura imprimible |
| `/compras`, `/compras/nueva` | Compras a proveedores |
| `/productos`, `/productos/nuevo`, `/productos/[id]/editar` | Catálogo |
| `/productos/[id]/kardex` | Kardex y ajustes de inventario |
| `/categorias`, `/clientes`, `/proveedores` | Catálogos |

---

## 6. Pruebas

```bash
cd backend
mvn test     # unitarias (Mockito), sin base de datos
mvn verify   # unitarias + integración: requiere Docker en ejecución
```
- **Unitarias** (`*Test`): reglas de stock, anulación y límites de validación, sin base de datos.
- **Integración** (`integracion/*IT`): levantan la aplicación completa contra PostgreSQL 16 en Docker (Testcontainers), creado con la migración de `../supabase/migrations/`, y verifican por HTTP los criterios de aceptación de `../docs/specs/`. Incluyen ventas y anulaciones simultáneas, el rollback de una venta fallida y que la migración tenga RLS en todas las tablas.

---

## 7. Ideas para seguir creciendo
- **Spring Security + JWT** con roles (admin, cajero). El código ya está separado en capas, así que se agrega sin rehacer nada.
- **MapStruct** para reemplazar los métodos `from()` de los DTOs.
- **Factura en PDF** desde el backend (OpenPDF o JasperReports).
- **Exportar reportes a Excel** (Apache POI).
- **Flyway** para versionar el esquema de la base de datos en lugar de `ddl-auto=update`.
- **Despliegue**: backend en Railway/Render, frontend en Vercel, base de datos en un MySQL administrado.
