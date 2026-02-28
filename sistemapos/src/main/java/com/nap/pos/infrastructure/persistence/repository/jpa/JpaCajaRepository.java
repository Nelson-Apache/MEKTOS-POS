package com.nap.pos.infrastructure.persistence.repository.jpa;

import com.nap.pos.domain.model.enums.EstadoCaja;
import com.nap.pos.infrastructure.persistence.entity.CajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaCajaRepository extends JpaRepository<CajaEntity, Long> {

    // Busca la caja con estado ABIERTA — debe haber como máximo una
    Optional<CajaEntity> findByEstado(EstadoCaja estado);
}
