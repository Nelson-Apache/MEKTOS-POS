package com.nap.pos.domain.port;

/**
 * Puerto de salida para impresión de tickets de venta.
 * Recibe el texto del ticket ya formateado — la responsabilidad de formatear
 * pertenece a ImpresionService (capa de aplicación), no a este puerto.
 * La implementación concreta reside en infrastructure/printing.
 */
public interface TicketPrinter {

    /**
     * Envía el texto del ticket a la impresora.
     * Si la impresora no está disponible o falla, lanza TechnicalException;
     * el llamador decide si propaga o absorbe el error para no revertir la venta.
     *
     * @param textoTicket contenido del ticket ya formateado como texto plano
     */
    void imprimir(String textoTicket);
}
