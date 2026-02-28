package com.nap.pos.application.dto.importacion;

/**
 * Representa un registro del archivo que ya existe en la base de datos.
 *
 * @param numeroFila    número de fila en el archivo (comienza en 2 — fila 1 es el encabezado)
 * @param identificador valor clave que generó la colisión (código de barras o cédula)
 * @param datosActuales resumen textual del registro existente en BD
 * @param datosNuevos   resumen textual del registro que se intenta importar
 */
public record DuplicadoEncontrado(
        int    numeroFila,
        String identificador,
        String datosActuales,
        String datosNuevos
) {}
