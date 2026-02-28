package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.Rol;
import com.nap.pos.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Verifica credenciales para el login.
     * Usa BCrypt para comparar la contraseña ingresada con el hash almacenado.
     * El mensaje de error es deliberadamente genérico para no revelar si el usuario existe.
     */
    public Usuario login(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Usuario o contraseña incorrectos."));
        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new BusinessException("Usuario o contraseña incorrectos.");
        }
        return usuario;
    }

    /**
     * Crea un nuevo usuario. La contraseña se hashea con BCrypt antes de persistir.
     * Solo el ADMIN puede invocar este servicio (verificar en el controlador).
     */
    @Transactional
    public Usuario crear(String username, String rawPassword, Rol rol) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("Ya existe un usuario con el nombre '" + username + "'.");
        }
        Usuario usuario = Usuario.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .rol(rol)
                .activo(true)
                .build();
        return usuarioRepository.save(usuario);
    }

    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario con ID " + id + " no encontrado."));
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }
}
