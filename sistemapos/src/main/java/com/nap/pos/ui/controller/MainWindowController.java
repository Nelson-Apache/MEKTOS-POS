package com.nap.pos.ui.controller;

import com.nap.pos.Launcher;
import com.nap.pos.application.service.CajaService;
import com.nap.pos.application.service.ConfiguracionService;
import com.nap.pos.application.service.NotificacionService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.model.Notificacion;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.Rol;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MainWindowController {

    private final ConfiguracionService configuracionService;
    private final CajaService          cajaService;
    private final NotificacionService  notificacionService;

    // ── Sidebar ───────────────────────────────────────────────────
    @FXML private Label  lblNombreTienda;
    @FXML private Label  lblUsername;
    @FXML private Label  lblRol;

    @FXML private Button btnVentas;
    @FXML private Button btnInventario;
    @FXML private Button btnCompras;
    @FXML private Button btnClientes;
    @FXML private Button btnReportes;
    @FXML private Button btnConfiguracion;

    // ── Topbar ────────────────────────────────────────────────────
    @FXML private Label  lblSeccion;
    @FXML private HBox   hboxCajaBadge;
    @FXML private Label  lblCaja;
    @FXML private Button btnNotificaciones;
    @FXML private Label  lblUserChip;

    // ── Contenido central ─────────────────────────────────────────
    @FXML private StackPane contenido;

    // ── Estado ───────────────────────────────────────────────────
    private Usuario usuarioActual;
    private Button  navActivo;

    /**
     * Llamado por LoginController inmediatamente después de cargar el FXML,
     * antes de mostrar la ventana.
     */
    public void inicializar(Usuario usuario) {
        this.usuarioActual = usuario;
        configurarDatosUsuario();
        configurarAccesoSegunRol();
        actualizarEstadoCaja();
        navVentas(); // pantalla inicial por defecto
    }

    // ── Navegación ────────────────────────────────────────────────

    @FXML
    public void navVentas() {
        activarNav(btnVentas, "Punto de Venta");
        mostrarPlaceholder("Punto de Venta",
                "Escanea o busca un producto para comenzar la venta");
    }

    @FXML
    public void navInventario() {
        activarNav(btnInventario, "Inventario");
        mostrarPlaceholder("Inventario",
                "Gestión de productos, categorías y stock");
    }

    @FXML
    public void navCompras() {
        activarNav(btnCompras, "Compras");
        mostrarPlaceholder("Compras",
                "Registro de compras a proveedores");
    }

    @FXML
    public void navClientes() {
        activarNav(btnClientes, "Clientes");
        mostrarPlaceholder("Clientes",
                "Gestión de clientes y créditos");
    }

    @FXML
    public void navReportes() {
        activarNav(btnReportes, "Reportes");
        mostrarPlaceholder("Reportes",
                "Ventas, caja y análisis del negocio");
    }

    @FXML
    public void navConfiguracion() {
        activarNav(btnConfiguracion, "Configuración");
        mostrarPlaceholder("Configuración",
                "Usuarios, categorías, proveedores y ajustes");
    }

    // ── Topbar handlers ──────────────────────────────────────────

    @FXML
    public void onNotificaciones() {
        try {
            List<Notificacion> notifs = notificacionService.getNotificaciones();
            if (notifs.isEmpty()) {
                mostrarAlerta("Sin notificaciones",
                        "No hay alertas pendientes en este momento.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (Notificacion n : notifs) {
                    sb.append("• ").append(n.titulo()).append(": ").append(n.mensaje()).append("\n");
                }
                mostrarAlerta("Notificaciones (" + notifs.size() + ")", sb.toString().trim());
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron cargar las notificaciones.");
        }
    }

    @FXML
    public void onCerrarSesion() {
        try {
            Stage mainStage = (Stage) contenido.getScene().getWindow();
            mainStage.close();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(Launcher.getContext()::getBean);

            Scene scene = new Scene(loader.load(), 900, 620);

            Stage loginStage = new Stage();
            loginStage.setTitle("NAP POS — Iniciar sesión");
            loginStage.setScene(scene);
            loginStage.setResizable(false);
            loginStage.centerOnScreen();
            loginStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helpers privados ──────────────────────────────────────────

    private void configurarDatosUsuario() {
        lblUsername.setText(usuarioActual.getUsername());
        lblRol.setText(formatearRol(usuarioActual.getRol()));
        lblUserChip.setText(
                usuarioActual.getUsername().substring(0, 1).toUpperCase()
                + "  " + usuarioActual.getUsername());

        try {
            ConfiguracionTienda config = configuracionService.obtener();
            lblNombreTienda.setText(config.getNombreTienda());
        } catch (Exception ignored) {
            lblNombreTienda.setText("");
        }
    }

    private void configurarAccesoSegunRol() {
        boolean esAdmin = usuarioActual.esAdmin();
        btnConfiguracion.setVisible(esAdmin);
        btnConfiguracion.setManaged(esAdmin);
    }

    private void actualizarEstadoCaja() {
        try {
            cajaService.getCajaAbierta(); // lanza BusinessException si no hay caja abierta
            hboxCajaBadge.getStyleClass().setAll("caja-badge", "caja-abierta");
            lblCaja.getStyleClass().setAll("caja-badge-text-abierta");
            lblCaja.setText("Caja abierta");
        } catch (BusinessException e) {
            hboxCajaBadge.getStyleClass().setAll("caja-badge", "caja-cerrada");
            lblCaja.getStyleClass().setAll("caja-badge-text-cerrada");
            lblCaja.setText("Sin caja abierta");
        }
    }

    private void activarNav(Button btn, String titulo) {
        if (navActivo != null) {
            navActivo.getStyleClass().remove("nav-item-active");
        }
        navActivo = btn;
        btn.getStyleClass().add("nav-item-active");
        lblSeccion.setText(titulo);
    }

    private void mostrarPlaceholder(String titulo, String subtitulo) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("placeholder-container");

        Label icon = new Label("◫");
        icon.getStyleClass().add("placeholder-icon");

        Label title = new Label(titulo);
        title.getStyleClass().add("placeholder-title");

        Label sub = new Label(subtitulo);
        sub.getStyleClass().add("placeholder-sub");

        box.getChildren().addAll(icon, title, sub);
        contenido.getChildren().setAll(box);
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private String formatearRol(Rol rol) {
        return switch (rol) {
            case ADMIN  -> "Administrador";
            case CAJERO -> "Cajero";
        };
    }
}
