"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { api, erroresCampos } from "@/lib/api";
import { aNumero, formatoFecha, formatoMoneda, mensajeError } from "@/lib/format";
import type { ConceptoMovimiento, Movimiento, Producto, TipoMovimiento } from "@/lib/types";
import { Alerta, Campo, Cargando, FilaVacia, PageHeader, StatCard } from "@/components/ui";

const conceptos: Record<ConceptoMovimiento, string> = {
  INVENTARIO_INICIAL: "Inventario inicial",
  COMPRA: "Compra",
  VENTA: "Venta",
  ANULACION_VENTA: "Anulación de venta",
  AJUSTE: "Ajuste manual",
};

export default function KardexPage() {
  const { id } = useParams<{ id: string }>();
  const [producto, setProducto] = useState<Producto | null>(null);
  const [movimientos, setMovimientos] = useState<Movimiento[]>([]);
  const [recarga, setRecarga] = useState(0);
  const [ajuste, setAjuste] = useState<{ tipo: TipoMovimiento; cantidad: string; observacion: string }>({
    tipo: "ENTRADA",
    cantidad: "",
    observacion: "",
  });
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([api.get<Producto>(`/productos/${id}`), api.get<Movimiento[]>(`/productos/${id}/kardex`)])
      .then(([p, m]) => {
        setProducto(p);
        setMovimientos(m);
      })
      .catch((e) => setError(mensajeError(e)));
  }, [id, recarga]);

  async function registrarAjuste(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setExito(null);
    setErrores({});
    try {
      await api.post(`/productos/${id}/ajustes`, {
        tipo: ajuste.tipo,
        cantidad: aNumero(ajuste.cantidad),
        observacion: ajuste.observacion,
      });
      setExito("Ajuste registrado");
      setAjuste({ tipo: "ENTRADA", cantidad: "", observacion: "" });
      setRecarga((r) => r + 1);
    } catch (err) {
      setError(mensajeError(err));
      setErrores(erroresCampos(err));
    }
  }

  if (!producto) return error ? <Alerta mensaje={error} /> : <Cargando />;

  return (
    <>
      <PageHeader titulo={`Kardex · ${producto.nombre}`} descripcion={`Código ${producto.codigo} · ${producto.categoriaNombre}`}>
        <Link href="/productos" className="btn btn-secundario">
          Volver
        </Link>
      </PageHeader>

      <Alerta mensaje={error} onCerrar={() => setError(null)} />
      <Alerta tipo="exito" mensaje={exito} onCerrar={() => setExito(null)} />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard titulo="Stock actual" valor={String(producto.stock)} detalle={`Mínimo: ${producto.stockMinimo}`} alerta={producto.stockBajo} />
        <StatCard titulo="Valor en inventario" valor={formatoMoneda(producto.stock * producto.precioCompra)} detalle="A precio de compra" />
        <StatCard titulo="Movimientos" valor={String(movimientos.length)} />
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        <div className="tarjeta overflow-x-auto p-0 xl:col-span-2">
          <table className="tabla">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Concepto</th>
                <th>Referencia</th>
                <th className="text-right">Entrada</th>
                <th className="text-right">Salida</th>
                <th className="text-right">Saldo</th>
              </tr>
            </thead>
            <tbody>
              {movimientos.length === 0 && <FilaVacia columnas={6} texto="Sin movimientos" />}
              {movimientos.map((m) => (
                <tr key={m.id}>
                  <td className="whitespace-nowrap">{formatoFecha(m.fecha)}</td>
                  <td>
                    {conceptos[m.concepto]}
                    {m.observacion && <span className="block text-xs text-slate-500">{m.observacion}</span>}
                  </td>
                  <td className="font-mono text-xs">{m.referencia}</td>
                  <td className="text-right text-emerald-600">{m.tipo === "ENTRADA" ? `+${m.cantidad}` : ""}</td>
                  <td className="text-right text-red-600">{m.tipo === "SALIDA" ? `-${m.cantidad}` : ""}</td>
                  <td className="text-right font-semibold">{m.stockResultante}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <form onSubmit={registrarAjuste} className="tarjeta h-fit space-y-4">
          <h2 className="font-semibold text-slate-900">Ajuste de inventario</h2>
          <p className="text-xs text-slate-500">Úsalo para conteos físicos, productos dañados o vencidos.</p>
          <Campo label="Tipo" error={errores.tipo}>
            <select className="input" value={ajuste.tipo} onChange={(e) => setAjuste({ ...ajuste, tipo: e.target.value as TipoMovimiento })}>
              <option value="ENTRADA">Entrada (+)</option>
              <option value="SALIDA">Salida (−)</option>
            </select>
          </Campo>
          <Campo label="Cantidad" error={errores.cantidad}>
            <input className="input" type="number" min="1" value={ajuste.cantidad} onChange={(e) => setAjuste({ ...ajuste, cantidad: e.target.value })} />
          </Campo>
          <Campo label="Motivo" error={errores.observacion}>
            <input
              className="input"
              placeholder="Ej: 2 unidades vencidas"
              value={ajuste.observacion}
              onChange={(e) => setAjuste({ ...ajuste, observacion: e.target.value })}
            />
          </Campo>
          <button className="btn btn-primario w-full" type="submit">
            Registrar ajuste
          </button>
        </form>
      </div>
    </>
  );
}
