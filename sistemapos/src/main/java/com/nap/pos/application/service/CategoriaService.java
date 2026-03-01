package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional
    public Categoria crear(Categoria categoria) {
        categoriaRepository.findByNombre(categoria.getNombre())
                .ifPresent(c -> {
                    throw new BusinessException(
                            "Ya existe una categoría con el nombre '" + categoria.getNombre() + "'.");
                });
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria actualizar(Categoria categoria) {
        findById(categoria.getId());
        categoriaRepository.findByNombre(categoria.getNombre())
                .filter(c -> !c.getId().equals(categoria.getId()))
                .ifPresent(c -> {
                    throw new BusinessException(
                            "Ya existe una categoría con el nombre '" + categoria.getNombre() + "'.");
                });
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void activar(Long id) {
        Categoria categoria = findById(id);
        categoria.activar();
        categoriaRepository.save(categoria);
    }

    @Transactional
    public void desactivar(Long id) {
        Categoria categoria = findById(id);
        categoria.desactivar();
        categoriaRepository.save(categoria);
    }

    public Categoria findById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Categoría con ID " + id + " no encontrada."));
    }

    public List<Categoria> findAll() {
        return categoriaRepository.findAll();
    }

    public List<Categoria> findAllActivas() {
        return categoriaRepository.findAllActivas();
    }
}
