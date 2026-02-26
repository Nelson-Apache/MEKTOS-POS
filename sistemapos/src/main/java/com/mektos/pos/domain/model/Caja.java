package com.mektos.pos.domain.model;

import com.mektos.pos.domain.exception.BusinessException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
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

    public void cerrar(BigDecimal montoFinal) {
        if (!estaAbierta()) {
            throw new BusinessException("La caja ya está cerrada.");
        }
        this.fechaCierre = LocalDateTime.now();
        this.montoFinal = montoFinal;
        this.estado = EstadoCaja.CERRADA;
    }
}
