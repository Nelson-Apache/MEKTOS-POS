package com.nap.pos.ui.controller;

import com.nap.pos.application.service.ConfiguracionService;
import com.nap.pos.application.service.UsuarioService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.RegimenTributario;
import com.nap.pos.domain.model.enums.TipoPersona;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
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
    private Button tabDatos, tabTributario, tabComprobantes, tabInventario, tabSeguridad;
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(tabDatos, tabTributario, tabComprobantes, tabInventario, tabSeguridad, spacer);
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
        List<Button> todos = List.of(tabDatos, tabTributario, tabComprobantes, tabInventario, tabSeguridad);
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
}
