import type { Metadata } from "next";
import type { ReactNode } from "react";
import MarcoApp from "@/components/MarcoApp";
import "./globals.css";

export const metadata: Metadata = {
  title: "Mini-ERP · Inventario y facturación",
  description: "Gestión de productos, compras, ventas e inventario para pymes",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="es">
      <body className="bg-slate-100 text-slate-800 antialiased">
        <MarcoApp>{children}</MarcoApp>
      </body>
    </html>
  );
}
