package com.nap.pos.infrastructure.persistence.repository.impl;

import com.nap.pos.domain.model.Subcategoria;
import com.nap.pos.domain.repository.SubcategoriaRepository;
import com.nap.pos.infrastructure.persistence.mapper.SubcategoriaMapper;
import com.nap.pos.infrastructure.persistence.repository.jpa.JpaSubcategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SubcategoriaRepositoryImpl implements SubcategoriaRepository {

    private final JpaSubcategoriaRepository jpaSubcategoriaRepository;
    private final SubcategoriaMapper        subcategoriaMapper;

    @Override
    public Subcategoria save(Subcategoria subcategoria) {
        return subcategoriaMapper.toDomain(
                jpaSubcategoriaRepository.save(subcategoriaMapper.toEntity(subcategoria)));
    }

    @Override
    public Optional<Subcategoria> findById(Long id) {
        return jpaSubcategoriaRepository.findById(id).map(subcategoriaMapper::toDomain);
    }

    @Override
    public Optional<Subcategoria> findByNombre(String nombre) {
        return jpaSubcategoriaRepository.findByNombre(nombre).map(subcategoriaMapper::toDomain);
    }

    @Override
    public Optional<Subcategoria> findByNombreAndCategoriaNombre(String nombre, String categoriaNombre) {
        return jpaSubcategoriaRepository.findByNombreAndCategoriaNombre(nombre, categoriaNombre)
                .map(subcategoriaMapper::toDomain);
    }

    @Override
    public List<Subcategoria> findAll() {
        return jpaSubcategoriaRepository.findAll().stream()
                .map(subcategoriaMapper::toDomain).toList();
    }

    @Override
    public List<Subcategoria> findAllActivas() {
        return jpaSubcategoriaRepository.findByActivoTrue().stream()
                .map(subcategoriaMapper::toDomain).toList();
    }

    @Override
    public List<Subcategoria> findByCategoriaId(Long categoriaId) {
        return jpaSubcategoriaRepository.findByCategoriaId(categoriaId).stream()
                .map(subcategoriaMapper::toDomain).toList();
    }
}
