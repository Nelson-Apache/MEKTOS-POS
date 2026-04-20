package com.nap.pos.application.dto;

import com.nap.pos.domain.model.enums.FuenteGasto;
import com.nap.pos.domain.model.enums.TipoGasto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GastoDetalleDto(
        Long id,
        LocalDateTime fecha,
        TipoGasto tipo,
        FuenteGasto fuentePago,
        String concepto,
        String categoria,
        BigDecimal monto,
        String proveedor,
        String referencia,
        String notas,
        String usuarioNombre
) {}
