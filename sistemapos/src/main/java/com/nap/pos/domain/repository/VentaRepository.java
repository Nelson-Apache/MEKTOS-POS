package com.nap.pos.domain.repository;

import com.nap.pos.domain.model.Venta;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Venta.
 * La implementación concreta vive en infrastructure/persistence.
 */
public interface VentaRepository {

    Venta save(Venta venta);

    Optional<Venta> findById(Long id);

    // Usado al cerrar caja para calcular el total de ventas del período
    List<Venta> findByCajaId(Long cajaId);

    List<Venta> findAll();
}
