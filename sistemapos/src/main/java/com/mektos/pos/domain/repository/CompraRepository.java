package com.mektos.pos.domain.repository;

import com.mektos.pos.domain.model.Compra;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Compra.
 * La implementación concreta vive en infrastructure/persistence.
 */
public interface CompraRepository {

    Compra save(Compra compra);

    Optional<Compra> findById(Long id);

    // Usado para ver el historial de compras por proveedor
    List<Compra> findByProveedorId(Long proveedorId);

    List<Compra> findAll();
}
