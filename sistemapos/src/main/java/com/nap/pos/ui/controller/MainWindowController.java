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
import com.nap.pos.domain.model.enums.TipoNotificacion;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class MainWindowController {

    private final ConfiguracionService configuracionService;
    private final CajaService          cajaService;
    private final NotificacionService  notificacionService;
    private final DashboardController      dashboardController;
    private final VentasController         ventasController;
    private final InventarioController     inventarioController;
    private final ComprasController        comprasController;
    private final ClientesController       clientesController;
    private final ProveedoresController    proveedoresController;
    private final CajaController           cajaController;
    private final GastosController         gastosController;
    private final ReportesController       reportesController;
    private final ConfiguracionController  configuracionController;

    // ── Sidebar ───────────────────────────────────────────────────
    @FXML private VBox   sidebar;
    @FXML private VBox   brandArea;
    @FXML private Label  lblAppName;
    @FXML private Label  lblNombreTienda;
    @FXML private Region sepBrand;
    @FXML private Region sepBottom;
    @FXML private Label  lblGrupoPrincipal;
    @FXML private Label  lblGrupoAdmin;
    @FXML private VBox   userArea;
    @FXML private Label  lblUsername;
    @FXML private Label  lblRol;
    @FXML private Button btnCerrarSesion;

    @FXML private Button btnDashboard;
    @FXML private Button btnVentas;
    @FXML private Button btnInventario;
    @FXML private Button btnCompras;
    @FXML private Button btnClientes;
    @FXML private Button btnProveedores;
    @FXML private Button btnCaja;
    @FXML private Button btnGastos;
    @FXML private Button btnReportes;
    @FXML private Button btnConfiguracion;

    // ── Topbar ────────────────────────────────────────────────────
    @FXML private Button btnToggleSidebar;
    @FXML private Label  lblSeccion;
    @FXML private HBox   hboxCajaBadge;
    @FXML private Label  lblCaja;
    @FXML private Button btnNotificaciones;
    @FXML private Label  lblUserAvatar;
    @FXML private Label  lblUserChip;

    // ── Contenido central ─────────────────────────────────────────
    @FXML private StackPane contenido;

    // ── Estado ───────────────────────────────────────────────────
    private Usuario usuarioActual;
    private Button                  navActivo;
    private final Map<Button, Timeline> navAnimaciones = new HashMap<>();
    private Label                   lblBadgeNotificaciones;
    private Popup                   notifPopup;

    // Color de fondo del nav-item activo: #FFFFFF (píldora blanca sobre sidebar oscuro)
    private static final Color NAV_ACTIVE_COLOR   = Color.color(1.0, 1.0, 1.0, 1.0);
    private static final Color NAV_TRANSPARENT    = Color.color(1.0, 1.0, 1.0, 0.0);
    private boolean sidebarCollapsed = false;

    private static final String[] NAV_TEXTS = {
        "Dashboard", "Ventas", "Inventario",
        "Compras", "Clientes", "Proveedores",
        "Caja", "Gastos", "Reportes", "Configuración"
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
        configurarBadgeNotificaciones();
        navDashboard();
    }

    // ── Navegación ────────────────────────────────────────────────

    @FXML
    public void navDashboard() {
        activarNav(btnDashboard, "Dashboard");
        mostrarDashboard();
    }

    @FXML
    public void navVentas() {
        activarNav(btnVentas, "Ventas");
        contenido.getChildren().setAll(ventasController.buildView(usuarioActual));
    }

    @FXML
    public void navInventario() {
        activarNav(btnInventario, "Inventario");
        contenido.getChildren().setAll(inventarioController.buildView());
    }

    @FXML
    public void navCompras() {
        activarNav(btnCompras, "Compras");
        contenido.getChildren().setAll(comprasController.buildView(usuarioActual));
    }

    @FXML
    public void navClientes() {
        activarNav(btnClientes, "Clientes");
        contenido.getChildren().setAll(clientesController.buildView(usuarioActual));
    }

    @FXML
    public void navProveedores() {
        activarNav(btnProveedores, "Proveedores");
        contenido.getChildren().setAll(proveedoresController.buildView(usuarioActual));
    }

    @FXML
    public void navCaja() {
        activarNav(btnCaja, "Caja");
        contenido.getChildren().setAll(cajaController.buildView(usuarioActual, this::actualizarEstadoCaja));
    }

    @FXML
    public void navGastos() {
        activarNav(btnGastos, "Gastos");
        contenido.getChildren().setAll(gastosController.buildView(usuarioActual));
    }

    @FXML
    public void navReportes() {
        activarNav(btnReportes, "Reportes");
        contenido.getChildren().setAll(reportesController.buildView());
    }

    @FXML
    public void navConfiguracion() {
        activarNav(btnConfiguracion, "Configuración");
        contenido.getChildren().setAll(configuracionController.buildView(usuarioActual));
    }

    // ── Sidebar toggle ────────────────────────────────────────────

    @FXML
    public void onToggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        if (sidebarCollapsed) {
            sidebar.setMinWidth(60);
            sidebar.setPrefWidth(60);
            sidebar.setMaxWidth(60);

            lblNombreTienda.setVisible(false);   lblNombreTienda.setManaged(false);
            lblGrupoPrincipal.setVisible(false); lblGrupoPrincipal.setManaged(false);
            lblGrupoAdmin.setVisible(false);      lblGrupoAdmin.setManaged(false);
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
            sidebar.setMinWidth(220);
            sidebar.setPrefWidth(220);
            sidebar.setMaxWidth(Double.MAX_VALUE);

            lblNombreTienda.setVisible(true);   lblNombreTienda.setManaged(true);
            lblGrupoPrincipal.setVisible(true); lblGrupoPrincipal.setManaged(true);
            lblGrupoAdmin.setVisible(true);      lblGrupoAdmin.setManaged(true);
            sepBrand.setVisible(true);          sepBrand.setManaged(true);
            lblUsername.setVisible(true);       lblUsername.setManaged(true);
            lblRol.setVisible(true);            lblRol.setManaged(true);
            btnCerrarSesion.setVisible(true);   btnCerrarSesion.setManaged(true);

            lblAppName.setText("NAP POS");
            brandArea.setAlignment(Pos.CENTER_LEFT);
            brandArea.setPadding(new Insets(24, 20, 18, 20));

            Button[] btns = { btnDashboard, btnVentas, btnInventario,
                              btnCompras, btnClientes, btnProveedores,
                              btnCaja, btnGastos, btnReportes, btnConfiguracion };
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
        if (notifPopup != null && notifPopup.isShowing()) {
            notifPopup.hide();
            return;
        }
        try {
            actualizarBadge();
            List<Notificacion> notifs = notificacionService.getNotificaciones();
            notifPopup = construirPopupNotificaciones(notifs);
            Bounds b = btnNotificaciones.localToScreen(btnNotificaciones.getBoundsInLocal());
            notifPopup.show(btnNotificaciones.getScene().getWindow(),
                    b.getMaxX() - 320, b.getMaxY() + 6);
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
        String nombreCompleto = usuarioActual.getNombreCompleto();
        lblUsername.setText(nombreCompleto);
        lblRol.setText(formatearRol(usuarioActual.getRol()));
        lblUserAvatar.setText(nombreCompleto.substring(0, 1).toUpperCase());
        // Solo el primer nombre para mantener el chip compacto
        String primerNombre = nombreCompleto.split(" ")[0];
        lblUserChip.setText(primerNombre);

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
            lblCaja.setText("Abierta");
        } catch (BusinessException e) {
            hboxCajaBadge.getStyleClass().setAll("caja-badge", "caja-cerrada");
            lblCaja.getStyleClass().setAll("caja-badge-text-cerrada");
            lblCaja.setText("Cerrada");
        }
    }

    private void activarNav(Button entrante, String titulo) {
        if (navActivo == entrante) return;

        // ── Saliente: quitar clase, fade-out de la píldora blanca (160 ms) ──
        if (navActivo != null) {
            Button saliente = navActivo;
            saliente.getStyleClass().remove("nav-item-active");
            animarFondoNav(saliente, NAV_ACTIVE_COLOR, NAV_TRANSPARENT, 160,
                    () -> saliente.setStyle(""));
        }

        // ── Entrante: añadir clase, fade-in de la píldora blanca (220 ms) ───
        navActivo = entrante;
        entrante.getStyleClass().add("nav-item-active");
        animarFondoNav(entrante, NAV_TRANSPARENT, NAV_ACTIVE_COLOR, 220,
                () -> entrante.setStyle(""));

        lblSeccion.setText(titulo);
    }

    /**
     * Interpola el color de fondo del botón via inline-style.
     * Al terminar limpia el inline-style para ceder control al CSS.
     * Cancela cualquier animación previa sobre el mismo botón.
     */
    private void animarFondoNav(Button btn, Color desde, Color hasta,
                                 int duracionMs, Runnable alTerminar) {
        Timeline anterior = navAnimaciones.remove(btn);
        if (anterior != null) anterior.stop();

        SimpleObjectProperty<Color> colorProp = new SimpleObjectProperty<>(desde);
        colorProp.addListener((obs, o, n) -> {
            if (n == null) return;
            btn.setStyle(String.format(Locale.US,
                    "-fx-background-color: rgba(%d,%d,%d,%.4f);" +
                    "-fx-background-radius: 10;" +
                    "-fx-background-insets: 4 10;" +
                    "-fx-text-fill: #192030;",
                    (int) Math.round(n.getRed()   * 255),
                    (int) Math.round(n.getGreen() * 255),
                    (int) Math.round(n.getBlue()  * 255),
                    n.getOpacity()));
        });

        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,                new KeyValue(colorProp, desde)),
                new KeyFrame(Duration.millis(duracionMs), new KeyValue(colorProp, hasta,
                        Interpolator.EASE_BOTH))
        );
        tl.setOnFinished(e -> {
            navAnimaciones.remove(btn);
            if (alTerminar != null) alTerminar.run();
        });

        navAnimaciones.put(btn, tl);
        tl.play();
    }

    private List<Button> navButtons() {
        return List.of(btnDashboard, btnVentas, btnInventario,
                       btnCompras, btnClientes, btnProveedores,
                       btnCaja, btnGastos, btnReportes, btnConfiguracion);
    }

    // ── Dashboard — delegado a DashboardController ───────────────

    private void mostrarDashboard() {
        contenido.getChildren().setAll(dashboardController.buildView(this::navReportes));
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

    private void configurarBadgeNotificaciones() {
        if (!(btnNotificaciones.getParent() instanceof HBox parent)) return;
        int idx = parent.getChildren().indexOf(btnNotificaciones);
        if (idx < 0) return;

        lblBadgeNotificaciones = new Label();
        lblBadgeNotificaciones.getStyleClass().add("notif-badge");
        lblBadgeNotificaciones.setVisible(false);
        lblBadgeNotificaciones.setManaged(false);
        StackPane.setAlignment(lblBadgeNotificaciones, Pos.TOP_RIGHT);
        StackPane.setMargin(lblBadgeNotificaciones, new Insets(-2, -2, 0, 0));

        StackPane wrapper = new StackPane(btnNotificaciones, lblBadgeNotificaciones);
        wrapper.setAlignment(Pos.CENTER);
        parent.getChildren().set(idx, wrapper);

        actualizarBadge();
    }

    private void actualizarBadge() {
        if (lblBadgeNotificaciones == null) return;
        try {
            int count = notificacionService.contarNotificaciones();
            if (count > 0) {
                lblBadgeNotificaciones.setText(count > 99 ? "99+" : String.valueOf(count));
                lblBadgeNotificaciones.setVisible(true);
                lblBadgeNotificaciones.setManaged(true);
            } else {
                lblBadgeNotificaciones.setVisible(false);
                lblBadgeNotificaciones.setManaged(false);
            }
        } catch (Exception ignored) {}
    }

    private Popup construirPopupNotificaciones(List<Notificacion> notifs) {
        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setStyle(
            "-fx-background-color: #F8F9FB;" +
            "-fx-border-color: transparent transparent rgba(26,31,46,0.08) transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        FontIcon bellIco = new FontIcon("fas-bell");
        bellIco.setIconSize(14);
        bellIco.setIconColor(Paint.valueOf("#5A6ACF"));

        Label lTitle = new Label("Notificaciones");
        lTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label lCount = new Label(notifs.size() + " alerta(s)");
        lCount.setStyle(
            "-fx-font-size: 11px; -fx-text-fill: #FFFFFF; -fx-font-weight: 700;" +
            "-fx-background-color: " + (notifs.isEmpty() ? "#94A3B8" : "#5A6ACF") + ";" +
            "-fx-background-radius: 10px; -fx-padding: 2 8 2 8;"
        );
        header.getChildren().addAll(bellIco, lTitle, spacer, lCount);

        // Lista de filas
        VBox lista = new VBox(0);
        if (notifs.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(24, 16, 24, 16));

            FontIcon emptyIco = new FontIcon("fas-check-circle");
            emptyIco.setIconSize(28);
            emptyIco.setIconColor(Paint.valueOf("#22C55E"));

            Label lVacio = new Label("Sin alertas pendientes");
            lVacio.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

            emptyBox.getChildren().addAll(emptyIco, lVacio);
            lista.getChildren().add(emptyBox);
        } else {
            for (Notificacion n : notifs) {
                lista.getChildren().add(crearFilaNotificacion(n));
            }
        }

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(320);
        scroll.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox panel = new VBox(0);
        panel.setPrefWidth(330);
        panel.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: rgba(26,31,46,0.13);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 10px;" +
            "-fx-background-radius: 10px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 20, 0, 0, 6);"
        );
        panel.getChildren().addAll(header, scroll);

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(panel);
        return popup;
    }

    private HBox crearFilaNotificacion(Notificacion n) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setStyle(
            "-fx-border-color: transparent transparent rgba(26,31,46,0.06) transparent;" +
            "-fx-border-width: 0 0 1 0; -fx-background-color: transparent;"
        );

        boolean esPago = TipoNotificacion.PAGO_CREDITO_PROXIMO.equals(n.tipo());
        String icoColor = switch (n.severidad()) {
            case CRITICA     -> "#EF4444";
            case ADVERTENCIA -> esPago ? "#F59E0B" : "#F97316";
            default          -> "#64748B";
        };
        String bgColor = switch (n.severidad()) {
            case CRITICA     -> "#FEF2F2";
            case ADVERTENCIA -> esPago ? "#FFFBEB" : "#FFF7ED";
            default          -> "#F1F5F9";
        };

        FontIcon ico = new FontIcon(esPago ? "fas-credit-card" : "fas-exclamation-triangle");
        ico.setIconSize(14);
        ico.setIconColor(Paint.valueOf(icoColor));

        StackPane icoWrap = new StackPane(ico);
        icoWrap.setMinSize(32, 32);
        icoWrap.setMaxSize(32, 32);
        icoWrap.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8px;");

        VBox texts = new VBox(3);
        HBox.setHgrow(texts, javafx.scene.layout.Priority.ALWAYS);

        Label lTit = new Label(n.titulo());
        lTit.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        Label lMsg = new Label(n.mensaje());
        lMsg.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        lMsg.setWrapText(true);
        lMsg.setMaxWidth(230);

        texts.getChildren().addAll(lTit, lMsg);
        row.getChildren().addAll(icoWrap, texts);
        return row;
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
