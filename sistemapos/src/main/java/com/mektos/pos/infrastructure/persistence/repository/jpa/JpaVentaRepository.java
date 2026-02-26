package com.mektos.pos.infrastructure.persistence.repository.jpa;

import com.mektos.pos.infrastructure.persistence.entity.VentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaVentaRepository extends JpaRepository<VentaEntity, Long> {

    // Usado al cerrar caja para calcular el total de ventas del período
    List<VentaEntity> findByCajaId(Long cajaId);
}
