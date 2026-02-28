package com.mektos.pos.infrastructure.persistence.repository.jpa;

import com.mektos.pos.infrastructure.persistence.entity.ConfiguracionTiendaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaConfiguracionRepository extends JpaRepository<ConfiguracionTiendaEntity, Long> {
}
