import type { Metadata } from "next";
import type { ReactNode } from "react";
import Sidebar from "@/components/Sidebar";
import "./globals.css";

export const metadata: Metadata = {
  title: "Mini-ERP · Inventario y facturación",
  description: "Gestión de productos, compras, ventas e inventario para pymes",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="es">
      <body className="bg-slate-100 text-slate-800 antialiased">
        <div className="min-h-screen lg:flex">
          <Sidebar />
          <main className="min-w-0 flex-1 p-4 sm:p-6 lg:p-8 print:p-0">{children}</main>
        </div>
      </body>
    </html>
  );
}
