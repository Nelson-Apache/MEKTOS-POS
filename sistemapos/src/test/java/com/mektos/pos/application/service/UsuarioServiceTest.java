package com.mektos.pos.application.service;

import com.mektos.pos.domain.exception.BusinessException;
import com.mektos.pos.domain.model.Usuario;
import com.mektos.pos.domain.model.enums.Rol;
import com.mektos.pos.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    // --- login ---

    @Test
    void login_credencialesValidas_retornaUsuario() {
        Usuario usuario = Usuario.builder()
                .id(1L).username("admin").passwordHash("$2a$hash")
                .rol(Rol.ADMIN).activo(true).build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("pass123", "$2a$hash")).thenReturn(true);

        Usuario resultado = usuarioService.login("admin", "pass123");

        assertThat(resultado.getUsername()).isEqualTo("admin");
        assertThat(resultado.getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void login_usuarioNoExiste_lanzaBusinessException() {
        when(usuarioRepository.findByUsername("noexiste")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> usuarioService.login("noexiste", "pass"));
    }

    @Test
    void login_contrasenaIncorrecta_lanzaBusinessException() {
        Usuario usuario = Usuario.builder()
                .id(1L).username("admin").passwordHash("$2a$hash")
                .rol(Rol.ADMIN).activo(true).build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala_clave", "$2a$hash")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> usuarioService.login("admin", "mala_clave"));
    }

    // --- crear ---

    @Test
    void crear_usernameNuevo_guardaConPasswordHasheado() {
        when(usuarioRepository.findByUsername("cajero1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("$2a$10$hashed");
        Usuario guardado = Usuario.builder()
                .id(2L).username("cajero1").passwordHash("$2a$10$hashed")
                .rol(Rol.CAJERO).activo(true).build();
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);

        Usuario resultado = usuarioService.crear("cajero1", "pass", Rol.CAJERO);

        assertThat(resultado.getPasswordHash()).isEqualTo("$2a$10$hashed");
        verify(passwordEncoder).encode("pass");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void crear_usernameDuplicado_lanzaBusinessException() {
        Usuario existente = Usuario.builder()
                .id(1L).username("admin").passwordHash("hash")
                .rol(Rol.ADMIN).activo(true).build();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class,
                () -> usuarioService.crear("admin", "pass", Rol.ADMIN));
        verify(usuarioRepository, never()).save(any());
    }
}
