package com.mektos.pos.domain.model;

import com.mektos.pos.domain.exception.BusinessException;
import com.mektos.pos.domain.model.enums.EstadoVenta;
import com.mektos.pos.domain.model.enums.MetodoPago;
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
public class Venta {

    private Long id;
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
    private BigDecimal total;
    private MetodoPago metodoPago;
    private Cliente cliente;
    private Usuario usuario;
    private Caja caja;
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.COMPLETADA;
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();

    /**
     * Suma los subtotales de todos los detalles para obtener el total de la venta.
     * El total NUNCA se recibe desde la UI — siempre se calcula aquí en el dominio.
     */
    public void calcularTotal() {
        this.total = detalles.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Anula la venta. Solo se pueden anular ventas en estado COMPLETADA.
     * El servicio de aplicación restaura el stock y el crédito antes de llamar a este método.
     */
    public void anular() {
        if (!EstadoVenta.COMPLETADA.equals(this.estado)) {
            throw new BusinessException("Solo se pueden anular ventas en estado COMPLETADA.");
        }
        this.estado = EstadoVenta.ANULADA;
    }

    /**
     * Valida las reglas de negocio antes de persistir la venta.
     * El servicio de aplicación llama a este método antes de guardar.
     */
    public void validar() {
        if (caja == null || !caja.estaAbierta()) {
            throw new BusinessException("No hay una caja abierta para procesar la venta.");
        }
        if (detalles.isEmpty()) {
            throw new BusinessException("La venta debe tener al menos un producto.");
        }
        // Si el método de pago es CREDITO, el cliente es obligatorio
        if (MetodoPago.CREDITO.equals(metodoPago) && cliente == null) {
            throw new BusinessException("Una venta a crédito requiere un cliente asociado.");
        }
    }
}
