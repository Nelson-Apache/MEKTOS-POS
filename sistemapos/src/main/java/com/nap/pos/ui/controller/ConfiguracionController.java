package com.nap.pos.ui.controller;

import com.nap.pos.application.service.ConfiguracionService;
import com.nap.pos.application.service.UsuarioService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.RegimenTributario;
import com.nap.pos.domain.model.enums.Rol;
import com.nap.pos.domain.model.enums.TipoPersona;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;
    private final UsuarioService       usuarioService;

    // ── Estado ──────────────────────────────────────────────────────────────
    private ConfiguracionTienda configActual;
    private Usuario             usuarioActual;
    private TipoPersona         tipoPersona;
    private String              rutaLogoSeleccionada;

    // ── Estructura principal ─────────────────────────────────────────────────
    private StackPane rootStack;
    private VBox      contentArea;

    // ── Tabs ─────────────────────────────────────────────────────────────────
    private Button tabDatos, tabTributario, tabComprobantes, tabInventario, tabSeguridad, tabUsuarios;
    private static final String COLOR_ACTIVO   = "#5A6ACF";
    private static final String COLOR_INACTIVO = "#78716C";

    // ── Datos del Negocio ────────────────────────────────────────────────────
    private TextField   txtNombreTienda;
    private RadioButton rbNatural, rbJuridica;
    private VBox        grupoNatural, grupoJuridica;
    private TextField   txtNombre, txtApellido, txtCedula;
    private TextField   txtRazonSocial, txtNit, txtRepLegalNombre, txtRepLegalApellido;
    private TextField   txtTelefono, txtCorreo;
    private TextArea    txtDireccion;
    private ImageView   imgLogo;
    private Label       lblRutaLogo;

    // ── Tributario ───────────────────────────────────────────────────────────
    private ComboBox<RegimenTributario> cmbRegimen;
    private VBox             vboxIva;
    private Spinner<Integer> spnIva;
    private CheckBox         chkPrecioConIvaIncluido;
    private Spinner<Integer> spnGanancia;

    // ── Comprobantes ─────────────────────────────────────────────────────────
    private TextField        txtPrefijo;
    private Spinner<Integer> spnNumeroInicial;

    // ── Inventario ───────────────────────────────────────────────────────────
    private Spinner<Integer>  spnStockMinimo;
    private ComboBox<String>  cmbMesInventario;
    private ComboBox<Integer> cmbDiaInventario;

    // ── Seguridad ────────────────────────────────────────────────────────────
    private PasswordField txtPasswordActual, txtPasswordNueva, txtPasswordConfirm;
    private Label         lblPasswordError, lblPasswordSuccess;

    // ════════════════════════════════════════════════════════════════════════
    //  Punto de entrada
    // ════════════════════════════════════════════════════════════════════════

    public Node buildView(Usuario usuario) {
        this.usuarioActual        = usuario;
        this.configActual         = configuracionService.obtener();
        this.tipoPersona          = configActual.getTipoPersona();
        this.rutaLogoSeleccionada = configActual.getRutaLogo();

        rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color: #F5F1EB;");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F5F1EB;");

        contentArea = new VBox(0);
        contentArea.setStyle("-fx-background-color: #F5F1EB;");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        root.getChildren().addAll(buildTabBar(), contentArea);
        rootStack.getChildren().add(root);

        mostrarTab(tabDatos, buildDatosPanel());
        return rootStack;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navbar de tabs (mismo patrón que InventarioController)
    // ════════════════════════════════════════════════════════════════════════

    private HBox buildTabBar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("inventario-tab-bar");
        bar.setPadding(new Insets(14, 28, 10, 28));

        tabDatos        = crearTab("fas-store",               "Datos del Negocio", true);
        tabTributario   = crearTab("fas-file-invoice-dollar", "Tributario",         false);
        tabComprobantes = crearTab("fas-receipt",             "Comprobantes",       false);
        tabInventario   = crearTab("fas-boxes",               "Inventario",         false);
        tabSeguridad    = crearTab("fas-shield-alt",          "Seguridad",          false);

        tabDatos.setOnAction(e        -> { animarClickTab(tabDatos);        mostrarTab(tabDatos,        buildDatosPanel()); });
        tabTributario.setOnAction(e   -> { animarClickTab(tabTributario);   mostrarTab(tabTributario,   buildTributarioPanel()); });
        tabComprobantes.setOnAction(e -> { animarClickTab(tabComprobantes); mostrarTab(tabComprobantes, buildComprobantesPanel()); });
        tabInventario.setOnAction(e   -> { animarClickTab(tabInventario);   mostrarTab(tabInventario,   buildInventarioPanel()); });
        tabSeguridad.setOnAction(e    -> { animarClickTab(tabSeguridad);    mostrarTab(tabSeguridad,    buildSeguridadPanel()); });

        bar.getChildren().addAll(tabDatos, tabTributario, tabComprobantes, tabInventario, tabSeguridad);

        if (usuarioActual.esAdmin()) {
            tabUsuarios = crearTab("fas-users", "Usuarios", false);
            tabUsuarios.setOnAction(e -> { animarClickTab(tabUsuarios); mostrarTab(tabUsuarios, buildUsuariosPanel()); });
            bar.getChildren().add(tabUsuarios);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().add(spacer);
        return bar;
    }

    private Button crearTab(String iconLiteral, String texto, boolean activo) {
        FontIcon ico = new FontIcon(iconLiteral);
        ico.setIconSize(14);
        ico.setIconColor(Paint.valueOf(activo ? COLOR_ACTIVO : COLOR_INACTIVO));

        Button btn = new Button(texto, ico);
        btn.getStyleClass().add("inventario-tab");
        if (activo) btn.getStyleClass().add("inventario-tab-active");
        btn.setGraphicTextGap(8);
        return btn;
    }

    private void activarTab(Button activo) {
        List<Button> todos = new ArrayList<>(List.of(tabDatos, tabTributario, tabComprobantes, tabInventario, tabSeguridad));
        if (tabUsuarios != null) todos.add(tabUsuarios);
        for (Button b : todos) {
            b.getStyleClass().remove("inventario-tab-active");
            ((FontIcon) b.getGraphic()).setIconColor(Paint.valueOf(COLOR_INACTIVO));
        }
        activo.getStyleClass().add("inventario-tab-active");
        ((FontIcon) activo.getGraphic()).setIconColor(Paint.valueOf(COLOR_ACTIVO));
    }

    private void animarClickTab(Button tab) {
        ScaleTransition press = new ScaleTransition(Duration.millis(80), tab);
        press.setToX(0.95); press.setToY(0.95);
        press.setInterpolator(Interpolator.EASE_IN);
        ScaleTransition release = new ScaleTransition(Duration.millis(120), tab);
        release.setToX(1.0); release.setToY(1.0);
        release.setInterpolator(Interpolator.EASE_OUT);
        press.setOnFinished(e -> release.play());
        press.play();
    }

    private void mostrarTab(Button tab, Node panel) {
        activarTab(tab);
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("cfg-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentArea.getChildren().setAll(scroll);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Panel — Datos del Negocio
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildDatosPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-padding: 28 32 28 32; -fx-background-color: #F5F1EB;");

        // Card: Información general
        VBox cardInfo = buildCard("Información General", "fas-building");

        txtNombreTienda = new TextField(nvl(configActual.getNombreTienda()));
        cardInfo.getChildren().add(buildField("Nombre de la Tienda", txtNombreTienda, null));

        ToggleGroup grupo = new ToggleGroup();
        rbNatural  = new RadioButton("Persona Natural");
        rbJuridica = new RadioButton("Persona Jurídica");
        rbNatural.setToggleGroup(grupo);
        rbJuridica.setToggleGroup(grupo);
        (configActual.getTipoPersona() == TipoPersona.JURIDICA ? rbJuridica : rbNatural).setSelected(true);
        rbNatural.setOnAction(e  -> { tipoPersona = TipoPersona.NATURAL;  actualizarGruposPersona(); });
        rbJuridica.setOnAction(e -> { tipoPersona = TipoPersona.JURIDICA; actualizarGruposPersona(); });

        HBox tipoRow = new HBox(20, rbNatural, rbJuridica);
        tipoRow.setAlignment(Pos.CENTER_LEFT);
        cardInfo.getChildren().add(buildFieldWithNode("Tipo de Persona", tipoRow));

        grupoNatural = new VBox(14);
        txtNombre   = new TextField(nvl(configActual.getNombre()));
        txtApellido = new TextField(nvl(configActual.getApellido()));
        txtCedula   = new TextField(nvl(configActual.getCedula()));
        grupoNatural.getChildren().addAll(
                buildField("Nombre", txtNombre, null),
                buildField("Apellido", txtApellido, null),
                buildField("Cédula", txtCedula, null));

        grupoJuridica = new VBox(14);
        txtRazonSocial      = new TextField(nvl(configActual.getRazonSocial()));
        txtNit              = new TextField(nvl(configActual.getNit()));
        txtRepLegalNombre   = new TextField(nvl(configActual.getRepresentanteLegalNombre()));
        txtRepLegalApellido = new TextField(nvl(configActual.getRepresentanteLegalApellido()));
        grupoJuridica.getChildren().addAll(
                buildField("Razón Social", txtRazonSocial, null),
                buildField("NIT", txtNit, null),
                buildField("Nombre del Representante Legal", txtRepLegalNombre, null),
                buildField("Apellido del Representante Legal", txtRepLegalApellido, null));

        cardInfo.getChildren().addAll(grupoNatural, grupoJuridica);
        actualizarGruposPersona();

        // Card: Contacto
        VBox cardContacto = buildCard("Contacto", "fas-address-card");
        txtTelefono  = new TextField(nvl(configActual.getTelefono()));
        txtCorreo    = new TextField(nvl(configActual.getCorreo()));
        txtDireccion = new TextArea(nvl(configActual.getDireccion()));
        txtDireccion.setPrefRowCount(3);
        txtDireccion.setWrapText(true);
        cardContacto.getChildren().addAll(
                buildField("Teléfono", txtTelefono, null),
                buildField("Correo Electrónico", txtCorreo, null),
                buildFieldWithNode("Dirección", txtDireccion));

        // Card: Logo
        VBox cardLogo = buildCard("Logo del Negocio", "fas-image");

        imgLogo = new ImageView();
        imgLogo.setFitWidth(72); imgLogo.setFitHeight(72);
        imgLogo.setPreserveRatio(true);
        cargarImagenLogo();

        StackPane logoFrame = new StackPane(imgLogo);
        logoFrame.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 10; " +
                "-fx-min-width: 72; -fx-min-height: 72; -fx-max-width: 72; -fx-max-height: 72;");

        lblRutaLogo = new Label(configActual.getRutaLogo() != null ? "Logo configurado" : "Sin logo seleccionado");
        lblRutaLogo.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716C;");

        Button btnLogo = new Button("Seleccionar logo");
        FontIcon uploadIco = FontIcon.of(FontAwesomeSolid.UPLOAD, 13);
        btnLogo.setGraphic(uploadIco);
        btnLogo.getStyleClass().add("btn-secundario");
        btnLogo.setOnAction(e -> onSeleccionarLogo());

        Label logoHint = new Label("PNG, JPG o GIF. Mínimo 400 × 400 px recomendado.");
        logoHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");

        VBox logoMeta = new VBox(6, lblRutaLogo, btnLogo, logoHint);
        logoMeta.setAlignment(Pos.CENTER_LEFT);
        HBox logoRow = new HBox(16, logoFrame, logoMeta);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        cardLogo.getChildren().add(logoRow);

        panel.getChildren().addAll(cardInfo, cardContacto, cardLogo, buildActionBar(this::guardarDatos));
        return panel;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Panel — Tributario
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildTributarioPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-padding: 28 32 28 32; -fx-background-color: #F5F1EB;");

        VBox card = buildCard("Régimen Tributario", "fas-file-invoice-dollar");

        cmbRegimen = new ComboBox<>(FXCollections.observableArrayList(RegimenTributario.values()));
        cmbRegimen.setValue(configActual.getRegimenTributario());
        cmbRegimen.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(buildFieldWithNode("Régimen Tributario", cmbRegimen));

        vboxIva = new VBox(14);
        spnIva = new Spinner<>(0, 100, configActual.getIvaPorDefecto());
        spnIva.setEditable(true);
        spnIva.setMaxWidth(140);
        chkPrecioConIvaIncluido = new CheckBox("El precio de venta ya incluye IVA");
        chkPrecioConIvaIncluido.setSelected(configActual.isPrecioConIvaIncluido());
        VBox ivaBox = new VBox(6, labelForm("Porcentaje de IVA (%)"), spnIva);
        vboxIva.getChildren().addAll(ivaBox, chkPrecioConIvaIncluido);
        card.getChildren().add(vboxIva);

        spnGanancia = new Spinner<>(0, 500, configActual.getPorcentajeGananciaGlobal());
        spnGanancia.setEditable(true);
        spnGanancia.setMaxWidth(140);
        card.getChildren().add(buildSpinnerField("Porcentaje de Ganancia Global (%)",
                spnGanancia, "Se aplica al crear productos sin proveedor asignado"));

        cmbRegimen.setOnAction(e -> actualizarVisibilidadIva());
        actualizarVisibilidadIva();

        panel.getChildren().addAll(card, buildActionBar(this::guardarTributario));
        return panel;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Panel — Comprobantes
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildComprobantesPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-padding: 28 32 28 32; -fx-background-color: #F5F1EB;");

        VBox card = buildCard("Numeración de Comprobantes", "fas-receipt");

        txtPrefijo = new TextField(configActual.getPrefijoComprobante());
        spnNumeroInicial = new Spinner<>(1, 999_999, configActual.getNumeroInicialComprobante());
        spnNumeroInicial.setEditable(true);
        spnNumeroInicial.setMaxWidth(160);

        Label preview = new Label();
        preview.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #5A6ACF;");
        actualizarPreviewComprobante(preview);
        txtPrefijo.textProperty().addListener((o, ov, nv) -> actualizarPreviewComprobante(preview));
        spnNumeroInicial.valueProperty().addListener((o, ov, nv) -> actualizarPreviewComprobante(preview));

        Label lblPrev = new Label("Vista previa del próximo comprobante:");
        lblPrev.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
        VBox previewBox = new VBox(4, lblPrev, preview);

        card.getChildren().addAll(
                buildField("Prefijo del Comprobante", txtPrefijo, "Ejemplo: FAC-, BOL-, VENTA-"),
                buildSpinnerField("Número Inicial", spnNumeroInicial, null),
                previewBox);

        panel.getChildren().addAll(card, buildActionBar(this::guardarComprobantes));
        return panel;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Panel — Inventario
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildInventarioPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-padding: 28 32 28 32; -fx-background-color: #F5F1EB;");

        VBox card = buildCard("Parámetros de Inventario", "fas-boxes");

        spnStockMinimo = new Spinner<>(0, 9999, configActual.getStockMinimoGlobal());
        spnStockMinimo.setEditable(true);
        spnStockMinimo.setMaxWidth(140);
        card.getChildren().add(buildSpinnerField("Umbral de Stock Mínimo Global",
                spnStockMinimo, "Productos con stock ≤ este valor generan una alerta"));

        card.getChildren().add(buildSeparatorRegion());

        String[] meses = {"", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        cmbMesInventario = new ComboBox<>(FXCollections.observableArrayList(meses));
        String mesActual = (configActual.getMesInventarioAnual() != null
                && configActual.getMesInventarioAnual() >= 1
                && configActual.getMesInventarioAnual() <= 12)
                ? meses[configActual.getMesInventarioAnual()] : "";
        cmbMesInventario.setValue(mesActual);
        cmbMesInventario.setMaxWidth(180);

        cmbDiaInventario = new ComboBox<>();
        actualizarDiasInventario();
        cmbDiaInventario.setValue(configActual.getDiaInventarioAnual());
        cmbDiaInventario.setMaxWidth(110);
        cmbMesInventario.setOnAction(e -> actualizarDiasInventario());

        HBox invRow = new HBox(12,
                new VBox(6, labelForm("Mes"),  cmbMesInventario),
                new VBox(6, labelForm("Día"),  cmbDiaInventario));
        invRow.setAlignment(Pos.BOTTOM_LEFT);

        Label hintInv = new Label("Deja en blanco si no quieres programar un inventario anual");
        hintInv.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
        card.getChildren().add(new VBox(8, labelForm("Fecha del Inventario Anual"), invRow, hintInv));

        panel.getChildren().addAll(card, buildActionBar(this::guardarInventario));
        return panel;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Panel — Seguridad
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildSeguridadPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-padding: 28 32 28 32; -fx-background-color: #F5F1EB;");

        VBox card = buildCard("Cambiar Contraseña", "fas-lock");

        Label infoUser = new Label("Usuario activo: " + usuarioActual.getUsername());
        infoUser.setStyle("-fx-font-size: 12px; -fx-text-fill: #4B5563;");

        txtPasswordActual  = new PasswordField();
        txtPasswordNueva   = new PasswordField();
        txtPasswordConfirm = new PasswordField();
        txtPasswordActual.setPromptText("Contraseña actual");
        txtPasswordNueva.setPromptText("Mínimo 6 caracteres");
        txtPasswordConfirm.setPromptText("Repite la nueva contraseña");

        lblPasswordError = new Label();
        lblPasswordError.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");
        lblPasswordError.setVisible(false); lblPasswordError.setManaged(false);

        lblPasswordSuccess = new Label("Contraseña cambiada correctamente.");
        lblPasswordSuccess.setStyle("-fx-text-fill: #15803D; -fx-font-size: 12px;");
        lblPasswordSuccess.setVisible(false); lblPasswordSuccess.setManaged(false);

        card.getChildren().addAll(
                infoUser,
                buildField("Contraseña Actual", txtPasswordActual, null),
                buildField("Nueva Contraseña", txtPasswordNueva, null),
                buildField("Confirmar Nueva Contraseña", txtPasswordConfirm, null),
                lblPasswordError, lblPasswordSuccess);

        panel.getChildren().addAll(card, buildActionBar(this::guardarSeguridad));
        return panel;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Handlers de guardado
    // ════════════════════════════════════════════════════════════════════════

    private void guardarDatos() {
        if (txtNombreTienda.getText().isBlank()) { mostrarError("El nombre de la tienda es obligatorio."); return; }
        if (tipoPersona == TipoPersona.NATURAL) {
            if (txtNombre.getText().isBlank() || txtApellido.getText().isBlank() || txtCedula.getText().isBlank()) {
                mostrarError("Nombre, apellido y cédula son obligatorios."); return;
            }
        } else {
            if (txtRazonSocial.getText().isBlank() || txtNit.getText().isBlank()) {
                mostrarError("Razón social y NIT son obligatorios."); return;
            }
            if (txtRepLegalNombre.getText().isBlank() || txtRepLegalApellido.getText().isBlank()) {
                mostrarError("Nombre y apellido del representante legal son obligatorios."); return;
            }
        }
        if (txtTelefono.getText().isBlank()) { mostrarError("El teléfono es obligatorio."); return; }
        String correo = txtCorreo.getText().trim();
        if (!correo.isEmpty() && !correo.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            mostrarError("El correo no tiene un formato válido."); return;
        }
        if (txtDireccion.getText().isBlank()) { mostrarError("La dirección es obligatoria."); return; }

        configActual = configuracionService.guardar(withBase()
                .tipoPersona(tipoPersona)
                .nombreTienda(txtNombreTienda.getText().trim())
                .nombre(tipoPersona == TipoPersona.NATURAL ? txtNombre.getText().trim() : null)
                .apellido(tipoPersona == TipoPersona.NATURAL ? txtApellido.getText().trim() : null)
                .cedula(tipoPersona == TipoPersona.NATURAL ? txtCedula.getText().trim() : null)
                .razonSocial(tipoPersona == TipoPersona.JURIDICA ? txtRazonSocial.getText().trim() : null)
                .nit(tipoPersona == TipoPersona.JURIDICA ? txtNit.getText().trim() : null)
                .representanteLegalNombre(tipoPersona == TipoPersona.JURIDICA ? txtRepLegalNombre.getText().trim() : null)
                .representanteLegalApellido(tipoPersona == TipoPersona.JURIDICA ? txtRepLegalApellido.getText().trim() : null)
                .telefono(txtTelefono.getText().trim())
                .correo(correo.isEmpty() ? null : correo)
                .direccion(txtDireccion.getText().trim())
                .rutaLogo(rutaLogoSeleccionada)
                .build());
        mostrarExito("Datos del negocio actualizados.");
    }

    private void guardarTributario() {
        RegimenTributario regimen = cmbRegimen.getValue();
        int iva = regimen != RegimenTributario.NO_RESPONSABLE_IVA ? spnIva.getValue() : 0;
        configActual = configuracionService.guardar(withBase()
                .regimenTributario(regimen)
                .ivaPorDefecto(iva)
                .precioConIvaIncluido(chkPrecioConIvaIncluido.isSelected())
                .porcentajeGananciaGlobal(spnGanancia.getValue())
                .build());
        mostrarExito("Configuración tributaria actualizada.");
    }

    private void guardarComprobantes() {
        if (txtPrefijo.getText().isBlank()) { mostrarError("El prefijo del comprobante es obligatorio."); return; }
        configActual = configuracionService.guardar(withBase()
                .prefijoComprobante(txtPrefijo.getText().trim())
                .numeroInicialComprobante(spnNumeroInicial.getValue())
                .build());
        mostrarExito("Configuración de comprobantes actualizada.");
    }

    private void guardarInventario() {
        Integer mes = obtenerMesInventario();
        Integer dia = cmbDiaInventario.getValue();
        if (mes != null && dia == null) { mostrarError("Selecciona también el día del inventario anual."); return; }
        if (mes == null) dia = null;
        configActual = configuracionService.guardar(withBase()
                .stockMinimoGlobal(spnStockMinimo.getValue())
                .mesInventarioAnual(mes)
                .diaInventarioAnual(dia)
                .build());
        mostrarExito("Configuración de inventario actualizada.");
    }

    private void guardarSeguridad() {
        lblPasswordError.setVisible(false);   lblPasswordError.setManaged(false);
        lblPasswordSuccess.setVisible(false); lblPasswordSuccess.setManaged(false);

        if (txtPasswordActual.getText().isBlank() || txtPasswordNueva.getText().isBlank()
                || txtPasswordConfirm.getText().isBlank()) {
            mostrarPasswordError("Todos los campos son obligatorios."); return;
        }
        if (txtPasswordNueva.getText().length() < 6) {
            mostrarPasswordError("La nueva contraseña debe tener al menos 6 caracteres."); return;
        }
        if (!txtPasswordNueva.getText().equals(txtPasswordConfirm.getText())) {
            mostrarPasswordError("Las contraseñas no coinciden."); return;
        }
        try {
            usuarioService.login(usuarioActual.getUsername(), txtPasswordActual.getText());
        } catch (BusinessException ex) {
            mostrarPasswordError("La contraseña actual es incorrecta."); return;
        }
        usuarioService.actualizarPassword(usuarioActual.getUsername(), txtPasswordNueva.getText());
        txtPasswordActual.clear(); txtPasswordNueva.clear(); txtPasswordConfirm.clear();
        lblPasswordSuccess.setVisible(true); lblPasswordSuccess.setManaged(true);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Builder base — preserva todos los campos del configActual
    // ════════════════════════════════════════════════════════════════════════

    private ConfiguracionTienda.ConfiguracionTiendaBuilder withBase() {
        return ConfiguracionTienda.builder()
                .id(1L)
                .tipoPersona(configActual.getTipoPersona())
                .nombreTienda(configActual.getNombreTienda())
                .nombre(configActual.getNombre())
                .apellido(configActual.getApellido())
                .cedula(configActual.getCedula())
                .razonSocial(configActual.getRazonSocial())
                .nit(configActual.getNit())
                .representanteLegalNombre(configActual.getRepresentanteLegalNombre())
                .representanteLegalApellido(configActual.getRepresentanteLegalApellido())
                .telefono(configActual.getTelefono())
                .correo(configActual.getCorreo())
                .direccion(configActual.getDireccion())
                .rutaLogo(configActual.getRutaLogo())
                .stockMinimoGlobal(configActual.getStockMinimoGlobal())
                .regimenTributario(configActual.getRegimenTributario())
                .ivaPorDefecto(configActual.getIvaPorDefecto())
                .precioConIvaIncluido(configActual.isPrecioConIvaIncluido())
                .porcentajeGananciaGlobal(configActual.getPorcentajeGananciaGlobal())
                .prefijoComprobante(configActual.getPrefijoComprobante())
                .numeroInicialComprobante(configActual.getNumeroInicialComprobante())
                .mesInventarioAnual(configActual.getMesInventarioAnual())
                .diaInventarioAnual(configActual.getDiaInventarioAnual())
                .propietarioId(configActual.getPropietarioId());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Lógica auxiliar
    // ════════════════════════════════════════════════════════════════════════

    private void actualizarGruposPersona() {
        boolean esNatural = tipoPersona == TipoPersona.NATURAL;
        grupoNatural.setVisible(esNatural);   grupoNatural.setManaged(esNatural);
        grupoJuridica.setVisible(!esNatural); grupoJuridica.setManaged(!esNatural);
    }

    private void actualizarVisibilidadIva() {
        boolean esResponsable = cmbRegimen.getValue() != RegimenTributario.NO_RESPONSABLE_IVA;
        vboxIva.setVisible(esResponsable); vboxIva.setManaged(esResponsable);
    }

    private void onSeleccionarLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar logo del negocio");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File origen = chooser.showOpenDialog(rootStack.getScene().getWindow());
        if (origen == null) return;
        try {
            String ext     = obtenerExtension(origen.getName());
            Path   carpeta = Paths.get(System.getProperty("user.home"), ".nappos", "assets");
            Files.createDirectories(carpeta);
            Path destino = carpeta.resolve("logo." + ext);
            Files.copy(origen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
            rutaLogoSeleccionada = destino.toAbsolutePath().toString();
            imgLogo.setImage(new Image(destino.toUri().toString()));
            lblRutaLogo.setText("logo." + ext + " — guardado");
            lblRutaLogo.setStyle("-fx-font-size: 11px; -fx-text-fill: #15803D; -fx-font-weight: 600;");
        } catch (IOException e) {
            mostrarError("No se pudo guardar el logo: " + e.getMessage());
        }
    }

    private void cargarImagenLogo() {
        if (configActual.getRutaLogo() == null) return;
        try {
            File f = new File(configActual.getRutaLogo());
            if (f.exists()) imgLogo.setImage(new Image(f.toURI().toString()));
        } catch (Exception ignored) {}
    }

    private void actualizarDiasInventario() {
        String mes = cmbMesInventario != null ? cmbMesInventario.getValue() : "";
        int maxDias = switch (mes == null ? "" : mes) {
            case "Febrero"                                    -> 29;
            case "Abril", "Junio", "Septiembre", "Noviembre" -> 30;
            default                                           -> 31;
        };
        List<Integer> dias = new ArrayList<>();
        dias.add(null);
        for (int i = 1; i <= maxDias; i++) dias.add(i);
        Integer prev = cmbDiaInventario.getValue();
        cmbDiaInventario.setItems(FXCollections.observableArrayList(dias));
        cmbDiaInventario.setValue(prev != null && prev <= maxDias ? prev : null);
    }

    private Integer obtenerMesInventario() {
        String[] meses = {"", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        String mes = cmbMesInventario.getValue();
        if (mes == null || mes.isEmpty()) return null;
        for (int i = 1; i < meses.length; i++) if (meses[i].equals(mes)) return i;
        return null;
    }

    private void actualizarPreviewComprobante(Label preview) {
        String pref = txtPrefijo != null ? txtPrefijo.getText().trim() : configActual.getPrefijoComprobante();
        int    num  = spnNumeroInicial != null ? spnNumeroInicial.getValue() : configActual.getNumeroInicialComprobante();
        preview.setText(pref + String.format("%06d", num));
    }

    private String obtenerExtension(String nombre) {
        int dot = nombre.lastIndexOf('.');
        return dot >= 0 ? nombre.substring(dot + 1).toLowerCase() : "png";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers de UI
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildCard(String titulo, String iconLiteral) {
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: #FDFCFA; " +
                "-fx-border-color: rgba(26,31,46,0.10); -fx-border-width: 1; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 22 24 22 24;");

        FontIcon ico = new FontIcon(iconLiteral);
        ico.setIconSize(15);
        ico.getStyleClass().add("icon-accent");
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox cardHeader = new HBox(10, ico, lblTitulo);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        Region sep = new Region();
        sep.setMinHeight(1); sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: rgba(26,31,46,0.07);");

        card.getChildren().addAll(cardHeader, sep);
        return card;
    }

    private VBox buildField(String label, Control control, String hint) {
        control.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(6, labelForm(label), control);
        if (hint != null) {
            Label h = new Label(hint);
            h.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
            box.getChildren().add(h);
        }
        return box;
    }

    private VBox buildFieldWithNode(String label, Node control) {
        if (control instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, labelForm(label), control);
    }

    private VBox buildSpinnerField(String label, Control control, String hint) {
        VBox box = new VBox(6, labelForm(label), control);
        if (hint != null) {
            Label h = new Label(hint);
            h.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
            box.getChildren().add(h);
        }
        return box;
    }

    private Label labelForm(String texto) {
        Label lbl = new Label(texto);
        lbl.getStyleClass().add("form-label");
        return lbl;
    }

    private HBox buildActionBar(Runnable onSave) {
        Button btnGuardar = new Button("Guardar Cambios");
        FontIcon ico = FontIcon.of(FontAwesomeSolid.CHECK, 13);
        ico.getStyleClass().add("icon-on-primary");
        btnGuardar.setGraphic(ico);
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setGraphicTextGap(8);
        btnGuardar.setContentDisplay(ContentDisplay.RIGHT);
        btnGuardar.setOnAction(e -> onSave.run());

        HBox bar = new HBox(btnGuardar);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setStyle("-fx-padding: 12 0 4 0;");
        return bar;
    }

    private Region buildSeparatorRegion() {
        Region sep = new Region();
        sep.setMinHeight(1); sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: rgba(26,31,46,0.07);");
        VBox.setMargin(sep, new Insets(4, 0, 4, 0));
        return sep;
    }

    private void mostrarPasswordError(String msg) {
        lblPasswordError.setText(msg);
        lblPasswordError.setVisible(true); lblPasswordError.setManaged(true);
    }

    private void mostrarError(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Datos incompletos");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void mostrarExito(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configuración guardada");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    // ════════════════════════════════════════════════════════════════════════
    //  Panel — Usuarios (solo ADMIN)
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildUsuariosPanel() {
        List<Usuario> todos = usuarioService.findAll();
        long nAdmins  = todos.stream().filter(Usuario::esAdmin).count();
        long nCajeros = todos.stream().filter(u -> !u.esAdmin()).count();
        long nActivos = todos.stream().filter(Usuario::isActivo).count();

        VBox panel = new VBox(20);
        panel.setStyle("-fx-padding: 28 32 28 32; -fx-background-color: #F5F1EB;");

        // ── KPI ──────────────────────────────────────────────────────────────
        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
            crearKpiUsuario("fas-users",         "#5A6ACF", "Total usuarios",    String.valueOf(todos.size())),
            crearKpiUsuario("fas-user-shield",   "#7C3AED", "Administradores",   String.valueOf(nAdmins)),
            crearKpiUsuario("fas-cash-register", "#D97706", "Cajeros",           String.valueOf(nCajeros)),
            crearKpiUsuario("fas-user-check",    "#15803D", "Activos",           String.valueOf(nActivos))
        );

        // ── Card con tabla ────────────────────────────────────────────────────
        VBox card = buildCard("Gestión de Usuarios", "fas-users");

        FontIcon icoPlus = new FontIcon("fas-user-plus");
        icoPlus.setIconSize(13);
        icoPlus.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnNuevo = new Button("Nuevo Usuario", icoPlus);
        btnNuevo.getStyleClass().add("btn-primario");
        btnNuevo.setStyle("-fx-padding: 8 16 8 16; -fx-font-size: 13px;");
        btnNuevo.setOnAction(e -> abrirModalUsuario(null));

        HBox toolbar = new HBox(btnNuevo);
        toolbar.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(toolbar, crearTablaUsuarios(todos));
        panel.getChildren().addAll(kpiRow, card);
        return panel;
    }

    private VBox crearKpiUsuario(String icon, String color, String titulo, String valor) {
        VBox card = new VBox(8);
        card.setStyle(
            "-fx-background-color: #FDFCFA; -fx-border-color: rgba(26,31,46,0.10); " +
            "-fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; " +
            "-fx-padding: 18 20 18 20;"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 8px;");
        icoCircle.setMinSize(36, 36);
        icoCircle.setMaxSize(36, 36);
        FontIcon ico = new FontIcon(icon);
        ico.setIconSize(16);
        ico.setIconColor(Paint.valueOf(color));
        icoCircle.getChildren().add(ico);

        Label lblVal = new Label(valor);
        lblVal.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        Label lblTit = new Label(titulo);
        lblTit.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");

        card.getChildren().addAll(icoCircle, lblVal, lblTit);
        return card;
    }

    @SuppressWarnings("unchecked")
    private TableView<Usuario> crearTablaUsuarios(List<Usuario> lista) {
        TableView<Usuario> tabla = new TableView<>();
        tabla.getStyleClass().add("inventario-table");
        tabla.setPrefHeight(Math.min(lista.size() * 44 + 44, 420));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("No hay usuarios registrados."));

        TableColumn<Usuario, String> colUser = new TableColumn<>("Usuario");
        colUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colUser.setPrefWidth(150);

        TableColumn<Usuario, String> colNombre = new TableColumn<>("Nombre completo");
        colNombre.setCellValueFactory(c -> {
            Usuario u = c.getValue();
            String n = u.getNombre() != null ? u.getNombre() : "";
            String a = u.getApellido() != null ? u.getApellido() : "";
            String full = (n + " " + a).trim();
            return new SimpleStringProperty(full.isEmpty() ? "—" : full);
        });
        colNombre.setPrefWidth(190);

        TableColumn<Usuario, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().esAdmin() ? "Admin" : "Cajero"));
        colRol.setPrefWidth(100);
        colRol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                boolean admin = "Admin".equals(s);
                Label badge = new Label(s);
                badge.setStyle(
                    "-fx-background-color: " + (admin ? "#EDE9FA" : "#ECEEF7") + ";" +
                    "-fx-text-fill: " + (admin ? "#7C3AED" : "#5A6ACF") + ";" +
                    "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 10;" +
                    "-fx-background-radius: 10px;"
                );
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<Usuario, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colEstado.setPrefWidth(90);
        colEstado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                boolean activo = "Activo".equals(s);
                Label badge = new Label(s);
                badge.setStyle(
                    "-fx-background-color: " + (activo ? "#DCFCE7" : "#FEE2E2") + ";" +
                    "-fx-text-fill: " + (activo ? "#15803D" : "#DC2626") + ";" +
                    "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 10;" +
                    "-fx-background-radius: 10px;"
                );
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<Usuario, Void> colAcciones = new TableColumn<>("");
        colAcciones.setPrefWidth(100);
        colAcciones.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEditar = new Button();
            private final Button btnToggle = new Button();
            {
                FontIcon icoEdit = new FontIcon("fas-pen");
                icoEdit.setIconSize(12);
                icoEdit.setIconColor(Paint.valueOf("#5A6ACF"));
                btnEditar.setGraphic(icoEdit);
                btnEditar.setStyle(
                    "-fx-background-color: #ECEEF7; -fx-background-radius: 6px;" +
                    "-fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 5 8;"
                );
                btnEditar.setOnAction(e -> abrirModalUsuario(getTableView().getItems().get(getIndex())));

                btnToggle.setStyle(
                    "-fx-background-color: #F5F1EB; -fx-background-radius: 6px;" +
                    "-fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 5 8;"
                );
                btnToggle.setOnAction(e -> toggleActivoUsuario(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Usuario u = getTableView().getItems().get(getIndex());
                FontIcon icoToggle = new FontIcon(u.isActivo() ? "fas-user-slash" : "fas-user-check");
                icoToggle.setIconSize(12);
                icoToggle.setIconColor(Paint.valueOf(u.isActivo() ? "#DC2626" : "#15803D"));
                btnToggle.setGraphic(icoToggle);
                HBox acciones = new HBox(6, btnEditar, btnToggle);
                acciones.setAlignment(Pos.CENTER);
                setGraphic(acciones);
            }
        });

        tabla.getItems().addAll(lista);
        tabla.getColumns().addAll(colUser, colNombre, colRol, colEstado, colAcciones);
        return tabla;
    }

    private void toggleActivoUsuario(Usuario u) {
        String accion = u.isActivo() ? "Desactivar" : "Activar";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(accion + " usuario");
        confirm.setHeaderText(null);
        confirm.setContentText("¿" + accion + " al usuario \"" + u.getUsername() + "\"?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    usuarioService.toggleActivo(u.getId(), usuarioActual.getId());
                    mostrarTab(tabUsuarios, buildUsuariosPanel());
                } catch (BusinessException ex) {
                    mostrarError(ex.getMessage());
                }
            }
        });
    }

    private void abrirModalUsuario(Usuario usuarioEditar) {
        boolean esEdicion = usuarioEditar != null;

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(25,32,48,0.55);");
        overlay.setAlignment(Pos.CENTER);

        VBox panel = new VBox(18);
        panel.setMinWidth(320);
        panel.prefWidthProperty().bind(
            Bindings.when(overlay.widthProperty().subtract(64).lessThan(480))
                    .then(overlay.widthProperty().subtract(64))
                    .otherwise(480.0)
        );
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setPadding(new Insets(28, 32, 28, 32));
        panel.setStyle(
            "-fx-background-color: #FDFCFA; -fx-background-radius: 14px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 28, 0, 0, 8);"
        );

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: #5A6ACF22; -fx-background-radius: 10px;");
        icoCircle.setMinSize(40, 40);
        icoCircle.setMaxSize(40, 40);
        FontIcon icoH = new FontIcon(esEdicion ? "fas-user-edit" : "fas-user-plus");
        icoH.setIconSize(18);
        icoH.setIconColor(Paint.valueOf("#5A6ACF"));
        icoCircle.getChildren().add(icoH);
        Label lblTituloModal = new Label(esEdicion ? "Editar Usuario" : "Nuevo Usuario");
        lblTituloModal.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        Region spacerH = new Region();
        HBox.setHgrow(spacerH, Priority.ALWAYS);
        Button btnX = new Button("✕");
        btnX.setStyle(
            "-fx-background-color: transparent; -fx-font-size: 15px;" +
            "-fx-text-fill: #94A3B8; -fx-cursor: hand; -fx-padding: 4 8;"
        );
        header.getChildren().addAll(icoCircle, lblTituloModal, spacerH, btnX);

        // Formulario
        VBox form = new VBox(14);

        // Username
        TextField txtUsernameM = new TextField(esEdicion ? usuarioEditar.getUsername() : "");
        txtUsernameM.setPromptText("nombre.usuario");
        txtUsernameM.setMaxWidth(Double.MAX_VALUE);
        txtUsernameM.setDisable(esEdicion);
        txtUsernameM.setStyle(
            "-fx-font-size: 13px; -fx-background-color: " + (esEdicion ? "#E9E5DE" : "#EDE9E2") + ";" +
            "-fx-background-radius: 8px; -fx-border-color: transparent; -fx-padding: 8 12;"
        );
        VBox grpUser = new VBox(6, labelForm("Usuario" + (esEdicion ? "" : " *")), txtUsernameM);

        // Nombre y Apellido
        TextField txtNombreM = new TextField(esEdicion && usuarioEditar.getNombre() != null ? usuarioEditar.getNombre() : "");
        txtNombreM.setPromptText("Nombre");
        txtNombreM.setMaxWidth(Double.MAX_VALUE);
        txtNombreM.setStyle(
            "-fx-font-size: 13px; -fx-background-color: #EDE9E2;" +
            "-fx-background-radius: 8px; -fx-border-color: transparent; -fx-padding: 8 12;"
        );
        TextField txtApellidoM = new TextField(esEdicion && usuarioEditar.getApellido() != null ? usuarioEditar.getApellido() : "");
        txtApellidoM.setPromptText("Apellido");
        txtApellidoM.setMaxWidth(Double.MAX_VALUE);
        txtApellidoM.setStyle(
            "-fx-font-size: 13px; -fx-background-color: #EDE9E2;" +
            "-fx-background-radius: 8px; -fx-border-color: transparent; -fx-padding: 8 12;"
        );
        VBox grpNombreM   = new VBox(6, labelForm("Nombre"), txtNombreM);
        VBox grpApellidoM = new VBox(6, labelForm("Apellido"), txtApellidoM);
        grpNombreM.setMaxWidth(Double.MAX_VALUE);
        grpApellidoM.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(grpNombreM, Priority.ALWAYS);
        HBox.setHgrow(grpApellidoM, Priority.ALWAYS);
        HBox nombreRow = new HBox(12, grpNombreM, grpApellidoM);

        // Rol
        ComboBox<Rol> cbRolM = new ComboBox<>(FXCollections.observableArrayList(Rol.values()));
        cbRolM.setValue(esEdicion ? usuarioEditar.getRol() : Rol.CAJERO);
        cbRolM.setMaxWidth(Double.MAX_VALUE);
        cbRolM.setConverter(new StringConverter<>() {
            @Override public String toString(Rol r) {
                if (r == null) return "";
                return r == Rol.ADMIN ? "Administrador" : "Cajero";
            }
            @Override public Rol fromString(String s) { return null; }
        });
        VBox grpRolM = new VBox(6, labelForm("Rol *"), cbRolM);

        // Contraseña
        PasswordField txtPwdM = new PasswordField();
        txtPwdM.setPromptText(esEdicion ? "Nueva contraseña (opcional)" : "Mínimo 6 caracteres");
        txtPwdM.setMaxWidth(Double.MAX_VALUE);
        txtPwdM.setStyle(
            "-fx-font-size: 13px; -fx-background-color: #EDE9E2;" +
            "-fx-background-radius: 8px; -fx-border-color: transparent; -fx-padding: 8 12;"
        );
        PasswordField txtPwdConfirmM = new PasswordField();
        txtPwdConfirmM.setPromptText("Confirmar contraseña");
        txtPwdConfirmM.setMaxWidth(Double.MAX_VALUE);
        txtPwdConfirmM.setStyle(
            "-fx-font-size: 13px; -fx-background-color: #EDE9E2;" +
            "-fx-background-radius: 8px; -fx-border-color: transparent; -fx-padding: 8 12;"
        );
        VBox grpPwdM = new VBox(6, labelForm(esEdicion ? "Nueva Contraseña" : "Contraseña *"), txtPwdM);
        VBox grpPwdConfirmM = new VBox(6, labelForm("Confirmar Contraseña" + (esEdicion ? "" : " *")), txtPwdConfirmM);
        if (esEdicion) {
            Label hintPwd = new Label("Deja en blanco para no cambiar la contraseña.");
            hintPwd.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
            grpPwdM.getChildren().add(hintPwd);
        }

        form.getChildren().addAll(grpUser, nombreRow, grpRolM, grpPwdM, grpPwdConfirmM);

        // Error
        Label lblModalError = new Label();
        lblModalError.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");
        lblModalError.setWrapText(true);
        lblModalError.setVisible(false);
        lblModalError.setManaged(false);

        // Botones
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelarM = new Button("Cancelar");
        btnCancelarM.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #4B5563;" +
            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 18;" +
            "-fx-border-color: rgba(26,31,46,0.15); -fx-border-radius: 8px;" +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        );
        FontIcon icoSave = new FontIcon("fas-save");
        icoSave.setIconSize(13);
        icoSave.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnGuardarM = new Button("Guardar", icoSave);
        btnGuardarM.getStyleClass().add("btn-primario");
        btnGuardarM.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 13px;");
        botones.getChildren().addAll(btnCancelarM, btnGuardarM);

        panel.getChildren().addAll(header, new Separator(), form, lblModalError, botones);

        ScrollPane panelScroll = new ScrollPane(panel);
        panelScroll.setFitToWidth(true);
        panelScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panelScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        panelScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        panelScroll.setMinWidth(320);
        panelScroll.prefWidthProperty().bind(panel.prefWidthProperty());
        panelScroll.maxWidthProperty().bind(panel.prefWidthProperty());
        panelScroll.maxHeightProperty().bind(overlay.heightProperty().subtract(48));
        overlay.getChildren().add(panelScroll);

        Runnable cerrar = () -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), overlay);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(ev -> rootStack.getChildren().remove(overlay));
            ft.play();
        };
        btnX.setOnAction(e -> cerrar.run());
        btnCancelarM.setOnAction(e -> cerrar.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrar.run(); });

        btnGuardarM.setOnAction(e -> {
            lblModalError.setVisible(false);
            lblModalError.setManaged(false);

            String uname    = txtUsernameM.getText().trim();
            String nombre   = txtNombreM.getText().trim();
            String apellido = txtApellidoM.getText().trim();
            Rol    rol      = cbRolM.getValue();
            String pwd      = txtPwdM.getText();
            String pwdC     = txtPwdConfirmM.getText();

            if (!esEdicion && uname.isBlank()) {
                setModalError(lblModalError, "El nombre de usuario es obligatorio."); return;
            }
            if (rol == null) {
                setModalError(lblModalError, "Selecciona un rol."); return;
            }
            if (!esEdicion && pwd.isBlank()) {
                setModalError(lblModalError, "La contraseña es obligatoria."); return;
            }
            if (!pwd.isBlank()) {
                if (pwd.length() < 6) {
                    setModalError(lblModalError, "La contraseña debe tener al menos 6 caracteres."); return;
                }
                if (!pwd.equals(pwdC)) {
                    setModalError(lblModalError, "Las contraseñas no coinciden."); return;
                }
            }

            try {
                if (esEdicion) {
                    usuarioService.actualizarUsuario(usuarioEditar.getId(),
                            nombre.isBlank() ? null : nombre,
                            apellido.isBlank() ? null : apellido,
                            rol);
                    if (!pwd.isBlank()) {
                        usuarioService.resetPasswordById(usuarioEditar.getId(), pwd);
                    }
                } else {
                    usuarioService.crear(uname, pwd, rol,
                            nombre.isBlank() ? null : nombre,
                            apellido.isBlank() ? null : apellido,
                            usuarioActual.getId());
                }
                cerrar.run();
                mostrarTab(tabUsuarios, buildUsuariosPanel());
            } catch (BusinessException ex) {
                setModalError(lblModalError, ex.getMessage());
            } catch (Exception ex) {
                setModalError(lblModalError, "Error al guardar. Intenta de nuevo.");
            }
        });

        rootStack.getChildren().add(overlay);
        overlay.setOpacity(0);
        panelScroll.setScaleX(0.93);
        panelScroll.setScaleY(0.93);

        FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ScaleTransition st = new ScaleTransition(Duration.millis(220), panelScroll);
        st.setFromX(0.93); st.setToX(1.0); st.setFromY(0.93); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, st).play();
    }

    private void setModalError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }
}
