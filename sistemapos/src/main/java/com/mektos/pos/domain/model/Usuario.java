package com.mektos.pos.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    private Long id;
    private String username;
    private String passwordHash;
    private Rol rol;
    private boolean activo;

    public boolean esAdmin() {
        return Rol.ADMIN.equals(this.rol);
    }
}
