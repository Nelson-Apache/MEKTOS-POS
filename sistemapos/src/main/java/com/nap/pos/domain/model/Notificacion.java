package com.nap.pos.domain.model;

import com.nap.pos.domain.model.enums.Severidad;
import com.nap.pos.domain.model.enums.TipoNotificacion;

/**
 * Representa una notificación generada en tiempo de ejecución.
 * No se persiste en BD — se calcula al consultar el servicio.
 *
 * @param tipo         categoría de la notificación (ej. STOCK_BAJO)
 * @param titulo       texto corto para mostrar en el badge/campana
 * @param mensaje      descripción detallada para el panel
 * @param severidad    nivel de urgencia (INFO, ADVERTENCIA, CRITICA)
 * @param referenciaId ID de la entidad relacionada (producto, etc.), puede ser null
 */
public record Notificacion(
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        Severidad severidad,
        Long referenciaId
) {}
