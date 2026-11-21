"use client";

import { useState, type FormEvent } from "react";
import { api, erroresCampos } from "@/lib/api";
import { mensajeError } from "@/lib/format";
import { useSesion } from "@/lib/sesion";
import { Alerta, Campo, PageHeader } from "@/components/ui";

const ROLES = { ADMIN: "Administrador", CAJERO: "Cajero" } as const;
const vacio = { actual: "", nueva: "", confirmar: "" };

/** Datos de la cuenta y cambio de la contraseña propia. */
export default function CuentaPage() {
  const sesion = useSesion();
  const [form, setForm] = useState(vacio);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  async function cambiar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setExito(null);
    setErrores({});
    if (form.nueva !== form.confirmar) {
      setErrores({ confirmar: "No coincide con la nueva contraseña" });
      return;
    }
    setGuardando(true);
    try {
      await api.post("/auth/cambiar-clave", { actual: form.actual, nueva: form.nueva });
      setForm(vacio);
      setExito("Contraseña actualizada. Úsala la próxima vez que inicies sesión.");
    } catch (err) {
      setError(mensajeError(err));
      setErrores(erroresCampos(err));
    } finally {
      setGuardando(false);
    }
  }

  if (!sesion) return null;
  const { usuario } = sesion;

  return (
    <>
      <PageHeader titulo="Mi cuenta" />
      <div className="grid max-w-3xl gap-6 md:grid-cols-2">
        <div className="tarjeta h-fit">
          <dl className="space-y-3 text-sm">
            <div>
              <dt className="text-slate-500">Nombre</dt>
              <dd className="font-medium text-slate-900">{usuario.nombre}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Correo</dt>
              <dd className="break-all font-medium text-slate-900">{usuario.email}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Rol</dt>
              <dd className="font-medium text-slate-900">{ROLES[usuario.rol]}</dd>
            </div>
          </dl>
        </div>

        <form onSubmit={cambiar} className="tarjeta space-y-4">
          <h2 className="font-semibold text-slate-900">Cambiar contraseña</h2>
          <Alerta mensaje={error} onCerrar={() => setError(null)} />
          <Alerta tipo="exito" mensaje={exito} onCerrar={() => setExito(null)} />
          <Campo label="Contraseña actual" error={errores.actual}>
            <input type="password" className="input" autoComplete="current-password" value={form.actual}
              onChange={(e) => setForm({ ...form, actual: e.target.value })} />
          </Campo>
          <Campo label="Nueva contraseña" error={errores.nueva}>
            <input type="password" className="input" autoComplete="new-password" value={form.nueva}
              onChange={(e) => setForm({ ...form, nueva: e.target.value })} />
          </Campo>
          <Campo label="Confirmar nueva contraseña" error={errores.confirmar}>
            <input type="password" className="input" autoComplete="new-password" value={form.confirmar}
              onChange={(e) => setForm({ ...form, confirmar: e.target.value })} />
          </Campo>
          <p className="text-xs text-slate-500">Mínimo 8 caracteres, con al menos una letra y un número.</p>
          <button type="submit" className="btn btn-primario w-full" disabled={guardando}>
            {guardando ? "Guardando…" : "Cambiar contraseña"}
          </button>
        </form>
      </div>
    </>
  );
}
