package com.nap.pos.infrastructure.persistence.mapper;

import com.nap.pos.domain.model.DetalleVenta;
import com.nap.pos.domain.model.Venta;
import com.nap.pos.infrastructure.persistence.entity.DetalleVentaEntity;
import com.nap.pos.infrastructure.persistence.entity.VentaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VentaMapper {

    private final ClienteMapper clienteMapper;
    private final UsuarioMapper usuarioMapper;
    private final CajaMapper cajaMapper;
    private final ProductoMapper productoMapper;

    /** Entity (DB) → Domain model */
    public Venta toDomain(VentaEntity entity) {
        if (entity == null) return null;
        return Venta.builder()
                .id(entity.getId())
                .fecha(entity.getFecha())
                .total(entity.getTotal())
                .metodoPago(entity.getMetodoPago())
                .cliente(clienteMapper.toDomain(entity.getCliente()))
                .usuario(usuarioMapper.toDomain(entity.getUsuario()))
                .caja(cajaMapper.toDomain(entity.getCaja()))
                .estado(entity.getEstado())
                .detalles(detallesEntityToDomain(entity.getDetalles()))
                .build();
    }

    /** Domain model → Entity (DB) */
    public VentaEntity toEntity(Venta domain) {
        if (domain == null) return null;
        VentaEntity entity = new VentaEntity();
        entity.setId(domain.getId());
        entity.setFecha(domain.getFecha());
        entity.setTotal(domain.getTotal());
        entity.setMetodoPago(domain.getMetodoPago());
        entity.setEstado(domain.getEstado());
        // Referencias mínimas para establecer FK sin recargar objetos completos
        entity.setCliente(domain.getCliente() != null
                ? clienteMapper.toRef(domain.getCliente().getId()) : null);
        entity.setUsuario(usuarioMapper.toRef(domain.getUsuario().getId()));
        entity.setCaja(cajaMapper.toRef(domain.getCaja().getId()));
        // Mapea detalles y establece la referencia bidireccional venta ↔ detalle
        List<DetalleVentaEntity> detalles = detallesDomainToEntity(domain.getDetalles(), entity);
        entity.setDetalles(detalles);
        return entity;
    }

    // ── Métodos privados para mapear detalles ──────────────────────────────

    private List<DetalleVenta> detallesEntityToDomain(List<DetalleVentaEntity> entities) {
        return entities.stream()
                .map(d -> DetalleVenta.builder()
                        .id(d.getId())
                        .producto(productoMapper.toDomain(d.getProducto()))
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .subtotal(d.getSubtotal())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DetalleVentaEntity> detallesDomainToEntity(List<DetalleVenta> detalles,
                                                             VentaEntity ventaEntity) {
        return detalles.stream()
                .map(d -> {
                    DetalleVentaEntity e = new DetalleVentaEntity();
                    e.setId(d.getId());
                    // Referencia bidireccional requerida por JPA (mappedBy = "venta")
                    e.setVenta(ventaEntity);
                    e.setProducto(productoMapper.toRef(d.getProducto().getId()));
                    e.setCantidad(d.getCantidad());
                    e.setPrecioUnitario(d.getPrecioUnitario());
                    e.setSubtotal(d.getSubtotal());
                    return e;
                })
                .collect(Collectors.toList());
    }
}
