package com.nap.pos.infrastructure.persistence.mapper;

import com.nap.pos.domain.model.Producto;
import com.nap.pos.infrastructure.persistence.entity.ProductoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductoMapper {

    private final ProveedorMapper proveedorMapper;

    /** Entity (DB) → Domain model */
    public Producto toDomain(ProductoEntity entity) {
        if (entity == null) return null;
        return Producto.builder()
                .id(entity.getId())
                .codigoBarras(entity.getCodigoBarras())
                .nombre(entity.getNombre())
                .precioVenta(entity.getPrecioVenta())
                .precioCompra(entity.getPrecioCompra())
                .proveedorPrincipal(proveedorMapper.toDomain(entity.getProveedorPrincipal()))
                .ajusteProducto(entity.getAjusteProducto())
                .stock(entity.getStock())
                .activo(entity.isActivo())
                .build();
    }

    /** Domain model → Entity (DB) */
    public ProductoEntity toEntity(Producto domain) {
        if (domain == null) return null;
        ProductoEntity entity = new ProductoEntity();
        entity.setId(domain.getId());
        entity.setCodigoBarras(domain.getCodigoBarras());
        entity.setNombre(domain.getNombre());
        entity.setPrecioVenta(domain.getPrecioVenta());
        entity.setPrecioCompra(domain.getPrecioCompra());
        // Referencia mínima al proveedor (solo ID) — evita insertar un proveedor duplicado
        if (domain.getProveedorPrincipal() != null) {
            entity.setProveedorPrincipal(proveedorMapper.toRef(domain.getProveedorPrincipal().getId()));
        }
        entity.setAjusteProducto(domain.getAjusteProducto());
        entity.setStock(domain.getStock());
        entity.setActivo(domain.isActivo());
        return entity;
    }

    /** Referencia mínima por ID para establecer FK en otras entidades. */
    public ProductoEntity toRef(Long id) {
        if (id == null) return null;
        ProductoEntity ref = new ProductoEntity();
        ref.setId(id);
        return ref;
    }
}
