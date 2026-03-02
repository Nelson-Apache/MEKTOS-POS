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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;
    private final ConfiguracionService configuracionService;

    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private TextField     txtPasswordVisible;
    @FXML private Button        btnTogglePassword;
    @FXML private FontIcon      icoTogglePassword;
    @FXML private Label         lblNombreTienda;
    @FXML private Label         lblMensaje;
    @FXML private ImageView     imgLogo;
    @FXML private Region        logoSpace;

    @FXML
    public void initialize() {
        try {
            var config = configuracionService.obtener();

            // Nombre de la tienda
            lblNombreTienda.setText(config.getNombreTienda());

            // Logo: mostrar solo si el archivo existe
            String rutaLogo = config.getRutaLogo();
            if (rutaLogo != null && !rutaLogo.isBlank()) {
                File archivo = new File(rutaLogo);
                if (archivo.exists()) {
                    imgLogo.setImage(new Image(archivo.toURI().toString()));
                    imgLogo.setVisible(true);
                    imgLogo.setManaged(true);
                    logoSpace.setVisible(true);
                    logoSpace.setManaged(true);
                }
            }
        } catch (Exception ignored) {
            // Sin configuración aún: deja los valores por defecto del FXML
        }

        // Sincroniza texto entre PasswordField y TextField para el toggle
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());

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

    @FXML
    public void onTogglePassword() {
        boolean mostrar = !txtPasswordVisible.isVisible();
        txtPassword.setVisible(!mostrar);
        txtPassword.setManaged(!mostrar);
        txtPasswordVisible.setVisible(mostrar);
        txtPasswordVisible.setManaged(mostrar);
        icoTogglePassword.setIconLiteral(mostrar ? "fas-eye-slash" : "fas-eye");
    }

    @FXML
    public void onRecuperarPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/recuperar_contrasena.fxml"));
            loader.setControllerFactory(Launcher.getContext()::getBean);
            Scene scene = new Scene(loader.load(), 520, 500);
            Stage stage = new Stage();
            stage.setTitle("Recuperar contraseña — NAP POS");
            stage.setMinWidth(420);
            stage.setMinHeight(450);
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(txtUsuario.getScene().getWindow());
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            mostrarError("No se pudo abrir la ventana de recuperación.");
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
