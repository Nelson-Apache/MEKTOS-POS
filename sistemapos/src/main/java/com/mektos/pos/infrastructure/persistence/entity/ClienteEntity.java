package com.mektos.pos.infrastructure.persistence.entity;

import com.mektos.pos.domain.model.enums.PlazoPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clientes")
public class ClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, length = 20)
    private String celular;

    @Column(nullable = false, length = 200)
    private String direccion;

    // Null si el cliente no tiene crédito aprobado
    @Column(name = "monto_credito", precision = 15, scale = 2)
    private BigDecimal montoCredito;

    // Solo aplica si montoCredito está definido
    @Enumerated(EnumType.STRING)
    @Column(name = "plazo_pago", length = 20)
    private PlazoPago plazoPago;

    // Acumulado de ventas a crédito pendientes de pago
    @Column(name = "saldo_utilizado", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoUtilizado;

    @Column(nullable = false)
    private boolean activo;
}
