package com.nap.pos.infrastructure.persistence.repository.jpa;

import com.nap.pos.infrastructure.persistence.entity.ConfiguracionTiendaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaConfiguracionRepository extends JpaRepository<ConfiguracionTiendaEntity, Long> {
}
