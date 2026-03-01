package com.nap.pos.domain.model.enums;

public enum Severidad {
    /** Informativo — no requiere acción inmediata. */
    INFO,
    /** Advertencia — requiere atención pronto. */
    ADVERTENCIA,
    /** Crítico — requiere acción inmediata (ej: stock = 0). */
    CRITICA
}
