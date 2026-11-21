"use client";

import { useEffect, useState, type FormEvent } from "react";
import { api, erroresCampos } from "@/lib/api";
import { mensajeError } from "@/lib/format";
import type { Categoria } from "@/lib/types";
import { mostrarFormulario } from "@/lib/dom";
import { Alerta, Campo,Cargando, FilaVacia, PageHeader } from "@/components/ui";

const vacio = { nombre: "", descripcion: "" };

export default function CategoriasPage() {
  const [categorias, setCategorias] = useState<Categoria[] | null>(null);
  const [form, setForm] = useState(vacio);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);
  const [recarga, setRecarga] = useState(0);

  useEffect(() => {
    api.get<Categoria[]>("/categorias").then(setCategorias).catch((e) => setError(mensajeError(e)));
  }, [recarga]);

  function editar(c: Categoria) {
    setEditandoId(c.id);
    setForm({ nombre: c.nombre, descripcion: c.descripcion ?? "" });
    setErrores({});
    mostrarFormulario("formulario-categoria");
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
      if (editandoId) await api.put(`/categorias/${editandoId}`, form);
      else await api.post("/categorias", form);
      setExito(editandoId ? "Categoría actualizada" : "Categoría creada");
      cancelar();
      setRecarga((r) => r + 1);
    } catch (err) {
      setError(mensajeError(err));
      setErrores(erroresCampos(err));
    }
  }

  async function eliminar(c: Categoria) {
    if (!confirm(`¿Eliminar la categoría "${c.nombre}"?`)) return;
    setError(null);
    try {
      await api.delete(`/categorias/${c.id}`);
      setRecarga((r) => r + 1);
    } catch (err) {
      setError(mensajeError(err));
    }
  }

  return (
    <>
      <PageHeader titulo="Categorías" descripcion="Agrupa los productos del catálogo" />
      <Alerta mensaje={error} onCerrar={() => setError(null)} />
      <Alerta tipo="exito" mensaje={exito} onCerrar={() => setExito(null)} />

      <div className="grid gap-6 xl:grid-cols-3">
        <div className="tarjeta overflow-x-auto p-0 xl:col-span-2">
          {!categorias ? (
            <Cargando />
          ) : (
            <table className="tabla tabla-tarjetas">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Descripción</th>
                  <th className="text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {categorias.length === 0 && <FilaVacia columnas={3} />}
                {categorias.map((c) => (
                  <tr key={c.id}>
                    <td className="celda-titulo font-medium">{c.nombre}</td>
                    <td className="text-left text-slate-500">{c.descripcion}</td>
                    <td>
                      <div className="flex justify-end gap-4 py-1 md:gap-3 md:py-0">
                        <button className="btn-enlace" onClick={() => editar(c)}>
                          Editar
                        </button>
                        <button className="text-sm font-medium text-red-600 hover:underline" onClick={() => eliminar(c)}>
                          Eliminar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <form id="formulario-categoria" onSubmit={guardar} className="tarjeta h-fit scroll-mt-20 space-y-4">
          <h2 className="font-semibold text-slate-900">{editandoId ? "Editar categoría" : "Nueva categoría"}</h2>
          <Campo label="Nombre" error={errores.nombre}>
            <input className="input" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
          </Campo>
          <Campo label="Descripción" error={errores.descripcion}>
            <textarea className="input" rows={3} value={form.descripcion} onChange={(e) => setForm({ ...form, descripcion: e.target.value })} />
          </Campo>
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
