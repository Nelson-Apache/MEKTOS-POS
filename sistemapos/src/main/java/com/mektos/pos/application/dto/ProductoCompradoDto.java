package com.mektos.pos.application.dto;

import java.math.BigDecimal;

/**
 * Resumen de un producto dentro del reporte de compras de un proveedor.
 * Agrega todas las compras del mismo producto en un solo registro.
 */
public record ProductoCompradoDto(
        String nombre,
        String codigoBarras,
        int cantidadTotal,
        BigDecimal totalInvertido
) {}
