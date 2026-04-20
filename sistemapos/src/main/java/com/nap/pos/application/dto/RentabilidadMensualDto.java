package com.nap.pos.application.dto;

import java.math.BigDecimal;

/**
 * Rentabilidad de un mes individual dentro del reporte anual.
 * Siempre se generan los 12 meses — los meses sin actividad
 * aparecen con totales en cero.
 */
public record RentabilidadMensualDto(
        int mes,
        String nombreMes,
        BigDecimal totalInvertido,
        BigDecimal totalVendido,
        BigDecimal totalGastos,      // gastos operativos del mes (no inventario)
        BigDecimal ajusteCaja,
        BigDecimal gananciaBruta,    // puede ser negativa
        boolean tuvoPerdida
) {}
