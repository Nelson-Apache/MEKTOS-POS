package com.nap.pos.infrastructure.persistence.repository.jpa;

import com.nap.pos.infrastructure.persistence.entity.VentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JpaVentaRepository extends JpaRepository<VentaEntity, Long> {

    // Usado al cerrar caja para calcular el total de ventas del período
    List<VentaEntity> findByCajaId(Long cajaId);

    @Query("SELECT MAX(v.numeroComprobante) FROM VentaEntity v")
    Optional<Long> findMaxNumeroComprobante();
}
