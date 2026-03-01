package com.nap.pos.infrastructure.persistence.repository.jpa;

import com.nap.pos.infrastructure.persistence.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaCategoriaRepository extends JpaRepository<CategoriaEntity, Long> {

    Optional<CategoriaEntity> findByNombre(String nombre);

    List<CategoriaEntity> findByActivoTrue();
}
