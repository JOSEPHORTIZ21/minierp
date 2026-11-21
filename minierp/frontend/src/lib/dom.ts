/**
 * En pantallas angostas el formulario de edición queda debajo del listado:
 * al pulsar "Editar" se desplaza hasta él para que el usuario vea que se cargó el registro.
 */
export function mostrarFormulario(id: string) {
  if (typeof window === "undefined" || window.matchMedia("(min-width: 80rem)").matches) return;
  document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
}
