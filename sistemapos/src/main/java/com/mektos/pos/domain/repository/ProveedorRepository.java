package com.mektos.pos.domain.repository;

import com.mektos.pos.domain.model.Proveedor;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Proveedor.
 * La implementación concreta vive en infrastructure/persistence.
 */
public interface ProveedorRepository {

    Proveedor save(Proveedor proveedor);

    Optional<Proveedor> findById(Long id);

    List<Proveedor> findAll();

    // Solo proveedores activos — usados al asignar proveedor principal a un producto
    List<Proveedor> findAllActivos();

    // Usado para detectar NIT duplicado antes de guardar (el NIT es opcional pero único si existe)
    Optional<Proveedor> findByNit(String nit);

    // Usado para advertir si ya existe un proveedor con el mismo nombre
    Optional<Proveedor> findByNombre(String nombre);
}
