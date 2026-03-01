package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    // --- crear ---

    @Test
    void crear_nombreUnico_guarda() {
        Categoria nueva = categoria(null, "Bebidas");
        when(categoriaRepository.findByNombre("Bebidas")).thenReturn(Optional.empty());
        when(categoriaRepository.save(nueva)).thenReturn(nueva);

        Categoria resultado = categoriaService.crear(nueva);

        assertThat(resultado.getNombre()).isEqualTo("Bebidas");
        verify(categoriaRepository).save(nueva);
    }

    @Test
    void crear_nombreDuplicado_lanzaBusinessException() {
        Categoria existente = categoria(1L, "Bebidas");
        Categoria nueva = categoria(null, "Bebidas");
        when(categoriaRepository.findByNombre("Bebidas")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> categoriaService.crear(nueva));
        verify(categoriaRepository, never()).save(any());
    }

    // --- actualizar ---

    @Test
    void actualizar_nombreDuplicadoEnOtraCategoria_lanzaBusinessException() {
        Categoria existente = categoria(2L, "Bebidas");
        Categoria aActualizar = categoria(1L, "Bebidas");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(aActualizar));
        when(categoriaRepository.findByNombre("Bebidas")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> categoriaService.actualizar(aActualizar));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void actualizar_mismoNombreMismaCategoria_guarda() {
        Categoria cat = categoria(1L, "Bebidas");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoriaRepository.findByNombre("Bebidas")).thenReturn(Optional.of(cat));
        when(categoriaRepository.save(cat)).thenReturn(cat);

        Categoria resultado = categoriaService.actualizar(cat);

        assertThat(resultado.getNombre()).isEqualTo("Bebidas");
        verify(categoriaRepository).save(cat);
    }

    // --- activar / desactivar ---

    @Test
    void activar_categoriaInactiva_quedaActiva() {
        Categoria cat = Categoria.builder().id(1L).nombre("Bebidas").activo(false).build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        categoriaService.activar(1L);

        verify(categoriaRepository).save(argThat(Categoria::isActivo));
    }

    @Test
    void desactivar_categoriaActiva_quedaInactiva() {
        Categoria cat = Categoria.builder().id(1L).nombre("Bebidas").activo(true).build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        categoriaService.desactivar(1L);

        verify(categoriaRepository).save(argThat(c -> !c.isActivo()));
    }

    // --- findById ---

    @Test
    void findById_noExiste_lanzaBusinessException() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> categoriaService.findById(99L));
    }

    // --- findAll / findAllActivas ---

    @Test
    void findAll_retornaTodasLasCategorias() {
        List<Categoria> lista = List.of(categoria(1L, "Bebidas"), categoria(2L, "Snacks"));
        when(categoriaRepository.findAll()).thenReturn(lista);

        List<Categoria> resultado = categoriaService.findAll();

        assertThat(resultado).hasSize(2);
    }

    @Test
    void findAllActivas_retornaSoloActivas() {
        List<Categoria> activas = List.of(categoria(1L, "Bebidas"));
        when(categoriaRepository.findAllActivas()).thenReturn(activas);

        List<Categoria> resultado = categoriaService.findAllActivas();

        assertThat(resultado).hasSize(1);
        verify(categoriaRepository).findAllActivas();
    }

    // --- helper ---

    private Categoria categoria(Long id, String nombre) {
        return Categoria.builder().id(id).nombre(nombre).activo(true).build();
    }
}
