package com.mektos.pos.application.service;

import com.mektos.pos.domain.exception.BusinessException;
import com.mektos.pos.domain.model.ConfiguracionTienda;
import com.mektos.pos.domain.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    /**
     * Retorna true si la BD no tiene configuración guardada todavía.
     * Se usa en el arranque para decidir si mostrar el wizard inicial.
     */
    public boolean esPrimeraEjecucion() {
        return configuracionRepository.obtener().isEmpty();
    }

    /**
     * Guarda o actualiza la configuración del negocio.
     * Siempre fuerza id = 1 (fila única en la tabla).
     */
    @Transactional
    public ConfiguracionTienda guardar(ConfiguracionTienda config) {
        return configuracionRepository.guardar(config);
    }

    /**
     * Obtiene la configuración del negocio.
     * Lanza BusinessException si el wizard todavía no se ha completado.
     */
    public ConfiguracionTienda obtener() {
        return configuracionRepository.obtener()
                .orElseThrow(() -> new BusinessException(
                        "La configuración del negocio no ha sido completada."));
    }
}
