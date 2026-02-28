package com.nap.pos.domain.repository;

import com.nap.pos.domain.model.Caja;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Caja.
 * La implementación concreta vive en infrastructure/persistence.
 */
public interface CajaRepository {

    Caja save(Caja caja);

    Optional<Caja> findById(Long id);

    // Regla del PRD: solo puede haber una caja abierta a la vez.
    // Este método se usa para validar antes de abrir una nueva caja
    // y para obtener la caja activa al registrar una venta.
    Optional<Caja> findCajaAbierta();

    List<Caja> findAll();
}
