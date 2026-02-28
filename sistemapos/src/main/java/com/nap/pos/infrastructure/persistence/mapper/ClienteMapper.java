package com.nap.pos.infrastructure.persistence.mapper;

import com.nap.pos.domain.model.Cliente;
import com.nap.pos.infrastructure.persistence.entity.ClienteEntity;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    /** Entity (DB) → Domain model */
    public Cliente toDomain(ClienteEntity entity) {
        if (entity == null) return null;
        return Cliente.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .cedula(entity.getCedula())
                .celular(entity.getCelular())
                .direccion(entity.getDireccion())
                .montoCredito(entity.getMontoCredito())
                .plazoPago(entity.getPlazoPago())
                .saldoUtilizado(entity.getSaldoUtilizado())
                .activo(entity.isActivo())
                .build();
    }

    /** Domain model → Entity (DB) */
    public ClienteEntity toEntity(Cliente domain) {
        if (domain == null) return null;
        ClienteEntity entity = new ClienteEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setCedula(domain.getCedula());
        entity.setCelular(domain.getCelular());
        entity.setDireccion(domain.getDireccion());
        entity.setMontoCredito(domain.getMontoCredito());
        entity.setPlazoPago(domain.getPlazoPago());
        entity.setSaldoUtilizado(domain.getSaldoUtilizado());
        entity.setActivo(domain.isActivo());
        return entity;
    }

    /** Referencia mínima por ID para establecer FK en otras entidades. */
    public ClienteEntity toRef(Long id) {
        if (id == null) return null;
        ClienteEntity ref = new ClienteEntity();
        ref.setId(id);
        return ref;
    }
}
