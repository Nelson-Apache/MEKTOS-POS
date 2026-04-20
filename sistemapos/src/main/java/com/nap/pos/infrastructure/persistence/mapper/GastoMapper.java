package com.nap.pos.infrastructure.persistence.mapper;

import com.nap.pos.domain.model.Gasto;
import com.nap.pos.infrastructure.persistence.entity.GastoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GastoMapper {

    private final UsuarioMapper usuarioMapper;

    public Gasto toDomain(GastoEntity entity) {
        if (entity == null) return null;
        return Gasto.builder()
                .id(entity.getId())
                .fecha(entity.getFecha())
                .tipo(entity.getTipo())
                .fuentePago(entity.getFuentePago())
                .concepto(entity.getConcepto())
                .categoria(entity.getCategoria())
                .monto(entity.getMonto())
                .proveedor(entity.getProveedor())
                .referencia(entity.getReferencia())
                .notas(entity.getNotas())
                .usuario(usuarioMapper.toDomain(entity.getUsuario()))
                .build();
    }

    public GastoEntity toEntity(Gasto domain) {
        if (domain == null) return null;
        GastoEntity entity = new GastoEntity();
        entity.setId(domain.getId());
        entity.setFecha(domain.getFecha());
        entity.setTipo(domain.getTipo());
        entity.setFuentePago(domain.getFuentePago());
        entity.setConcepto(domain.getConcepto());
        entity.setCategoria(domain.getCategoria());
        entity.setMonto(domain.getMonto());
        entity.setProveedor(domain.getProveedor());
        entity.setReferencia(domain.getReferencia());
        entity.setNotas(domain.getNotas());
        entity.setUsuario(usuarioMapper.toRef(domain.getUsuario().getId()));
        return entity;
    }
}
