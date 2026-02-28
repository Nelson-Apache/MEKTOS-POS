package com.nap.pos.domain.model;

import com.nap.pos.domain.model.enums.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
