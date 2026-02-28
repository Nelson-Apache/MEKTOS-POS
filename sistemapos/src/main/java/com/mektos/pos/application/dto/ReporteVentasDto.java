package com.mektos.pos.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte de ventas de un período de caja.
 * Incluye desglose por método de pago y los productos más vendidos.
 * Solo las ventas en estado COMPLETADA aportan al total.
 */
public record ReporteVentasDto(
        Long cajaId,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,       // null si la caja aún está abierta
        int totalVentas,
        int ventasCompletadas,
        int ventasAnuladas,
        BigDecimal totalEfectivo,
        BigDecimal totalTransferencia,
        BigDecimal totalCredito,
        BigDecimal totalGeneral,
        List<ProductoVendidoDto> topProductos  // ordenados de mayor a menor cantidad vendida
) {}
