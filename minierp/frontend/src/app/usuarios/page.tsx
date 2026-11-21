"use client";

import { useEffect, useState, type FormEvent } from "react";
import { api, erroresCampos, query } from "@/lib/api";
import { mostrarFormulario } from "@/lib/dom";
import { mensajeError } from "@/lib/format";
import { useSesion } from "@/lib/sesion";
import type { Page, Rol, Usuario } from "@/lib/types";
import { Alerta, Campo, Cargando, FilaVacia, PageHeader, Paginacion } from "@/components/ui";

const ROLES: Record<Rol, string> = { ADMIN: "Administrador", CAJERO: "Cajero" };
const vacio = { nombre: "", email: "", password: "", rol: "CAJERO" as Rol, activo: true };

/** Gestión de usuarios (solo ADMIN): alta, rol, activación y restablecer contraseña. */
export default function UsuariosPage() {
  const sesion = useSesion();
  const [data, setData] = useState<Page<Usuario> | null>(null);
  const [page, setPage] = useState(0);
  const [recarga, setRecarga] = useState(0);
  const [form, setForm] = useState(vacio);
  const [editando, setEditando] = useState<Usuario | null>(null);
  const [nuevaClave, setNuevaClave] = useState("");
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Page<Usuario>>(`/usuarios${query({ page, size: 20, sort: "nombre" })}`)
      .then(setData)
      .catch((e) => setError(mensajeError(e)));
  }, [page, recarga]);

  function editar(u: Usuario) {
    setEditando(u);
    setForm({ nombre: u.nombre, email: u.email, password: "", rol: u.rol, activo: u.activo });
    setNuevaClave("");
    setErrores({});
    setExito(null);
    mostrarFormulario("formulario-usuario");
  }

  function cancelar() {
    setEditando(null);
    setForm(vacio);
    setNuevaClave("");
    setErrores({});
  }

  async function guardar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setExito(null);
    setErrores({});
    try {
      if (editando) {
        await api.put(`/usuarios/${editando.id}`, { nombre: form.nombre, rol: form.rol, activo: form.activo });
        setExito(`Usuario ${form.email} actualizado`);
      } else {
        await api.post("/usuarios", { nombre: form.nombre, email: form.email, password: form.password, rol: form.rol });
        setExito(`Usuario ${form.email} creado. Compártele su contraseña por un medio seguro.`);
      }
      cancelar();
      setRecarga((r) => r + 1);
    } catch (err) {
      setError(mensajeError(err));
      setErrores(erroresCampos(err));
    }
  }

  async function restablecer() {
    if (!editando) return;
    setError(null);
    setExito(null);
    setErrores({});
    try {
      await api.put(`/usuarios/${editando.id}/clave`, { password: nuevaClave });
      setNuevaClave("");
      setExito(`Contraseña de ${editando.email} restablecida`);
    } catch (err) {
      setError(mensajeError(err));
      setErrores(erroresCampos(err));
    }
  }

  const esYo = editando?.id === sesion?.usuario.id;

  return (
    <>
      <PageHeader titulo="Usuarios" descripcion="Quién puede entrar al sistema y con qué rol" />
      <Alerta mensaje={error} onCerrar={() => setError(null)} />
      <Alerta tipo="exito" mensaje={exito} onCerrar={() => setExito(null)} />

      <div className="grid gap-6 xl:grid-cols-3">
        <div className="tarjeta overflow-x-auto p-0 xl:col-span-2">
          {!data ? (
            <Cargando />
          ) : (
            <>
              <table className="tabla tabla-tarjetas">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Correo</th>
                    <th>Rol</th>
                    <th>Estado</th>
                    <th className="text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.length === 0 && <FilaVacia columnas={5} />}
                  {data.content.map((u) => (
                    <tr key={u.id} className={u.activo ? "" : "opacity-60"}>
                      <td className="celda-titulo font-medium">
                        {u.nombre}
                        {u.id === sesion?.usuario.id && <span className="ml-2 text-xs font-normal text-slate-500">(tú)</span>}
                      </td>
                      <td data-label="Correo" className="break-all md:break-normal">{u.email}</td>
                      <td data-label="Rol">{ROLES[u.rol]}</td>
                      <td data-label="Estado">
                        <span
                          className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                            u.activo ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-600"
                          }`}
                        >
                          {u.activo ? "Activo" : "Inactivo"}
                        </span>
                      </td>
                      <td>
                        <div className="flex justify-end py-1 md:py-0">
                          <button className="btn-enlace" onClick={() => editar(u)}>
                            Editar
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

        <div id="formulario-usuario" className="h-fit scroll-mt-20 space-y-6">
          <form onSubmit={guardar} className="tarjeta space-y-4">
            <h2 className="font-semibold text-slate-900">{editando ? "Editar usuario" : "Nuevo usuario"}</h2>
            <Campo label="Nombre" error={errores.nombre}>
              <input className="input" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
            </Campo>
            <Campo label="Correo" error={errores.email}>
              <input
                type="email"
                className="input disabled:bg-slate-100 disabled:text-slate-500"
                autoComplete="off"
                value={form.email}
                disabled={!!editando}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </Campo>
            {!editando && (
              <Campo label="Contraseña inicial" error={errores.password}>
                <input
                  type="password"
                  className="input"
                  autoComplete="new-password"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                />
                <span className="mt-1 block text-xs text-slate-500">Mínimo 8 caracteres, con letras y números. El usuario puede cambiarla en “Mi cuenta”.</span>
              </Campo>
            )}
            <Campo label="Rol" error={errores.rol}>
              <select className="input" value={form.rol} disabled={esYo} onChange={(e) => setForm({ ...form, rol: e.target.value as Rol })}>
                <option value="CAJERO">Cajero — vende y consulta</option>
                <option value="ADMIN">Administrador — todo el sistema</option>
              </select>
            </Campo>
            {editando && (
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={form.activo} disabled={esYo} onChange={(e) => setForm({ ...form, activo: e.target.checked })} />
                Activo (puede iniciar sesión)
              </label>
            )}
            {esYo && <p className="text-xs text-slate-500">No puedes cambiar tu propio rol ni desactivar tu cuenta.</p>}
            <div className="flex gap-2">
              {editando && (
                <button type="button" className="btn btn-secundario flex-1" onClick={cancelar}>
                  Cancelar
                </button>
              )}
              <button type="submit" className="btn btn-primario flex-1">
                {editando ? "Guardar" : "Crear usuario"}
              </button>
            </div>
          </form>

          {editando && !esYo && (
            <div className="tarjeta space-y-3">
              <h2 className="font-semibold text-slate-900">Restablecer contraseña</h2>
              <p className="text-xs text-slate-500">Úsalo si {editando.nombre} olvidó su contraseña.</p>
              <Campo label="Nueva contraseña" error={errores.password}>
                <input type="password" className="input" autoComplete="new-password" value={nuevaClave}
                  onChange={(e) => setNuevaClave(e.target.value)} />
              </Campo>
              <button type="button" className="btn btn-secundario w-full" disabled={!nuevaClave} onClick={restablecer}>
                Restablecer contraseña
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
