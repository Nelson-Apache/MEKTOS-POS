package com.nap.pos.infrastructure.persistence.repository.impl;

import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.repository.ConfiguracionRepository;
import com.nap.pos.infrastructure.persistence.entity.ConfiguracionTiendaEntity;
import com.nap.pos.infrastructure.persistence.mapper.ConfiguracionMapper;
import com.nap.pos.infrastructure.persistence.repository.jpa.JpaConfiguracionRepository;
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
