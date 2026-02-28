package com.nap.pos.application.dto.importacion;

import java.util.List;

/**
 * Resumen del resultado de una operación de importación masiva.
 */
public record ResultadoImportacion(
        int            importados,
        int            actualizados,
        int            omitidos,
        List<FilaError> errores
) {}
