package com.nap.pos.infrastructure.persistence.repository.impl;

import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.repository.CategoriaRepository;
import com.nap.pos.infrastructure.persistence.mapper.CategoriaMapper;
import com.nap.pos.infrastructure.persistence.repository.jpa.JpaCategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoriaRepositoryImpl implements CategoriaRepository {

    private final JpaCategoriaRepository jpaCategoriaRepository;
    private final CategoriaMapper        categoriaMapper;

    @Override
    public Categoria save(Categoria categoria) {
        return categoriaMapper.toDomain(
                jpaCategoriaRepository.save(categoriaMapper.toEntity(categoria)));
    }

    @Override
    public Optional<Categoria> findById(Long id) {
        return jpaCategoriaRepository.findById(id).map(categoriaMapper::toDomain);
    }

    @Override
    public Optional<Categoria> findByNombre(String nombre) {
        return jpaCategoriaRepository.findByNombre(nombre).map(categoriaMapper::toDomain);
    }

    @Override
    public List<Categoria> findAll() {
        return jpaCategoriaRepository.findAll().stream()
                .map(categoriaMapper::toDomain).toList();
    }

    @Override
    public List<Categoria> findAllActivas() {
        return jpaCategoriaRepository.findByActivoTrue().stream()
                .map(categoriaMapper::toDomain).toList();
    }
}
