package com.nap.pos.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reporte de créditos pendientes de todos los clientes.
 * Solo incluye clientes que tienen saldo utilizado mayor a cero.
 * Ordenados de mayor a menor deuda para identificar rápidamente
 * quiénes deben más.
 */
public record ReporteCreditosDto(
        int clientesConDeuda,
        BigDecimal totalDeudaPendiente,
        List<ClienteCreditoDto> clientes
) {}
