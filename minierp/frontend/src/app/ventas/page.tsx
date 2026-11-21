"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { api, query } from "@/lib/api";
import { formatoFecha, formatoMoneda, mensajeError } from "@/lib/format";
import type { Page, VentaResumen } from "@/lib/types";
import { Alerta, BadgeEstado, Cargando, FilaVacia, PageHeader, Paginacion } from "@/components/ui";

export default function VentasPage() {
  const [data, setData] = useState<Page<VentaResumen> | null>(null);
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [filtros, setFiltros] = useState({ desde: "", hasta: "" });
  const [page, setPage] = useState(0);
  const [recarga, setRecarga] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Page<VentaResumen>>(`/ventas${query({ desde: filtros.desde, hasta: filtros.hasta, page, size: 10 })}`)
      .then(setData)
      .catch((e) => setError(mensajeError(e)));
  }, [filtros, page, recarga]);

  function filtrar(e: FormEvent) {
    e.preventDefault();
    setPage(0);
    setFiltros({ desde, hasta });
  }

  async function anular(v: VentaResumen) {
    if (!confirm(`¿Anular la venta ${v.numero}? Las unidades vuelven al inventario.`)) return;
    try {
      await api.patch(`/ventas/${v.id}/anular`);
      setRecarga((r) => r + 1);
    } catch (e) {
      setError(mensajeError(e));
    }
  }

  return (
    <>
      <PageHeader titulo="Ventas" descripcion="Historial de facturas">
        <Link href="/ventas/nueva" className="btn btn-primario">
          Nueva venta
        </Link>
      </PageHeader>

      <Alerta mensaje={error} onCerrar={() => setError(null)} />

      <form onSubmit={filtrar} className="tarjeta mb-4 flex flex-wrap items-end gap-3">
        <label className="min-w-36 flex-1 sm:flex-none">
          <span className="label">Desde</span>
          <input type="date" className="input" value={desde} onChange={(e) => setDesde(e.target.value)} />
        </label>
        <label className="min-w-36 flex-1 sm:flex-none">
          <span className="label">Hasta</span>
          <input type="date" className="input" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        </label>
        <button className="btn btn-secundario w-full sm:w-auto" type="submit">
          Filtrar
        </button>
        {(filtros.desde || filtros.hasta) && (
          <button
            type="button"
            className="btn-enlace"
            onClick={() => {
              setDesde("");
              setHasta("");
              setFiltros({ desde: "", hasta: "" });
            }}
          >
            Limpiar
          </button>
        )}
      </form>

      <div className="tarjeta overflow-x-auto p-0">
        {!data ? (
          <Cargando />
        ) : (
          <>
            <table className="tabla tabla-tarjetas">
              <thead>
                <tr>
                  <th>Número</th>
                  <th>Fecha</th>
                  <th>Cliente</th>
                  <th className="text-right md:max-xl:hidden">Subtotal</th>
                  <th className="text-right md:max-xl:hidden">IVA</th>
                  <th className="text-right">Total</th>
                  <th>Estado</th>
                  <th className="text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 && <FilaVacia columnas={8} />}
                {data.content.map((v) => (
                  <tr key={v.id}>
                    <td className="celda-titulo whitespace-nowrap font-mono text-xs">
                      <span className="flex items-center justify-between gap-3">
                        <Link href={`/ventas/${v.id}`} className="btn-enlace">
                          {v.numero}
                        </Link>
                        <span className="md:hidden">
                          <BadgeEstado estado={v.estado} />
                        </span>
                      </span>
                    </td>
                    <td data-label="Fecha" className="whitespace-nowrap">{formatoFecha(v.fecha)}</td>
                    <td data-label="Cliente">{v.clienteNombre}</td>
                    <td data-label="Subtotal" className="text-right md:max-xl:hidden">{formatoMoneda(v.subtotal)}</td>
                    <td data-label="IVA" className="text-right md:max-xl:hidden">{formatoMoneda(v.totalIva)}</td>
                    <td data-label="Total" className="text-right font-semibold">{formatoMoneda(v.total)}</td>
                    <td className="hidden md:table-cell">
                      <BadgeEstado estado={v.estado} />
                    </td>
                    <td>
                      <div className="flex justify-end gap-4 py-1 md:gap-3 md:py-0">
                        <Link href={`/ventas/${v.id}`} className="btn-enlace">
                          Ver
                        </Link>
                        {v.estado === "COMPLETADA" && (
                          <button className="text-sm font-medium text-red-600 hover:underline" onClick={() => anular(v)}>
                            Anular
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Paginacion pagina={data} onCambiar={setPage} />
          </>
        )}
      </div>
    </>
  );
}
