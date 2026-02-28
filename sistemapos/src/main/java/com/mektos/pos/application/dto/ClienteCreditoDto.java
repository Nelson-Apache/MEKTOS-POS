package com.mektos.pos.application.dto;

import java.math.BigDecimal;

/**
 * Estado de crédito de un cliente individual.
 * Muestra cuánto tiene disponible y cuánto debe actualmente.
 */
public record ClienteCreditoDto(
        Long id,
        String nombre,
        String cedula,
        BigDecimal limiteCredito,
        BigDecimal saldoUtilizado,
        BigDecimal saldoDisponible
) {}
