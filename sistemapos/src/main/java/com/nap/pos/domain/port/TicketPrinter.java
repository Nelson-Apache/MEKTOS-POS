package com.nap.pos.domain.port;

/**
 * Puerto de salida para impresión de tickets de venta.
 * Recibe el texto del ticket ya formateado — la responsabilidad de formatear
 * pertenece a ImpresionService (capa de aplicación), no a este puerto.
 * La implementación concreta reside en infrastructure/printing.
 */
public interface TicketPrinter {

    /**
     * Guarda y/o envía el texto del ticket.
     *
     * @param textoTicket   contenido del ticket ya formateado como texto plano
     * @param nombreArchivo nombre base del archivo a guardar (sin extensión)
     */
    void imprimir(String textoTicket, String nombreArchivo);
}
