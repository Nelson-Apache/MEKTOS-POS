package com.mektos.pos.infrastructure.persistence.mapper;

import com.mektos.pos.domain.model.Proveedor;
import com.mektos.pos.infrastructure.persistence.entity.ProveedorEntity;
import org.springframework.stereotype.Component;

@Component
public class ProveedorMapper {

    /** Entity (DB) → Domain model */
    public Proveedor toDomain(ProveedorEntity entity) {
        if (entity == null) return null;
        return Proveedor.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .nit(entity.getNit())
                .celular(entity.getCelular())
                .direccion(entity.getDireccion())
                .porcentajeGanancia(entity.getPorcentajeGanancia())
                .activo(entity.isActivo())
                .build();
    }

    /** Domain model → Entity (DB) */
    public ProveedorEntity toEntity(Proveedor domain) {
        if (domain == null) return null;
        ProveedorEntity entity = new ProveedorEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setNit(domain.getNit());
        entity.setCelular(domain.getCelular());
        entity.setDireccion(domain.getDireccion());
        entity.setPorcentajeGanancia(domain.getPorcentajeGanancia());
        entity.setActivo(domain.isActivo());
        return entity;
    }

    /**
     * Crea una referencia mínima de entidad usando solo el ID.
     * Usado por otros mappers para establecer la FK sin cargar el objeto completo.
     */
    public ProveedorEntity toRef(Long id) {
        if (id == null) return null;
        ProveedorEntity ref = new ProveedorEntity();
        ref.setId(id);
        return ref;
    }
}
