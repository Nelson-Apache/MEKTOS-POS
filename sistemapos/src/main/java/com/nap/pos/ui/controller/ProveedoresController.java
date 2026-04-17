package com.nap.pos.ui.controller;

import com.nap.pos.application.service.ProveedorService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Proveedor;
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
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProveedoresController {

    private final ProveedorService proveedorService;

    private static final NumberFormat FMT_PCT =
            NumberFormat.getPercentInstance(Locale.of("es", "CO"));

    // ── Estado ────────────────────────────────────────────────────
    private Usuario         usuarioActual;
    private List<Proveedor> todosProveedores = new ArrayList<>();

    // ── Refs UI ───────────────────────────────────────────────────
    private StackPane rootStack;
    private VBox      contentArea;
    private Button    tabDashboard;
    private Button    tabProveedores;

    // ─────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────

    public Node buildView(Usuario usuario) {
        this.usuarioActual = usuario;
        FMT_PCT.setMaximumFractionDigits(2);
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
        try { todosProveedores = proveedorService.findAll(); }
        catch (Exception e) { todosProveedores = new ArrayList<>(); }
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

        FontIcon icoList = new FontIcon("fas-truck");
        icoList.setIconSize(14);
        icoList.setIconColor(Paint.valueOf("#78716C"));
        tabProveedores = new Button("Proveedores", icoList);
        tabProveedores.getStyleClass().add("inventario-tab");
        tabProveedores.setOnAction(e -> { animarClickTab(tabProveedores); mostrarTablaProveedores(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        FontIcon icoNuevo = new FontIcon("fas-plus");
        icoNuevo.setIconSize(13);
        icoNuevo.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnNuevo = new Button("Nuevo proveedor", icoNuevo);
        btnNuevo.getStyleClass().add("btn-primario");
        btnNuevo.setStyle("-fx-padding: 8 18 8 18; -fx-font-size: 13px;");
        btnNuevo.setOnAction(e -> abrirModalProveedor(null));

        bar.getChildren().addAll(tabDashboard, tabProveedores, spacer, btnNuevo);
        return bar;
    }

    private void activarTab(Button activo) {
        tabDashboard.getStyleClass().remove("inventario-tab-active");
        tabProveedores.getStyleClass().remove("inventario-tab-active");
        activo.getStyleClass().add("inventario-tab-active");

        String inactivo = "#78716C";
        String actColor = "#5A6ACF";
        ((FontIcon) tabDashboard.getGraphic()).setIconColor(Paint.valueOf(activo == tabDashboard   ? actColor : inactivo));
        ((FontIcon) tabProveedores.getGraphic()).setIconColor(Paint.valueOf(activo == tabProveedores ? actColor : inactivo));
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

        // ── KPIs ────────────────────────────────────────────────
        long totalProveedores  = todosProveedores.size();
        long activos           = todosProveedores.stream().filter(Proveedor::isActivo).count();
        long inactivos         = totalProveedores - activos;
        long conPct            = todosProveedores.stream()
                .filter(p -> p.getPorcentajeGanancia() != null
                        && p.getPorcentajeGanancia().compareTo(BigDecimal.ZERO) > 0).count();

        BigDecimal promedioPct = todosProveedores.stream()
                .filter(p -> p.getPorcentajeGanancia() != null)
                .map(Proveedor::getPorcentajeGanancia)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (conPct > 0) promedioPct = promedioPct.divide(BigDecimal.valueOf(conPct), 2, RoundingMode.HALF_UP);

        String promedioPctStr = conPct > 0
                ? promedioPct.toPlainString() + "%"
                : "—";

        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
            crearKpiCard("fas-truck",         "#5A6ACF", "Total proveedores",   String.valueOf(totalProveedores)),
            crearKpiCard("fas-check-circle",  "#15803D", "Activos",             String.valueOf(activos)),
            crearKpiCard("fas-ban",           "#DC2626", "Inactivos",           String.valueOf(inactivos)),
            crearKpiCard("fas-percentage",    "#D97706", "% Ganancia promedio", promedioPctStr)
        );

        // ── Gráficas ─────────────────────────────────────────────
        HBox chartsRow = new HBox(16);
        VBox chartPct    = crearGananciaPorProveedorCard();
        VBox cardResumen = crearResumenMargenesCard();
        HBox.setHgrow(chartPct,    Priority.ALWAYS);
        HBox.setHgrow(cardResumen, Priority.ALWAYS);
        chartsRow.getChildren().addAll(chartPct, cardResumen);

        // ── Lista recientes ──────────────────────────────────────
        Label lblRecientes = new Label("Proveedores recientes");
        lblRecientes.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        VBox listaRecientes = new VBox(8);
        List<Proveedor> recientes = todosProveedores.stream()
                .sorted(Comparator.comparing(Proveedor::getId, Comparator.reverseOrder()))
                .limit(8)
                .collect(Collectors.toList());

        if (recientes.isEmpty()) {
            Label lv = new Label("Sin proveedores registrados todavía.");
            lv.setStyle("-fx-font-size: 13px; -fx-text-fill: #A8A29E;");
            lv.setPadding(new Insets(12, 0, 0, 0));
            listaRecientes.getChildren().add(lv);
        } else {
            for (int i = 0; i < recientes.size(); i++) {
                HBox row = crearFilaProveedor(recientes.get(i));
                listaRecientes.getChildren().add(row);
                animarEntrada(row, i * 30);
            }
        }

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

    private VBox crearGananciaPorProveedorCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lTit = new Label("% GANANCIA POR PROVEEDOR");
        lTit.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);
        FontIcon ico = new FontIcon("fas-chart-bar");
        ico.setIconSize(14); ico.setIconColor(Paint.valueOf("#94A3B8"));
        header.getChildren().addAll(lTit, ico);

        List<Proveedor> conPct = todosProveedores.stream()
                .filter(p -> p.getPorcentajeGanancia() != null
                        && p.getPorcentajeGanancia().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Proveedor::getPorcentajeGanancia, Comparator.reverseOrder()))
                .limit(8)
                .collect(Collectors.toList());

        if (conPct.isEmpty()) {
            card.getChildren().addAll(header, buildChartEmptyState("fas-percentage",
                    "Sin márgenes definidos",
                    "Define el porcentaje de ganancia de cada proveedor para ver esta gráfica"));
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setLabel("% ganancia");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.getStyleClass().add("inventario-bar-chart");
        chart.setPrefHeight(220);
        VBox.setVgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        conPct.forEach(p -> series.getData().add(
                new XYChart.Data<>(p.getNombre(), p.getPorcentajeGanancia())));
        chart.getData().add(series);

        card.getChildren().addAll(header, chart);
        return card;
    }

    private VBox crearResumenMargenesCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lTit = new Label("RESUMEN DE MÁRGENES");
        lTit.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);
        FontIcon ico = new FontIcon("fas-percentage");
        ico.setIconSize(14); ico.setIconColor(Paint.valueOf("#94A3B8"));
        header.getChildren().addAll(lTit, ico);

        List<Proveedor> conPct = todosProveedores.stream()
                .filter(p -> p.getPorcentajeGanancia() != null
                        && p.getPorcentajeGanancia().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        VBox filas = new VBox(12);

        if (conPct.isEmpty()) {
            Label lv = new Label("Sin márgenes configurados.");
            lv.setStyle("-fx-font-size: 12px; -fx-text-fill: #A8A29E;");
            card.getChildren().addAll(header, lv);
            return card;
        }

        // Mayor margen
        Proveedor mayor = conPct.stream()
                .max(Comparator.comparing(Proveedor::getPorcentajeGanancia)).orElse(null);
        // Menor margen
        Proveedor menor = conPct.stream()
                .min(Comparator.comparing(Proveedor::getPorcentajeGanancia)).orElse(null);

        long sinPct = todosProveedores.stream()
                .filter(p -> p.getPorcentajeGanancia() == null
                        || p.getPorcentajeGanancia().compareTo(BigDecimal.ZERO) == 0).count();

        if (mayor != null)
            filas.getChildren().add(crearFilaResumen("fas-arrow-up",   "#15803D",
                    "Mayor margen — " + mayor.getNombre(),
                    mayor.getPorcentajeGanancia().toPlainString() + "%"));
        if (menor != null)
            filas.getChildren().add(crearFilaResumen("fas-arrow-down", "#DC2626",
                    "Menor margen — " + menor.getNombre(),
                    menor.getPorcentajeGanancia().toPlainString() + "%"));

        filas.getChildren().addAll(
            crearFilaResumen("fas-check-circle", "#5A6ACF", "Con margen definido",  conPct.size() + " proveedores"),
            crearFilaResumen("fas-exclamation-triangle", "#D97706", "Sin margen definido", sinPct + " proveedores")
        );

        // Top 3 por margen
        Label lblTop = new Label("TOP % GANANCIA");
        lblTop.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E; -fx-padding: 6 0 0 0;");
        filas.getChildren().add(lblTop);

        conPct.stream()
              .sorted(Comparator.comparing(Proveedor::getPorcentajeGanancia, Comparator.reverseOrder()))
              .limit(3)
              .forEach(p -> {
                  BigDecimal pct = p.getPorcentajeGanancia();
                  String color = pct.compareTo(BigDecimal.valueOf(30)) >= 0 ? "#15803D"
                               : pct.compareTo(BigDecimal.valueOf(15)) >= 0 ? "#D97706"
                               : "#DC2626";
                  filas.getChildren().add(crearFilaResumen("fas-tag", color,
                          p.getNombre(), pct.toPlainString() + "%"));
              });

        card.getChildren().addAll(header, filas);
        return card;
    }

    private HBox crearFilaResumen(String iconLiteral, String color, String label, String valor) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        FontIcon ico = new FontIcon(iconLiteral);
        ico.setIconSize(13); ico.setIconColor(Paint.valueOf(color));

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Label val = new Label(valor);
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        row.getChildren().addAll(ico, lbl, val);
        return row;
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

    private HBox crearFilaProveedor(Proveedor p) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 10px; " +
                     "-fx-border-color: rgba(26,31,46,0.08); -fx-border-width: 1; " +
                     "-fx-border-radius: 10px; -fx-padding: 12 16 12 16;");
        row.setOnMouseEntered(e -> row.setStyle(row.getStyle().replace("#FDFCFA", "#F5F1EB")));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("#F5F1EB", "#FDFCFA")));

        // Avatar con inicial
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: #D97706" + "22; -fx-background-radius: 20px;");
        avatar.setMinSize(36, 36); avatar.setMaxSize(36, 36);
        String inicial = p.getNombre() != null && !p.getNombre().isBlank()
                ? p.getNombre().substring(0, 1).toUpperCase() : "?";
        Label lblIni = new Label(inicial);
        lblIni.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #D97706;");
        avatar.getChildren().add(lblIni);

        // Info
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lNombre = new Label(p.getNombre() != null ? p.getNombre() : "—");
        lNombre.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1A1F2E;");
        String sub = (p.getNit() != null ? "NIT " + p.getNit() : "Sin NIT")
                   + (p.getCelular() != null ? "  ·  " + p.getCelular() : "");
        Label lSub = new Label(sub);
        lSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
        info.getChildren().addAll(lNombre, lSub);

        row.getChildren().addAll(avatar, info);

        // Badge % ganancia
        if (p.getPorcentajeGanancia() != null && p.getPorcentajeGanancia().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pct = p.getPorcentajeGanancia();
            String bg = pct.compareTo(BigDecimal.valueOf(30)) >= 0 ? "#DCFCE7"
                      : pct.compareTo(BigDecimal.valueOf(15)) >= 0 ? "#FEF3C7"
                      : "#FEE2E2";
            String fg = pct.compareTo(BigDecimal.valueOf(30)) >= 0 ? "#15803D"
                      : pct.compareTo(BigDecimal.valueOf(15)) >= 0 ? "#D97706"
                      : "#DC2626";
            row.getChildren().add(crearBadge(pct.toPlainString() + "% ganancia", bg, fg));
        }

        // Estado badge
        Label lblEstado = p.isActivo()
                ? crearBadge("Activo",   "#DCFCE7", "#15803D")
                : crearBadge("Inactivo", "#FEE2E2", "#DC2626");
        row.getChildren().add(lblEstado);

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // Vista: Tabla de proveedores
    // ─────────────────────────────────────────────────────────────

    private void mostrarTablaProveedores() {
        activarTab(tabProveedores);
        contentArea.getChildren().clear();

        VBox wrapper = new VBox(16);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        wrapper.setPadding(new Insets(24, 28, 28, 28));

        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lblTit = new Label("Todos los proveedores");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);

        ComboBox<String> cmbFiltro = new ComboBox<>();
        cmbFiltro.getItems().addAll("Todos", "Activos", "Inactivos", "Con % ganancia");
        cmbFiltro.setValue("Todos");
        cmbFiltro.getStyleClass().add("inventario-combo");

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por nombre o NIT...");
        txtBuscar.getStyleClass().add("venta-search-field");
        txtBuscar.setPrefWidth(240);

        toolbar.getChildren().addAll(lblTit, cmbFiltro, txtBuscar);

        // Tabla
        TableView<Proveedor> tabla = new TableView<>();
        tabla.getStyleClass().add("inventario-table-card");
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin proveedores registrados."));

        TableColumn<Proveedor, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNombre() != null ? d.getValue().getNombre() : "—"));
        colNombre.setMinWidth(150);

        TableColumn<Proveedor, String> colNit = new TableColumn<>("NIT");
        colNit.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNit() != null ? d.getValue().getNit() : "—"));
        colNit.setPrefWidth(120);

        TableColumn<Proveedor, String> colCelular = new TableColumn<>("Celular");
        colCelular.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCelular() != null ? d.getValue().getCelular() : "—"));
        colCelular.setPrefWidth(110);

        TableColumn<Proveedor, String> colDireccion = new TableColumn<>("Dirección");
        colDireccion.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDireccion() != null ? d.getValue().getDireccion() : "—"));
        colDireccion.setPrefWidth(160);

        TableColumn<Proveedor, String> colPct = new TableColumn<>("% Ganancia");
        colPct.setCellValueFactory(d -> {
            BigDecimal pct = d.getValue().getPorcentajeGanancia();
            return new SimpleStringProperty(
                    pct != null && pct.compareTo(BigDecimal.ZERO) > 0
                            ? pct.toPlainString() + "%" : "—");
        });
        colPct.setPrefWidth(110);
        colPct.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null || "—".equals(s)) { setText(s); setStyle(""); }
                else { setText(s); setStyle("-fx-font-weight: 700; -fx-text-fill: #D97706;"); }
            }
        });

        TableColumn<Proveedor, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isActivo() ? "Activo" : "Inactivo"));
        colEstado.setPrefWidth(90);
        colEstado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                boolean activo = "Activo".equals(s);
                Label badge = crearBadge(s, activo ? "#DCFCE7" : "#FEE2E2", activo ? "#15803D" : "#DC2626");
                setGraphic(badge); setText(null);
            }
        });

        TableColumn<Proveedor, String> colAccion = new TableColumn<>("");
        colAccion.setCellValueFactory(d -> new SimpleStringProperty(""));
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                FontIcon ico = new FontIcon("fas-edit");
                ico.setIconSize(13); ico.setIconColor(Paint.valueOf("#5A6ACF"));
                btn.setGraphic(ico);
                btn.getStyleClass().add("prod-row-arrow");
                btn.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null)
                        abrirModalProveedor(getTableRow().getItem());
                });
                setAlignment(Pos.CENTER);
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn); setText(null);
            }
        });
        colAccion.setMinWidth(44); colAccion.setMaxWidth(44); colAccion.setSortable(false);

        tabla.getColumns().addAll(colNombre, colNit, colCelular, colDireccion, colPct, colEstado, colAccion);

        tabla.setRowFactory(tv -> {
            TableRow<Proveedor> row = new TableRow<>();
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle("-fx-background-color: rgba(90,106,207,0.05); -fx-cursor: hand;"); });
            row.setOnMouseExited(e -> row.setStyle(""));
            row.setOnMouseClicked(e -> { if (!row.isEmpty() && row.getItem() != null) abrirModalProveedor(row.getItem()); });
            return row;
        });

        List<Proveedor> proveedoresOrdenados = todosProveedores.stream()
                .sorted(Comparator.comparing(Proveedor::getNombre, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        Runnable aplicarFiltro = () -> {
            String texto  = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase();
            String filtro = cmbFiltro.getValue();
            List<Proveedor> resultado = proveedoresOrdenados.stream()
                .filter(p -> {
                    boolean matchTexto = texto.isBlank()
                            || (p.getNombre() != null && p.getNombre().toLowerCase().contains(texto))
                            || (p.getNit()    != null && p.getNit().toLowerCase().contains(texto));
                    boolean matchFiltro = switch (filtro) {
                        case "Activos"        -> p.isActivo();
                        case "Inactivos"      -> !p.isActivo();
                        case "Con % ganancia" -> p.getPorcentajeGanancia() != null
                                                  && p.getPorcentajeGanancia().compareTo(BigDecimal.ZERO) > 0;
                        default               -> true;
                    };
                    return matchTexto && matchFiltro;
                })
                .collect(Collectors.toList());
            tabla.setItems(FXCollections.observableArrayList(resultado));
        };

        txtBuscar.textProperty().addListener((obs, o, n) -> aplicarFiltro.run());
        cmbFiltro.valueProperty().addListener((obs, o, n) -> aplicarFiltro.run());
        aplicarFiltro.run();

        wrapper.getChildren().addAll(toolbar, tabla);
        contentArea.getChildren().add(wrapper);
    }

    // ─────────────────────────────────────────────────────────────
    // Modal: Nuevo / Editar proveedor
    // ─────────────────────────────────────────────────────────────

    private void abrirModalProveedor(Proveedor proveedorExistente) {
        final Proveedor prov   = proveedorExistente;
        boolean         esNuevo = (prov == null);

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");

        VBox modal = new VBox(0);
        modal.setMaxWidth(520);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 16px; " +
                       "-fx-border-color: rgba(26,31,46,0.10); -fx-border-radius: 16px; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 36, 0, 0, 10);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #D97706; -fx-background-radius: 16px 16px 0 0; " +
                        "-fx-padding: 16 20 16 20;");
        FontIcon icoH = new FontIcon(esNuevo ? "fas-truck" : "fas-edit");
        icoH.setIconSize(16); icoH.setIconColor(Paint.valueOf("#FFFFFF"));
        Label lblH = new Label(esNuevo ? "Nuevo proveedor" : "Editar proveedor");
        lblH.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        HBox.setHgrow(lblH, Priority.ALWAYS);
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); " +
                      "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        header.getChildren().addAll(icoH, lblH, btnX);

        // Body
        VBox body = new VBox(12);
        body.setPadding(new Insets(24, 24, 8, 24));

        Label secDatos = new Label("DATOS DEL PROVEEDOR");
        secDatos.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;");

        TextField txtNombre    = crearCampo("Nombre / Razón social *");
        TextField txtNit       = crearCampo("NIT");
        TextField txtCelular   = crearCampo("Celular");
        TextField txtDireccion = crearCampo("Dirección");

        Label secMargen = new Label("MARGEN DE GANANCIA");
        secMargen.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E; -fx-padding: 6 0 0 0;");

        TextField txtPct = crearCampo("% de ganancia (ej: 25)");

        Label notaPct = new Label("Al cambiar el %, los precios de venta de los productos\nasociados a este proveedor se recalcularán automáticamente.");
        notaPct.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E; -fx-wrap-text: true;");

        // Pre-llenar si es edición
        BigDecimal[] pctAnterior = { null };
        if (!esNuevo && prov != null) {
            txtNombre.setText(prov.getNombre());
            if (prov.getNit()       != null) txtNit.setText(prov.getNit());
            if (prov.getCelular()   != null) txtCelular.setText(prov.getCelular());
            if (prov.getDireccion() != null) txtDireccion.setText(prov.getDireccion());
            if (prov.getPorcentajeGanancia() != null
                    && prov.getPorcentajeGanancia().compareTo(BigDecimal.ZERO) > 0) {
                txtPct.setText(prov.getPorcentajeGanancia().toPlainString());
                pctAnterior[0] = prov.getPorcentajeGanancia();
            }
        }

        body.getChildren().addAll(secDatos, txtNombre, txtNit, txtCelular, txtDireccion,
                                   secMargen, txtPct, notaPct);

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroll.getStyleClass().add("inventario-root-stack");
        bodyScroll.setMaxHeight(400);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 24, 20, 24));

        // Botón activar/desactivar solo en edición
        if (!esNuevo && prov != null) {
            String toggleLabel = prov.isActivo() ? "Desactivar" : "Activar";
            String toggleColor = prov.isActivo() ? "#DC2626"    : "#15803D";
            Button btnToggle   = new Button(toggleLabel);
            btnToggle.setStyle("-fx-background-color: transparent; -fx-border-color: " + toggleColor + "; " +
                    "-fx-border-radius: 8px; -fx-background-radius: 8px; " +
                    "-fx-text-fill: " + toggleColor + "; -fx-font-size: 13px; " +
                    "-fx-padding: 8 16 8 16; -fx-cursor: hand;");
            HBox.setHgrow(btnToggle, Priority.ALWAYS);
            btnToggle.setOnAction(e -> {
                try {
                    Proveedor actualizado = Proveedor.builder()
                            .id(prov.getId())
                            .nombre(prov.getNombre())
                            .nit(prov.getNit())
                            .celular(prov.getCelular())
                            .direccion(prov.getDireccion())
                            .porcentajeGanancia(prov.getPorcentajeGanancia())
                            .activo(!prov.isActivo())
                            .build();
                    proveedorService.actualizar(actualizado);
                    cerrarModal(overlay);
                    recargarDatos();
                    mostrarTablaProveedores();
                } catch (Exception ex) {
                    mostrarAlerta("Error", ex.getMessage());
                }
            });
            footer.getChildren().add(btnToggle);
        }

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 8px; " +
                "-fx-text-fill: #57534E; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");

        Button btnGuardar = new Button(esNuevo ? "Crear proveedor" : "Guardar cambios");
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 13px;");

        footer.getChildren().addAll(btnCancelar, btnGuardar);

        modal.getChildren().addAll(header, bodyScroll, footer);
        overlay.getChildren().add(modal);
        StackPane.setAlignment(modal, Pos.CENTER);

        btnX.setOnAction(e -> cerrarModal(overlay));
        btnCancelar.setOnAction(e -> cerrarModal(overlay));
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrarModal(overlay); });

        btnGuardar.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isBlank()) {
                mostrarAlerta("Campo requerido", "El nombre del proveedor es obligatorio.");
                return;
            }

            BigDecimal nuevoPct = null;
            String pctTxt = txtPct.getText().trim().replace(",", ".");
            if (!pctTxt.isBlank()) {
                try {
                    nuevoPct = new BigDecimal(pctTxt);
                    if (nuevoPct.compareTo(BigDecimal.ZERO) <= 0) {
                        mostrarAlerta("Valor inválido", "El % de ganancia debe ser mayor que cero.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    mostrarAlerta("Valor inválido", "El % de ganancia debe ser un número (ej: 25).");
                    return;
                }
            }

            try {
                Proveedor.ProveedorBuilder builder = Proveedor.builder()
                        .nombre(nombre)
                        .nit(txtNit.getText().trim().isBlank()       ? null : txtNit.getText().trim())
                        .celular(txtCelular.getText().trim().isBlank()   ? null : txtCelular.getText().trim())
                        .direccion(txtDireccion.getText().trim().isBlank() ? null : txtDireccion.getText().trim())
                        .porcentajeGanancia(nuevoPct)
                        .activo(esNuevo || (prov != null && prov.isActivo()));

                if (esNuevo) {
                    proveedorService.crear(builder.build());
                } else if (prov != null) {
                    builder.id(prov.getId());
                    proveedorService.actualizar(builder.build());

                    // Si el % cambió, disparar recálculo de precios de productos
                    boolean pctCambio = nuevoPct != null
                            && (pctAnterior[0] == null || nuevoPct.compareTo(pctAnterior[0]) != 0);
                    if (pctCambio) {
                        proveedorService.actualizarPorcentajeGanancia(prov.getId(), nuevoPct);
                    }
                }

                cerrarModal(overlay);
                recargarDatos();
                mostrarTablaProveedores();

            } catch (BusinessException ex) {
                mostrarAlerta("Error de negocio", ex.getMessage());
            } catch (Exception ex) {
                mostrarAlerta("Error inesperado", "No se pudo guardar el proveedor.");
            }
        });

        rootStack.getChildren().add(overlay);
        animarEntradaModal(overlay, modal);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers UI
    // ─────────────────────────────────────────────────────────────

    private TextField crearCampo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("inventario-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Label crearBadge(String texto, String bg, String fg) {
        Label l = new Label(texto);
        l.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 6px; " +
                   "-fx-text-fill: " + fg + "; -fx-font-size: 11px; -fx-font-weight: 600; " +
                   "-fx-padding: 3 8 3 8;");
        return l;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    // Animaciones
    // ─────────────────────────────────────────────────────────────

    private void animarEntrada(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(12);
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));
        ft.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), node);
        tt.setFromY(12); tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    private void animarClickTab(Button tab) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), tab);
        st.setFromX(1); st.setToX(0.96);
        st.setFromY(1); st.setToY(0.96);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();
    }

    private void cerrarModal(StackPane overlay) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);
        fadeOut.setOnFinished(e -> rootStack.getChildren().remove(overlay));
        fadeOut.play();
    }

    private void animarEntradaModal(StackPane overlay, Node modal) {
        overlay.setOpacity(0);
        modal.setScaleX(0.92); modal.setScaleY(0.92);
        modal.setTranslateY(20);

        FadeTransition ft  = new FadeTransition(Duration.millis(220), overlay);
        ft.setFromValue(0); ft.setToValue(1);

        ScaleTransition st = new ScaleTransition(Duration.millis(220), modal);
        st.setFromX(0.92); st.setToX(1);
        st.setFromY(0.92); st.setToY(1);
        st.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition tt = new TranslateTransition(Duration.millis(220), modal);
        tt.setFromY(20); tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, new ParallelTransition(st, tt)).play();
    }
}
