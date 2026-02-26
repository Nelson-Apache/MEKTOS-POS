package com.mektos.pos.infrastructure.persistence.mapper;

import com.mektos.pos.domain.model.Usuario;
import com.mektos.pos.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    /** Entity (DB) → Domain model */
    public Usuario toDomain(UsuarioEntity entity) {
        if (entity == null) return null;
        return Usuario.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .passwordHash(entity.getPasswordHash())
                .rol(entity.getRol())
                .activo(entity.isActivo())
                .build();
    }

    /** Domain model → Entity (DB) */
    public UsuarioEntity toEntity(Usuario domain) {
        if (domain == null) return null;
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(domain.getId());
        entity.setUsername(domain.getUsername());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRol(domain.getRol());
        entity.setActivo(domain.isActivo());
        return entity;
    }

    /** Referencia mínima por ID para establecer FK en otras entidades. */
    public UsuarioEntity toRef(Long id) {
        if (id == null) return null;
        UsuarioEntity ref = new UsuarioEntity();
        ref.setId(id);
        return ref;
    }
}
