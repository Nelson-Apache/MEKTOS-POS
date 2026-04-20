package com.nap.pos.domain.model;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.enums.FuenteGasto;
import com.nap.pos.domain.model.enums.TipoGasto;
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
public class Gasto {

    private Long id;
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
    private TipoGasto tipo;
    private FuenteGasto fuentePago;
    private String concepto;
    private String categoria;
    private BigDecimal monto;
    private String proveedor;
    private String referencia;
    private String notas;
    private Usuario usuario;

    public void validar() {
        if (concepto == null || concepto.isBlank()) {
            throw new BusinessException("El concepto del gasto es obligatorio.");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto del gasto debe ser mayor que cero.");
        }
        if (tipo == null) {
            throw new BusinessException("El tipo de gasto es obligatorio.");
        }
        if (fuentePago == null) {
            throw new BusinessException("La fuente de pago es obligatoria.");
        }
    }
}
