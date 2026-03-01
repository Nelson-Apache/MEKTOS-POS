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
    private boolean activo;

    public void activar()    { this.activo = true;  }
    public void desactivar() { this.activo = false; }
}
