package com.nap.pos.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categorias")
public class CategoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    /** Literal Ikonli, ej. "fas-tshirt". Nullable — categorías sin ícono usan solo texto. */
    @Column(length = 60)
    private String icono;

    @Column(nullable = false)
    private boolean activo;
}
