package com.nap.pos.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rentabilidad consolidada de un año completo.
 * Incluye los totales anuales y el desglose por cada uno de los 12 meses,
 * lo que permite identificar temporadas de baja o alta rentabilidad.
 *
 * Fórmula:
 *   gananciaBruta    = totalVendido - totalInvertido
 *   margenPorcentaje = (gananciaBruta / totalVendido) × 100
 *
 * Si no hubo ventas en todo el año, margenPorcentaje es null.
 */
public record ReporteRentabilidadAnualDto(
        int anio,
        BigDecimal totalInvertido,
        BigDecimal totalVendido,
        BigDecimal gananciaBruta,
        BigDecimal margenPorcentaje,   // null si totalVendido = 0
        boolean tuvoPerdida,
        List<RentabilidadMensualDto> meses  // siempre 12 elementos (Enero→Diciembre)
) {}
