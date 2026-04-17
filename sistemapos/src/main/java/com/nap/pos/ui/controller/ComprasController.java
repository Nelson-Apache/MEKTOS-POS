package com.nap.pos.ui.controller;

import com.nap.pos.application.service.CategoriaService;
import com.nap.pos.application.service.CompraService;
import com.nap.pos.application.service.ProductoService;
import com.nap.pos.application.service.ProveedorService;
import com.nap.pos.application.service.SubcategoriaService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.model.Compra;
import com.nap.pos.domain.model.DetalleCompra;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.Proveedor;
import com.nap.pos.domain.model.Subcategoria;
import com.nap.pos.domain.model.Usuario;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ComprasController {

    private final CompraService       compraService;
    private final ProveedorService    proveedorService;
    private final ProductoService     productoService;
    private final CategoriaService    categoriaService;
    private final SubcategoriaService subcategoriaService;

    private static final NumberFormat     FMT  = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
    private static final DateTimeFormatter DFT  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Estado ────────────────────────────────────────────────────
    private Usuario       usuarioActual;
    private List<Compra>  todasCompras = new ArrayList<>();

    // ── Refs UI ───────────────────────────────────────────────────
    private StackPane rootStack;
    private VBox      contentArea;
    private Button    tabDashboard;
    private Button    tabHistorial;

    // ─────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────

    public Node buildView(Usuario usuario) {
        this.usuarioActual = usuario;
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

        mostrarDashboard();
        return rootStack;
    }

    private void recargarDatos() {
        try { todasCompras = compraService.findAll(); }
        catch (Exception e) { todasCompras = new ArrayList<>(); }
    }

    // ─────────────────────────────────────────────────────────────
    // Tab bar
    // ─────────────────────────────────────────────────────────────

    private HBox buildTabBar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("inventario-tab-bar");
        bar.setPadding(new Insets(14, 28, 10, 28));

        FontIcon icoDash = new FontIcon("fas-chart-bar");
        icoDash.setIconSize(14);
        icoDash.setIconColor(Paint.valueOf("#5A6ACF"));
        tabDashboard = new Button("Dashboard", icoDash);
        tabDashboard.getStyleClass().addAll("inventario-tab", "inventario-tab-active");
        tabDashboard.setOnAction(e -> { animarClickTab(tabDashboard); mostrarDashboard(); });

        FontIcon icoHist = new FontIcon("fas-history");
        icoHist.setIconSize(14);
        icoHist.setIconColor(Paint.valueOf("#78716C"));
        tabHistorial = new Button("Historial", icoHist);
        tabHistorial.getStyleClass().add("inventario-tab");
        tabHistorial.setOnAction(e -> { animarClickTab(tabHistorial); mostrarHistorial(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        FontIcon icoNueva = new FontIcon("fas-plus");
        icoNueva.setIconSize(13);
        icoNueva.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnNueva = new Button("Nueva Compra", icoNueva);
        btnNueva.getStyleClass().add("btn-primario");
        btnNueva.setStyle("-fx-padding: 8 18 8 18; -fx-font-size: 13px;");
        btnNueva.setOnAction(e -> abrirModalNuevaCompra());

        bar.getChildren().addAll(tabDashboard, tabHistorial, spacer, btnNueva);
        return bar;
    }

    private void activarTab(Button activo) {
        tabDashboard.getStyleClass().remove("inventario-tab-active");
        tabHistorial.getStyleClass().remove("inventario-tab-active");
        activo.getStyleClass().add("inventario-tab-active");

        String inactivo = "#78716C";
        String actColor = "#5A6ACF";
        ((FontIcon) tabDashboard.getGraphic()).setIconColor(Paint.valueOf(activo == tabDashboard ? actColor : inactivo));
        ((FontIcon) tabHistorial.getGraphic()).setIconColor(Paint.valueOf(activo == tabHistorial  ? actColor : inactivo));
    }

    // ─────────────────────────────────────────────────────────────
    // Vista: Dashboard
    // ─────────────────────────────────────────────────────────────

    private void mostrarDashboard() {
        activarTab(tabDashboard);
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("inventario-root-stack");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox inner = new VBox(28);
        inner.setPadding(new Insets(28, 32, 32, 32));

        // ── KPI cards ─────────────────────────────────────────────
        long totalCompras  = todasCompras.size();
        BigDecimal totalGastado = todasCompras.stream()
                .map(c -> c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long proveedoresDistintos = todasCompras.stream()
                .filter(c -> c.getProveedor() != null)
                .map(c -> c.getProveedor().getId())
                .distinct().count();
        long productosDistintos = todasCompras.stream()
                .flatMap(c -> c.getDetalles().stream())
                .map(d -> d.getProducto().getId())
                .distinct().count();

        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
            crearKpiCard("fas-shopping-bag",   "#5A6ACF", "Total compras",         String.valueOf(totalCompras)),
            crearKpiCard("fas-dollar-sign",    "#15803D", "Total gastado",         FMT.format(totalGastado)),
            crearKpiCard("fas-truck",          "#D97706", "Proveedores activos",   String.valueOf(proveedoresDistintos)),
            crearKpiCard("fas-boxes",          "#7C3AED", "Productos comprados",   String.valueOf(productosDistintos))
        );

        // ── Sección título ─────────────────────────────────────────
        Label lblRecientes = new Label("Compras recientes");
        lblRecientes.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        // ── Lista de últimas 10 compras ────────────────────────────
        VBox listaRecientes = new VBox(8);
        List<Compra> recientes = todasCompras.stream()
                .sorted(Comparator.comparing(Compra::getFecha, Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toList());

        if (recientes.isEmpty()) {
            Label lv = new Label("Sin compras registradas todavía.");
            lv.setStyle("-fx-font-size: 13px; -fx-text-fill: #A8A29E;");
            lv.setPadding(new Insets(12, 0, 0, 0));
            listaRecientes.getChildren().add(lv);
        } else {
            for (int i = 0; i < recientes.size(); i++) {
                HBox row = crearFilaReciente(recientes.get(i));
                listaRecientes.getChildren().add(row);
                animarEntrada(row, i * 30);
            }
        }

        // ── Gráficas ───────────────────────────────────────────────
        HBox chartsRow = new HBox(16);
        VBox chartGasto      = crearGastoMensualCard();
        VBox chartProveedores = crearTopProveedoresCard();
        HBox.setHgrow(chartGasto,       Priority.ALWAYS);
        HBox.setHgrow(chartProveedores, Priority.ALWAYS);
        chartsRow.getChildren().addAll(chartGasto, chartProveedores);

        inner.getChildren().addAll(kpiRow, new Separator(), chartsRow, new Separator(), lblRecientes, listaRecientes);
        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    private VBox crearKpiCard(String iconLiteral, String color, String titulo, String valor) {
        VBox card = new VBox(8);
        card.getStyleClass().add("inventario-stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 8px;");
        icoCircle.setMinSize(36, 36);
        icoCircle.setMaxSize(36, 36);
        FontIcon ico = new FontIcon(iconLiteral);
        ico.setIconSize(16);
        ico.setIconColor(Paint.valueOf(color));
        icoCircle.getChildren().add(ico);

        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");

        card.getChildren().addAll(icoCircle, lblValor, lblTitulo);
        return card;
    }

    private VBox crearGastoMensualCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lTit = new Label("GASTO MENSUAL");
        lTit.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);
        FontIcon ico = new FontIcon("fas-chart-bar");
        ico.setIconSize(14); ico.setIconColor(Paint.valueOf("#94A3B8"));
        header.getChildren().addAll(lTit, ico);

        // Últimos 6 meses
        DateTimeFormatter mesYFmt = DateTimeFormatter.ofPattern("MMM yy", Locale.of("es", "CO"));
        YearMonth ahora = YearMonth.now();
        Map<String, BigDecimal> gastoMes = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            gastoMes.put(ahora.minusMonths(i).format(mesYFmt), BigDecimal.ZERO);
        }
        for (Compra c : todasCompras) {
            if (c.getFecha() == null) continue;
            String mes = YearMonth.from(c.getFecha()).format(mesYFmt);
            if (gastoMes.containsKey(mes)) {
                gastoMes.merge(mes, c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO, BigDecimal::add);
            }
        }

        boolean sinDatos = gastoMes.values().stream().allMatch(v -> v.compareTo(BigDecimal.ZERO) == 0);
        if (sinDatos) {
            card.getChildren().addAll(header, buildChartEmptyState("fas-calendar-alt",
                    "Sin compras recientes",
                    "Registra compras a proveedores para ver el historial mensual"));
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setLabel("");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.getStyleClass().add("inventario-bar-chart");
        chart.setPrefHeight(220);
        VBox.setVgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        gastoMes.forEach((mes, valor) -> series.getData().add(new XYChart.Data<>(mes, valor)));
        chart.getData().add(series);

        card.getChildren().addAll(header, chart);
        return card;
    }

    private VBox crearTopProveedoresCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lTit = new Label("TOP PROVEEDORES");
        lTit.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);
        FontIcon ico = new FontIcon("fas-truck");
        ico.setIconSize(14); ico.setIconColor(Paint.valueOf("#94A3B8"));
        header.getChildren().addAll(lTit, ico);

        // Gasto total por proveedor, top 5
        Map<String, BigDecimal> porProveedor = todasCompras.stream()
                .filter(c -> c.getProveedor() != null && c.getTotal() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getProveedor().getNombre(),
                        Collectors.reducing(BigDecimal.ZERO, Compra::getTotal, BigDecimal::add)));

        if (porProveedor.isEmpty()) {
            card.getChildren().addAll(header, buildChartEmptyState("fas-truck",
                    "Sin compras a proveedores",
                    "Registra compras para ver el ranking de proveedores"));
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setLabel("");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.getStyleClass().add("inventario-bar-chart");
        chart.setPrefHeight(220);
        VBox.setVgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        porProveedor.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        chart.getData().add(series);

        card.getChildren().addAll(header, chart);
        return card;
    }

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

    private HBox crearFilaReciente(Compra c) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 10px; " +
                     "-fx-border-color: rgba(26,31,46,0.08); -fx-border-width: 1; " +
                     "-fx-border-radius: 10px; -fx-padding: 12 16 12 16;");
        row.setOnMouseEntered(e -> row.setStyle(row.getStyle().replace("#FDFCFA", "#F5F1EB")));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("#F5F1EB", "#FDFCFA")));

        // Ícono
        StackPane ico = new StackPane();
        ico.setStyle("-fx-background-color: #5A6ACF22; -fx-background-radius: 8px;");
        ico.setMinSize(34, 34); ico.setMaxSize(34, 34);
        FontIcon fi = new FontIcon("fas-file-invoice");
        fi.setIconSize(14); fi.setIconColor(Paint.valueOf("#5A6ACF"));
        ico.getChildren().add(fi);

        // Info
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        String proveedor = c.getProveedor() != null ? c.getProveedor().getNombre() : "Sin proveedor";
        Label lProv = new Label(proveedor);
        lProv.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1A1F2E;");
        String factura = c.getNumeroFactura() != null && !c.getNumeroFactura().isBlank()
                ? "Factura #" + c.getNumeroFactura() : "Sin número de factura";
        Label lFact = new Label(factura + "  ·  " + c.getDetalles().size() + " productos");
        lFact.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
        info.getChildren().addAll(lProv, lFact);

        // Fecha
        Label lFecha = new Label(c.getFecha() != null ? c.getFecha().format(DFT) : "—");
        lFecha.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");

        // Total
        Label lTotal = new Label(FMT.format(c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO));
        lTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #15803D;");

        row.getChildren().addAll(ico, info, lFecha, lTotal);
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // Vista: Historial
    // ─────────────────────────────────────────────────────────────

    private void mostrarHistorial() {
        activarTab(tabHistorial);
        contentArea.getChildren().clear();

        VBox wrapper = new VBox(0);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        wrapper.setPadding(new Insets(24, 28, 28, 28));
        wrapper.setSpacing(16);

        // Toolbar: busqueda
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lblTit = new Label("Historial de compras");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por proveedor o factura...");
        txtBuscar.getStyleClass().add("venta-search-field");
        txtBuscar.setPrefWidth(260);

        toolbar.getChildren().addAll(lblTit, txtBuscar);

        // Tabla
        TableView<Compra> tabla = new TableView<>();
        tabla.getStyleClass().add("inventario-table-card");
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin compras registradas."));

        TableColumn<Compra, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? d.getValue().getFecha().format(DFT) : "—"));
        colFecha.setPrefWidth(130);

        TableColumn<Compra, String> colProveedor = new TableColumn<>("Proveedor");
        colProveedor.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProveedor() != null ? d.getValue().getProveedor().getNombre() : "—"));

        TableColumn<Compra, String> colFactura = new TableColumn<>("N° Factura");
        colFactura.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNumeroFactura() != null ? d.getValue().getNumeroFactura() : "—"));
        colFactura.setPrefWidth(120);

        TableColumn<Compra, String> colProductos = new TableColumn<>("Productos");
        colProductos.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getDetalles().size())));
        colProductos.setPrefWidth(80);
        colProductos.setStyle("-fx-alignment: CENTER;");

        TableColumn<Compra, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(
                FMT.format(d.getValue().getTotal() != null ? d.getValue().getTotal() : BigDecimal.ZERO)));
        colTotal.setPrefWidth(130);
        colTotal.setCellFactory(tc -> {
            TableCell<Compra, String> cell = new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setText(null); setStyle(""); }
                    else { setText(s); setStyle("-fx-font-weight: 700; -fx-text-fill: #15803D;"); }
                }
            };
            return cell;
        });

        // Columna de acción rápida (ver detalle)
        TableColumn<Compra, String> colAccion = new TableColumn<>("");
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
                        mostrarDetalleCompra(getTableRow().getItem());
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

        List<TableColumn<Compra, ?>> cols = List.of(colFecha, colProveedor, colFactura, colProductos, colTotal, colAccion);
        tabla.getColumns().addAll(cols);

        // Estilo filas + click para ver detalle
        tabla.setRowFactory(tv -> {
            TableRow<Compra> row = new TableRow<>();
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle("-fx-background-color: rgba(90,106,207,0.05); -fx-cursor: hand;"); });
            row.setOnMouseExited(e -> row.setStyle(""));
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && row.getItem() != null) {
                    mostrarDetalleCompra(row.getItem());
                }
            });
            return row;
        });

        // Datos iniciales y filtrado
        List<Compra> comprasOrdenadas = todasCompras.stream()
                .sorted(Comparator.comparing(Compra::getFecha, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        tabla.setItems(FXCollections.observableArrayList(comprasOrdenadas));

        txtBuscar.textProperty().addListener((obs, o, n) -> {
            String texto = n == null ? "" : n.toLowerCase();
            if (texto.isBlank()) {
                tabla.setItems(FXCollections.observableArrayList(comprasOrdenadas));
            } else {
                tabla.setItems(FXCollections.observableArrayList(
                    comprasOrdenadas.stream()
                        .filter(c -> {
                            boolean matchProv = c.getProveedor() != null
                                    && c.getProveedor().getNombre().toLowerCase().contains(texto);
                            boolean matchFact = c.getNumeroFactura() != null
                                    && c.getNumeroFactura().toLowerCase().contains(texto);
                            return matchProv || matchFact;
                        })
                        .collect(Collectors.toList())
                ));
            }
        });

        wrapper.getChildren().addAll(toolbar, tabla);
        contentArea.getChildren().add(wrapper);
        animarEntrada(wrapper, 0);
    }

    // ─────────────────────────────────────────────────────────────
    // Detalle de compra (view switching)
    // ─────────────────────────────────────────────────────────────

    private void mostrarDetalleCompra(Compra compra) {
        String provNombre = compra.getProveedor() != null ? compra.getProveedor().getNombre() : "Sin proveedor";

        VBox view = new VBox(0);
        view.getStyleClass().add("prod-detalle-root");
        VBox.setVgrow(view, Priority.ALWAYS);

        // ── Header / breadcrumb ───────────────────────────────────
        HBox header = new HBox(10);
        header.getStyleClass().add("prod-detalle-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Button btnVolver = new Button("Historial");
        FontIcon icoBack = new FontIcon("fas-arrow-left");
        icoBack.setIconSize(13); icoBack.setIconColor(Paint.valueOf("#5A6ACF"));
        btnVolver.setGraphic(icoBack);
        btnVolver.getStyleClass().add("prod-detalle-back-btn");
        btnVolver.setOnAction(e -> mostrarHistorial());

        Label lblSep = new Label("›");
        lblSep.getStyleClass().add("prod-detalle-breadcrumb-sep");

        Label lblTitulo = new Label(provNombre);
        lblTitulo.getStyleClass().add("prod-detalle-title");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        String fechaStr = compra.getFecha() != null ? compra.getFecha().format(DFT) : "—";
        Label badgeFecha = new Label(fechaStr);
        badgeFecha.setStyle("-fx-background-color: #F5F1EB; -fx-background-radius: 6; " +
                "-fx-padding: 4 10; -fx-font-size: 11px; -fx-text-fill: #78716C;");

        header.getChildren().addAll(btnVolver, lblSep, lblTitulo, badgeFecha);

        // ── Cuerpo con scroll ─────────────────────────────────────
        VBox body = new VBox(24);
        body.setPadding(new Insets(24, 32, 32, 32));
        VBox.setVgrow(body, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dashboard-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Tarjetas resumen ──────────────────────────────────────
        String facturaStr = (compra.getNumeroFactura() != null && !compra.getNumeroFactura().isBlank())
                ? "#" + compra.getNumeroFactura() : "Sin número";
        String totalStr = FMT.format(compra.getTotal() != null ? compra.getTotal() : BigDecimal.ZERO);
        int numProductos = compra.getDetalles().size();

        HBox cards = new HBox(14);
        cards.getChildren().addAll(
            buildInfoCard("fas-truck",          "#5A6ACF", "Proveedor",    provNombre),
            buildInfoCard("fas-file-invoice",   "#D97706", "N° Factura",   facturaStr),
            buildInfoCard("fas-boxes",          "#7C3AED", "Productos",    String.valueOf(numProductos)),
            buildInfoCard("fas-dollar-sign",    "#15803D", "Total",        totalStr)
        );

        // ── Sección: productos comprados ──────────────────────────
        HBox secHeader = new HBox(8);
        secHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon icoSec = new FontIcon("fas-shopping-basket");
        icoSec.setIconSize(13); icoSec.setIconColor(Paint.valueOf("#5A6ACF"));
        Label lblSec = new Label("PRODUCTOS COMPRADOS");
        lblSec.getStyleClass().add("prod-detalle-section-label");
        secHeader.getChildren().addAll(icoSec, lblSec);

        // Cabecera de la tabla
        HBox tabHead = new HBox();
        tabHead.setPadding(new Insets(9, 16, 9, 16));
        tabHead.setStyle("-fx-background-color: #1A1F2E; -fx-background-radius: 8 8 0 0;");
        tabHead.setAlignment(Pos.CENTER_LEFT);

        Label hNom = new Label("Producto");
        hNom.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        HBox.setHgrow(hNom, Priority.ALWAYS);
        hNom.setMaxWidth(Double.MAX_VALUE);

        Label hCant = new Label("Cant.");
        hCant.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        hCant.setMinWidth(70); hCant.setMaxWidth(70); hCant.setPrefWidth(70);
        hCant.setAlignment(Pos.CENTER);

        Label hUnit = new Label("P. Unitario");
        hUnit.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        hUnit.setMinWidth(130); hUnit.setMaxWidth(130); hUnit.setPrefWidth(130);
        hUnit.setAlignment(Pos.CENTER_RIGHT);

        Label hSub = new Label("Subtotal");
        hSub.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        hSub.setMinWidth(130); hSub.setMaxWidth(130); hSub.setPrefWidth(130);
        hSub.setAlignment(Pos.CENTER_RIGHT);

        tabHead.getChildren().addAll(hNom, hCant, hUnit, hSub);

        // Filas de productos
        VBox filas = new VBox(0);
        filas.setStyle("-fx-border-color: rgba(26,31,46,0.10); -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 8 8;");
        boolean[] par = {true};
        for (var d : compra.getDetalles()) {
            String bg = par[0] ? "#FFFFFF" : "#FDFCFA";
            par[0] = !par[0];

            HBox fila = new HBox();
            fila.setPadding(new Insets(11, 16, 11, 16));
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setStyle("-fx-background-color: " + bg + "; " +
                    "-fx-border-color: transparent transparent rgba(26,31,46,0.06) transparent; -fx-border-width: 1;");

            String nom = d.getProducto() != null ? d.getProducto().getNombre() : "—";
            Label lNom = new Label(nom);
            lNom.setStyle("-fx-font-size: 13px; -fx-text-fill: #1A1F2E;");
            HBox.setHgrow(lNom, Priority.ALWAYS);
            lNom.setMaxWidth(Double.MAX_VALUE);

            Label lCant = new Label(String.valueOf(d.getCantidad()));
            lCant.setStyle("-fx-font-size: 13px; -fx-text-fill: #1A1F2E;");
            lCant.setMinWidth(70); lCant.setMaxWidth(70); lCant.setPrefWidth(70);
            lCant.setAlignment(Pos.CENTER);

            Label lUnit = new Label(FMT.format(d.getPrecioCompraUnitario() != null ? d.getPrecioCompraUnitario() : BigDecimal.ZERO));
            lUnit.setStyle("-fx-font-size: 13px; -fx-text-fill: #78716C;");
            lUnit.setMinWidth(130); lUnit.setMaxWidth(130); lUnit.setPrefWidth(130);
            lUnit.setAlignment(Pos.CENTER_RIGHT);

            Label lSub = new Label(FMT.format(d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO));
            lSub.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
            lSub.setMinWidth(130); lSub.setMaxWidth(130); lSub.setPrefWidth(130);
            lSub.setAlignment(Pos.CENTER_RIGHT);

            fila.getChildren().addAll(lNom, lCant, lUnit, lSub);
            filas.getChildren().add(fila);
        }

        // Fila de total
        HBox totalFila = new HBox();
        totalFila.setPadding(new Insets(12, 16, 12, 16));
        totalFila.setAlignment(Pos.CENTER_RIGHT);
        totalFila.setSpacing(16);
        totalFila.setStyle("-fx-background-color: #F5F1EB; -fx-background-radius: 0 0 8 8; " +
                "-fx-border-color: rgba(26,31,46,0.10); -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 8 8;");

        Label lblTotLit = new Label("TOTAL DE LA COMPRA");
        lblTotLit.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #78716C;");
        Region spacerTotal = new Region(); HBox.setHgrow(spacerTotal, Priority.ALWAYS);
        Label lblTotVal = new Label(totalStr);
        lblTotVal.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #15803D;");
        totalFila.getChildren().addAll(lblTotLit, spacerTotal, lblTotVal);

        VBox tablaCompleta = new VBox(0);
        tablaCompleta.getChildren().addAll(tabHead, filas, totalFila);

        body.getChildren().addAll(cards, new Separator(), secHeader, tablaCompleta);

        view.getChildren().addAll(header, scroll);

        animarEntrada(header, 0);
        animarEntrada(scroll,  60);

        contentArea.getChildren().setAll(view);
    }

    private VBox buildInfoCard(String iconLiteral, String color, String titulo, String valor) {
        VBox card = new VBox(8);
        card.getStyleClass().add("inventario-stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 8;");
        icoCircle.setMinSize(34, 34); icoCircle.setMaxSize(34, 34);
        FontIcon ico = new FontIcon(iconLiteral);
        ico.setIconSize(15); ico.setIconColor(Paint.valueOf(color));
        icoCircle.getChildren().add(ico);

        Label lblVal = new Label(valor);
        lblVal.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        lblVal.setWrapText(true);

        Label lblTit = new Label(titulo);
        lblTit.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716C;");

        card.getChildren().addAll(icoCircle, lblVal, lblTit);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // Paso 1 — Selector de proveedor
    // ─────────────────────────────────────────────────────────────

    private void abrirModalNuevaCompra() {
        List<Proveedor> proveedores;
        List<Producto>  productos;
        try { proveedores = proveedorService.findAllActivos(); }
        catch (Exception e) { proveedores = new ArrayList<>(); }
        try { productos = new ArrayList<>(productoService.findAllActivos()); }
        catch (Exception e) { productos = new ArrayList<>(); }

        final List<Producto> listaProductos = productos;

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrarModal(overlay); });

        mostrarSelectorProveedor(overlay, new ArrayList<>(proveedores), listaProductos);

        rootStack.getChildren().add(overlay);
        overlay.setOpacity(0);
        FadeTransition ftOverlay = new FadeTransition(Duration.millis(200), overlay);
        ftOverlay.setFromValue(0); ftOverlay.setToValue(1); ftOverlay.play();
    }

    private void mostrarSelectorProveedor(StackPane overlay, List<Proveedor> proveedores, List<Producto> listaProductos) {
        overlay.getChildren().clear();

        VBox modal = new VBox(16);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(720);
        modal.setPrefWidth(700);
        modal.setMaxHeight(560);
        modal.setAlignment(Pos.TOP_LEFT);

        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon icoTit = new FontIcon("fas-truck");
        icoTit.setIconSize(16); icoTit.setIconColor(Paint.valueOf("#5A6ACF"));
        Label lblTit = new Label("Seleccionar proveedor");
        lblTit.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);

        Button btnCerrar = new Button();
        FontIcon icoX = new FontIcon("fas-times"); icoX.setIconSize(14); icoX.setIconColor(Paint.valueOf("#78716C"));
        btnCerrar.setGraphic(icoX); btnCerrar.getStyleClass().add("inventario-modal-close");
        btnCerrar.setOnAction(e -> cerrarModal(overlay));
        header.getChildren().addAll(icoTit, lblTit, btnCerrar);

        // ── Búsqueda de proveedor ─────────────────────────────────
        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar proveedor por nombre o NIT...");
        txtBuscar.getStyleClass().add("venta-search-field");
        txtBuscar.setMaxWidth(Double.MAX_VALUE);

        // ── Grid de cards ─────────────────────────────────────────
        javafx.scene.layout.FlowPane grid = new javafx.scene.layout.FlowPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(4, 0, 8, 0));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefHeight(380);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Runnable poblarGrid = () -> {
            grid.getChildren().clear();
            // "Nuevo proveedor" card always first, unaffected by search filter
            VBox cardNuevo = crearCardNuevoProveedor(overlay, listaProductos);
            grid.getChildren().add(cardNuevo);

            String filtro = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase();
            List<Proveedor> filtrados = proveedores.stream()
                    .filter(p -> filtro.isBlank()
                            || p.getNombre().toLowerCase().contains(filtro)
                            || (p.getNit() != null && p.getNit().toLowerCase().contains(filtro)))
                    .collect(Collectors.toList());

            if (!filtrados.isEmpty()) {
                int[] idx = {0};
                for (Proveedor p : filtrados) {
                    VBox card = crearCardProveedor(p, overlay, listaProductos);
                    grid.getChildren().add(card);
                    animarEntrada(card, idx[0]++ * 25);
                }
            }
        };

        poblarGrid.run();
        txtBuscar.textProperty().addListener((obs, o, n) -> poblarGrid.run());

        modal.getChildren().addAll(header, new Separator(), txtBuscar, scroll);

        overlay.getChildren().add(modal);
        modal.setTranslateY(18);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), modal);
        tt.setFromY(18); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT); tt.play();
    }

    private VBox crearCardProveedor(Proveedor p, StackPane overlay, List<Producto> listaProductos) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #FDFCFA; -fx-border-color: rgba(26,31,46,0.10); " +
                      "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; " +
                      "-fx-padding: 16; -fx-cursor: hand;");
        card.setPrefWidth(200); card.setMaxWidth(200);

        // Ícono circular
        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: #5A6ACF22; -fx-background-radius: 10;");
        icoCircle.setMinSize(40, 40); icoCircle.setMaxSize(40, 40);
        FontIcon ico = new FontIcon("fas-building"); ico.setIconSize(16); ico.setIconColor(Paint.valueOf("#5A6ACF"));
        icoCircle.getChildren().add(ico);

        // Nombre
        Label lblNombre = new Label(p.getNombre());
        lblNombre.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        lblNombre.setWrapText(true);

        // NIT
        Label lblNit = new Label(p.getNit() != null && !p.getNit().isBlank() ? "NIT: " + p.getNit() : "Sin NIT");
        lblNit.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716C;");

        // Margen
        String margen = p.getPorcentajeGanancia() != null
                ? "Margen: " + p.getPorcentajeGanancia().stripTrailingZeros().toPlainString() + "%"
                : "Sin margen configurado";
        Label lblMargen = new Label(margen);
        lblMargen.setStyle("-fx-font-size: 11px; -fx-text-fill: #5A6ACF; -fx-font-weight: 600;");

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        // Botón seleccionar
        Button btnSel = new Button("Seleccionar");
        btnSel.getStyleClass().add("btn-primario");
        btnSel.setMaxWidth(Double.MAX_VALUE);
        btnSel.setStyle("-fx-padding: 7 12; -fx-font-size: 12px;");
        btnSel.setOnAction(e -> mostrarFormRegistrarCompra(overlay, p, listaProductos));

        card.getChildren().addAll(icoCircle, lblNombre, lblNit, lblMargen, spacer, btnSel);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("#FDFCFA", "#F5F1EB")
                .replace("rgba(26,31,46,0.10)", "rgba(90,106,207,0.30)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("#F5F1EB", "#FDFCFA")
                .replace("rgba(90,106,207,0.30)", "rgba(26,31,46,0.10)")));
        card.setOnMouseClicked(e -> mostrarFormRegistrarCompra(overlay, p, listaProductos));

        return card;
    }

    private VBox crearCardNuevoProveedor(StackPane overlay, List<Producto> listaProductos) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: #FDFCFA; " +
                "-fx-border-color: #5A6ACF; -fx-border-width: 2; " +
                "-fx-border-style: dashed; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; " +
                "-fx-padding: 16; -fx-cursor: hand; -fx-opacity: 0.85;");
        card.setPrefWidth(200); card.setMaxWidth(200);
        card.setAlignment(Pos.CENTER);

        // "+" circle icon
        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: #5A6ACF22; -fx-background-radius: 10;");
        icoCircle.setMinSize(40, 40); icoCircle.setMaxSize(40, 40);
        FontIcon ico = new FontIcon("fas-plus"); ico.setIconSize(18); ico.setIconColor(Paint.valueOf("#5A6ACF"));
        icoCircle.getChildren().add(ico);

        Label lblTitle = new Label("Nuevo proveedor");
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #5A6ACF; -fx-wrap-text: true;");
        lblTitle.setWrapText(true);

        Label lblSub = new Label("Registrar un proveedor nuevo");
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E; -fx-wrap-text: true;");
        lblSub.setWrapText(true);

        card.getChildren().addAll(icoCircle, lblTitle, lblSub);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                .replace("#FDFCFA", "#EEF0FB").replace("0.85;", "1.0;")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
                .replace("#EEF0FB", "#FDFCFA").replace("1.0;", "0.85;")));
        card.setOnMouseClicked(e -> mostrarFormCrearProveedor(overlay, listaProductos));

        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // Paso 2 — Formulario de compra
    // ─────────────────────────────────────────────────────────────

    private void mostrarFormRegistrarCompra(StackPane overlay, Proveedor proveedor, List<Producto> listaProductos) {
        overlay.getChildren().clear();

        VBox modal = new VBox(14);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(680);
        modal.setPrefWidth(660);
        modal.setMaxHeight(660);
        modal.setAlignment(Pos.TOP_LEFT);

        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon icoTit = new FontIcon("fas-shopping-cart");
        icoTit.setIconSize(15); icoTit.setIconColor(Paint.valueOf("#5A6ACF"));
        Label lblTit = new Label("Registrar compra");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);
        Button btnCerrar = new Button();
        FontIcon icoX = new FontIcon("fas-times"); icoX.setIconSize(13); icoX.setIconColor(Paint.valueOf("#78716C"));
        btnCerrar.setGraphic(icoX); btnCerrar.getStyleClass().add("inventario-modal-close");
        btnCerrar.setOnAction(e -> cerrarModal(overlay));
        header.getChildren().addAll(icoTit, lblTit, btnCerrar);

        // ── Info del proveedor ─────────────────────────────────────
        HBox provInfo = new HBox(14);
        provInfo.setAlignment(Pos.CENTER_LEFT);
        provInfo.setStyle("-fx-background-color: #5A6ACF14; -fx-background-radius: 10; " +
                          "-fx-border-color: #5A6ACF33; -fx-border-width: 1; -fx-border-radius: 10; -fx-padding: 12 16;");

        StackPane provIco = new StackPane();
        provIco.setStyle("-fx-background-color: #5A6ACF; -fx-background-radius: 8;");
        provIco.setMinSize(38, 38); provIco.setMaxSize(38, 38);
        FontIcon icoEd = new FontIcon("fas-building"); icoEd.setIconSize(16); icoEd.setIconColor(Paint.valueOf("#FFFFFF"));
        provIco.getChildren().add(icoEd);

        VBox provData = new VBox(3);
        HBox.setHgrow(provData, Priority.ALWAYS);
        Label lblProvNombre = new Label(proveedor.getNombre());
        lblProvNombre.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        String nitText = proveedor.getNit() != null && !proveedor.getNit().isBlank()
                ? "NIT: " + proveedor.getNit() : "Sin NIT";
        String margenText = proveedor.getPorcentajeGanancia() != null
                ? "  ·  Margen: " + proveedor.getPorcentajeGanancia().stripTrailingZeros().toPlainString() + "%" : "";
        Label lblProvSub = new Label(nitText + margenText);
        lblProvSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716C;");
        provData.getChildren().addAll(lblProvNombre, lblProvSub);

        // Botón "Cambiar proveedor"
        FontIcon icoVolver = new FontIcon("fas-exchange-alt"); icoVolver.setIconSize(11); icoVolver.setIconColor(Paint.valueOf("#5A6ACF"));
        Button btnCambiar = new Button("Cambiar", icoVolver);
        btnCambiar.setStyle("-fx-background-color: transparent; -fx-text-fill: #5A6ACF; " +
                            "-fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 4 8;");
        btnCambiar.setOnAction(e -> {
            List<Proveedor> prov2;
            try { prov2 = proveedorService.findAllActivos(); }
            catch (Exception ex) { prov2 = new ArrayList<>(); }
            mostrarSelectorProveedor(overlay, prov2, listaProductos);
        });

        provInfo.getChildren().addAll(provIco, provData, btnCambiar);

        // ── N° Factura ─────────────────────────────────────────────
        VBox facturaBox = new VBox(5);
        Label lblFactLbl = new Label("N° Factura (opcional)");
        lblFactLbl.getStyleClass().add("form-label");
        TextField txtFactura = new TextField();
        txtFactura.setPromptText("Ej: F-2024-001");
        txtFactura.setMaxWidth(260);
        facturaBox.getChildren().addAll(lblFactLbl, txtFactura);

        // ── Sección productos ──────────────────────────────────────
        // Forward-reference holders (needed by btnNuevoProd before the variables are declared)
        TextField[] txtBuscarProdRef = new TextField[1];
        @SuppressWarnings("unchecked")
        List<ItemLineaCompra>[] lineasRef = new List[]{null};
        VBox[] listaLineasRef = {null};
        Runnable[] recalcularRef = {null};

        // Banner pre-carga (se muestra si hay última compra del proveedor)
        HBox bannerPreCarga = new HBox(8);
        bannerPreCarga.setAlignment(Pos.CENTER_LEFT);
        bannerPreCarga.setStyle("-fx-background-color: #EEF0FB; -fx-background-radius: 8; " +
                                "-fx-border-color: #C7CCF0; -fx-border-width: 1; -fx-border-radius: 8; -fx-padding: 8 12;");
        bannerPreCarga.setVisible(false);
        bannerPreCarga.setManaged(false);

        HBox secProdHeader = new HBox(10);
        secProdHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblProdSec = new Label("Productos");
        lblProdSec.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblProdSec, Priority.ALWAYS);

        FontIcon icoNuevoProd = new FontIcon("fas-plus-circle");
        icoNuevoProd.setIconSize(12); icoNuevoProd.setIconColor(Paint.valueOf("#5A6ACF"));
        Button btnNuevoProd = new Button("Nuevo producto", icoNuevoProd);
        btnNuevoProd.setStyle(
                "-fx-background-color: #EEF0FB; -fx-text-fill: #5A6ACF; " +
                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-background-radius: 7; " +
                "-fx-padding: 4 10; -fx-cursor: hand; -fx-border-color: #C7CCF0; " +
                "-fx-border-width: 1; -fx-border-radius: 7;");
        btnNuevoProd.setOnMouseEntered(e -> btnNuevoProd.setStyle(btnNuevoProd.getStyle()
                .replace("#EEF0FB", "#DDE1F7")));
        btnNuevoProd.setOnMouseExited(e -> btnNuevoProd.setStyle(btnNuevoProd.getStyle()
                .replace("#DDE1F7", "#EEF0FB")));
        btnNuevoProd.setOnAction(e -> {
            String hint = txtBuscarProdRef[0] != null ? txtBuscarProdRef[0].getText().trim() : "";
            mostrarFormCrearProducto(hint, listaProductos, lineasRef[0], listaLineasRef[0], recalcularRef[0]);
        });
        secProdHeader.getChildren().addAll(lblProdSec, btnNuevoProd);

        // Búsqueda de productos
        VBox searchBox = new VBox(0);
        TextField txtBuscarProd = new TextField();
        txtBuscarProdRef[0] = txtBuscarProd;
        txtBuscarProd.setPromptText("Buscar producto por nombre o código...");
        txtBuscarProd.getStyleClass().add("venta-search-field");
        txtBuscarProd.setMaxWidth(Double.MAX_VALUE);

        // Panel de sugerencias
        VBox sugerencias = new VBox(0);
        sugerencias.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: rgba(26,31,46,0.15); " +
                             "-fx-border-width: 1; -fx-border-radius: 0 0 8 8; -fx-max-height: 180;");
        ScrollPane scrollSug = new ScrollPane(sugerencias);
        scrollSug.setFitToWidth(true);
        scrollSug.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollSug.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollSug.setMaxHeight(170);
        scrollSug.setStyle("-fx-background-color: transparent; -fx-background: transparent; " +
                           "-fx-border-color: rgba(26,31,46,0.15); -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 8 8;");
        scrollSug.setVisible(false); scrollSug.setManaged(false);

        searchBox.getChildren().addAll(txtBuscarProd, scrollSug);

        // ── Lista de líneas ────────────────────────────────────────
        List<ItemLineaCompra> lineas = new ArrayList<>();
        VBox listaLineas = new VBox(6);

        Label lblTotalVal = new Label(FMT.format(BigDecimal.ZERO));
        lblTotalVal.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #15803D;");

        Runnable recalcularTotal = () -> {
            BigDecimal total = lineas.stream().map(ItemLineaCompra::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTotalVal.setText(FMT.format(total));
        };

        // Wire forward-reference holders now that all variables are declared
        lineasRef[0] = lineas;
        listaLineasRef[0] = listaLineas;
        recalcularRef[0] = recalcularTotal;

        // ── Pre-carga de la última compra del proveedor ───────────
        try {
            compraService.findUltimaCompraByProveedorId(proveedor.getId()).ifPresent(ultima -> {
                List<DetalleCompra> detalles = ultima.getDetalles();
                if (detalles == null || detalles.isEmpty()) return;

                Map<Long, Producto> productosMap = listaProductos.stream()
                        .filter(p -> p.getId() != null)
                        .collect(Collectors.toMap(Producto::getId, p -> p, (a, b) -> a));

                int cargados = 0;
                for (DetalleCompra detalle : detalles) {
                    if (detalle.getProducto() == null) continue;
                    Producto prod = productosMap.getOrDefault(detalle.getProducto().getId(), detalle.getProducto());
                    agregarLineaConDetalle(prod, detalle.getCantidad(), detalle.getPrecioCompraUnitario(),
                            lineas, listaLineas, recalcularTotal);
                    cargados++;
                }

                if (cargados > 0) {
                    String fechaStr = ultima.getFecha() != null
                            ? ultima.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
                    FontIcon icoBanner = new FontIcon("fas-history");
                    icoBanner.setIconSize(13); icoBanner.setIconColor(Paint.valueOf("#5A6ACF"));
                    Label lblBanner = new Label("Productos de la última compra (" + fechaStr + "). Edítalos o agrega nuevos.");
                    lblBanner.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");
                    HBox.setHgrow(lblBanner, Priority.ALWAYS);
                    FontIcon icoBorrar = new FontIcon("fas-times");
                    icoBorrar.setIconSize(11); icoBorrar.setIconColor(Paint.valueOf("#78716C"));
                    Button btnLimpiar = new Button("Limpiar", icoBorrar);
                    btnLimpiar.setStyle("-fx-background-color: transparent; -fx-text-fill: #78716C; " +
                                       "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6;");
                    btnLimpiar.setOnAction(ev -> {
                        lineas.clear();
                        listaLineas.getChildren().clear();
                        recalcularTotal.run();
                        bannerPreCarga.setVisible(false);
                        bannerPreCarga.setManaged(false);
                    });
                    bannerPreCarga.getChildren().addAll(icoBanner, lblBanner, btnLimpiar);
                    bannerPreCarga.setVisible(true);
                    bannerPreCarga.setManaged(true);
                }
            });
        } catch (Exception ignored) {}

        // Listener del buscador de productos
        txtBuscarProd.textProperty().addListener((obs, o, n) -> {
            sugerencias.getChildren().clear();
            String textoOriginal = n == null ? "" : n.trim();
            String texto = textoOriginal.toLowerCase();
            if (texto.isBlank()) {
                scrollSug.setVisible(false); scrollSug.setManaged(false);
                return;
            }
            List<Producto> matches = listaProductos.stream()
                    .filter(p -> p.getNombre().toLowerCase().contains(texto)
                            || (p.getCodigoBarras() != null && p.getCodigoBarras().toLowerCase().contains(texto)))
                    .limit(8)
                    .collect(Collectors.toList());

            if (matches.isEmpty()) {
                Label lv = new Label("Sin resultados para \"" + textoOriginal + "\"");
                lv.setStyle("-fx-font-size: 12px; -fx-text-fill: #A8A29E; -fx-padding: 10 14;");
                sugerencias.getChildren().add(lv);
            } else {
                for (Producto prod : matches) {
                    HBox fila = new HBox(10);
                    fila.setAlignment(Pos.CENTER_LEFT);
                    fila.setStyle("-fx-padding: 9 14; -fx-cursor: hand;");
                    fila.setOnMouseEntered(ev -> fila.setStyle("-fx-background-color: #F5F1EB; -fx-padding: 9 14; -fx-cursor: hand;"));
                    fila.setOnMouseExited(ev -> fila.setStyle("-fx-padding: 9 14; -fx-cursor: hand;"));

                    Label lNom = new Label(prod.getNombre());
                    lNom.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1A1F2E;");
                    HBox.setHgrow(lNom, Priority.ALWAYS);

                    String costoText = prod.getPrecioCompra() != null
                            ? "Último costo: " + FMT.format(prod.getPrecioCompra()) : "";
                    Label lCosto = new Label(costoText);
                    lCosto.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716C;");

                    fila.getChildren().addAll(lNom, lCosto);
                    fila.setOnMousePressed(ev -> {
                        agregarLineaProducto(prod, lineas, listaLineas, recalcularTotal);
                        txtBuscarProd.clear();
                        scrollSug.setVisible(false); scrollSug.setManaged(false);
                    });
                    sugerencias.getChildren().add(fila);
                    if (matches.indexOf(prod) < matches.size() - 1) {
                        Separator sepLine = new Separator();
                        sepLine.setStyle("-fx-padding: 0; -fx-opacity: 0.5;");
                        sugerencias.getChildren().add(sepLine);
                    }
                }
            }

            // ── Fila "Crear producto" siempre al fondo ────────────
            Separator sepCrear = new Separator();
            sepCrear.setStyle("-fx-opacity: 0.4;");
            sugerencias.getChildren().add(sepCrear);

            HBox filaCrear = new HBox(8);
            filaCrear.setAlignment(Pos.CENTER_LEFT);
            filaCrear.setStyle("-fx-padding: 9 14; -fx-cursor: hand;");
            filaCrear.setOnMouseEntered(ev -> filaCrear.setStyle("-fx-background-color: #EEF0FB; -fx-padding: 9 14; -fx-cursor: hand;"));
            filaCrear.setOnMouseExited(ev -> filaCrear.setStyle("-fx-padding: 9 14; -fx-cursor: hand;"));
            FontIcon icoPlus = new FontIcon("fas-plus-circle");
            icoPlus.setIconSize(13); icoPlus.setIconColor(Paint.valueOf("#5A6ACF"));
            Label lblCrear = new Label("Crear producto \"" + textoOriginal + "\"");
            lblCrear.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #5A6ACF;");
            filaCrear.getChildren().addAll(icoPlus, lblCrear);
            filaCrear.setOnMousePressed(ev -> {
                scrollSug.setVisible(false); scrollSug.setManaged(false);
                txtBuscarProd.clear();
                mostrarFormCrearProducto(textoOriginal, listaProductos, lineas, listaLineas, recalcularTotal);
            });
            sugerencias.getChildren().add(filaCrear);

            scrollSug.setVisible(true); scrollSug.setManaged(true);
        });

        txtBuscarProd.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) {
                // Defer hide so mouse-click on a suggestion fires before it disappears
                javafx.application.Platform.runLater(() -> {
                    scrollSug.setVisible(false); scrollSug.setManaged(false);
                });
            }
        });

        // ── Header de la tabla de líneas ──────────────────────────
        HBox headerLineas = new HBox();
        headerLineas.setStyle("-fx-padding: 4 10; -fx-background-color: #F5F1EB; -fx-background-radius: 6 6 0 0;");
        Label hProd = new Label("Producto"); hProd.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #78716C;"); HBox.setHgrow(hProd, Priority.ALWAYS);
        Label hCant = new Label("Cant."); hCant.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #78716C; -fx-min-width: 56px; -fx-alignment: CENTER;");
        Label hPrecio = new Label("Precio unit."); hPrecio.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #78716C; -fx-min-width: 100px;");
        Label hSub = new Label("Subtotal"); hSub.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #78716C; -fx-min-width: 90px;");
        Label hAcc = new Label(""); hAcc.setStyle("-fx-min-width: 28px;");
        headerLineas.getChildren().addAll(hProd, hCant, hPrecio, hSub, hAcc);

        ScrollPane scrollLineas = new ScrollPane(listaLineas);
        scrollLineas.setFitToWidth(true);
        scrollLineas.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollLineas.setPrefHeight(160);
        scrollLineas.setMaxHeight(180);
        scrollLineas.setStyle("-fx-background-color: transparent; -fx-background: transparent; " +
                              "-fx-border-color: rgba(26,31,46,0.10); -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 6 6;");

        VBox tablaLineas = new VBox(0);
        tablaLineas.getChildren().addAll(headerLineas, scrollLineas);

        // ── Total ─────────────────────────────────────────────────
        Separator sep = new Separator();

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle("-fx-background-color: rgba(90,106,207,0.06); -fx-background-radius: 8; -fx-padding: 10 14;");
        Label lblTotalLbl = new Label("Total de la compra");
        lblTotalLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #4B5563;");
        HBox.setHgrow(lblTotalLbl, Priority.ALWAYS);
        totalRow.getChildren().addAll(lblTotalLbl, lblTotalVal);

        // ── Error + acciones ──────────────────────────────────────
        Label lblError = new Label();
        lblError.getStyleClass().add("login-error");
        lblError.setWrapText(true);
        lblError.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 7; -fx-padding: 8 12;");
        lblError.setVisible(false); lblError.setManaged(false);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> cerrarModal(overlay));

        FontIcon icoCheck = new FontIcon("fas-check"); icoCheck.setIconSize(13); icoCheck.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnConfirmar = new Button("Registrar Compra", icoCheck);
        btnConfirmar.getStyleClass().add("btn-primario");
        btnConfirmar.setStyle("-fx-padding: 8 22;");
        btnConfirmar.setOnAction(e -> {
            lblError.setVisible(false); lblError.setManaged(false);
            if (lineas.isEmpty()) { mostrarErrorModal(lblError, "Agrega al menos un producto."); return; }
            boolean vacio = lineas.stream().anyMatch(l -> l.cantidad <= 0 || l.precio == null || l.precio.compareTo(BigDecimal.ZERO) <= 0);
            if (vacio) { mostrarErrorModal(lblError, "Completa la cantidad y el precio de cada producto."); return; }
            try {
                List<CompraService.ItemCompra> items = lineas.stream()
                        .map(l -> new CompraService.ItemCompra(l.producto.getId(), l.cantidad, l.precio))
                        .collect(Collectors.toList());
                compraService.registrarCompra(
                        proveedor.getId(), usuarioActual.getId(),
                        txtFactura.getText().trim().isEmpty() ? null : txtFactura.getText().trim(),
                        items);
                cerrarModal(overlay);
                recargarDatos();
                mostrarDashboard();
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Compra registrada");
                ok.setHeaderText(null);
                ok.setContentText("¡La compra fue registrada exitosamente!");
                ok.showAndWait();
            } catch (BusinessException ex) {
                mostrarErrorModal(lblError, ex.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblError, "Error inesperado: " + ex.getMessage());
            }
        });

        actions.getChildren().addAll(btnCancelar, btnConfirmar);

        modal.getChildren().addAll(header, new Separator(), provInfo, facturaBox,
                sep, secProdHeader, bannerPreCarga, searchBox, tablaLineas, new Separator(), totalRow, lblError, actions);

        overlay.getChildren().add(modal);
        modal.setTranslateY(16);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), modal);
        tt.setFromY(16); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT); tt.play();
    }

    private void agregarLineaProducto(Producto prod, List<ItemLineaCompra> lineas,
                                       VBox listaLineas, Runnable recalcular) {
        // Si ya existe la misma línea para este producto, no duplicar
        boolean yaExiste = lineas.stream().anyMatch(l -> l.producto != null && l.producto.getId().equals(prod.getId()));
        if (yaExiste) return;

        ItemLineaCompra linea = new ItemLineaCompra();
        linea.producto = prod;
        linea.cantidad = 1;
        // Pre-llenar con el último costo si está disponible
        if (prod.getPrecioCompra() != null) {
            linea.precio = prod.getPrecioCompra();
        }
        lineas.add(linea);

        HBox fila = crearFilaLinea(linea, lineas, listaLineas, recalcular);
        listaLineas.getChildren().add(fila);
        animarEntrada(fila, 0);
        recalcular.run();
    }

    private void agregarLineaConDetalle(Producto prod, int cantidad, BigDecimal precio,
                                        List<ItemLineaCompra> lineas, VBox listaLineas, Runnable recalcular) {
        boolean yaExiste = lineas.stream().anyMatch(l -> l.producto != null && l.producto.getId().equals(prod.getId()));
        if (yaExiste) return;

        ItemLineaCompra linea = new ItemLineaCompra();
        linea.producto = prod;
        linea.cantidad = cantidad;
        linea.precio = precio;
        lineas.add(linea);

        HBox fila = crearFilaLinea(linea, lineas, listaLineas, recalcular);
        listaLineas.getChildren().add(fila);
        recalcular.run();
    }

    private HBox crearFilaLinea(ItemLineaCompra linea, List<ItemLineaCompra> lineas,
                                 VBox contenedor, Runnable recalcular) {
        HBox fila = new HBox(8);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setStyle("-fx-background-color: #FDFCFA; -fx-border-color: rgba(26,31,46,0.07); " +
                      "-fx-border-width: 0 0 1 0; -fx-padding: 8 10;");

        // Nombre del producto (read-only)
        Label lblProdNom = new Label(linea.producto != null ? linea.producto.getNombre() : "—");
        lblProdNom.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblProdNom, Priority.ALWAYS);

        // Cantidad
        TextField txtCant = new TextField(String.valueOf(linea.cantidad));
        txtCant.setPrefWidth(56); txtCant.setMinWidth(56); txtCant.setMaxWidth(56);
        txtCant.setStyle("-fx-alignment: CENTER;");
        txtCant.textProperty().addListener((obs, o, n) -> {
            try { linea.cantidad = Integer.parseInt(n.trim()); } catch (NumberFormatException ex) { linea.cantidad = 0; }
            recalcular.run();
            actualizarSubtotalFila(fila, linea);
        });

        // Precio unitario (pre-llenado si hay costo previo)
        TextField txtPrecio = new TextField(linea.precio != null ? linea.precio.toPlainString() : "");
        txtPrecio.setPromptText("0.00");
        txtPrecio.setPrefWidth(100); txtPrecio.setMinWidth(100); txtPrecio.setMaxWidth(100);
        txtPrecio.textProperty().addListener((obs, o, n) -> {
            try { linea.precio = new BigDecimal(n.trim().replace(",", ".")); }
            catch (NumberFormatException ex) { linea.precio = null; }
            recalcular.run();
            actualizarSubtotalFila(fila, linea);
        });

        // Subtotal
        Label lblSub = new Label(FMT.format(linea.calcularSubtotal()));
        lblSub.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #15803D; -fx-min-width: 90px;");
        lblSub.setId("subtotal");

        // Eliminar
        Button btnDel = new Button();
        FontIcon icoD = new FontIcon("fas-times"); icoD.setIconSize(11); icoD.setIconColor(Paint.valueOf("#DC2626"));
        btnDel.setGraphic(icoD); btnDel.getStyleClass().add("venta-btn-del");
        btnDel.setOnAction(e -> {
            lineas.remove(linea);
            contenedor.getChildren().remove(fila);
            recalcular.run();
        });

        fila.getChildren().addAll(lblProdNom, txtCant, txtPrecio, lblSub, btnDel);
        return fila;
    }

    private void actualizarSubtotalFila(HBox fila, ItemLineaCompra linea) {
        fila.getChildren().stream()
                .filter(n -> "subtotal".equals(n.getId()))
                .findFirst()
                .ifPresent(n -> ((Label) n).setText(FMT.format(linea.calcularSubtotal())));
    }

    // ─────────────────────────────────────────────────────────────
    // Modal: Crear proveedor
    // ─────────────────────────────────────────────────────────────

    private void mostrarFormCrearProveedor(StackPane overlayPadre, List<Producto> listaProductos) {
        StackPane overlayCrear = new StackPane();
        overlayCrear.getStyleClass().add("inventario-modal-overlay");
        overlayCrear.setOnMouseClicked(e -> { if (e.getTarget() == overlayCrear) cerrarModal(overlayCrear); });

        VBox modal = new VBox(14);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(480);
        modal.setPrefWidth(460);
        modal.setMaxHeight(520);
        modal.setAlignment(Pos.TOP_LEFT);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon icoTit = new FontIcon("fas-plus-circle"); icoTit.setIconSize(15); icoTit.setIconColor(Paint.valueOf("#5A6ACF"));
        Label lblTit = new Label("Crear proveedor");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);
        Button btnCerrar = new Button();
        FontIcon icoX = new FontIcon("fas-times"); icoX.setIconSize(13); icoX.setIconColor(Paint.valueOf("#78716C"));
        btnCerrar.setGraphic(icoX); btnCerrar.getStyleClass().add("inventario-modal-close");
        btnCerrar.setOnAction(e -> cerrarModal(overlayCrear));
        header.getChildren().addAll(icoTit, lblTit, btnCerrar);

        // Campos
        TextField txtNombre = crearCampoForm("Nombre *", "Nombre del proveedor", false);
        TextField txtNit     = crearCampoForm("NIT (opcional)", "Ej: 900123456-7", false);
        TextField txtCelular = crearCampoForm("Celular (opcional)", "Ej: 3001234567", false);
        TextField txtDir     = crearCampoForm("Dirección (opcional)", "Ej: Calle 10 #5-20", false);
        TextField txtMargen  = crearCampoForm("Margen de ganancia % *", "Ej: 25", false);

        // Error
        Label lblError = new Label();
        lblError.getStyleClass().add("login-error");
        lblError.setWrapText(true);
        lblError.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 7; -fx-padding: 8 12;");
        lblError.setVisible(false); lblError.setManaged(false);

        // Acciones
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> cerrarModal(overlayCrear));

        FontIcon icoCheck = new FontIcon("fas-check"); icoCheck.setIconSize(12); icoCheck.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnGuardar = new Button("Guardar proveedor", icoCheck);
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setStyle("-fx-padding: 8 20;");
        btnGuardar.setOnAction(e -> {
            lblError.setVisible(false); lblError.setManaged(false);
            String nombre = txtNombre.getText().trim();
            if (nombre.isBlank()) { mostrarErrorModal(lblError, "El nombre del proveedor es obligatorio."); return; }
            String margenStr = txtMargen.getText().trim();
            if (margenStr.isBlank()) { mostrarErrorModal(lblError, "El margen de ganancia es obligatorio."); return; }
            BigDecimal margen;
            try { margen = new BigDecimal(margenStr.replace(",", ".")); }
            catch (NumberFormatException ex) { mostrarErrorModal(lblError, "El margen debe ser un número válido."); return; }
            try {
                Proveedor nuevo = Proveedor.builder()
                        .nombre(nombre)
                        .nit(txtNit.getText().trim().isEmpty() ? null : txtNit.getText().trim())
                        .celular(txtCelular.getText().trim().isEmpty() ? null : txtCelular.getText().trim())
                        .direccion(txtDir.getText().trim().isEmpty() ? null : txtDir.getText().trim())
                        .porcentajeGanancia(margen)
                        .activo(true)
                        .build();
                proveedorService.crear(nuevo);
                cerrarModal(overlayCrear);
                // Refrescar selector con lista actualizada
                List<Proveedor> nuevaLista;
                try { nuevaLista = proveedorService.findAllActivos(); }
                catch (Exception ex2) { nuevaLista = new ArrayList<>(); }
                mostrarSelectorProveedor(overlayPadre, nuevaLista, listaProductos);
            } catch (BusinessException ex) {
                mostrarErrorModal(lblError, ex.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblError, "Error: " + ex.getMessage());
            }
        });

        actions.getChildren().addAll(btnCancelar, btnGuardar);

        // Wrap fields in ScrollPane to prevent overflow
        VBox camposBox = new VBox(14,
                campoConLabel("Nombre *", txtNombre),
                campoConLabel("NIT (opcional)", txtNit),
                campoConLabel("Celular (opcional)", txtCelular),
                campoConLabel("Dirección (opcional)", txtDir),
                campoConLabel("Margen de ganancia % *", txtMargen),
                lblError);
        ScrollPane scrollCampos = new ScrollPane(camposBox);
        scrollCampos.setFitToWidth(true);
        scrollCampos.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollCampos.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollCampos, Priority.ALWAYS);

        modal.getChildren().addAll(header, new Separator(), scrollCampos, actions);

        overlayCrear.getChildren().add(modal);
        rootStack.getChildren().add(overlayCrear);
        overlayCrear.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), overlayCrear);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        modal.setTranslateY(14);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), modal);
        tt.setFromY(14); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT); tt.play();
    }

    // ─────────────────────────────────────────────────────────────
    // Modal: Crear producto (reusa el mismo formulario de Inventario)
    // ─────────────────────────────────────────────────────────────

    private void mostrarFormCrearProducto(String nombreInicial,
                                          List<Producto> listaProductos,
                                          List<ItemLineaCompra> lineas,
                                          VBox listaLineas,
                                          Runnable recalcularTotal) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrarModal(overlay); });

        VBox modal = new VBox(14);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(580);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setAlignment(Pos.TOP_LEFT);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon icoTit = new FontIcon("fas-plus-circle"); icoTit.setIconSize(18); icoTit.setIconColor(Paint.valueOf("#5A6ACF"));
        Label lblTit = new Label("Nuevo Producto");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);
        Button btnCerrar = new Button();
        FontIcon icoX = new FontIcon("fas-times"); icoX.setIconSize(14); icoX.setIconColor(Paint.valueOf("#78716C"));
        btnCerrar.setGraphic(icoX); btnCerrar.getStyleClass().add("inventario-modal-close");
        btnCerrar.setOnAction(e -> cerrarModal(overlay));
        header.getChildren().addAll(icoTit, lblTit, btnCerrar);

        // ── Nombre + Código ─────────────────────────────────────
        VBox fNombre = crearCampoVBox("Nombre del producto *");
        TextField txtNombre = (TextField) fNombre.getChildren().get(1);
        txtNombre.setPromptText("Ej: Coca-Cola 350ml");
        txtNombre.setText(nombreInicial);

        VBox fCodigo = crearCampoVBox("Código de barras *");
        TextField txtCodigo = (TextField) fCodigo.getChildren().get(1);
        txtCodigo.setPromptText("Ej: 7701234567890");

        HBox row1 = new HBox(14, fNombre, fCodigo);
        HBox.setHgrow(fNombre, Priority.ALWAYS); HBox.setHgrow(fCodigo, Priority.ALWAYS);

        // ── Categoría + Subcategoría ─────────────────────────────
        VBox fCat = new VBox(6);
        Label lblCat = new Label("Categoría *"); lblCat.getStyleClass().add("form-label");
        ComboBox<Categoria> cmbCat = new ComboBox<>();
        cmbCat.setPromptText("Seleccionar categoría"); cmbCat.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(cmbCat, Priority.ALWAYS);
        try { cmbCat.setItems(FXCollections.observableArrayList(categoriaService.findAllActivas())); }
        catch (Exception ignored) {}
        cmbCat.setConverter(new StringConverter<>() {
            @Override public String toString(Categoria c) { return c == null ? "" : c.getNombre(); }
            @Override public Categoria fromString(String s) { return null; }
        });
        Button btnNuevaCat = new Button();
        FontIcon icoPlusCat = new FontIcon("fas-plus"); icoPlusCat.setIconSize(11); icoPlusCat.setIconColor(Paint.valueOf("#5A6ACF"));
        btnNuevaCat.setGraphic(icoPlusCat); btnNuevaCat.getStyleClass().add("inventario-btn-masiva");
        HBox catRow = new HBox(6, cmbCat, btnNuevaCat); catRow.setAlignment(Pos.CENTER_LEFT);
        fCat.getChildren().addAll(lblCat, catRow);

        VBox fSubcat = new VBox(6);
        Label lblSubcat = new Label("Subcategoría *"); lblSubcat.getStyleClass().add("form-label");
        ComboBox<Subcategoria> cmbSubcat = new ComboBox<>();
        cmbSubcat.setPromptText("Seleccionar subcategoría"); cmbSubcat.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(cmbSubcat, Priority.ALWAYS);
        cmbSubcat.setDisable(true);
        cmbSubcat.setConverter(new StringConverter<>() {
            @Override public String toString(Subcategoria s) { return s == null ? "" : s.getNombre(); }
            @Override public Subcategoria fromString(String str) { return null; }
        });
        Button btnNuevaSubcat = new Button();
        FontIcon icoPlusSub = new FontIcon("fas-plus"); icoPlusSub.setIconSize(11); icoPlusSub.setIconColor(Paint.valueOf("#5A6ACF"));
        btnNuevaSubcat.setGraphic(icoPlusSub); btnNuevaSubcat.getStyleClass().add("inventario-btn-masiva");
        btnNuevaSubcat.setDisable(true);
        HBox subcatRow = new HBox(6, cmbSubcat, btnNuevaSubcat); subcatRow.setAlignment(Pos.CENTER_LEFT);
        fSubcat.getChildren().addAll(lblSubcat, subcatRow);

        cmbCat.valueProperty().addListener((obs, old, cat) -> {
            cmbSubcat.getItems().clear(); cmbSubcat.setValue(null);
            if (cat != null) {
                try {
                    cmbSubcat.setItems(FXCollections.observableArrayList(
                            subcategoriaService.findByCategoriaId(cat.getId())));
                    cmbSubcat.setDisable(false); btnNuevaSubcat.setDisable(false);
                } catch (Exception ignored) { cmbSubcat.setDisable(true); btnNuevaSubcat.setDisable(true); }
            } else { cmbSubcat.setDisable(true); btnNuevaSubcat.setDisable(true); }
        });

        btnNuevaCat.setOnAction(e -> abrirMiniModalCategoria(cmbCat, cmbSubcat, btnNuevaSubcat));
        btnNuevaSubcat.setOnAction(e -> abrirMiniModalSubcategoria(cmbCat.getValue(), cmbSubcat));

        HBox row2 = new HBox(14, fCat, fSubcat);
        HBox.setHgrow(fCat, Priority.ALWAYS); HBox.setHgrow(fSubcat, Priority.ALWAYS);

        // ── Proveedor + Precio ───────────────────────────────────
        VBox fProv = new VBox(6);
        Label lblProv = new Label("Proveedor *"); lblProv.getStyleClass().add("form-label");
        ComboBox<Proveedor> cmbProv = new ComboBox<>();
        cmbProv.setPromptText("Seleccionar proveedor"); cmbProv.setMaxWidth(Double.MAX_VALUE);
        try { cmbProv.setItems(FXCollections.observableArrayList(proveedorService.findAllActivos())); }
        catch (Exception ignored) {}
        cmbProv.setConverter(new StringConverter<>() {
            @Override public String toString(Proveedor p) {
                return p == null ? "" : p.getNombre() + " (" + p.getPorcentajeGanancia() + "%)";
            }
            @Override public Proveedor fromString(String s) { return null; }
        });
        Label lblMargenBase = new Label();
        lblMargenBase.setStyle("-fx-font-size: 11px; -fx-text-fill: #5A6ACF; -fx-font-weight: 600;");
        lblMargenBase.setVisible(false); lblMargenBase.setManaged(false);
        fProv.getChildren().addAll(lblProv, cmbProv, lblMargenBase);

        VBox fPrecio = crearCampoVBox("Precio de compra *");
        TextField txtPrecio = (TextField) fPrecio.getChildren().get(1);
        txtPrecio.setPromptText("Ej: 1500");

        HBox row3 = new HBox(14, fProv, fPrecio);
        HBox.setHgrow(fProv, Priority.ALWAYS); HBox.setHgrow(fPrecio, Priority.ALWAYS);

        // ── Stock ────────────────────────────────────────────────
        VBox fStock = crearCampoVBox("Stock inicial *");
        TextField txtStock = (TextField) fStock.getChildren().get(1);
        txtStock.setPromptText("Ej: 0");
        txtStock.setText("0");

        // Listener proveedor: muestra margen del proveedor
        cmbProv.valueProperty().addListener((obs, old, prov) -> {
            if (prov != null) {
                String margenStr = prov.getPorcentajeGanancia().stripTrailingZeros().toPlainString();
                lblMargenBase.setText("Margen del proveedor: " + margenStr + "%");
                lblMargenBase.setVisible(true); lblMargenBase.setManaged(true);
            } else {
                lblMargenBase.setVisible(false); lblMargenBase.setManaged(false);
            }
        });

        HBox row4 = new HBox(14, fStock);
        HBox.setHgrow(fStock, Priority.ALWAYS);

        // ── Imagen del producto ──────────────────────────────────
        File[] imgFileRef = {null};
        StackPane imgBox = new StackPane();
        imgBox.setStyle("-fx-background-color: #F5F1EB; -fx-background-radius: 8; " +
                        "-fx-border-color: rgba(26,31,46,0.12); -fx-border-radius: 8; -fx-border-width: 1;");
        imgBox.setMinSize(72, 72); imgBox.setMaxSize(72, 72);
        FontIcon icoImgPh = new FontIcon("fas-image"); icoImgPh.setIconSize(22); icoImgPh.setIconColor(Paint.valueOf("#C8C2BB"));
        imgBox.getChildren().add(icoImgPh);

        FontIcon icoCamera = new FontIcon("fas-camera"); icoCamera.setIconSize(12); icoCamera.setIconColor(Paint.valueOf("#5A6ACF"));
        Button btnSelImg = new Button("Seleccionar imagen", icoCamera);
        btnSelImg.getStyleClass().add("inventario-btn-masiva");
        btnSelImg.setOnAction(ev -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleccionar imagen del producto");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"));
            File archivo = chooser.showOpenDialog(modal.getScene().getWindow());
            if (archivo == null) return;
            imgFileRef[0] = archivo;
            imgBox.getChildren().setAll(new ImageView(new Image(archivo.toURI().toString(), 72, 72, true, true)));
        });

        Label lblSecImg = new Label("Imagen (opcional)");
        lblSecImg.getStyleClass().add("form-label");
        HBox rowImagen = new HBox(14);
        rowImagen.setAlignment(Pos.CENTER_LEFT);
        VBox imgControls = new VBox(6, lblSecImg, btnSelImg);
        HBox.setHgrow(imgControls, Priority.ALWAYS);
        rowImagen.getChildren().addAll(imgBox, imgControls);

        // ── Error + acciones ─────────────────────────────────────
        Label lblError = new Label();
        lblError.getStyleClass().add("login-error");
        lblError.setWrapText(true);
        lblError.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 7; -fx-padding: 8 12;");
        lblError.setVisible(false); lblError.setManaged(false);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelar2 = new Button("Cancelar");
        btnCancelar2.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar2.setOnAction(e -> cerrarModal(overlay));

        FontIcon icoCheck = new FontIcon("fas-save"); icoCheck.setIconSize(13); icoCheck.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnGuardar = new Button("Crear Producto", icoCheck);
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setStyle("-fx-padding: 8 20;");
        btnGuardar.setOnAction(e -> {
            lblError.setVisible(false); lblError.setManaged(false);
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
                mostrarErrorModal(lblError, "El precio de compra debe ser un número mayor a 0."); return;
            }
            int stock;
            try {
                stock = Integer.parseInt(txtStock.getText().trim());
                if (stock < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                mostrarErrorModal(lblError, "El stock debe ser un número entero no negativo."); return;
            }
            try {
                Producto nuevo = Producto.builder()
                        .nombre(nombre)
                        .codigoBarras(codigo)
                        .subcategoria(cmbSubcat.getValue())
                        .proveedorPrincipal(cmbProv.getValue())
                        .precioCompra(precioCompra)
                        .stock(stock)
                        .activo(true)
                        .build();
                nuevo.calcularPrecioVenta();
                Producto creado = productoService.crear(nuevo);
                // Guardar imagen si el usuario eligió una
                if (imgFileRef[0] != null) {
                    try {
                        int dot = imgFileRef[0].getName().lastIndexOf('.');
                        String ext = dot >= 0 ? imgFileRef[0].getName().substring(dot + 1).toLowerCase() : "png";
                        Path carpeta = Paths.get(System.getProperty("user.home"), ".nappos", "assets", "products");
                        Files.createDirectories(carpeta);
                        Path destino = carpeta.resolve("producto_" + creado.getId() + "." + ext);
                        Files.copy(imgFileRef[0].toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
                        productoService.actualizarImagen(creado.getId(), destino.toAbsolutePath().toString());
                    } catch (Exception ignored) { /* imagen opcional */ }
                }
                listaProductos.add(creado);
                cerrarModal(overlay);
                agregarLineaProducto(creado, lineas, listaLineas, recalcularTotal);
            } catch (BusinessException ex) {
                mostrarErrorModal(lblError, ex.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblError, "Error inesperado: " + ex.getMessage());
            }
        });

        actions.getChildren().addAll(btnCancelar2, btnGuardar);

        modal.getChildren().addAll(header, new Separator(),
                row1, row2, row3, row4, rowImagen, lblError, new Separator(), actions);

        overlay.getChildren().add(modal);
        rootStack.getChildren().add(overlay);
        overlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        modal.setTranslateY(18);
        TranslateTransition tt = new TranslateTransition(Duration.millis(240), modal);
        tt.setFromY(18); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT); tt.play();
    }

    /** Helper genérico: VBox con Label + TextField (sin ID, habilitado) */
    private VBox crearCampoVBox(String labelText) {
        VBox box = new VBox(6);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        TextField tf = new TextField();
        tf.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(lbl, tf);
        return box;
    }

    /** Crea un TextField sin label (el label se agrega por separado con campoConLabel) */
    private TextField crearCampoForm(String id, String prompt, boolean disabled) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setId(id);
        tf.setDisable(disabled);
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    /** Envuelve un TextField con su label en un VBox */
    private VBox campoConLabel(String labelText, TextField field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        VBox box = new VBox(5, lbl, field);
        return box;
    }

    // Modelo interno de línea
    private static class ItemLineaCompra {
        Producto   producto;
        int        cantidad;
        BigDecimal precio;

        BigDecimal calcularSubtotal() {
            if (precio == null || cantidad <= 0) return BigDecimal.ZERO;
            return precio.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────
    // Mini-modales: crear categoría / subcategoría
    // ─────────────────────────────────────────────────────────────

    private void abrirMiniModalCategoria(ComboBox<Categoria> cmbCat,
                                          ComboBox<Subcategoria> cmbSubcat,
                                          Button btnNuevaSubcat) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrarModal(overlay); });

        VBox modal = new VBox(16);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(380); modal.setPadding(new Insets(22));

        HBox hdr = new HBox(10); hdr.setAlignment(Pos.CENTER_LEFT);
        FontIcon icoT = new FontIcon("fas-tag"); icoT.setIconSize(15); icoT.setIconColor(Paint.valueOf("#5A6ACF"));
        Label lblTit = new Label("Nueva Categoría");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);
        Button btnX = new Button();
        FontIcon icoX = new FontIcon("fas-times"); icoX.setIconSize(13); icoX.setIconColor(Paint.valueOf("#78716C"));
        btnX.setGraphic(icoX); btnX.getStyleClass().add("inventario-modal-close");
        btnX.setOnAction(e -> cerrarModal(overlay));
        hdr.getChildren().addAll(icoT, lblTit, btnX);

        VBox fieldNom = crearCampoVBox("Nombre de la categoría *");
        TextField txtNom = (TextField) fieldNom.getChildren().get(1);
        txtNom.setPromptText("Ej: Ropa, Electrónica...");

        Label lblErr = new Label();
        lblErr.getStyleClass().add("inventario-error-label");
        lblErr.setVisible(false); lblErr.setManaged(false);

        HBox actions = new HBox(10); actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> cerrarModal(overlay));
        FontIcon icoSave = new FontIcon("fas-save"); icoSave.setIconSize(13); icoSave.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnGuardar = new Button("Crear", icoSave);
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setOnAction(e -> {
            String nombre = txtNom.getText().trim();
            if (nombre.isEmpty()) { mostrarErrorModal(lblErr, "El nombre es obligatorio."); return; }
            try {
                Categoria nueva = Categoria.builder().nombre(nombre).activo(true).build();
                Categoria creada = categoriaService.crear(nueva);
                cmbCat.getItems().add(creada);
                cmbCat.setValue(creada);
                cmbSubcat.getItems().clear(); cmbSubcat.setValue(null);
                cmbSubcat.setDisable(false); btnNuevaSubcat.setDisable(false);
                cerrarModal(overlay);
            } catch (com.nap.pos.domain.exception.BusinessException be) {
                mostrarErrorModal(lblErr, be.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblErr, "Error al crear la categoría.");
            }
        });
        actions.getChildren().addAll(btnCancelar, btnGuardar);

        modal.getChildren().addAll(hdr, new Separator(), fieldNom, lblErr, actions);
        overlay.getChildren().add(modal);
        rootStack.getChildren().add(overlay);
        overlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), overlay); ft.setFromValue(0); ft.setToValue(1); ft.play();
        modal.setTranslateY(12);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), modal);
        tt.setFromY(12); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT); tt.play();
    }

    private void abrirMiniModalSubcategoria(Categoria categoria, ComboBox<Subcategoria> cmbSubcat) {
        if (categoria == null) return;

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrarModal(overlay); });

        VBox modal = new VBox(16);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(380); modal.setPadding(new Insets(22));

        HBox hdr = new HBox(10); hdr.setAlignment(Pos.CENTER_LEFT);
        FontIcon icoT = new FontIcon("fas-tag"); icoT.setIconSize(15); icoT.setIconColor(Paint.valueOf("#D97706"));
        Label lblTit = new Label("Nueva Subcategoría");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);
        Button btnX = new Button();
        FontIcon icoX = new FontIcon("fas-times"); icoX.setIconSize(13); icoX.setIconColor(Paint.valueOf("#78716C"));
        btnX.setGraphic(icoX); btnX.getStyleClass().add("inventario-modal-close");
        btnX.setOnAction(e -> cerrarModal(overlay));
        hdr.getChildren().addAll(icoT, lblTit, btnX);

        Label lblCatInfo = new Label("Categoría: " + categoria.getNombre());
        lblCatInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C; " +
                "-fx-background-color: #F5F1EB; -fx-background-radius: 6; -fx-padding: 6 10;");

        VBox fieldNom = crearCampoVBox("Nombre de la subcategoría *");
        TextField txtNom = (TextField) fieldNom.getChildren().get(1);
        txtNom.setPromptText("Ej: Camisas, Zapatos...");

        Label lblErr = new Label();
        lblErr.getStyleClass().add("inventario-error-label");
        lblErr.setVisible(false); lblErr.setManaged(false);

        HBox actions = new HBox(10); actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> cerrarModal(overlay));
        FontIcon icoSave = new FontIcon("fas-save"); icoSave.setIconSize(13); icoSave.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnGuardar = new Button("Crear", icoSave);
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setOnAction(e -> {
            String nombre = txtNom.getText().trim();
            if (nombre.isEmpty()) { mostrarErrorModal(lblErr, "El nombre es obligatorio."); return; }
            try {
                Subcategoria nueva = Subcategoria.builder().nombre(nombre).categoria(categoria).activo(true).build();
                Subcategoria creada = subcategoriaService.crear(nueva);
                cmbSubcat.getItems().add(creada);
                cmbSubcat.setValue(creada);
                cerrarModal(overlay);
            } catch (com.nap.pos.domain.exception.BusinessException be) {
                mostrarErrorModal(lblErr, be.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblErr, "Error al crear la subcategoría.");
            }
        });
        actions.getChildren().addAll(btnCancelar, btnGuardar);

        modal.getChildren().addAll(hdr, new Separator(), lblCatInfo, fieldNom, lblErr, actions);
        overlay.getChildren().add(modal);
        rootStack.getChildren().add(overlay);
        overlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), overlay); ft.setFromValue(0); ft.setToValue(1); ft.play();
        modal.setTranslateY(12);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), modal);
        tt.setFromY(12); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT); tt.play();
    }

    private void cerrarModal(StackPane overlay) {
        FadeTransition ft = new FadeTransition(Duration.millis(160), overlay);
        ft.setToValue(0);
        ft.setOnFinished(e -> rootStack.getChildren().remove(overlay));
        ft.play();
    }

    private void mostrarErrorModal(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true); lbl.setManaged(true);
        animarEntrada(lbl, 0);
    }

    private void animarEntrada(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(12);
        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setFromY(12); slide.setToY(0); slide.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    private void animarClickTab(Button tab) {
        ScaleTransition press = new ScaleTransition(Duration.millis(80), tab);
        press.setToX(0.95); press.setToY(0.95); press.setInterpolator(Interpolator.EASE_IN);
        ScaleTransition release = new ScaleTransition(Duration.millis(120), tab);
        release.setToX(1.0); release.setToY(1.0); release.setInterpolator(Interpolator.EASE_OUT);
        press.setOnFinished(e -> release.play());
        press.play();
    }
}
