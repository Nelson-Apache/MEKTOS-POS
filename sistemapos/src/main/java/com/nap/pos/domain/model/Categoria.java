package com.nap.pos.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    private Long id;
    private String nombre;
    /**
     * Literal de icono Ikonli (FontAwesome Solid).
     * Ejemplos: "fas-tshirt", "fas-apple-alt", "fas-mobile-alt", "fas-box"
     * Si es null o vacío, el chip de la pantalla de ventas solo muestra el texto.
     */
    private String icono;
    private boolean activo;

    public void activar()    { this.activo = true;  }
    public void desactivar() { this.activo = false; }
}
