package com.nap.pos.infrastructure.persistence.repository.jpa;

import com.nap.pos.domain.model.enums.FuenteGasto;
import com.nap.pos.domain.model.enums.TipoGasto;
import com.nap.pos.infrastructure.persistence.entity.GastoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaGastoRepository extends JpaRepository<GastoEntity, Long> {
    List<GastoEntity> findByTipo(TipoGasto tipo);
    List<GastoEntity> findByFuentePago(FuenteGasto fuentePago);
}
