package com.nap.pos.ui.controller;

import com.nap.pos.application.service.ConfiguracionService;
import com.nap.pos.application.service.EmailService;
import com.nap.pos.application.service.RecuperacionContrasenaService;
import com.nap.pos.application.service.UsuarioService;
import com.nap.pos.domain.exception.BusinessException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Controla el flujo de 4 pasos para recuperar contraseña vía correo:
 * <ol>
 *   <li>El usuario ingresa su nombre de usuario.</li>
 *   <li>Se envía un código de 6 dígitos al correo de la tienda.</li>
 *   <li>El usuario verifica el código y establece una nueva contraseña.</li>
 *   <li>Pantalla de éxito.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class RecuperarContrasenaController {

    private final UsuarioService                usuarioService;
    private final ConfiguracionService          configuracionService;
    private final EmailService                  emailService;
    private final RecuperacionContrasenaService recuperacionService;

    // Paso 1
    @FXML private VBox      paso1;
    @FXML private TextField txtUsernameRec;
    @FXML private Label     lblErrorP1;

    // Paso 2
    @FXML private VBox      paso2;
    @FXML private TextField txtCodigo;
    @FXML private Label     lblInfoCodigo;
    @FXML private Label     lblErrorP2;

    // Paso 3
    @FXML private VBox          paso3;
    @FXML private PasswordField txtNuevaPassword;
    @FXML private TextField     txtNuevaPasswordVisible;
    @FXML private Button        btnToggleNueva;
    @FXML private PasswordField txtConfirmarPassword;
    @FXML private TextField     txtConfirmarPasswordVisible;
    @FXML private Button        btnToggleConfirmar;
    @FXML private Label         lblErrorP3;

    // Paso 4 (éxito)
    @FXML private VBox paso4;

    /** Username activo en el flujo. */
    private String usernameActual;

    @FXML
    public void initialize() {
        txtCodigo.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("[0-9]{0,6}") ? change : null));

        // Sincroniza texto entre PasswordField y TextField para el toggle
        txtNuevaPasswordVisible.textProperty()
                .bindBidirectional(txtNuevaPassword.textProperty());
        txtConfirmarPasswordVisible.textProperty()
                .bindBidirectional(txtConfirmarPassword.textProperty());
    }

    // ── Paso 1: verificar usuario y enviar código ────────────────

    @FXML
    public void onEnviarCodigo() {
        limpiarError(lblErrorP1);

        String username = txtUsernameRec.getText().trim();
        if (username.isBlank()) {
            mostrarError(lblErrorP1, "Ingresa tu nombre de usuario.");
            return;
        }

        try {
            usuarioService.buscarPorUsername(username);
        } catch (BusinessException e) {
            mostrarError(lblErrorP1, "Usuario no encontrado.");
            return;
        }

        String correoTienda;
        String nombreTienda;
        try {
            var config   = configuracionService.obtener();
            correoTienda = config.getCorreo();
            nombreTienda = config.getNombreTienda();
        } catch (Exception e) {
            mostrarError(lblErrorP1, "No se pudo leer la configuración.");
            return;
        }

        if (correoTienda == null || correoTienda.isBlank()) {
            mostrarError(lblErrorP1,
                "No hay correo configurado en la tienda.\n" +
                "El administrador debe configurarlo en Ajustes → Tienda.");
            return;
        }

        if (!emailService.estaConfigurado()) {
            mostrarError(lblErrorP1,
                "El servicio de correo no está configurado.\n" +
                "Completa ~/.nappos/mail.properties con las credenciales SMTP.");
            return;
        }

        try {
            String codigo = recuperacionService.generarCodigo(username);
            emailService.enviarCodigoRecuperacion(correoTienda, username, codigo, nombreTienda);
            usernameActual = username;
            lblInfoCodigo.setText(
                "Código enviado a " + enmascararCorreo(correoTienda) +
                ". Válido por 15 minutos.\n" +
                "Pide el código al administrador si no tienes acceso al correo.");
            mostrarPaso(2);
        } catch (org.springframework.mail.MailAuthenticationException e) {
            mostrarError(lblErrorP1,
                "Credenciales SMTP inválidas.\n" +
                "Revisa spring.mail.username y spring.mail.password en:\n" +
                System.getProperty("user.home") + "\\.nappos\\mail.properties");
        } catch (Exception e) {
            String causa = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            mostrarError(lblErrorP1, "Error al enviar el correo:\n" + causa);
        }
    }

    // ── Paso 2: verificar código ─────────────────────────────────

    @FXML
    public void onVerificarCodigo() {
        limpiarError(lblErrorP2);

        String codigo = txtCodigo.getText().trim();
        if (codigo.length() != 6) {
            mostrarError(lblErrorP2, "El código debe tener 6 dígitos.");
            return;
        }

        if (!recuperacionService.verificar(usernameActual, codigo)) {
            mostrarError(lblErrorP2, "Código incorrecto o expirado.");
            return;
        }

        mostrarPaso(3);
    }

    @FXML
    public void onReenviarCodigo() {
        txtUsernameRec.setText(usernameActual != null ? usernameActual : "");
        limpiarError(lblErrorP1);
        mostrarPaso(1);
    }

    // ── Paso 3: nueva contraseña ─────────────────────────────────

    @FXML
    public void onCambiarPassword() {
        limpiarError(lblErrorP3);

        String nueva    = txtNuevaPassword.getText();
        String confirma = txtConfirmarPassword.getText();

        if (nueva.isBlank()) {
            mostrarError(lblErrorP3, "Ingresa una contraseña.");
            return;
        }
        if (nueva.length() < 6) {
            mostrarError(lblErrorP3, "La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!nueva.equals(confirma)) {
            mostrarError(lblErrorP3, "Las contraseñas no coinciden.");
            return;
        }

        try {
            usuarioService.actualizarPassword(usernameActual, nueva);
            recuperacionService.invalidar(usernameActual);
            mostrarPaso(4);
        } catch (Exception e) {
            mostrarError(lblErrorP3, "Error al guardar la contraseña. Intenta de nuevo.");
        }
    }

    // ── Toggles de visibilidad ───────────────────────────────────

    @FXML
    public void onToggleNuevaPassword() {
        boolean mostrar = !txtNuevaPasswordVisible.isVisible();
        txtNuevaPassword.setVisible(!mostrar);
        txtNuevaPassword.setManaged(!mostrar);
        txtNuevaPasswordVisible.setVisible(mostrar);
        txtNuevaPasswordVisible.setManaged(mostrar);
        btnToggleNueva.setText(mostrar ? "🔒" : "👁");
    }

    @FXML
    public void onToggleConfirmarPassword() {
        boolean mostrar = !txtConfirmarPasswordVisible.isVisible();
        txtConfirmarPassword.setVisible(!mostrar);
        txtConfirmarPassword.setManaged(!mostrar);
        txtConfirmarPasswordVisible.setVisible(mostrar);
        txtConfirmarPasswordVisible.setManaged(mostrar);
        btnToggleConfirmar.setText(mostrar ? "🔒" : "👁");
    }

    // ── Navegación ───────────────────────────────────────────────

    @FXML
    public void onVolver() {
        cerrarVentana();
    }

    private void mostrarPaso(int paso) {
        paso1.setVisible(paso == 1); paso1.setManaged(paso == 1);
        paso2.setVisible(paso == 2); paso2.setManaged(paso == 2);
        paso3.setVisible(paso == 3); paso3.setManaged(paso == 3);
        paso4.setVisible(paso == 4); paso4.setManaged(paso == 4);
    }

    private void cerrarVentana() {
        Stage stage = (Stage) paso1.getScene().getWindow();
        stage.close();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void mostrarError(Label lbl, String msg) { lbl.setText(msg); }
    private void limpiarError(Label lbl)              { lbl.setText(""); }

    private String enmascararCorreo(String correo) {
        int at = correo.indexOf('@');
        if (at <= 1) return correo;
        String local  = correo.substring(0, at);
        String domain = correo.substring(at);
        if (local.length() <= 2) return local.charAt(0) + "***" + domain;
        int visible = Math.min(local.length() - 2, 4);
        return local.charAt(0) + "*".repeat(visible) + local.charAt(local.length() - 1) + domain;
    }
}
