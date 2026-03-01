package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Subcategoria;
import com.nap.pos.domain.repository.SubcategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubcategoriaService {

    private final SubcategoriaRepository subcategoriaRepository;

    @Transactional
    public Subcategoria crear(Subcategoria subcategoria) {
        subcategoriaRepository.findByNombre(subcategoria.getNombre())
                .ifPresent(s -> {
                    throw new BusinessException(
                            "Ya existe una subcategoría con el nombre '" + subcategoria.getNombre() + "'.");
                });
        return subcategoriaRepository.save(subcategoria);
    }

    @Transactional
    public Subcategoria actualizar(Subcategoria subcategoria) {
        findById(subcategoria.getId());
        subcategoriaRepository.findByNombre(subcategoria.getNombre())
                .filter(s -> !s.getId().equals(subcategoria.getId()))
                .ifPresent(s -> {
                    throw new BusinessException(
                            "Ya existe una subcategoría con el nombre '" + subcategoria.getNombre() + "'.");
                });
        return subcategoriaRepository.save(subcategoria);
    }

    @Transactional
    public void activar(Long id) {
        Subcategoria sub = findById(id);
        sub.activar();
        subcategoriaRepository.save(sub);
    }

    @Transactional
    public void desactivar(Long id) {
        Subcategoria sub = findById(id);
        sub.desactivar();
        subcategoriaRepository.save(sub);
    }

    public Subcategoria findById(Long id) {
        return subcategoriaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Subcategoría con ID " + id + " no encontrada."));
    }

    public List<Subcategoria> findAll() {
        return subcategoriaRepository.findAll();
    }

    public List<Subcategoria> findAllActivas() {
        return subcategoriaRepository.findAllActivas();
    }

    public List<Subcategoria> findByCategoriaId(Long categoriaId) {
        return subcategoriaRepository.findByCategoriaId(categoriaId);
    }
}
