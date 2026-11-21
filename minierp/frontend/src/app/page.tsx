"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "@/lib/api";
import { formatoDia, formatoMoneda, mensajeError } from "@/lib/format";
import type { Dashboard } from "@/lib/types";
import { Alerta, Cargando, PageHeader, StatCard } from "@/components/ui";

export default function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.get<Dashboard>("/dashboard").then(setData).catch((e) => setError(mensajeError(e)));
  }, []);

  if (error) return <Alerta mensaje={error} />;
  if (!data) return <Cargando />;

  const grafica = data.ventasUltimos7Dias.map((d) => ({ dia: formatoDia(d.fecha), total: d.total }));

  return (
    <>
      <PageHeader titulo="Dashboard" descripcion="Resumen del negocio">
        <Link href="/ventas/nueva" className="btn btn-primario">
          Nueva venta
        </Link>
      </PageHeader>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard titulo="Ventas de hoy" valor={formatoMoneda(data.ventasHoy)} />
        <StatCard
          titulo="Ventas del mes"
          valor={formatoMoneda(data.ventasMes)}
          detalle={`${data.numeroVentasMes} ventas · ticket promedio ${formatoMoneda(data.ticketPromedioMes)}`}
        />
        <StatCard titulo="Valor del inventario" valor={formatoMoneda(data.valorInventario)} detalle="Calculado a precio de compra" />
        <StatCard
          titulo="Productos con stock bajo"
          valor={String(data.productosStockBajo)}
          detalle="En o por debajo del mínimo"
          alerta={data.productosStockBajo > 0}
        />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-3">
        <div className="tarjeta xl:col-span-2">
          <h2 className="mb-4 font-semibold text-slate-900">Ventas de los últimos 7 días</h2>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={grafica}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="dia" tickLine={false} axisLine={false} fontSize={12} />
                <YAxis
                  tickLine={false}
                  axisLine={false}
                  fontSize={12}
                  width={60}
                  tickFormatter={(v: number) => `$${Math.round(v / 1000)}k`}
                />
                <Tooltip formatter={(v) => formatoMoneda(Number(v))} cursor={{ fill: "#eef2ff" }} />
                <Bar dataKey="total" name="Total" fill="#4f46e5" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="tarjeta">
          <h2 className="mb-4 font-semibold text-slate-900">Más vendidos del mes</h2>
          {data.topProductos.length === 0 ? (
            <p className="text-sm text-slate-500">Aún no hay ventas este mes.</p>
          ) : (
            <ol className="space-y-3">
              {data.topProductos.map((p, i) => (
                <li key={p.productoId} className="flex items-center justify-between gap-3 text-sm">
                  <span className="flex items-center gap-3">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-indigo-100 text-xs font-bold text-indigo-700">
                      {i + 1}
                    </span>
                    {p.nombre}
                  </span>
                  <span className="text-right">
                    <span className="block font-semibold">{p.cantidadVendida} und</span>
                    <span className="text-xs text-slate-500">{formatoMoneda(p.totalVendido)}</span>
                  </span>
                </li>
              ))}
            </ol>
          )}
        </div>
      </div>

      {data.alertasStock.length > 0 && (
        <div className="tarjeta mt-6 p-0">
          <div className="flex items-center justify-between px-5 py-4">
            <h2 className="font-semibold text-slate-900">Alertas de stock</h2>
            <Link href="/productos?stockBajo=true" className="btn-enlace">
              Ver todos
            </Link>
          </div>
          <table className="tabla">
            <thead>
              <tr>
                <th>Producto</th>
                <th>Categoría</th>
                <th className="text-right">Stock</th>
                <th className="text-right">Mínimo</th>
              </tr>
            </thead>
            <tbody>
              {data.alertasStock.map((p) => (
                <tr key={p.id}>
                  <td>
                    <Link href={`/productos/${p.id}/kardex`} className="btn-enlace">
                      {p.nombre}
                    </Link>
                  </td>
                  <td>{p.categoriaNombre}</td>
                  <td className="text-right font-semibold text-amber-600">{p.stock}</td>
                  <td className="text-right">{p.stockMinimo}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
