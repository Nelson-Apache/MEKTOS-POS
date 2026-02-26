package com.mektos.pos.domain.repository;

import com.mektos.pos.domain.model.Cliente;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Cliente.
 * La implementación concreta vive en infrastructure/persistence.
 */
public interface ClienteRepository {

    Cliente save(Cliente cliente);

    Optional<Cliente> findById(Long id);

    // Usado para evitar duplicados al registrar un nuevo cliente
    Optional<Cliente> findByCedula(String cedula);

    // Módulo de gestión: muestra TODOS los clientes, activos e inactivos
    // Permite al ADMIN reactivar un cliente desactivado
    List<Cliente> findAll();

    // Pantalla de ventas: solo clientes activos disponibles para asignar a una venta
    List<Cliente> findAllActivos();
}
