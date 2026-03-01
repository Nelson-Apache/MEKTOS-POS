package com.nap.pos.infrastructure.persistence.entity;

import com.nap.pos.domain.model.enums.RegimenTributario;
import com.nap.pos.domain.model.enums.TipoPersona;
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

    // Contacto
    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String correo;

    @Column(name = "stock_minimo_global", nullable = false, columnDefinition = "INTEGER DEFAULT 5")
    private int stockMinimoGlobal = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_tributario", nullable = false, length = 30,
            columnDefinition = "TEXT DEFAULT 'REGIMEN_ORDINARIO'")
    private RegimenTributario regimenTributario = RegimenTributario.REGIMEN_ORDINARIO;

    @Column(name = "iva_por_defecto", nullable = false, columnDefinition = "INTEGER DEFAULT 19")
    private int ivaPorDefecto = 19;

    @Column(name = "precio_con_iva_incluido", nullable = false, columnDefinition = "BOOLEAN DEFAULT 0")
    private boolean precioConIvaIncluido = false;

    @Column(name = "porcentaje_ganancia_global", nullable = false, columnDefinition = "INTEGER DEFAULT 30")
    private int porcentajeGananciaGlobal = 30;

    @Column(name = "prefijo_comprobante", nullable = false, length = 10, columnDefinition = "TEXT DEFAULT 'FAC-'")
    private String prefijoComprobante = "FAC-";

    @Column(name = "numero_inicial_comprobante", nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private int numeroInicialComprobante = 1;
}
