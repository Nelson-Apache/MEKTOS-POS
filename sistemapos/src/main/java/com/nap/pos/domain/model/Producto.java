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
    private int stock;
    private Subcategoria subcategoria;
    private boolean activo;
    /** Ruta absoluta a la imagen del producto en ~/.nappos/assets/products/ */
    private String imagenPath;

    /**
     * Recalcula el precioVenta usando el margen del proveedor principal:
     *   precioVenta = precioCompra × (1 + % proveedor / 100)
     *
     * Ejemplo: precioCompra=1000, proveedor=30%
     *   factor = 1 + (30/100) = 1.30
     *   precioVenta = 1000 × 1.30 = 1300
     */
    public void calcularPrecioVenta() {
        if (precioCompra == null || proveedorPrincipal == null) {
            return;
        }
        BigDecimal margen = proveedorPrincipal.getPorcentajeGanancia();
        if (margen.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El margen del proveedor del producto '" + nombre + "' debe ser mayor que cero.");
        }
        BigDecimal factor = BigDecimal.ONE.add(
                margen.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
        );
        this.precioVenta = precioCompra.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Cambia el proveedor principal del producto y recalcula el precioVenta
     * con el margen del nuevo proveedor.
     */
    public void cambiarProveedorPrincipal(Proveedor nuevoProveedor) {
        this.proveedorPrincipal = nuevoProveedor;
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

    /** Actualiza la ruta de la imagen del producto. */
    public void actualizarImagen(String rutaImagen) {
        this.imagenPath = rutaImagen;
    }
}
