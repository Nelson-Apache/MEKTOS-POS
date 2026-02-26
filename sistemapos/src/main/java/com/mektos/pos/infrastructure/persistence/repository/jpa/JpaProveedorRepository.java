package com.mektos.pos.infrastructure.persistence.repository.jpa;

import com.mektos.pos.infrastructure.persistence.entity.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaProveedorRepository extends JpaRepository<ProveedorEntity, Long> {

    List<ProveedorEntity> findByActivoTrue();

    Optional<ProveedorEntity> findByNit(String nit);

    Optional<ProveedorEntity> findByNombre(String nombre);
}
