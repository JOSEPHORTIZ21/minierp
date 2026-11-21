const cop = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  maximumFractionDigits: 0,
});

export const formatoMoneda = (valor: number | null | undefined) => cop.format(valor ?? 0);

export const formatoFecha = (iso: string) =>
  new Date(iso).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });

/** "2026-11-21" → "vie 21" (sin problemas de zona horaria). */
export const formatoDia = (fechaIso: string) => {
  const [y, m, d] = fechaIso.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString("es-CO", { weekday: "short", day: "numeric" });
};

export function mensajeError(e: unknown): string {
  return e instanceof Error ? e.message : "Ocurrió un error inesperado";
}

/** Convierte el valor de un input a número, o null si está vacío. */
export const aNumero = (valor: string): number | null => (valor.trim() === "" ? null : Number(valor));
