package com.nap.pos.infrastructure.persistence.repository.impl;

import com.nap.pos.domain.model.Gasto;
import com.nap.pos.domain.model.enums.FuenteGasto;
import com.nap.pos.domain.model.enums.TipoGasto;
import com.nap.pos.domain.repository.GastoRepository;
import com.nap.pos.infrastructure.persistence.mapper.GastoMapper;
import com.nap.pos.infrastructure.persistence.repository.jpa.JpaGastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GastoRepositoryImpl implements GastoRepository {

    private final JpaGastoRepository jpaGastoRepository;
    private final GastoMapper gastoMapper;

    @Override
    public Gasto save(Gasto gasto) {
        return gastoMapper.toDomain(
                jpaGastoRepository.save(gastoMapper.toEntity(gasto))
        );
    }

    @Override
    public Optional<Gasto> findById(Long id) {
        return jpaGastoRepository.findById(id)
                .map(gastoMapper::toDomain);
    }

    @Override
    public List<Gasto> findAll() {
        return jpaGastoRepository.findAll().stream()
                .map(gastoMapper::toDomain)
                .toList();
    }

    @Override
    public List<Gasto> findByTipo(TipoGasto tipo) {
        return jpaGastoRepository.findByTipo(tipo).stream()
                .map(gastoMapper::toDomain)
                .toList();
    }

    @Override
    public List<Gasto> findByFuentePago(FuenteGasto fuentePago) {
        return jpaGastoRepository.findByFuentePago(fuentePago).stream()
                .map(gastoMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaGastoRepository.deleteById(id);
    }
}
