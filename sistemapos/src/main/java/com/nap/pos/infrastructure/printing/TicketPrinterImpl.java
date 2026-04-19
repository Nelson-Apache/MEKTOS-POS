package com.nap.pos.infrastructure.printing;

import com.nap.pos.domain.exception.TechnicalException;
import com.nap.pos.domain.port.TicketPrinter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.nio.charset.StandardCharsets;

/**
 * Implementación de TicketPrinter usando la API javax.print del JDK.
 * No requiere dependencias externas ni de la capa de aplicación.
 * Envía el texto recibido como bytes a la impresora predeterminada del sistema.
 *
 * En Windows las impresoras térmicas USB se registran como impresoras del
 * sistema, por lo que javax.print las detecta correctamente.
 */
@Slf4j
@Component
public class TicketPrinterImpl implements TicketPrinter {

    @Override
    public void imprimir(String textoTicket) {
        byte[] bytes = textoTicket.getBytes(StandardCharsets.UTF_8);

        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new Copies(1));

        PrintService servicio = encontrarServicio(flavor, attrs);
        if (servicio == null) {
            throw new TechnicalException(
                    "No se encontró ninguna impresora disponible en el sistema.");
        }

        log.info("Imprimiendo ticket en: {}", servicio.getName());

        try {
            Doc doc = new SimpleDoc(bytes, flavor, null);
            DocPrintJob job = servicio.createPrintJob();
            job.print(doc, attrs);
        } catch (PrintException e) {
            throw new TechnicalException("Error al enviar el ticket a la impresora: " + e.getMessage());
        }
    }

    private PrintService encontrarServicio(DocFlavor flavor, PrintRequestAttributeSet attrs) {
        PrintService defecto = PrintServiceLookup.lookupDefaultPrintService();
        if (defecto != null && defecto.isDocFlavorSupported(flavor)) {
            return defecto;
        }
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(flavor, attrs);
        if (servicios.length > 0) {
            return servicios[0];
        }
        return null;
    }
}
