package com.nap.pos.domain.model;

import com.nap.pos.domain.model.enums.RegimenTributario;
import com.nap.pos.domain.model.enums.TipoPersona;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionTienda {

    private Long id;                 // siempre 1 — fila única en BD
    private TipoPersona tipoPersona;
    private String nombreTienda;

    // Solo cuando tipoPersona = NATURAL
    private String nombre;
    private String apellido;
    private String cedula;

    // Solo cuando tipoPersona = JURIDICA
    private String razonSocial;
    private String nit;
    private String representanteLegalNombre;
    private String representanteLegalApellido;

    private String direccion;
    private String rutaLogo;         // null si no se configuró

    // Contacto (aparece en recibos)
    private String telefono;
    private String correo;           // null si no se configuró

    /** Umbral global de stock mínimo. Productos con stock ≤ este valor generan alerta. */
    @Builder.Default
    private int stockMinimoGlobal = 5;

    /** Régimen tributario del negocio. Determina si cobra IVA y cómo se contabiliza. */
    @Builder.Default
    private RegimenTributario regimenTributario = RegimenTributario.REGIMEN_ORDINARIO;

    /**
     * Porcentaje de IVA aplicado en ventas (0, 5 o 19 en Colombia).
     * Aplica cuando el negocio es responsable de IVA: tanto en Régimen Ordinario
     * como en RST, excepto en RST Grupo 1 (tiendas/minimercados/peluquerías) o
     * personas naturales RST con ingresos < 3.500 UVT, quienes deben poner 0.
     */
    @Builder.Default
    private int ivaPorDefecto = 19;

    /**
     * Indica si el precioVenta almacenado ya incluye el IVA dentro.
     * - true  → precio ya incluye IVA; se desglosa en el recibo
     * - false → precio es base; el IVA se calcula y suma al total
     * Aplica cuando el negocio es responsable de IVA (Ordinario o RST responsable).
     */
    @Builder.Default
    private boolean precioConIvaIncluido = false;

    /** Conveniencia: true si el régimen obliga a cobrar IVA en ventas. */
    public boolean isResponsableIva() {
        return regimenTributario != null && regimenTributario.isResponsableIva();
    }

    /** % de ganancia global al crear productos sin proveedor asignado. */
    @Builder.Default
    private int porcentajeGananciaGlobal = 30;

    /** Prefijo de los comprobantes de venta (ej: "FAC-"). */
    @Builder.Default
    private String prefijoComprobante = "FAC-";

    /** Número a partir del cual se numera el primer comprobante. */
    @Builder.Default
    private int numeroInicialComprobante = 1;

    /**
     * Mes (1-12) y día (1-31) programados para el inventario general anual.
     * null si el usuario no los configuró. El sistema entiende que es anual
     * y cada año verifica si se realizó el conteo correspondiente.
     */
    private Integer mesInventarioAnual;
    private Integer diaInventarioAnual;

    /** ID del usuario (administrador) que configuró la tienda inicialmente. */
    private Long propietarioId;
}
