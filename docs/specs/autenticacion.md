# Spec: Autenticación y autorización

**Historia:** como dueño del negocio, quiero que solo personas autorizadas usen el sistema y que cada una vea y haga solo lo que corresponde a su rol, para proteger los datos de clientes, ventas e inventario.

Operaciones: `/api/auth/*`, `/api/usuarios/*` y la autorización de todas las demás ([contrato](../contratos/api.md#autenticación-y-usuarios)). Regla: R18 ([modelo](../datos/modelo.md#reglas-de-negocio-y-capa-responsable)).

| Rol | Puede |
|---|---|
| ADMIN | Todo, incluida la gestión de usuarios |
| CAJERO | Registrar ventas; consultar ventas, productos (y su kardex), categorías y clientes; registrar clientes nuevos |

## Criterios de aceptación del back

1. **Login válido:** `POST /api/auth/login` con correo y contraseña correctos responde 200 con `token`, `expiraEn` (8 horas después) y `usuario` (id, email, nombre, rol, activo), sin ningún campo de contraseña ni hash.
2. **Correo sin distinguir mayúsculas:** `" ADMIN@Minierp.Local "` inicia sesión igual que `admin@minierp.local`.
3. **Credenciales inválidas:** contraseña incorrecta o correo inexistente responden 401 con el **mismo** mensaje (no revela qué correos existen).
4. **Usuario desactivado:** no puede iniciar sesión (401) y un token que obtuvo antes deja de servir de inmediato (401), aunque no haya expirado.
5. **Límite de intentos:** tras 5 intentos fallidos con el mismo correo, el siguiente responde 429 con cabecera `Retry-After`, incluso si la contraseña es correcta.
6. **Sin sesión:** cualquier ruta de la API distinta de `POST /api/auth/login` y `GET /api/salud` responde 401 sin token, con un token mal formado, con la firma alterada, expirado o emitido por otro sistema. `GET /api/salud` responde 200 sin token.
7. **Permisos del CAJERO:** puede `GET` productos, kardex, categorías, clientes y ventas, y `POST` ventas y clientes. Recibe 403 en dashboard, escritura de productos y categorías, ajustes de inventario, edición y borrado de clientes, compras, proveedores, anulación de ventas y usuarios.
8. **El rol vigente manda:** si un ADMIN cambia el rol de un usuario, el siguiente pedido de ese usuario ya usa el rol nuevo, sin volver a iniciar sesión.
9. **Alta de usuarios:** el ADMIN crea usuarios (201). Correo repetido (sin distinguir mayúsculas) → 409. Contraseña de menos de 8 caracteres, sin letras o sin números → 400. Rol inexistente → 400.
10. **Nunca sin administrador:** un ADMIN no puede quitarse el rol ni desactivarse a sí mismo (422), y no se puede desactivar o degradar al último ADMIN activo (422).
11. **Cambio de contraseña propia:** con la contraseña actual incorrecta → 422; igual a la actual → 422; correcta → 204, la nueva sirve para iniciar sesión y la anterior ya no.
12. **Restablecer contraseña:** el ADMIN asigna una contraseña nueva a otro usuario (204) y ese usuario inicia sesión con ella.
13. **Sin datos sensibles:** ninguna respuesta de `/api/usuarios` ni `/api/auth` incluye la contraseña o su hash.
