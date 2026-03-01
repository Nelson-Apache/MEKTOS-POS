package com.nap.pos.ui.controller;

import com.nap.pos.Launcher;
import com.nap.pos.application.service.ConfiguracionService;
import com.nap.pos.application.service.UsuarioService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.Rol;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;
    private final ConfiguracionService configuracionService;

    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblNombreTienda;
    @FXML private Label         lblMensaje;

    @FXML
    public void initialize() {
        // Muestra el nombre de la tienda configurada
        try {
            String nombre = configuracionService.obtener().getNombreTienda();
            lblNombreTienda.setText(nombre);
        } catch (Exception ignored) {
            // Si no hay configuración aún, deja el texto por defecto
        }

        // Primera ejecución: crea usuario admin inicial si la BD está vacía
        crearAdminInicialSiNecesario();
    }

    @FXML
    public void onLogin() {
        String username = txtUsuario.getText().trim();
        String password = txtPassword.getText();

        if (username.isBlank() || password.isBlank()) {
            mostrarError("Ingresa usuario y contraseña.");
            return;
        }

        limpiarMensaje();

        try {
            Usuario usuario = usuarioService.login(username, password);
            abrirVentanaPrincipal(usuario);
        } catch (BusinessException e) {
            mostrarError(e.getMessage());
            txtPassword.clear();
            txtPassword.requestFocus();
        } catch (Exception e) {
            mostrarError("Error al iniciar sesión. Intenta de nuevo.");
        }
    }

    // ── Helpers privados ──────────────────────────────────────────

    private void abrirVentanaPrincipal(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/main_window.fxml"));
            loader.setControllerFactory(Launcher.getContext()::getBean);

            Scene scene = new Scene(loader.load(), 1280, 800);

            MainWindowController mainCtrl = loader.getController();
            mainCtrl.inicializar(usuario);

            Stage loginStage = (Stage) txtUsuario.getScene().getWindow();
            loginStage.close();

            Stage mainStage = new Stage();
            mainStage.setTitle(
                    lblNombreTienda.getText() + "  —  NAP POS");
            mainStage.setMinWidth(1100);
            mainStage.setMinHeight(700);
            mainStage.setScene(scene);
            mainStage.centerOnScreen();
            mainStage.show();

        } catch (Exception e) {
            mostrarError("No se pudo abrir la aplicación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Si la base de datos no tiene ningún usuario, crea un administrador
     * inicial con credenciales temporales y avisa al operador que las cambie.
     * Esto solo ocurre después del wizard de configuración inicial.
     */
    private void crearAdminInicialSiNecesario() {
        try {
            if (usuarioService.findAll().isEmpty()) {
                usuarioService.crear("admin", "admin123", Rol.ADMIN);
                mostrarInfo(
                    "Usuario inicial creado: admin / admin123\n" +
                    "Ingresa y cámbiala desde Configuración → Usuarios.");
                txtUsuario.setText("admin");
                txtPassword.requestFocus();
            }
        } catch (Exception ignored) {
            // Si falla la creación, el login lo detectará
        }
    }

    private void mostrarError(String msg) {
        lblMensaje.setText(msg);
        lblMensaje.getStyleClass().setAll("login-error");
    }

    private void mostrarInfo(String msg) {
        lblMensaje.setText(msg);
        lblMensaje.getStyleClass().setAll("login-info");
    }

    private void limpiarMensaje() {
        lblMensaje.setText("");
    }
}
