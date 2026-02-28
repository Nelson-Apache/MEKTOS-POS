package com.mektos.pos.application.service;

import com.mektos.pos.application.dto.*;
import com.mektos.pos.domain.exception.BusinessException;
import com.mektos.pos.domain.model.*;
import com.mektos.pos.domain.model.enums.*;
import com.mektos.pos.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private CompraRepository compraRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private CajaRepository cajaRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private ProductoRepository productoRepository;

    @InjectMocks
    private ReporteService reporteService;

    // ----------------------------------------------------------------
    // reporteVentasPorCaja
    // ----------------------------------------------------------------

    @Test
    void reporteVentasPorCaja_conVentasMixtas_calculaTotalesPorMetodo() {
        Caja caja = Caja.builder()
                .id(1L).estado(EstadoCaja.ABIERTA)
                .fechaApertura(LocalDateTime.now().minusHours(6))
                .montoInicial(new BigDecimal("200000")).build();

        Producto prodA = productoBuilder(1L, "Café", "BAR-A");
        Producto prodB = productoBuilder(2L, "Agua", "BAR-B");

        // Venta 1: EFECTIVO — 3 unidades de Café a 2000 = subtotal 6000
        DetalleVenta d1 = new DetalleVenta(prodA, 3, new BigDecimal("2000"));
        Venta v1 = ventaBuilder(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("6000"), List.of(d1));

        // Venta 2: CREDITO — 2 unidades de Agua a 1500 = subtotal 3000
        DetalleVenta d2 = new DetalleVenta(prodB, 2, new BigDecimal("1500"));
        Venta v2 = ventaBuilder(MetodoPago.CREDITO, EstadoVenta.COMPLETADA,
                new BigDecimal("3000"), List.of(d2));

        // Venta 3: ANULADA — no debe sumar al total
        Venta v3 = ventaBuilder(MetodoPago.EFECTIVO, EstadoVenta.ANULADA,
                new BigDecimal("5000"), List.of());

        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(ventaRepository.findByCajaId(1L)).thenReturn(List.of(v1, v2, v3));

        ReporteVentasDto reporte = reporteService.reporteVentasPorCaja(1L);

        assertThat(reporte.totalVentas()).isEqualTo(3);
        assertThat(reporte.ventasCompletadas()).isEqualTo(2);
        assertThat(reporte.ventasAnuladas()).isEqualTo(1);
        assertThat(reporte.totalEfectivo()).isEqualByComparingTo("6000");
        assertThat(reporte.totalCredito()).isEqualByComparingTo("3000");
        assertThat(reporte.totalTransferencia()).isEqualByComparingTo("0");
        // Total general solo suma completadas: 6000 + 3000 = 9000
        assertThat(reporte.totalGeneral()).isEqualByComparingTo("9000");
    }

    @Test
    void reporteVentasPorCaja_topProductos_ordenadoPorCantidadDesc() {
        Caja caja = cajaBuilder();
        Producto prodA = productoBuilder(1L, "Café",  "BAR-A");
        Producto prodB = productoBuilder(2L, "Agua",  "BAR-B");
        Producto prodC = productoBuilder(3L, "Jugo",  "BAR-C");

        // V1: prodA(2 uds), prodB(5 uds)
        Venta v1 = ventaBuilder(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("14000"),
                List.of(new DetalleVenta(prodA, 2, new BigDecimal("2000")),
                        new DetalleVenta(prodB, 5, new BigDecimal("2000"))));

        // V2: prodA(3 uds más), prodC(1 ud)
        Venta v2 = ventaBuilder(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("8000"),
                List.of(new DetalleVenta(prodA, 3, new BigDecimal("2000")),
                        new DetalleVenta(prodC, 1, new BigDecimal("2000"))));

        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(ventaRepository.findByCajaId(1L)).thenReturn(List.of(v1, v2));

        ReporteVentasDto reporte = reporteService.reporteVentasPorCaja(1L);

        List<ProductoVendidoDto> top = reporte.topProductos();
        // prodA total: 2+3=5 uds, prodB: 5 uds, prodC: 1 ud
        // Ordenado desc por cantidad: prodB(5) o prodA(5) primero, prodC(1) último
        assertThat(top).hasSize(3);
        assertThat(top.get(2).nombre()).isEqualTo("Jugo"); // el menos vendido va al final
        // prodA acumula: 2+3=5 unidades
        ProductoVendidoDto cafe = top.stream()
                .filter(p -> p.nombre().equals("Café")).findFirst().orElseThrow();
        assertThat(cafe.cantidadVendida()).isEqualTo(5);
        assertThat(cafe.totalGenerado()).isEqualByComparingTo("10000"); // 5 × 2000
    }

    @Test
    void reporteVentasPorCaja_cajaNoExiste_lanzaBusinessException() {
        when(cajaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> reporteService.reporteVentasPorCaja(99L));
    }

    // ----------------------------------------------------------------
    // reporteComprasPorProveedor
    // ----------------------------------------------------------------

    @Test
    void reporteComprasPorProveedor_variosItems_agregaYOrdena() {
        Proveedor proveedor = Proveedor.builder()
                .id(1L).nombre("Distribuidora XYZ")
                .porcentajeGanancia(new BigDecimal("30")).activo(true).build();

        Producto prodA = productoBuilder(1L, "Café",  "BAR-A");
        Producto prodB = productoBuilder(2L, "Azúcar", "BAR-B");

        // Compra 1: 100 uds de Café a 800 c/u = 80.000; 50 uds de Azúcar a 500 = 25.000
        Compra c1 = compraBuilder(proveedor, List.of(
                new DetalleCompra(prodA, 100, new BigDecimal("800")),
                new DetalleCompra(prodB,  50, new BigDecimal("500"))));

        // Compra 2: 200 uds más de Café a 750 = 150.000
        Compra c2 = compraBuilder(proveedor, List.of(
                new DetalleCompra(prodA, 200, new BigDecimal("750"))));

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(compraRepository.findByProveedorId(1L)).thenReturn(List.of(c1, c2));

        ReporteComprasDto reporte = reporteService.reporteComprasPorProveedor(1L);

        assertThat(reporte.nombreProveedor()).isEqualTo("Distribuidora XYZ");
        assertThat(reporte.totalCompras()).isEqualTo(2);
        // Total: 80.000 + 25.000 + 150.000 = 255.000
        assertThat(reporte.totalInvertido()).isEqualByComparingTo("255000");
        assertThat(reporte.productos()).hasSize(2);

        // Café: 100+200=300 uds, invertido 80.000+150.000=230.000
        ProductoCompradoDto cafe = reporte.productos().stream()
                .filter(p -> p.nombre().equals("Café")).findFirst().orElseThrow();
        assertThat(cafe.cantidadTotal()).isEqualTo(300);
        assertThat(cafe.totalInvertido()).isEqualByComparingTo("230000");

        // El primer producto debe ser Café (mayor inversión)
        assertThat(reporte.productos().get(0).nombre()).isEqualTo("Café");
    }

    @Test
    void reporteComprasPorProveedor_proveedorNoExiste_lanzaBusinessException() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> reporteService.reporteComprasPorProveedor(99L));
    }

    // ----------------------------------------------------------------
    // reporteCreditos
    // ----------------------------------------------------------------

    @Test
    void reporteCreditos_filtraSoloClientesConDeudaYOrdenaDescendente() {
        // Cliente A: debe 150.000 de 500.000
        Cliente conDeuda1 = clienteBuilder(1L, "Ana López",   "111", new BigDecimal("500000"), new BigDecimal("150000"));
        // Cliente B: debe 300.000 de 800.000 — la deuda más alta, debe aparecer primero
        Cliente conDeuda2 = clienteBuilder(2L, "Carlos Ruiz", "222", new BigDecimal("800000"), new BigDecimal("300000"));
        // Cliente C: sin deuda — no debe aparecer en el reporte
        Cliente sinDeuda  = clienteBuilder(3L, "María Torres","333", new BigDecimal("200000"), BigDecimal.ZERO);
        // Cliente D: sin crédito habilitado — no debe aparecer
        Cliente sinCredito = Cliente.builder()
                .id(4L).nombre("Sin Crédito").cedula("444")
                .montoCredito(BigDecimal.ZERO).saldoUtilizado(BigDecimal.ZERO).activo(true).build();

        when(clienteRepository.findAll()).thenReturn(List.of(conDeuda1, conDeuda2, sinDeuda, sinCredito));

        ReporteCreditosDto reporte = reporteService.reporteCreditos();

        assertThat(reporte.clientesConDeuda()).isEqualTo(2);
        // Total deuda: 150.000 + 300.000 = 450.000
        assertThat(reporte.totalDeudaPendiente()).isEqualByComparingTo("450000");
        // Ordenado por deuda descendente: Carlos primero
        assertThat(reporte.clientes().get(0).nombre()).isEqualTo("Carlos Ruiz");
        assertThat(reporte.clientes().get(1).nombre()).isEqualTo("Ana López");
        // Saldo disponible de Ana: 500.000 - 150.000 = 350.000
        assertThat(reporte.clientes().get(1).saldoDisponible()).isEqualByComparingTo("350000");
    }

    @Test
    void reporteCreditos_sinDeudas_retornaReporteVacio() {
        Cliente sinDeuda = clienteBuilder(1L, "Sin Deuda", "111",
                new BigDecimal("500000"), BigDecimal.ZERO);
        when(clienteRepository.findAll()).thenReturn(List.of(sinDeuda));

        ReporteCreditosDto reporte = reporteService.reporteCreditos();

        assertThat(reporte.clientesConDeuda()).isZero();
        assertThat(reporte.totalDeudaPendiente()).isEqualByComparingTo("0");
        assertThat(reporte.clientes()).isEmpty();
    }

    // ----------------------------------------------------------------
    // reporteInventario
    // ----------------------------------------------------------------

    @Test
    void reporteInventario_separaAgotadosYBajoStock() {
        Producto agotado  = stockProductoBuilder(1L, "Café",   "BAR-A", 0);
        Producto critico  = stockProductoBuilder(2L, "Azúcar", "BAR-B", 3);  // bajo umbral=5
        Producto suficiente = stockProductoBuilder(3L, "Arroz", "BAR-C", 20); // ok
        when(productoRepository.findAllActivos()).thenReturn(List.of(agotado, critico, suficiente));

        ReporteInventarioDto reporte = reporteService.reporteInventario(5);

        assertThat(reporte.totalProductosActivos()).isEqualTo(3);
        assertThat(reporte.umbralBajoStock()).isEqualTo(5);
        assertThat(reporte.productosAgotados()).isEqualTo(1);
        assertThat(reporte.productosBajoStock()).isEqualTo(1);
        assertThat(reporte.agotados().get(0).nombre()).isEqualTo("Café");
        assertThat(reporte.bajoStock().get(0).nombre()).isEqualTo("Azúcar");
        assertThat(reporte.bajoStock().get(0).stock()).isEqualTo(3);
    }

    @Test
    void reporteInventario_variosAgotados_ordenadosAlfabeticamente() {
        Producto p1 = stockProductoBuilder(1L, "Sal",   "BAR-S", 0);
        Producto p2 = stockProductoBuilder(2L, "Arroz", "BAR-A", 0);
        when(productoRepository.findAllActivos()).thenReturn(List.of(p1, p2));

        ReporteInventarioDto reporte = reporteService.reporteInventario(5);

        // Ordenados alfabéticamente: Arroz antes que Sal
        assertThat(reporte.agotados().get(0).nombre()).isEqualTo("Arroz");
        assertThat(reporte.agotados().get(1).nombre()).isEqualTo("Sal");
    }

    @Test
    void reporteInventario_sinProblemas_listasVacias() {
        Producto ok = stockProductoBuilder(1L, "Café", "BAR-A", 100);
        when(productoRepository.findAllActivos()).thenReturn(List.of(ok));

        ReporteInventarioDto reporte = reporteService.reporteInventario(5);

        assertThat(reporte.productosAgotados()).isZero();
        assertThat(reporte.productosBajoStock()).isZero();
        assertThat(reporte.agotados()).isEmpty();
        assertThat(reporte.bajoStock()).isEmpty();
    }

    // ----------------------------------------------------------------
    // reporteRentabilidad
    // ----------------------------------------------------------------

    @Test
    void reporteRentabilidad_conGanancia_calculaMargenYGanancia() {
        // Compra del mes: 500.000
        Compra c1 = compraConFecha(new BigDecimal("500000"), LocalDateTime.of(2026, 2, 10, 9, 0));
        // Venta completada del mes: 800.000
        Venta v1 = ventaConFecha(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("800000"), LocalDateTime.of(2026, 2, 15, 14, 0));
        // Venta de otro mes — no debe contar
        Venta v2 = ventaConFecha(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("200000"), LocalDateTime.of(2026, 1, 20, 10, 0));

        when(compraRepository.findAll()).thenReturn(List.of(c1));
        when(ventaRepository.findAll()).thenReturn(List.of(v1, v2));

        ReporteRentabilidadDto reporte = reporteService.reporteRentabilidad(2026, 2);

        assertThat(reporte.totalInvertido()).isEqualByComparingTo("500000");
        assertThat(reporte.totalVendido()).isEqualByComparingTo("800000");
        // Ganancia: 800.000 - 500.000 = 300.000
        assertThat(reporte.gananciaBruta()).isEqualByComparingTo("300000");
        // Margen: (300.000 / 800.000) × 100 = 37.50%
        assertThat(reporte.margenPorcentaje()).isEqualByComparingTo("37.50");
        assertThat(reporte.tuvoPerdida()).isFalse();
    }

    @Test
    void reporteRentabilidad_conPerdida_detectaPerdida() {
        Compra c1 = compraConFecha(new BigDecimal("1000000"), LocalDateTime.of(2026, 2, 5, 9, 0));
        Venta v1 = ventaConFecha(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("600000"), LocalDateTime.of(2026, 2, 20, 14, 0));

        when(compraRepository.findAll()).thenReturn(List.of(c1));
        when(ventaRepository.findAll()).thenReturn(List.of(v1));

        ReporteRentabilidadDto reporte = reporteService.reporteRentabilidad(2026, 2);

        // Pérdida: 600.000 - 1.000.000 = -400.000
        assertThat(reporte.gananciaBruta()).isEqualByComparingTo("-400000");
        assertThat(reporte.tuvoPerdida()).isTrue();
    }

    @Test
    void reporteRentabilidad_ventasAnuladasNoSuman() {
        Compra c1 = compraConFecha(new BigDecimal("200000"), LocalDateTime.of(2026, 2, 1, 9, 0));
        // Venta COMPLETADA: sí suma
        Venta v1 = ventaConFecha(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("300000"), LocalDateTime.of(2026, 2, 10, 12, 0));
        // Venta ANULADA: NO debe sumar aunque esté en el mismo mes
        Venta v2 = ventaConFecha(MetodoPago.EFECTIVO, EstadoVenta.ANULADA,
                new BigDecimal("500000"), LocalDateTime.of(2026, 2, 15, 15, 0));

        when(compraRepository.findAll()).thenReturn(List.of(c1));
        when(ventaRepository.findAll()).thenReturn(List.of(v1, v2));

        ReporteRentabilidadDto reporte = reporteService.reporteRentabilidad(2026, 2);

        // Solo cuenta v1=300.000, v2 ANULADA es ignorada
        assertThat(reporte.totalVendido()).isEqualByComparingTo("300000");
    }

    @Test
    void reporteRentabilidad_sinVentas_margenEsNulo() {
        Compra c1 = compraConFecha(new BigDecimal("500000"), LocalDateTime.of(2026, 2, 1, 9, 0));
        when(compraRepository.findAll()).thenReturn(List.of(c1));
        when(ventaRepository.findAll()).thenReturn(List.of());

        ReporteRentabilidadDto reporte = reporteService.reporteRentabilidad(2026, 2);

        assertThat(reporte.totalVendido()).isEqualByComparingTo("0");
        assertThat(reporte.margenPorcentaje()).isNull(); // no se puede calcular margen sin ventas
        assertThat(reporte.tuvoPerdida()).isTrue();      // invertimos sin vender nada
    }

    // ----------------------------------------------------------------
    // reporteRentabilidadAnual
    // ----------------------------------------------------------------

    @Test
    void reporteRentabilidadAnual_conActividadEnDosesMeses_calculaTotalesYMeses() {
        // Febrero: compra 500.000, venta 800.000  → ganancia 300.000
        Compra cFeb = compraConFecha(new BigDecimal("500000"), LocalDateTime.of(2026, 2, 10, 9, 0));
        Venta  vFeb = ventaConFecha(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("800000"), LocalDateTime.of(2026, 2, 20, 14, 0));

        // Junio: compra 200.000, venta 350.000   → ganancia 150.000
        Compra cJun = compraConFecha(new BigDecimal("200000"), LocalDateTime.of(2026, 6, 5, 9, 0));
        Venta  vJun = ventaConFecha(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("350000"), LocalDateTime.of(2026, 6, 15, 14, 0));

        // Venta de otro año — no debe contar
        Venta vOtro = ventaConFecha(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("999000"), LocalDateTime.of(2025, 6, 1, 10, 0));

        when(compraRepository.findAll()).thenReturn(List.of(cFeb, cJun));
        when(ventaRepository.findAll()).thenReturn(List.of(vFeb, vJun, vOtro));

        ReporteRentabilidadAnualDto reporte = reporteService.reporteRentabilidadAnual(2026);

        // Totales anuales: invertido=700.000, vendido=1.150.000, ganancia=450.000
        assertThat(reporte.anio()).isEqualTo(2026);
        assertThat(reporte.totalInvertido()).isEqualByComparingTo("700000");
        assertThat(reporte.totalVendido()).isEqualByComparingTo("1150000");
        assertThat(reporte.gananciaBruta()).isEqualByComparingTo("450000");
        assertThat(reporte.tuvoPerdida()).isFalse();
        // Margen: 450.000 / 1.150.000 × 100 = 39.13%
        assertThat(reporte.margenPorcentaje()).isEqualByComparingTo("39.13");

        // Siempre 12 meses
        assertThat(reporte.meses()).hasSize(12);

        // Verificar desglose de febrero (mes 2)
        RentabilidadMensualDto feb = reporte.meses().get(1); // índice 1 = febrero
        assertThat(feb.mes()).isEqualTo(2);
        assertThat(feb.nombreMes()).isEqualTo("Febrero");
        assertThat(feb.totalInvertido()).isEqualByComparingTo("500000");
        assertThat(feb.totalVendido()).isEqualByComparingTo("800000");
        assertThat(feb.gananciaBruta()).isEqualByComparingTo("300000");
        assertThat(feb.tuvoPerdida()).isFalse();

        // Enero (mes 1) sin actividad → todo en cero
        RentabilidadMensualDto ene = reporte.meses().get(0);
        assertThat(ene.nombreMes()).isEqualTo("Enero");
        assertThat(ene.totalInvertido()).isEqualByComparingTo("0");
        assertThat(ene.totalVendido()).isEqualByComparingTo("0");
        assertThat(ene.tuvoPerdida()).isFalse();
    }

    @Test
    void reporteRentabilidadAnual_mesConPerdida_setuvoPerdidaEnEseMes() {
        // Marzo: invierte mucho, vende poco → pérdida
        Compra cMar = compraConFecha(new BigDecimal("1000000"), LocalDateTime.of(2026, 3, 1, 9, 0));
        Venta  vMar = ventaConFecha(MetodoPago.EFECTIVO, EstadoVenta.COMPLETADA,
                new BigDecimal("400000"), LocalDateTime.of(2026, 3, 20, 14, 0));

        when(compraRepository.findAll()).thenReturn(List.of(cMar));
        when(ventaRepository.findAll()).thenReturn(List.of(vMar));

        ReporteRentabilidadAnualDto reporte = reporteService.reporteRentabilidadAnual(2026);

        RentabilidadMensualDto mar = reporte.meses().get(2); // índice 2 = marzo
        assertThat(mar.nombreMes()).isEqualTo("Marzo");
        assertThat(mar.gananciaBruta()).isEqualByComparingTo("-600000");
        assertThat(mar.tuvoPerdida()).isTrue();

        // El año en global también tuvo pérdida
        assertThat(reporte.tuvoPerdida()).isTrue();
        assertThat(reporte.gananciaBruta()).isEqualByComparingTo("-600000");
    }

    @Test
    void reporteRentabilidadAnual_sinActividad_retorna12MesesEnCeroYMargenNulo() {
        when(compraRepository.findAll()).thenReturn(List.of());
        when(ventaRepository.findAll()).thenReturn(List.of());

        ReporteRentabilidadAnualDto reporte = reporteService.reporteRentabilidadAnual(2026);

        assertThat(reporte.meses()).hasSize(12);
        assertThat(reporte.totalInvertido()).isEqualByComparingTo("0");
        assertThat(reporte.totalVendido()).isEqualByComparingTo("0");
        assertThat(reporte.gananciaBruta()).isEqualByComparingTo("0");
        assertThat(reporte.margenPorcentaje()).isNull();
        assertThat(reporte.tuvoPerdida()).isFalse();

        // Diciembre (mes 12) = índice 11
        RentabilidadMensualDto dic = reporte.meses().get(11);
        assertThat(dic.nombreMes()).isEqualTo("Diciembre");
        assertThat(dic.totalVendido()).isEqualByComparingTo("0");
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private Caja cajaBuilder() {
        return Caja.builder()
                .id(1L).estado(EstadoCaja.ABIERTA)
                .fechaApertura(LocalDateTime.now().minusHours(4))
                .montoInicial(new BigDecimal("200000")).build();
    }

    private Producto productoBuilder(Long id, String nombre, String codigo) {
        return Producto.builder()
                .id(id).nombre(nombre).codigoBarras(codigo)
                .precioVenta(new BigDecimal("2000")).stock(100).activo(true).build();
    }

    private Venta ventaBuilder(MetodoPago metodo, EstadoVenta estado,
                               BigDecimal total, List<DetalleVenta> detalles) {
        return Venta.builder()
                .metodoPago(metodo).estado(estado).total(total)
                .detalles(detalles).build();
    }

    private Compra compraBuilder(Proveedor proveedor, List<DetalleCompra> detalles) {
        BigDecimal total = detalles.stream()
                .map(DetalleCompra::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Compra.builder()
                .proveedor(proveedor).total(total).detalles(detalles).build();
    }

    private Cliente clienteBuilder(Long id, String nombre, String cedula,
                                   BigDecimal montoCredito, BigDecimal saldoUtilizado) {
        return Cliente.builder()
                .id(id).nombre(nombre).cedula(cedula)
                .montoCredito(montoCredito).saldoUtilizado(saldoUtilizado)
                .activo(true).build();
    }

    private Producto stockProductoBuilder(Long id, String nombre, String codigo, int stock) {
        Proveedor proveedor = Proveedor.builder()
                .id(1L).nombre("Proveedor Test")
                .porcentajeGanancia(new BigDecimal("30")).activo(true).build();
        return Producto.builder()
                .id(id).nombre(nombre).codigoBarras(codigo)
                .precioCompra(new BigDecimal("1000")).precioVenta(new BigDecimal("1300"))
                .proveedorPrincipal(proveedor).stock(stock).activo(true).build();
    }

    private Compra compraConFecha(BigDecimal total, LocalDateTime fecha) {
        return Compra.builder()
                .total(total).fecha(fecha)
                .detalles(List.of()).build();
    }

    private Venta ventaConFecha(MetodoPago metodo, EstadoVenta estado,
                                BigDecimal total, LocalDateTime fecha) {
        return Venta.builder()
                .metodoPago(metodo).estado(estado)
                .total(total).fecha(fecha)
                .detalles(List.of()).build();
    }
}
