package com.nap.pos.infrastructure.persistence.mapper;

import com.nap.pos.domain.model.Subcategoria;
import com.nap.pos.infrastructure.persistence.entity.SubcategoriaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubcategoriaMapper {

    private final CategoriaMapper categoriaMapper;

    public Subcategoria toDomain(SubcategoriaEntity entity) {
        if (entity == null) return null;
        return Subcategoria.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .categoria(categoriaMapper.toDomain(entity.getCategoria()))
                .activo(entity.isActivo())
                .build();
    }

    public SubcategoriaEntity toEntity(Subcategoria domain) {
        if (domain == null) return null;
        SubcategoriaEntity entity = new SubcategoriaEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setActivo(domain.isActivo());
        if (domain.getCategoria() != null) {
            entity.setCategoria(categoriaMapper.toRef(domain.getCategoria().getId()));
        }
        return entity;
    }

    /** Referencia mínima por ID para uso como FK en ProductoEntity. */
    public SubcategoriaEntity toRef(Long id) {
        if (id == null) return null;
        SubcategoriaEntity ref = new SubcategoriaEntity();
        ref.setId(id);
        return ref;
    }
}
