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
    private String nombre;
    private String apellido;
    /** ID del usuario que creó esta cuenta. Null para el admin inicial del wizard. */
    private Long creadoPorId;

    public boolean esAdmin() {
        return Rol.ADMIN.equals(this.rol);
    }

    /** Devuelve "Nombre Apellido" si están definidos, o el username como fallback. */
    public String getNombreCompleto() {
        if (nombre != null && !nombre.isBlank()) {
            return (apellido != null && !apellido.isBlank())
                    ? nombre + " " + apellido
                    : nombre;
        }
        return username;
    }
}
