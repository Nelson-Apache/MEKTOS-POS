package com.mektos.pos.domain.model;

import com.mektos.pos.domain.exception.BusinessException;
import com.mektos.pos.domain.model.enums.EstadoCaja;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Caja {

    private Long id;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal montoInicial;
    private BigDecimal montoFinal;
    private EstadoCaja estado;

    public boolean estaAbierta() {
        return EstadoCaja.ABIERTA.equals(this.estado);
    }

    /**
     * Cierra la caja registrando la hora de cierre y el monto final.
     * Solo puede cerrarse si está abierta.
     */
    public void cerrar(BigDecimal montoFinal) {
        if (!estaAbierta()) {
            throw new BusinessException("La caja ya está cerrada.");
        }
        this.fechaCierre = LocalDateTime.now();
        this.montoFinal = montoFinal;
        this.estado = EstadoCaja.CERRADA;
    }
}
