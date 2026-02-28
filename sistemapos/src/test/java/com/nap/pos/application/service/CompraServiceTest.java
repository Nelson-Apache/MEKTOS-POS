package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Compra;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.Proveedor;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.Rol;
import com.nap.pos.domain.repository.CompraRepository;
import com.nap.pos.domain.repository.ProductoRepository;
import com.nap.pos.domain.repository.ProveedorRepository;
import com.nap.pos.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock private CompraRepository compraRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CompraService compraService;

    // --- registrarCompra ---

    @Test
    void registrarCompra_itemsValidos_incrementaStockYActualizaCosto() {
        Proveedor proveedor = proveedorBuilder(1L, new BigDecimal("30"));
        Usuario usuario = usuarioBuilder(1L);
        // Producto con stock=5 y costo actual=1000
        Producto producto = productoBuilder(1L, 5, new BigDecimal("1000"), proveedor);

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(compraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<CompraService.ItemCompra> items = List.of(
                new CompraService.ItemCompra(1L, 10, new BigDecimal("1200"))
        );
        Compra resultado = compraService.registrarCompra(1L, 1L, "FAC-001", items);

        // Total: 10 × 1200 = 12.000
        assertThat(resultado.getTotal()).isEqualByComparingTo("12000");

        // Stock incrementado: 5 + 10 = 15
        // Costo actualizado a 1200, precio recalculado: 1200 × 1.30 = 1560.00
        verify(productoRepository).save(argThat(p ->
                p.getStock() == 15 &&
                p.getPrecioVenta().compareTo(new BigDecimal("1560.00")) == 0
        ));

        verify(compraRepository).save(any(Compra.class));
    }

    @Test
    void registrarCompra_proveedorNoEncontrado_lanzaBusinessException() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        List<CompraService.ItemCompra> items = List.of(
                new CompraService.ItemCompra(1L, 5, new BigDecimal("1000"))
        );

        assertThrows(BusinessException.class,
                () -> compraService.registrarCompra(99L, 1L, "FAC-001", items));
        verify(compraRepository, never()).save(any());
    }

    @Test
    void registrarCompra_productoNoEncontrado_lanzaBusinessException() {
        Proveedor proveedor = proveedorBuilder(1L, new BigDecimal("30"));
        Usuario usuario = usuarioBuilder(1L);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        List<CompraService.ItemCompra> items = List.of(
                new CompraService.ItemCompra(99L, 5, new BigDecimal("1000"))
        );

        assertThrows(BusinessException.class,
                () -> compraService.registrarCompra(1L, 1L, "FAC-001", items));
        verify(compraRepository, never()).save(any());
    }

    @Test
    void registrarCompra_precioCompraInvalido_lanzaBusinessException() {
        Proveedor proveedor = proveedorBuilder(1L, new BigDecimal("30"));
        Usuario usuario = usuarioBuilder(1L);
        Producto producto = productoBuilder(1L, 5, new BigDecimal("1000"), proveedor);

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        List<CompraService.ItemCompra> items = List.of(
                new CompraService.ItemCompra(1L, 5, BigDecimal.ZERO) // precio inválido
        );

        assertThrows(BusinessException.class,
                () -> compraService.registrarCompra(1L, 1L, "FAC-001", items));
    }

    // --- helpers ---

    private Proveedor proveedorBuilder(Long id, BigDecimal porcentaje) {
        return Proveedor.builder()
                .id(id).nombre("Proveedor Test").nit("NIT-001")
                .porcentajeGanancia(porcentaje).activo(true).build();
    }

    private Usuario usuarioBuilder(Long id) {
        return Usuario.builder()
                .id(id).username("admin").passwordHash("hash")
                .rol(Rol.ADMIN).activo(true).build();
    }

    private Producto productoBuilder(Long id, int stock, BigDecimal precioCompra, Proveedor proveedor) {
        return Producto.builder()
                .id(id).codigoBarras("COD-" + id).nombre("Prod " + id)
                .precioCompra(precioCompra)
                .proveedorPrincipal(proveedor)
                .stock(stock).activo(true).build();
    }
}
