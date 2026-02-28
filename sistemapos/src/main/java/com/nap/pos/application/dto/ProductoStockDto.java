package com.nap.pos.application.dto;

import java.math.BigDecimal;

/**
 * Estado de stock de un producto activo.
 * Se usa en el reporte de inventario para identificar productos
 * agotados o con stock por debajo del umbral de alerta.
 */
public record ProductoStockDto(
        Long id,
        String nombre,
        String codigoBarras,
        String proveedor,      // nombre del proveedor principal
        int stock,
        BigDecimal precioCompra,
        BigDecimal precioVenta
) {}
