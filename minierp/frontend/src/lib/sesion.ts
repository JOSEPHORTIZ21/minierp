// Sesión del usuario: token JWT + datos para mostrar. Se guarda en localStorage para sobrevivir a recargas.
// El backend vuelve a verificar el token y el rol en cada petición: lo que hay aquí solo decide qué se muestra.
import { useSyncExternalStore } from "react";
import type { LoginResponse, Usuario } from "./types";

const CLAVE = "minierp.sesion";

export interface Sesion {
  token: string;
  expiraEn: string;
  usuario: Usuario;
}

const oyentes = new Set<() => void>();
let cache: { texto: string | null; sesion: Sesion | null } = { texto: null, sesion: null };

function avisar() {
  oyentes.forEach((o) => o());
}

function leerAlmacenada(): string | null {
  try {
    return localStorage.getItem(CLAVE);
  } catch {
    return null;
  }
}

/** Sesión vigente, o null si no hay o ya expiró. */
export function obtenerSesion(): Sesion | null {
  if (typeof window === "undefined") return null;
  const texto = leerAlmacenada();
  if (texto !== cache.texto) {
    let sesion: Sesion | null = null;
    try {
      sesion = texto ? (JSON.parse(texto) as Sesion) : null;
    } catch {
      sesion = null;
    }
    cache = { texto, sesion };
  }
  const s = cache.sesion;
  if (s && new Date(s.expiraEn).getTime() <= Date.now()) return null;
  return s;
}

export function guardarSesion(respuesta: LoginResponse) {
  try {
    localStorage.setItem(CLAVE, JSON.stringify({ token: respuesta.token, expiraEn: respuesta.expiraEn, usuario: respuesta.usuario }));
  } catch {
    // sin almacenamiento disponible (modo privado estricto): la sesión dura lo que la pestaña
  }
  avisar();
}

/** Actualiza los datos del usuario (por ejemplo, si un administrador le cambió el rol). */
export function actualizarUsuario(usuario: Usuario) {
  const s = obtenerSesion();
  if (!s) return;
  if (s.usuario.rol === usuario.rol && s.usuario.nombre === usuario.nombre && s.usuario.activo === usuario.activo) return;
  guardarSesion({ token: s.token, expiraEn: s.expiraEn, usuario });
}

export function cerrarSesion() {
  try {
    localStorage.removeItem(CLAVE);
  } catch {
    // nada que borrar
  }
  avisar();
}

function suscribir(oyente: () => void) {
  oyentes.add(oyente);
  // Cerrar sesión o iniciarla en otra pestaña se refleja en esta
  const alCambiarOtraPestana = (e: StorageEvent) => e.key === CLAVE && oyente();
  window.addEventListener("storage", alCambiarOtraPestana);
  return () => {
    oyentes.delete(oyente);
    window.removeEventListener("storage", alCambiarOtraPestana);
  };
}

/** Hook: la sesión actual, que se actualiza al iniciar o cerrar sesión (también desde otra pestaña). */
export function useSesion(): Sesion | null {
  return useSyncExternalStore(suscribir, obtenerSesion, () => null);
}
