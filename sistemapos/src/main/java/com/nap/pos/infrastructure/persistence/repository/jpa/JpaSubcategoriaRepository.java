package com.nap.pos.infrastructure.persistence.repository.jpa;

import com.nap.pos.infrastructure.persistence.entity.SubcategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaSubcategoriaRepository extends JpaRepository<SubcategoriaEntity, Long> {

    Optional<SubcategoriaEntity> findByNombre(String nombre);

    Optional<SubcategoriaEntity> findByNombreAndCategoriaNombre(String nombre, String categoriaNombre);

    List<SubcategoriaEntity> findByActivoTrue();

    List<SubcategoriaEntity> findByCategoriaId(Long categoriaId);
}
