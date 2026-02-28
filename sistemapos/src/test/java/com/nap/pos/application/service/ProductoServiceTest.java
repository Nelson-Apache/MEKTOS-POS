package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.Proveedor;
import com.nap.pos.domain.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    // --- crear ---

    @Test
    void crear_codigoBarrasNuevo_guarda() {
        Producto nuevo = productoBuilder(1L, "111", true, 10).build();
        when(productoRepository.findByCodigoBarras("111")).thenReturn(Optional.empty());
        when(productoRepository.save(nuevo)).thenReturn(nuevo);

        Producto resultado = productoService.crear(nuevo);

        assertThat(resultado.getCodigoBarras()).isEqualTo("111");
        verify(productoRepository).save(nuevo);
    }

    @Test
    void crear_codigoBarrasDuplicado_lanzaBusinessException() {
        Producto existente = productoBuilder(1L, "111", true, 10).build();
        Producto nuevo = productoBuilder(null, "111", true, 0).build();
        when(productoRepository.findByCodigoBarras("111")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> productoService.crear(nuevo));
        verify(productoRepository, never()).save(any());
    }

    // --- activar / desactivar ---

    @Test
    void activar_productoInactivo_cambiaActivoATrue() {
        Producto inactivo = productoBuilder(1L, "111", false, 0).build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(inactivo));
        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        productoService.activar(1L);

        // El método activar() muta el objeto y lo guarda — verificamos que save fue llamado
        // con un producto activo
        verify(productoRepository).save(argThat(p -> p.isActivo()));
    }

    @Test
    void desactivar_productoActivo_cambiaActivoAFalse() {
        Producto activo = productoBuilder(1L, "111", true, 5).build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(activo));
        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        productoService.desactivar(1L);

        verify(productoRepository).save(argThat(p -> !p.isActivo()));
    }

    // --- findById ---

    @Test
    void findById_productoNoExiste_lanzaBusinessException() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> productoService.findById(99L));
    }

    // --- helper ---

    private Producto.ProductoBuilder productoBuilder(Long id, String codigo, boolean activo, int stock) {
        Proveedor proveedor = Proveedor.builder()
                .id(1L).nombre("Proveedor A")
                .porcentajeGanancia(new BigDecimal("30")).activo(true).build();
        return Producto.builder()
                .id(id).codigoBarras(codigo).nombre("Producto " + codigo)
                .precioCompra(new BigDecimal("1000"))
                .proveedorPrincipal(proveedor)
                .stock(stock).activo(activo);
    }
}
