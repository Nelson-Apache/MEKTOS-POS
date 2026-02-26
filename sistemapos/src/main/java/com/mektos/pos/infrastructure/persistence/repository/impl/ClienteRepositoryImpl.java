package com.mektos.pos.infrastructure.persistence.repository.impl;

import com.mektos.pos.domain.model.Cliente;
import com.mektos.pos.domain.repository.ClienteRepository;
import com.mektos.pos.infrastructure.persistence.mapper.ClienteMapper;
import com.mektos.pos.infrastructure.persistence.repository.jpa.JpaClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta de ClienteRepository.
 * Traduce entre el modelo de dominio (Cliente) y la entidad JPA (ClienteEntity)
 * usando el mapper y delegando la persistencia al JpaClienteRepository.
 */
@Repository
@RequiredArgsConstructor
public class ClienteRepositoryImpl implements ClienteRepository {

    private final JpaClienteRepository jpaClienteRepository;
    private final ClienteMapper clienteMapper;

    @Override
    public Cliente save(Cliente cliente) {
        return clienteMapper.toDomain(
                jpaClienteRepository.save(clienteMapper.toEntity(cliente))
        );
    }

    @Override
    public Optional<Cliente> findById(Long id) {
        return jpaClienteRepository.findById(id)
                .map(clienteMapper::toDomain);
    }

    // Usado para evitar duplicados al registrar un nuevo cliente
    @Override
    public Optional<Cliente> findByCedula(String cedula) {
        return jpaClienteRepository.findByCedula(cedula)
                .map(clienteMapper::toDomain);
    }

    // Módulo de gestión: muestra TODOS los clientes (activos e inactivos)
    // Permite al ADMIN reactivar un cliente desactivado
    @Override
    public List<Cliente> findAll() {
        return jpaClienteRepository.findAll().stream()
                .map(clienteMapper::toDomain)
                .toList();
    }

    // Pantalla de ventas: solo clientes activos disponibles para asignar a una venta
    @Override
    public List<Cliente> findAllActivos() {
        return jpaClienteRepository.findByActivoTrue().stream()
                .map(clienteMapper::toDomain)
                .toList();
    }
}
