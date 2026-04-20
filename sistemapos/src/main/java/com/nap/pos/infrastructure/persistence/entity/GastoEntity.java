package com.nap.pos.infrastructure.persistence.entity;

import com.nap.pos.domain.model.enums.FuenteGasto;
import com.nap.pos.domain.model.enums.TipoGasto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gastos")
public class GastoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoGasto tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente_pago", nullable = false, length = 20)
    private FuenteGasto fuentePago;

    @Column(nullable = false, length = 200)
    private String concepto;

    @Column(length = 100)
    private String categoria;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(length = 200)
    private String proveedor;

    @Column(length = 100)
    private String referencia;

    @Column(length = 500)
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;
}
