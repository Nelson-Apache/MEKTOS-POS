package com.nap.pos.infrastructure.printing;

import com.nap.pos.domain.exception.TechnicalException;
import com.nap.pos.domain.port.TicketPrinter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Guarda el ticket como archivo de texto en ~/.nappos/tickets/
 * y lo abre con el visor predeterminado del sistema operativo.
 *
 * No requiere impresora física: el operador puede imprimir desde el visor
 * de texto si lo necesita. El archivo queda como respaldo histórico.
 */
@Slf4j
@Component
public class TicketPrinterImpl implements TicketPrinter {

    @Override
    public void imprimir(String textoTicket, String nombreArchivo) {
        try {
            Path carpeta = Paths.get(System.getProperty("user.home"), ".nappos", "tickets");
            Files.createDirectories(carpeta);

            Path archivo = carpeta.resolve(nombreArchivo + ".txt");
            Files.writeString(archivo, textoTicket, StandardCharsets.UTF_8);
            log.info("Ticket guardado en: {}", archivo.toAbsolutePath());

            abrirArchivo(archivo);
        } catch (IOException e) {
            throw new TechnicalException("Error al guardar el ticket: " + e.getMessage());
        }
    }

    private void abrirArchivo(Path archivo) {
        if (!Desktop.isDesktopSupported()) {
            log.warn("Desktop no disponible — el ticket no se abrirá automáticamente.");
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            log.warn("La acción OPEN no está soportada — el ticket no se abrirá automáticamente.");
            return;
        }
        try {
            desktop.open(archivo.toFile());
        } catch (IOException e) {
            // No lanzar excepción: el archivo ya fue guardado correctamente.
            log.warn("No se pudo abrir el ticket automáticamente: {}", e.getMessage());
        }
    }
}
