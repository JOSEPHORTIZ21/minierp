"use client";

import { Suspense, useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { api, query } from "@/lib/api";
import { puede } from "@/lib/permisos";
import { useSesion } from "@/lib/sesion";
import { formatoMoneda, mensajeError } from "@/lib/format";
import type { Categoria, Page, Producto } from "@/lib/types";
import { Alerta, Cargando, FilaVacia, PageHeader, Paginacion } from "@/components/ui";

export default function ProductosPage() {
  // useSearchParams necesita un límite de Suspense en Next.js
  return (
    <Suspense fallback={<Cargando />}>
      <ListadoProductos />
    </Suspense>
  );
}

function ListadoProductos() {
  const searchParams = useSearchParams();
  const rol = useSesion()?.usuario.rol ?? "CAJERO";
  const gestiona = puede.gestionarCatalogo(rol);
  const [data, setData] = useState<Page<Producto> | null>(null);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [texto, setTexto] = useState("");
  const [buscar, setBuscar] = useState("");
  const [categoriaId, setCategoriaId] = useState("");
  const [soloStockBajo, setSoloStockBajo] = useState(searchParams.get("stockBajo") === "true");
  const [incluirInactivos, setIncluirInactivos] = useState(false);
  const [page, setPage] = useState(0);
  const [recarga, setRecarga] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.get<Categoria[]>("/categorias").then(setCategorias).catch(() => {});
  }, []);

  useEffect(() => {
    api
      .get<Page<Producto>>(
        `/productos${query({
          buscar,
          categoriaId,
          stockBajo: soloStockBajo || undefined,
          incluirInactivos: incluirInactivos || undefined,
          page,
          size: 10,
          sort: "nombre",
        })}`,
      )
      .then(setData)
      .catch((e) => setError(mensajeError(e)));
  }, [buscar, categoriaId, soloStockBajo, incluirInactivos, page, recarga]);

  function aplicarBusqueda(e: FormEvent) {
    e.preventDefault();
    setPage(0);
    setBuscar(texto);
  }

  async function cambiarEstado(p: Producto) {
    if (p.activo && !confirm(`¿Desactivar "${p.nombre}"? No aparecerá en ventas, pero se conserva su historial.`)) return;
    try {
      if (p.activo) await api.delete(`/productos/${p.id}`);
      else await api.patch(`/productos/${p.id}/activar`);
      setRecarga((r) => r + 1);
    } catch (e) {
      setError(mensajeError(e));
    }
  }

  return (
    <>
      <PageHeader titulo="Productos" descripcion="Catálogo e inventario">
        {gestiona && (
          <Link href="/productos/nuevo" className="btn btn-primario">
            Nuevo producto
          </Link>
        )}
      </PageHeader>

      <Alerta mensaje={error} onCerrar={() => setError(null)} />

      <div className="tarjeta mb-4 flex flex-wrap items-center gap-3">
        <form onSubmit={aplicarBusqueda} className="flex w-full gap-2 sm:w-auto sm:min-w-64 sm:flex-1">
          <input className="input" placeholder="Buscar por nombre o código..." value={texto} onChange={(e) => setTexto(e.target.value)} />
          <button className="btn btn-secundario" type="submit">
            Buscar
          </button>
        </form>
        <select
          className="input w-full sm:w-48"
          value={categoriaId}
          onChange={(e) => {
            setCategoriaId(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Todas las categorías</option>
          {categorias.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </select>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={soloStockBajo}
            onChange={(e) => {
              setSoloStockBajo(e.target.checked);
              setPage(0);
            }}
          />
          Solo stock bajo
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={incluirInactivos}
            onChange={(e) => {
              setIncluirInactivos(e.target.checked);
              setPage(0);
            }}
          />
          Incluir inactivos
        </label>
      </div>

      <div className="tarjeta overflow-x-auto p-0">
        {!data ? (
          <Cargando />
        ) : (
          <>
            <table className="tabla tabla-tarjetas">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nombre</th>
                  <th>Categoría</th>
                  <th className="text-right md:max-xl:hidden">P. compra</th>
                  <th className="text-right">P. venta</th>
                  <th className="text-right md:max-xl:hidden">IVA</th>
                  <th className="text-right">Stock</th>
                  <th className="text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 && <FilaVacia columnas={8} />}
                {data.content.map((p) => (
                  <tr key={p.id} className={p.activo ? "" : "opacity-50"}>
                    <td data-label="Código" className="font-mono text-xs">{p.codigo}</td>
                    <td className="celda-titulo font-medium">
                      {p.nombre}
                      {!p.activo && <span className="ml-2 text-xs text-slate-500">(inactivo)</span>}
                    </td>
                    <td data-label="Categoría">{p.categoriaNombre}</td>
                    <td data-label="P. compra" className="text-right md:max-xl:hidden">{formatoMoneda(p.precioCompra)}</td>
                    <td data-label="P. venta" className="text-right">{formatoMoneda(p.precioVenta)}</td>
                    <td data-label="IVA" className="text-right md:max-xl:hidden">{p.porcentajeIva}%</td>
                    <td data-label="Stock" className="text-right">
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          p.stockBajo ? "bg-amber-100 text-amber-700" : "bg-slate-100 text-slate-700"
                        }`}
                      >
                        {p.stock}
                      </span>
                    </td>
                    <td>
                      <div className="flex justify-end gap-4 py-1 md:gap-3 md:py-0">
                        <Link href={`/productos/${p.id}/kardex`} className="btn-enlace">
                          Kardex
                        </Link>
                        {gestiona && (
                          <>
                            <Link href={`/productos/${p.id}/editar`} className="btn-enlace">
                              Editar
                            </Link>
                            <button onClick={() => cambiarEstado(p)} className={p.activo ? "text-sm font-medium text-red-600 hover:underline" : "btn-enlace"}>
                              {p.activo ? "Desactivar" : "Activar"}
                            </button>
                          </>
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
