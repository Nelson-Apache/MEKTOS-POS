package com.nap.pos.domain.model;

import com.nap.pos.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitarios de las reglas de negocio del modelo de dominio Cliente.
 * No usan Spring ni mocks — prueban la lógica pura de crédito en el dominio.
 */
class ClienteTest {

    // ─────────────────────────────────────────────────────────────────────────
    // tieneCreditoHabilitado
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Criterio PRD: "Si un cliente no tiene monto de crédito asignado,
     * no puede realizar compras a crédito."
     */
    @Test
    void tieneCreditoHabilitado_montoNull_retornaFalse() {
        Cliente cliente = clienteBuilder(null, BigDecimal.ZERO);

        assertThat(cliente.tieneCreditoHabilitado()).isFalse();
    }

    @Test
    void tieneCreditoHabilitado_montoCero_retornaFalse() {
        Cliente cliente = clienteBuilder(BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(cliente.tieneCreditoHabilitado()).isFalse();
    }

    @Test
    void tieneCreditoHabilitado_montoPositivo_retornaTrue() {
        Cliente cliente = clienteBuilder(new BigDecimal("500000"), BigDecimal.ZERO);

        assertThat(cliente.tieneCreditoHabilitado()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSaldoDisponible
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getSaldoDisponible_sinDeuda_retornaMontoCompleto() {
        Cliente cliente = clienteBuilder(new BigDecimal("500000"), BigDecimal.ZERO);

        assertThat(cliente.getSaldoDisponible()).isEqualByComparingTo("500000");
    }

    @Test
    void getSaldoDisponible_conDeuda_restaSaldoUtilizado() {
        // Límite 500.000, ya debe 200.000 → disponible 300.000
        Cliente cliente = clienteBuilder(new BigDecimal("500000"), new BigDecimal("200000"));

        assertThat(cliente.getSaldoDisponible()).isEqualByComparingTo("300000");
    }

    @Test
    void getSaldoDisponible_sinCreditoHabilitado_retornaCero() {
        Cliente cliente = clienteBuilder(null, BigDecimal.ZERO);

        assertThat(cliente.getSaldoDisponible()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // utilizarCredito
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Criterio PRD: "No se puede asignar una venta a crédito si el cliente
     * no tiene crédito aprobado."
     */
    @Test
    void utilizarCredito_sinCreditoHabilitado_lanzaBusinessException() {
        Cliente cliente = clienteBuilder(null, BigDecimal.ZERO);

        assertThrows(BusinessException.class,
                () -> cliente.utilizarCredito(new BigDecimal("10000")));
        assertThat(cliente.getSaldoUtilizado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Criterio PRD: "El saldo utilizado de un cliente no puede superar su monto de crédito.
     * El monto de una venta a crédito no puede superar el saldo disponible."
     */
    @Test
    void utilizarCredito_excedeSaldoDisponible_lanzaBusinessException() {
        // Límite 100.000, ya debe 80.000 → disponible 20.000
        Cliente cliente = clienteBuilder(new BigDecimal("100000"), new BigDecimal("80000"));

        // Intenta usar 25.000 con solo 20.000 disponibles
        assertThrows(BusinessException.class,
                () -> cliente.utilizarCredito(new BigDecimal("25000")));
        // El saldo no debe haber cambiado
        assertThat(cliente.getSaldoUtilizado()).isEqualByComparingTo("80000");
    }

    @Test
    void utilizarCredito_montoExactoAlDisponible_actualizaSaldo() {
        // Límite 100.000, sin deuda → disponible 100.000
        Cliente cliente = clienteBuilder(new BigDecimal("100000"), BigDecimal.ZERO);

        cliente.utilizarCredito(new BigDecimal("100000"));

        assertThat(cliente.getSaldoUtilizado()).isEqualByComparingTo("100000");
        assertThat(cliente.getSaldoDisponible()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void utilizarCredito_montoValido_incrementaSaldoUtilizado() {
        Cliente cliente = clienteBuilder(new BigDecimal("500000"), new BigDecimal("100000"));

        cliente.utilizarCredito(new BigDecimal("50000"));

        assertThat(cliente.getSaldoUtilizado()).isEqualByComparingTo("150000");
        assertThat(cliente.getSaldoDisponible()).isEqualByComparingTo("350000");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // abonar
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void abonar_montoNegativo_lanzaBusinessException() {
        Cliente cliente = clienteBuilder(new BigDecimal("500000"), new BigDecimal("200000"));

        assertThrows(BusinessException.class,
                () -> cliente.abonar(new BigDecimal("-1000")));
        assertThat(cliente.getSaldoUtilizado()).isEqualByComparingTo("200000");
    }

    @Test
    void abonar_montoCero_lanzaBusinessException() {
        Cliente cliente = clienteBuilder(new BigDecimal("500000"), new BigDecimal("200000"));

        assertThrows(BusinessException.class, () -> cliente.abonar(BigDecimal.ZERO));
    }

    @Test
    void abonar_montoValido_reduceSaldoUtilizado() {
        Cliente cliente = clienteBuilder(new BigDecimal("500000"), new BigDecimal("200000"));

        cliente.abonar(new BigDecimal("50000"));

        assertThat(cliente.getSaldoUtilizado()).isEqualByComparingTo("150000");
    }

    /**
     * El saldo utilizado nunca puede quedar negativo aunque el abono sea mayor que la deuda.
     */
    @Test
    void abonar_montoMayorQueDeuda_saldoQuedaEnCero() {
        Cliente cliente = clienteBuilder(new BigDecimal("500000"), new BigDecimal("100000"));

        cliente.abonar(new BigDecimal("999999"));

        assertThat(cliente.getSaldoUtilizado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private Cliente clienteBuilder(BigDecimal montoCredito, BigDecimal saldoUtilizado) {
        return Cliente.builder()
                .id(1L).nombre("Test").cedula("123456").celular("3001234567")
                .montoCredito(montoCredito).saldoUtilizado(saldoUtilizado)
                .activo(true).build();
    }
}
