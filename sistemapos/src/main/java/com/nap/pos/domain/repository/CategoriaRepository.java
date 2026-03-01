package com.nap.pos.domain.repository;

import com.nap.pos.domain.model.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository {

    Categoria save(Categoria categoria);

    Optional<Categoria> findById(Long id);

    Optional<Categoria> findByNombre(String nombre);

    List<Categoria> findAll();

    List<Categoria> findAllActivas();
}
