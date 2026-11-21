"use client";

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

export default function Sidebar() {
  const pathname = usePathname();

  // El enlace activo es el de ruta más larga que coincide (así /ventas/nueva no marca también /ventas)
  const activo = secciones
    .flatMap((s) => s.enlaces)
    .filter((e) => (e.href === "/" ? pathname === "/" : pathname === e.href || pathname.startsWith(e.href + "/")))
    .sort((a, b) => b.href.length - a.href.length)[0]?.href;

  return (
    <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col bg-slate-900 text-slate-300 md:flex print:hidden">
      <div className="border-b border-slate-800 px-6 py-5">
        <p className="text-lg font-bold text-white">Mini-ERP</p>
        <p className="text-xs text-slate-400">Inventario y facturación</p>
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
                    className={`block rounded-lg px-3 py-2 text-sm transition-colors ${
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
  );
}
