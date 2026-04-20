package com.nap.pos.domain.repository;

import com.nap.pos.domain.model.Gasto;
import com.nap.pos.domain.model.enums.FuenteGasto;
import com.nap.pos.domain.model.enums.TipoGasto;

import java.util.List;
import java.util.Optional;

public interface GastoRepository {
    Gasto save(Gasto gasto);
    Optional<Gasto> findById(Long id);
    List<Gasto> findAll();
    List<Gasto> findByTipo(TipoGasto tipo);
    List<Gasto> findByFuentePago(FuenteGasto fuentePago);
    void deleteById(Long id);
}
