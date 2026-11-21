"use client";

import { useEffect, useState, type FormEvent } from "react";
import { api, erroresCampos } from "@/lib/api";
import { mensajeError } from "@/lib/format";
import { guardarSesion } from "@/lib/sesion";
import type { LoginResponse } from "@/lib/types";

/** Al guardar la sesión, MarcoApp lleva a la página pedida o al inicio del rol. */
export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [verClave, setVerClave] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [enviando, setEnviando] = useState(false);
  const [lento, setLento] = useState(false);

  // El plan gratuito de Render apaga el backend tras 15 min sin uso: el primer login puede tardar
  useEffect(() => {
    if (!enviando) {
      setLento(false);
      return;
    }
    const t = setTimeout(() => setLento(true), 4000);
    return () => clearTimeout(t);
  }, [enviando]);

  async function ingresar(e: FormEvent) {
    e.preventDefault();
    setEnviando(true);
    setError(null);
    setErrores({});
    try {
      guardarSesion(await api.post<LoginResponse>("/auth/login", { email, password }));
    } catch (err) {
      setError(mensajeError(err));
      setErrores(erroresCampos(err));
      setEnviando(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-900 px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="mb-6 text-center">
          <p className="text-2xl font-bold text-white">Mini-ERP</p>
          <p className="text-sm text-slate-400">Inventario y facturación</p>
        </div>

        <form onSubmit={ingresar} className="tarjeta space-y-4 p-6" noValidate>
          <h1 className="text-lg font-semibold text-slate-900">Iniciar sesión</h1>

          {error && (
            <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          )}

          <label className="block">
            <span className="label">Correo</span>
            <input
              type="email"
              className="input"
              autoComplete="username"
              autoFocus
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            {errores.email && <span className="mt-1 block text-xs text-red-600">{errores.email}</span>}
          </label>

          <label className="block">
            <span className="label">Contraseña</span>
            <div className="relative">
              <input
                type={verClave ? "text" : "password"}
                className="input pr-20"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <button
                type="button"
                onClick={() => setVerClave((v) => !v)}
                className="absolute inset-y-0 right-0 px-3 text-xs font-medium text-indigo-600 hover:text-indigo-800"
                aria-label={verClave ? "Ocultar contraseña" : "Mostrar contraseña"}
              >
                {verClave ? "Ocultar" : "Mostrar"}
              </button>
            </div>
            {errores.password && <span className="mt-1 block text-xs text-red-600">{errores.password}</span>}
          </label>

          <button type="submit" className="btn btn-primario w-full py-2.5" disabled={enviando}>
            {enviando ? "Ingresando…" : "Ingresar"}
          </button>

          {lento && (
            <p className="text-center text-xs text-slate-500">
              El servidor se está iniciando; la primera vez del día puede tardar hasta un minuto.
            </p>
          )}
        </form>

        <p className="mt-4 text-center text-xs text-slate-500">¿Olvidaste tu contraseña? Pídele a un administrador que la restablezca.</p>
      </div>
    </div>
  );
}
