package com.nap.pos.infrastructure.persistence.repository.jpa;

import com.nap.pos.infrastructure.persistence.entity.CompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaCompraRepository extends JpaRepository<CompraEntity, Long> {

    // Usado para ver el historial de compras por proveedor
    List<CompraEntity> findByProveedorId(Long proveedorId);

    // Última compra registrada para un proveedor
    Optional<CompraEntity> findTopByProveedorIdOrderByFechaDesc(Long proveedorId);
}
