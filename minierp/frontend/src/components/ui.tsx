// Componentes pequeños reutilizables en toda la aplicación
import type { ReactNode } from "react";
import type { EstadoVenta, Page } from "@/lib/types";

export function PageHeader({ titulo, descripcion, children }: { titulo: string; descripcion?: string; children?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4 print:hidden">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{titulo}</h1>
        {descripcion && <p className="mt-1 text-sm text-slate-500">{descripcion}</p>}
      </div>
      {children && <div className="flex flex-wrap gap-2">{children}</div>}
    </div>
  );
}

export function Alerta({ tipo = "error", mensaje, onCerrar }: { tipo?: "error" | "exito"; mensaje: string | null; onCerrar?: () => void }) {
  if (!mensaje) return null;
  const estilos = tipo === "error" ? "border-red-200 bg-red-50 text-red-700" : "border-emerald-200 bg-emerald-50 text-emerald-700";
  return (
    <div className={`mb-4 flex items-start justify-between gap-4 rounded-lg border px-4 py-3 text-sm ${estilos}`}>
      <span>{mensaje}</span>
      {onCerrar && (
        <button type="button" onClick={onCerrar} className="font-bold leading-none" aria-label="Cerrar">
          ×
        </button>
      )}
    </div>
  );
}

export function Cargando({ texto = "Cargando..." }: { texto?: string }) {
  return <p className="animate-pulse py-10 text-center text-sm text-slate-500">{texto}</p>;
}

export function Campo({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      {children}
      {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
    </label>
  );
}

export function StatCard({ titulo, valor, detalle, alerta = false }: { titulo: string; valor: string; detalle?: string; alerta?: boolean }) {
  return (
    <div className={`tarjeta ${alerta ? "border-amber-300 bg-amber-50" : ""}`}>
      <p className="text-sm text-slate-500">{titulo}</p>
      <p className="mt-2 text-2xl font-bold text-slate-900">{valor}</p>
      {detalle && <p className="mt-1 text-xs text-slate-500">{detalle}</p>}
    </div>
  );
}

export function Paginacion({ pagina, onCambiar }: { pagina: Page<unknown>; onCambiar: (p: number) => void }) {
  if (pagina.totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between px-4 py-3 text-sm text-slate-600">
      <span>
        Página {pagina.page + 1} de {pagina.totalPages} · {pagina.totalElements} registros
      </span>
      <div className="flex gap-2">
        <button className="btn btn-secundario" disabled={pagina.first} onClick={() => onCambiar(pagina.page - 1)}>
          Anterior
        </button>
        <button className="btn btn-secundario" disabled={pagina.last} onClick={() => onCambiar(pagina.page + 1)}>
          Siguiente
        </button>
      </div>
    </div>
  );
}

export function BadgeEstado({ estado }: { estado: EstadoVenta }) {
  const estilos = estado === "COMPLETADA" ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700";
  return <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${estilos}`}>{estado === "COMPLETADA" ? "Completada" : "Anulada"}</span>;
}

export function FilaVacia({ columnas, texto = "No hay registros" }: { columnas: number; texto?: string }) {
  return (
    <tr>
      <td colSpan={columnas} className="py-10 text-center text-slate-500">
        {texto}
      </td>
    </tr>
  );
}
