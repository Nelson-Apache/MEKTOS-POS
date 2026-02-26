package com.mektos.pos.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class DetalleCompra {

    private Long id;
    private Producto producto;
    private int cantidad;
    private BigDecimal precioCompraUnitario;
    private BigDecimal subtotal;

    public DetalleCompra(Producto producto, int cantidad, BigDecimal precioCompraUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioCompraUnitario = precioCompraUnitario;
        this.subtotal = precioCompraUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
