package com.mektos.pos.domain.repository;

import com.mektos.pos.domain.model.ConfiguracionTienda;

import java.util.Optional;

public interface ConfiguracionRepository {

    Optional<ConfiguracionTienda> obtener();

    ConfiguracionTienda guardar(ConfiguracionTienda config);
}
