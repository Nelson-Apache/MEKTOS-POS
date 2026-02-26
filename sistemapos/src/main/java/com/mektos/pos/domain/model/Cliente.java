package com.mektos.pos.domain.model;

import com.mektos.pos.domain.exception.BusinessException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    private Long id;
    private String nombre;
    private String cedula;
    private String celular;
    private String direccion;
    private BigDecimal montoCredito;
    private PlazoPago plazoPago;
    // saldoUtilizado arranca en 0 al crear el cliente
    private BigDecimal saldoUtilizado = BigDecimal.ZERO;
    private boolean activo;

    /**
     * Un cliente tiene crédito habilitado solo si se le asignó un montoCredito mayor a cero.
     * Si montoCredito es null o 0, no puede hacer compras a crédito.
     */
    public boolean tieneCreditoHabilitado() {
        return montoCredito != null && montoCredito.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Saldo disponible = montoCredito aprobado - lo que ya debe.
     * Ejemplo: límite 500.000, ya debe 200.000 → disponible 300.000
     */
    public BigDecimal getSaldoDisponible() {
        if (!tieneCreditoHabilitado()) {
            return BigDecimal.ZERO;
        }
        return montoCredito.subtract(saldoUtilizado);
    }

    /**
     * Se llama al registrar una venta a crédito.
     * Suma el monto de la venta al saldo utilizado del cliente.
     */
    public void utilizarCredito(BigDecimal monto) {
        if (!tieneCreditoHabilitado()) {
            throw new BusinessException("El cliente no tiene crédito habilitado.");
        }
        if (monto.compareTo(getSaldoDisponible()) > 0) {
            throw new BusinessException("El monto supera el saldo de crédito disponible del cliente.");
        }
        this.saldoUtilizado = this.saldoUtilizado.add(monto);
    }

    /**
     * Se llama al registrar un pago parcial o total de la deuda del cliente.
     * El saldo utilizado nunca puede bajar de 0 (max con ZERO evita negativos).
     */
    public void abonar(BigDecimal monto) {
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto del abono debe ser mayor que cero.");
        }
        this.saldoUtilizado = this.saldoUtilizado.subtract(monto).max(BigDecimal.ZERO);
    }
}
