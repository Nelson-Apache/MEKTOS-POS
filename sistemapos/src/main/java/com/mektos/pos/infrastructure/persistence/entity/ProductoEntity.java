package com.mektos.pos.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "productos")
public class ProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_barras", unique = true, length = 50)
    private String codigoBarras;

    @Column(nullable = false, length = 150)
    private String nombre;

    // Precio calculado — nunca se muestra al cajero el precioCompra
    @Column(name = "precio_venta", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "precio_compra", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioCompra;

    // Proveedor cuyo % de ganancia se usa para calcular el precioVenta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_principal_id")
    private ProveedorEntity proveedorPrincipal;

    // Ajuste individual al margen del proveedor para este producto (puede ser negativo)
    @Column(name = "ajuste_producto", nullable = false, precision = 10, scale = 4)
    private BigDecimal ajusteProducto;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean activo;
}
