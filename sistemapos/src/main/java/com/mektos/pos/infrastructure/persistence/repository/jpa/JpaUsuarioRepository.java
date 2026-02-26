package com.mektos.pos.infrastructure.persistence.repository.jpa;

import com.mektos.pos.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Spring Data genera la implementación SQL automáticamente en base al nombre del método
public interface JpaUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByUsername(String username);
}
