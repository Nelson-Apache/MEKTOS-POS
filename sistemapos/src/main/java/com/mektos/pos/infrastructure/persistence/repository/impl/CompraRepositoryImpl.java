package com.mektos.pos.infrastructure.persistence.repository.impl;

import com.mektos.pos.domain.model.Compra;
import com.mektos.pos.domain.repository.CompraRepository;
import com.mektos.pos.infrastructure.persistence.mapper.CompraMapper;
import com.mektos.pos.infrastructure.persistence.repository.jpa.JpaCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta de CompraRepository.
 * Traduce entre el modelo de dominio (Compra) y la entidad JPA (CompraEntity)
 * usando el mapper y delegando la persistencia al JpaCompraRepository.
 */
@Repository
@RequiredArgsConstructor
public class CompraRepositoryImpl implements CompraRepository {

    private final JpaCompraRepository jpaCompraRepository;
    private final CompraMapper compraMapper;

    @Override
    public Compra save(Compra compra) {
        return compraMapper.toDomain(
                jpaCompraRepository.save(compraMapper.toEntity(compra))
        );
    }

    @Override
    public Optional<Compra> findById(Long id) {
        return jpaCompraRepository.findById(id)
                .map(compraMapper::toDomain);
    }

    // Usado para ver el historial de compras por proveedor
    @Override
    public List<Compra> findByProveedorId(Long proveedorId) {
        return jpaCompraRepository.findByProveedorId(proveedorId).stream()
                .map(compraMapper::toDomain)
                .toList();
    }

    @Override
    public List<Compra> findAll() {
        return jpaCompraRepository.findAll().stream()
                .map(compraMapper::toDomain)
                .toList();
    }
}
