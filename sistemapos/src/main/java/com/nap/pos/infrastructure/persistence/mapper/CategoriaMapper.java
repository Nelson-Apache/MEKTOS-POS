package com.nap.pos.infrastructure.persistence.mapper;

import com.nap.pos.domain.model.Categoria;
import com.nap.pos.infrastructure.persistence.entity.CategoriaEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public Categoria toDomain(CategoriaEntity entity) {
        if (entity == null) return null;
        return Categoria.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .activo(entity.isActivo())
                .build();
    }

    public CategoriaEntity toEntity(Categoria domain) {
        if (domain == null) return null;
        CategoriaEntity entity = new CategoriaEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setActivo(domain.isActivo());
        return entity;
    }

    /** Referencia mínima por ID para uso como FK en SubcategoriaEntity. */
    public CategoriaEntity toRef(Long id) {
        if (id == null) return null;
        CategoriaEntity ref = new CategoriaEntity();
        ref.setId(id);
        return ref;
    }
}
