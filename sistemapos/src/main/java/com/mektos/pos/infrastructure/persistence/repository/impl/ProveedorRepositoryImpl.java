package com.mektos.pos.infrastructure.persistence.repository.impl;

import com.mektos.pos.domain.model.Proveedor;
import com.mektos.pos.domain.repository.ProveedorRepository;
import com.mektos.pos.infrastructure.persistence.mapper.ProveedorMapper;
import com.mektos.pos.infrastructure.persistence.repository.jpa.JpaProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta de ProveedorRepository.
 * Traduce entre el modelo de dominio (Proveedor) y la entidad JPA (ProveedorEntity)
 * usando el mapper y delegando la persistencia al JpaProveedorRepository.
 */
@Repository
@RequiredArgsConstructor
public class ProveedorRepositoryImpl implements ProveedorRepository {

    private final JpaProveedorRepository jpaProveedorRepository;
    private final ProveedorMapper proveedorMapper;

    @Override
    public Proveedor save(Proveedor proveedor) {
        return proveedorMapper.toDomain(
                jpaProveedorRepository.save(proveedorMapper.toEntity(proveedor))
        );
    }

    @Override
    public Optional<Proveedor> findById(Long id) {
        return jpaProveedorRepository.findById(id)
                .map(proveedorMapper::toDomain);
    }

    // Devuelve todos los proveedores (activos e inactivos) para el módulo de gestión
    @Override
    public List<Proveedor> findAll() {
        return jpaProveedorRepository.findAll().stream()
                .map(proveedorMapper::toDomain)
                .toList();
    }

    // Solo proveedores activos — usados al asignar proveedor principal a un producto
    @Override
    public List<Proveedor> findAllActivos() {
        return jpaProveedorRepository.findByActivoTrue().stream()
                .map(proveedorMapper::toDomain)
                .toList();
    }

    // Usado para detectar NIT duplicado antes de guardar
    @Override
    public Optional<Proveedor> findByNit(String nit) {
        return jpaProveedorRepository.findByNit(nit)
                .map(proveedorMapper::toDomain);
    }

    // Usado para advertir si ya existe un proveedor con el mismo nombre
    @Override
    public Optional<Proveedor> findByNombre(String nombre) {
        return jpaProveedorRepository.findByNombre(nombre)
                .map(proveedorMapper::toDomain);
    }
}
