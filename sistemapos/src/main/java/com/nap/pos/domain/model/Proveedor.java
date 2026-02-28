package com.nap.pos.domain.model;

import com.nap.pos.domain.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    private Long id;
    private String nombre;
    private String nit;
    private String celular;
    private String direccion;
    private BigDecimal porcentajeGanancia;
    private boolean activo;

    /**
     * Único punto de mutación del porcentajeGanancia.
     * Incluye validación — no puede ser nulo ni menor o igual a cero.
     * Al cambiar el porcentaje, el servicio recalculará los precios de todos
     * los productos que tienen este proveedor como principal.
     */
    public void actualizarPorcentajeGanancia(BigDecimal nuevoPorcentaje) {
        if (nuevoPorcentaje == null || nuevoPorcentaje.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El porcentaje de ganancia del proveedor debe ser mayor que cero.");
        }
        this.porcentajeGanancia = nuevoPorcentaje;
    }
}
