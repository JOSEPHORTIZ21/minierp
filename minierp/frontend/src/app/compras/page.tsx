"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api, query } from "@/lib/api";
import { formatoFecha, formatoMoneda, mensajeError } from "@/lib/format";
import type { CompraResumen, Page } from "@/lib/types";
import { Alerta, Cargando, FilaVacia, PageHeader, Paginacion } from "@/components/ui";

export default function ComprasPage() {
  const [data, setData] = useState<Page<CompraResumen> | null>(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Page<CompraResumen>>(`/compras${query({ page, size: 10 })}`)
      .then(setData)
      .catch((e) => setError(mensajeError(e)));
  }, [page]);

  return (
    <>
      <PageHeader titulo="Compras" descripcion="Facturas de proveedores: cada compra suma stock">
        <Link href="/compras/nueva" className="btn btn-primario">
          Registrar compra
        </Link>
      </PageHeader>

      <Alerta mensaje={error} onCerrar={() => setError(null)} />

      <div className="tarjeta overflow-x-auto p-0">
        {!data ? (
          <Cargando />
        ) : (
          <>
            <table className="tabla">
              <thead>
                <tr>
                  <th>Factura</th>
                  <th>Fecha</th>
                  <th>Proveedor</th>
                  <th className="text-right">Subtotal</th>
                  <th className="text-right">IVA</th>
                  <th className="text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 && <FilaVacia columnas={6} texto="Aún no hay compras registradas" />}
                {data.content.map((c) => (
                  <tr key={c.id}>
                    <td className="font-mono text-xs">{c.numeroFactura}</td>
                    <td className="whitespace-nowrap">{formatoFecha(c.fecha)}</td>
                    <td>{c.proveedorNombre}</td>
                    <td className="text-right">{formatoMoneda(c.subtotal)}</td>
                    <td className="text-right">{formatoMoneda(c.totalIva)}</td>
                    <td className="text-right font-semibold">{formatoMoneda(c.total)}</td>
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
