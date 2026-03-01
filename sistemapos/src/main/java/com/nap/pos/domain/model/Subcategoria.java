package com.nap.pos.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subcategoria {

    private Long id;
    private String nombre;
    private Categoria categoria;
    private boolean activo;

    public void activar()    { this.activo = true;  }
    public void desactivar() { this.activo = false; }
}
