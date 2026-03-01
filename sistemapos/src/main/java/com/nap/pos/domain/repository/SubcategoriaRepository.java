package com.nap.pos.domain.repository;

import com.nap.pos.domain.model.Subcategoria;

import java.util.List;
import java.util.Optional;

public interface SubcategoriaRepository {

    Subcategoria save(Subcategoria subcategoria);

    Optional<Subcategoria> findById(Long id);

    Optional<Subcategoria> findByNombre(String nombre);

    Optional<Subcategoria> findByNombreAndCategoriaNombre(String nombre, String categoriaNombre);

    List<Subcategoria> findAll();

    List<Subcategoria> findAllActivas();

    List<Subcategoria> findByCategoriaId(Long categoriaId);
}
