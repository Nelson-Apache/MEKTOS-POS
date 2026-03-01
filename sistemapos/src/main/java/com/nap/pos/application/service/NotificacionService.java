package com.nap.pos.application.service;

import com.nap.pos.domain.model.Notificacion;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.enums.Severidad;
import com.nap.pos.domain.model.enums.TipoNotificacion;
import com.nap.pos.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calcula las notificaciones activas del sistema en tiempo de ejecución.
 * Las notificaciones NO se persisten — se recalculan cada vez que se consultan.
 *
 * <p>Integración UI prevista:
 * <ul>
 *   <li>Al hacer login: llamar {@link #getNotificaciones()} y mostrar popup si hay resultados.</li>
 *   <li>Campana en barra principal: mostrar {@link #contarNotificaciones()} como badge.</li>
 *   <li>Panel de notificaciones: listar el resultado completo de {@link #getNotificaciones()}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final ProductoRepository    productoRepository;
    private final ConfiguracionService  configuracionService;

    /**
     * Retorna todas las notificaciones activas del sistema.
     * Actualmente: productos activos con stock ≤ stockMinimoGlobal.
     */
    public List<Notificacion> getNotificaciones() {
        int stockMinimo = configuracionService.obtener().getStockMinimoGlobal();
        return productoRepository.findByStockBajoYActivo(stockMinimo)
                .stream()
                .map(p -> toNotificacion(p, stockMinimo))
                .toList();
    }

    /**
     * Retorna solo el conteo — útil para mostrar el badge de la campana sin
     * construir todos los objetos Notificacion.
     */
    public int contarNotificaciones() {
        int stockMinimo = configuracionService.obtener().getStockMinimoGlobal();
        return productoRepository.findByStockBajoYActivo(stockMinimo).size();
    }

    // ── privado ─────────────────────────────────────────────────────────

    private Notificacion toNotificacion(Producto p, int stockMinimo) {
        Severidad severidad = p.getStock() == 0 ? Severidad.CRITICA : Severidad.ADVERTENCIA;
        String titulo   = "Stock bajo: " + p.getNombre();
        String mensaje  = "Stock actual: " + p.getStock() + " | Mínimo configurado: " + stockMinimo;
        return new Notificacion(TipoNotificacion.STOCK_BAJO, titulo, mensaje, severidad, p.getId());
    }
}
