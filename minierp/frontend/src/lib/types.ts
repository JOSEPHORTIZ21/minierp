// Tipos que reflejan los DTOs del backend Spring Boot

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Categoria {
  id: number;
  nombre: string;
  descripcion: string | null;
}

export interface Cliente {
  id: number;
  documento: string;
  nombre: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
}

export interface Proveedor {
  id: number;
  nit: string;
  razonSocial: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
}

export interface Producto {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  precioCompra: number;
  precioVenta: number;
  porcentajeIva: number;
  stock: number;
  stockMinimo: number;
  activo: boolean;
  stockBajo: boolean;
  categoriaId: number;
  categoriaNombre: string;
}

export interface ProductoRequest {
  codigo: string;
  nombre: string;
  descripcion: string | null;
  precioCompra: number | null;
  precioVenta: number | null;
  porcentajeIva: number | null;
  stockInicial: number | null;
  stockMinimo: number | null;
  categoriaId: number | null;
}

export type TipoMovimiento = "ENTRADA" | "SALIDA";
export type ConceptoMovimiento = "INVENTARIO_INICIAL" | "COMPRA" | "VENTA" | "ANULACION_VENTA" | "AJUSTE";

export interface Movimiento {
  id: number;
  fecha: string;
  tipo: TipoMovimiento;
  concepto: ConceptoMovimiento;
  cantidad: number;
  stockAnterior: number;
  stockResultante: number;
  referencia: string | null;
  observacion: string | null;
}

export type EstadoVenta = "COMPLETADA" | "ANULADA";

export interface DetalleVenta {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  porcentajeIva: number;
  subtotal: number;
  valorIva: number;
  total: number;
}

export interface Venta {
  id: number;
  numero: string;
  fecha: string;
  clienteId: number | null;
  clienteNombre: string;
  clienteDocumento: string;
  subtotal: number;
  totalIva: number;
  total: number;
  estado: EstadoVenta;
  detalles: DetalleVenta[];
}

export interface VentaResumen {
  id: number;
  numero: string;
  fecha: string;
  clienteNombre: string;
  subtotal: number;
  totalIva: number;
  total: number;
  estado: EstadoVenta;
}

export interface CompraResumen {
  id: number;
  numeroFactura: string;
  fecha: string;
  proveedorNombre: string;
  subtotal: number;
  totalIva: number;
  total: number;
}

export interface Compra extends CompraResumen {
  proveedorId: number;
  proveedorNit: string;
  detalles: {
    id: number;
    productoId: number;
    productoCodigo: string;
    productoNombre: string;
    cantidad: number;
    costoUnitario: number;
    porcentajeIva: number;
    subtotal: number;
    valorIva: number;
    total: number;
  }[];
}

export interface Dashboard {
  ventasHoy: number;
  ventasMes: number;
  numeroVentasMes: number;
  ticketPromedioMes: number;
  productosStockBajo: number;
  valorInventario: number;
  ventasUltimos7Dias: { fecha: string; total: number; cantidad: number }[];
  topProductos: { productoId: number; nombre: string; cantidadVendida: number; totalVendido: number }[];
  alertasStock: Producto[];
}

export type Rol = "ADMIN" | "CAJERO";

export interface Usuario {
  id: number;
  email: string;
  nombre: string;
  rol: Rol;
  activo: boolean;
  creadoEn: string;
}

export interface LoginResponse {
  token: string;
  expiraEn: string;
  usuario: Usuario;
}
