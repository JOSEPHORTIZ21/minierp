package com.minierp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    /** Último costo de compra (sin IVA). Se actualiza con cada compra. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precioCompra;

    /** Precio de venta antes de IVA. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precioVenta;

    /** 0, 5 o 19 en Colombia. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIva;

    /** Solo cambia a través de InventarioService, que registra cada movimiento. */
    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer stockMinimo;

    /** Borrado lógico: un producto con ventas no se elimina, se desactiva. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
