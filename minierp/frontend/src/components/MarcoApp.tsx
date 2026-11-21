"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { inicioDe, rutaPermitida } from "@/lib/permisos";
import { actualizarUsuario, useSesion } from "@/lib/sesion";
import type { Usuario } from "@/lib/types";
import Sidebar from "@/components/Sidebar";

/** Solo se aceptan rutas internas para volver después del login (evita redirecciones a otros sitios). */
function destinoSeguro(valor: string | null): string | null {
  return valor && valor.startsWith("/") && !valor.startsWith("//") && valor !== "/login" ? valor : null;
}

function PantallaCarga() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <p className="animate-pulse text-sm text-slate-500">Cargando…</p>
    </div>
  );
}

/**
 * Marco de toda la aplicación:
 * - Sin sesión → lleva al login (recordando a qué página se quería ir).
 * - Con sesión en /login → lleva a la página pedida o al inicio de su rol.
 * - Con sesión → menú lateral + página, o un aviso si su rol no puede ver esa página.
 * Al cargar, consulta /auth/yo para usar el rol vigente (un administrador pudo cambiarlo).
 */
export default function MarcoApp({ children }: { children: ReactNode }) {
  const sesion = useSesion();
  const pathname = usePathname();
  const router = useRouter();
  const [montado, setMontado] = useState(false);
  const esLogin = pathname === "/login";
  const rol = sesion?.usuario.rol;

  useEffect(() => setMontado(true), []);

  useEffect(() => {
    if (!sesion) return;
    api
      .get<Usuario>("/auth/yo")
      .then(actualizarUsuario)
      .catch(() => {}); // un 401 ya cierra la sesión en lib/api
    // Solo al iniciar sesión o recargar, no en cada navegación
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sesion?.token]);

  useEffect(() => {
    if (!montado) return;
    if (!sesion && !esLogin) {
      const destino = pathname === "/" ? "" : `?siguiente=${encodeURIComponent(pathname + window.location.search)}`;
      router.replace(`/login${destino}`);
    } else if (sesion && esLogin) {
      const siguiente = destinoSeguro(new URLSearchParams(window.location.search).get("siguiente"));
      router.replace(siguiente && rutaPermitida(sesion.usuario.rol, siguiente.split("?")[0]) ? siguiente : inicioDe(sesion.usuario.rol));
    } else if (sesion && pathname === "/" && sesion.usuario.rol !== "ADMIN") {
      router.replace(inicioDe(sesion.usuario.rol));
    }
  }, [montado, sesion, esLogin, pathname, router]);

  // Antes de montar no se conoce la sesión (vive en el navegador): evita mostrar datos o el login por un instante
  if (!montado) return <PantallaCarga />;
  if (esLogin) return sesion ? <PantallaCarga /> : <>{children}</>;
  if (!sesion || !rol) return <PantallaCarga />;
  if (pathname === "/" && rol !== "ADMIN") return <PantallaCarga />;

  return (
    <div className="min-h-screen lg:flex">
      <Sidebar />
      <main className="min-w-0 flex-1 p-4 sm:p-6 lg:p-8 print:p-0">
        {rutaPermitida(rol, pathname) ? (
          children
        ) : (
          <div className="tarjeta mx-auto mt-10 max-w-md text-center">
            <p className="text-lg font-semibold text-slate-900">No tienes acceso a esta sección</p>
            <p className="mt-2 text-sm text-slate-500">Tu rol no permite ver esta página. Si lo necesitas, pídeselo a un administrador.</p>
            <Link href={inicioDe(rol)} className="btn btn-primario mt-5">
              Ir al inicio
            </Link>
          </div>
        )}
      </main>
    </div>
  );
}
