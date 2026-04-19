package com.nap.pos.ui.controller;

import com.nap.pos.application.service.CategoriaService;
import com.nap.pos.application.service.ConfiguracionService;
import com.nap.pos.application.service.ProductoService;
import com.nap.pos.application.service.ProveedorService;
import com.nap.pos.application.service.SubcategoriaService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.Proveedor;
import com.nap.pos.domain.model.Subcategoria;
import com.nap.pos.ui.component.CatalogoMiniModalComponent;
import com.nap.pos.ui.component.ProductoModalComponent;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    private final ProductoService      productoService;
    private final CategoriaService     categoriaService;
    private final SubcategoriaService  subcategoriaService;
    private final ProveedorService     proveedorService;
    private final ConfiguracionService configuracionService;
    private final CatalogoMiniModalComponent catalogoMiniModalComponent;
    private final ProductoModalComponent productoModalComponent;

    private List<Producto> todosProductos = new ArrayList<>();
    private List<Producto> productosFiltrados = new ArrayList<>();
    private StackPane      rootStack;
    private VBox           contentArea;
    private Button         tabResumen;
    private Button         tabProductos;
    private Button         tabAjuste;
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

        FontIcon icoAjuste = new FontIcon("fas-sliders-h");
        icoAjuste.setIconSize(14);
        icoAjuste.setIconColor(Paint.valueOf("#78716C"));

        tabAjuste = new Button("Ajuste de Stock", icoAjuste);
        tabAjuste.getStyleClass().add("inventario-tab");
        tabAjuste.setOnAction(e -> {
            animarClickTab(tabAjuste);
            mostrarAdvertenciaAjuste(this::mostrarAjusteStock);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(tabResumen, tabProductos, tabAjuste, spacer);
        return bar;
    }

    private void activarTab(Button activo) {
        tabResumen.getStyleClass().remove("inventario-tab-active");
        tabProductos.getStyleClass().remove("inventario-tab-active");
        tabAjuste.getStyleClass().remove("inventario-tab-active");
        activo.getStyleClass().add("inventario-tab-active");

        String inactivo = "#78716C";
        String activo_  = "#5A6ACF";
        ((FontIcon) tabResumen.getGraphic()) .setIconColor(Paint.valueOf(activo == tabResumen  ? activo_ : inactivo));
        ((FontIcon) tabProductos.getGraphic()).setIconColor(Paint.valueOf(activo == tabProductos ? activo_ : inactivo));
        ((FontIcon) tabAjuste.getGraphic())  .setIconColor(Paint.valueOf(activo == tabAjuste   ? activo_ : inactivo));
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
    //  Modal de advertencia — Ajuste de Stock
    // ══════════════════════════════════════════════════════════════

    private void mostrarAdvertenciaAjuste(Runnable onConfirmar) {
        // Leer fecha programada de la configuración
        String fechaProgramada = "no configurada";
        try {
            ConfiguracionTienda cfg = configuracionService.obtener();
            if (cfg.getDiaInventarioAnual() != null && cfg.getMesInventarioAnual() != null) {
                String[] meses = {"enero","febrero","marzo","abril","mayo","junio",
                        "julio","agosto","septiembre","octubre","noviembre","diciembre"};
                String mes = meses[cfg.getMesInventarioAnual() - 1];
                fechaProgramada = cfg.getDiaInventarioAnual() + " de " + mes;
            }
        } catch (Exception ignored) {}
        final String fecha = fechaProgramada;

        // ── Overlay ───────────────────────────────────────────────
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");

        // ── Card ─────────────────────────────────────────────────
        VBox modal = new VBox(16);
        modal.getStyleClass().addAll("inventario-modal", "ajuste-warn-modal");
        modal.setMaxWidth(400);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setAlignment(Pos.TOP_CENTER);

        // Icono centrado
        StackPane icoCircle = new StackPane();
        icoCircle.getStyleClass().add("ajuste-warn-ico-circle");
        icoCircle.setMinSize(56, 56);
        icoCircle.setMaxSize(56, 56);
        FontIcon ico = new FontIcon("fas-calendar-check");
        ico.setIconSize(22);
        ico.setIconColor(Paint.valueOf("#D97706"));
        icoCircle.getChildren().add(ico);

        VBox icoWrapper = new VBox(icoCircle);
        icoWrapper.setAlignment(Pos.CENTER);

        // Título
        Label lblTitulo = new Label("Ajuste de Inventario Anual");
        lblTitulo.getStyleClass().add("ajuste-warn-title");
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(Double.MAX_VALUE);

        // Fecha programada
        Label lblFecha = new Label("Fecha programada: " + fecha);
        lblFecha.getStyleClass().add("ajuste-warn-fecha");

        // Cuerpo del mensaje (sin doble salto — pregunta separada)
        Label lblMensaje = new Label(
                "El ajuste de stock es una operación de inventario anual. " +
                "Realizarla fuera de la fecha programada puede afectar " +
                "la exactitud de los reportes y el historial contable.");
        lblMensaje.getStyleClass().add("ajuste-warn-mensaje");
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(Double.MAX_VALUE);

        Label lblPregunta = new Label("¿Deseas continuar de todas formas?");
        lblPregunta.getStyleClass().add("ajuste-warn-pregunta");

        Separator sep = new Separator();

        // Botones
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> {
            // Revertir la selección visual del tab
            activarTab(tabResumen);
            cerrarModal(overlay);
        });

        FontIcon icoAceptar = new FontIcon("fas-check");
        icoAceptar.setIconSize(13);
        icoAceptar.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnAceptar = new Button("Sí, continuar", icoAceptar);
        btnAceptar.getStyleClass().add("inventario-btn-guardar");
        btnAceptar.setOnAction(e -> {
            cerrarModal(overlay);
            onConfirmar.run();
        });

        actions.getChildren().addAll(btnCancelar, btnAceptar);

        modal.getChildren().addAll(icoWrapper, lblTitulo, lblFecha, lblMensaje, lblPregunta, sep, actions);
        overlay.getChildren().add(modal);
        rootStack.getChildren().add(overlay);
        animarEntradaModal(overlay, modal);
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
        cmbCategoria.getStyleClass().addAll("combo-box", "inventario-filter");
        cmbCategoria.setPrefWidth(200);

        try {
            List<Categoria> categorias = categoriaService.findAllActivas();
            List<Categoria> opcionesConTodas = new java.util.ArrayList<>();
            opcionesConTodas.add(null);
            opcionesConTodas.addAll(categorias);
            cmbCategoria.setItems(FXCollections.observableArrayList(opcionesConTodas));
            cmbCategoria.setValue(null);
        } catch (Exception ignored) {}

        cmbCategoria.setConverter(new StringConverter<>() {
            @Override public String toString(Categoria c) { return c == null ? "Todas las categorías" : c.getNombre(); }
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

        // Botón subida masiva
        FontIcon icoUpload = new FontIcon("fas-file-upload");
        icoUpload.setIconSize(14);
        icoUpload.setIconColor(Paint.valueOf("#5A6ACF"));

        Button btnSubidaMasiva = new Button("Subida Masiva", icoUpload);
        btnSubidaMasiva.getStyleClass().add("inventario-btn-masiva");
        btnSubidaMasiva.setOnAction(e -> {
            animarClickBtn(btnSubidaMasiva);
            abrirModalSubidaMasiva();
        });

        HBox botonesAccion = new HBox(10, btnSubidaMasiva, btnNuevo);
        botonesAccion.setAlignment(Pos.CENTER_RIGHT);

        toolbar.getChildren().addAll(searchBox, cmbCategoria, cmbEstado, spacer, lblTotalFiltrados, botonesAccion);

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
        tabla.getStyleClass().addAll("table-view", "prod-table-clickable");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Placeholder con estilo mejorado
        VBox placeholder = new VBox(10);
        placeholder.setAlignment(Pos.CENTER);
        FontIcon icoEmpty = new FontIcon("fas-box-open");
        icoEmpty.setIconSize(36);
        icoEmpty.setIconColor(Paint.valueOf("#A8A29E"));
        Label lblEmpty = new Label("No hay productos para mostrar");
        lblEmpty.getStyleClass().add("inventario-empty");
        placeholder.getChildren().addAll(icoEmpty, lblEmpty);
        tabla.setPlaceholder(placeholder);

        // ── Row factory: hover cursor + click → detalle ───────────
        tabla.setRowFactory(tv -> {
            TableRow<Producto> row = new TableRow<>();
            row.getStyleClass().add("prod-table-row");
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 1) {
                    mostrarDetalleProducto(row.getItem());
                }
            });
            return row;
        });

        // ── Columna: miniatura ─────────────────────────────────────
        TableColumn<Producto, String> colImg = new TableColumn<>("");
        colImg.setCellValueFactory(d -> new SimpleStringProperty(""));
        colImg.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(buildThumbnailCell(getTableRow().getItem().getImagenPath()));
                    setAlignment(Pos.CENTER);
                }
            }
        });
        colImg.setMinWidth(60);
        colImg.setMaxWidth(60);
        colImg.setSortable(false);

        TableColumn<Producto, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCodigoBarras() != null ? d.getValue().getCodigoBarras() : "—"));
        colCodigo.setMinWidth(110);
        colCodigo.setMaxWidth(150);

        TableColumn<Producto, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colNombre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.getStyleClass().add("prod-nombre-cell");
                setGraphic(lbl);
                setText(null);
            }
        });

        TableColumn<Producto, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(d -> {
            Subcategoria sub = d.getValue().getSubcategoria();
            if (sub != null && sub.getCategoria() != null) {
                return new SimpleStringProperty(sub.getCategoria().getNombre());
            }
            return new SimpleStringProperty("—");
        });
        colCategoria.setMinWidth(110);
        colCategoria.setMaxWidth(170);

        TableColumn<Producto, String> colSubcategoria = new TableColumn<>("Subcategoría");
        colSubcategoria.setCellValueFactory(d -> {
            Subcategoria sub = d.getValue().getSubcategoria();
            return new SimpleStringProperty(sub != null ? sub.getNombre() : "—");
        });
        colSubcategoria.setMinWidth(110);
        colSubcategoria.setMaxWidth(170);

        TableColumn<Producto, String> colPrecioVenta = new TableColumn<>("P. Venta");
        colPrecioVenta.setCellValueFactory(d -> {
            BigDecimal pv = d.getValue().getPrecioVenta();
            return new SimpleStringProperty(pv != null ? FMT_MONEDA.format(pv) : "—");
        });
        colPrecioVenta.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                getStyleClass().add("prod-precio-cell");
            }
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
                    setText(null);
                }
            }
        });
        colStock.setMinWidth(76);
        colStock.setMaxWidth(96);

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
                    setText(null);
                }
            }
        });
        colEstado.setMinWidth(86);
        colEstado.setMaxWidth(106);

        // Columna de acción rápida (ver detalle)
        TableColumn<Producto, String> colAccion = new TableColumn<>("");
        colAccion.setCellValueFactory(d -> new SimpleStringProperty(""));
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                FontIcon ico = new FontIcon("fas-eye");
                ico.setIconSize(13);
                ico.setIconColor(Paint.valueOf("#5A6ACF"));
                btn.setGraphic(ico);
                btn.getStyleClass().add("prod-row-arrow");
                btn.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null)
                        mostrarDetalleProducto(getTableRow().getItem());
                });
                setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setText(null);
            }
        });
        colAccion.setMinWidth(44);
        colAccion.setMaxWidth(44);
        colAccion.setSortable(false);

        tabla.getColumns().addAll(colImg, colCodigo, colNombre, colCategoria, colSubcategoria,
                colPrecioVenta, colStock, colEstado, colAccion);

        return tabla;
    }

    /** Miniatura para celda de tabla — imagen o placeholder */
    private StackPane buildThumbnailCell(String ruta) {
        StackPane wrap = new StackPane();
        wrap.getStyleClass().add("prod-thumbnail-cell");
        if (ruta != null && !ruta.isBlank()) {
            try {
                ImageView iv = new ImageView();
                iv.setFitWidth(40);
                iv.setFitHeight(40);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                iv.getStyleClass().add("prod-thumbnail");
                Image img = new Image(Paths.get(ruta).toUri().toString(), 40, 40, true, true);
                iv.setImage(img);
                wrap.getChildren().add(iv);
            } catch (Exception ex) {
                wrap.getChildren().add(buildImgPlaceholder(18));
            }
        } else {
            wrap.getChildren().add(buildImgPlaceholder(18));
        }
        return wrap;
    }

    /** Miniatura placeholder cuando el producto no tiene imagen */
    private Node buildImgPlaceholder(int iconSize) {
        StackPane ph = new StackPane();
        ph.getStyleClass().add("prod-thumbnail-placeholder");
        ph.setMinSize(40, 40);
        ph.setMaxSize(40, 40);
        FontIcon ico = new FontIcon("fas-image");
        ico.setIconSize(iconSize);
        ico.setIconColor(Paint.valueOf("#C4BEB7"));
        ph.getChildren().add(ico);
        return ph;
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
        productoModalComponent.abrirModalCrearProducto(
                rootStack,
                "",
                "Ej: 50",
                "",
                creado -> {
                    recargarDatos();
                    mostrarProductos();

                    Alert exito = new Alert(Alert.AlertType.INFORMATION);
                    exito.setTitle("Producto creado");
                    exito.setHeaderText(null);
                    exito.setContentText("El producto '" + creado.getNombre() + "' se ha creado exitosamente.");
                    exito.showAndWait();
                },
                true
        );
    }

    private void abrirMiniModalCategoria(ComboBox<Categoria> cmbCat,
                                          ComboBox<Subcategoria> cmbSubcat,
                                          Button btnNuevaSubcat) {
        catalogoMiniModalComponent.abrirMiniModalCategoria(rootStack, cmbCat, cmbSubcat, btnNuevaSubcat);
    }

    private void abrirMiniModalSubcategoria(Categoria categoria, ComboBox<Subcategoria> cmbSubcat) {
        catalogoMiniModalComponent.abrirMiniModalSubcategoria(rootStack, categoria, cmbSubcat);
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
            VBox emptyState = buildChartEmptyState("fas-chart-bar",
                    "Sin datos de categorías",
                    "Asigna categorías a tus productos para ver la distribución");
            card.getChildren().addAll(header, emptyState);
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
        int catPad = series.getData().size();
        for (int i = catPad; i < 5; i++) series.getData().add(new XYChart.Data<>(" ".repeat(i + 1), 0));
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
            VBox emptyState = buildChartEmptyState("fas-truck",
                    "Sin datos de proveedores",
                    "Asigna proveedores a tus productos para ver el resumen");
            lista.getChildren().add(emptyState);
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

    private VBox buildChartEmptyState(String icon, String titulo, String subtitulo) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24, 0, 8, 0));
        FontIcon ico = new FontIcon(icon);
        ico.setIconSize(32);
        ico.setIconColor(Paint.valueOf("#D4CEC8"));
        Label lTitle = new Label(titulo);
        lTitle.getStyleClass().add("inventario-chart-empty-title");
        Label lSub = new Label(subtitulo);
        lSub.getStyleClass().add("inventario-chart-empty-sub");
        lSub.setWrapText(true);
        lSub.setMaxWidth(220);
        box.getChildren().addAll(ico, lTitle, lSub);
        return box;
    }

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

    // ══════════════════════════════════════════════════════════════════
    //  Wizard de Importación Masiva (CSV / Excel)
    // ══════════════════════════════════════════════════════════════════

    private void abrirModalSubidaMasiva() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/importacion.fxml"));
            loader.setControllerFactory(com.nap.pos.Launcher.getContext()::getBean);

            javafx.scene.Parent root = loader.load();
            Scene scene = new Scene(root);

            Stage stage = new Stage();
            stage.setTitle("NAP POS — Importación Masiva");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(rootStack.getScene().getWindow());
            stage.setScene(scene);
            stage.setWidth(940);
            stage.setHeight(680);
            stage.setMinWidth(860);
            stage.setMinHeight(580);
            stage.centerOnScreen();
            stage.showAndWait();

            // Refrescar inventario si se importaron registros
            recargarDatos();
            productosFiltrados = new ArrayList<>(todosProductos);
            actualizarTabla();

        } catch (Exception e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error");
            alerta.setHeaderText(null);
            alerta.setContentText("No se pudo abrir el wizard de importación:\n" + e.getMessage());
            alerta.showAndWait();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PANTALLA 3 — Detalle de Producto (View Switching)
    // ══════════════════════════════════════════════════════════════

    private void mostrarDetalleProducto(Producto productoIn) {
        // Refrescar el producto desde la BD para tener datos actualizados
        Producto producto;
        try { producto = productoService.findById(productoIn.getId()); }
        catch (Exception e) { producto = productoIn; }
        final Producto p = producto;

        activarTab(tabProductos);

        VBox view = new VBox(0);
        view.getStyleClass().add("prod-detalle-root");
        VBox.setVgrow(view, Priority.ALWAYS);

        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.getStyleClass().add("prod-detalle-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Button btnVolver = new Button("Productos");
        FontIcon icoBack = new FontIcon("fas-arrow-left");
        icoBack.setIconSize(13);
        icoBack.setIconColor(Paint.valueOf("#5A6ACF"));
        btnVolver.setGraphic(icoBack);
        btnVolver.getStyleClass().add("prod-detalle-back-btn");
        btnVolver.setOnAction(e -> mostrarProductos());

        Label lblSep = new Label("›");
        lblSep.getStyleClass().add("prod-detalle-breadcrumb-sep");

        Label lblNombre = new Label(p.getNombre());
        lblNombre.getStyleClass().add("prod-detalle-title");
        HBox.setHgrow(lblNombre, Priority.ALWAYS);

        Label badgeEstado = new Label(p.isActivo() ? "Activo" : "Inactivo");
        badgeEstado.getStyleClass().addAll("dash-badge",
                p.isActivo() ? "dash-badge-success" : "dash-badge-danger");

        header.getChildren().addAll(btnVolver, lblSep, lblNombre, badgeEstado);

        // ── Cuerpo ────────────────────────────────────────────────
        HBox body = new HBox(20);
        body.getStyleClass().add("prod-detalle-body");
        body.setPadding(new Insets(24, 28, 0, 28));
        VBox.setVgrow(body, Priority.ALWAYS);

        // ── Panel izquierdo: imagen ───────────────────────────────
        VBox panelImagen = new VBox(14);
        panelImagen.getStyleClass().add("prod-detalle-image-panel");
        panelImagen.setAlignment(Pos.TOP_CENTER);
        panelImagen.setMinWidth(220);
        panelImagen.setMaxWidth(220);

        // Título sección
        Label lblSecImg = new Label("IMAGEN DEL PRODUCTO");
        lblSecImg.getStyleClass().add("prod-detalle-section-label");

        // Contenedor de imagen
        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("prod-detalle-img-box");
        imgBox.setMinSize(190, 190);
        imgBox.setMaxSize(190, 190);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(178);
        imgView.setFitHeight(178);
        imgView.setPreserveRatio(true);
        imgView.setSmooth(true);

        if (p.getImagenPath() != null && !p.getImagenPath().isBlank()) {
            try {
                imgView.setImage(new Image(Paths.get(p.getImagenPath()).toUri().toString(),
                        178, 178, true, true));
                imgBox.getChildren().add(imgView);
            } catch (Exception ex) {
                imgBox.getChildren().add(buildImgBoxPlaceholder());
            }
        } else {
            imgBox.getChildren().add(buildImgBoxPlaceholder());
        }

        // Botón cambiar imagen
        FontIcon icoCamera = new FontIcon("fas-camera");
        icoCamera.setIconSize(13);
        icoCamera.setIconColor(Paint.valueOf("#5A6ACF"));
        Button btnCambiarImg = new Button("Cambiar imagen", icoCamera);
        btnCambiarImg.getStyleClass().add("inventario-btn-masiva");
        btnCambiarImg.setMaxWidth(Double.MAX_VALUE);
        btnCambiarImg.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleccionar imagen del producto");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"));
            File origen = chooser.showOpenDialog(rootStack.getScene().getWindow());
            if (origen == null) return;
            try {
                String ext = obtenerExtensionImg(origen.getName());
                Path carpeta = Paths.get(System.getProperty("user.home"), ".nappos", "assets", "products");
                Files.createDirectories(carpeta);
                Path destino = carpeta.resolve("producto_" + p.getId() + "." + ext);
                Files.copy(origen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
                String rutaNueva = destino.toAbsolutePath().toString();

                productoService.actualizarImagen(p.getId(), rutaNueva);

                // Actualizar imagen en pantalla
                Image nuevaImg = new Image(destino.toUri().toString(), 178, 178, true, true);
                imgView.setImage(nuevaImg);
                imgBox.getChildren().setAll(imgView);

                actualizarTabla(); // refrescar thumbnails de la tabla

                animarPulse(imgBox);
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText(null);
                err.setContentText("No se pudo guardar la imagen: " + ex.getMessage());
                err.showAndWait();
            }
        });

        panelImagen.getChildren().addAll(lblSecImg, imgBox, btnCambiarImg);

        // ── Panel derecho: especificaciones ───────────────────────
        VBox panelInfo = new VBox(16);
        HBox.setHgrow(panelInfo, Priority.ALWAYS);

        // Sección: Información General
        VBox secGeneral = crearSeccionDetalle("INFORMACIÓN GENERAL", "fas-tag", "#5A6ACF");
        VBox gridGeneral = new VBox(0);
        gridGeneral.getStyleClass().add("prod-detalle-grid");

        String catNombre = (p.getSubcategoria() != null && p.getSubcategoria().getCategoria() != null)
                ? p.getSubcategoria().getCategoria().getNombre() : "—";
        String subNombre = p.getSubcategoria() != null ? p.getSubcategoria().getNombre() : "—";
        String provNombre = p.getProveedorPrincipal() != null ? p.getProveedorPrincipal().getNombre() : "—";

        gridGeneral.getChildren().addAll(
                crearFilaDetalle("Código de barras", p.getCodigoBarras() != null ? p.getCodigoBarras() : "—"),
                crearFilaDetalle("Nombre", p.getNombre()),
                crearFilaDetalle("Categoría", catNombre),
                crearFilaDetalle("Subcategoría", subNombre),
                crearFilaDetalle("Proveedor", provNombre)
        );
        secGeneral.getChildren().add(gridGeneral);

        // Sección: Precios
        VBox secPrecios = crearSeccionDetalle("PRECIOS Y MARGEN", "fas-dollar-sign", "#D97706");
        VBox gridPrecios = new VBox(0);
        gridPrecios.getStyleClass().add("prod-detalle-grid");

        String pcStr = p.getPrecioCompra() != null ? FMT_MONEDA.format(p.getPrecioCompra()) : "—";
        String pvStr = p.getPrecioVenta() != null ? FMT_MONEDA.format(p.getPrecioVenta()) : "—";
        String margenStr = p.getProveedorPrincipal() != null
                ? p.getProveedorPrincipal().getPorcentajeGanancia() + "%" : "—";

        gridPrecios.getChildren().addAll(
                crearFilaDetalle("Precio de compra", pcStr),
                crearFilaDetalle("Precio de venta", pvStr),
                crearFilaDetalle("Margen (%)", margenStr)
        );
        secPrecios.getChildren().add(gridPrecios);

        // Sección: Stock
        VBox secStock = crearSeccionDetalle("STOCK", "fas-boxes", p.getStock() == 0 ? "#DC2626" : "#15803D");
        VBox gridStock = new VBox(0);
        gridStock.getStyleClass().add("prod-detalle-grid");

        Label lblStockVal = new Label(String.valueOf(p.getStock()));
        String stockBadge = p.getStock() == 0 ? "dash-badge-danger"
                : p.getStock() <= 5 ? "dash-badge-credito" : "dash-badge-success";
        lblStockVal.getStyleClass().addAll("dash-badge", stockBadge);

        HBox filaStock = crearFilaDetalle("Unidades disponibles", "");
        ((Label) filaStock.getChildren().get(1)).setGraphic(lblStockVal);
        ((Label) filaStock.getChildren().get(1)).setText(null);

        gridStock.getChildren().addAll(
                filaStock,
                crearFilaDetalle("Estado", p.isActivo() ? "Activo — disponible en ventas" : "Inactivo — oculto en ventas")
        );
        secStock.getChildren().add(gridStock);

        panelInfo.getChildren().addAll(secGeneral, secPrecios, secStock);

        ScrollPane scrollInfo = new ScrollPane(panelInfo);
        scrollInfo.setFitToWidth(true);
        scrollInfo.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollInfo.getStyleClass().add("dashboard-scroll");
        HBox.setHgrow(scrollInfo, Priority.ALWAYS);

        body.getChildren().addAll(panelImagen, scrollInfo);

        // ── Footer: botones de acción ─────────────────────────────
        HBox footer = new HBox(12);
        footer.getStyleClass().add("prod-detalle-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        // Botón editar
        FontIcon icoEdit = new FontIcon("fas-edit");
        icoEdit.setIconSize(13);
        icoEdit.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnEditar = new Button("Editar Producto", icoEdit);
        btnEditar.getStyleClass().add("inventario-btn-guardar");
        btnEditar.setOnAction(e -> abrirModalEditarProducto(p, view));

        // Botón activar/desactivar
        String toggleLabel = p.isActivo() ? "Desactivar" : "Activar";
        String toggleStyle = p.isActivo() ? "prod-btn-danger" : "prod-btn-success";
        String toggleIco = p.isActivo() ? "fas-ban" : "fas-check-circle";
        FontIcon icoToggle = new FontIcon(toggleIco);
        icoToggle.setIconSize(13);
        icoToggle.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnToggle = new Button(toggleLabel, icoToggle);
        btnToggle.getStyleClass().addAll(toggleStyle);
        btnToggle.setOnAction(e -> {
            String accion = p.isActivo() ? "desactivar" : "activar";
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar");
            confirm.setHeaderText(null);
            confirm.setContentText("¿Deseas " + accion + " el producto \"" + p.getNombre() + "\"?");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    try {
                        if (p.isActivo()) productoService.desactivar(p.getId());
                        else productoService.activar(p.getId());
                        recargarDatos();
                        // Reabrir detalle con datos actualizados
                        Producto actualizado = productoService.findById(p.getId());
                        mostrarDetalleProducto(actualizado);
                    } catch (BusinessException be) {
                        Alert err = new Alert(Alert.AlertType.ERROR);
                        err.setHeaderText(null);
                        err.setContentText(be.getMessage());
                        err.showAndWait();
                    }
                }
            });
        });

        footer.getChildren().addAll(btnToggle, btnEditar);

        view.getChildren().addAll(header, body, footer);

        // Animaciones de entrada
        animarEntrada(header, 0);
        animarEntrada(body,   80);
        animarEntrada(footer, 140);

        contentArea.getChildren().setAll(view);
    }

    /** Placeholder grande para la caja de imagen en el detalle */
    private Node buildImgBoxPlaceholder() {
        VBox ph = new VBox(10);
        ph.setAlignment(Pos.CENTER);
        FontIcon ico = new FontIcon("fas-image");
        ico.setIconSize(48);
        ico.setIconColor(Paint.valueOf("#C4BEB7"));
        Label lbl = new Label("Sin imagen");
        lbl.getStyleClass().add("prod-detalle-img-hint");
        ph.getChildren().addAll(ico, lbl);
        return ph;
    }

    /** Crea el encabezado de una sección en el panel de detalle */
    private VBox crearSeccionDetalle(String titulo, String icoLiteral, String icoColor) {
        VBox sec = new VBox(10);
        sec.getStyleClass().add("prod-detalle-section");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon ico = new FontIcon(icoLiteral);
        ico.setIconSize(13);
        ico.setIconColor(Paint.valueOf(icoColor));

        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("prod-detalle-section-label");
        header.getChildren().addAll(ico, lbl);
        sec.getChildren().add(header);
        return sec;
    }

    /** Fila label → valor para el grid de especificaciones */
    private HBox crearFilaDetalle(String etiqueta, String valor) {
        HBox row = new HBox();
        row.getStyleClass().add("prod-detalle-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label lKey = new Label(etiqueta);
        lKey.getStyleClass().add("prod-detalle-key");
        lKey.setMinWidth(160);

        Label lVal = new Label(valor);
        lVal.getStyleClass().add("prod-detalle-val");
        HBox.setHgrow(lVal, Priority.ALWAYS);

        row.getChildren().addAll(lKey, lVal);
        return row;
    }

    private String obtenerExtensionImg(String nombre) {
        int dot = nombre.lastIndexOf('.');
        return dot >= 0 ? nombre.substring(dot + 1).toLowerCase() : "png";
    }

    // ══════════════════════════════════════════════════════════════
    //  MODAL — Editar Producto (abre desde el detalle)
    // ══════════════════════════════════════════════════════════════

    private void abrirModalEditarProducto(Producto producto, VBox detalleView) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) cerrarModal(overlay);
        });

        VBox modal = new VBox(16);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(600);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setAlignment(Pos.TOP_LEFT);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon icoModal = new FontIcon("fas-edit");
        icoModal.setIconSize(20);
        icoModal.setIconColor(Paint.valueOf("#5A6ACF"));
        Label lblTitle = new Label("Editar Producto");
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

        // ── Campos ────────────────────────────────────────────────
        VBox fieldNombre = crearCampo("Nombre del producto *");
        TextField txtNombre = (TextField) fieldNombre.getChildren().get(1);
        txtNombre.setText(producto.getNombre());

        VBox fieldCodigo = crearCampo("Código de barras *");
        TextField txtCodigo = (TextField) fieldCodigo.getChildren().get(1);
        txtCodigo.setText(producto.getCodigoBarras() != null ? producto.getCodigoBarras() : "");

        HBox row1 = new HBox(14, fieldNombre, fieldCodigo);
        HBox.setHgrow(fieldNombre, Priority.ALWAYS);
        HBox.setHgrow(fieldCodigo, Priority.ALWAYS);

        // Categoría
        VBox fieldCategoria = new VBox(6);
        Label lblCat = new Label("Categoría *");
        lblCat.getStyleClass().add("form-label");
        ComboBox<Categoria> cmbCat = new ComboBox<>();
        cmbCat.setPromptText("Seleccionar categoría");
        cmbCat.setMaxWidth(Double.MAX_VALUE);
        try { cmbCat.setItems(FXCollections.observableArrayList(categoriaService.findAllActivas())); }
        catch (Exception ignored) {}
        cmbCat.setConverter(new StringConverter<>() {
            @Override public String toString(Categoria c) { return c == null ? "" : c.getNombre(); }
            @Override public Categoria fromString(String s) { return null; }
        });
        // Pre-seleccionar
        if (producto.getSubcategoria() != null && producto.getSubcategoria().getCategoria() != null) {
            cmbCat.getItems().stream()
                    .filter(c -> c.getId().equals(producto.getSubcategoria().getCategoria().getId()))
                    .findFirst().ifPresent(cmbCat::setValue);
        }
        fieldCategoria.getChildren().addAll(lblCat, cmbCat);

        // Subcategoría
        VBox fieldSubcat = new VBox(6);
        Label lblSubcat = new Label("Subcategoría *");
        lblSubcat.getStyleClass().add("form-label");
        ComboBox<Subcategoria> cmbSubcat = new ComboBox<>();
        cmbSubcat.setPromptText("Seleccionar subcategoría");
        cmbSubcat.setMaxWidth(Double.MAX_VALUE);
        cmbSubcat.setConverter(new StringConverter<>() {
            @Override public String toString(Subcategoria s) { return s == null ? "" : s.getNombre(); }
            @Override public Subcategoria fromString(String str) { return null; }
        });
        // Cargar subcategorías de la categoría actual
        if (cmbCat.getValue() != null) {
            try {
                cmbSubcat.setItems(FXCollections.observableArrayList(
                        subcategoriaService.findByCategoriaId(cmbCat.getValue().getId())));
                cmbSubcat.setDisable(false);
            } catch (Exception ignored) {}
        } else {
            cmbSubcat.setDisable(true);
        }
        // Pre-seleccionar subcategoría
        if (producto.getSubcategoria() != null) {
            cmbSubcat.getItems().stream()
                    .filter(s -> s.getId().equals(producto.getSubcategoria().getId()))
                    .findFirst().ifPresent(cmbSubcat::setValue);
        }
        fieldSubcat.getChildren().addAll(lblSubcat, cmbSubcat);

        cmbCat.valueProperty().addListener((obs, old, cat) -> {
            cmbSubcat.getItems().clear();
            cmbSubcat.setValue(null);
            if (cat != null) {
                try {
                    cmbSubcat.setItems(FXCollections.observableArrayList(
                            subcategoriaService.findByCategoriaId(cat.getId())));
                    cmbSubcat.setDisable(false);
                } catch (Exception ignored) { cmbSubcat.setDisable(true); }
            } else { cmbSubcat.setDisable(true); }
        });

        HBox row2 = new HBox(14, fieldCategoria, fieldSubcat);
        HBox.setHgrow(fieldCategoria, Priority.ALWAYS);
        HBox.setHgrow(fieldSubcat, Priority.ALWAYS);

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
        if (producto.getProveedorPrincipal() != null) {
            cmbProv.getItems().stream()
                    .filter(pr -> pr.getId().equals(producto.getProveedorPrincipal().getId()))
                    .findFirst().ifPresent(cmbProv::setValue);
        }
        fieldProveedor.getChildren().addAll(lblProv, cmbProv);

        // Precio de compra
        VBox fieldPrecio = crearCampo("Precio de compra *");
        TextField txtPrecio = (TextField) fieldPrecio.getChildren().get(1);
        txtPrecio.setText(producto.getPrecioCompra() != null
                ? producto.getPrecioCompra().toPlainString() : "");

        HBox row3 = new HBox(14, fieldProveedor, fieldPrecio);
        HBox.setHgrow(fieldProveedor, Priority.ALWAYS);
        HBox.setHgrow(fieldPrecio, Priority.ALWAYS);

        // Stock
        VBox fieldStock = crearCampo("Stock actual *");
        TextField txtStock = (TextField) fieldStock.getChildren().get(1);
        txtStock.setText(String.valueOf(producto.getStock()));

        HBox row4 = new HBox(14, fieldStock);
        HBox.setHgrow(fieldStock, Priority.ALWAYS);

        // Preview precio
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
                BigDecimal pc = new BigDecimal(txtPrecio.getText().trim());
                Proveedor prov = cmbProv.getValue();
                if (prov == null) { lblPreview.setText("Selecciona proveedor para ver el precio"); return; }
                BigDecimal margen = prov.getPorcentajeGanancia();
                BigDecimal factor = BigDecimal.ONE.add(margen.divide(new BigDecimal("100"), 10, java.math.RoundingMode.HALF_UP));
                BigDecimal pv = pc.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
                lblPreview.setText("Precio de venta estimado: " + FMT_MONEDA.format(pv) + "  (margen " + margen + "%)");
                animarPulse(previewBox);
            } catch (Exception ignored) {
                lblPreview.setText("El precio de venta se calcula automáticamente");
            }
        };
        txtPrecio.textProperty().addListener((obs, o, n) -> calcularPreview.run());
        cmbProv.valueProperty().addListener((obs, o, n) -> calcularPreview.run());
        calcularPreview.run();

        // Error label
        Label lblError = new Label();
        lblError.getStyleClass().add("inventario-error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);

        Separator sep2 = new Separator();

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> cerrarModal(overlay));

        FontIcon icoGuardar = new FontIcon("fas-save");
        icoGuardar.setIconSize(14);
        icoGuardar.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnGuardar = new Button("Guardar Cambios", icoGuardar);
        btnGuardar.getStyleClass().add("inventario-btn-guardar");
        btnGuardar.setOnAction(e -> {
            lblError.setVisible(false);
            lblError.setManaged(false);
            try {
                String nombre = txtNombre.getText().trim();
                String codigo = txtCodigo.getText().trim();
                if (nombre.isEmpty() || codigo.isEmpty()) {
                    mostrarErrorModal(lblError, "El nombre y código de barras son obligatorios."); return;
                }
                if (cmbCat.getValue() == null || cmbSubcat.getValue() == null) {
                    mostrarErrorModal(lblError, "Selecciona una categoría y subcategoría."); return;
                }
                if (cmbProv.getValue() == null) {
                    mostrarErrorModal(lblError, "Selecciona un proveedor."); return;
                }
                BigDecimal precioCompra;
                try {
                    precioCompra = new BigDecimal(txtPrecio.getText().trim());
                    if (precioCompra.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    mostrarErrorModal(lblError, "El precio de compra debe ser mayor a 0."); return;
                }
                int stock;
                try {
                    stock = Integer.parseInt(txtStock.getText().trim());
                    if (stock < 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    mostrarErrorModal(lblError, "El stock debe ser un número entero no negativo."); return;
                }
                Producto actualizado = Producto.builder()
                        .id(producto.getId())
                        .nombre(nombre)
                        .codigoBarras(codigo)
                        .subcategoria(cmbSubcat.getValue())
                        .proveedorPrincipal(cmbProv.getValue())
                        .precioCompra(precioCompra)
                        .stock(stock)
                        .activo(producto.isActivo())
                        .imagenPath(producto.getImagenPath())
                        .build();
                actualizado.calcularPrecioVenta();

                productoService.actualizar(actualizado);

                animarExitoModal(modal, () -> {
                    cerrarModal(overlay);
                    recargarDatos();
                    mostrarDetalleProducto(actualizado);
                });

            } catch (BusinessException be) {
                mostrarErrorModal(lblError, be.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblError, "Error inesperado: " + ex.getMessage());
            }
        });

        actions.getChildren().addAll(btnCancelar, btnGuardar);

        modal.getChildren().addAll(header, sep, row1, row2, row3, row4, previewBox, lblError, sep2, actions);

        overlay.getChildren().add(modal);
        rootStack.getChildren().add(overlay);
        animarEntradaModal(overlay, modal);

        int delay = 60;
        for (Node child : List.of(row1, row2, row3, row4, previewBox, actions)) {
            child.setOpacity(0);
            child.setTranslateX(-8);
            FadeTransition ft = new FadeTransition(Duration.millis(250), child);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.setInterpolator(Interpolator.EASE_OUT);
            TranslateTransition tt = new TranslateTransition(Duration.millis(250), child);
            tt.setFromX(-8); tt.setToX(0);
            tt.setDelay(Duration.millis(delay));
            tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
            delay += 50;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PANTALLA 4 — Ajuste de Stock
    // ══════════════════════════════════════════════════════════════

    private void mostrarAjusteStock() {
        activarTab(tabAjuste);
        recargarDatos();

        // ── Contenedor raíz ───────────────────────────────────────
        VBox view = new VBox(0);
        view.getStyleClass().add("ajuste-root");
        VBox.setVgrow(view, Priority.ALWAYS);

        // ── Toolbar de búsqueda ───────────────────────────────────
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("inventario-toolbar");
        toolbar.setPadding(new Insets(16, 28, 12, 28));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        StackPane searchBox = new StackPane();
        searchBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon icoSearch = new FontIcon("fas-search");
        icoSearch.setIconSize(14);
        icoSearch.setIconColor(Paint.valueOf("#A8A29E"));
        StackPane.setMargin(icoSearch, new Insets(0, 0, 0, 12));

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar producto por nombre o código de barras…");
        txtBuscar.getStyleClass().add("inventario-search-field");
        txtBuscar.setPrefWidth(380);
        searchBox.getChildren().addAll(txtBuscar, icoSearch);
        txtBuscar.focusedProperty().addListener((obs, old, focused) ->
                icoSearch.setIconColor(Paint.valueOf(focused ? "#5A6ACF" : "#A8A29E")));

        // Filtro activos/todos
        ComboBox<String> cmbFiltro = new ComboBox<>();
        cmbFiltro.setItems(FXCollections.observableArrayList("Solo activos", "Todos"));
        cmbFiltro.setValue("Solo activos");
        cmbFiltro.getStyleClass().addAll("combo-box", "inventario-filter");
        cmbFiltro.setPrefWidth(150);

        Label lblConteo = new Label(todosProductos.size() + " productos");
        lblConteo.getStyleClass().add("inventario-count-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(searchBox, cmbFiltro, spacer, lblConteo);

        // ── Cuerpo: lista izquierda + panel derecho ───────────────
        HBox body = new HBox(0);
        VBox.setVgrow(body, Priority.ALWAYS);

        // Lista de productos (panel izquierdo)
        VBox listaPanel = new VBox(0);
        listaPanel.getStyleClass().add("ajuste-lista-panel");
        listaPanel.setMinWidth(380);
        listaPanel.setMaxWidth(440);
        HBox.setHgrow(listaPanel, Priority.SOMETIMES);

        ScrollPane scrollLista = new ScrollPane();
        scrollLista.setFitToWidth(true);
        scrollLista.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollLista.getStyleClass().add("dashboard-scroll");
        scrollLista.getStyleClass().add("ajuste-lista-scroll");
        VBox.setVgrow(scrollLista, Priority.ALWAYS);

        VBox listaRows = new VBox(0);
        listaRows.getStyleClass().add("ajuste-lista-rows");
        scrollLista.setContent(listaRows);
        listaPanel.getChildren().add(scrollLista);

        // Panel derecho (formulario de ajuste)
        VBox panelDerecho = new VBox(0);
        HBox.setHgrow(panelDerecho, Priority.ALWAYS);
        panelDerecho.getStyleClass().add("ajuste-panel-derecho");

        // Estado: ningún producto seleccionado
        mostrarPlaceholderAjuste(panelDerecho);

        // ── Referencia mutable al row seleccionado ─────────────────
        final Node[] rowSeleccionado = {null};

        // ── Función que llena la lista con resultados ──────────────
        // Wrapped in array so the lambda can call itself (self-reference)
        Runnable[] actualizarListaRef = {null};
        actualizarListaRef[0] = () -> {
            recargarDatos();
            listaRows.getChildren().clear();
            String texto = txtBuscar.getText().toLowerCase().trim();
            boolean soloActivos = "Solo activos".equals(cmbFiltro.getValue());

            List<Producto> filtrados = todosProductos.stream()
                    .filter(p -> !soloActivos || p.isActivo())
                    .filter(p -> {
                        if (texto.isEmpty()) return true;
                        boolean matchNombre = p.getNombre() != null
                                && p.getNombre().toLowerCase().contains(texto);
                        boolean matchCodigo = p.getCodigoBarras() != null
                                && p.getCodigoBarras().toLowerCase().contains(texto);
                        return matchNombre || matchCodigo;
                    })
                    .sorted(Comparator.comparing(Producto::getNombre))
                    .collect(Collectors.toList());

            lblConteo.setText(filtrados.size() + " producto" + (filtrados.size() == 1 ? "" : "s"));

            if (filtrados.isEmpty()) {
                VBox empty = new VBox(10);
                empty.setAlignment(Pos.CENTER);
                empty.setPadding(new Insets(40));
                FontIcon icoEmpty = new FontIcon("fas-box-open");
                icoEmpty.setIconSize(32);
                icoEmpty.setIconColor(Paint.valueOf("#A8A29E"));
                Label lEmpty = new Label("No se encontraron productos");
                lEmpty.getStyleClass().add("inventario-empty");
                empty.getChildren().addAll(icoEmpty, lEmpty);
                listaRows.getChildren().add(empty);
                return;
            }

            for (Producto p : filtrados) {
                HBox row = buildAjusteRow(p);
                row.setOnMouseClicked(e -> {
                    // Deseleccionar fila previa
                    if (rowSeleccionado[0] != null)
                        rowSeleccionado[0].getStyleClass().remove("ajuste-row-selected");
                    row.getStyleClass().add("ajuste-row-selected");
                    rowSeleccionado[0] = row;

                    // Actualizar panel derecho con el formulario
                    mostrarFormularioAjuste(p, panelDerecho, () -> {
                        if (rowSeleccionado[0] != null)
                            rowSeleccionado[0].getStyleClass().remove("ajuste-row-selected");
                        rowSeleccionado[0] = null;
                        mostrarPlaceholderAjuste(panelDerecho);
                        actualizarListaRef[0].run(); // refresca BD + lista correctamente
                    });
                });
                listaRows.getChildren().add(row);
            }
        };

        txtBuscar.textProperty().addListener((obs, o, n) -> actualizarListaRef[0].run());
        cmbFiltro.valueProperty().addListener((obs, o, n) -> actualizarListaRef[0].run());
        actualizarListaRef[0].run();

        body.getChildren().addAll(listaPanel, panelDerecho);
        view.getChildren().addAll(toolbar, body);

        animarEntrada(toolbar, 0);
        animarEntrada(body, 80);

        contentArea.getChildren().setAll(view);
    }

    /** Fila de producto en la lista de ajuste de stock */
    private HBox buildAjusteRow(Producto p) {
        HBox row = new HBox(14);
        row.getStyleClass().add("ajuste-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(javafx.scene.Cursor.HAND);

        // Miniatura
        row.getChildren().add(buildThumbnailCell(p.getImagenPath()));

        // Info central
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblNom = new Label(p.getNombre());
        lblNom.getStyleClass().add("ajuste-row-nombre");

        String catStr = "";
        if (p.getSubcategoria() != null) {
            catStr = p.getSubcategoria().getNombre();
            if (p.getSubcategoria().getCategoria() != null)
                catStr = p.getSubcategoria().getCategoria().getNombre() + " · " + catStr;
        }
        Label lblCat = new Label(catStr.isEmpty() ? "Sin categoría" : catStr);
        lblCat.getStyleClass().add("ajuste-row-cat");

        if (p.getCodigoBarras() != null && !p.getCodigoBarras().isBlank()) {
            Label lblCod = new Label(p.getCodigoBarras());
            lblCod.getStyleClass().add("ajuste-row-cod");
            info.getChildren().addAll(lblNom, lblCat, lblCod);
        } else {
            info.getChildren().addAll(lblNom, lblCat);
        }

        // Badge de stock
        Label stockBadge = new Label(String.valueOf(p.getStock()));
        stockBadge.getStyleClass().addAll("dash-badge",
                p.getStock() == 0 ? "dash-badge-danger"
                : p.getStock() <= 5 ? "dash-badge-credito" : "dash-badge-success");

        // Indicador de estado
        Label estadoBadge = new Label(p.isActivo() ? "Activo" : "Inactivo");
        estadoBadge.getStyleClass().addAll("dash-badge",
                p.isActivo() ? "dash-badge-success" : "dash-badge-danger");

        VBox badges = new VBox(5);
        badges.setAlignment(Pos.CENTER_RIGHT);
        badges.getChildren().addAll(stockBadge, estadoBadge);

        row.getChildren().addAll(info, badges);
        return row;
    }

    /** Placeholder cuando no hay producto seleccionado en el panel derecho */
    private void mostrarPlaceholderAjuste(VBox panelDerecho) {
        VBox ph = new VBox(14);
        ph.setAlignment(Pos.CENTER);
        ph.getStyleClass().add("ajuste-placeholder");
        VBox.setVgrow(ph, Priority.ALWAYS);

        StackPane icoBox = new StackPane();
        icoBox.getStyleClass().add("ajuste-placeholder-ico-box");
        icoBox.setMinSize(72, 72);
        icoBox.setMaxSize(72, 72);
        FontIcon ico = new FontIcon("fas-hand-pointer");
        ico.setIconSize(28);
        ico.setIconColor(Paint.valueOf("#5A6ACF"));
        icoBox.getChildren().add(ico);

        Label lbl = new Label("Selecciona un producto");
        lbl.getStyleClass().add("ajuste-placeholder-title");

        Label sub = new Label("Haz clic en cualquier producto de la lista\npara ajustar su stock.");
        sub.getStyleClass().add("ajuste-placeholder-sub");
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        ph.getChildren().addAll(icoBox, lbl, sub);
        panelDerecho.getChildren().setAll(ph);
    }

    /** Formulario de ajuste para un producto seleccionado */
    private void mostrarFormularioAjuste(Producto productoIn, VBox panelDerecho, Runnable onSuccess) {
        // Refrescar desde BD
        Producto p;
        try { p = productoService.findById(productoIn.getId()); }
        catch (Exception e) { p = productoIn; }
        final Producto producto = p;

        VBox form = new VBox(0);
        form.getStyleClass().add("ajuste-form");
        VBox.setVgrow(form, Priority.ALWAYS);

        // ── Cabecera del producto ─────────────────────────────────
        HBox prodHeader = new HBox(14);
        prodHeader.getStyleClass().add("ajuste-form-prod-header");
        prodHeader.setAlignment(Pos.CENTER_LEFT);

        // Imagen
        StackPane imgWrap = buildThumbnailCell(producto.getImagenPath());
        imgWrap.setMinSize(56, 56);
        imgWrap.setMaxSize(56, 56);

        VBox prodInfo = new VBox(4);
        HBox.setHgrow(prodInfo, Priority.ALWAYS);
        Label lblProdNombre = new Label(producto.getNombre());
        lblProdNombre.getStyleClass().add("ajuste-form-prod-nombre");
        String catStr = "";
        if (producto.getSubcategoria() != null) {
            catStr = producto.getSubcategoria().getNombre();
            if (producto.getSubcategoria().getCategoria() != null)
                catStr = producto.getSubcategoria().getCategoria().getNombre() + " · " + catStr;
        }
        Label lblProdCat = new Label(catStr.isEmpty() ? "Sin categoría" : catStr);
        lblProdCat.getStyleClass().add("ajuste-form-prod-cat");
        prodInfo.getChildren().addAll(lblProdNombre, lblProdCat);

        // Stock actual con badge
        VBox stockBox = new VBox(3);
        stockBox.setAlignment(Pos.CENTER_RIGHT);
        Label lblStockTitle = new Label("STOCK ACTUAL");
        lblStockTitle.getStyleClass().add("ajuste-form-stock-label");
        Label stockBadge = new Label(String.valueOf(producto.getStock()));
        stockBadge.getStyleClass().addAll("ajuste-form-stock-value",
                producto.getStock() == 0 ? "ajuste-stock-cero"
                : producto.getStock() <= 5 ? "ajuste-stock-bajo" : "ajuste-stock-ok");
        stockBox.getChildren().addAll(lblStockTitle, stockBadge);

        prodHeader.getChildren().addAll(imgWrap, prodInfo, stockBox);

        Separator sep1 = new Separator();
        sep1.getStyleClass().add("ajuste-sep");

        // ── Tipo de movimiento ────────────────────────────────────
        VBox tipoSection = new VBox(10);
        tipoSection.getStyleClass().add("ajuste-form-section");

        Label lblTipo = new Label("Tipo de movimiento");
        lblTipo.getStyleClass().add("ajuste-form-section-title");

        HBox toggleBox = new HBox(8);
        toggleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon icoEntrada = new FontIcon("fas-arrow-down");
        icoEntrada.setIconSize(13);
        icoEntrada.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnEntrada = new Button("Entrada", icoEntrada);
        btnEntrada.getStyleClass().addAll("ajuste-toggle-btn", "ajuste-toggle-active-entrada");

        FontIcon icoSalida = new FontIcon("fas-arrow-up");
        icoSalida.setIconSize(13);
        icoSalida.setIconColor(Paint.valueOf("#78716C"));
        Button btnSalida = new Button("Salida", icoSalida);
        btnSalida.getStyleClass().add("ajuste-toggle-btn");

        final boolean[] esEntrada = {true};

        btnEntrada.setOnAction(e -> {
            esEntrada[0] = true;
            btnEntrada.getStyleClass().removeAll("ajuste-toggle-inactive");
            btnEntrada.getStyleClass().add("ajuste-toggle-active-entrada");
            btnSalida.getStyleClass().removeAll("ajuste-toggle-active-salida", "ajuste-toggle-active-entrada");
            btnSalida.getStyleClass().add("ajuste-toggle-inactive");
            icoEntrada.setIconColor(Paint.valueOf("#FFFFFF"));
            icoSalida.setIconColor(Paint.valueOf("#78716C"));
        });
        btnSalida.setOnAction(e -> {
            esEntrada[0] = false;
            btnSalida.getStyleClass().removeAll("ajuste-toggle-inactive");
            btnSalida.getStyleClass().add("ajuste-toggle-active-salida");
            btnEntrada.getStyleClass().removeAll("ajuste-toggle-active-entrada", "ajuste-toggle-inactive");
            btnEntrada.getStyleClass().add("ajuste-toggle-inactive");
            icoSalida.setIconColor(Paint.valueOf("#FFFFFF"));
            icoEntrada.setIconColor(Paint.valueOf("#78716C"));
        });

        toggleBox.getChildren().addAll(btnEntrada, btnSalida);
        tipoSection.getChildren().addAll(lblTipo, toggleBox);

        // ── Cantidad ──────────────────────────────────────────────
        VBox cantidadSection = new VBox(10);
        cantidadSection.getStyleClass().add("ajuste-form-section");

        Label lblCantidad = new Label("Cantidad");
        lblCantidad.getStyleClass().add("ajuste-form-section-title");

        HBox cantidadBox = new HBox(0);
        cantidadBox.setAlignment(Pos.CENTER_LEFT);
        cantidadBox.getStyleClass().add("ajuste-cantidad-box");

        Button btnMenos = new Button("−");
        btnMenos.getStyleClass().add("ajuste-qty-btn");

        TextField txtCantidad = new TextField("1");
        txtCantidad.getStyleClass().add("ajuste-qty-field");
        txtCantidad.setPrefWidth(80);
        txtCantidad.setAlignment(Pos.CENTER);

        // Solo números
        txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) txtCantidad.setText(oldVal);
            if (!newVal.isEmpty() && newVal.startsWith("0"))
                txtCantidad.setText(newVal.replaceFirst("^0+", ""));
        });

        Button btnMas = new Button("+");
        btnMas.getStyleClass().add("ajuste-qty-btn");

        btnMenos.setOnAction(e -> {
            try {
                int v = Integer.parseInt(txtCantidad.getText());
                if (v > 1) txtCantidad.setText(String.valueOf(v - 1));
            } catch (NumberFormatException ignored) { txtCantidad.setText("1"); }
        });
        btnMas.setOnAction(e -> {
            try {
                int v = Integer.parseInt(txtCantidad.getText());
                txtCantidad.setText(String.valueOf(v + 1));
            } catch (NumberFormatException ignored) { txtCantidad.setText("1"); }
        });

        cantidadBox.getChildren().addAll(btnMenos, txtCantidad, btnMas);
        cantidadSection.getChildren().addAll(lblCantidad, cantidadBox);

        // ── Notas (opcional) ──────────────────────────────────────
        VBox notasSection = new VBox(8);
        notasSection.getStyleClass().add("ajuste-form-section");

        Label lblNotas = new Label("Motivo / Notas  (opcional)");
        lblNotas.getStyleClass().add("ajuste-form-section-title");

        TextField txtNotas = new TextField();
        txtNotas.setPromptText("Ej: recepción de mercancía, corrección de inventario…");
        txtNotas.getStyleClass().add("inventario-search-field");
        txtNotas.setMaxWidth(Double.MAX_VALUE);
        notasSection.getChildren().addAll(lblNotas, txtNotas);

        // ── Error label ───────────────────────────────────────────
        Label lblError = new Label();
        lblError.getStyleClass().add("inventario-error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // ── Botones de acción ─────────────────────────────────────
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("ajuste-form-actions");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> {
            mostrarPlaceholderAjuste(panelDerecho);
            onSuccess.run();
        });

        FontIcon icoAplicar = new FontIcon("fas-check");
        icoAplicar.setIconSize(14);
        icoAplicar.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnAplicar = new Button("Aplicar Ajuste", icoAplicar);
        btnAplicar.getStyleClass().add("inventario-btn-guardar");
        btnAplicar.setOnAction(e -> {
            lblError.setVisible(false);
            lblError.setManaged(false);
            int cantidad;
            try {
                cantidad = Integer.parseInt(txtCantidad.getText().trim());
                if (cantidad <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                mostrarErrorModal(lblError, "La cantidad debe ser un número entero mayor que cero.");
                return;
            }
            if (!esEntrada[0] && cantidad > producto.getStock()) {
                mostrarErrorModal(lblError, "No hay suficiente stock. Disponible: " + producto.getStock());
                return;
            }
            try {
                productoService.ajustarStock(producto.getId(), cantidad, esEntrada[0]);
                recargarDatos();

                // Flash de éxito en el badge de stock
                String tipoStr = esEntrada[0] ? "entrada" : "salida";
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Ajuste aplicado");
                ok.setHeaderText(null);
                ok.setContentText("Se registró una " + tipoStr + " de " + cantidad
                        + " unidad" + (cantidad == 1 ? "" : "es")
                        + " para \"" + producto.getNombre() + "\".");
                ok.showAndWait();
                onSuccess.run();
            } catch (BusinessException be) {
                mostrarErrorModal(lblError, be.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblError, "Error inesperado: " + ex.getMessage());
            }
        });

        actions.getChildren().addAll(btnCancelar, btnAplicar);

        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);

        form.getChildren().addAll(
                prodHeader, sep1,
                tipoSection, cantidadSection, notasSection,
                lblError, vSpacer, actions);

        panelDerecho.getChildren().setAll(form);
        animarEntrada(form, 0);
    }

    private void recargarDatos() {
        try { todosProductos = productoService.findAll(); }
        catch (Exception e) { todosProductos = new ArrayList<>(); }
    }
}
