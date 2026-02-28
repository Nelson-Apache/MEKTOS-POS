package com.nap.pos.infrastructure.persistence.mapper;

import com.nap.pos.domain.model.Caja;
import com.nap.pos.infrastructure.persistence.entity.CajaEntity;
import org.springframework.stereotype.Component;

@Component
public class CajaMapper {

    /** Entity (DB) → Domain model */
    public Caja toDomain(CajaEntity entity) {
        if (entity == null) return null;
        return Caja.builder()
                .id(entity.getId())
                .fechaApertura(entity.getFechaApertura())
                .fechaCierre(entity.getFechaCierre())
                .montoInicial(entity.getMontoInicial())
                .montoFinal(entity.getMontoFinal())
                .estado(entity.getEstado())
                .build();
    }

    /** Domain model → Entity (DB) */
    public CajaEntity toEntity(Caja domain) {
        if (domain == null) return null;
        CajaEntity entity = new CajaEntity();
        entity.setId(domain.getId());
        entity.setFechaApertura(domain.getFechaApertura());
        entity.setFechaCierre(domain.getFechaCierre());
        entity.setMontoInicial(domain.getMontoInicial());
        entity.setMontoFinal(domain.getMontoFinal());
        entity.setEstado(domain.getEstado());
        return entity;
    }

    /** Referencia mínima por ID para establecer FK en otras entidades. */
    public CajaEntity toRef(Long id) {
        if (id == null) return null;
        CajaEntity ref = new CajaEntity();
        ref.setId(id);
        return ref;
    }
}
