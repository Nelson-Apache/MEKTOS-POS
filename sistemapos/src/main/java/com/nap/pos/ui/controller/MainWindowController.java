package com.nap.pos.ui.controller;

import com.nap.pos.Launcher;
import com.nap.pos.application.service.CajaService;
import com.nap.pos.application.service.ClienteService;
import com.nap.pos.application.service.ConfiguracionService;
import com.nap.pos.application.service.NotificacionService;
import com.nap.pos.application.service.ProductoService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.model.Notificacion;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.Rol;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MainWindowController {

    private final ConfiguracionService configuracionService;
    private final CajaService          cajaService;
    private final NotificacionService  notificacionService;
    private final ProductoService      productoService;
    private final ClienteService       clienteService;

    // ── Sidebar ───────────────────────────────────────────────────
    @FXML private VBox   sidebar;
    @FXML private VBox   brandArea;
    @FXML private Label  lblAppName;
    @FXML private Label  lblNombreTienda;
    @FXML private Region sepBrand;
    @FXML private Region sepBottom;
    @FXML private Label  lblGrupoPrincipal;
    @FXML private Label  lblGrupoAnalisis;
    @FXML private VBox   userArea;
    @FXML private Label  lblUsername;
    @FXML private Label  lblRol;
    @FXML private Button btnCerrarSesion;

    @FXML private Button btnDashboard;
    @FXML private Button btnVentas;
    @FXML private Button btnInventario;
    @FXML private Button btnCompras;
    @FXML private Button btnClientes;
    @FXML private Button btnReportes;
    @FXML private Button btnConfiguracion;

    // ── Topbar ────────────────────────────────────────────────────
    @FXML private Button btnToggleSidebar;
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
    private boolean sidebarCollapsed = false;

    private static final String[] NAV_TEXTS = {
        "Resumen", "Punto de Venta", "Inventario",
        "Compras", "Clientes", "Reportes", "Configuración"
    };

    /**
     * Llamado por LoginController inmediatamente después de cargar el FXML,
     * antes de mostrar la ventana.
     */
    public void inicializar(Usuario usuario) {
        this.usuarioActual = usuario;
        configurarDatosUsuario();
        configurarAccesoSegunRol();
        actualizarEstadoCaja();
        navDashboard();
    }

    // ── Navegación ────────────────────────────────────────────────

    @FXML
    public void navDashboard() {
        activarNav(btnDashboard, "Resumen");
        mostrarDashboard();
    }

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

    // ── Sidebar toggle ────────────────────────────────────────────

    @FXML
    public void onToggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        if (sidebarCollapsed) {
            sidebar.setPrefWidth(60);
            sidebar.setMaxWidth(60);

            lblNombreTienda.setVisible(false);   lblNombreTienda.setManaged(false);
            lblGrupoPrincipal.setVisible(false); lblGrupoPrincipal.setManaged(false);
            lblGrupoAnalisis.setVisible(false);  lblGrupoAnalisis.setManaged(false);
            sepBrand.setVisible(false);          sepBrand.setManaged(false);
            lblUsername.setVisible(false);       lblUsername.setManaged(false);
            lblRol.setVisible(false);            lblRol.setManaged(false);
            btnCerrarSesion.setVisible(false);   btnCerrarSesion.setManaged(false);

            lblAppName.setText("N");
            brandArea.setAlignment(Pos.CENTER);
            brandArea.setPadding(new Insets(14, 0, 14, 0));

            navButtons().forEach(b -> {
                b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                b.setAlignment(Pos.CENTER);
            });

        } else {
            sidebar.setPrefWidth(220);
            sidebar.setMaxWidth(Double.MAX_VALUE);

            lblNombreTienda.setVisible(true);   lblNombreTienda.setManaged(true);
            lblGrupoPrincipal.setVisible(true); lblGrupoPrincipal.setManaged(true);
            lblGrupoAnalisis.setVisible(true);  lblGrupoAnalisis.setManaged(true);
            sepBrand.setVisible(true);          sepBrand.setManaged(true);
            lblUsername.setVisible(true);       lblUsername.setManaged(true);
            lblRol.setVisible(true);            lblRol.setManaged(true);
            btnCerrarSesion.setVisible(true);   btnCerrarSesion.setManaged(true);

            lblAppName.setText("NAP POS");
            brandArea.setAlignment(Pos.CENTER_LEFT);
            brandArea.setPadding(new Insets(24, 20, 18, 20));

            Button[] btns = { btnDashboard, btnVentas, btnInventario,
                              btnCompras, btnClientes, btnReportes, btnConfiguracion };
            for (int i = 0; i < btns.length; i++) {
                btns[i].setContentDisplay(ContentDisplay.LEFT);
                btns[i].setAlignment(Pos.CENTER_LEFT);
                btns[i].setText(NAV_TEXTS[i]);
            }
        }
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
            cajaService.getCajaAbierta();
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

    private List<Button> navButtons() {
        return List.of(btnDashboard, btnVentas, btnInventario,
                       btnCompras, btnClientes, btnReportes, btnConfiguracion);
    }

    // ── Dashboard ─────────────────────────────────────────────────

    private void mostrarDashboard() {
        int totalProductos = 0;
        int sinStock       = 0;
        int totalClientes  = 0;
        boolean cajaAbierta = false;
        List<Notificacion> notifs = List.of();

        try { totalProductos = productoService.findAllActivos().size(); } catch (Exception ignored) {}
        try {
            sinStock = (int) productoService.findAllActivos().stream()
                    .filter(p -> p.getStock() == 0).count();
        } catch (Exception ignored) {}
        try { totalClientes = clienteService.findAll().size(); } catch (Exception ignored) {}
        try { cajaService.getCajaAbierta(); cajaAbierta = true; } catch (Exception ignored) {}
        try { notifs = notificacionService.getNotificaciones(); } catch (Exception ignored) {}

        VBox root = new VBox(28);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("dashboard-root");

        // Saludo
        String nombre = usuarioActual.getUsername();
        Label lblBienvenido = new Label("Bienvenido, " + nombre + "!");
        lblBienvenido.getStyleClass().add("dashboard-welcome");

        // Fila de stat cards
        HBox cardsRow = new HBox(16);
        cardsRow.setAlignment(Pos.TOP_LEFT);

        VBox cardProductos = crearStatCard("Productos activos",
                String.valueOf(totalProductos), "fas-boxes", "#3B82F6");
        VBox cardSinStock  = crearStatCard("Sin stock",
                String.valueOf(sinStock), "fas-exclamation-triangle",
                sinStock > 0 ? "#DC2626" : "#15803D");
        VBox cardClientes  = crearStatCard("Clientes",
                String.valueOf(totalClientes), "fas-users", "#3B82F6");
        VBox cardCaja      = crearStatCard("Caja",
                cajaAbierta ? "Abierta" : "Cerrada",
                cajaAbierta ? "fas-lock-open" : "fas-lock",
                cajaAbierta ? "#15803D" : "#D97706");

        for (VBox c : List.of(cardProductos, cardSinStock, cardClientes, cardCaja)) {
            HBox.setHgrow(c, Priority.ALWAYS);
            cardsRow.getChildren().add(c);
        }

        // Notificaciones
        VBox notifSection = new VBox(10);
        Label lblNotifTitle = new Label("Alertas");
        lblNotifTitle.getStyleClass().add("dashboard-section-title");
        notifSection.getChildren().add(lblNotifTitle);

        if (notifs.isEmpty()) {
            Label lblVacio = new Label("Sin alertas pendientes.");
            lblVacio.getStyleClass().add("dashboard-empty");
            notifSection.getChildren().add(lblVacio);
        } else {
            for (Notificacion n : notifs) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("dashboard-notif-row");

                FontIcon ico = new FontIcon("fas-circle");
                ico.setIconSize(8);
                ico.getStyleClass().add("icon-accent");

                Label lbl = new Label(n.titulo() + ": " + n.mensaje());
                lbl.getStyleClass().add("dashboard-notif-text");
                lbl.setWrapText(true);

                row.getChildren().addAll(ico, lbl);
                notifSection.getChildren().add(row);
            }
        }

        root.getChildren().addAll(lblBienvenido, cardsRow, notifSection);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dashboard-scroll");

        contenido.getChildren().setAll(scroll);
    }

    private VBox crearStatCard(String titulo, String valor, String icono, String iconColor) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("card", "dashboard-stat-card");
        card.setPadding(new Insets(20));

        FontIcon icon = new FontIcon(icono);
        icon.setIconSize(20);
        icon.setStyle("-fx-icon-color: " + iconColor + ";");

        Label lValor  = new Label(valor);
        lValor.getStyleClass().add("dashboard-stat-value");

        Label lTitulo = new Label(titulo);
        lTitulo.getStyleClass().add("dashboard-stat-label");

        card.getChildren().addAll(icon, lValor, lTitulo);
        return card;
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
