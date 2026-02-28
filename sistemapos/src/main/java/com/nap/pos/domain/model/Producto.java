package com.nap.pos.domain.model;

import com.nap.pos.domain.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    private Long id;
    private String codigoBarras;
    private String nombre;
    private BigDecimal precioVenta;
    private BigDecimal precioCompra;
    private Proveedor proveedorPrincipal;
    // Por defecto 0: el precio se calcula con el % base del proveedor sin ajuste
    @Builder.Default
    private BigDecimal ajusteProducto = BigDecimal.ZERO;
    private int stock;
    private boolean activo;

    /**
     * Recalcula el precioVenta usando la fórmula del PRD:
     *   precioVenta = precioCompra × (1 + (% proveedor + ajusteProducto) / 100)
     *
     * Ejemplo: precioCompra=1000, proveedor=30%, ajuste=+10%
     *   margenEfectivo = 30 + 10 = 40%
     *   factor = 1 + (40/100) = 1.40
     *   precioVenta = 1000 × 1.40 = 1400
     */
    public void calcularPrecioVenta() {
        if (precioCompra == null || proveedorPrincipal == null) {
            return;
        }
        BigDecimal margenEfectivo = proveedorPrincipal.getPorcentajeGanancia().add(ajusteProducto);
        if (margenEfectivo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El margen efectivo del producto '" + nombre + "' debe ser mayor que cero.");
        }
        // Se usa 10 decimales intermedios para evitar pérdida de precisión,
        // y al final se redondea a 2 decimales (centavos).
        BigDecimal factor = BigDecimal.ONE.add(
                margenEfectivo.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
        );
        this.precioVenta = precioCompra.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Cambia el proveedor principal del producto.
     * Según las reglas del PRD, al hacer este cambio:
     *  1. El ajusteProducto se reinicia a 0.
     *  2. El precioVenta se recalcula con el % base del nuevo proveedor.
     */
    public void cambiarProveedorPrincipal(Proveedor nuevoProveedor) {
        this.proveedorPrincipal = nuevoProveedor;
        this.ajusteProducto = BigDecimal.ZERO;
        calcularPrecioVenta();
    }

    /**
     * Aplica un ajuste al margen del proveedor para este producto específico.
     * El valor puede ser positivo (aumentar) o negativo (reducir).
     * El margen efectivo total nunca puede quedar en 0 o negativo.
     */
    public void aplicarAjuste(BigDecimal ajuste) {
        BigDecimal margenResultante = proveedorPrincipal.getPorcentajeGanancia().add(ajuste);
        if (margenResultante.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El margen efectivo del producto '" + nombre + "' debe ser mayor que cero.");
        }
        this.ajusteProducto = ajuste;
        calcularPrecioVenta();
    }

    /**
     * Descuenta stock al registrar una venta.
     * Lanza excepción si no hay suficiente stock — nunca puede quedar negativo.
     */
    public void descontarStock(int cantidad) {
        if (cantidad > this.stock) {
            throw new BusinessException("Stock insuficiente para el producto: " + nombre);
        }
        this.stock -= cantidad;
    }

    /**
     * Incrementa stock al registrar una compra de inventario.
     */
    public void incrementarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new BusinessException("La cantidad a incrementar debe ser mayor que cero.");
        }
        this.stock += cantidad;
    }

    /**
     * Actualiza el precio de compra del producto (registrado durante una compra al proveedor)
     * y recalcula automáticamente el precio de venta con el nuevo costo base.
     */
    public void actualizarCosto(BigDecimal nuevoPrecioCompra) {
        if (nuevoPrecioCompra == null || nuevoPrecioCompra.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El precio de compra debe ser mayor que cero.");
        }
        this.precioCompra = nuevoPrecioCompra;
        calcularPrecioVenta();
    }

    /** Reactiva un producto — vuelve a aparecer disponible en ventas. */
    public void activar() {
        this.activo = true;
    }

    /** Desactiva un producto — deja de aparecer en ventas sin eliminarlo del historial. */
    public void desactivar() {
        this.activo = false;
    }
}
