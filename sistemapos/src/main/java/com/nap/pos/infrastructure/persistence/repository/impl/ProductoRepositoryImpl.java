package com.nap.pos.infrastructure.persistence.repository.impl;

import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.repository.ProductoRepository;
import com.nap.pos.infrastructure.persistence.mapper.ProductoMapper;
import com.nap.pos.infrastructure.persistence.repository.jpa.JpaProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta de ProductoRepository.
 * Traduce entre el modelo de dominio (Producto) y la entidad JPA (ProductoEntity)
 * usando el mapper y delegando la persistencia al JpaProductoRepository.
 */
@Repository
@RequiredArgsConstructor
public class ProductoRepositoryImpl implements ProductoRepository {

    private final JpaProductoRepository jpaProductoRepository;
    private final ProductoMapper productoMapper;

    @Override
    public Producto save(Producto producto) {
        return productoMapper.toDomain(
                jpaProductoRepository.save(productoMapper.toEntity(producto))
        );
    }

    @Override
    public Optional<Producto> findById(Long id) {
        return jpaProductoRepository.findById(id)
                .map(productoMapper::toDomain);
    }

    // Búsqueda por scanner en la pantalla de ventas
    @Override
    public Optional<Producto> findByCodigoBarras(String codigoBarras) {
        return jpaProductoRepository.findByCodigoBarras(codigoBarras)
                .map(productoMapper::toDomain);
    }

    // Módulo de inventario: muestra TODOS los productos, activos e inactivos
    @Override
    public List<Producto> findAll() {
        return jpaProductoRepository.findAll().stream()
                .map(productoMapper::toDomain)
                .toList();
    }

    // Pantalla de ventas: solo productos activos disponibles para vender
    @Override
    public List<Producto> findAllActivos() {
        return jpaProductoRepository.findByActivoTrue().stream()
                .map(productoMapper::toDomain)
                .toList();
    }

    // Usado para recalcular precios cuando el ADMIN cambia el % de ganancia del proveedor
    @Override
    public List<Producto> findByProveedorPrincipalId(Long proveedorId) {
        return jpaProductoRepository.findByProveedorPrincipalId(proveedorId).stream()
                .map(productoMapper::toDomain)
                .toList();
    }

    // Productos activos con stock igual o por debajo del umbral mínimo
    @Override
    public List<Producto> findByStockBajoYActivo(int stockMinimo) {
        return jpaProductoRepository.findByActivoTrueAndStockLessThanEqual(stockMinimo).stream()
                .map(productoMapper::toDomain)
                .toList();
    }
}
