package com.nap.pos.application.dto.importacion;

/**
 * Describe un error de validación en una fila del archivo de importación.
 * numeroFila = 0 indica un error de configuración de mapeo (no de una fila concreta).
 */
public record FilaError(int numeroFila, String campo, String motivo) {}
