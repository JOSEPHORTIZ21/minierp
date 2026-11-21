"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { api, erroresCampos, query } from "@/lib/api";
import { formatoMoneda, mensajeError } from "@/lib/format";
import type { Page, Producto, Proveedor } from "@/lib/types";
import BuscadorProductos from "@/components/BuscadorProductos";
import { Alerta, Campo, PageHeader } from "@/components/ui";

interface ItemCompra {
  producto: Producto;
  cantidad: number;
  costoUnitario: number;
}

export default function NuevaCompraPage() {
  const router = useRouter();
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [proveedorId, setProveedorId] = useState("");
  const [numeroFactura, setNumeroFactura] = useState("");
  const [items, setItems] = useState<ItemCompra[]>([]);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    api
      .get<Page<Proveedor>>(`/proveedores${query({ size: 100, sort: "razonSocial" })}`)
      .then((p) => setProveedores(p.content))
      .catch((e) => setError(mensajeError(e)));
  }, []);

  function agregar(p: Producto) {
    setItems((actual) =>
      actual.some((i) => i.producto.id === p.id)
        ? actual.map((i) => (i.producto.id === p.id ? { ...i, cantidad: i.cantidad + 1 } : i))
        : [...actual, { producto: p, cantidad: 1, costoUnitario: p.precioCompra }],
    );
  }

  function actualizar(id: number, cambios: Partial<Pick<ItemCompra, "cantidad" | "costoUnitario">>) {
    setItems((actual) => actual.map((i) => (i.producto.id === id ? { ...i, ...cambios } : i)));
  }

  const totales = useMemo(
    () =>
      items.reduce(
        (acc, i) => {
          const subtotal = i.costoUnitario * i.cantidad;
          return { subtotal: acc.subtotal + subtotal, iva: acc.iva + Math.round(subtotal * i.producto.porcentajeIva) / 100 };
        },
        { subtotal: 0, iva: 0 },
      ),
    [items],
  );

  async function registrar() {
    setGuardando(true);
    setError(null);
    setErrores({});
    try {
      await api.post("/compras", {
        proveedorId: proveedorId ? Number(proveedorId) : null,
        numeroFactura,
        items: items.map((i) => ({ productoId: i.producto.id, cantidad: i.cantidad, costoUnitario: i.costoUnitario })),
      });
      router.push("/compras");
    } catch (e) {
      setError(mensajeError(e));
      setErrores(erroresCampos(e));
      setGuardando(false);
    }
  }

  return (
    <>
      <PageHeader titulo="Registrar compra" descripcion="Ingresa la factura del proveedor: el stock y el costo se actualizan solos" />
      <Alerta mensaje={error} onCerrar={() => setError(null)} />

      <div className="grid gap-6 xl:grid-cols-3">
        <div className="space-y-6 xl:col-span-2">
          <BuscadorProductos onSeleccionar={agregar} mostrarCosto />

          <div className="tarjeta overflow-x-auto p-0">
            <table className="tabla">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="w-28 text-center">Cantidad</th>
                  <th className="w-40 text-center">Costo unitario</th>
                  <th className="text-right">Subtotal</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {items.length === 0 && (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-slate-500">
                      Agrega los productos de la factura
                    </td>
                  </tr>
                )}
                {items.map((i) => (
                  <tr key={i.producto.id}>
                    <td>
                      <p className="font-medium">{i.producto.nombre}</p>
                      <p className="text-xs text-slate-500">Stock actual: {i.producto.stock}</p>
                    </td>
                    <td>
                      <input
                        type="number"
                        min={1}
                        className="input text-center"
                        value={i.cantidad}
                        onChange={(e) => actualizar(i.producto.id, { cantidad: Math.max(1, Number(e.target.value) || 1) })}
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        min={0}
                        step="any"
                        className="input text-right"
                        value={i.costoUnitario}
                        onChange={(e) => actualizar(i.producto.id, { costoUnitario: Number(e.target.value) })}
                      />
                    </td>
                    <td className="text-right font-medium">{formatoMoneda(i.costoUnitario * i.cantidad)}</td>
                    <td className="text-right">
                      <button
                        onClick={() => setItems((a) => a.filter((x) => x.producto.id !== i.producto.id))}
                        className="text-lg text-slate-400 hover:text-red-600"
                        aria-label="Quitar"
                      >
                        ×
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="tarjeta h-fit space-y-4">
          <h2 className="font-semibold text-slate-900">Datos de la factura</h2>
          <Campo label="Proveedor" error={errores.proveedorId}>
            <select className="input" value={proveedorId} onChange={(e) => setProveedorId(e.target.value)}>
              <option value="">Selecciona...</option>
              {proveedores.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.razonSocial}
                </option>
              ))}
            </select>
          </Campo>
          <Campo label="Número de factura del proveedor" error={errores.numeroFactura}>
            <input className="input uppercase" value={numeroFactura} onChange={(e) => setNumeroFactura(e.target.value)} placeholder="FE-12345" />
          </Campo>

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

          <button className="btn btn-primario w-full py-3" disabled={items.length === 0 || guardando} onClick={registrar}>
            {guardando ? "Registrando..." : "Registrar compra"}
          </button>
        </div>
      </div>
    </>
  );
}
