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
        return crear(username, rawPassword, rol, null, null, null);
    }

    /**
     * Crea un usuario con nombre y apellido explícitos (usado desde el wizard
     * para que el admin quede identificado con su nombre real desde el inicio).
     */
    @Transactional
    public Usuario crear(String username, String rawPassword, Rol rol,
                         String nombre, String apellido) {
        return crear(username, rawPassword, rol, nombre, apellido, null);
    }

    /**
     * Crea un usuario con todos los campos. creadoPorId identifica al admin que
     * registró la cuenta; null cuando el propio sistema crea el primer usuario (wizard).
     */
    @Transactional
    public Usuario crear(String username, String rawPassword, Rol rol,
                         String nombre, String apellido, Long creadoPorId) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("Ya existe un usuario con el nombre '" + username + "'.");
        }
        Usuario usuario = Usuario.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .rol(rol)
                .activo(true)
                .nombre(nombre)
                .apellido(apellido)
                .creadoPorId(creadoPorId)
                .build();
        return usuarioRepository.save(usuario);
    }

    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario con ID " + id + " no encontrado."));
    }

    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado."));
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    /**
     * Reemplaza la contraseña de un usuario por una nueva (post-verificación
     * de código de recuperación). No requiere la contraseña actual.
     */
    @Transactional
    public void actualizarPassword(String username, String newRawPassword) {
        Usuario existente = buscarPorUsername(username);
        Usuario actualizado = Usuario.builder()
                .id(existente.getId())
                .username(existente.getUsername())
                .passwordHash(passwordEncoder.encode(newRawPassword))
                .rol(existente.getRol())
                .activo(existente.isActivo())
                .nombre(existente.getNombre())
                .apellido(existente.getApellido())
                .creadoPorId(existente.getCreadoPorId())
                .build();
        usuarioRepository.save(actualizado);
    }

    /** Actualiza nombre, apellido y rol de un usuario existente. */
    @Transactional
    public Usuario actualizarUsuario(Long id, String nombre, String apellido, Rol rol) {
        Usuario existente = findById(id);
        Usuario actualizado = Usuario.builder()
                .id(existente.getId())
                .username(existente.getUsername())
                .passwordHash(existente.getPasswordHash())
                .rol(rol)
                .activo(existente.isActivo())
                .nombre(nombre)
                .apellido(apellido)
                .creadoPorId(existente.getCreadoPorId())
                .build();
        return usuarioRepository.save(actualizado);
    }

    /** Restablece la contraseña de cualquier usuario (uso exclusivo del admin). */
    @Transactional
    public void resetPasswordById(Long id, String newRawPassword) {
        Usuario existente = findById(id);
        Usuario actualizado = Usuario.builder()
                .id(existente.getId())
                .username(existente.getUsername())
                .passwordHash(passwordEncoder.encode(newRawPassword))
                .rol(existente.getRol())
                .activo(existente.isActivo())
                .nombre(existente.getNombre())
                .apellido(existente.getApellido())
                .creadoPorId(existente.getCreadoPorId())
                .build();
        usuarioRepository.save(actualizado);
    }

    /**
     * Alterna el estado activo/inactivo de un usuario.
     * Impide desactivar la propia cuenta o al último admin activo.
     */
    @Transactional
    public Usuario toggleActivo(Long id, Long requestingUserId) {
        Usuario existente = findById(id);
        if (existente.getId().equals(requestingUserId)) {
            throw new BusinessException("No puedes desactivar tu propia cuenta.");
        }
        if (existente.isActivo() && existente.esAdmin()) {
            long otrosAdminsActivos = usuarioRepository.findAll().stream()
                    .filter(u -> u.esAdmin() && u.isActivo() && !u.getId().equals(id))
                    .count();
            if (otrosAdminsActivos == 0) {
                throw new BusinessException("No puedes desactivar al único administrador activo.");
            }
        }
        Usuario actualizado = Usuario.builder()
                .id(existente.getId())
                .username(existente.getUsername())
                .passwordHash(existente.getPasswordHash())
                .rol(existente.getRol())
                .activo(!existente.isActivo())
                .nombre(existente.getNombre())
                .apellido(existente.getApellido())
                .creadoPorId(existente.getCreadoPorId())
                .build();
        return usuarioRepository.save(actualizado);
    }
}
