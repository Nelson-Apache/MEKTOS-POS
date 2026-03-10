package com.nap.pos.ui.controller;

import com.nap.pos.application.service.CajaService;
import com.nap.pos.application.service.CategoriaService;
import com.nap.pos.application.service.ClienteService;
import com.nap.pos.application.service.ProductoService;
import com.nap.pos.application.service.VentaService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Caja;
import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.model.Cliente;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.MetodoPago;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Construye y gestiona la vista de Ventas (pantalla POS).
 * Flujo:
 *  1. Catálogo de productos → agregar al carrito
 *  2. Carrito → "Continuar con el pago"
 *  3. Panel de pago → cliente, método, monto recibido → "Procesar Venta"
 */
@Component
@RequiredArgsConstructor
public class VentasController {

    private final VentaService     ventaService;
    private final ProductoService  productoService;
    private final CategoriaService categoriaService;
    private final ClienteService   clienteService;
    private final CajaService      cajaService;

    // ── Estado ────────────────────────────────────────────────────
    private Usuario        usuarioActual;
    private List<Producto> todosProductos = new ArrayList<>();
    private final List<ItemCarrito> carrito = new ArrayList<>();
    private Long   categoriaFiltro  = null;
    private String textoBusqueda    = "";

    // ── Refs UI (catálogo) ────────────────────────────────────────
    private FlowPane gridProductos;
    private VBox     contenedorCarrito;
    private Label    lblTotal;
    private Label    lblContadorItems;

    // ── Refs UI (paneles intercambiables) ────────────────────────
    private VBox vistaCarrito;
    private VBox vistaPago;

    // ── Refs UI (formulario de pago) ─────────────────────────────
    private ComboBox<Cliente> cboCliente;
    private Label             lblCreditoDisponible;
    private ToggleGroup       grupoMetodo;
    private ToggleButton      btnEfectivo;
    private ToggleButton      btnTransferencia;
    private ToggleButton      btnCredito;
    private VBox              seccionRecibido;
    private TextField         txtRecibido;
    private Label             lblCambio;
    private Label             lblTotalPago;
    private Label             lblErrorPago;

    // ── Modelo de carrito ─────────────────────────────────────────
    public static final class ItemCarrito {
        public final Producto producto;
        public int cantidad;
        public ItemCarrito(Producto p, int c) { producto = p; cantidad = c; }
    }

    // ─────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────

    public Node buildView(Usuario usuario) {
        this.usuarioActual = usuario;
        carrito.clear();
        categoriaFiltro = null;
        textoBusqueda   = "";

        loadProductos();
        List<Categoria> categorias = loadCategorias();
        List<Cliente>   clientes   = loadClientes();

        HBox root = new HBox();
        root.setFillHeight(true);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMaxHeight(Double.MAX_VALUE);

        VBox panelIzq = buildPanelIzq(categorias);
        HBox.setHgrow(panelIzq, Priority.ALWAYS);

        VBox panelDer = buildPanelDer(clientes);
        panelDer.setMinWidth(320);
        panelDer.setPrefWidth(340);
        panelDer.setMaxWidth(360);

        // Animar entrada de los dos paneles
        animarEntrada(panelIzq, 0);
        animarEntrada(panelDer, 60);

        root.getChildren().addAll(panelIzq, panelDer);
        return root;
    }

    // ─────────────────────────────────────────────────────────────
    // Carga de datos
    // ─────────────────────────────────────────────────────────────

    private void loadProductos() {
        try { todosProductos = productoService.findAllActivos(); }
        catch (Exception e) { todosProductos = new ArrayList<>(); }
    }

