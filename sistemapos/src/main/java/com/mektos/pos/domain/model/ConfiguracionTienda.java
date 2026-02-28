package com.mektos.pos.domain.model;

import com.mektos.pos.domain.model.enums.TipoPersona;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionTienda {

    private Long id;                 // siempre 1 — fila única en BD
    private TipoPersona tipoPersona;
    private String nombreTienda;

    // Solo cuando tipoPersona = NATURAL
    private String nombre;
    private String apellido;
    private String cedula;

    // Solo cuando tipoPersona = JURIDICA
    private String razonSocial;
    private String nit;

    private String direccion;
    private String rutaLogo;         // null si no se configuró
}
