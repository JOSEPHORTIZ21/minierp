"use client";

import { useEffect, useState } from "react";
import { api, query } from "@/lib/api";
import { formatoMoneda } from "@/lib/format";
import type { Page, Producto } from "@/lib/types";

interface Props {
  onSeleccionar: (producto: Producto) => void;
  /** En compras se muestra el costo; en ventas, el precio de venta. */
  mostrarCosto?: boolean;
}

/** Buscador con "debounce": espera 300 ms después de escribir antes de consultar la API. */
export default function BuscadorProductos({ onSeleccionar, mostrarCosto = false }: Props) {
  const [texto, setTexto] = useState("");
  const [resultados, setResultados] = useState<Producto[]>([]);
  const [cargando, setCargando] = useState(false);

  useEffect(() => {
    const temporizador = setTimeout(() => {
      setCargando(true);
      api
        .get<Page<Producto>>(`/productos${query({ buscar: texto, size: 8, sort: "nombre" })}`)
        .then((p) => setResultados(p.content))
        .catch(() => setResultados([]))
        .finally(() => setCargando(false));
    }, 300);
    return () => clearTimeout(temporizador);
  }, [texto]);

  return (
    <div className="tarjeta">
      <input
        className="input"
        placeholder="Buscar producto por nombre o código..."
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
        autoFocus
      />
      <ul className="mt-3 divide-y divide-slate-100">
        {resultados.map((p) => (
          <li key={p.id}>
            <button
              type="button"
              onClick={() => onSeleccionar(p)}
              className="flex w-full items-center justify-between gap-3 rounded-lg px-2 py-2.5 text-left hover:bg-indigo-50"
            >
              <div>
                <p className="font-medium text-slate-800">{p.nombre}</p>
                <p className={`text-xs ${p.stockBajo ? "text-amber-600" : "text-slate-500"}`}>
                  {p.codigo} · Stock: {p.stock}
                </p>
              </div>
              <span className="text-sm font-semibold">{formatoMoneda(mostrarCosto ? p.precioCompra : p.precioVenta)}</span>
            </button>
          </li>
        ))}
        {!cargando && resultados.length === 0 && <li className="py-4 text-center text-sm text-slate-500">Sin resultados</li>}
      </ul>
    </div>
  );
}
