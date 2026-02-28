package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.Proveedor;
import com.nap.pos.domain.repository.ProductoRepository;
import com.nap.pos.domain.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    @Transactional
    public Proveedor crear(Proveedor proveedor) {
        validarDuplicados(proveedor, null);
        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public Proveedor actualizar(Proveedor proveedor) {
        findById(proveedor.getId());
        validarDuplicados(proveedor, proveedor.getId());
        return proveedorRepository.save(proveedor);
    }

    /**
     * Actualiza el porcentaje de ganancia del proveedor y recalcula automáticamente
     * el precioVenta de todos los productos que tienen este proveedor como principal.
     */
    @Transactional
    public void actualizarPorcentajeGanancia(Long proveedorId, BigDecimal nuevoPorcentaje) {
        Proveedor proveedor = findById(proveedorId);
        proveedor.actualizarPorcentajeGanancia(nuevoPorcentaje);
        proveedorRepository.save(proveedor);

        List<Producto> productos = productoRepository.findByProveedorPrincipalId(proveedorId);
        productos.forEach(p -> {
            p.calcularPrecioVenta();
            productoRepository.save(p);
        });
    }

    public Proveedor findById(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Proveedor con ID " + id + " no encontrado."));
    }

    public List<Proveedor> findAll() {
        return proveedorRepository.findAll();
    }

    public List<Proveedor> findAllActivos() {
        return proveedorRepository.findAllActivos();
    }

    // Valida que no exista otro proveedor con el mismo NIT o nombre.
    // idExcluir se usa al actualizar — excluye el propio registro del proveedor.
    private void validarDuplicados(Proveedor proveedor, Long idExcluir) {
        if (proveedor.getNit() != null && !proveedor.getNit().isBlank()) {
            proveedorRepository.findByNit(proveedor.getNit())
                    .filter(p -> !p.getId().equals(idExcluir))
                    .ifPresent(p -> {
                        throw new BusinessException("Ya existe un proveedor con NIT '" + proveedor.getNit() + "'.");
                    });
        }
        proveedorRepository.findByNombre(proveedor.getNombre())
                .filter(p -> !p.getId().equals(idExcluir))
                .ifPresent(p -> {
                    throw new BusinessException("Ya existe un proveedor con nombre '" + proveedor.getNombre() + "'.");
                });
    }
}
