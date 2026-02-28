package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.model.enums.TipoPersona;
import com.nap.pos.domain.repository.ConfiguracionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @InjectMocks
    private ConfiguracionService configuracionService;

    // ── esPrimeraEjecucion ────────────────────────────────────────────────

    @Test
    void esPrimeraEjecucion_sinConfiguracionGuardada_retornaTrue() {
        when(configuracionRepository.obtener()).thenReturn(Optional.empty());

        assertThat(configuracionService.esPrimeraEjecucion()).isTrue();
    }

    @Test
    void esPrimeraEjecucion_conConfiguracionGuardada_retornaFalse() {
        when(configuracionRepository.obtener()).thenReturn(Optional.of(configNatural()));

        assertThat(configuracionService.esPrimeraEjecucion()).isFalse();
    }

    // ── guardar ───────────────────────────────────────────────────────────

    @Test
    void guardar_configValida_delegaAlRepositorioYRetornaGuardado() {
        ConfiguracionTienda config = configNatural();
        when(configuracionRepository.guardar(config)).thenReturn(config);

        ConfiguracionTienda resultado = configuracionService.guardar(config);

        verify(configuracionRepository).guardar(config);
        assertThat(resultado.getNombreTienda()).isEqualTo("Tienda Test");
        assertThat(resultado.getTipoPersona()).isEqualTo(TipoPersona.NATURAL);
    }

    @Test
    void guardar_personaJuridica_guarda_nitYRazonSocial() {
        ConfiguracionTienda config = ConfiguracionTienda.builder()
                .id(1L)
                .tipoPersona(TipoPersona.JURIDICA)
                .nombreTienda("Comercializadora ABC")
                .razonSocial("Comercializadora ABC S.A.S.")
                .nit("900123456-7")
                .direccion("Calle 10 # 5-20, Bogotá")
                .build();
        when(configuracionRepository.guardar(config)).thenReturn(config);

        ConfiguracionTienda resultado = configuracionService.guardar(config);

        assertThat(resultado.getNit()).isEqualTo("900123456-7");
        assertThat(resultado.getRazonSocial()).isEqualTo("Comercializadora ABC S.A.S.");
        assertThat(resultado.getNombre()).isNull();   // no aplica para jurídica
    }

    // ── obtener ───────────────────────────────────────────────────────────

    @Test
    void obtener_conConfiguracionGuardada_retornaConfig() {
        ConfiguracionTienda config = configNatural();
        when(configuracionRepository.obtener()).thenReturn(Optional.of(config));

        ConfiguracionTienda resultado = configuracionService.obtener();

        assertThat(resultado.getNombreTienda()).isEqualTo("Tienda Test");
        assertThat(resultado.getDireccion()).isEqualTo("Carrera 5 # 10-30");
    }

    @Test
    void obtener_sinConfiguracionGuardada_lanzaBusinessException() {
        when(configuracionRepository.obtener()).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> configuracionService.obtener());
    }

    // ── helper ────────────────────────────────────────────────────────────

    private ConfiguracionTienda configNatural() {
        return ConfiguracionTienda.builder()
                .id(1L)
                .tipoPersona(TipoPersona.NATURAL)
                .nombreTienda("Tienda Test")
                .nombre("Juan")
                .apellido("García")
                .cedula("12345678")
                .direccion("Carrera 5 # 10-30")
                .build();
    }
}
