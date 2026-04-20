package com.nap.pos.application.dto;

import java.math.BigDecimal;

/**
 * Producto incluido dentro de una orden de compra.
 */
public record CompraProductoDetalleDto(
        String nombreProducto,
        String codigoBarras,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {}
