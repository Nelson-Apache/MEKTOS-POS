package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Caja;
import com.nap.pos.domain.model.enums.EstadoCaja;
import com.nap.pos.domain.repository.CajaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    @Mock
    private CajaRepository cajaRepository;

    @InjectMocks
    private CajaService cajaService;

    // --- abrirCaja ---

    @Test
    void abrirCaja_sinCajaAbierta_creaYGuarda() {
        when(cajaRepository.findCajaAbierta()).thenReturn(Optional.empty());
        Caja cajaGuardada = Caja.builder()
                .id(1L).fechaApertura(LocalDateTime.now())
                .montoInicial(new BigDecimal("500000"))
                .estado(EstadoCaja.ABIERTA).build();
        when(cajaRepository.save(any(Caja.class))).thenReturn(cajaGuardada);

        Caja resultado = cajaService.abrirCaja(new BigDecimal("500000"));

        assertThat(resultado.getEstado()).isEqualTo(EstadoCaja.ABIERTA);
        assertThat(resultado.getMontoInicial()).isEqualByComparingTo("500000");
        verify(cajaRepository).save(any(Caja.class));
    }

    @Test
    void abrirCaja_conCajaYaAbierta_lanzaBusinessException() {
        Caja cajaExistente = Caja.builder()
                .id(1L).estado(EstadoCaja.ABIERTA)
                .fechaApertura(LocalDateTime.now().minusHours(3))
                .montoInicial(new BigDecimal("200000")).build();
        when(cajaRepository.findCajaAbierta()).thenReturn(Optional.of(cajaExistente));

        assertThrows(BusinessException.class,
                () -> cajaService.abrirCaja(new BigDecimal("500000")));
        verify(cajaRepository, never()).save(any());
    }

    // --- cerrarCaja ---

    @Test
    void cerrarCaja_cajaAbierta_registraMontoFinalYGuarda() {
        Caja cajaAbierta = Caja.builder()
                .id(1L).estado(EstadoCaja.ABIERTA)
                .fechaApertura(LocalDateTime.now().minusHours(8))
                .montoInicial(new BigDecimal("200000")).build();
        when(cajaRepository.findCajaAbierta()).thenReturn(Optional.of(cajaAbierta));
        when(cajaRepository.save(any(Caja.class))).thenAnswer(inv -> inv.getArgument(0));

        Caja resultado = cajaService.cerrarCaja(new BigDecimal("750000"));

        assertThat(resultado.getEstado()).isEqualTo(EstadoCaja.CERRADA);
        assertThat(resultado.getMontoFinal()).isEqualByComparingTo("750000");
        assertThat(resultado.getFechaCierre()).isNotNull();
        verify(cajaRepository).save(cajaAbierta);
    }

    // --- getCajaAbierta ---

    @Test
    void getCajaAbierta_sinCajaAbierta_lanzaBusinessException() {
        when(cajaRepository.findCajaAbierta()).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> cajaService.getCajaAbierta());
    }
}
