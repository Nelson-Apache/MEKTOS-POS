package com.nap.pos.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteGastosDto(
        int totalRegistros,
        int totalComprasGasto,
        int totalPagos,
        BigDecimal montoTotalGastos,
        BigDecimal montoComprasGasto,
        BigDecimal montoPagos,
        BigDecimal montoFuenteCaja,
        BigDecimal montoFuenteTransferencia,
        List<GastoDetalleDto> detalles
) {}
