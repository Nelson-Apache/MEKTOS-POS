package com.nap.pos.domain.model.enums;

/**
 * Régimen tributario del negocio respecto al IVA (Colombia).
 *
 * <ul>
 *   <li>{@link #NO_RESPONSABLE_IVA} — Personas naturales con ingresos anuales inferiores a
 *       3.500 UVT (tiendas de barrio, artesanos, micronegocios). No cobran IVA al cliente;
 *       el IVA pagado en compras se absorbe como costo. No están obligados a factura
 *       electrónica, aunque pueden adoptarla.</li>
 *   <li>{@link #REGIMEN_ORDINARIO} — Empresas (S.A.S, S.A.) y personas con ventas altas.
 *       Discriminan IVA en ventas (19 %, 5 % o 0 % según producto), declaran bimestralmente
 *       y están obligados a emitir factura electrónica o Documento Equivalente POS.</li>
 *   <li>{@link #REGIMEN_SIMPLE} — Régimen Simple de Tributación (RST, Art. 903-916 ET).
 *       Pagan un impuesto unificado (renta + ICA + consumo), pero el IVA sigue siendo
 *       un impuesto autónomo y aparte. Son responsables de IVA <b>excepto</b> cuando
 *       desarrollan <b>únicamente</b> actividades del Grupo 1 del Art. 908 (tiendas
 *       pequeñas, minimercados, micromercados y peluquerías) o cuando son personas
 *       naturales con ingresos &lt; 3.500 UVT (Art. 437, par. 4 ET). Obligados
 *       siempre a emitir factura electrónica o POS electrónico; no pueden usar
 *       comprobantes en papel.</li>
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

    /**
     * Indica si el negocio <b>puede</b> ser responsable de IVA.
     * <p>
     * Tanto el Régimen Ordinario como el RST obligan a cobrar IVA en ventas de
     * bienes y servicios gravados. Sin embargo, dentro del RST existe la excepción
     * del Art. 437, par. 4 del ET: <b>tiendas pequeñas, minimercados, micromercados
     * y peluquerías</b> (Grupo 1, Art. 908), así como personas naturales con ingresos
     * &lt; 3.500 UVT, <b>no</b> son responsables de IVA.
     * <p>
     * Este método retorna {@code true} como indicador general. Para el RST, el
     * usuario debe configurar {@code ivaPorDefecto = 0} si su actividad corresponde
     * al Grupo 1 o si sus ingresos no superan el umbral.
     */
    public boolean isResponsableIva() {
        return this == REGIMEN_ORDINARIO || this == REGIMEN_SIMPLE;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
