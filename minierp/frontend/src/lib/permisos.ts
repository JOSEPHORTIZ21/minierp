// Permisos por rol en la interfaz. Reflejan las reglas de SecurityConfig en el backend, que es quien
// realmente las hace cumplir (403): aquí solo se ocultan opciones y se evita mostrar pantallas inútiles.
import type { Rol } from "./types";

/** Rutas a las que puede entrar un CAJERO. El ADMIN puede entrar a todas. */
const RUTAS_CAJERO = [
  /^\/ventas(\/nueva|\/\d+)?$/,
  /^\/productos$/,
  /^\/productos\/\d+\/kardex$/,
  /^\/clientes$/,
  /^\/cuenta$/,
];

export function rutaPermitida(rol: Rol, ruta: string): boolean {
  if (rol === "ADMIN") return true;
  return RUTAS_CAJERO.some((r) => r.test(ruta));
}

/** Página de inicio después de iniciar sesión. */
export function inicioDe(rol: Rol): string {
  return rol === "ADMIN" ? "/" : "/ventas/nueva";
}

/** Acciones dentro de las pantallas que el CAJERO puede ver. */
export const puede = {
  gestionarCatalogo: (rol: Rol) => rol === "ADMIN", // crear/editar/desactivar productos y categorías
  ajustarInventario: (rol: Rol) => rol === "ADMIN",
  anularVentas: (rol: Rol) => rol === "ADMIN",
  editarClientes: (rol: Rol) => rol === "ADMIN", // el CAJERO solo puede registrar clientes nuevos
};
