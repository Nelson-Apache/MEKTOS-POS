package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.*;
import com.nap.pos.domain.model.enums.*;
import com.nap.pos.domain.repository.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CajaRepository cajaRepository;

    @InjectMocks
    private VentaService ventaService;

    // --- registrarVenta ---

    @Test
    void registrarVenta_efectivo_descuentaStockYGuardaVenta() {
        // La caja debe estar ABIERTA para que venta.validar() pase
        Caja caja = cajaBuilder(1L, EstadoCaja.ABIERTA);
        Usuario usuario = usuarioBuilder(1L);
        // Producto con stock=10, precio=2000
        Producto producto = productoBuilder(1L, 10, new BigDecimal("2000"));

        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<VentaService.ItemVenta> items = List.of(new VentaService.ItemVenta(1L, 3));
        Venta resultado = ventaService.registrarVenta(1L, 1L, null, MetodoPago.EFECTIVO, items);

        // Total = 2000 × 3 = 6000
        assertThat(resultado.getTotal()).isEqualByComparingTo("6000");

        // Stock descontado: 10 - 3 = 7
        verify(productoRepository).save(argThat(p -> p.getStock() == 7));

        // Venta guardada — nunca se tocó el repositorio de clientes
        verify(ventaRepository).save(any(Venta.class));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void registrarVenta_credito_cargaSaldoAlClienteYDescuentaStock() {
        Caja caja = cajaBuilder(1L, EstadoCaja.ABIERTA);
        Usuario usuario = usuarioBuilder(1L);
        Producto producto = productoBuilder(1L, 5, new BigDecimal("1000"));
        // Cliente con crédito de 50.000, sin deuda previa
        Cliente cliente = clienteBuilder(1L, new BigDecimal("50000"), BigDecimal.ZERO);

        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<VentaService.ItemVenta> items = List.of(new VentaService.ItemVenta(1L, 2));
        ventaService.registrarVenta(1L, 1L, 2L, MetodoPago.CREDITO, items);

        // Crédito utilizado: 2 × 1000 = 2000
        verify(clienteRepository).save(argThat(c ->
                c.getSaldoUtilizado().compareTo(new BigDecimal("2000")) == 0
        ));
        // Stock descontado: 5 - 2 = 3
        verify(productoRepository).save(argThat(p -> p.getStock() == 3));
    }

    @Test
    void registrarVenta_productoNoEncontrado_lanzaBusinessException() {
        Caja caja = cajaBuilder(1L, EstadoCaja.ABIERTA);
        Usuario usuario = usuarioBuilder(1L);

        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        List<VentaService.ItemVenta> items = List.of(new VentaService.ItemVenta(99L, 1));

        assertThrows(BusinessException.class,
                () -> ventaService.registrarVenta(1L, 1L, null, MetodoPago.EFECTIVO, items));
        verify(ventaRepository, never()).save(any());
    }

    // --- anularVenta ---

    @Test
    void anularVenta_completada_restauraStockYMarcaAnulada() {
        Producto producto = productoBuilder(1L, 8, new BigDecimal("2000"));
        DetalleVenta detalle = new DetalleVenta(producto, 2, new BigDecimal("2000"));
        Caja caja = cajaBuilder(1L, EstadoCaja.ABIERTA);
        Venta venta = Venta.builder()
                .id(1L).estado(EstadoVenta.COMPLETADA).metodoPago(MetodoPago.EFECTIVO)
                .total(new BigDecimal("4000")).caja(caja)
                .detalles(List.of(detalle)).build();

        // Al restaurar el stock, el servicio recarga el producto desde el repositorio
        Producto productoParaRestaurar = productoBuilder(1L, 8, new BigDecimal("2000"));

        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoParaRestaurar));
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Venta resultado = ventaService.anularVenta(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoVenta.ANULADA);

        // Stock restaurado: 8 + 2 = 10
        verify(productoRepository).save(argThat(p -> p.getStock() == 10));
        verify(clienteRepository, never()).save(any()); // no era a crédito
    }

    @Test
    void anularVenta_credito_restauraCreditoYStock() {
        Producto producto = productoBuilder(1L, 3, new BigDecimal("1000"));
        DetalleVenta detalle = new DetalleVenta(producto, 2, new BigDecimal("1000"));
        Caja caja = cajaBuilder(1L, EstadoCaja.ABIERTA);
        Cliente clienteEnVenta = clienteBuilder(10L, new BigDecimal("50000"), new BigDecimal("2000"));
        Venta venta = Venta.builder()
                .id(1L).estado(EstadoVenta.COMPLETADA).metodoPago(MetodoPago.CREDITO)
                .total(new BigDecimal("2000")).caja(caja).cliente(clienteEnVenta)
                .detalles(List.of(detalle)).build();

        Cliente clienteActual = clienteBuilder(10L, new BigDecimal("50000"), new BigDecimal("2000"));
        Producto productoParaRestaurar = productoBuilder(1L, 3, new BigDecimal("1000"));

        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteActual));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoParaRestaurar));
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ventaService.anularVenta(1L);

        // Crédito restaurado: saldoUtilizado pasa de 2000 a 0
        verify(clienteRepository).save(argThat(c ->
                c.getSaldoUtilizado().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    void anularVenta_yaAnulada_lanzaBusinessException() {
        Venta ventaAnulada = Venta.builder()
                .id(1L).estado(EstadoVenta.ANULADA).metodoPago(MetodoPago.EFECTIVO)
                .total(new BigDecimal("1000")).build();
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(ventaAnulada));

        assertThrows(BusinessException.class, () -> ventaService.anularVenta(1L));
        verify(ventaRepository, never()).save(any());
    }

    // --- helpers ---

    private Caja cajaBuilder(Long id, EstadoCaja estado) {
        return Caja.builder()
                .id(id).fechaApertura(LocalDateTime.now().minusHours(1))
                .montoInicial(new BigDecimal("200000")).estado(estado).build();
    }

    private Usuario usuarioBuilder(Long id) {
        return Usuario.builder()
                .id(id).username("cajero").passwordHash("hash")
                .rol(Rol.CAJERO).activo(true).build();
    }

    private Producto productoBuilder(Long id, int stock, BigDecimal precio) {
        Proveedor proveedor = Proveedor.builder()
                .id(1L).nombre("Prov").porcentajeGanancia(new BigDecimal("30")).activo(true).build();
        return Producto.builder()
                .id(id).codigoBarras("COD-" + id).nombre("Prod " + id)
                .precioCompra(new BigDecimal("1000")).precioVenta(precio)
                .proveedorPrincipal(proveedor).stock(stock).activo(true).build();
    }

    private Cliente clienteBuilder(Long id, BigDecimal montoCredito, BigDecimal saldoUtilizado) {
        return Cliente.builder()
                .id(id).nombre("Cliente Test").cedula("12345")
                .montoCredito(montoCredito).saldoUtilizado(saldoUtilizado)
                .activo(true).build();
    }
}
