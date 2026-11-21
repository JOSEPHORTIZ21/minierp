# Spec: Catálogos (categorías, clientes, proveedores)

**Historia:** como administrador, quiero mantener categorías, clientes y proveedores sin perder la integridad de las ventas y compras ya registradas.

Operaciones: `/api/categorias`, `/api/clientes`, `/api/proveedores` ([contrato](../contratos/api.md#categorías)). Reglas: R13, R15.

## Criterios de aceptación del back

1. **CRUD básico:** crear responde 201, consultar y editar responden 200, eliminar responde 204; un `{id}` inexistente responde 404 en consultar, editar y eliminar.
2. **Únicos:** repetir el nombre de categoría (sin distinguir mayúsculas), el documento de cliente o el NIT de proveedor, al crear o al editar otro registro, responde 409. Editar un registro conservando su propio valor sí se acepta.
3. **Limpieza de texto:** los campos opcionales enviados como `""` o con solo espacios se guardan como `null`; los obligatorios se guardan recortados.
4. **Borrado protegido:** eliminar una categoría con productos, un cliente con ventas o un proveedor con compras responde 409 y no borra nada.
5. **Validación:** campos obligatorios vacíos, textos más largos que su límite o un email mal formado responden 400 con `errores` por campo.
6. **Búsqueda:** `GET /api/clientes?buscar=` coincide con nombre (sin distinguir mayúsculas) o documento; `GET /api/proveedores?buscar=` con razón social o NIT.
7. **Errores sin filtrar detalles internos:** ninguna respuesta de error incluye trazas, nombres de tablas ni SQL; las violaciones de restricción de la base se responden como 409 con un mensaje genérico.
