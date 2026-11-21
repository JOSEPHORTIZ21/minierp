"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import { formatoFecha, formatoMoneda, mensajeError } from "@/lib/format";
import type { Venta } from "@/lib/types";
import { Alerta, BadgeEstado, Cargando } from "@/components/ui";

/** Factura imprimible (Ctrl+P o el botón "Imprimir" → Guardar como PDF). */
export default function DetalleVentaPage() {
  const { id } = useParams<{ id: string }>();
  const [venta, setVenta] = useState<Venta | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.get<Venta>(`/ventas/${id}`).then(setVenta).catch((e) => setError(mensajeError(e)));
  }, [id]);

  async function anular() {
    if (!venta || !confirm(`¿Anular la venta ${venta.numero}? Las unidades vuelven al inventario.`)) return;
    try {
      setVenta(await api.patch<Venta>(`/ventas/${venta.id}/anular`));
    } catch (e) {
      setError(mensajeError(e));
    }
  }

  if (!venta) return error ? <Alerta mensaje={error} /> : <Cargando />;

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-4 flex flex-wrap justify-between gap-2 print:hidden">
        <Link href="/ventas" className="btn btn-secundario">
          ← Ventas
        </Link>
        <div className="flex gap-2">
          {venta.estado === "COMPLETADA" && (
            <button className="btn btn-peligro" onClick={anular}>
              Anular
            </button>
          )}
          <button className="btn btn-secundario" onClick={() => window.print()}>
            Imprimir
          </button>
          <Link href="/ventas/nueva" className="btn btn-primario">
            Nueva venta
          </Link>
        </div>
      </div>

      <Alerta mensaje={error} onCerrar={() => setError(null)} />

      <div className="tarjeta p-8 print:border-0 print:shadow-none">
        <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-200 pb-6">
          <div>
            <p className="text-xl font-bold text-slate-900">Mi Negocio S.A.S.</p>
            <p className="text-sm text-slate-500">NIT 900.000.000-0 · Villavicencio, Meta</p>
          </div>
          <div className="text-right">
            <p className="text-xs uppercase tracking-wide text-slate-500">Factura de venta</p>
            <p className="font-mono text-lg font-bold">{venta.numero}</p>
            <p className="text-sm text-slate-500">{formatoFecha(venta.fecha)}</p>
            <div className="mt-2">
              <BadgeEstado estado={venta.estado} />
            </div>
          </div>
        </div>

        <div className="py-5 text-sm">
          <p className="text-xs uppercase tracking-wide text-slate-500">Cliente</p>
          <p className="font-medium">{venta.clienteNombre}</p>
          <p className="text-slate-500">Doc. {venta.clienteDocumento}</p>
        </div>

        <table className="tabla">
          <thead>
            <tr>
              <th>Producto</th>
              <th className="text-right">Cant.</th>
              <th className="text-right">Precio</th>
              <th className="text-right">IVA</th>
              <th className="text-right">Total</th>
            </tr>
          </thead>
          <tbody>
            {venta.detalles.map((d) => (
              <tr key={d.id}>
                <td>
                  {d.productoNombre}
                  <span className="block font-mono text-xs text-slate-500">{d.productoCodigo}</span>
                </td>
                <td className="text-right">{d.cantidad}</td>
                <td className="text-right">{formatoMoneda(d.precioUnitario)}</td>
                <td className="text-right">
                  {formatoMoneda(d.valorIva)} <span className="text-xs text-slate-500">({d.porcentajeIva}%)</span>
                </td>
                <td className="text-right font-medium">{formatoMoneda(d.total)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <dl className="ml-auto mt-6 w-64 space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-500">Subtotal</dt>
            <dd>{formatoMoneda(venta.subtotal)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">IVA</dt>
            <dd>{formatoMoneda(venta.totalIva)}</dd>
          </div>
          <div className="flex justify-between border-t border-slate-200 pt-2 text-lg font-bold">
            <dt>Total</dt>
            <dd>{formatoMoneda(venta.total)}</dd>
          </div>
        </dl>
      </div>
    </div>
  );
}
