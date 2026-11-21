// Cliente HTTP centralizado para hablar con la API de Spring Boot

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

/** Error con el formato de ErrorResponse del backend. */
export class ApiError extends Error {
  status: number;
  errores?: Record<string, string>;

  constructor(status: number, message: string, errores?: Record<string, string>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errores = errores;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_URL}${path}`, {
      ...init,
      headers: { "Content-Type": "application/json", ...(init.headers ?? {}) },
      cache: "no-store",
    });
  } catch {
    throw new ApiError(0, "No se pudo conectar con el servidor. ¿Está corriendo el backend en el puerto 8080?");
  }

  if (!res.ok) {
    let mensaje = `Error ${res.status}`;
    let errores: Record<string, string> | undefined;
    try {
      const body = await res.json();
      mensaje = body.mensaje ?? mensaje;
      errores = body.errores ?? undefined;
    } catch {
      // la respuesta no traía JSON
    }
    throw new ApiError(res.status, mensaje, errores);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

/** Construye "?a=1&b=2" ignorando valores vacíos. */
export function query(params: Record<string, string | number | boolean | null | undefined>): string {
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== "") sp.append(clave, String(valor));
  });
  const texto = sp.toString();
  return texto ? `?${texto}` : "";
}

/** Errores por campo (de @Valid) para mostrarlos bajo cada input. */
export function erroresCampos(e: unknown): Record<string, string> {
  return e instanceof ApiError ? e.errores ?? {} : {};
}

const conCuerpo = (method: string, body?: unknown): RequestInit => ({
  method,
  body: body === undefined ? undefined : JSON.stringify(body),
});

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, conCuerpo("POST", body)),
  put: <T>(path: string, body?: unknown) => request<T>(path, conCuerpo("PUT", body)),
  patch: <T>(path: string, body?: unknown) => request<T>(path, conCuerpo("PATCH", body)),
  delete: <T = void>(path: string) => request<T>(path, { method: "DELETE" }),
};
