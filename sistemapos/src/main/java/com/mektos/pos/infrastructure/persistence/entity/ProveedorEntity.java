package com.mektos.pos.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "proveedores")
public class ProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    // unique = true: si el NIT se ingresa, no puede repetirse entre proveedores
    @Column(unique = true, length = 20)
    private String nit;

    @Column(length = 20)
    private String celular;

    @Column(length = 200)
    private String direccion;

    // Margen base que se aplica a todos los productos de este proveedor
    @Column(name = "porcentaje_ganancia", nullable = false, precision = 10, scale = 4)
    private BigDecimal porcentajeGanancia;

    @Column(nullable = false)
    private boolean activo;
}
