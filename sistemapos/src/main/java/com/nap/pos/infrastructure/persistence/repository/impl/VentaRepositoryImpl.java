package com.nap.pos.infrastructure.persistence.repository.impl;

import com.nap.pos.domain.model.Venta;
import com.nap.pos.domain.repository.VentaRepository;
import com.nap.pos.infrastructure.persistence.mapper.VentaMapper;
import com.nap.pos.infrastructure.persistence.repository.jpa.JpaVentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta de VentaRepository.
 * Traduce entre el modelo de dominio (Venta) y la entidad JPA (VentaEntity)
 * usando el mapper y delegando la persistencia al JpaVentaRepository.
 */
@Repository
@RequiredArgsConstructor
public class VentaRepositoryImpl implements VentaRepository {

    private final JpaVentaRepository jpaVentaRepository;
    private final VentaMapper ventaMapper;

    @Override
    public Venta save(Venta venta) {
        return ventaMapper.toDomain(
                jpaVentaRepository.save(ventaMapper.toEntity(venta))
        );
    }

    @Override
    public Optional<Venta> findById(Long id) {
        return jpaVentaRepository.findById(id)
                .map(ventaMapper::toDomain);
    }

    // Usado al cerrar caja para calcular el total de ventas del período
    @Override
    public List<Venta> findByCajaId(Long cajaId) {
        return jpaVentaRepository.findByCajaId(cajaId).stream()
                .map(ventaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Venta> findAll() {
        return jpaVentaRepository.findAll().stream()
                .map(ventaMapper::toDomain)
                .toList();
    }
}
