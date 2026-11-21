import ProductoForm from "@/components/ProductoForm";
import { PageHeader } from "@/components/ui";

export default function NuevoProductoPage() {
  return (
    <>
      <PageHeader titulo="Nuevo producto" descripcion="El stock inicial quedará registrado en el kardex" />
      <ProductoForm />
    </>
  );
}
