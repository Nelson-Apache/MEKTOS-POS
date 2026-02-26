package com.mektos.pos.domain.repository;

import com.mektos.pos.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Usuario.
 * La implementación concreta vive en infrastructure/persistence.
 */
public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(Long id);

    // Usado en el login — busca por nombre de usuario
    Optional<Usuario> findByUsername(String username);

    List<Usuario> findAll();
}
