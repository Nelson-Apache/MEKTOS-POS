package com.mektos.pos.infrastructure.persistence.repository.impl;

import com.mektos.pos.domain.model.Usuario;
import com.mektos.pos.domain.repository.UsuarioRepository;
import com.mektos.pos.infrastructure.persistence.mapper.UsuarioMapper;
import com.mektos.pos.infrastructure.persistence.repository.jpa.JpaUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta de UsuarioRepository.
 * Traduce entre el modelo de dominio (Usuario) y la entidad JPA (UsuarioEntity)
 * usando el mapper y delegando la persistencia al JpaUsuarioRepository.
 */
@Repository
@RequiredArgsConstructor
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final JpaUsuarioRepository jpaUsuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Override
    public Usuario save(Usuario usuario) {
        return usuarioMapper.toDomain(
                jpaUsuarioRepository.save(usuarioMapper.toEntity(usuario))
        );
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return jpaUsuarioRepository.findById(id)
                .map(usuarioMapper::toDomain);
    }

    // Usado en el login para buscar el usuario por nombre de acceso
    @Override
    public Optional<Usuario> findByUsername(String username) {
        return jpaUsuarioRepository.findByUsername(username)
                .map(usuarioMapper::toDomain);
    }

    @Override
    public List<Usuario> findAll() {
        return jpaUsuarioRepository.findAll().stream()
                .map(usuarioMapper::toDomain)
                .toList();
    }
}
