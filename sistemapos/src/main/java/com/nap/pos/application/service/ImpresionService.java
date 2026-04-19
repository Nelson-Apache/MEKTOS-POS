package com.nap.pos.application.service;

import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.model.DetalleVenta;
import com.nap.pos.domain.model.Venta;
import com.nap.pos.domain.model.enums.MetodoPago;
import com.nap.pos.domain.model.enums.TipoPersona;
import com.nap.pos.domain.port.TicketPrinter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImpresionService {

    private static final int ANCHO = 44;
    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");
    private static final NumberFormat FMT_MONEDA =
            NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

    private final TicketPrinter ticketPrinter;
    private final ConfiguracionService configuracionService;

    public void imprimirTicket(Venta venta, BigDecimal montoRecibido) {
        try {
            ConfiguracionTienda config = configuracionService.obtener();
            String texto = formatearTicket(venta, config, montoRecibido);
            String nombreArchivo = config.getPrefijoComprobante().replaceAll("[^a-zA-Z0-9_-]", "")
                    + String.format("%06d", venta.getNumeroComprobante());
            ticketPrinter.imprimir(texto, nombreArchivo);
        } catch (Exception e) {
            log.warn("No se pudo imprimir el ticket de la venta {}: {}", venta.getId(), e.getMessage());
        }
    }

    public String formatearTicket(Venta venta, ConfiguracionTienda cfg, BigDecimal montoRecibido) {
        StringBuilder sb = new StringBuilder();
        String sep  = "=".repeat(ANCHO);
        String sepL = "-".repeat(ANCHO);

        // ── Encabezado tienda ─────────────────────────────────────
        sb.append(sep).append('\n');
        sb.append(centrar(cfg.getNombreTienda())).append('\n');
        if (str(cfg.getDireccion())) sb.append(centrar(truncar(cfg.getDireccion(), ANCHO))).append('\n');

        // ── Identificación fiscal ─────────────────────────────────
        sb.append(sepL).append('\n');
        if (cfg.getTipoPersona() == TipoPersona.JURIDICA) {
            if (str(cfg.getNit()))
                sb.append(centrar("NIT: " + cfg.getNit())).append('\n');
            if (str(cfg.getRazonSocial()))
                sb.append(centrar(truncar(cfg.getRazonSocial(), ANCHO))).append('\n');
        } else {
            if (str(cfg.getCedula()))
                sb.append(centrar("CC: " + cfg.getCedula())).append('\n');
            String nombreTitular = joinNonEmpty(cfg.getNombre(), cfg.getApellido());
            if (!nombreTitular.isBlank())
                sb.append(centrar(truncar(nombreTitular, ANCHO))).append('\n');
        }
        if (cfg.getRegimenTributario() != null)
            sb.append(centrar(truncar(cfg.getRegimenTributario().getDescripcion(), ANCHO))).append('\n');

        // ── Datos del comprobante ────────────────────────────────
        sb.append(sep).append('\n');
        String numComp = String.format("%s%06d", cfg.getPrefijoComprobante(), venta.getNumeroComprobante());
        sb.append(columnas("Comprobante N°:", numComp)).append('\n');
        sb.append(columnas("Fecha:", venta.getFecha().format(FMT_FECHA))).append('\n');

        // ── Detalle de productos ─────────────────────────────────
        sb.append(sepL).append('\n');
        sb.append(columnas("PRODUCTO", "CANT    V/UNIT    TOTAL")).append('\n');
        sb.append(sepL).append('\n');

        for (DetalleVenta d : venta.getDetalles()) {
            String nombre = truncar(d.getProducto().getNombre(), ANCHO);
            sb.append(nombre).append('\n');

            String cant    = String.valueOf(d.getCantidad());
            String unitStr = formatMoneda(d.getPrecioUnitario());
            String subStr  = formatMoneda(d.getSubtotal());
            // Línea de números alineada a la derecha
            String numLine = cant + "  x  " + unitStr + "  =  " + subStr;
            sb.append(derechaJustificar(numLine)).append('\n');
        }

        // ── Totales / IVA ────────────────────────────────────────
        sb.append(sepL).append('\n');
        boolean responsableIva = cfg.isResponsableIva() && cfg.getIvaPorDefecto() > 0;
        if (responsableIva) {
            int pct = cfg.getIvaPorDefecto();
            if (cfg.isPrecioConIvaIncluido()) {
                // Desglosar IVA incluido en el precio
                BigDecimal divisor = BigDecimal.ONE.add(
                        BigDecimal.valueOf(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                BigDecimal base = venta.getTotal().divide(divisor, 0, RoundingMode.HALF_UP);
                BigDecimal ivaAmt = venta.getTotal().subtract(base);
                sb.append(columnas("Subtotal (sin IVA):", formatMoneda(base))).append('\n');
                sb.append(columnas("IVA " + pct + "% (incluido):", formatMoneda(ivaAmt))).append('\n');
            } else {
                BigDecimal ivaAmt = venta.getTotal()
                        .multiply(BigDecimal.valueOf(pct))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                BigDecimal totalConIva = venta.getTotal().add(ivaAmt);
                sb.append(columnas("Subtotal:", formatMoneda(venta.getTotal()))).append('\n');
                sb.append(columnas("IVA " + pct + "%:", formatMoneda(ivaAmt))).append('\n');
                sb.append(columnas("TOTAL:", formatMoneda(totalConIva))).append('\n');
            }
        }
        if (!responsableIva || cfg.isPrecioConIvaIncluido()) {
            sb.append(columnas("TOTAL:", formatMoneda(venta.getTotal()))).append('\n');
        }

        // ── Datos de la transacción ──────────────────────────────
        sb.append(sepL).append('\n');
        String metodoPago = switch (venta.getMetodoPago()) {
            case EFECTIVO     -> "Efectivo";
            case TRANSFERENCIA -> "Transferencia";
            case CREDITO      -> "Crédito";
        };
        sb.append(columnas("Forma de pago:", metodoPago)).append('\n');

        if (venta.getCliente() != null)
            sb.append(columnas("Cliente:", truncar(venta.getCliente().getNombre(), 22))).append('\n');

        if (venta.getUsuario() != null) {
            String rol = venta.getUsuario().esAdmin() ? "Administrador" : "Cajero";
            String nombre = truncar(venta.getUsuario().getNombreCompleto(), 18);
            sb.append(columnas("Atendido por:", nombre + " (" + rol + ")")).append('\n');
        }

        // ── Efectivo: recibido y cambio ──────────────────────────
        if (MetodoPago.EFECTIVO.equals(venta.getMetodoPago()) && montoRecibido != null) {
            sb.append(sepL).append('\n');
            sb.append(columnas("Recibido:", formatMoneda(montoRecibido))).append('\n');
            BigDecimal cambio = montoRecibido.subtract(venta.getTotal()).max(BigDecimal.ZERO);
            sb.append(columnas("Cambio:", formatMoneda(cambio))).append('\n');
        }

        // ── Pie legal ────────────────────────────────────────────
        sb.append(sep).append('\n');
        String textoLegal = textoLegal(cfg);
        if (!textoLegal.isBlank())
            sb.append(centrar(textoLegal)).append('\n');
        sb.append(centrar("¡Gracias por su compra!")).append('\n');
        sb.append(sep).append('\n');

        return sb.toString();
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private String textoLegal(ConfiguracionTienda cfg) {
        if (cfg.getRegimenTributario() == null) return "";
        return switch (cfg.getRegimenTributario()) {
            case NO_RESPONSABLE_IVA -> "No somos responsables del IVA";
            case REGIMEN_ORDINARIO  -> "Factura equivalente POS - Reg. Ordinario";
            case REGIMEN_SIMPLE     -> "Comprobante POS - Régimen Simple de Tributación";
        };
    }

    private String centrar(String texto) {
        if (texto == null || texto.length() >= ANCHO) return texto == null ? "" : texto;
        int pad = (ANCHO - texto.length()) / 2;
        return " ".repeat(pad) + texto;
    }

    private String columnas(String izq, String der) {
        int espacios = ANCHO - izq.length() - der.length();
        return izq + " ".repeat(Math.max(1, espacios)) + der;
    }

    private String derechaJustificar(String texto) {
        if (texto.length() >= ANCHO) return texto;
        return " ".repeat(ANCHO - texto.length()) + texto;
    }

    private String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() > max ? texto.substring(0, max - 1) + "…" : texto;
    }

    private String formatMoneda(BigDecimal valor) {
        return FMT_MONEDA.format(valor);
    }

    private boolean str(String s) {
        return s != null && !s.isBlank();
    }

    private String joinNonEmpty(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }
}
