package com.nap.pos.domain.model.enums;

/**
 * Régimen tributario del negocio respecto al IVA.
 *
 * <ul>
 *   <li>{@link #NO_RESPONSABLE_IVA} — No cobra IVA al cliente. El IVA pagado en compras
 *       se absorbe como parte del costo del producto.</li>
 *   <li>{@link #REGIMEN_ORDINARIO} — Responsable de IVA. Cobra IVA en ventas (generado)
 *       y puede descontar el IVA pagado en compras (descontable). Declara bimestralmente.</li>
 *   <li>{@link #REGIMEN_SIMPLE} — Régimen Simple de Tributación (RST). Tarifa unificada;
 *       generalmente no discrimina IVA en ventas pero sí lo paga en compras.</li>
 * </ul>
 */
public enum RegimenTributario {

    NO_RESPONSABLE_IVA("No responsable de IVA"),
    REGIMEN_ORDINARIO("Régimen Ordinario (responsable de IVA)"),
    REGIMEN_SIMPLE("Régimen Simple de Tributación");

    private final String descripcion;

    RegimenTributario(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Indica si el negocio cobra IVA en sus ventas. */
    public boolean isResponsableIva() {
        return this == REGIMEN_ORDINARIO;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
