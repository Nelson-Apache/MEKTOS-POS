package com.nap.pos.application.dto;

import java.math.BigDecimal;

/**
 * Agregado de productos comprados por cada cliente en una sesión de ventas.
 */
public record ClienteProductoVendidoDto(
        Long clienteId,
        String clienteNombre,
        String clienteCedula,
        String productoNombre,
        String codigoBarras,
        int cantidadComprada,
        BigDecimal totalComprado
) {}
