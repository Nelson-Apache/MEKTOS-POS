package com.nap.pos.domain.repository;

import com.nap.pos.domain.model.Producto;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Producto.
 * La implementación concreta vive en infrastructure/persistence.
 */
public interface ProductoRepository {

    Producto save(Producto producto);

    Optional<Producto> findById(Long id);

    // Usado para búsqueda por scanner de código de barras en la pantalla de ventas
    Optional<Producto> findByCodigoBarras(String codigoBarras);

    // Módulo de inventario: muestra TODOS los productos, activos e inactivos
    List<Producto> findAll();

    // Pantalla de ventas: solo productos activos disponibles para vender
    List<Producto> findAllActivos();

    // Usado para recalcular precios cuando el ADMIN cambia el % de ganancia del proveedor
    List<Producto> findByProveedorPrincipalId(Long proveedorId);

    // Productos activos cuyo stock está en o por debajo del umbral mínimo global
    List<Producto> findByStockBajoYActivo(int stockMinimo);
}
