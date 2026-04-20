package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.*;
import com.nap.pos.domain.model.enums.EstadoVenta;
import com.nap.pos.domain.model.enums.MetodoPago;
import com.nap.pos.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaRepository cajaRepository;
    private final ConfiguracionService configuracionService;
    private final ImpresionService impresionService;

    /**
     * Datos mínimos que la UI envía por cada producto en la venta.
     * El precio unitario se toma del producto en BD — nunca de la UI.
     */
    public record ItemVenta(Long productoId, int cantidad) {}

    /**
     * Registra una venta completa:
     * 1. Construye los detalles y calcula el total en el dominio.
     * 2. Valida las reglas de negocio (caja abierta, crédito con cliente, etc.).
     * 3. Si es a crédito: registra el cargo en el saldo del cliente.
     * 4. Descuenta el stock de cada producto vendido.
     */
    @Transactional
    public Venta registrarVenta(Long cajaId, Long usuarioId, Long clienteId,
                                MetodoPago metodoPago, List<ItemVenta> items,
                                java.math.BigDecimal montoRecibido) {
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new BusinessException("Caja con ID " + cajaId + " no encontrada."));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Usuario con ID " + usuarioId + " no encontrado."));
        Cliente cliente = (clienteId != null)
                ? clienteRepository.findById(clienteId)
                        .orElseThrow(() -> new BusinessException("Cliente con ID " + clienteId + " no encontrado."))
                : null;

        // Número de comprobante secuencial: max actual + 1 ó número inicial configurado
        long numeroComprobante = ventaRepository.findMaxNumeroComprobante()
                .map(n -> n + 1)
                .orElseGet(() -> (long) configuracionService.obtener().getNumeroInicialComprobante());

        // Construir detalles usando el precio de venta actual del producto
        List<DetalleVenta> detalles = new ArrayList<>();
        for (ItemVenta item : items) {
            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new BusinessException("Producto con ID " + item.productoId() + " no encontrado."));
            detalles.add(new DetalleVenta(producto, item.cantidad(), producto.getPrecioVenta()));
        }

        // El dominio calcula el total y valida las reglas de negocio
        Venta venta = Venta.builder()
                .caja(caja)
                .usuario(usuario)
                .cliente(cliente)
                .metodoPago(metodoPago)
                .detalles(detalles)
                .numeroComprobante(numeroComprobante)
                .build();
        venta.calcularTotal();
        venta.validar();

        // Registrar el crédito en el cliente ANTES de descontar stock
        // (si el crédito falla, el @Transactional hace rollback de todo)
        if (MetodoPago.CREDITO.equals(metodoPago) && cliente != null) {
            cliente.utilizarCredito(venta.getTotal());
            clienteRepository.save(cliente);
        }

        // Descontar stock de cada producto
        detalles.forEach(detalle -> {
            Producto p = detalle.getProducto();
            p.descontarStock(detalle.getCantidad());
            productoRepository.save(p);
        });

        Venta ventaGuardada = ventaRepository.save(venta);

        // Se imprime usando `venta` (pre-save) porque sus objetos anidados (producto, usuario)
        // están completamente cargados. `ventaGuardada` solo tiene proxies JPA con el ID.
        // `venta` ya tiene el numeroComprobante asignado antes del save.
        impresionService.imprimirTicket(venta, montoRecibido);

        return ventaGuardada;
    }

    /**
     * Anula una venta en estado COMPLETADA:
     * 1. Restaura el crédito del cliente si fue venta a crédito.
     * 2. Devuelve el stock de cada producto.
     * 3. Marca la venta como ANULADA.
     */
    @Transactional
    public Venta anularVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new BusinessException("Venta con ID " + ventaId + " no encontrada."));

        venta.anular(); // lanza BusinessException si ya está ANULADA

        // Restaurar crédito si fue venta a crédito
        if (MetodoPago.CREDITO.equals(venta.getMetodoPago()) && venta.getCliente() != null) {
            Cliente cliente = clienteRepository.findById(venta.getCliente().getId())
                    .orElseThrow(() -> new BusinessException("Cliente no encontrado al anular la venta."));
            cliente.abonar(venta.getTotal());
            clienteRepository.save(cliente);
        }

        // Restaurar stock de cada producto
        venta.getDetalles().forEach(detalle -> {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new BusinessException("Producto no encontrado al anular la venta."));
            producto.incrementarStock(detalle.getCantidad());
            productoRepository.save(producto);
        });

        return ventaRepository.save(venta);
    }

    // Usado al cerrar caja para calcular el resumen del período
    @Transactional(readOnly = true)
    public List<Venta> findByCajaId(Long cajaId) {
        return ventaRepository.findByCajaId(cajaId);
    }

        @Transactional(readOnly = true)
        public void reimprimirTicket(Long ventaId, BigDecimal montoRecibido) {
                Venta venta = ventaRepository.findById(ventaId)
                                .orElseThrow(() -> new BusinessException("Venta con ID " + ventaId + " no encontrada."));

                if (!EstadoVenta.COMPLETADA.equals(venta.getEstado())) {
                        throw new BusinessException("Solo se pueden imprimir facturas de ventas completadas.");
                }

                if (MetodoPago.EFECTIVO.equals(venta.getMetodoPago())
                                && montoRecibido != null
                                && venta.getTotal() != null
                                && montoRecibido.compareTo(venta.getTotal()) < 0) {
                        throw new BusinessException("El monto recibido no puede ser menor al total de la venta.");
                }

                impresionService.imprimirTicket(venta, montoRecibido);
        }

    @Transactional(readOnly = true)
    public List<Venta> findAll() {
        return ventaRepository.findAll();
    }
}
