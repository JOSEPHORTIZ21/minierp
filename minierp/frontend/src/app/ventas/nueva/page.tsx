"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { api, query } from "@/lib/api";
import { formatoMoneda, mensajeError } from "@/lib/format";
import type { Cliente, Page, Producto, Venta } from "@/lib/types";
import BuscadorProductos from "@/components/BuscadorProductos";
import { Alerta, PageHeader } from "@/components/ui";

interface ItemCarrito {
  producto: Producto;
  cantidad: number;
}

/** Punto de venta: buscar productos, armar el carrito y facturar. */
export default function NuevaVentaPage() {
  const router = useRouter();
  const [carrito, setCarrito] = useState<ItemCarrito[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [clienteId, setClienteId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    api
      .get<Page<Cliente>>(`/clientes${query({ size: 100, sort: "nombre" })}`)
      .then((p) => setClientes(p.content))
      .catch((e) => setError(mensajeError(e)));
  }, []);

  function agregar(p: Producto) {
    setError(null);
    if (p.stock <= 0) {
      setError(`"${p.nombre}" no tiene stock disponible`);
      return;
    }
    setCarrito((actual) => {
      const existe = actual.find((i) => i.producto.id === p.id);
      if (existe) {
        return actual.map((i) => (i.producto.id === p.id ? { ...i, cantidad: Math.min(i.cantidad + 1, p.stock) } : i));
      }
      return [...actual, { producto: p, cantidad: 1 }];
    });
  }

  function cambiarCantidad(id: number, valor: number) {
    setCarrito((actual) =>
      actual.map((i) => (i.producto.id === id ? { ...i, cantidad: Math.max(1, Math.min(valor || 1, i.producto.stock)) } : i)),
    );
  }

  const quitar = (id: number) => setCarrito((actual) => actual.filter((i) => i.producto.id !== id));

  // Vista previa: el valor definitivo lo calcula el backend
  const totales = useMemo(
    () =>
      carrito.reduce(
        (acc, i) => {
          const subtotal = i.producto.precioVenta * i.cantidad;
          const iva = Math.round(subtotal * i.producto.porcentajeIva) / 100;
          return { subtotal: acc.subtotal + subtotal, iva: acc.iva + iva };
        },
        { subtotal: 0, iva: 0 },
      ),
    [carrito],
  );

  async function confirmar() {
    if (carrito.length === 0) return;
    setGuardando(true);
    setError(null);
    try {
      const venta = await api.post<Venta>("/ventas", {
        clienteId: clienteId ? Number(clienteId) : null,
        items: carrito.map((i) => ({ productoId: i.producto.id, cantidad: i.cantidad })),
      });
      router.push(`/ventas/${venta.id}`);
    } catch (e) {
      setError(mensajeError(e));
      setGuardando(false);
    }
  }

  return (
    <>
      <PageHeader titulo="Punto de venta" descripcion="Busca productos y agrégalos a la venta" />
      <Alerta mensaje={error} onCerrar={() => setError(null)} />

      <div className="grid gap-6 xl:grid-cols-3">
        <div className="space-y-6 xl:col-span-2">
          <BuscadorProductos onSeleccionar={agregar} />

          <div className="tarjeta overflow-x-auto p-0">
            <table className="tabla">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="text-right">Precio</th>
                  <th className="w-28 text-center">Cantidad</th>
                  <th className="text-right">IVA</th>
                  <th className="text-right">Subtotal</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {carrito.length === 0 && (
                  <tr>
                    <td colSpan={6} className="py-10 text-center text-slate-500">
                      El carrito está vacío
                    </td>
                  </tr>
                )}
                {carrito.map((i) => (
                  <tr key={i.producto.id}>
                    <td>
                      <p className="font-medium">{i.producto.nombre}</p>
                      <p className="text-xs text-slate-500">Disponible: {i.producto.stock}</p>
                    </td>
                    <td className="text-right">{formatoMoneda(i.producto.precioVenta)}</td>
                    <td>
                      <input
                        type="number"
                        min={1}
                        max={i.producto.stock}
                        value={i.cantidad}
                        onChange={(e) => cambiarCantidad(i.producto.id, Number(e.target.value))}
                        className="input text-center"
                      />
                    </td>
                    <td className="text-right">{i.producto.porcentajeIva}%</td>
                    <td className="text-right font-medium">{formatoMoneda(i.producto.precioVenta * i.cantidad)}</td>
                    <td className="text-right">
                      <button onClick={() => quitar(i.producto.id)} className="text-lg text-slate-400 hover:text-red-600" aria-label="Quitar">
                        ×
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="tarjeta h-fit space-y-5">
          <h2 className="font-semibold text-slate-900">Resumen</h2>
          <label className="block">
            <span className="label">Cliente</span>
            <select className="input" value={clienteId} onChange={(e) => setClienteId(e.target.value)}>
              <option value="">Consumidor final</option>
              {clientes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre} — {c.documento}
                </option>
              ))}
            </select>
          </label>

          <dl className="space-y-2 border-t border-slate-100 pt-4 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">Subtotal</dt>
              <dd>{formatoMoneda(totales.subtotal)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">IVA</dt>
              <dd>{formatoMoneda(totales.iva)}</dd>
            </div>
            <div className="flex justify-between border-t border-slate-100 pt-2 text-lg font-bold">
              <dt>Total</dt>
              <dd>{formatoMoneda(totales.subtotal + totales.iva)}</dd>
            </div>
          </dl>

          <button className="btn btn-primario w-full py-3 text-base" disabled={carrito.length === 0 || guardando} onClick={confirmar}>
            {guardando ? "Registrando..." : "Confirmar venta"}
          </button>
          {carrito.length > 0 && (
            <button className="btn btn-secundario w-full" onClick={() => setCarrito([])} disabled={guardando}>
              Vaciar carrito
            </button>
          )}
        </div>
      </div>
    </>
  );
}
