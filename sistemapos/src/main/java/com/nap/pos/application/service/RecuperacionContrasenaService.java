package com.nap.pos.application.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona los códigos de verificación de un solo uso para recuperación
 * de contraseña. Los códigos viven en memoria (no en BD); se invalidan
 * automáticamente al verificarse correctamente o al expirar.
 *
 * <p>Al ser una app de escritorio con un único proceso, no se necesita
 * Redis ni almacenamiento persistente — el reinicio limpia todo.</p>
 */
@Service
public class RecuperacionContrasenaService {

    private static final int EXPIRACION_MINUTOS = 15;
    private static final Random RANDOM = new Random();

    private record Entrada(String codigo, LocalDateTime expira) {}

    private final Map<String, Entrada> codigos = new ConcurrentHashMap<>();

    /**
     * Genera un código de 6 dígitos para el usuario y lo registra con
     * su tiempo de expiración. Reemplaza cualquier código previo pendiente.
     *
     * @param username nombre de usuario (case-insensitive)
     * @return el código generado (para enviarlo por correo)
     */
    public String generarCodigo(String username) {
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        codigos.put(username.toLowerCase(),
                new Entrada(codigo, LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS)));
        return codigo;
    }

    /**
     * Verifica si el código es correcto y no ha expirado.
     * No consume el código — debe llamarse {@link #invalidar} al confirmar
     * el cambio de contraseña.
     */
    public boolean verificar(String username, String codigo) {
        Entrada entrada = codigos.get(username.toLowerCase());
        if (entrada == null) return false;
        if (LocalDateTime.now().isAfter(entrada.expira())) {
            codigos.remove(username.toLowerCase());
            return false;
        }
        return entrada.codigo().equals(codigo);
    }

    /** Elimina el código del usuario (post-cambio o al cancelar). */
    public void invalidar(String username) {
        codigos.remove(username.toLowerCase());
    }
}
