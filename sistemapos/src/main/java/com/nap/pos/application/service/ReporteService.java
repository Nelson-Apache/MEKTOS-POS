package com.nap.pos.application.service;

import com.nap.pos.application.dto.*;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.*;
import com.nap.pos.domain.model.enums.EstadoVenta;
import com.nap.pos.domain.model.enums.MetodoPago;
import com.nap.pos.domain.model.enums.FuenteGasto;
import com.nap.pos.domain.model.enums.TipoGasto;
import com.nap.pos.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    private final GastoRepository gastoRepository;

    /**
     * Reporte de ventas de un período de caja.
     * Desglosa el total por método de pago (efectivo, transferencia, crédito)
     * lista el detalle de cada venta y muestra los productos más vendidos.
     * Las ventas ANULADAS se cuentan pero no aportan al total de dinero.
     */
    @Transactional(readOnly = true)
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

        LocalDateTime apertura = caja.getFechaApertura();
        LocalDateTime cierre   = caja.getFechaCierre() != null ? caja.getFechaCierre() : LocalDateTime.now();
        BigDecimal totalGastosCaja = listaSegura(gastoRepository.findAll()).stream()
                .filter(g -> FuenteGasto.CAJA.equals(g.getFuentePago())
                          && g.getFecha() != null
                          && !g.getFecha().isBefore(apertura)
                          && !g.getFecha().isAfter(cierre))
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                totalGastosCaja,
                calcularDetalleVentas(todas),
                calcularComprasPorCliente(completadas),
                calcularTopProductosVendidos(completadas)
        );
    }

    /**
     * Reporte de compras realizadas a un proveedor.
     * Agrega todos los ítems del mismo producto a través de distintas compras,
     * ordenados de mayor a menor inversión.
     */
    @Transactional(readOnly = true)
    public ReporteComprasDto reporteComprasPorProveedor(Long proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new BusinessException("Proveedor con ID " + proveedorId + " no encontrado."));

        List<Compra> compras = listaSegura(compraRepository.findByProveedorId(proveedorId));

        BigDecimal totalInvertido = compras.stream()
                .map(c -> c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalUnidades = calcularUnidadesCompradas(compras);

        BigDecimal ticketPromedio = compras.isEmpty()
                ? BigDecimal.ZERO
                : totalInvertido.divide(BigDecimal.valueOf(compras.size()), 2, RoundingMode.HALF_UP);

        LocalDateTime fechaUltimaCompra = compras.stream()
                .map(Compra::getFecha)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new ReporteComprasDto(
                proveedorId,
                proveedor.getNombre(),
                compras.size(),
                totalInvertido,
                totalUnidades,
                ticketPromedio,
                fechaUltimaCompra,
                calcularHistorialCompras(compras),
                calcularProductosComprados(compras)
        );
    }

    /**
     * Reporte de créditos pendientes de todos los clientes.
     * Solo incluye clientes con saldo utilizado mayor a cero,
     * ordenados de mayor a menor deuda.
     */
    @Transactional(readOnly = true)
    public ReporteCreditosDto reporteCreditos() {
        return reporteCreditos(null);
    }

    /**
     * Reporte de créditos pendientes.
     * Si clienteId es null, retorna el reporte general; si no,
     * solo evalúa al cliente indicado.
     */
    @Transactional(readOnly = true)
    public ReporteCreditosDto reporteCreditos(Long clienteId) {
        List<ClienteCreditoDto> conDeuda = listaSegura(clienteRepository.findAll()).stream()
                .filter(c -> clienteId == null || Objects.equals(c.getId(), clienteId))
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
     * Historial de compras (ventas) de un cliente.
     * Incluye solo ventas a crédito asociadas al cliente,
     * ordenadas de la más reciente a la más antigua.
     */
    @Transactional(readOnly = true)
    public List<Venta> historialComprasCliente(Long clienteId) {
        if (clienteId == null) return List.of();

        return listaSegura(ventaRepository.findAll()).stream()
                .filter(v -> v != null
                        && v.getCliente() != null
                        && MetodoPago.CREDITO.equals(v.getMetodoPago())
                        && Objects.equals(v.getCliente().getId(), clienteId))
                .sorted(Comparator.comparing(Venta::getFecha,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    /**
     * Reporte de inventario con alertas de stock.
     * Separa los productos activos en dos listas:
     * - Agotados (stock = 0): requieren reabastecimiento urgente.
     * - Bajo stock (0 < stock ≤ umbralBajoStock): están por agotarse.
     *
     * @param umbralBajoStock cantidad mínima aceptable (ej: 5 unidades)
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public ReporteRentabilidadDto reporteRentabilidad(int anio, int mes) {
        List<Compra> compras = listaSegura(compraRepository.findAll());
        List<Venta> ventas = listaSegura(ventaRepository.findAll());
        List<Caja> cajas = listaSegura(cajaRepository.findAll());

        BigDecimal totalInvertido = compras.stream()
                .filter(c -> c.getFecha().getYear() == anio
                          && c.getFecha().getMonthValue() == mes)
                .map(Compra::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVendido = ventas.stream()
                .filter(v -> EstadoVenta.COMPLETADA.equals(v.getEstado())
                          && v.getFecha().getYear() == anio
                          && v.getFecha().getMonthValue() == mes)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGastos = listaSegura(gastoRepository.findAll()).stream()
                .filter(g -> g.getFecha() != null
                          && g.getFecha().getYear() == anio
                          && g.getFecha().getMonthValue() == mes)
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Integer, BigDecimal> desajusteMes = calcularDesajusteCajaPorMes(anio, cajas, ventas);
        BigDecimal ajusteCaja = desajusteMes.getOrDefault(mes, BigDecimal.ZERO);

        BigDecimal gananciaBruta = totalVendido
                .subtract(totalInvertido)
                .subtract(totalGastos)
                .add(ajusteCaja);

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
                totalGastos,
                ajusteCaja,
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
    @Transactional(readOnly = true)
    public ReporteRentabilidadAnualDto reporteRentabilidadAnual(int anio) {
        final String[] NOMBRES = {"","Enero","Febrero","Marzo","Abril","Mayo","Junio",
                                  "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};

        List<Compra> comprasAnio = listaSegura(compraRepository.findAll()).stream()
                .filter(c -> c.getFecha().getYear() == anio)
                .toList();

        List<Venta> todasVentas = listaSegura(ventaRepository.findAll());
        List<Venta> ventasAnio = todasVentas.stream()
                .filter(v -> EstadoVenta.COMPLETADA.equals(v.getEstado())
                          && v.getFecha().getYear() == anio)
                .toList();

        List<Gasto> gastosAnio = listaSegura(gastoRepository.findAll()).stream()
                .filter(g -> g.getFecha() != null && g.getFecha().getYear() == anio)
                .toList();

        List<Caja> cajas = listaSegura(cajaRepository.findAll());
        Map<Integer, BigDecimal> desajusteMes = calcularDesajusteCajaPorMes(anio, cajas, todasVentas);

        List<RentabilidadMensualDto> meses = new ArrayList<>(12);
        BigDecimal totalInvertidoAnual  = BigDecimal.ZERO;
        BigDecimal totalVendidoAnual    = BigDecimal.ZERO;
        BigDecimal totalGastosAnual     = BigDecimal.ZERO;
        BigDecimal totalAjusteCajaAnual = BigDecimal.ZERO;

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

            BigDecimal gastosMes = gastosAnio.stream()
                    .filter(g -> g.getFecha().getMonthValue() == m)
                    .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal ajusteCaja = desajusteMes.getOrDefault(mes, BigDecimal.ZERO);
            BigDecimal gananciaMes = vendidoMes
                    .subtract(invertidoMes)
                    .subtract(gastosMes)
                    .add(ajusteCaja);

            meses.add(new RentabilidadMensualDto(
                    mes,
                    NOMBRES[mes],
                    invertidoMes,
                    vendidoMes,
                    gastosMes,
                    ajusteCaja,
                    gananciaMes,
                    gananciaMes.compareTo(BigDecimal.ZERO) < 0
            ));

            totalInvertidoAnual  = totalInvertidoAnual.add(invertidoMes);
            totalVendidoAnual    = totalVendidoAnual.add(vendidoMes);
            totalGastosAnual     = totalGastosAnual.add(gastosMes);
            totalAjusteCajaAnual = totalAjusteCajaAnual.add(ajusteCaja);
        }

        BigDecimal gananciaAnual = totalVendidoAnual
                .subtract(totalInvertidoAnual)
                .subtract(totalGastosAnual)
                .add(totalAjusteCajaAnual);

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
                totalGastosAnual,
                totalAjusteCajaAnual,
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

        private <T> List<T> listaSegura(List<T> lista) {
                return lista != null ? lista : List.of();
        }

        private Map<Integer, BigDecimal> calcularDesajusteCajaPorMes(int anio, List<Caja> cajas, List<Venta> ventas) {
                Map<Integer, BigDecimal> desajusteMes = new HashMap<>();
                if (cajas == null || cajas.isEmpty()) return desajusteMes;

                Map<Long, BigDecimal> efectivoPorCaja = new HashMap<>();
                if (ventas != null) {
                        for (Venta venta : ventas) {
                                if (venta == null
                                                || !EstadoVenta.COMPLETADA.equals(venta.getEstado())
                                                || !MetodoPago.EFECTIVO.equals(venta.getMetodoPago())
                                                || venta.getCaja() == null
                                                || venta.getCaja().getId() == null) {
                                        continue;
                                }
                                BigDecimal totalVenta = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
                                efectivoPorCaja.merge(venta.getCaja().getId(), totalVenta, BigDecimal::add);
                        }
                }

                for (Caja caja : cajas) {
                        if (caja == null
                                        || caja.getId() == null
                                        || caja.getFechaCierre() == null
                                        || caja.getFechaCierre().getYear() != anio
                                        || caja.getMontoFinal() == null) {
                                continue;
                        }

                        BigDecimal montoInicial = caja.getMontoInicial() != null ? caja.getMontoInicial() : BigDecimal.ZERO;
                        BigDecimal efectivoCaja = efectivoPorCaja.getOrDefault(caja.getId(), BigDecimal.ZERO);
                        BigDecimal esperadoCaja = montoInicial.add(efectivoCaja);
                        BigDecimal desajusteCaja = caja.getMontoFinal().subtract(esperadoCaja);

                        int mes = caja.getFechaCierre().getMonthValue();
                        desajusteMes.merge(mes, desajusteCaja, BigDecimal::add);
                }

                return desajusteMes;
        }

    private BigDecimal sumarPorMetodo(List<Venta> ventas, MetodoPago metodo) {
        return ventas.stream()
                .filter(v -> metodo.equals(v.getMetodoPago()))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<VentaDetalleDto> calcularDetalleVentas(List<Venta> ventas) {
        return ventas.stream()
                .sorted(Comparator.comparing(Venta::getFecha,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(v -> {
                    Cliente c = v.getCliente();
                    return new VentaDetalleDto(
                            v.getId(),
                            v.getNumeroComprobante(),
                            v.getFecha(),
                            c != null ? c.getNombre() : null,
                            c != null ? c.getCedula() : null,
                            v.getMetodoPago(),
                            v.getEstado(),
                            v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO
                    );
                })
                .toList();
    }

    private List<ClienteProductoVendidoDto> calcularComprasPorCliente(List<Venta> completadas) {
        Map<ClienteProductoKey, ClienteProductoVendidoDto> mapa = new LinkedHashMap<>();

        for (Venta venta : completadas) {
            Cliente cliente = venta.getCliente();
            if (cliente == null) continue;

            for (DetalleVenta d : venta.getDetalles()) {
                Producto producto = d.getProducto();
                ClienteProductoKey key = new ClienteProductoKey(cliente.getId(), producto.getId());

                mapa.merge(key,
                        new ClienteProductoVendidoDto(
                                cliente.getId(),
                                cliente.getNombre(),
                                cliente.getCedula(),
                                producto.getNombre(),
                                producto.getCodigoBarras(),
                                d.getCantidad(),
                                d.getSubtotal()),
                        (prev, nuevo) -> new ClienteProductoVendidoDto(
                                prev.clienteId(),
                                prev.clienteNombre(),
                                prev.clienteCedula(),
                                prev.productoNombre(),
                                prev.codigoBarras(),
                                prev.cantidadComprada() + nuevo.cantidadComprada(),
                                prev.totalComprado().add(nuevo.totalComprado())
                        ));
            }
        }

        return mapa.values().stream()
                .sorted(Comparator.comparing(ClienteProductoVendidoDto::clienteNombre,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(ClienteProductoVendidoDto::totalComprado, Comparator.reverseOrder())
                        .thenComparing(ClienteProductoVendidoDto::productoNombre,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private record ClienteProductoKey(Long clienteId, Long productoId) {}

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

    private int calcularUnidadesCompradas(List<Compra> compras) {
        return compras.stream()
                .mapToInt(compra -> listaSegura(compra.getDetalles()).stream()
                        .filter(Objects::nonNull)
                        .mapToInt(DetalleCompra::getCantidad)
                        .sum())
                .sum();
    }

    private List<CompraHistorialDto> calcularHistorialCompras(List<Compra> compras) {
        return compras.stream()
                .sorted(Comparator.comparing(Compra::getFecha,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(compra -> {
                    List<DetalleCompra> detalles = listaSegura(compra.getDetalles());
                    int unidades = detalles.stream()
                            .filter(Objects::nonNull)
                            .mapToInt(DetalleCompra::getCantidad)
                            .sum();

                    BigDecimal totalCompra = compra.getTotal() != null
                            ? compra.getTotal()
                            : detalles.stream()
                                    .filter(Objects::nonNull)
                                    .map(DetalleCompra::getSubtotal)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<CompraProductoDetalleDto> productos = detalles.stream()
                            .filter(Objects::nonNull)
                            .map(d -> new CompraProductoDetalleDto(
                                    d.getProducto() != null && d.getProducto().getNombre() != null
                                            ? d.getProducto().getNombre()
                                            : "Producto",
                                    d.getProducto() != null ? d.getProducto().getCodigoBarras() : null,
                                    d.getCantidad(),
                                    d.getPrecioCompraUnitario() != null ? d.getPrecioCompraUnitario() : BigDecimal.ZERO,
                                    d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO
                            ))
                            .toList();

                    return new CompraHistorialDto(
                            compra.getId(),
                            compra.getFecha(),
                            compra.getNumeroFactura(),
                            productos.size(),
                            unidades,
                            totalCompra,
                            productos
                    );
                })
                .toList();
    }

    /**
     * Reporte consolidado de gastos operativos.
     * Separa los registros en dos grupos: compras-gasto y pagos.
     * Calcula totales y construye el detalle completo ordenado por fecha descendente.
     */
    @Transactional(readOnly = true)
    public ReporteGastosDto reporteGastos() {
        List<Gasto> todos = listaSegura(gastoRepository.findAll());

        List<Gasto> comprasGasto = todos.stream()
                .filter(g -> TipoGasto.COMPRA_GASTO.equals(g.getTipo()))
                .toList();

        List<Gasto> pagos = todos.stream()
                .filter(g -> TipoGasto.PAGO.equals(g.getTipo()))
                .toList();

        BigDecimal montoCompras = comprasGasto.stream()
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoPagos = pagos.stream()
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoFuenteCaja = todos.stream()
                .filter(g -> FuenteGasto.CAJA.equals(g.getFuentePago()))
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoFuenteTransferencia = todos.stream()
                .filter(g -> FuenteGasto.TRANSFERENCIA.equals(g.getFuentePago()))
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<GastoDetalleDto> detalles = todos.stream()
                .sorted(Comparator.comparing(Gasto::getFecha,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(g -> new GastoDetalleDto(
                        g.getId(),
                        g.getFecha(),
                        g.getTipo(),
                        g.getFuentePago(),
                        g.getConcepto(),
                        g.getCategoria(),
                        g.getMonto(),
                        g.getProveedor(),
                        g.getReferencia(),
                        g.getNotas(),
                        g.getUsuario() != null ? g.getUsuario().getNombreCompleto() : null
                ))
                .toList();

        return new ReporteGastosDto(
                todos.size(),
                comprasGasto.size(),
                pagos.size(),
                montoCompras.add(montoPagos),
                montoCompras,
                montoPagos,
                montoFuenteCaja,
                montoFuenteTransferencia,
                detalles
        );
    }

    /** Agrega unidades e inversión por producto a través de todas las compras del proveedor. */
    private List<ProductoCompradoDto> calcularProductosComprados(List<Compra> compras) {
        Map<Long, ProductoCompradoDto> mapa = new LinkedHashMap<>();
        for (Compra compra : compras) {
            for (DetalleCompra d : listaSegura(compra.getDetalles())) {
                if (d == null || d.getProducto() == null || d.getProducto().getId() == null) continue;
                Long id = d.getProducto().getId();
                mapa.merge(id,
                        new ProductoCompradoDto(
                                d.getProducto().getNombre() != null ? d.getProducto().getNombre() : "Producto",
                                d.getProducto().getCodigoBarras(),
                                d.getCantidad(),
                                d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO),
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
