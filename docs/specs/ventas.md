# Spec: Ventas

**Historia:** como cajero, quiero registrar una venta con varios productos para cobrarle al cliente y descontar el inventario.

Operaciones: `POST /api/ventas`, `PATCH /api/ventas/{id}/anular`, `GET /api/ventas`, `GET /api/ventas/{id}` ([contrato](../contratos/api.md#ventas)). Reglas: R3–R8, R17 ([modelo](../datos/modelo.md#reglas-de-negocio-y-capa-responsable)).

## Criterios de aceptación del back

1. **Venta válida**
   - Dado un producto activo con stock 10, precio 1 000 e IVA 19 %
   - Cuando envío `POST /api/ventas` con `{items: [{productoId, cantidad: 3}]}`
   - Entonces responde 201 con `numero` con formato `FV-######`, `estado=COMPLETADA`, `subtotal=3000.00`, `totalIva=570.00`, `total=3570.00`
   - Y el stock del producto queda en 7
   - Y el kardex tiene un movimiento `SALIDA`/`VENTA` con `stockAnterior=10`, `stockResultante=7` y `referencia` igual al número de la venta.
2. **Precios del servidor:** si el cuerpo incluye `precioUnitario` o `total`, se ignoran; los valores salen del producto.
3. **Atomicidad:** dados el producto A con stock 10 y el B con stock 1, cuando vendo 2 de A y 5 de B, entonces responde 422 con el mensaje de stock insuficiente de B, y ni A ni B cambian su stock ni su kardex, y no queda ninguna venta creada.
4. **Productos repetidos:** `items` con el mismo `productoId` dos veces (2 + 3) genera un solo detalle con cantidad 5.
5. **Consumidor final:** sin `clienteId`, la venta se registra con cliente nulo y el detalle de la venta muestra el documento `222222222222`.
6. **Producto inactivo:** vender un producto con `activo=false` responde 422 y no descuenta nada.
7. **Inexistentes:** un `clienteId` o `productoId` inexistente responde 404.
8. **Validación:** `items` vacío, más de 100 ítems, `cantidad` ≤ 0 o mayor que 100 000 responde 400 con `errores` por campo.
9. **Concurrencia:** dos ventas simultáneas por la última unidad de un producto: exactamente una responde 201 y la otra 422; el stock final es 0, nunca negativo.
10. **Anulación:** anular una venta `COMPLETADA` responde 200 con `estado=ANULADA`, devuelve las unidades al stock y crea movimientos `ENTRADA`/`ANULACION_VENTA`.
11. **Doble anulación:** anular una venta ya anulada responde 422 y no cambia el stock. Dos anulaciones simultáneas de la misma venta devuelven el stock una sola vez.
12. **Dashboard:** las ventas anuladas no suman en `ventasHoy`, `ventasMes` ni en el top de productos.
13. **Listado:** `GET /api/ventas?desde=2026-11-01&hasta=2026-11-30` solo devuelve ventas de ese rango, ordenadas de la más reciente a la más antigua.
