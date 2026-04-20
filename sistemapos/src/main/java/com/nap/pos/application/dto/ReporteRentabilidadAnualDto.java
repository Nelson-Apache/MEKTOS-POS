package com.nap.pos.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rentabilidad consolidada de un año completo.
 * Fórmula:
 *   gananciaBruta    = totalVendido - totalInvertido - totalGastosAnual + ajusteCajaTotal
 *   margenPorcentaje = (gananciaBruta / totalVendido) × 100
 */
public record ReporteRentabilidadAnualDto(
        int anio,
        BigDecimal totalInvertido,
        BigDecimal totalVendido,
        BigDecimal totalGastosAnual,
        BigDecimal ajusteCajaTotal,
        BigDecimal gananciaBruta,
        BigDecimal margenPorcentaje,   // null si totalVendido = 0
        boolean tuvoPerdida,
        List<RentabilidadMensualDto> meses  // siempre 12 elementos (Enero→Diciembre)
) {}
