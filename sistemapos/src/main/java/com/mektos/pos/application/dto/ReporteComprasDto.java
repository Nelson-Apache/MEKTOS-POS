package com.mektos.pos.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reporte de todas las compras realizadas a un proveedor específico.
 * Los productos aparecen agregados — si el mismo producto se compró varias veces,
 * se suma la cantidad y el total invertido.
 */
public record ReporteComprasDto(
        Long proveedorId,
        String nombreProveedor,
        int totalCompras,
        BigDecimal totalInvertido,
        List<ProductoCompradoDto> productos  // ordenados de mayor a menor inversión
) {}
