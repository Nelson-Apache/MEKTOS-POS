package com.nap.pos.application.dto;

import java.math.BigDecimal;

/**
 * Reporte de rentabilidad de un mes específico.
 * Fórmula:
 *   gananciaBruta    = totalVendido - totalInvertido - totalGastos + ajusteCaja
 *   margenPorcentaje = (gananciaBruta / totalVendido) × 100
 */
public record ReporteRentabilidadDto(
        int anio,
        int mes,
        BigDecimal totalInvertido,
        BigDecimal totalVendido,
        BigDecimal totalGastos,          // gastos operativos del mes
        BigDecimal ajusteCaja,
        BigDecimal gananciaBruta,
        BigDecimal margenPorcentaje,     // null si totalVendido = 0
        boolean tuvoPerdida
) {}
