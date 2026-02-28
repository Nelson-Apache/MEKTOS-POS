package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Cliente;
import com.nap.pos.domain.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public Cliente crear(Cliente cliente) {
        clienteRepository.findByCedula(cliente.getCedula())
                .ifPresent(c -> {
                    throw new BusinessException("Ya existe un cliente con cédula '" + cliente.getCedula() + "'.");
                });
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Cliente cliente) {
        findById(cliente.getId());
        clienteRepository.findByCedula(cliente.getCedula())
                .filter(c -> !c.getId().equals(cliente.getId()))
                .ifPresent(c -> {
                    throw new BusinessException("Ya existe un cliente con cédula '" + cliente.getCedula() + "'.");
                });
        return clienteRepository.save(cliente);
    }

    /** Reactiva el cliente para que vuelva a aparecer disponible en ventas. */
    @Transactional
    public void activar(Long id) {
        Cliente cliente = findById(id);
        cliente.activar();
        clienteRepository.save(cliente);
    }

    /** Desactiva el cliente sin eliminarlo — preserva el historial de ventas. */
    @Transactional
    public void desactivar(Long id) {
        Cliente cliente = findById(id);
        cliente.desactivar();
        clienteRepository.save(cliente);
    }

    /**
     * Registra un pago parcial o total sobre la deuda de crédito del cliente.
     * El saldo utilizado disminuye, liberando capacidad de crédito disponible.
     */
    @Transactional
    public Cliente abonar(Long id, BigDecimal monto) {
        Cliente cliente = findById(id);
        cliente.abonar(monto);
        return clienteRepository.save(cliente);
    }

    // Búsqueda por cédula — usada para verificar duplicados y en la pantalla de ventas
    public Optional<Cliente> findByCedula(String cedula) {
        return clienteRepository.findByCedula(cedula);
    }

    public Cliente findById(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cliente con ID " + id + " no encontrado."));
    }

    // Módulo de gestión: todos los clientes (activos e inactivos)
    public List<Cliente> findAll() {
        return clienteRepository.findAll();
    }

    // Pantalla de ventas: solo clientes activos
    public List<Cliente> findAllActivos() {
        return clienteRepository.findAllActivos();
    }
}
