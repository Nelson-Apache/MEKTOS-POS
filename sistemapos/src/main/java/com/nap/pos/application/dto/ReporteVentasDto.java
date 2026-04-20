package com.nap.pos.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte de ventas de un período de caja.
 * Incluye desglose por método de pago, detalle por venta,
 * productos comprados por cliente y los productos más vendidos.
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
        BigDecimal totalGastosCaja,      // gastos pagados desde caja en este período
        List<VentaDetalleDto> detalleVentas,   // ordenado de la venta más reciente a la más antigua
        List<ClienteProductoVendidoDto> comprasPorCliente,
        List<ProductoVendidoDto> topProductos  // ordenados de mayor a menor cantidad vendida
) {}
