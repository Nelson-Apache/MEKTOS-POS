package com.nap.pos.infrastructure.persistence.mapper;

import com.nap.pos.domain.model.Compra;
import com.nap.pos.domain.model.DetalleCompra;
import com.nap.pos.infrastructure.persistence.entity.CompraEntity;
import com.nap.pos.infrastructure.persistence.entity.DetalleCompraEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompraMapper {

    private final ProveedorMapper proveedorMapper;
    private final UsuarioMapper usuarioMapper;
    private final ProductoMapper productoMapper;

    /** Entity (DB) → Domain model */
    public Compra toDomain(CompraEntity entity) {
        if (entity == null) return null;
        return Compra.builder()
                .id(entity.getId())
                .fecha(entity.getFecha())
                .proveedor(proveedorMapper.toDomain(entity.getProveedor()))
                .numeroFactura(entity.getNumeroFactura())
                .total(entity.getTotal())
                .usuario(usuarioMapper.toDomain(entity.getUsuario()))
                .detalles(detallesEntityToDomain(entity.getDetalles()))
                .build();
    }

    /** Domain model → Entity (DB) */
    public CompraEntity toEntity(Compra domain) {
        if (domain == null) return null;
        CompraEntity entity = new CompraEntity();
        entity.setId(domain.getId());
        entity.setFecha(domain.getFecha());
        entity.setNumeroFactura(domain.getNumeroFactura());
        entity.setTotal(domain.getTotal());
        // Referencias mínimas para establecer FK sin recargar objetos completos
        entity.setProveedor(proveedorMapper.toRef(domain.getProveedor().getId()));
        entity.setUsuario(usuarioMapper.toRef(domain.getUsuario().getId()));
        // Mapea detalles y establece la referencia bidireccional compra ↔ detalle
        List<DetalleCompraEntity> detalles = detallesDomainToEntity(domain.getDetalles(), entity);
        entity.setDetalles(detalles);
        return entity;
    }

    // ── Métodos privados para mapear detalles ──────────────────────────────

    private List<DetalleCompra> detallesEntityToDomain(List<DetalleCompraEntity> entities) {
        return entities.stream()
                .map(d -> DetalleCompra.builder()
                        .id(d.getId())
                        .producto(productoMapper.toDomain(d.getProducto()))
                        .cantidad(d.getCantidad())
                        .precioCompraUnitario(d.getPrecioCompraUnitario())
                        .subtotal(d.getSubtotal())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DetalleCompraEntity> detallesDomainToEntity(List<DetalleCompra> detalles,
                                                              CompraEntity compraEntity) {
        return detalles.stream()
                .map(d -> {
                    DetalleCompraEntity e = new DetalleCompraEntity();
                    e.setId(d.getId());
                    // Referencia bidireccional requerida por JPA (mappedBy = "compra")
                    e.setCompra(compraEntity);
                    e.setProducto(productoMapper.toRef(d.getProducto().getId()));
                    e.setCantidad(d.getCantidad());
                    e.setPrecioCompraUnitario(d.getPrecioCompraUnitario());
                    e.setSubtotal(d.getSubtotal());
                    return e;
                })
                .collect(Collectors.toList());
    }
}
