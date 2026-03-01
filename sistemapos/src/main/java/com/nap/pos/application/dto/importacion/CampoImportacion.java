package com.nap.pos.application.dto.importacion;

/**
 * Enum de todos los campos que el sistema puede importar.
 * Cada campo lleva su label en español, si es obligatorio, y a qué entidad pertenece.
 */
public enum CampoImportacion {

    // ── Producto ──────────────────────────────────────────────────────────
    CODIGO_BARRAS("Código de barras",   false, TipoEntidad.PRODUCTO),
    NOMBRE_PRODUCTO("Nombre",           true,  TipoEntidad.PRODUCTO),
    PRECIO_COMPRA("Precio de compra",   true,  TipoEntidad.PRODUCTO),
    PRECIO_VENTA("Precio de venta",     true,  TipoEntidad.PRODUCTO),
    STOCK("Stock inicial",              true,  TipoEntidad.PRODUCTO),
    NIT_PROVEEDOR("NIT del proveedor",  false, TipoEntidad.PRODUCTO),
    NOMBRE_CATEGORIA("Categoría",       false, TipoEntidad.PRODUCTO),
    NOMBRE_SUBCATEGORIA("Subcategoría", false, TipoEntidad.PRODUCTO),

    // ── Cliente ───────────────────────────────────────────────────────────
    NOMBRE_CLIENTE("Nombre",            true,  TipoEntidad.CLIENTE),
    CEDULA("Cédula",                    false, TipoEntidad.CLIENTE),
    CELULAR("Celular",                  false, TipoEntidad.CLIENTE),
    DIRECCION("Dirección",              false, TipoEntidad.CLIENTE),
    MONTO_CREDITO("Monto crédito",      false, TipoEntidad.CLIENTE),
    PLAZO_PAGO("Plazo de pago (días)",  false, TipoEntidad.CLIENTE);

    private final String label;
    private final boolean requerido;
    private final TipoEntidad tipo;

    CampoImportacion(String label, boolean requerido, TipoEntidad tipo) {
        this.label     = label;
        this.requerido = requerido;
        this.tipo      = tipo;
    }

    public String     getLabel()     { return label; }
    public boolean    isRequerido()  { return requerido; }
    public TipoEntidad getTipo()     { return tipo; }
}
