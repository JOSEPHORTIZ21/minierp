"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

const secciones = [
  {
    titulo: "General",
    enlaces: [{ href: "/", texto: "Dashboard" }],
  },
  {
    titulo: "Operación",
    enlaces: [
      { href: "/ventas/nueva", texto: "Punto de venta" },
      { href: "/ventas", texto: "Ventas" },
      { href: "/compras", texto: "Compras" },
    ],
  },
  {
    titulo: "Inventario",
    enlaces: [
      { href: "/productos", texto: "Productos" },
      { href: "/categorias", texto: "Categorías" },
    ],
  },
  {
    titulo: "Terceros",
    enlaces: [
      { href: "/clientes", texto: "Clientes" },
      { href: "/proveedores", texto: "Proveedores" },
    ],
  },
];

/**
 * Navegación principal.
 * - Pantallas grandes (lg, ≥1024 px): barra lateral fija.
 * - Teléfonos y tablets: barra superior con botón de menú que abre la barra lateral como panel deslizable.
 */
export default function Sidebar() {
  const pathname = usePathname();
  const [abierto, setAbierto] = useState(false);

  // El enlace activo es el de ruta más larga que coincide (así /ventas/nueva no marca también /ventas)
  const activo = secciones
    .flatMap((s) => s.enlaces)
    .filter((e) => (e.href === "/" ? pathname === "/" : pathname === e.href || pathname.startsWith(e.href + "/")))
    .sort((a, b) => b.href.length - a.href.length)[0]?.href;
  const tituloActual = secciones.flatMap((s) => s.enlaces).find((e) => e.href === activo)?.texto ?? "Mini-ERP";

  // Cierra el panel al navegar
  useEffect(() => {
    setAbierto(false);
  }, [pathname]);

  // Con el panel abierto: Escape lo cierra y la página de fondo no se desplaza
  useEffect(() => {
    if (!abierto) return;
    const alPresionar = (e: KeyboardEvent) => e.key === "Escape" && setAbierto(false);
    document.addEventListener("keydown", alPresionar);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", alPresionar);
      document.body.style.overflow = "";
    };
  }, [abierto]);

  return (
    <>
      {/* Barra superior: solo en teléfonos y tablets */}
      <header className="sticky top-0 z-20 flex items-center gap-3 bg-slate-900 px-4 py-3 text-white shadow-md lg:hidden print:hidden">
        <button
          type="button"
          onClick={() => setAbierto(true)}
          className="-ml-1 flex h-10 w-10 items-center justify-center rounded-lg hover:bg-slate-800"
          aria-label="Abrir menú"
          aria-expanded={abierto}
          aria-controls="menu-principal"
        >
          <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" aria-hidden="true">
            <path d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
        <div className="min-w-0">
          <p className="truncate font-semibold leading-tight">{tituloActual}</p>
          <p className="text-xs leading-tight text-slate-400">Mini-ERP</p>
        </div>
      </header>

      {/* Fondo oscuro detrás del panel en móvil */}
      <div
        className={`fixed inset-0 z-30 bg-slate-950/50 transition-opacity lg:hidden print:hidden ${
          abierto ? "opacity-100" : "pointer-events-none opacity-0"
        }`}
        onClick={() => setAbierto(false)}
        aria-hidden="true"
      />

      <aside
        id="menu-principal"
        className={`fixed inset-y-0 left-0 z-40 flex w-64 max-w-[85vw] flex-col bg-slate-900 text-slate-300 shadow-xl transition-[transform,visibility] duration-200
          lg:sticky lg:top-0 lg:z-auto lg:h-screen lg:w-60 lg:max-w-none lg:shrink-0 lg:translate-x-0 lg:shadow-none lg:visible print:hidden
          ${abierto ? "visible translate-x-0" : "invisible -translate-x-full"}`}
      >
        <div className="flex items-center justify-between border-b border-slate-800 px-6 py-5">
          <div>
            <p className="text-lg font-bold text-white">Mini-ERP</p>
            <p className="text-xs text-slate-400">Inventario y facturación</p>
          </div>
          <button
            type="button"
            onClick={() => setAbierto(false)}
            className="-mr-2 flex h-10 w-10 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-800 hover:text-white lg:hidden"
            aria-label="Cerrar menú"
          >
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" aria-hidden="true">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </div>
        <nav className="flex-1 space-y-6 overflow-y-auto px-3 py-5">
          {secciones.map((seccion) => (
            <div key={seccion.titulo}>
              <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-slate-500">{seccion.titulo}</p>
              <ul className="space-y-1">
                {seccion.enlaces.map((enlace) => (
                  <li key={enlace.href}>
                    <Link
                      href={enlace.href}
                      aria-current={activo === enlace.href ? "page" : undefined}
                      className={`block rounded-lg px-3 py-2.5 text-sm transition-colors lg:py-2 ${
                        activo === enlace.href ? "bg-indigo-600 font-medium text-white" : "hover:bg-slate-800 hover:text-white"
                      }`}
                    >
                      {enlace.texto}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
      </aside>
    </>
  );
}
