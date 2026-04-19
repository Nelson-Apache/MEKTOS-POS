package com.nap.pos.application.service;

import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.model.DetalleVenta;
import com.nap.pos.domain.model.Venta;
import com.nap.pos.domain.port.TicketPrinter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Orquesta la impresión de tickets de venta.
 * Formatea el contenido del ticket y delega la impresión física al TicketPrinter.
 * El llamador captura cualquier excepción para que un fallo de impresora
 * nunca revierta la venta ya guardada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImpresionService {

    private static final int ANCHO = 42;
    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final NumberFormat FMT_MONEDA =
            NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

    private final TicketPrinter ticketPrinter;
    private final ConfiguracionService configuracionService;

    /**
     * Imprime el ticket de la venta. Si la impresora falla el error se registra
     * en log pero NO se propaga — la venta ya fue confirmada en BD.
     */
    public void imprimirTicket(Venta venta) {
        try {
            ConfiguracionTienda config = configuracionService.obtener();
            String texto = formatearTicket(
                    venta,
                    config.getNombreTienda(),
                    config.getDireccion(),
                    config.getTelefono(),
                    config.getPrefijoComprobante()
            );
            ticketPrinter.imprimir(texto);
        } catch (Exception e) {
            log.warn("No se pudo imprimir el ticket de la venta {}: {}", venta.getId(), e.getMessage());
        }
    }

    /**
     * Construye el texto del ticket formateado a {@value ANCHO} caracteres de ancho.
     * Se expone como método público para poder generar una vista previa en pantalla.
     */
    public String formatearTicket(Venta venta, String nombreTienda,
                                   String direccion, String telefono, String prefijo) {
        StringBuilder sb = new StringBuilder();
        String sep  = "=".repeat(ANCHO);
        String sepL = "-".repeat(ANCHO);

        sb.append(sep).append('\n');
        sb.append(centrar(nombreTienda)).append('\n');
        if (direccion != null && !direccion.isBlank()) {
            sb.append(centrar(truncar(direccion, ANCHO))).append('\n');
        }
        if (telefono != null && !telefono.isBlank()) {
            sb.append(centrar("Tel: " + telefono)).append('\n');
        }
        sb.append(sep).append('\n');

        String numComp = String.format("%s%06d", prefijo, venta.getNumeroComprobante());
        sb.append(columnas("Comprobante:", numComp)).append('\n');
        sb.append(columnas("Fecha:", venta.getFecha().format(FMT_FECHA))).append('\n');
        sb.append(sepL).append('\n');

        for (DetalleVenta d : venta.getDetalles()) {
            String nombreProd = truncar(d.getProducto().getNombre(), 22);
            String qty        = String.valueOf(d.getCantidad());
            String precio     = formatearMoneda(d.getSubtotal());
            // Línea: nombre(22) + qty(4) + precio(resto)
            int espacios = ANCHO - nombreProd.length() - qty.length() - precio.length();
            sb.append(nombreProd)
              .append(" ".repeat(Math.max(1, espacios / 2)))
              .append(qty)
              .append(" ".repeat(Math.max(1, ANCHO - nombreProd.length() - qty.length() - precio.length() - Math.max(1, espacios / 2))))
              .append(precio)
              .append('\n');
        }

        sb.append(sepL).append('\n');
        sb.append(columnas("TOTAL:", formatearMoneda(venta.getTotal()))).append('\n');
        sb.append(columnas("Método:", venta.getMetodoPago().name())).append('\n');
        if (venta.getCliente() != null) {
            sb.append(columnas("Cliente:", truncar(venta.getCliente().getNombre(), 20))).append('\n');
        }
        sb.append(sep).append('\n');
        sb.append(centrar("¡Gracias por su compra!")).append('\n');
        sb.append(sep).append('\n');

        return sb.toString();
    }

    // ── Helpers de formato ────────────────────────────────────────────────────

    private String centrar(String texto) {
        if (texto.length() >= ANCHO) return texto;
        int pad = (ANCHO - texto.length()) / 2;
        return " ".repeat(pad) + texto;
    }

    private String columnas(String izq, String der) {
        int espacios = ANCHO - izq.length() - der.length();
        return izq + " ".repeat(Math.max(1, espacios)) + der;
    }

    private String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() > max ? texto.substring(0, max - 1) + "…" : texto;
    }

    private String formatearMoneda(BigDecimal valor) {
        return FMT_MONEDA.format(valor);
    }
}
