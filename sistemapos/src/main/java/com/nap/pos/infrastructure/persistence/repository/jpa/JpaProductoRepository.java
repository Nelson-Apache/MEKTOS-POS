package com.nap.pos.infrastructure.persistence.repository.jpa;

import com.nap.pos.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaProductoRepository extends JpaRepository<ProductoEntity, Long> {

    Optional<ProductoEntity> findByCodigoBarras(String codigoBarras);

    List<ProductoEntity> findByActivoTrue();

    // Usado para recalcular precios cuando cambia el % de ganancia del proveedor
    List<ProductoEntity> findByProveedorPrincipalId(Long proveedorId);
}
