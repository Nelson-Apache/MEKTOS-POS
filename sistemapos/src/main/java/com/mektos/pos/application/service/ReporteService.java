package com.mektos.pos.application.service;

import com.mektos.pos.application.dto.*;
import com.mektos.pos.domain.exception.BusinessException;
import com.mektos.pos.domain.model.*;
import com.mektos.pos.domain.model.enums.EstadoVenta;
import com.mektos.pos.domain.model.enums.MetodoPago;
import com.mektos.pos.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final ClienteRepository clienteRepository;
    private final CajaRepository cajaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    /**
     * Reporte de ventas de un período de caja.
     * Desglosa el total por método de pago (efectivo, transferencia, crédito)
     * y lista los productos más vendidos, ordenados por cantidad.
     * Las ventas ANULADAS se cuentan pero no aportan al total de dinero.
     */
    public ReporteVentasDto reporteVentasPorCaja(Long cajaId) {
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new BusinessException("Caja con ID " + cajaId + " no encontrada."));

        List<Venta> todas = ventaRepository.findByCajaId(cajaId);

        List<Venta> completadas = todas.stream()
                .filter(v -> EstadoVenta.COMPLETADA.equals(v.getEstado()))
                .toList();

        long anuladas = todas.stream()
                .filter(v -> EstadoVenta.ANULADA.equals(v.getEstado()))
                .count();

        BigDecimal totalEfectivo      = sumarPorMetodo(completadas, MetodoPago.EFECTIVO);
        BigDecimal totalTransferencia = sumarPorMetodo(completadas, MetodoPago.TRANSFERENCIA);
        BigDecimal totalCredito       = sumarPorMetodo(completadas, MetodoPago.CREDITO);
        BigDecimal totalGeneral       = totalEfectivo.add(totalTransferencia).add(totalCredito);

        return new ReporteVentasDto(
                cajaId,
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                todas.size(),
                completadas.size(),
                (int) anuladas,
                totalEfectivo,
                totalTransferencia,
                totalCredito,
                totalGeneral,
                calcularTopProductosVendidos(completadas)
        );
    }

    /**
     * Reporte de compras realizadas a un proveedor.
     * Agrega todos los ítems del mismo producto a través de distintas compras,
     * ordenados de mayor a menor inversión.
     */
    public ReporteComprasDto reporteComprasPorProveedor(Long proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new BusinessException("Proveedor con ID " + proveedorId + " no encontrado."));

        List<Compra> compras = compraRepository.findByProveedorId(proveedorId);

        BigDecimal totalInvertido = compras.stream()
                .map(Compra::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteComprasDto(
                proveedorId,
                proveedor.getNombre(),
                compras.size(),
                totalInvertido,
                calcularProductosComprados(compras)
        );
    }

    /**
     * Reporte de créditos pendientes de todos los clientes.
     * Solo incluye clientes con saldo utilizado mayor a cero,
     * ordenados de mayor a menor deuda.
     */
    public ReporteCreditosDto reporteCreditos() {
        List<ClienteCreditoDto> conDeuda = clienteRepository.findAll().stream()
                .filter(c -> c.tieneCreditoHabilitado()
                          && c.getSaldoUtilizado().compareTo(BigDecimal.ZERO) > 0)
                .map(c -> new ClienteCreditoDto(
                        c.getId(),
                        c.getNombre(),
                        c.getCedula(),
                        c.getMontoCredito(),
                        c.getSaldoUtilizado(),
                        c.getSaldoDisponible()))
                .sorted(Comparator.comparing(ClienteCreditoDto::saldoUtilizado).reversed())
                .toList();

        BigDecimal totalDeuda = conDeuda.stream()
                .map(ClienteCreditoDto::saldoUtilizado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteCreditosDto(conDeuda.size(), totalDeuda, conDeuda);
    }

    /**
     * Reporte de inventario con alertas de stock.
     * Separa los productos activos en dos listas:
     * - Agotados (stock = 0): requieren reabastecimiento urgente.
     * - Bajo stock (0 < stock ≤ umbralBajoStock): están por agotarse.
     *
     * @param umbralBajoStock cantidad mínima aceptable (ej: 5 unidades)
     */
    public ReporteInventarioDto reporteInventario(int umbralBajoStock) {
        List<Producto> activos = productoRepository.findAllActivos();

        List<ProductoStockDto> agotados = activos.stream()
                .filter(p -> p.getStock() == 0)
                .map(this::toStockDto)
                .sorted(Comparator.comparing(ProductoStockDto::nombre))
                .toList();

        List<ProductoStockDto> bajoStock = activos.stream()
                .filter(p -> p.getStock() > 0 && p.getStock() <= umbralBajoStock)
                .map(this::toStockDto)
                .sorted(Comparator.comparingInt(ProductoStockDto::stock)) // el más crítico primero
                .toList();

        return new ReporteInventarioDto(
                activos.size(),
                agotados.size(),
                bajoStock.size(),
                umbralBajoStock,
                agotados,
                bajoStock
        );
    }

    /**
     * Reporte de rentabilidad de un mes específico.
     * Compara el total invertido en compras contra el total recaudado en ventas.
     * Calcula ganancia bruta y margen porcentual.
     *
     * @param anio año (ej: 2026)
     * @param mes  mes 1–12 (ej: 2 = febrero)
     */
    public ReporteRentabilidadDto reporteRentabilidad(int anio, int mes) {
        // Total invertido: suma de todas las compras del mes
        BigDecimal totalInvertido = compraRepository.findAll().stream()
                .filter(c -> c.getFecha().getYear() == anio
                          && c.getFecha().getMonthValue() == mes)
                .map(Compra::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total vendido: solo ventas COMPLETADAS del mes (las ANULADAS no generan ingreso)
        BigDecimal totalVendido = ventaRepository.findAll().stream()
                .filter(v -> EstadoVenta.COMPLETADA.equals(v.getEstado())
                          && v.getFecha().getYear() == anio
                          && v.getFecha().getMonthValue() == mes)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciaBruta = totalVendido.subtract(totalInvertido);

        // El margen es null si no hubo ventas — evita división por cero
        BigDecimal margenPorcentaje = null;
        if (totalVendido.compareTo(BigDecimal.ZERO) > 0) {
            margenPorcentaje = gananciaBruta
                    .divide(totalVendido, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new ReporteRentabilidadDto(
                anio,
                mes,
                totalInvertido,
                totalVendido,
                gananciaBruta,
                margenPorcentaje,
                gananciaBruta.compareTo(BigDecimal.ZERO) < 0
        );
    }

    /**
     * Reporte de rentabilidad de un año completo, con desglose mes a mes.
     * Siempre retorna los 12 meses (enero–diciembre); los meses sin actividad
     * aparecen con todos los valores en cero.
     *
     * Carga compras y ventas una sola vez para evitar N consultas al repositorio.
     *
     * @param anio año completo (ej: 2026)
     */
    public ReporteRentabilidadAnualDto reporteRentabilidadAnual(int anio) {
        final String[] NOMBRES = {"","Enero","Febrero","Marzo","Abril","Mayo","Junio",
                                  "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};

        // Carga única — filtra por año en memoria
        List<Compra> comprasAnio = compraRepository.findAll().stream()
                .filter(c -> c.getFecha().getYear() == anio)
                .toList();

        List<Venta> ventasAnio = ventaRepository.findAll().stream()
                .filter(v -> EstadoVenta.COMPLETADA.equals(v.getEstado())
                          && v.getFecha().getYear() == anio)
                .toList();

        List<RentabilidadMensualDto> meses = new ArrayList<>(12);
        BigDecimal totalInvertidoAnual = BigDecimal.ZERO;
        BigDecimal totalVendidoAnual   = BigDecimal.ZERO;

        for (int mes = 1; mes <= 12; mes++) {
            final int m = mes;

            BigDecimal invertidoMes = comprasAnio.stream()
                    .filter(c -> c.getFecha().getMonthValue() == m)
                    .map(Compra::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal vendidoMes = ventasAnio.stream()
                    .filter(v -> v.getFecha().getMonthValue() == m)
                    .map(Venta::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal gananciaMes = vendidoMes.subtract(invertidoMes);

            meses.add(new RentabilidadMensualDto(
                    mes,
                    NOMBRES[mes],
                    invertidoMes,
                    vendidoMes,
                    gananciaMes,
                    gananciaMes.compareTo(BigDecimal.ZERO) < 0
            ));

            totalInvertidoAnual = totalInvertidoAnual.add(invertidoMes);
            totalVendidoAnual   = totalVendidoAnual.add(vendidoMes);
        }

        BigDecimal gananciaAnual = totalVendidoAnual.subtract(totalInvertidoAnual);

        BigDecimal margenAnual = null;
        if (totalVendidoAnual.compareTo(BigDecimal.ZERO) > 0) {
            margenAnual = gananciaAnual
                    .divide(totalVendidoAnual, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new ReporteRentabilidadAnualDto(
                anio,
                totalInvertidoAnual,
                totalVendidoAnual,
                gananciaAnual,
                margenAnual,
                gananciaAnual.compareTo(BigDecimal.ZERO) < 0,
                meses
        );
    }

    // ----- helpers privados -----

    private ProductoStockDto toStockDto(Producto p) {
        String nombreProveedor = (p.getProveedorPrincipal() != null)
                ? p.getProveedorPrincipal().getNombre()
                : "Sin proveedor";
        return new ProductoStockDto(
                p.getId(),
                p.getNombre(),
                p.getCodigoBarras(),
                nombreProveedor,
                p.getStock(),
                p.getPrecioCompra(),
                p.getPrecioVenta()
        );
    }

    private BigDecimal sumarPorMetodo(List<Venta> ventas, MetodoPago metodo) {
        return ventas.stream()
                .filter(v -> metodo.equals(v.getMetodoPago()))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Agrega unidades y dinero por producto a través de todas las ventas completadas. */
    private List<ProductoVendidoDto> calcularTopProductosVendidos(List<Venta> completadas) {
        Map<Long, ProductoVendidoDto> mapa = new LinkedHashMap<>();
        for (Venta venta : completadas) {
            for (DetalleVenta d : venta.getDetalles()) {
                Long id = d.getProducto().getId();
                mapa.merge(id,
                        new ProductoVendidoDto(
                                d.getProducto().getNombre(),
                                d.getProducto().getCodigoBarras(),
                                d.getCantidad(),
                                d.getSubtotal()),
                        (prev, nuevo) -> new ProductoVendidoDto(
                                prev.nombre(),
                                prev.codigoBarras(),
                                prev.cantidadVendida() + nuevo.cantidadVendida(),
                                prev.totalGenerado().add(nuevo.totalGenerado())));
            }
        }
        return mapa.values().stream()
                .sorted(Comparator.comparing(ProductoVendidoDto::cantidadVendida).reversed())
                .toList();
    }

    /** Agrega unidades e inversión por producto a través de todas las compras del proveedor. */
    private List<ProductoCompradoDto> calcularProductosComprados(List<Compra> compras) {
        Map<Long, ProductoCompradoDto> mapa = new LinkedHashMap<>();
        for (Compra compra : compras) {
            for (DetalleCompra d : compra.getDetalles()) {
                Long id = d.getProducto().getId();
                mapa.merge(id,
                        new ProductoCompradoDto(
                                d.getProducto().getNombre(),
                                d.getProducto().getCodigoBarras(),
                                d.getCantidad(),
                                d.getSubtotal()),
                        (prev, nuevo) -> new ProductoCompradoDto(
                                prev.nombre(),
                                prev.codigoBarras(),
                                prev.cantidadTotal() + nuevo.cantidadTotal(),
                                prev.totalInvertido().add(nuevo.totalInvertido())));
            }
        }
        return mapa.values().stream()
                .sorted(Comparator.comparing(ProductoCompradoDto::totalInvertido).reversed())
                .toList();
    }
}
