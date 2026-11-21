import CatalogoTerceros from "@/components/CatalogoTerceros";

export default function ProveedoresPage() {
  return (
    <CatalogoTerceros
      endpoint="/proveedores"
      titulo="Proveedores"
      descripcion="Empresas a las que les compras mercancía"
      singular="proveedor"
      ordenarPor="razonSocial"
      campoNombre="razonSocial"
      campos={[
        { clave: "nit", etiqueta: "NIT" },
        { clave: "razonSocial", etiqueta: "Razón social" },
        { clave: "telefono", etiqueta: "Teléfono", tipo: "tel" },
        { clave: "email", etiqueta: "Correo", tipo: "email" },
        { clave: "direccion", etiqueta: "Dirección", enTabla: false },
      ]}
    />
  );
}
