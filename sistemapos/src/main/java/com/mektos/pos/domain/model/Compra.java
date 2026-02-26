package com.mektos.pos.domain.model;

import com.mektos.pos.domain.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Compra {

    private Long id;
    private LocalDateTime fecha;
    private Proveedor proveedor;
    private String numeroFactura;
    private BigDecimal total;
    private Usuario usuario;
    private List<DetalleCompra> detalles;

    public Compra() {
        this.detalles = new ArrayList<>();
        this.fecha = LocalDateTime.now();
    }

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
