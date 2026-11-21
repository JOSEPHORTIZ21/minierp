import CatalogoTerceros from "@/components/CatalogoTerceros";

export default function ClientesPage() {
  return (
    <CatalogoTerceros
      endpoint="/clientes"
      titulo="Clientes"
      descripcion="Personas y empresas a las que les vendes"
      singular="cliente"
      ordenarPor="nombre"
      campoNombre="nombre"
      campos={[
        { clave: "documento", etiqueta: "Documento / NIT" },
        { clave: "nombre", etiqueta: "Nombre" },
        { clave: "telefono", etiqueta: "Teléfono", tipo: "tel" },
        { clave: "email", etiqueta: "Correo", tipo: "email" },
        { clave: "direccion", etiqueta: "Dirección", enTabla: false },
      ]}
    />
  );
}
