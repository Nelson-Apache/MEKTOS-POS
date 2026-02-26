package com.mektos.pos.domain.model;

import com.mektos.pos.domain.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compra {

    private Long id;
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
    private Proveedor proveedor;
    private String numeroFactura;
    private BigDecimal total;
    private Usuario usuario;
    @Builder.Default
    private List<DetalleCompra> detalles = new ArrayList<>();

    /**
     * Suma los subtotales de cada DetalleCompra para calcular el total.
     * El total NUNCA se ingresa desde la UI — siempre se calcula aquí.
     */
    public void calcularTotal() {
        this.total = detalles.stream()
                .map(DetalleCompra::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Valida las reglas de negocio antes de persistir la compra.
     * No se permiten compras vacías ni ítems con cantidad 0 o negativa.
     */
    public void validar() {
        if (detalles.isEmpty()) {
            throw new BusinessException("La compra debe tener al menos un producto.");
        }
        for (DetalleCompra detalle : detalles) {
            if (detalle.getCantidad() <= 0) {
                throw new BusinessException("La cantidad de cada ítem de la compra debe ser mayor que cero.");
            }
        }
    }
}
