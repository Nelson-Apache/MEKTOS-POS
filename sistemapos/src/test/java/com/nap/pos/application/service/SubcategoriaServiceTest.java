package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.model.Subcategoria;
import com.nap.pos.domain.repository.SubcategoriaRepository;
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
class SubcategoriaServiceTest {

    @Mock
    private SubcategoriaRepository subcategoriaRepository;

    @InjectMocks
    private SubcategoriaService subcategoriaService;

    // --- crear ---

    @Test
    void crear_nombreUnico_guarda() {
        Subcategoria nueva = subcategoria(null, "Gaseosas");
        when(subcategoriaRepository.findByNombre("Gaseosas")).thenReturn(Optional.empty());
        when(subcategoriaRepository.save(nueva)).thenReturn(nueva);

        Subcategoria resultado = subcategoriaService.crear(nueva);

        assertThat(resultado.getNombre()).isEqualTo("Gaseosas");
        verify(subcategoriaRepository).save(nueva);
    }

    @Test
    void crear_nombreDuplicado_lanzaBusinessException() {
        Subcategoria existente = subcategoria(1L, "Gaseosas");
        Subcategoria nueva = subcategoria(null, "Gaseosas");
        when(subcategoriaRepository.findByNombre("Gaseosas")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> subcategoriaService.crear(nueva));
        verify(subcategoriaRepository, never()).save(any());
    }

    // --- actualizar ---

    @Test
    void actualizar_nombreDuplicadoEnOtraSubcategoria_lanzaBusinessException() {
        Subcategoria existente = subcategoria(2L, "Gaseosas");
        Subcategoria aActualizar = subcategoria(1L, "Gaseosas");
        when(subcategoriaRepository.findById(1L)).thenReturn(Optional.of(aActualizar));
        when(subcategoriaRepository.findByNombre("Gaseosas")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> subcategoriaService.actualizar(aActualizar));
        verify(subcategoriaRepository, never()).save(any());
    }

    @Test
    void actualizar_mismoNombreMismaSubcategoria_guarda() {
        Subcategoria sub = subcategoria(1L, "Gaseosas");
        when(subcategoriaRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subcategoriaRepository.findByNombre("Gaseosas")).thenReturn(Optional.of(sub));
        when(subcategoriaRepository.save(sub)).thenReturn(sub);

        Subcategoria resultado = subcategoriaService.actualizar(sub);

        assertThat(resultado.getNombre()).isEqualTo("Gaseosas");
        verify(subcategoriaRepository).save(sub);
    }

    // --- activar / desactivar ---

    @Test
    void activar_subcategoriaInactiva_quedaActiva() {
        Subcategoria sub = Subcategoria.builder().id(1L).nombre("Gaseosas").activo(false).build();
        when(subcategoriaRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subcategoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subcategoriaService.activar(1L);

        verify(subcategoriaRepository).save(argThat(Subcategoria::isActivo));
    }

    @Test
    void desactivar_subcategoriaActiva_quedaInactiva() {
        Subcategoria sub = Subcategoria.builder().id(1L).nombre("Gaseosas").activo(true).build();
        when(subcategoriaRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subcategoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subcategoriaService.desactivar(1L);

        verify(subcategoriaRepository).save(argThat(s -> !s.isActivo()));
    }

    // --- findById ---

    @Test
    void findById_noExiste_lanzaBusinessException() {
        when(subcategoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> subcategoriaService.findById(99L));
    }

    // --- findAll / findAllActivas / findByCategoriaId ---

    @Test
    void findAll_retornaTodasLasSubcategorias() {
        List<Subcategoria> lista = List.of(subcategoria(1L, "Gaseosas"), subcategoria(2L, "Jugos"));
        when(subcategoriaRepository.findAll()).thenReturn(lista);

        List<Subcategoria> resultado = subcategoriaService.findAll();

        assertThat(resultado).hasSize(2);
    }

    @Test
    void findAllActivas_retornaSoloActivas() {
        List<Subcategoria> activas = List.of(subcategoria(1L, "Gaseosas"));
        when(subcategoriaRepository.findAllActivas()).thenReturn(activas);

        List<Subcategoria> resultado = subcategoriaService.findAllActivas();

        assertThat(resultado).hasSize(1);
        verify(subcategoriaRepository).findAllActivas();
    }

    @Test
    void findByCategoriaId_retornaSubcategoriasDeLaCategoria() {
        List<Subcategoria> subs = List.of(subcategoria(1L, "Gaseosas"), subcategoria(2L, "Jugos"));
        when(subcategoriaRepository.findByCategoriaId(10L)).thenReturn(subs);

        List<Subcategoria> resultado = subcategoriaService.findByCategoriaId(10L);

        assertThat(resultado).hasSize(2);
        verify(subcategoriaRepository).findByCategoriaId(10L);
    }

    // --- helper ---

    private Subcategoria subcategoria(Long id, String nombre) {
        Categoria cat = Categoria.builder().id(10L).nombre("Bebidas").activo(true).build();
        return Subcategoria.builder().id(id).nombre(nombre).categoria(cat).activo(true).build();
    }
}
