package com.mektos.pos.application.dto;

import java.math.BigDecimal;

/**
 * Resumen de un producto dentro del reporte de ventas.
 * Muestra cuántas unidades se vendieron y cuánto dinero generó.
 */
public record ProductoVendidoDto(
        String nombre,
        String codigoBarras,
        int cantidadVendida,
        BigDecimal totalGenerado
) {}