    private List<Categoria> loadCategorias() {
        try { return categoriaService.findAllActivas(); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    private List<Cliente> loadClientes() {
        try { return clienteService.findAllActivos(); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    // ─────────────────────────────────────────────────────────────
    // Panel izquierdo — catálogo
    // ─────────────────────────────────────────────────────────────

    private VBox buildPanelIzq(List<Categoria> categorias) {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("venta-panel-izq");
        VBox.setVgrow(panel, Priority.ALWAYS);

        panel.getChildren().addAll(
            buildSearchBar(),
            buildCatChips(categorias),
            buildGridScroll()
        );
        actualizarGrid();
        return panel;
    }

    private HBox buildSearchBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(18, 20, 12, 20));
        bar.getStyleClass().add("venta-topbar");

        // Título con ícono
        FontIcon storIco = new FontIcon("fas-store");
        storIco.setIconSize(16);
        storIco.setIconColor(Paint.valueOf("#5A6ACF"));

        Label lTit = new Label("Nueva Venta");
        lTit.getStyleClass().add("topbar-title");

        HBox titleGroup = new HBox(8, storIco, lTit);
        titleGroup.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Campo de búsqueda con ícono
        StackPane searchWrap = new StackPane();
        searchWrap.setAlignment(Pos.CENTER_LEFT);

        TextField field = new TextField();
        field.setPromptText("Buscar producto o código de barras...");
        field.getStyleClass().add("venta-search-field");
        field.setPrefWidth(290);

        FontIcon ico = new FontIcon("fas-search");
        ico.setIconSize(13);
        ico.setIconColor(Paint.valueOf("#A8A29E"));
        StackPane.setMargin(ico, new Insets(0, 0, 0, 11));

        searchWrap.getChildren().addAll(field, ico);

        field.textProperty().addListener((obs, o, n) -> {
            textoBusqueda = n == null ? "" : n.toLowerCase();
            actualizarGrid();
        });

        // Efecto focus en el campo de búsqueda
        field.focusedProperty().addListener((obs, o, focused) -> {
            ico.setIconColor(Paint.valueOf(focused ? "#5A6ACF" : "#A8A29E"));
        });

        bar.getChildren().addAll(titleGroup, spacer, searchWrap);
        return bar;
    }

    private ScrollPane buildCatChips(List<Categoria> categorias) {
        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.setPadding(new Insets(6, 20, 12, 20));

        // Chip "Todas" con ícono de cuadrícula
        chips.getChildren().add(crearChip("Todas", null, "fas-th", true, chips));
        for (Categoria c : categorias) {
            chips.getChildren().add(crearChip(c.getNombre(), c.getId(), c.getIcono(), false, chips));
        }

        ScrollPane sp = new ScrollPane(chips);
        sp.setFitToHeight(true);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("venta-cat-scroll");
        sp.setPrefHeight(50);
        sp.setMinHeight(50);
        sp.setMaxHeight(50);
        return sp;
    }

    private Button crearChip(String texto, Long catId, String iconoLiteral, boolean activo, HBox parent) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("venta-cat-chip");
        if (activo) btn.getStyleClass().add("venta-cat-chip-active");

        // Ícono: se muestra si hay literal válido
        if (iconoLiteral != null && !iconoLiteral.isBlank()) {
            FontIcon icon = new FontIcon(iconoLiteral);
            icon.setIconSize(12);
            // Color inicial según estado
            icon.setIconColor(Paint.valueOf(activo ? "#FFFFFF" : "#5A6ACF"));
            btn.setGraphic(icon);
            btn.setContentDisplay(ContentDisplay.LEFT);
            btn.setGraphicTextGap(5);

            // Sincronizar color del ícono con el estado activo/inactivo del chip
            btn.getStyleClass().addListener(
                (javafx.collections.ListChangeListener<String>) change ->
                    icon.setIconColor(Paint.valueOf(
                        btn.getStyleClass().contains("venta-cat-chip-active") ? "#FFFFFF" : "#5A6ACF"))
            );
        }

        btn.setOnAction(e -> {
            categoriaFiltro = catId;
            parent.getChildren().forEach(n -> {
                if (n instanceof Button b) b.getStyleClass().remove("venta-cat-chip-active");
            });
            btn.getStyleClass().add("venta-cat-chip-active");
            actualizarGrid();
        });

        // Efecto hover: escala suave
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyleClass().contains("venta-cat-chip-active")) {
                ScaleTransition sc = new ScaleTransition(Duration.millis(100), btn);
                sc.setToX(1.05); sc.setToY(1.05);
                sc.play();
            }
        });
        btn.setOnMouseExited(e -> {
            ScaleTransition sc = new ScaleTransition(Duration.millis(80), btn);
            sc.setToX(1.0); sc.setToY(1.0);
            sc.play();
        });

        return btn;
    }

    private ScrollPane buildGridScroll() {
        gridProductos = new FlowPane();
        gridProductos.setHgap(14);
        gridProductos.setVgap(14);
        gridProductos.setPadding(new Insets(16, 20, 20, 20));

        ScrollPane sp = new ScrollPane(gridProductos);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().add("venta-grid-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private void actualizarGrid() {
        gridProductos.getChildren().clear();
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

        List<Producto> filtrados = todosProductos.stream()
            .filter(p -> {
                if (categoriaFiltro != null) {
                    if (p.getSubcategoria() == null
                            || p.getSubcategoria().getCategoria() == null
                            || !categoriaFiltro.equals(p.getSubcategoria().getCategoria().getId()))
                        return false;
                }
                if (!textoBusqueda.isEmpty()) {
                    boolean matchN = p.getNombre() != null
                            && p.getNombre().toLowerCase().contains(textoBusqueda);
                    boolean matchB = p.getCodigoBarras() != null
                            && p.getCodigoBarras().toLowerCase().contains(textoBusqueda);
                    return matchN || matchB;
                }
                return true;
            })
            .collect(Collectors.toList());

        if (filtrados.isEmpty()) {
            Label lV = new Label("No se encontraron productos.");
            lV.getStyleClass().add("dashboard-empty");
            lV.setPadding(new Insets(24, 0, 0, 0));
            gridProductos.getChildren().add(lV);
            return;
        }

        // Entrada staggered: cada card con 40ms de delay adicional (máx 10 cards animadas)
        int idx = 0;
        for (Producto p : filtrados) {
            VBox card = crearProductoCard(p, fmt);
            gridProductos.getChildren().add(card);
            if (idx < 16) {
                animarEntrada(card, idx * 35);
            }
            idx++;
        }
    }

    private VBox crearProductoCard(Producto p, NumberFormat fmt) {
        VBox card = new VBox(8);
        card.getStyleClass().add("venta-producto-card");
        card.setPrefWidth(158);
        card.setMaxWidth(158);

        boolean sinStock = p.getStock() == 0;
        if (sinStock) {
            card.setOpacity(0.50);
        } else {
            card.setCursor(Cursor.HAND);
            aplicarHoverCard(card);
        }

        // Área de ícono con inicial y badge de stock
        StackPane iconArea = new StackPane();
        iconArea.getStyleClass().add("venta-producto-icon");
        iconArea.setPrefHeight(76);
        iconArea.setMinHeight(76);

        String ini = (p.getNombre() != null && !p.getNombre().isBlank())
                ? p.getNombre().substring(0, 1).toUpperCase() : "?";
        Label lIni = new Label(ini);
        lIni.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: #5A6ACF;");

        Label lStock = new Label(sinStock ? "Agotado" : String.valueOf(p.getStock()));
        lStock.getStyleClass().addAll("venta-stock-badge", sinStock ? "venta-stock-agotado" : "venta-stock-ok");
        StackPane.setAlignment(lStock, Pos.TOP_RIGHT);
        StackPane.setMargin(lStock, new Insets(6, 6, 0, 0));

        iconArea.getChildren().addAll(lIni, lStock);

        // Nombre
        Label lNombre = new Label(p.getNombre());
        lNombre.getStyleClass().add("venta-producto-nombre");
        lNombre.setWrapText(true);
        lNombre.setMaxWidth(Double.MAX_VALUE);

        // Subcategoría (si tiene)
        if (p.getSubcategoria() != null) {
            Label lSub = new Label(p.getSubcategoria().getNombre());
            lSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #A8A29E; -fx-padding: -4 0 0 0;");
            card.getChildren().addAll(iconArea, lNombre, lSub);
        } else {
            card.getChildren().addAll(iconArea, lNombre);
        }

        // Fila precio + botón agregar
        HBox priceRow = new HBox();
        priceRow.setAlignment(Pos.CENTER_LEFT);

        String precioStr = fmt.format(p.getPrecioVenta() != null ? p.getPrecioVenta() : BigDecimal.ZERO);
        Label lPrecio = new Label(precioStr);
        lPrecio.getStyleClass().add("venta-producto-precio");
        HBox.setHgrow(lPrecio, Priority.ALWAYS);

        Button btnAdd = new Button();
        FontIcon addIco = new FontIcon("fas-plus");
        addIco.setIconSize(10);
        addIco.setIconColor(Paint.valueOf("#FFFFFF"));
        btnAdd.setGraphic(addIco);
        btnAdd.getStyleClass().add("venta-btn-add");
        btnAdd.setDisable(sinStock);
        btnAdd.setOnAction(e -> {
            e.consume();
            animarBotonAdd(btnAdd);
            agregarAlCarrito(p);
        });

        priceRow.getChildren().addAll(lPrecio, btnAdd);
        card.getChildren().add(priceRow);

        if (!sinStock) {
            card.setOnMouseClicked(e -> agregarAlCarrito(p));
        }
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // Panel derecho — carrito + pago
    // ─────────────────────────────────────────────────────────────

    private VBox buildPanelDer(List<Cliente> clientes) {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("venta-panel-der");
        VBox.setVgrow(panel, Priority.ALWAYS);

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 14, 20));
        header.setStyle("-fx-border-color: transparent transparent rgba(26,31,46,0.08) transparent; -fx-border-width: 0 0 1 0;");

        FontIcon ico = new FontIcon("fas-shopping-cart");
        ico.setIconSize(15);
        ico.setIconColor(Paint.valueOf("#1A1F2E"));

        Label lTit = new Label("Mi carrito");
        lTit.getStyleClass().add("dashboard-section-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);

        lblContadorItems = new Label("0 items");
        lblContadorItems.getStyleClass().add("venta-cart-count");

        header.getChildren().addAll(ico, lTit, lblContadorItems);

        vistaCarrito = buildVistaCarrito();
        VBox.setVgrow(vistaCarrito, Priority.ALWAYS);

        vistaPago = buildVistaPago(clientes);
        vistaPago.setVisible(false);
        vistaPago.setManaged(false);
        VBox.setVgrow(vistaPago, Priority.ALWAYS);

        panel.getChildren().addAll(header, vistaCarrito, vistaPago);
        return panel;
    }

    private VBox buildVistaCarrito() {
        VBox v = new VBox(0);
        VBox.setVgrow(v, Priority.ALWAYS);

        contenedorCarrito = new VBox(0);

        ScrollPane sc = new ScrollPane(contenedorCarrito);
        sc.setFitToWidth(true);
        sc.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sc.getStyleClass().add("venta-cart-scroll");
        VBox.setVgrow(sc, Priority.ALWAYS);

        // Footer con total y botón continuar
        VBox footer = new VBox(10);
        footer.setPadding(new Insets(14, 20, 20, 20));
        footer.setStyle("-fx-border-color: rgba(26,31,46,0.08) transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label lTLbl = new Label("Total");
        lTLbl.getStyleClass().add("venta-total-label");
        HBox.setHgrow(lTLbl, Priority.ALWAYS);
        lblTotal = new Label("$0");
        lblTotal.getStyleClass().add("venta-total-valor");
        totalRow.getChildren().addAll(lTLbl, lblTotal);

        // Botón Continuar con ícono y hover effect
        FontIcon arrowIco = new FontIcon("fas-arrow-right");
        arrowIco.setIconSize(12);
        arrowIco.setIconColor(Paint.valueOf("#FFFFFF"));

        Button btnCont = new Button("Continuar con el pago");
        btnCont.setGraphic(arrowIco);
        btnCont.setContentDisplay(ContentDisplay.RIGHT);
        btnCont.setGraphicTextGap(8);
        btnCont.getStyleClass().add("btn-primario");
        btnCont.setMaxWidth(Double.MAX_VALUE);
        btnCont.setOnAction(e -> mostrarVistaPago());
        aplicarHoverBtn(btnCont, "#5A6ACF", "#4A58BF");

        footer.getChildren().addAll(totalRow, btnCont);
        v.getChildren().addAll(sc, footer);

        actualizarVistaCarrito();
        return v;
    }

    private VBox buildVistaPago(List<Cliente> clientes) {
        VBox v = new VBox(14);
        v.setPadding(new Insets(16, 20, 20, 20));
        VBox.setVgrow(v, Priority.ALWAYS);

        // Volver al carrito
        FontIcon backIco = new FontIcon("fas-arrow-left");
        backIco.setIconSize(11);
        backIco.setIconColor(Paint.valueOf("#5A6ACF"));

        Button btnVolver = new Button("Volver al carrito");
        btnVolver.setGraphic(backIco);
        btnVolver.setContentDisplay(ContentDisplay.LEFT);
        btnVolver.setGraphicTextGap(6);
        btnVolver.getStyleClass().add("btn-enlace");
        btnVolver.setStyle("-fx-text-fill: #5A6ACF; -fx-font-weight: 600; -fx-font-size: 12px;");
        btnVolver.setOnAction(e -> mostrarVistaCarrito());
        btnVolver.setOnMouseEntered(e -> backIco.setIconColor(Paint.valueOf("#3C48AA")));
        btnVolver.setOnMouseExited(e -> backIco.setIconColor(Paint.valueOf("#5A6ACF")));

        // ── Cliente ──────────────────────────────────────────────
        HBox lClienteRow = crearLabelConIcono("fas-user", "Cliente (opcional)");

        cboCliente = new ComboBox<>();
        cboCliente.setPromptText("Consumidor final");
        cboCliente.setMaxWidth(Double.MAX_VALUE);
        cboCliente.getItems().add(null);
        cboCliente.getItems().addAll(clientes);
        cboCliente.setConverter(new StringConverter<Cliente>() {
            @Override public String toString(Cliente c) {
                return c == null ? "Sin cliente" : c.getNombre() + "  ·  " + c.getCedula();
            }
            @Override public Cliente fromString(String s) { return null; }
        });

        lblCreditoDisponible = new Label();
        lblCreditoDisponible.getStyleClass().add("login-info");
        lblCreditoDisponible.setVisible(false);
        lblCreditoDisponible.setManaged(false);
        cboCliente.valueProperty().addListener((obs, o, n) -> actualizarInfoCredito(n));

        VBox clienteBox = new VBox(6, lClienteRow, cboCliente, lblCreditoDisponible);

        // ── Separador visual ─────────────────────────────────────
        Separator sep1 = new Separator();

        // ── Método de pago ───────────────────────────────────────
        HBox lMetodoRow = crearLabelConIcono("fas-wallet", "Método de pago");

        grupoMetodo = new ToggleGroup();
        btnEfectivo      = crearToggleMetodo("Efectivo",      MetodoPago.EFECTIVO,      "fas-money-bill-wave");
        btnTransferencia = crearToggleMetodo("Transferencia", MetodoPago.TRANSFERENCIA, "fas-exchange-alt");
        btnCredito       = crearToggleMetodo("Crédito",       MetodoPago.CREDITO,       "fas-credit-card");
        btnEfectivo.setSelected(true);

        HBox metodosRow = new HBox(6);
        metodosRow.getChildren().addAll(btnEfectivo, btnTransferencia, btnCredito);
        grupoMetodo.selectedToggleProperty().addListener((obs, o, n) -> onMetodoCambio());

        VBox metodoBox = new VBox(6, lMetodoRow, metodosRow);

        // ── Monto recibido (solo efectivo) ───────────────────────
        HBox lRecibidoRow = crearLabelConIcono("fas-coins", "Monto recibido");

        txtRecibido = new TextField();
        txtRecibido.setPromptText("0.00");
        txtRecibido.textProperty().addListener((obs, o, n) -> calcularCambio());

        HBox cambioRow = new HBox();
        cambioRow.setAlignment(Pos.CENTER_LEFT);
        Label lCLbl = new Label("Cambio");
        lCLbl.getStyleClass().add("form-label");
        HBox.setHgrow(lCLbl, Priority.ALWAYS);
        lblCambio = new Label("—");
        lblCambio.getStyleClass().add("venta-cambio-valor");
        cambioRow.getChildren().addAll(lCLbl, lblCambio);

        seccionRecibido = new VBox(10,
            new VBox(6, lRecibidoRow, txtRecibido),
            cambioRow
        );

        // ── Resumen total ────────────────────────────────────────
        Separator sep2 = new Separator();

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle("-fx-background-color: rgba(90,106,207,0.06); -fx-background-radius: 8px; -fx-padding: 10 14 10 14;");
        Label lTLbl = new Label("Total a cobrar");
        lTLbl.getStyleClass().add("venta-total-label");
        HBox.setHgrow(lTLbl, Priority.ALWAYS);
        lblTotalPago = new Label("$0");
        lblTotalPago.getStyleClass().add("venta-total-valor");
        lblTotalPago.setStyle("-fx-text-fill: #5A6ACF;");
        totalRow.getChildren().addAll(lTLbl, lblTotalPago);

        // ── Error y botón procesar ───────────────────────────────
        lblErrorPago = new Label();
        lblErrorPago.getStyleClass().add("login-error");
        lblErrorPago.setWrapText(true);
        lblErrorPago.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 7px; -fx-padding: 8 12 8 12;");
        lblErrorPago.setVisible(false);
        lblErrorPago.setManaged(false);

        FontIcon checkIco = new FontIcon("fas-check");
        checkIco.setIconSize(12);
        checkIco.setIconColor(Paint.valueOf("#FFFFFF"));

        Button btnProcesar = new Button("Procesar Venta");
        btnProcesar.setGraphic(checkIco);
        btnProcesar.setContentDisplay(ContentDisplay.RIGHT);
        btnProcesar.setGraphicTextGap(8);
        btnProcesar.getStyleClass().add("btn-primario");
        btnProcesar.setMaxWidth(Double.MAX_VALUE);
        btnProcesar.setStyle("-fx-padding: 12 28 12 28;");
        btnProcesar.setOnAction(e -> procesarVenta());
        aplicarHoverBtn(btnProcesar, "#5A6ACF", "#4A58BF");

        v.getChildren().addAll(
            btnVolver,
            clienteBox,
            sep1,
            metodoBox,
            seccionRecibido,
            sep2,
            totalRow,
            lblErrorPago,
            btnProcesar
        );
        return v;
    }

    // ─────────────────────────────────────────────────────────────
    // Operaciones de carrito
    // ─────────────────────────────────────────────────────────────

    private void agregarAlCarrito(Producto p) {
        carrito.stream()
            .filter(i -> i.producto.getId().equals(p.getId()))
            .findFirst()
            .ifPresentOrElse(
                i -> i.cantidad++,
                () -> carrito.add(new ItemCarrito(p, 1))
            );
        pulsarContador();
        actualizarVistaCarrito();
    }

    private void cambiarCantidad(Long pId, int delta) {
        carrito.stream()
            .filter(i -> i.producto.getId().equals(pId))
            .findFirst()
            .ifPresent(item -> {
                item.cantidad += delta;
                if (item.cantidad <= 0) carrito.remove(item);
            });
        actualizarVistaCarrito();
    }

    private void quitarItem(Long pId) {
        carrito.removeIf(i -> i.producto.getId().equals(pId));
        actualizarVistaCarrito();
    }

    private void actualizarVistaCarrito() {
        contenedorCarrito.getChildren().clear();
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

        if (carrito.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(36, 16, 16, 16));

            FontIcon cartIco = new FontIcon("fas-shopping-cart");
            cartIco.setIconSize(32);
            cartIco.setIconColor(Paint.valueOf("#D1D5DB"));

            Label lV = new Label("Carrito vacío");
            lV.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #A8A29E;");
            Label lHint = new Label("Agrega productos de la lista");
            lHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #C4C0BB;");

            emptyState.getChildren().addAll(cartIco, lV, lHint);
            animarEntrada(emptyState, 0);
            contenedorCarrito.getChildren().add(emptyState);
        } else {
            int idx = 0;
            for (ItemCarrito item : carrito) {
                HBox row = crearCartItem(item, fmt);
                contenedorCarrito.getChildren().add(row);
                animarCartItem(row, idx * 30);
                idx++;
            }
        }

        BigDecimal total = calcularTotal();
        lblTotal.setText(fmt.format(total));

        int n = carrito.stream().mapToInt(i -> i.cantidad).sum();
        lblContadorItems.setText(n + (n == 1 ? " item" : " items"));
    }

    private HBox crearCartItem(ItemCarrito item, NumberFormat fmt) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("venta-cart-item");
        row.setPadding(new Insets(10, 20, 10, 20));

        // Hover sobre la fila
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(90,106,207,0.04);"));
        row.setOnMouseExited(e -> row.setStyle(""));

        // Avatar circular con inicial
        StackPane av = new StackPane();
        av.getStyleClass().add("venta-cart-avatar");
        av.setMinWidth(34); av.setMinHeight(34);
        av.setMaxWidth(34); av.setMaxHeight(34);
        String ini = (item.producto.getNombre() != null && !item.producto.getNombre().isBlank())
                ? item.producto.getNombre().substring(0, 1).toUpperCase() : "?";
        Label lI = new Label(ini);
        lI.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #5A6ACF;");
        av.getChildren().add(lI);

        // Nombre y precio unitario
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lN = new Label(item.producto.getNombre());
        lN.getStyleClass().add("venta-cart-nombre");
        BigDecimal unitPrice = item.producto.getPrecioVenta() != null
                ? item.producto.getPrecioVenta() : BigDecimal.ZERO;
        Label lP = new Label(fmt.format(unitPrice));
        lP.getStyleClass().add("venta-cart-precio-unit");
        info.getChildren().addAll(lN, lP);

        // Controles de cantidad con efecto
        HBox qty = new HBox(5);
        qty.setAlignment(Pos.CENTER);

        Button bMinus = new Button("−");
        bMinus.getStyleClass().add("venta-qty-btn");
        bMinus.setOnAction(e -> { animarQtyBtn(bMinus); cambiarCantidad(item.producto.getId(), -1); });

        Label lQty = new Label(String.valueOf(item.cantidad));
        lQty.getStyleClass().add("venta-qty-label");
        lQty.setMinWidth(24);
        lQty.setAlignment(Pos.CENTER);

        Button bPlus = new Button("+");
        bPlus.getStyleClass().add("venta-qty-btn");
        bPlus.setOnAction(e -> { animarQtyBtn(bPlus); cambiarCantidad(item.producto.getId(), +1); });

        qty.getChildren().addAll(bMinus, lQty, bPlus);

        // Subtotal
        BigDecimal sub = unitPrice.multiply(BigDecimal.valueOf(item.cantidad));
        Label lSub = new Label(fmt.format(sub));
        lSub.getStyleClass().add("venta-cart-subtotal");

        // Botón eliminar con animación
        Button bDel = new Button();
        FontIcon dIco = new FontIcon("fas-trash-alt");
        dIco.setIconSize(11);
        dIco.setIconColor(Paint.valueOf("#DC2626"));
        bDel.setGraphic(dIco);
        bDel.getStyleClass().add("venta-btn-del");
        bDel.setOnMouseEntered(e -> dIco.setIconColor(Paint.valueOf("#B91C1C")));
        bDel.setOnMouseExited(e -> dIco.setIconColor(Paint.valueOf("#DC2626")));
        bDel.setOnAction(e -> {
            animarEliminacion(row, () -> quitarItem(item.producto.getId()));
        });

        row.getChildren().addAll(av, info, qty, lSub, bDel);
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // Navegación entre vistas
    // ─────────────────────────────────────────────────────────────

    private void mostrarVistaPago() {
        if (carrito.isEmpty()) return;
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
        lblTotalPago.setText(fmt.format(calcularTotal()));
        lblErrorPago.setVisible(false);
        lblErrorPago.setManaged(false);

        // Transición: fade out carrito → fade in pago
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), vistaCarrito);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            vistaCarrito.setVisible(false);
            vistaCarrito.setManaged(false);
            vistaCarrito.setOpacity(1);
            vistaPago.setOpacity(0);
            vistaPago.setVisible(true);
            vistaPago.setManaged(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), vistaPago);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
        onMetodoCambio();
    }

    private void mostrarVistaCarrito() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), vistaPago);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            vistaPago.setVisible(false);
            vistaPago.setManaged(false);
            vistaPago.setOpacity(1);
            vistaCarrito.setOpacity(0);
            vistaCarrito.setVisible(true);
            vistaCarrito.setManaged(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), vistaCarrito);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    // ─────────────────────────────────────────────────────────────
    // Lógica del formulario de pago
    // ─────────────────────────────────────────────────────────────

    private void actualizarInfoCredito(Cliente c) {
        if (c != null && c.tieneCreditoHabilitado()) {
            NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
            lblCreditoDisponible.setText("✓  Crédito disponible: " + fmt.format(c.getSaldoDisponible()));
            lblCreditoDisponible.setVisible(true);
            lblCreditoDisponible.setManaged(true);
            animarEntrada(lblCreditoDisponible, 0);
        } else {
            lblCreditoDisponible.setVisible(false);
            lblCreditoDisponible.setManaged(false);
        }
    }

    private void onMetodoCambio() {
        boolean esEfectivo = btnEfectivo != null && btnEfectivo.isSelected();
        if (seccionRecibido != null) {
            if (esEfectivo && !seccionRecibido.isVisible()) {
                seccionRecibido.setVisible(true);
                seccionRecibido.setManaged(true);
                animarEntrada(seccionRecibido, 0);
            } else if (!esEfectivo) {
                seccionRecibido.setVisible(false);
                seccionRecibido.setManaged(false);
            }
        }
        if (esEfectivo) calcularCambio();
    }

    private void calcularCambio() {
        try {
            BigDecimal total = calcularTotal();
            String txt = txtRecibido.getText().trim().replace(",", ".");
            if (txt.isEmpty()) { lblCambio.setText("—"); lblCambio.setStyle(""); return; }
            BigDecimal recibido = new BigDecimal(txt);
            BigDecimal cambio = recibido.subtract(total);
            NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
            if (cambio.compareTo(BigDecimal.ZERO) >= 0) {
                lblCambio.setText(fmt.format(cambio));
                lblCambio.setStyle("-fx-text-fill: #15803D; -fx-font-weight: 700; -fx-font-size: 15px;");
            } else {
                lblCambio.setText("Faltan " + fmt.format(cambio.negate()));
                lblCambio.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: 700; -fx-font-size: 13px;");
            }
        } catch (NumberFormatException ignored) {
            lblCambio.setText("—");
            lblCambio.setStyle("");
        }
    }

    private ToggleButton crearToggleMetodo(String texto, MetodoPago metodo, String iconLiteral) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(13);
        icon.setIconColor(Paint.valueOf("#4B5563"));

        ToggleButton tb = new ToggleButton(texto);
        tb.setGraphic(icon);
        tb.setContentDisplay(ContentDisplay.LEFT);
        tb.setGraphicTextGap(6);
        tb.setToggleGroup(grupoMetodo);
        tb.getStyleClass().add("venta-metodo-btn");
        tb.setUserData(metodo);

        // Cambiar color del ícono según el estado seleccionado
        tb.selectedProperty().addListener((obs, o, selected) -> {
            icon.setIconColor(Paint.valueOf(selected ? "#FFFFFF" : "#4B5563"));
        });

        return tb;
    }

    // ─────────────────────────────────────────────────────────────
    // Procesamiento de venta
    // ─────────────────────────────────────────────────────────────

    private void procesarVenta() {
        lblErrorPago.setVisible(false);
        lblErrorPago.setManaged(false);

        if (carrito.isEmpty()) {
            mostrarError("El carrito no tiene productos.");
            return;
        }

        Toggle toggle = grupoMetodo.getSelectedToggle();
        if (toggle == null) {
            mostrarError("Selecciona un método de pago.");
            return;
        }

        MetodoPago metodo  = (MetodoPago) toggle.getUserData();
        Cliente    cliente = cboCliente.getValue();

        if (MetodoPago.CREDITO.equals(metodo) && cliente == null) {
            mostrarError("Las ventas a crédito requieren un cliente seleccionado.");
            return;
        }

        if (MetodoPago.EFECTIVO.equals(metodo)) {
            try {
                String recibidoStr = txtRecibido.getText().trim().replace(",", ".");
                if (recibidoStr.isEmpty()) {
                    mostrarError("Ingresa el monto recibido del cliente.");
                    return;
                }
                BigDecimal recibido = new BigDecimal(recibidoStr);
                if (recibido.compareTo(calcularTotal()) < 0) {
                    mostrarError("El monto recibido es menor al total de la venta.");
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarError("El monto recibido no es un número válido.");
                return;
            }
        }

        try {
            Caja caja = cajaService.getCajaAbierta();

            List<VentaService.ItemVenta> items = carrito.stream()
                .map(i -> new VentaService.ItemVenta(i.producto.getId(), i.cantidad))
                .collect(Collectors.toList());

            ventaService.registrarVenta(
                caja.getId(),
                usuarioActual.getId(),
                cliente != null ? cliente.getId() : null,
                metodo,
                items
            );

            // Éxito — reiniciar estado
            carrito.clear();
            loadProductos();
            actualizarVistaCarrito();
            actualizarGrid();
            mostrarVistaCarrito();
            cboCliente.setValue(null);
            txtRecibido.clear();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Venta registrada");
            ok.setHeaderText(null);
            ok.setContentText("¡Venta procesada exitosamente!");
            ok.showAndWait();

        } catch (BusinessException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error inesperado: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers de UI
    // ─────────────────────────────────────────────────────────────

    /** Label con ícono FontIcon a la izquierda, estilo form-label */
    private HBox crearLabelConIcono(String iconLiteral, String texto) {
        FontIcon ico = new FontIcon(iconLiteral);
        ico.setIconSize(12);
        ico.setIconColor(Paint.valueOf("#5A6ACF"));

        Label lbl = new Label(texto);
        lbl.getStyleClass().add("form-label");

        HBox row = new HBox(6, ico, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // Animaciones
    // ─────────────────────────────────────────────────────────────

    /** Fade + slide-up de entrada, con delay opcional */
    private void animarEntrada(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(14);

        FadeTransition fade = new FadeTransition(Duration.millis(280), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(280), node);
        slide.setFromY(14);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    /** Efecto hover de escala en tarjetas de producto */
    private void aplicarHoverCard(VBox card) {
        DropShadow shadowBase = new DropShadow(6, 0, 2, Color.color(0, 0, 0, 0.05));
        DropShadow shadowHover = new DropShadow(14, 0, 4, Color.color(0.35, 0.42, 0.81, 0.22));
        card.setEffect(shadowBase);

        card.setOnMouseEntered(e -> {
            ScaleTransition sc = new ScaleTransition(Duration.millis(130), card);
            sc.setToX(1.04); sc.setToY(1.04);
            sc.setInterpolator(Interpolator.EASE_OUT);
            sc.play();
            card.setEffect(shadowHover);
        });
        card.setOnMouseExited(e -> {
            ScaleTransition sc = new ScaleTransition(Duration.millis(110), card);
            sc.setToX(1.0); sc.setToY(1.0);
            sc.setInterpolator(Interpolator.EASE_OUT);
            sc.play();
            card.setEffect(shadowBase);
        });
        card.setOnMousePressed(e -> {
            ScaleTransition sc = new ScaleTransition(Duration.millis(60), card);
            sc.setToX(0.97); sc.setToY(0.97);
            sc.play();
        });
        card.setOnMouseReleased(e -> {
            ScaleTransition sc = new ScaleTransition(Duration.millis(80), card);
            sc.setToX(1.04); sc.setToY(1.04);
            sc.play();
        });
    }

    /** Pulsa el contador del carrito al agregar un ítem */
    private void pulsarContador() {
        ScaleTransition sc = new ScaleTransition(Duration.millis(120), lblContadorItems);
        sc.setFromX(1.0); sc.setFromY(1.0);
        sc.setToX(1.35); sc.setToY(1.35);
        sc.setAutoReverse(true);
        sc.setCycleCount(2);
        sc.setInterpolator(Interpolator.EASE_BOTH);
        sc.play();
    }

    /** Animación de escala en el botón "+" al hacer clic */
    private void animarBotonAdd(Button btn) {
        ScaleTransition sc = new ScaleTransition(Duration.millis(100), btn);
        sc.setFromX(1.0); sc.setFromY(1.0);
        sc.setToX(1.45); sc.setToY(1.45);
        sc.setAutoReverse(true);
        sc.setCycleCount(2);
        sc.setInterpolator(Interpolator.EASE_BOTH);
        sc.play();
    }

    /** Slide-in desde la derecha para ítems del carrito */
    private void animarCartItem(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateX(24);

        FadeTransition fade = new FadeTransition(Duration.millis(200), node);
        fade.setFromValue(0); fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), node);
        slide.setFromX(24); slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    /** Fade-out + slide a la derecha antes de eliminar un ítem */
    private void animarEliminacion(Node node, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(160), node);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition slide = new TranslateTransition(Duration.millis(160), node);
        slide.setToX(30);
        slide.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setOnFinished(e -> onFinished.run());
        pt.play();
    }

    /** Pulso en botones −/+ */
    private void animarQtyBtn(Button btn) {
        ScaleTransition sc = new ScaleTransition(Duration.millis(80), btn);
        sc.setFromX(1.0); sc.setFromY(1.0);
        sc.setToX(1.25); sc.setToY(1.25);
        sc.setAutoReverse(true);
        sc.setCycleCount(2);
        sc.play();
    }

    /** Hover suave en botones primarios interpolando color de fondo */
    private void aplicarHoverBtn(Button btn, String colorBase, String colorHover) {
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle()
            + " -fx-background-color: " + colorHover + ";"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle()
            .replace(" -fx-background-color: " + colorHover + ";", "")));
    }

    // ─────────────────────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────────────────────

    private BigDecimal calcularTotal() {
        return carrito.stream()
            .map(i -> i.producto.getPrecioVenta() != null
                ? i.producto.getPrecioVenta().multiply(BigDecimal.valueOf(i.cantidad))
                : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void mostrarError(String msg) {
        lblErrorPago.setText(msg);
        lblErrorPago.setVisible(true);
        lblErrorPago.setManaged(true);
        animarEntrada(lblErrorPago, 0);
    }
}
