package com.nap.pos.application.dto;

import java.util.List;

/**
 * Reporte de inventario enfocado en alertas de stock.
 * Solo muestra los productos que necesitan atención:
 * - Agotados (stock = 0): deben reabastecerse urgente.
 * - Bajo stock (0 < stock ≤ umbral): están por agotarse.
 * Los productos con stock suficiente no aparecen en este reporte.
 */
public record ReporteInventarioDto(
        int totalProductosActivos,
        int productosAgotados,
        int productosBajoStock,
        int umbralBajoStock,            // umbral usado para clasificar "bajo stock"
        List<ProductoStockDto> agotados,
        List<ProductoStockDto> bajoStock
) {}
