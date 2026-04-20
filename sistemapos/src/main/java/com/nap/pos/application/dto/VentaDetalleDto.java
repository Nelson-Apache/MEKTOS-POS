package com.nap.pos.application.dto;

import com.nap.pos.domain.model.enums.EstadoVenta;
import com.nap.pos.domain.model.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Detalle discriminado de cada venta en una sesión de caja.
 * Incluye cliente, fecha/hora, método de pago y estado.
 */
public record VentaDetalleDto(
        Long ventaId,
        Long numeroComprobante,
        LocalDateTime fecha,
        String clienteNombre,
        String clienteCedula,
        MetodoPago metodoPago,
        EstadoVenta estado,
        BigDecimal total
) {}
