package com.nap.pos.domain.model;

import com.nap.pos.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitarios de las reglas de negocio del modelo de dominio Producto.
 * No usan Spring ni mocks — prueban la lógica pura del dominio.
 */
class ProductoTest {

    // ─────────────────────────────────────────────────────────────────────────
    // descontarStock
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Criterio PRD: "No se puede vender un producto con stock insuficiente.
     * El sistema debe impedir que el stock quede en negativo."
     */
    @Test
    void descontarStock_cantidadMayorQueStock_lanzaBusinessException() {
        Producto producto = productoBuilder(5);

        assertThrows(BusinessException.class, () -> producto.descontarStock(6));
        assertThat(producto.getStock()).isEqualTo(5); // sin cambios
    }

    /** Vender la cantidad exacta disponible debe dejar stock en 0, no lanzar excepción. */
    @Test
    void descontarStock_cantidadExacta_dejaSaldoEnCero() {
        Producto producto = productoBuilder(3);

        producto.descontarStock(3);

        assertThat(producto.getStock()).isEqualTo(0);
    }

    @Test
    void descontarStock_cantidadParcial_reduceSaldo() {
        Producto producto = productoBuilder(10);

        producto.descontarStock(4);

        assertThat(producto.getStock()).isEqualTo(6);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // incrementarStock
    // ─────────────────────────────────────────────────────────────────────────

    /** Criterio PRD: "no se permite registrar una compra con cantidad cero o negativa." */
    @Test
    void incrementarStock_cantidadCero_lanzaBusinessException() {
        Producto producto = productoBuilder(5);

        assertThrows(BusinessException.class, () -> producto.incrementarStock(0));
        assertThat(producto.getStock()).isEqualTo(5);
    }

    @Test
    void incrementarStock_cantidadNegativa_lanzaBusinessException() {
        Producto producto = productoBuilder(5);

        assertThrows(BusinessException.class, () -> producto.incrementarStock(-1));
        assertThat(producto.getStock()).isEqualTo(5);
    }

    @Test
    void incrementarStock_cantidadPositiva_sumaCantidad() {
        Producto producto = productoBuilder(5);

        producto.incrementarStock(10);

        assertThat(producto.getStock()).isEqualTo(15);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // actualizarCosto
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void actualizarCosto_precioNegativo_lanzaBusinessException() {
        Producto producto = productoConProveedor(new BigDecimal("1000"), new BigDecimal("30"));

        assertThrows(BusinessException.class,
                () -> producto.actualizarCosto(new BigDecimal("-1")));
    }

    @Test
    void actualizarCosto_precioCero_lanzaBusinessException() {
        Producto producto = productoConProveedor(new BigDecimal("1000"), new BigDecimal("30"));

        assertThrows(BusinessException.class,
                () -> producto.actualizarCosto(BigDecimal.ZERO));
    }

    @Test
    void actualizarCosto_precioValido_recalculaPrecioVenta() {
        // precioCompra=1000, proveedor 30% → precioVenta = 1000 × 1.30 = 1300.00
        Producto producto = productoConProveedor(new BigDecimal("1000"), new BigDecimal("30"));

        producto.actualizarCosto(new BigDecimal("2000"));

        // precioVenta = 2000 × 1.30 = 2600.00
        assertThat(producto.getPrecioVenta()).isEqualByComparingTo("2600.00");
        assertThat(producto.getPrecioCompra()).isEqualByComparingTo("2000");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calcularPrecioVenta — fórmula del PRD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Criterio PRD: "precioVenta = precioCompra × (1 + porcentajeGananciaProveedor / 100)"
     */
    @Test
    void calcularPrecioVenta_formula_correcta() {
        // precioCompra=1000, margen=30% → factor=1.30 → precioVenta=1300.00
        Producto producto = productoConProveedor(new BigDecimal("1000"), new BigDecimal("30"));

        producto.calcularPrecioVenta();

        assertThat(producto.getPrecioVenta()).isEqualByComparingTo("1300.00");
    }

    @Test
    void calcularPrecioVenta_margen19porciento_calculaCorrectamente() {
        // precioCompra=5000, margen=19% → factor=1.19 → precioVenta=5950.00
        Producto producto = productoConProveedor(new BigDecimal("5000"), new BigDecimal("19"));

        producto.calcularPrecioVenta();

        assertThat(producto.getPrecioVenta()).isEqualByComparingTo("5950.00");
    }

    /**
     * Criterio PRD: "El margen efectivo total nunca puede ser cero o negativo."
     */
    @Test
    void calcularPrecioVenta_margenCero_lanzaBusinessException() {
        Proveedor sinMargen = Proveedor.builder()
                .id(1L).nombre("Prov").porcentajeGanancia(BigDecimal.ZERO).activo(true).build();
        Producto producto = Producto.builder()
                .id(1L).nombre("Prod").codigoBarras("P001")
                .precioCompra(new BigDecimal("1000"))
                .proveedorPrincipal(sinMargen)
                .stock(10).activo(true).build();

        assertThrows(BusinessException.class, producto::calcularPrecioVenta);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Producto productoBuilder(int stock) {
        return Producto.builder()
                .id(1L).nombre("Producto Test").codigoBarras("TEST-001")
                .precioCompra(new BigDecimal("1000")).precioVenta(new BigDecimal("1300"))
                .stock(stock).activo(true).build();
    }

    private Producto productoConProveedor(BigDecimal precioCompra, BigDecimal margenProveedor) {
        Proveedor proveedor = Proveedor.builder()
                .id(1L).nombre("Proveedor Test")
                .porcentajeGanancia(margenProveedor)
                .activo(true).build();
        return Producto.builder()
                .id(1L).nombre("Producto Test").codigoBarras("TEST-001")
                .precioCompra(precioCompra)
                .proveedorPrincipal(proveedor)
                .stock(10).activo(true).build();
    }
}
