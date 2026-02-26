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
public class Venta {

    private Long id;
    private LocalDateTime fecha;
    private BigDecimal total;
    private MetodoPago metodoPago;
    private Cliente cliente;
    private Usuario usuario;
    private Caja caja;
    private EstadoVenta estado;
    private List<DetalleVenta> detalles;

    public Venta() {
        this.detalles = new ArrayList<>();
        this.fecha = LocalDateTime.now();
        this.estado = EstadoVenta.COMPLETADA;
    }

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
