package com.mektos.pos.infrastructure.persistence.repository.jpa;

import com.mektos.pos.infrastructure.persistence.entity.CompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCompraRepository extends JpaRepository<CompraEntity, Long> {

    // Usado para ver el historial de compras por proveedor
    List<CompraEntity> findByProveedorId(Long proveedorId);
}
