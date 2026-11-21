"use client";

import { useEffect, useState, type FormEvent } from "react";
import { api, erroresCampos, query } from "@/lib/api";
import { mensajeError } from "@/lib/format";
import type { Page } from "@/lib/types";
import { Alerta, Campo, Cargando, FilaVacia, PageHeader, Paginacion } from "@/components/ui";

interface CampoConfig {
  clave: string;
  etiqueta: string;
  tipo?: "text" | "email" | "tel";
  enTabla?: boolean;
}

interface Props {
  endpoint: string; // "/clientes" o "/proveedores"
  titulo: string;
  descripcion: string;
  singular: string; // "cliente", "proveedor"
  campos: CampoConfig[];
  ordenarPor: string;
  campoNombre: string; // campo que se muestra al confirmar la eliminación
}

type Registro = { id: number } & Record<string, string | number | null>;

/** CRUD genérico para clientes y proveedores (comparten la misma estructura). */
export default function CatalogoTerceros({ endpoint, titulo, descripcion, singular, campos, ordenarPor, campoNombre }: Props) {
  const vacio = Object.fromEntries(campos.map((c) => [c.clave, ""])) as Record<string, string>;

  const [data, setData] = useState<Page<Registro> | null>(null);
  const [page, setPage] = useState(0);
  const [texto, setTexto] = useState("");
  const [buscar, setBuscar] = useState("");
  const [form, setForm] = useState<Record<string, string>>(vacio);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);
  const [recarga, setRecarga] = useState(0);

  useEffect(() => {
    api
      .get<Page<Registro>>(`${endpoint}${query({ buscar, page, size: 10, sort: ordenarPor })}`)
      .then(setData)
      .catch((e) => setError(mensajeError(e)));
  }, [endpoint, ordenarPor, buscar, page, recarga]);

  function editar(r: Registro) {
    setEditandoId(r.id);
    setForm(Object.fromEntries(campos.map((c) => [c.clave, String(r[c.clave] ?? "")])));
    setErrores({});
  }

  function cancelar() {
    setEditandoId(null);
    setForm(vacio);
    setErrores({});
  }

  async function guardar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setExito(null);
    setErrores({});
    try {
      if (editandoId) await api.put(`${endpoint}/${editandoId}`, form);
      else await api.post(endpoint, form);
      setExito(editandoId ? `Se actualizó el ${singular}` : `Se creó el ${singular}`);
      cancelar();
      setRecarga((r) => r + 1);
    } catch (err) {
      setError(mensajeError(err));
      setErrores(erroresCampos(err));
    }
  }

  async function eliminar(r: Registro) {
    if (!confirm(`¿Eliminar el ${singular} "${r[campoNombre]}"?`)) return;
    setError(null);
    try {
      await api.delete(`${endpoint}/${r.id}`);
      setRecarga((x) => x + 1);
    } catch (err) {
      setError(mensajeError(err));
    }
  }

  const columnas = campos.filter((c) => c.enTabla !== false);

  return (
    <>
      <PageHeader titulo={titulo} descripcion={descripcion} />
      <Alerta mensaje={error} onCerrar={() => setError(null)} />
      <Alerta tipo="exito" mensaje={exito} onCerrar={() => setExito(null)} />

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <form
            className="mb-4 flex gap-2"
            onSubmit={(e) => {
              e.preventDefault();
              setPage(0);
              setBuscar(texto);
            }}
          >
            <input className="input" placeholder="Buscar..." value={texto} onChange={(e) => setTexto(e.target.value)} />
            <button className="btn btn-secundario" type="submit">
              Buscar
            </button>
          </form>

          <div className="tarjeta overflow-x-auto p-0">
            {!data ? (
              <Cargando />
            ) : (
              <>
                <table className="tabla">
                  <thead>
                    <tr>
                      {columnas.map((c) => (
                        <th key={c.clave}>{c.etiqueta}</th>
                      ))}
                      <th className="text-right">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.length === 0 && <FilaVacia columnas={columnas.length + 1} />}
                    {data.content.map((r) => (
                      <tr key={r.id}>
                        {columnas.map((c) => (
                          <td key={c.clave}>{r[c.clave] ?? "—"}</td>
                        ))}
                        <td>
                          <div className="flex justify-end gap-3">
                            <button className="btn-enlace" onClick={() => editar(r)}>
                              Editar
                            </button>
                            <button className="text-sm font-medium text-red-600 hover:underline" onClick={() => eliminar(r)}>
                              Eliminar
                            </button>
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
        </div>

        <form onSubmit={guardar} className="tarjeta h-fit space-y-4">
          <h2 className="font-semibold capitalize text-slate-900">{editandoId ? `Editar ${singular}` : `Nuevo ${singular}`}</h2>
          {campos.map((c) => (
            <Campo key={c.clave} label={c.etiqueta} error={errores[c.clave]}>
              <input
                className="input"
                type={c.tipo ?? "text"}
                value={form[c.clave]}
                onChange={(e) => setForm({ ...form, [c.clave]: e.target.value })}
              />
            </Campo>
          ))}
          <div className="flex gap-2">
            {editandoId && (
              <button type="button" className="btn btn-secundario flex-1" onClick={cancelar}>
                Cancelar
              </button>
            )}
            <button type="submit" className="btn btn-primario flex-1">
              {editandoId ? "Guardar" : "Crear"}
            </button>
          </div>
        </form>
      </div>
    </>
  );
}
