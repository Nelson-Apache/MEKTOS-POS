package com.nap.pos.domain.repository;

import com.nap.pos.domain.model.ConfiguracionTienda;

import java.util.Optional;

public interface ConfiguracionRepository {

    Optional<ConfiguracionTienda> obtener();

    ConfiguracionTienda guardar(ConfiguracionTienda config);
}
