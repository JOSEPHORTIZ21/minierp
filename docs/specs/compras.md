# Spec: Compras

**Historia:** como administrador, quiero registrar la factura de un proveedor para sumar el inventario y mantener al día el costo de cada producto.

Operaciones: `POST /api/compras`, `GET /api/compras`, `GET /api/compras/{id}` ([contrato](../contratos/api.md#compras)). Reglas: R1, R9–R11.

## Criterios de aceptación del back

1. **Compra válida**
   - Dado un producto con stock 5, `precioCompra=1 800` e IVA 5 %
   - Cuando envío `POST /api/compras` con `{proveedorId, numeroFactura: "fac-10", items: [{productoId, cantidad: 10, costoUnitario: 2000}]}`
   - Entonces responde 201 con `numeroFactura="FAC-10"`, `subtotal=20000.00`, `totalIva=1000.00`, `total=21000.00`
   - Y el stock queda en 15 y `precioCompra` en 2 000
   - Y el kardex tiene un movimiento `ENTRADA`/`COMPRA` con `referencia="FAC-10"`.
2. **Factura repetida:** registrar otra vez `FAC-10` (o `fac-10`) para el mismo proveedor responde 409 y no suma stock. El mismo número con otro proveedor sí se acepta.
3. **Producto repetido:** el mismo `productoId` dos veces en `items` responde 422.
4. **Atomicidad:** si un ítem referencia un producto inexistente, responde 404 y ningún otro producto de la compra cambia su stock.
5. **Validación:** sin proveedor, sin número de factura, `items` vacío o con más de 100 elementos, `cantidad` fuera de 1..100 000 o `costoUnitario` ≤ 0 o con más de 2 decimales responde 400.
6. **Inexistentes:** un `proveedorId` inexistente responde 404.
7. **Detalle:** `GET /api/compras/{id}` devuelve los renglones con el costo y el IVA del momento de la compra.
