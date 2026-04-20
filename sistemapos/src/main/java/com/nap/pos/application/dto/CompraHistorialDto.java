package com.nap.pos.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fila de historial del reporte de compras por proveedor.
 */
public record CompraHistorialDto(
        Long compraId,
        LocalDateTime fecha,
        String numeroFactura,
        int items,
        int unidades,
        BigDecimal total,
        List<CompraProductoDetalleDto> productos
) {}
