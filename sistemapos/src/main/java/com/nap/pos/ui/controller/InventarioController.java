package com.nap.pos.ui.controller;

import com.nap.pos.application.service.CategoriaService;
import com.nap.pos.application.service.ProductoService;
import com.nap.pos.application.service.ProveedorService;
import com.nap.pos.application.service.SubcategoriaService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.Proveedor;
import com.nap.pos.domain.model.Subcategoria;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventarioController {

    private final ProductoService     productoService;
    private final CategoriaService    categoriaService;
    private final SubcategoriaService subcategoriaService;
    private final ProveedorService    proveedorService;

    private List<Producto> todosProductos = new ArrayList<>();
    private List<Producto> productosFiltrados = new ArrayList<>();
    private StackPane      rootStack;
    private VBox           contentArea;
    private Button         tabResumen;
    private Button         tabProductos;
    private TableView<Producto> tablaProductos;
    private Label          lblTotalFiltrados;

    private static final NumberFormat FMT_MONEDA =
            NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    // ══════════════════════════════════════════════════════════════
    //  Punto de entrada
    // ══════════════════════════════════════════════════════════════

    public Node buildView() {
        recargarDatos();

        rootStack = new StackPane();
        rootStack.getStyleClass().add("inventario-root-stack");

        VBox root = new VBox(0);
        root.getStyleClass().add("inventario-root");

        HBox tabBar = buildTabBar();

        contentArea = new VBox();
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        root.getChildren().addAll(tabBar, contentArea);
        rootStack.getChildren().add(root);

        mostrarResumen();
        return rootStack;
    }

    // ══════════════════════════════════════════════════════════════
    //  Tab bar
    // ══════════════════════════════════════════════════════════════

    private HBox buildTabBar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("inventario-tab-bar");
        bar.setPadding(new Insets(14, 28, 10, 28));

        FontIcon icoResumen = new FontIcon("fas-chart-pie");
        icoResumen.setIconSize(14);
        icoResumen.setIconColor(Paint.valueOf("#5A6ACF"));

        tabResumen = new Button("Resumen", icoResumen);
        tabResumen.getStyleClass().addAll("inventario-tab", "inventario-tab-active");
        tabResumen.setOnAction(e -> {
            animarClickTab(tabResumen);
            mostrarResumen();
        });

        FontIcon icoProductos = new FontIcon("fas-boxes");
        icoProductos.setIconSize(14);
        icoProductos.setIconColor(Paint.valueOf("#78716C"));

        tabProductos = new Button("Productos", icoProductos);
        tabProductos.getStyleClass().add("inventario-tab");
        tabProductos.setOnAction(e -> {
            animarClickTab(tabProductos);
            mostrarProductos();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(tabResumen, tabProductos, spacer);
        return bar;
    }

    private void activarTab(Button activo) {
        tabResumen.getStyleClass().remove("inventario-tab-active");
        tabProductos.getStyleClass().remove("inventario-tab-active");
        activo.getStyleClass().add("inventario-tab-active");

        // Actualizar colores de iconos
        FontIcon icoRes = (FontIcon) tabResumen.getGraphic();
        FontIcon icoProd = (FontIcon) tabProductos.getGraphic();
        if (activo == tabResumen) {
            icoRes.setIconColor(Paint.valueOf("#5A6ACF"));
            icoProd.setIconColor(Paint.valueOf("#78716C"));
        } else {
            icoRes.setIconColor(Paint.valueOf("#78716C"));
            icoProd.setIconColor(Paint.valueOf("#5A6ACF"));
        }
    }

    private void animarClickTab(Button tab) {
        ScaleTransition press = new ScaleTransition(Duration.millis(80), tab);
        press.setToX(0.95);
        press.setToY(0.95);
        press.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition release = new ScaleTransition(Duration.millis(120), tab);
        release.setToX(1.0);
        release.setToY(1.0);
        release.setInterpolator(Interpolator.EASE_OUT);

        press.setOnFinished(e -> release.play());
        press.play();
    }

    // ══════════════════════════════════════════════════════════════
    //  PANTALLA 1 — Dashboard de Inventario
    // ══════════════════════════════════════════════════════════════

    private void mostrarResumen() {
        activarTab(tabResumen);
        recargarDatos();

        VBox view = new VBox(20);
        view.setPadding(new Insets(24, 28, 28, 28));

        // ── Fila 1: 4 stat cards ─────────────────────────────────
        int total      = todosProductos.size();
        int activos    = (int) todosProductos.stream().filter(Producto::isActivo).count();
        int sinStock   = (int) todosProductos.stream()
                .filter(Producto::isActivo).filter(p -> p.getStock() == 0).count();
        BigDecimal valorTotal = todosProductos.stream()
                .filter(Producto::isActivo)
                .map(p -> {
                    BigDecimal pv = p.getPrecioVenta() != null ? p.getPrecioVenta() : BigDecimal.ZERO;
                    return pv.multiply(BigDecimal.valueOf(p.getStock()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        HBox cardsRow = new HBox(14);
        cardsRow.setAlignment(Pos.TOP_LEFT);

        VBox c1 = crearStatCard("TOTAL PRODUCTOS", String.valueOf(total),
                "Activos e inactivos", null, null,
                "fas-boxes", "#5A6ACF", "rgba(90,106,207,0.10)");

        VBox c2 = crearStatCard("PRODUCTOS ACTIVOS", String.valueOf(activos),
                "Disponibles en ventas", null, null,
                "fas-check-circle", "#16A34A", "#DCFCE7");

        VBox c3 = crearStatCard("SIN STOCK", String.valueOf(sinStock),
                null,
                sinStock > 0 ? "Requiere atención" : "Todo OK",
                sinStock > 0 ? "dash-badge-danger" : "dash-badge-success",
                "fas-exclamation-triangle",
                sinStock > 0 ? "#DC2626" : "#16A34A",
                sinStock > 0 ? "#FEE2E2" : "#DCFCE7");

        VBox c4 = crearStatCard("VALOR INVENTARIO", FMT_MONEDA.format(valorTotal),
                "Precio venta × stock", null, null,
                "fas-dollar-sign", "#D97706", "#FEF3C7");

        for (VBox c : List.of(c1, c2, c3, c4)) {
            HBox.setHgrow(c, Priority.ALWAYS);
            cardsRow.getChildren().add(c);
        }

        // ── Fila 2: Chart + Stock bajo ───────────────────────────
        HBox midRow = new HBox(14);
        midRow.setAlignment(Pos.TOP_LEFT);

        VBox chartCard = crearCategoriaChartCard();
        HBox.setHgrow(chartCard, Priority.ALWAYS);

        VBox stockBajoCard = crearStockBajoCard();
        stockBajoCard.setPrefWidth(320);
        stockBajoCard.setMinWidth(260);
        stockBajoCard.setMaxWidth(360);

        midRow.getChildren().addAll(chartCard, stockBajoCard);

        // ── Fila 3: Top productos + proveedores ──────────────────
        HBox bottomRow = new HBox(14);
        bottomRow.setAlignment(Pos.TOP_LEFT);

        VBox topProductosCard = crearTopProductosCard();
        HBox.setHgrow(topProductosCard, Priority.ALWAYS);

        VBox proveedoresCard = crearProveedoresResumenCard();
        HBox.setHgrow(proveedoresCard, Priority.ALWAYS);

        bottomRow.getChildren().addAll(topProductosCard, proveedoresCard);

        view.getChildren().addAll(cardsRow, midRow, bottomRow);

        // ── Animaciones staggered de entrada ─────────────────────
        animarEntrada(c1,               0);
        animarEntrada(c2,              70);
        animarEntrada(c3,             140);
        animarEntrada(c4,             210);
        animarEntrada(chartCard,      300);
        animarEntrada(stockBajoCard,  360);
        animarEntrada(topProductosCard,  430);
        animarEntrada(proveedoresCard,   490);

        // ── Animar conteo de stat cards ──────────────────────────
        animarConteo(c1, total);
        animarConteo(c2, activos);
        animarConteo(c3, sinStock);

        ScrollPane scroll = new ScrollPane(view);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dashboard-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        contentArea.getChildren().setAll(scroll);
    }

    // ══════════════════════════════════════════════════════════════
    //  PANTALLA 2 — Lista de Productos
    // ══════════════════════════════════════════════════════════════

    private void mostrarProductos() {
        activarTab(tabProductos);
        recargarDatos();
        productosFiltrados = new ArrayList<>(todosProductos);

        VBox view = new VBox(0);
        view.getStyleClass().add("inventario-productos-view");
        VBox.setVgrow(view, Priority.ALWAYS);

        // ── Barra de herramientas ─────────────────────────────────
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("inventario-toolbar");
        toolbar.setPadding(new Insets(16, 28, 12, 28));

        // Búsqueda
        StackPane searchBox = new StackPane();
        searchBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon icoSearch = new FontIcon("fas-search");
        icoSearch.setIconSize(14);
        icoSearch.setIconColor(Paint.valueOf("#A8A29E"));
        StackPane.setMargin(icoSearch, new Insets(0, 0, 0, 12));

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar producto por nombre o código…");
        txtBuscar.getStyleClass().add("inventario-search-field");
        txtBuscar.setPrefWidth(320);

        searchBox.getChildren().addAll(txtBuscar, icoSearch);

        // Cambiar color del icono al enfocar
        txtBuscar.focusedProperty().addListener((obs, old, focused) -> {
            icoSearch.setIconColor(Paint.valueOf(focused ? "#5A6ACF" : "#A8A29E"));
        });

        // Filtro de categoría
        ComboBox<Categoria> cmbCategoria = new ComboBox<>();
        cmbCategoria.setPromptText("Todas las categorías");
        cmbCategoria.getStyleClass().addAll("combo-box", "inventario-filter");
        cmbCategoria.setPrefWidth(200);

        try {
            List<Categoria> categorias = categoriaService.findAllActivas();
            cmbCategoria.setItems(FXCollections.observableArrayList(categorias));
        } catch (Exception ignored) {}

        cmbCategoria.setConverter(new StringConverter<>() {
            @Override public String toString(Categoria c) { return c == null ? "Todas" : c.getNombre(); }
            @Override public Categoria fromString(String s) { return null; }
        });

        // Filtro de estado
        ComboBox<String> cmbEstado = new ComboBox<>();
        cmbEstado.setItems(FXCollections.observableArrayList("Todos", "Activos", "Inactivos"));
        cmbEstado.setValue("Todos");
        cmbEstado.getStyleClass().addAll("combo-box", "inventario-filter");
        cmbEstado.setPrefWidth(140);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Contador
        lblTotalFiltrados = new Label(todosProductos.size() + " productos");
        lblTotalFiltrados.getStyleClass().add("inventario-count-label");

        // Botón nuevo producto con hover premium
        FontIcon icoAdd = new FontIcon("fas-plus");
        icoAdd.setIconSize(14);
        icoAdd.setIconColor(Paint.valueOf("#FFFFFF"));

        Button btnNuevo = new Button("Nuevo Producto", icoAdd);
        btnNuevo.getStyleClass().add("inventario-btn-nuevo");
        btnNuevo.setOnAction(e -> {
            animarClickBtn(btnNuevo);
            abrirModalCrearProducto();
        });

        toolbar.getChildren().addAll(searchBox, cmbCategoria, cmbEstado, spacer, lblTotalFiltrados, btnNuevo);

        // ── Tabla de productos ────────────────────────────────────
        tablaProductos = buildTablaProductos();
        VBox.setVgrow(tablaProductos, Priority.ALWAYS);

        VBox tableWrapper = new VBox(0);
        tableWrapper.setPadding(new Insets(0, 28, 28, 28));
        tableWrapper.getChildren().add(tablaProductos);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        view.getChildren().addAll(toolbar, tableWrapper);

        // ── Listeners de filtro ───────────────────────────────────
        txtBuscar.textProperty().addListener((obs, o, n) ->
                filtrarProductos(n, cmbCategoria.getValue(), cmbEstado.getValue()));
        cmbCategoria.valueProperty().addListener((obs, o, n) ->
                filtrarProductos(txtBuscar.getText(), n, cmbEstado.getValue()));
        cmbEstado.valueProperty().addListener((obs, o, n) ->
                filtrarProductos(txtBuscar.getText(), cmbCategoria.getValue(), n));

        actualizarTabla();

        animarEntrada(toolbar, 0);
        animarEntrada(tableWrapper, 100);

        contentArea.getChildren().setAll(view);
    }

    private TableView<Producto> buildTablaProductos() {
        TableView<Producto> tabla = new TableView<>();
        tabla.getStyleClass().add("table-view");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay productos para mostrar."));

        TableColumn<Producto, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCodigoBarras() != null ? d.getValue().getCodigoBarras() : "—"));
        colCodigo.setMinWidth(120);
        colCodigo.setMaxWidth(160);

        TableColumn<Producto, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Producto, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(d -> {
            Subcategoria sub = d.getValue().getSubcategoria();
            if (sub != null && sub.getCategoria() != null) {
                return new SimpleStringProperty(sub.getCategoria().getNombre());
            }
            return new SimpleStringProperty("—");
        });
        colCategoria.setMinWidth(120);
        colCategoria.setMaxWidth(180);

        TableColumn<Producto, String> colSubcategoria = new TableColumn<>("Subcategoría");
        colSubcategoria.setCellValueFactory(d -> {
            Subcategoria sub = d.getValue().getSubcategoria();
            return new SimpleStringProperty(sub != null ? sub.getNombre() : "—");
        });
        colSubcategoria.setMinWidth(120);
        colSubcategoria.setMaxWidth(180);

        TableColumn<Producto, String> colPrecioCompra = new TableColumn<>("P. Compra");
        colPrecioCompra.setCellValueFactory(d -> {
            BigDecimal pc = d.getValue().getPrecioCompra();
            return new SimpleStringProperty(pc != null ? FMT_MONEDA.format(pc) : "—");
        });
        colPrecioCompra.setMinWidth(100);
        colPrecioCompra.setMaxWidth(140);

        TableColumn<Producto, String> colPrecioVenta = new TableColumn<>("P. Venta");
        colPrecioVenta.setCellValueFactory(d -> {
            BigDecimal pv = d.getValue().getPrecioVenta();
            return new SimpleStringProperty(pv != null ? FMT_MONEDA.format(pv) : "—");
        });
        colPrecioVenta.setMinWidth(100);
        colPrecioVenta.setMaxWidth(140);

        TableColumn<Producto, String> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getStock())));
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); setText(null);
                } else {
                    Producto p = getTableRow().getItem();
                    Label lbl = new Label(String.valueOf(p.getStock()));
                    if (p.getStock() == 0) {
                        lbl.getStyleClass().addAll("dash-badge", "dash-badge-danger");
                    } else if (p.getStock() <= 5) {
                        lbl.getStyleClass().addAll("dash-badge", "dash-badge-credito");
                    } else {
                        lbl.getStyleClass().addAll("dash-badge", "dash-badge-success");
                    }
                    setGraphic(lbl);
                }
            }
        });
        colStock.setMinWidth(80);
        colStock.setMaxWidth(100);

        TableColumn<Producto, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(""));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Producto p = getTableRow().getItem();
                    Label badge = new Label(p.isActivo() ? "Activo" : "Inactivo");
                    badge.getStyleClass().addAll("dash-badge",
                            p.isActivo() ? "dash-badge-success" : "dash-badge-danger");
                    setGraphic(badge);
                }
            }
        });
        colEstado.setMinWidth(90);
        colEstado.setMaxWidth(110);

        tabla.getColumns().addAll(colCodigo, colNombre, colCategoria, colSubcategoria,
                colPrecioCompra, colPrecioVenta, colStock, colEstado);

        return tabla;
    }

    private void filtrarProductos(String texto, Categoria categoria, String estado) {
        String filtro = texto != null ? texto.toLowerCase().trim() : "";

        productosFiltrados = todosProductos.stream()
                .filter(p -> {
                    if (!filtro.isEmpty()) {
                        boolean matchNombre = p.getNombre() != null
                                && p.getNombre().toLowerCase().contains(filtro);
                        boolean matchCodigo = p.getCodigoBarras() != null
                                && p.getCodigoBarras().toLowerCase().contains(filtro);
                        if (!matchNombre && !matchCodigo) return false;
                    }
                    return true;
                })
                .filter(p -> {
                    if (categoria != null && p.getSubcategoria() != null
                            && p.getSubcategoria().getCategoria() != null) {
                        return p.getSubcategoria().getCategoria().getId().equals(categoria.getId());
                    }
                    return categoria == null;
                })
                .filter(p -> {
                    if ("Activos".equals(estado)) return p.isActivo();
                    if ("Inactivos".equals(estado)) return !p.isActivo();
                    return true;
                })
                .collect(Collectors.toList());

        actualizarTabla();
    }

    private void actualizarTabla() {
        tablaProductos.getItems().setAll(productosFiltrados);
        if (lblTotalFiltrados != null) {
            lblTotalFiltrados.setText(productosFiltrados.size() + " productos");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MODAL — Crear Producto
    // ══════════════════════════════════════════════════════════════

    private void abrirModalCrearProducto() {
        // ── Overlay oscuro ────────────────────────────────────────
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) cerrarModal(overlay);
        });

        // ── Card del modal ────────────────────────────────────────
        VBox modal = new VBox(16);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(580);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setAlignment(Pos.TOP_LEFT);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icoModal = new FontIcon("fas-plus-circle");
        icoModal.setIconSize(20);
        icoModal.setIconColor(Paint.valueOf("#5A6ACF"));

        Label lblTitle = new Label("Nuevo Producto");
        lblTitle.getStyleClass().add("inventario-modal-title");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Button btnCerrar = new Button();
        FontIcon icoCerrar = new FontIcon("fas-times");
        icoCerrar.setIconSize(16);
        icoCerrar.setIconColor(Paint.valueOf("#78716C"));
        btnCerrar.setGraphic(icoCerrar);
        btnCerrar.getStyleClass().add("inventario-modal-close");
        btnCerrar.setOnAction(e -> cerrarModal(overlay));

        header.getChildren().addAll(icoModal, lblTitle, btnCerrar);

        Separator sep = new Separator();

        // ── Formulario ────────────────────────────────────────────
        VBox fieldNombre = crearCampo("Nombre del producto *");
        TextField txtNombre = (TextField) fieldNombre.getChildren().get(1);
        txtNombre.setPromptText("Ej: Coca-Cola 350ml");

        VBox fieldCodigo = crearCampo("Código de barras *");
        TextField txtCodigo = (TextField) fieldCodigo.getChildren().get(1);
        txtCodigo.setPromptText("Ej: 7701234567890");

        HBox row1 = new HBox(14, fieldNombre, fieldCodigo);
        HBox.setHgrow(fieldNombre, Priority.ALWAYS);
        HBox.setHgrow(fieldCodigo, Priority.ALWAYS);

        // Categoría
        VBox fieldCategoria = new VBox(6);
        Label lblCategoria = new Label("Categoría *");
        lblCategoria.getStyleClass().add("form-label");
        ComboBox<Categoria> cmbCat = new ComboBox<>();
        cmbCat.setPromptText("Seleccionar categoría");
        cmbCat.setMaxWidth(Double.MAX_VALUE);
        try { cmbCat.setItems(FXCollections.observableArrayList(categoriaService.findAllActivas())); }
        catch (Exception ignored) {}
        cmbCat.setConverter(new StringConverter<>() {
            @Override public String toString(Categoria c) { return c == null ? "" : c.getNombre(); }
            @Override public Categoria fromString(String s) { return null; }
        });
        fieldCategoria.getChildren().addAll(lblCategoria, cmbCat);

        // Subcategoría
        VBox fieldSubcategoria = new VBox(6);
        Label lblSubcat = new Label("Subcategoría *");
        lblSubcat.getStyleClass().add("form-label");
        ComboBox<Subcategoria> cmbSubcat = new ComboBox<>();
        cmbSubcat.setPromptText("Seleccionar subcategoría");
        cmbSubcat.setMaxWidth(Double.MAX_VALUE);
        cmbSubcat.setDisable(true);
        cmbSubcat.setConverter(new StringConverter<>() {
            @Override public String toString(Subcategoria s) { return s == null ? "" : s.getNombre(); }
            @Override public Subcategoria fromString(String str) { return null; }
        });
        fieldSubcategoria.getChildren().addAll(lblSubcat, cmbSubcat);

        cmbCat.valueProperty().addListener((obs, old, cat) -> {
            cmbSubcat.getItems().clear();
            cmbSubcat.setValue(null);
            if (cat != null) {
                try {
                    List<Subcategoria> subs = subcategoriaService.findByCategoriaId(cat.getId());
                    cmbSubcat.setItems(FXCollections.observableArrayList(subs));
                    cmbSubcat.setDisable(false);
                } catch (Exception ignored) {
                    cmbSubcat.setDisable(true);
                }
            } else {
                cmbSubcat.setDisable(true);
            }
        });

        HBox row2 = new HBox(14, fieldCategoria, fieldSubcategoria);
        HBox.setHgrow(fieldCategoria, Priority.ALWAYS);
        HBox.setHgrow(fieldSubcategoria, Priority.ALWAYS);

        // Proveedor
        VBox fieldProveedor = new VBox(6);
        Label lblProv = new Label("Proveedor *");
        lblProv.getStyleClass().add("form-label");
        ComboBox<Proveedor> cmbProv = new ComboBox<>();
        cmbProv.setPromptText("Seleccionar proveedor");
        cmbProv.setMaxWidth(Double.MAX_VALUE);
        try { cmbProv.setItems(FXCollections.observableArrayList(proveedorService.findAllActivos())); }
        catch (Exception ignored) {}
        cmbProv.setConverter(new StringConverter<>() {
            @Override public String toString(Proveedor p) {
                return p == null ? "" : p.getNombre() + " (" + p.getPorcentajeGanancia() + "%)";
            }
            @Override public Proveedor fromString(String s) { return null; }
        });
        fieldProveedor.getChildren().addAll(lblProv, cmbProv);

        // Precio de compra
        VBox fieldPrecio = crearCampo("Precio de compra *");
        TextField txtPrecio = (TextField) fieldPrecio.getChildren().get(1);
        txtPrecio.setPromptText("Ej: 1500");

        HBox row3 = new HBox(14, fieldProveedor, fieldPrecio);
        HBox.setHgrow(fieldProveedor, Priority.ALWAYS);
        HBox.setHgrow(fieldPrecio, Priority.ALWAYS);

        // Stock inicial + Ajuste
        VBox fieldStock = crearCampo("Stock inicial *");
        TextField txtStock = (TextField) fieldStock.getChildren().get(1);
        txtStock.setPromptText("Ej: 50");

        VBox fieldAjuste = crearCampo("Ajuste margen (%)");
        TextField txtAjuste = (TextField) fieldAjuste.getChildren().get(1);
        txtAjuste.setPromptText("Ej: 5 (opcional)");

        HBox row4 = new HBox(14, fieldStock, fieldAjuste);
        HBox.setHgrow(fieldStock, Priority.ALWAYS);
        HBox.setHgrow(fieldAjuste, Priority.ALWAYS);

        // ── Previsualización de precio ────────────────────────────
        HBox previewBox = new HBox(10);
        previewBox.setAlignment(Pos.CENTER_LEFT);
        previewBox.getStyleClass().add("inventario-price-preview");

        FontIcon icoInfo = new FontIcon("fas-info-circle");
        icoInfo.setIconSize(14);
        icoInfo.setIconColor(Paint.valueOf("#5A6ACF"));

        Label lblPreview = new Label("El precio de venta se calcula automáticamente");
        lblPreview.getStyleClass().add("inventario-preview-text");

        previewBox.getChildren().addAll(icoInfo, lblPreview);

        Runnable calcularPreview = () -> {
            try {
                BigDecimal precioCompra = new BigDecimal(txtPrecio.getText().trim());
                Proveedor prov = cmbProv.getValue();
                if (prov == null) {
                    lblPreview.setText("Selecciona un proveedor para ver el precio de venta");
                    return;
                }
                BigDecimal ajuste = BigDecimal.ZERO;
                String ajusteStr = txtAjuste.getText().trim();
                if (!ajusteStr.isEmpty()) {
                    ajuste = new BigDecimal(ajusteStr);
                }
                BigDecimal margen = prov.getPorcentajeGanancia().add(ajuste);
                BigDecimal factor = BigDecimal.ONE.add(margen.divide(new BigDecimal("100"), 10, java.math.RoundingMode.HALF_UP));
                BigDecimal precioVenta = precioCompra.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);

                lblPreview.setText("Precio de venta estimado: " + FMT_MONEDA.format(precioVenta)
                        + "  (margen " + margen + "%)");

                // Pulso sutil en la previsualización
                animarPulse(previewBox);
            } catch (Exception ignored) {
                lblPreview.setText("El precio de venta se calcula automáticamente");
            }
        };

        txtPrecio.textProperty().addListener((obs, o, n) -> calcularPreview.run());
        txtAjuste.textProperty().addListener((obs, o, n) -> calcularPreview.run());
        cmbProv.valueProperty().addListener((obs, o, n) -> calcularPreview.run());

        // ── Label de error ────────────────────────────────────────
        Label lblError = new Label();
        lblError.getStyleClass().add("inventario-error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // ── Botones de acción ─────────────────────────────────────
        Separator sep2 = new Separator();

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> cerrarModal(overlay));

        FontIcon icoGuardar = new FontIcon("fas-save");
        icoGuardar.setIconSize(14);
        icoGuardar.setIconColor(Paint.valueOf("#FFFFFF"));

        Button btnGuardar = new Button("Crear Producto", icoGuardar);
        btnGuardar.getStyleClass().add("inventario-btn-guardar");
        btnGuardar.setOnAction(e -> {
            lblError.setVisible(false);
            lblError.setManaged(false);

            try {
                String nombre = txtNombre.getText().trim();
                String codigo = txtCodigo.getText().trim();
                if (nombre.isEmpty() || codigo.isEmpty()) {
                    mostrarErrorModal(lblError, "El nombre y código de barras son obligatorios.");
                    return;
                }
                if (cmbCat.getValue() == null || cmbSubcat.getValue() == null) {
                    mostrarErrorModal(lblError, "Selecciona una categoría y subcategoría.");
                    return;
                }
                if (cmbProv.getValue() == null) {
                    mostrarErrorModal(lblError, "Selecciona un proveedor.");
                    return;
                }
                BigDecimal precioCompra;
                try {
                    precioCompra = new BigDecimal(txtPrecio.getText().trim());
                    if (precioCompra.compareTo(BigDecimal.ZERO) <= 0)
                        throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    mostrarErrorModal(lblError, "El precio de compra debe ser un número mayor a 0.");
                    return;
                }
                int stock;
                try {
                    stock = Integer.parseInt(txtStock.getText().trim());
                    if (stock < 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    mostrarErrorModal(lblError, "El stock debe ser un número entero no negativo.");
                    return;
                }
                BigDecimal ajuste = BigDecimal.ZERO;
                String ajusteStr = txtAjuste.getText().trim();
                if (!ajusteStr.isEmpty()) {
                    try {
                        ajuste = new BigDecimal(ajusteStr);
                    } catch (NumberFormatException ex) {
                        mostrarErrorModal(lblError, "El ajuste debe ser un número válido.");
                        return;
                    }
                }

                Producto producto = Producto.builder()
                        .nombre(nombre)
                        .codigoBarras(codigo)
                        .subcategoria(cmbSubcat.getValue())
                        .proveedorPrincipal(cmbProv.getValue())
                        .precioCompra(precioCompra)
                        .ajusteProducto(ajuste)
                        .stock(stock)
                        .activo(true)
                        .build();
                producto.calcularPrecioVenta();

                productoService.crear(producto);

                // Animación de éxito antes de cerrar
                animarExitoModal(modal, () -> {
                    cerrarModal(overlay);
                    mostrarProductos();

                    Alert exito = new Alert(Alert.AlertType.INFORMATION);
                    exito.setTitle("Producto creado");
                    exito.setHeaderText(null);
                    exito.setContentText("El producto '" + nombre + "' se ha creado exitosamente.");
                    exito.showAndWait();
                });

            } catch (BusinessException be) {
                mostrarErrorModal(lblError, be.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblError, "Error inesperado: " + ex.getMessage());
            }
        });

        actions.getChildren().addAll(btnCancelar, btnGuardar);

        modal.getChildren().addAll(header, sep, row1, row2, row3, row4,
                previewBox, lblError, sep2, actions);

        overlay.getChildren().add(modal);
        rootStack.getChildren().add(overlay);

        // Animación de entrada del modal
        animarEntradaModal(overlay, modal);

        // Stagger en los campos del formulario
        int delay = 60;
        for (Node child : List.of(row1, row2, row3, row4, previewBox, actions)) {
            child.setOpacity(0);
            child.setTranslateX(-8);
            FadeTransition ft = new FadeTransition(Duration.millis(250), child);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition tt = new TranslateTransition(Duration.millis(250), child);
            tt.setFromX(-8);
            tt.setToX(0);
            tt.setDelay(Duration.millis(delay));
            tt.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(ft, tt).play();
            delay += 50;
        }
    }

    private void cerrarModal(StackPane overlay) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);
        fadeOut.setOnFinished(e -> rootStack.getChildren().remove(overlay));
        fadeOut.play();
    }

    private void animarEntradaModal(StackPane overlay, VBox modal) {
        overlay.setOpacity(0);
        modal.setScaleX(0.90);
        modal.setScaleY(0.90);
        modal.setTranslateY(24);

        FadeTransition fadeOverlay = new FadeTransition(Duration.millis(250), overlay);
        fadeOverlay.setFromValue(0);
        fadeOverlay.setToValue(1);
        fadeOverlay.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleModal = new ScaleTransition(Duration.millis(320), modal);
        scaleModal.setFromX(0.90);
        scaleModal.setFromY(0.90);
        scaleModal.setToX(1);
        scaleModal.setToY(1);
        scaleModal.setInterpolator(Interpolator.SPLINE(0.25, 0.46, 0.45, 0.94));

        TranslateTransition slideModal = new TranslateTransition(Duration.millis(320), modal);
        slideModal.setFromY(24);
        slideModal.setToY(0);
        slideModal.setInterpolator(Interpolator.SPLINE(0.25, 0.46, 0.45, 0.94));

        new ParallelTransition(fadeOverlay, scaleModal, slideModal).play();
    }

    private void animarExitoModal(VBox modal, Runnable onFinished) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), modal);
        scale.setToX(1.03);
        scale.setToY(1.03);
        scale.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(100), modal);
        scaleBack.setToX(1.0);
        scaleBack.setToY(1.0);
        scaleBack.setInterpolator(Interpolator.EASE_IN);

        scale.setOnFinished(e -> {
            scaleBack.setOnFinished(e2 -> onFinished.run());
            scaleBack.play();
        });
        scale.play();
    }

    private void mostrarErrorModal(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);

        // Fade in + shake
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), lbl);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition shake = new TranslateTransition(Duration.millis(50), lbl);
        shake.setFromX(0);
        shake.setByX(6);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setInterpolator(Interpolator.EASE_BOTH);

        fadeIn.setOnFinished(e -> shake.play());
        fadeIn.play();
    }

    private void animarPulse(Node node) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(120), node);
        pulse.setToX(1.02);
        pulse.setToY(1.02);
        pulse.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition back = new ScaleTransition(Duration.millis(120), node);
        back.setToX(1.0);
        back.setToY(1.0);
        back.setInterpolator(Interpolator.EASE_IN);

        pulse.setOnFinished(e -> back.play());
        pulse.play();
    }

    private void animarClickBtn(Button btn) {
        ScaleTransition press = new ScaleTransition(Duration.millis(60), btn);
        press.setToX(0.95);
        press.setToY(0.95);
        press.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition release = new ScaleTransition(Duration.millis(100), btn);
        release.setToX(1.0);
        release.setToY(1.0);
        release.setInterpolator(Interpolator.EASE_OUT);

        press.setOnFinished(e -> release.play());
        press.play();
    }

    // ══════════════════════════════════════════════════════════════
    //  Dashboard — Stat Cards con hover interactivo
    // ══════════════════════════════════════════════════════════════

    private VBox crearStatCard(String titulo, String valor, String subtitulo,
                                String badgeText, String badgeStyle,
                                String icoLiteral, String icoColorHex, String boxBgHex) {
        VBox card = new VBox();
        card.getStyleClass().add("inventario-stat-card");
        card.setCursor(javafx.scene.Cursor.HAND);

        HBox row = new HBox();
        row.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(4);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label lTitulo = new Label(titulo);
        lTitulo.getStyleClass().add("inventario-stat-label");

        Label lValor = new Label(valor);
        lValor.getStyleClass().add("inventario-stat-value");
        lValor.setPadding(new Insets(2, 0, 0, 0));

        left.getChildren().addAll(lTitulo, lValor);

        if (subtitulo != null) {
            Label lSub = new Label(subtitulo);
            lSub.getStyleClass().add("inventario-stat-sub");
            left.getChildren().add(lSub);
        }
        if (badgeText != null) {
            Label badge = new Label(badgeText);
            badge.getStyleClass().addAll("dash-badge", badgeStyle);
            badge.setPadding(new Insets(4, 0, 0, 0));
            left.getChildren().add(badge);
        }

        StackPane icoBox = new StackPane();
        icoBox.getStyleClass().add("inventario-icon-box");
        icoBox.setStyle("-fx-background-color: " + boxBgHex + ";");

        FontIcon icon = new FontIcon(icoLiteral);
        icon.setIconSize(20);
        icon.setIconColor(Paint.valueOf(icoColorHex));
        icoBox.getChildren().add(icon);

        row.getChildren().addAll(left, icoBox);
        card.getChildren().add(row);

        // Hover scale en icono
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), icoBox);
            st.setToX(1.12);
            st.setToY(1.12);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), icoBox);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });

        return card;
    }

    private void animarConteo(VBox card, int targetValue) {
        if (targetValue <= 0) return;
        // Buscar el label de valor dentro de la card
        HBox row = (HBox) card.getChildren().get(0);
        VBox left = (VBox) row.getChildren().get(0);
        Label lValor = (Label) left.getChildren().get(1);

        javafx.beans.property.IntegerProperty counter = new javafx.beans.property.SimpleIntegerProperty(0);
        counter.addListener((obs, oldVal, newVal) ->
                lValor.setText(String.valueOf(newVal.intValue())));

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(counter, 0)),
                new KeyFrame(Duration.millis(600), new KeyValue(counter, targetValue, Interpolator.EASE_OUT))
        );
        timeline.setDelay(Duration.millis(300));
        timeline.play();
    }

    private VBox crearCategoriaChartCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lLabel = new Label("DISTRIBUCIÓN POR CATEGORÍA");
        lLabel.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lLabel, Priority.ALWAYS);

        FontIcon icoChart = new FontIcon("fas-chart-bar");
        icoChart.setIconSize(15);
        icoChart.setIconColor(Paint.valueOf("#94A3B8"));

        header.getChildren().addAll(lLabel, icoChart);

        Map<String, Long> porCategoria = todosProductos.stream()
                .filter(Producto::isActivo)
                .filter(p -> p.getSubcategoria() != null && p.getSubcategoria().getCategoria() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getSubcategoria().getCategoria().getNombre(),
                        Collectors.counting()));

        if (porCategoria.isEmpty()) {
            Label lVacio = new Label("Sin datos de categorías.");
            lVacio.getStyleClass().add("inventario-empty");
            card.getChildren().addAll(header, lVacio);
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setLabel("");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setAnimated(true);
        barChart.getStyleClass().add("inventario-bar-chart");
        barChart.setPrefHeight(200);
        VBox.setVgrow(barChart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        porCategoria.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        barChart.getData().add(series);

        card.getChildren().addAll(header, barChart);
        return card;
    }

    private VBox crearStockBajoCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lTitulo = new Label("Stock Bajo");
        lTitulo.getStyleClass().add("inventario-section-title");
        HBox.setHgrow(lTitulo, Priority.ALWAYS);

        FontIcon ico = new FontIcon("fas-exclamation-circle");
        ico.setIconSize(15);
        ico.setIconColor(Paint.valueOf("#DC2626"));

        header.getChildren().addAll(lTitulo, ico);

        VBox lista = new VBox(0);

        List<Producto> stockBajo = todosProductos.stream()
                .filter(Producto::isActivo)
                .filter(p -> p.getStock() <= 5)
                .sorted(Comparator.comparingInt(Producto::getStock))
                .limit(6)
                .collect(Collectors.toList());

        if (stockBajo.isEmpty()) {
            Label lVacio = new Label("Todos los productos tienen buen stock.");
            lVacio.getStyleClass().add("inventario-empty");
            lista.getChildren().add(lVacio);
        } else {
            for (Producto p : stockBajo) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("inventario-alert-row");

                VBox left = new VBox(2);
                HBox.setHgrow(left, Priority.ALWAYS);

                Label lNom = new Label(p.getNombre());
                lNom.getStyleClass().add("inventario-alert-name");

                String catNombre = (p.getSubcategoria() != null && p.getSubcategoria().getCategoria() != null)
                        ? p.getSubcategoria().getCategoria().getNombre() : "";
                Label lCat = new Label(catNombre);
                lCat.getStyleClass().add("inventario-alert-cat");

                left.getChildren().addAll(lNom, lCat);

                // Stock badge con barra de progreso visual
                VBox stockVisual = new VBox(3);
                stockVisual.setAlignment(Pos.CENTER_RIGHT);
                stockVisual.setMinWidth(60);

                Label lStock = new Label(String.valueOf(p.getStock()));
                lStock.getStyleClass().add("inventario-alert-stock");

                // Mini barra de progreso
                StackPane barBg = new StackPane();
                barBg.getStyleClass().add("inventario-stock-bar-bg");
                barBg.setMinWidth(50);

                Region barFill = new Region();
                barFill.getStyleClass().add("inventario-stock-bar-fill");
                double pct = Math.min(1.0, p.getStock() / 10.0);
                barFill.setMaxWidth(50 * pct);

                String barColor = p.getStock() == 0 ? "#DC2626"
                        : p.getStock() <= 2 ? "#D97706" : "#16A34A";
                barFill.setStyle("-fx-background-color: " + barColor + ";");

                StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
                barBg.getChildren().add(barFill);

                stockVisual.getChildren().addAll(lStock, barBg);

                row.getChildren().addAll(left, stockVisual);
                lista.getChildren().add(row);
            }
        }

        card.getChildren().addAll(header, lista);
        return card;
    }

    private VBox crearTopProductosCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("inventario-table-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("inventario-table-header");

        Label lTitle = new Label("Productos con Mayor Valor en Stock");
        lTitle.getStyleClass().add("inventario-section-title");
        HBox.setHgrow(lTitle, Priority.ALWAYS);

        header.getChildren().add(lTitle);

        TableView<Producto> tabla = new TableView<>();
        tabla.getStyleClass().add("table-view");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(180);
        tabla.setPlaceholder(new Label("Sin productos."));

        TableColumn<Producto, String> colNombre = new TableColumn<>("Producto");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Producto, String> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getStock())));
        colStock.setMaxWidth(80);

        TableColumn<Producto, String> colPrecio = new TableColumn<>("P. Venta");
        colPrecio.setCellValueFactory(d -> {
            BigDecimal pv = d.getValue().getPrecioVenta();
            return new SimpleStringProperty(pv != null ? FMT_MONEDA.format(pv) : "—");
        });
        colPrecio.setMaxWidth(120);

        TableColumn<Producto, String> colValor = new TableColumn<>("Valor Total");
        colValor.setCellValueFactory(d -> {
            BigDecimal pv = d.getValue().getPrecioVenta();
            if (pv == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(
                    FMT_MONEDA.format(pv.multiply(BigDecimal.valueOf(d.getValue().getStock()))));
        });
        colValor.setMaxWidth(140);

        tabla.getColumns().addAll(colNombre, colStock, colPrecio, colValor);

        List<Producto> topValor = todosProductos.stream()
                .filter(Producto::isActivo)
                .filter(p -> p.getPrecioVenta() != null)
                .sorted((a, b) -> {
                    BigDecimal va = a.getPrecioVenta().multiply(BigDecimal.valueOf(a.getStock()));
                    BigDecimal vb = b.getPrecioVenta().multiply(BigDecimal.valueOf(b.getStock()));
                    return vb.compareTo(va);
                })
                .limit(5)
                .collect(Collectors.toList());
        tabla.getItems().addAll(topValor);

        card.getChildren().addAll(header, tabla);
        return card;
    }

    private VBox crearProveedoresResumenCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lTitulo = new Label("Productos por Proveedor");
        lTitulo.getStyleClass().add("inventario-section-title");
        HBox.setHgrow(lTitulo, Priority.ALWAYS);

        FontIcon ico = new FontIcon("fas-truck");
        ico.setIconSize(15);
        ico.setIconColor(Paint.valueOf("#94A3B8"));

        header.getChildren().addAll(lTitulo, ico);

        VBox lista = new VBox(0);

        Map<String, Long> porProveedor = todosProductos.stream()
                .filter(Producto::isActivo)
                .filter(p -> p.getProveedorPrincipal() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getProveedorPrincipal().getNombre(),
                        Collectors.counting()));

        if (porProveedor.isEmpty()) {
            Label lVacio = new Label("Sin datos de proveedores.");
            lVacio.getStyleClass().add("inventario-empty");
            lista.getChildren().add(lVacio);
        } else {
            long maxCount = porProveedor.values().stream().max(Long::compareTo).orElse(1L);

            porProveedor.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(6)
                    .forEach(e -> {
                        HBox row = new HBox(8);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.getStyleClass().add("inventario-proveedor-row");

                        FontIcon icoTruck = new FontIcon("fas-building");
                        icoTruck.setIconSize(13);
                        icoTruck.setIconColor(Paint.valueOf("#78716C"));

                        Label lNom = new Label(e.getKey());
                        lNom.getStyleClass().add("inventario-alert-name");
                        HBox.setHgrow(lNom, Priority.ALWAYS);

                        // Barra de progreso proporcional
                        StackPane barBg = new StackPane();
                        barBg.getStyleClass().add("inventario-stock-bar-bg");
                        barBg.setMinWidth(60);

                        Region barFill = new Region();
                        barFill.getStyleClass().add("inventario-stock-bar-fill");
                        double pct = (double) e.getValue() / maxCount;
                        barFill.setMaxWidth(60 * pct);
                        barFill.setStyle("-fx-background-color: #5A6ACF;");
                        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
                        barBg.getChildren().add(barFill);

                        Label lCount = new Label(e.getValue() + " prod.");
                        lCount.getStyleClass().addAll("dash-badge", "dash-badge-success");

                        row.getChildren().addAll(icoTruck, lNom, barBg, lCount);
                        lista.getChildren().add(row);
                    });
        }

        card.getChildren().addAll(header, lista);
        return card;
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers genéricos
    // ══════════════════════════════════════════════════════════════

    private VBox crearCampo(String labelText) {
        VBox field = new VBox(6);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        TextField txt = new TextField();
        txt.setMaxWidth(Double.MAX_VALUE);
        field.getChildren().addAll(lbl, txt);
        return field;
    }

    private void animarEntrada(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(18);
        node.setScaleX(0.97);
        node.setScaleY(0.97);

        FadeTransition fade = new FadeTransition(Duration.millis(380), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(380), node);
        slide.setFromY(18);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.SPLINE(0.25, 0.46, 0.45, 0.94));

        ScaleTransition scale = new ScaleTransition(Duration.millis(380), node);
        scale.setFromX(0.97);
        scale.setFromY(0.97);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition entrada = new ParallelTransition(fade, slide, scale);
        entrada.setDelay(Duration.millis(delayMs));
        entrada.play();
    }

    private void recargarDatos() {
        try { todosProductos = productoService.findAll(); }
        catch (Exception e) { todosProductos = new ArrayList<>(); }
    }
}
