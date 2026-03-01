package com.nap.pos.infrastructure.persistence.mapper;

import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.infrastructure.persistence.entity.ConfiguracionTiendaEntity;
import com.nap.pos.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

@Component
public class ConfiguracionMapper {

    public ConfiguracionTiendaEntity toEntity(ConfiguracionTienda domain) {
        ConfiguracionTiendaEntity e = new ConfiguracionTiendaEntity();
        e.setId(domain.getId());
        e.setTipoPersona(domain.getTipoPersona());
        e.setNombreTienda(domain.getNombreTienda());
        e.setNombre(domain.getNombre());
        e.setApellido(domain.getApellido());
        e.setCedula(domain.getCedula());
        e.setRazonSocial(domain.getRazonSocial());
        e.setNit(domain.getNit());
        e.setRepresentanteLegalNombre(domain.getRepresentanteLegalNombre());
        e.setRepresentanteLegalApellido(domain.getRepresentanteLegalApellido());
        e.setDireccion(domain.getDireccion());
        e.setRutaLogo(domain.getRutaLogo());
        e.setTelefono(domain.getTelefono());
        e.setCorreo(domain.getCorreo());
        e.setStockMinimoGlobal(domain.getStockMinimoGlobal());
        e.setRegimenTributario(domain.getRegimenTributario());
        e.setIvaPorDefecto(domain.getIvaPorDefecto());
        e.setPrecioConIvaIncluido(domain.isPrecioConIvaIncluido());
        e.setPorcentajeGananciaGlobal(domain.getPorcentajeGananciaGlobal());
        e.setPrefijoComprobante(domain.getPrefijoComprobante());
        e.setNumeroInicialComprobante(domain.getNumeroInicialComprobante());
        e.setFechaInventarioAnual(domain.getFechaInventarioAnual());
        if (domain.getPropietarioId() != null) {
            UsuarioEntity propietario = new UsuarioEntity();
            propietario.setId(domain.getPropietarioId());
            e.setPropietario(propietario);
        }
        return e;
    }

    public ConfiguracionTienda toDomain(ConfiguracionTiendaEntity e) {
        return ConfiguracionTienda.builder()
                .id(e.getId())
                .tipoPersona(e.getTipoPersona())
                .nombreTienda(e.getNombreTienda())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .cedula(e.getCedula())
                .razonSocial(e.getRazonSocial())
                .nit(e.getNit())
                .representanteLegalNombre(e.getRepresentanteLegalNombre())
                .representanteLegalApellido(e.getRepresentanteLegalApellido())
                .direccion(e.getDireccion())
                .rutaLogo(e.getRutaLogo())
                .telefono(e.getTelefono())
                .correo(e.getCorreo())
                .stockMinimoGlobal(e.getStockMinimoGlobal())
                .regimenTributario(e.getRegimenTributario())
                .ivaPorDefecto(e.getIvaPorDefecto())
                .precioConIvaIncluido(e.isPrecioConIvaIncluido())
                .porcentajeGananciaGlobal(e.getPorcentajeGananciaGlobal())
                .prefijoComprobante(e.getPrefijoComprobante())
                .numeroInicialComprobante(e.getNumeroInicialComprobante())
                .fechaInventarioAnual(e.getFechaInventarioAnual())
                .propietarioId(e.getPropietario() != null ? e.getPropietario().getId() : null)
                .build();
    }
}
