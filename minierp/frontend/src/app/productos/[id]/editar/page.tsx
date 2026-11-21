"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import { mensajeError } from "@/lib/format";
import type { Producto } from "@/lib/types";
import ProductoForm from "@/components/ProductoForm";
import { Alerta, Cargando, PageHeader } from "@/components/ui";

export default function EditarProductoPage() {
  const { id } = useParams<{ id: string }>();
  const [producto, setProducto] = useState<Producto | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.get<Producto>(`/productos/${id}`).then(setProducto).catch((e) => setError(mensajeError(e)));
  }, [id]);

  return (
    <>
      <PageHeader titulo="Editar producto" descripcion={producto ? `${producto.codigo} · ${producto.nombre}` : undefined} />
      <Alerta mensaje={error} />
      {producto ? <ProductoForm producto={producto} /> : !error && <Cargando />}
    </>
  );
}
