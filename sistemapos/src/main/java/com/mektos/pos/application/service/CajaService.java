package com.mektos.pos.application.service;

import com.mektos.pos.domain.exception.BusinessException;
import com.mektos.pos.domain.model.Caja;
import com.mektos.pos.domain.model.enums.EstadoCaja;
import com.mektos.pos.domain.repository.CajaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CajaService {

    private final CajaRepository cajaRepository;

    /**
     * Abre una nueva caja registradora con el monto inicial en efectivo.
     * Regla del PRD: solo puede existir una caja ABIERTA a la vez.
     */
    @Transactional
    public Caja abrirCaja(BigDecimal montoInicial) {
        cajaRepository.findCajaAbierta()
                .ifPresent(c -> {
                    throw new BusinessException("Ya existe una caja abierta. Ciérrela antes de abrir una nueva.");
                });
        Caja caja = Caja.builder()
                .fechaApertura(LocalDateTime.now())
                .montoInicial(montoInicial)
                .estado(EstadoCaja.ABIERTA)
                .build();
        return cajaRepository.save(caja);
    }

    /**
     * Cierra la caja abierta registrando el monto final contado en efectivo.
     * El montoFinal permite comparar contra el total calculado de ventas.
     */
    @Transactional
    public Caja cerrarCaja(BigDecimal montoFinal) {
        Caja caja = getCajaAbierta();
        caja.cerrar(montoFinal);
        return cajaRepository.save(caja);
    }

    /**
     * Retorna la caja actualmente abierta.
     * Lanza excepción si no hay caja abierta — requerida para registrar ventas.
     */
    public Caja getCajaAbierta() {
        return cajaRepository.findCajaAbierta()
                .orElseThrow(() -> new BusinessException("No hay una caja abierta. Abra una caja para continuar."));
    }

    // Historial de todas las cajas (abiertas y cerradas)
    public List<Caja> findAll() {
        return cajaRepository.findAll();
    }
}
