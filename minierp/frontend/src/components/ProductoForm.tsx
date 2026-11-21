"use client";

import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { api, erroresCampos } from "@/lib/api";
import { aNumero, mensajeError } from "@/lib/format";
import type { Categoria, Producto, ProductoRequest } from "@/lib/types";
import { Alerta, Campo } from "@/components/ui";

/** Formulario compartido para crear y editar productos. */
export default function ProductoForm({ producto }: { producto?: Producto }) {
  const router = useRouter();
  const esEdicion = Boolean(producto);

  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [form, setForm] = useState({
    codigo: producto?.codigo ?? "",
    nombre: producto?.nombre ?? "",
    descripcion: producto?.descripcion ?? "",
    precioCompra: producto ? String(producto.precioCompra) : "",
    precioVenta: producto ? String(producto.precioVenta) : "",
    porcentajeIva: producto ? String(producto.porcentajeIva) : "19",
    stockInicial: "0",
    stockMinimo: producto ? String(producto.stockMinimo) : "5",
    categoriaId: producto ? String(producto.categoriaId) : "",
  });
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    api.get<Categoria[]>("/categorias").then(setCategorias).catch((e) => setError(mensajeError(e)));
  }, []);

  const cambiar = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const compra = Number(form.precioCompra);
  const venta = Number(form.precioVenta);
  const margen = compra > 0 && venta > 0 ? ((venta - compra) / venta) * 100 : null;

  async function guardar(e: FormEvent) {
    e.preventDefault();
    setGuardando(true);
    setError(null);
    setErrores({});

    const body: ProductoRequest = {
      codigo: form.codigo,
      nombre: form.nombre,
      descripcion: form.descripcion || null,
      precioCompra: aNumero(form.precioCompra),
      precioVenta: aNumero(form.precioVenta),
      porcentajeIva: aNumero(form.porcentajeIva),
      stockInicial: esEdicion ? null : aNumero(form.stockInicial),
      stockMinimo: aNumero(form.stockMinimo),
      categoriaId: aNumero(form.categoriaId),
    };

    try {
      if (producto) await api.put(`/productos/${producto.id}`, body);
      else await api.post("/productos", body);
      router.push("/productos");
    } catch (err) {
      setError(mensajeError(err));
      setErrores(erroresCampos(err));
      setGuardando(false);
    }
  }

  return (
    <form onSubmit={guardar} className="tarjeta max-w-3xl space-y-5">
      <Alerta mensaje={error} onCerrar={() => setError(null)} />

      <div className="grid gap-4 sm:grid-cols-3">
        <Campo label="Código" error={errores.codigo}>
          <input name="codigo" value={form.codigo} onChange={cambiar} className="input uppercase" placeholder="ABA-001" />
        </Campo>
        <div className="sm:col-span-2">
          <Campo label="Nombre" error={errores.nombre}>
            <input name="nombre" value={form.nombre} onChange={cambiar} className="input" />
          </Campo>
        </div>
      </div>

      <Campo label="Descripción" error={errores.descripcion}>
        <textarea name="descripcion" value={form.descripcion} onChange={cambiar} className="input" rows={2} />
      </Campo>

      <div className="grid gap-4 sm:grid-cols-3">
        <Campo label="Precio de compra (sin IVA)" error={errores.precioCompra}>
          <input name="precioCompra" type="number" min="0" step="any" value={form.precioCompra} onChange={cambiar} className="input" />
        </Campo>
        <Campo label="Precio de venta (sin IVA)" error={errores.precioVenta}>
          <input name="precioVenta" type="number" min="0" step="any" value={form.precioVenta} onChange={cambiar} className="input" />
        </Campo>
        <Campo label="IVA" error={errores.porcentajeIva}>
          <select name="porcentajeIva" value={form.porcentajeIva} onChange={cambiar} className="input">
            <option value="0">0 % (excluido)</option>
            <option value="5">5 %</option>
            <option value="19">19 %</option>
          </select>
        </Campo>
      </div>
      {margen !== null && (
        <p className={`text-sm ${margen < 0 ? "text-red-600" : "text-slate-500"}`}>Margen de ganancia: {margen.toFixed(1)} %</p>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <Campo label="Categoría" error={errores.categoriaId}>
          <select name="categoriaId" value={form.categoriaId} onChange={cambiar} className="input">
            <option value="">Selecciona...</option>
            {categorias.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </select>
        </Campo>
        {!esEdicion && (
          <Campo label="Stock inicial" error={errores.stockInicial}>
            <input name="stockInicial" type="number" min="0" value={form.stockInicial} onChange={cambiar} className="input" />
          </Campo>
        )}
        <Campo label="Stock mínimo (alerta)" error={errores.stockMinimo}>
          <input name="stockMinimo" type="number" min="0" value={form.stockMinimo} onChange={cambiar} className="input" />
        </Campo>
      </div>
      {esEdicion && (
        <p className="text-xs text-slate-500">
          El stock no se edita aquí: cambia con compras, ventas o ajustes desde el kardex del producto.
        </p>
      )}

      <div className="flex flex-col-reverse gap-2 border-t border-slate-100 pt-4 sm:flex-row sm:justify-end">
        <button type="button" className="btn btn-secundario" onClick={() => router.push("/productos")}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-primario" disabled={guardando}>
          {guardando ? "Guardando..." : esEdicion ? "Guardar cambios" : "Crear producto"}
        </button>
      </div>
    </form>
  );
}
