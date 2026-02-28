package com.mektos.pos.application.service;

import com.mektos.pos.domain.exception.BusinessException;
import com.mektos.pos.domain.model.Producto;
import com.mektos.pos.domain.model.Proveedor;
import com.mektos.pos.domain.repository.ProductoRepository;
import com.mektos.pos.domain.repository.ProveedorRepository;
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
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProveedorService proveedorService;

    // --- crear ---

    @Test
    void crear_proveedorSinDuplicados_guarda() {
        Proveedor nuevo = proveedorBuilder(null, "NIT-001", "Proveedor A", new BigDecimal("25")).build();
        when(proveedorRepository.findByNit("NIT-001")).thenReturn(Optional.empty());
        when(proveedorRepository.findByNombre("Proveedor A")).thenReturn(Optional.empty());
        when(proveedorRepository.save(nuevo)).thenReturn(nuevo);

        Proveedor resultado = proveedorService.crear(nuevo);

        assertThat(resultado.getNombre()).isEqualTo("Proveedor A");
        verify(proveedorRepository).save(nuevo);
    }

    @Test
    void crear_nitDuplicado_lanzaBusinessException() {
        Proveedor existente = proveedorBuilder(1L, "NIT-001", "Otro", new BigDecimal("20")).build();
        Proveedor nuevo = proveedorBuilder(null, "NIT-001", "Proveedor A", new BigDecimal("25")).build();
        when(proveedorRepository.findByNit("NIT-001")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> proveedorService.crear(nuevo));
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void crear_nombreDuplicado_lanzaBusinessException() {
        Proveedor existente = proveedorBuilder(1L, null, "Proveedor A", new BigDecimal("20")).build();
        // Nuevo sin NIT para que pase la validación de NIT y falle en nombre
        Proveedor nuevo = Proveedor.builder()
                .nombre("Proveedor A").porcentajeGanancia(new BigDecimal("25")).activo(true).build();
        when(proveedorRepository.findByNombre("Proveedor A")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> proveedorService.crear(nuevo));
        verify(proveedorRepository, never()).save(any());
    }

    // --- actualizarPorcentajeGanancia ---

    @Test
    void actualizarPorcentajeGanancia_recalculaPreciosDeProductosAsociados() {
        // Proveedor con 20% inicial
        Proveedor proveedor = proveedorBuilder(1L, "NIT-001", "Proveedor A", new BigDecimal("20")).build();

        // Producto: precioCompra=1000, con el 20% el precio sería 1200; con 30% será 1300
        Producto producto = Producto.builder()
                .id(10L).nombre("Prod A").codigoBarras("AAA")
                .precioCompra(new BigDecimal("1000"))
                .proveedorPrincipal(proveedor)
                .stock(5).activo(true).build();

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(proveedorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.findByProveedorPrincipalId(1L)).thenReturn(List.of(producto));
        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        proveedorService.actualizarPorcentajeGanancia(1L, new BigDecimal("30"));

        // Proveedor guardado con el nuevo porcentaje
        verify(proveedorRepository).save(argThat(p ->
                p.getPorcentajeGanancia().compareTo(new BigDecimal("30")) == 0
        ));

        // Producto guardado con precio recalculado (1000 × 1.30 = 1300.00)
        verify(productoRepository).save(argThat(p ->
                p.getPrecioVenta().compareTo(new BigDecimal("1300.00")) == 0
        ));
    }

    @Test
    void actualizarPorcentajeGanancia_porcentajeCeroONegativo_lanzaBusinessException() {
        Proveedor proveedor = proveedorBuilder(1L, "NIT-001", "Proveedor A", new BigDecimal("20")).build();
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));

        assertThrows(BusinessException.class,
                () -> proveedorService.actualizarPorcentajeGanancia(1L, BigDecimal.ZERO));
    }

    // --- helper ---

    private Proveedor.ProveedorBuilder proveedorBuilder(Long id, String nit, String nombre, BigDecimal porcentaje) {
        return Proveedor.builder()
                .id(id).nit(nit).nombre(nombre).celular("3001234567")
                .porcentajeGanancia(porcentaje).activo(true);
    }
}
