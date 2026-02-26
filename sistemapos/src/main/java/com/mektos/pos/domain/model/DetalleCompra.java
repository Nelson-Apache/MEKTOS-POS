package com.mektos.pos.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompra {

    private Long id;
    private Producto producto;
    private int cantidad;
    private BigDecimal precioCompraUnitario;
    private BigDecimal subtotal;

    /**
     * Constructor para crear un detalle al registrar una compra.
     * Calcula el subtotal automáticamente — no se recibe desde la UI.
     */
    public DetalleCompra(Producto producto, int cantidad, BigDecimal precioCompraUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioCompraUnitario = precioCompraUnitario;
        this.subtotal = precioCompraUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
