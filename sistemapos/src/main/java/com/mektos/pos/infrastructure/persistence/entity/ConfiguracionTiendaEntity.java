package com.mektos.pos.infrastructure.persistence.entity;

import com.mektos.pos.domain.model.enums.TipoPersona;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion_tienda")
public class ConfiguracionTiendaEntity {

    /**
     * Siempre 1 — la tabla tiene una única fila.
     * No se usa GenerationType.IDENTITY para poder forzar el id desde el servicio.
     */
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_persona", nullable = false, length = 20)
    private TipoPersona tipoPersona;

    @Column(name = "nombre_tienda", nullable = false, length = 150)
    private String nombreTienda;

    // Persona natural
    @Column(length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellido;

    @Column(length = 30)
    private String cedula;

    // Persona jurídica
    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(length = 30)
    private String nit;

    @Column(nullable = false, length = 300)
    private String direccion;

    @Column(name = "ruta_logo", length = 500)
    private String rutaLogo;
}
