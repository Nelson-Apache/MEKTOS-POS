package com.nap.pos.application.dto;

import java.math.BigDecimal;

/**
 * Reporte de rentabilidad de un mes específico.
 * Compara lo invertido en compras contra lo recaudado en ventas.
 * Solo las ventas COMPLETADAS aportan al totalVendido.
 *
 * Fórmula:
 *   gananciaBruta    = totalVendido - totalInvertido
 *   margenPorcentaje = (gananciaBruta / totalVendido) × 100
 *
 * Si no hubo ventas en el mes, margenPorcentaje es null.
 */
public record ReporteRentabilidadDto(
        int anio,
        int mes,
        BigDecimal totalInvertido,       // suma de compras del mes
        BigDecimal totalVendido,         // suma de ventas COMPLETADAS del mes
        BigDecimal gananciaBruta,        // puede ser negativa (pérdida)
        BigDecimal margenPorcentaje,     // null si totalVendido = 0
        boolean tuvoPerdida
) {}
