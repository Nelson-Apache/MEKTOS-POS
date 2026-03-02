package com.nap.pos.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envía correos transaccionales usando el relay SMTP configurado en
 * ~/.nappos/mail.properties (credenciales) + application.properties (host/puerto).
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    /**
     * Envía el código de recuperación de contraseña al correo de la tienda.
     *
     * @param destinatario correo electrónico configurado en ConfiguracionTienda
     * @param username     nombre del usuario que solicita el reset
     * @param codigo       código de 6 dígitos generado por RecuperacionContrasenaService
     * @throws org.springframework.mail.MailException si falla el envío (sin conexión, auth, etc.)
     */
    public void enviarCodigoRecuperacion(String destinatario, String username,
                                         String codigo, String nombreTienda) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(destinatario);
        mensaje.setTo(destinatario);
        mensaje.setSubject(nombreTienda + " — Código de recuperación de contraseña");
        mensaje.setText(
            "Hola,\n\n" +
            "Se solicitó recuperar la contraseña del usuario \"" + username +
            "\" en " + nombreTienda + ".\n\n" +
            "Tu código de verificación es:\n\n" +
            "        " + codigo + "\n\n" +
            "Este código es válido por 15 minutos.\n\n" +
            "Si no solicitaste este cambio, ignora este mensaje.\n\n" +
            "— " + nombreTienda
        );
        mailSender.send(mensaje);
    }

    /** Devuelve true si las credenciales SMTP están configuradas. */
    public boolean estaConfigurado() {
        return remitente != null && !remitente.isBlank();
    }
}
