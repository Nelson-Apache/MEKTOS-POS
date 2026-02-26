package com.mektos.pos.infrastructure.persistence.repository.impl;

import com.mektos.pos.domain.model.Caja;
import com.mektos.pos.domain.model.enums.EstadoCaja;
import com.mektos.pos.domain.repository.CajaRepository;
import com.mektos.pos.infrastructure.persistence.mapper.CajaMapper;
import com.mektos.pos.infrastructure.persistence.repository.jpa.JpaCajaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta de CajaRepository.
 * Traduce entre el modelo de dominio (Caja) y la entidad JPA (CajaEntity)
 * usando el mapper y delegando la persistencia al JpaCajaRepository.
 */
@Repository
@RequiredArgsConstructor
public class CajaRepositoryImpl implements CajaRepository {

    private final JpaCajaRepository jpaCajaRepository;
    private final CajaMapper cajaMapper;

    @Override
    public Caja save(Caja caja) {
        return cajaMapper.toDomain(
                jpaCajaRepository.save(cajaMapper.toEntity(caja))
        );
    }

    @Override
    public Optional<Caja> findById(Long id) {
        return jpaCajaRepository.findById(id)
                .map(cajaMapper::toDomain);
    }

    // Regla del PRD: solo puede haber una caja con estado ABIERTA a la vez.
    // Usado para validar antes de abrir una nueva caja y para obtener la caja activa.
    @Override
    public Optional<Caja> findCajaAbierta() {
        return jpaCajaRepository.findByEstado(EstadoCaja.ABIERTA)
                .map(cajaMapper::toDomain);
    }

    @Override
    public List<Caja> findAll() {
        return jpaCajaRepository.findAll().stream()
                .map(cajaMapper::toDomain)
                .toList();
    }
}
