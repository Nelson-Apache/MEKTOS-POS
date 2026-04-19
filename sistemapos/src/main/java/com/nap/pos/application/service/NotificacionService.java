package com.nap.pos.application.service;

import com.nap.pos.domain.model.Cliente;
import com.nap.pos.domain.model.Notificacion;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.enums.PlazoPago;
import com.nap.pos.domain.model.enums.Severidad;
import com.nap.pos.domain.model.enums.TipoNotificacion;
import com.nap.pos.domain.repository.ClienteRepository;
import com.nap.pos.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final ProductoRepository    productoRepository;
    private final ClienteRepository     clienteRepository;
    private final ConfiguracionService  configuracionService;

    private static final int DIAS_ANTICIPACION = 5;

    public List<Notificacion> getNotificaciones() {
        List<Notificacion> resultado = new ArrayList<>();
        resultado.addAll(generarAlertasStock());
        resultado.addAll(generarAlertasPagos());
        return resultado;
    }

    public int contarNotificaciones() {
        return getNotificaciones().size();
    }

    // ── privado ─────────────────────────────────────────────────────────

    private List<Notificacion> generarAlertasStock() {
        int stockMinimo = configuracionService.obtener().getStockMinimoGlobal();
        return productoRepository.findByStockBajoYActivo(stockMinimo)
                .stream()
                .map(p -> toNotificacionStock(p, stockMinimo))
                .toList();
    }

    private List<Notificacion> generarAlertasPagos() {
        int diaActual = LocalDate.now().getDayOfMonth();
        List<Notificacion> alertas = new ArrayList<>();

        for (Cliente c : clienteRepository.findAllActivos()) {
            if (c.getPlazoPago() == null) continue;
            if (c.getSaldoUtilizado() == null || c.getSaldoUtilizado().compareTo(BigDecimal.ZERO) <= 0) continue;

            boolean alertaDia30 = estaEnVentana(diaActual, 30);
            boolean alertaDia15 = PlazoPago.QUINCE_DIAS.equals(c.getPlazoPago()) && estaEnVentana(diaActual, 15);

            if (alertaDia30) {
                alertas.add(toNotificacionPago(c, 30, diaActual));
            } else if (alertaDia15) {
                alertas.add(toNotificacionPago(c, 15, diaActual));
            }
        }
        return alertas;
    }

    /**
     * Retorna true si el día actual está dentro de la ventana de alerta
     * (los DIAS_ANTICIPACION días previos al vencimiento, o el propio día).
     */
    private boolean estaEnVentana(int diaActual, int diaVencimiento) {
        int inicio = diaVencimiento - DIAS_ANTICIPACION;
        return diaActual >= inicio && diaActual <= diaVencimiento;
    }

    private Notificacion toNotificacionStock(Producto p, int stockMinimo) {
        Severidad severidad = p.getStock() == 0 ? Severidad.CRITICA : Severidad.ADVERTENCIA;
        String titulo  = "Stock bajo: " + p.getNombre();
        String mensaje = "Stock actual: " + p.getStock() + " | Mínimo configurado: " + stockMinimo;
        return new Notificacion(TipoNotificacion.STOCK_BAJO, titulo, mensaje, severidad, p.getId());
    }

    private Notificacion toNotificacionPago(Cliente c, int diaVencimiento, int diaActual) {
        int diasRestantes = diaVencimiento - diaActual;
        Severidad severidad = diasRestantes <= 0 ? Severidad.CRITICA : Severidad.ADVERTENCIA;
        String titulo = "Pago próximo: " + c.getNombre();
        String mensaje = diasRestantes <= 0
                ? "Vencimiento hoy (día " + diaVencimiento + ") | Deuda: $" + c.getSaldoUtilizado().toPlainString()
                : "Vence en " + diasRestantes + " día(s) (día " + diaVencimiento + ") | Deuda: $" + c.getSaldoUtilizado().toPlainString();
        return new Notificacion(TipoNotificacion.PAGO_CREDITO_PROXIMO, titulo, mensaje, severidad, c.getId());
    }
}
