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
        <label>
          <span className="label">Desde</span>
          <input type="date" className="input" value={desde} onChange={(e) => setDesde(e.target.value)} />
        </label>
        <label>
          <span className="label">Hasta</span>
          <input type="date" className="input" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        </label>
        <button className="btn btn-secundario" type="submit">
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
            <table className="tabla">
              <thead>
                <tr>
                  <th>Número</th>
                  <th>Fecha</th>
                  <th>Cliente</th>
                  <th className="text-right">Subtotal</th>
                  <th className="text-right">IVA</th>
                  <th className="text-right">Total</th>
                  <th>Estado</th>
                  <th className="text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 && <FilaVacia columnas={8} />}
                {data.content.map((v) => (
                  <tr key={v.id}>
                    <td className="font-mono text-xs">
                      <Link href={`/ventas/${v.id}`} className="btn-enlace">
                        {v.numero}
                      </Link>
                    </td>
                    <td className="whitespace-nowrap">{formatoFecha(v.fecha)}</td>
                    <td>{v.clienteNombre}</td>
                    <td className="text-right">{formatoMoneda(v.subtotal)}</td>
                    <td className="text-right">{formatoMoneda(v.totalIva)}</td>
                    <td className="text-right font-semibold">{formatoMoneda(v.total)}</td>
                    <td>
                      <BadgeEstado estado={v.estado} />
                    </td>
                    <td>
                      <div className="flex justify-end gap-3">
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
