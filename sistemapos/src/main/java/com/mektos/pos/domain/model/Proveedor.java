package com.mektos.pos.domain.model;

import com.mektos.pos.domain.exception.BusinessException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class Proveedor {

    private Long id;
    private String nombre;
    private String nit;
    private String celular;
    private String direccion;

    @Setter(AccessLevel.NONE)
    private BigDecimal porcentajeGanancia;

    private boolean activo;

    public void setPorcentajeGanancia(BigDecimal porcentajeGanancia) {
        if (porcentajeGanancia == null || porcentajeGanancia.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El porcentaje de ganancia del proveedor debe ser mayor que cero.");
        }
        this.porcentajeGanancia = porcentajeGanancia;
    }
}
