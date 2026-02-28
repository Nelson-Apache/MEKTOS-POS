package com.mektos.pos.infrastructure.persistence.repository.impl;

import com.mektos.pos.domain.model.ConfiguracionTienda;
import com.mektos.pos.domain.repository.ConfiguracionRepository;
import com.mektos.pos.infrastructure.persistence.entity.ConfiguracionTiendaEntity;
import com.mektos.pos.infrastructure.persistence.mapper.ConfiguracionMapper;
import com.mektos.pos.infrastructure.persistence.repository.jpa.JpaConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConfiguracionRepositoryImpl implements ConfiguracionRepository {

    private final JpaConfiguracionRepository jpaRepository;
    private final ConfiguracionMapper mapper;

    @Override
    public Optional<ConfiguracionTienda> obtener() {
        return jpaRepository.findById(1L).map(mapper::toDomain);
    }

    @Override
    public ConfiguracionTienda guardar(ConfiguracionTienda config) {
        ConfiguracionTiendaEntity entity = mapper.toEntity(config);
        entity.setId(1L);   // fuerza id=1 — fila única
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
