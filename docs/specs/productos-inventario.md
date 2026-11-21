# Spec: Productos e inventario

**Historia:** como administrador, quiero mantener el catálogo y corregir el inventario con trazabilidad, para saber siempre por qué un producto tiene el stock que tiene.

Operaciones: `/api/productos`, `/api/productos/{id}/kardex`, `/api/productos/{id}/ajustes` ([contrato](../contratos/api.md#productos-e-inventario)). Reglas: R1, R2, R12–R14, R16.

## Criterios de aceptación del back

1. **Crear con stock inicial:** `POST /api/productos` con `codigo: "abc-1"` y `stockInicial: 20` responde 201 con `codigo="ABC-1"` y `stock=20`; el kardex tiene un movimiento `INVENTARIO_INICIAL` de 20 con `stockAnterior=0`.
2. **Crear sin stock inicial:** sin `stockInicial` (o en 0) el producto queda con stock 0 y sin movimientos.
3. **Código único:** crear o editar con un código que ya existe (sin distinguir mayúsculas) responde 409.
4. **Precios:** `precioVenta < precioCompra` responde 422. Precios ≤ 0 o con más de 2 decimales, o un IVA fuera de 0..100, responde 400.
5. **El stock no se edita:** `PUT /api/productos/{id}` con cualquier `stockInicial` no cambia `stock` ni crea movimientos.
6. **Borrado lógico:** `DELETE /api/productos/{id}` responde 204 y deja `activo=false`; el producto ya no aparece en `GET /api/productos`, pero sí con `incluirInactivos=true`, y sus ventas anteriores siguen consultables. `PATCH /activar` lo restablece.
7. **Filtros:** `stockBajo=true` devuelve solo productos con `stock ≤ stockMinimo`; `buscar` coincide con nombre o código sin distinguir mayúsculas; `categoriaId` filtra por categoría.
8. **Ajuste de entrada:** `POST /api/productos/{id}/ajustes` `{tipo: ENTRADA, cantidad: 5, observacion: "Conteo físico"}` responde 201 y suma 5 al stock con un movimiento `AJUSTE`.
9. **Ajuste de salida sin stock:** una salida mayor que el stock responde 422 y no cambia nada.
10. **Ajuste sin motivo:** sin `observacion` responde 400.
11. **Kardex coherente:** en `GET /api/productos/{id}/kardex`, para cada movimiento `stockResultante = stockAnterior ± cantidad`, y el `stockResultante` del último movimiento es igual al `stock` actual del producto.
12. **Paginación acotada:** `GET /api/productos?size=5000` devuelve como máximo 100 elementos. `sort=campoInexistente` responde 400, no 500.
