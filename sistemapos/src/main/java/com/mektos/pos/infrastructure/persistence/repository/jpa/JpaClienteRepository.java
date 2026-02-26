package com.mektos.pos.infrastructure.persistence.repository.jpa;

import com.mektos.pos.infrastructure.persistence.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaClienteRepository extends JpaRepository<ClienteEntity, Long> {

    Optional<ClienteEntity> findByCedula(String cedula);

    List<ClienteEntity> findByActivoTrue();
}
