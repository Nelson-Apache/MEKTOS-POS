package com.nap.pos.ui.controller;

import com.nap.pos.application.service.ClienteService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Cliente;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.PlazoPago;
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
import javafx.scene.control.Tooltip;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientesController {

    private final ClienteService clienteService;

    private static final NumberFormat FMT = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

    // ── Estado ────────────────────────────────────────────────────
    private Usuario       usuarioActual;
    private List<Cliente> todosClientes = new ArrayList<>();

    // ── Refs UI ───────────────────────────────────────────────────
    private StackPane rootStack;
    private VBox      contentArea;
    private Button    tabDashboard;
    private Button    tabClientes;

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
        try { todosClientes = clienteService.findAll(); }
        catch (Exception e) { todosClientes = new ArrayList<>(); }
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

        FontIcon icoList = new FontIcon("fas-user-friends");
        icoList.setIconSize(14);
        icoList.setIconColor(Paint.valueOf("#78716C"));
        tabClientes = new Button("Clientes", icoList);
        tabClientes.getStyleClass().add("inventario-tab");
        tabClientes.setOnAction(e -> { animarClickTab(tabClientes); mostrarTablaClientes(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        FontIcon icoNuevo = new FontIcon("fas-plus");
        icoNuevo.setIconSize(13);
        icoNuevo.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnNuevo = new Button("Nuevo cliente", icoNuevo);
        btnNuevo.getStyleClass().add("btn-primario");
        btnNuevo.setStyle("-fx-padding: 8 18 8 18; -fx-font-size: 13px;");
        btnNuevo.setOnAction(e -> abrirModalCliente(null));

        bar.getChildren().addAll(tabDashboard, tabClientes, spacer, btnNuevo);
        return bar;
    }

    private void activarTab(Button activo) {
        tabDashboard.getStyleClass().remove("inventario-tab-active");
        tabClientes.getStyleClass().remove("inventario-tab-active");
        activo.getStyleClass().add("inventario-tab-active");

        String inactivo = "#78716C";
        String actColor = "#5A6ACF";
        ((FontIcon) tabDashboard.getGraphic()).setIconColor(Paint.valueOf(activo == tabDashboard ? actColor : inactivo));
        ((FontIcon) tabClientes.getGraphic()).setIconColor(Paint.valueOf(activo == tabClientes   ? actColor : inactivo));
    }

    // ─────────────────────────────────────────────────────────────
    // Vista: Dashboard
    // ─────────────────────────────────────────────────────────────

    private void mostrarDashboard() {
        activarTab(tabDashboard);
        recargarDatos();
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dashboard-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox inner = new VBox(28);
        inner.setPadding(new Insets(28, 32, 32, 32));

        // ── KPIs ────────────────────────────────────────────────
        long totalClientes    = todosClientes.size();
        long clientesActivos  = todosClientes.stream().filter(Cliente::isActivo).count();
        long conCredito       = todosClientes.stream().filter(Cliente::tieneCreditoHabilitado).count();
        BigDecimal deudaTotal = todosClientes.stream()
                .map(c -> c.getSaldoUtilizado() != null ? c.getSaldoUtilizado() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
            crearKpiCard("fas-users",             "#5A6ACF", "Total clientes",   String.valueOf(totalClientes)),
            crearKpiCard("fas-user-check",        "#15803D", "Clientes activos", String.valueOf(clientesActivos)),
            crearKpiCard("fas-credit-card",       "#D97706", "Con crédito",      String.valueOf(conCredito)),
            crearKpiCard("fas-hand-holding-usd",  "#DC2626", "Deuda total",      FMT.format(deudaTotal))
        );

        // ── Gráficas ─────────────────────────────────────────────
        HBox chartsRow = new HBox(16);
        VBox chartDeudores = crearTopDeudoresCard();
        VBox chartCredito  = crearResumenCreditoCard();
        HBox.setHgrow(chartDeudores, Priority.ALWAYS);
        HBox.setHgrow(chartCredito,  Priority.ALWAYS);
        chartsRow.getChildren().addAll(chartDeudores, chartCredito);

        // ── Lista recientes ──────────────────────────────────────
        Label lblRecientes = new Label("Clientes recientes");
        lblRecientes.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        VBox listaRecientes = new VBox(8);
        List<Cliente> recientes = todosClientes.stream()
                .sorted(Comparator.comparing(Cliente::getId, Comparator.reverseOrder()))
                .limit(8)
                .collect(Collectors.toList());

        if (recientes.isEmpty()) {
            Label lv = new Label("Sin clientes registrados todavía.");
            lv.setStyle("-fx-font-size: 13px; -fx-text-fill: #A8A29E;");
            lv.setPadding(new Insets(12, 0, 0, 0));
            listaRecientes.getChildren().add(lv);
        } else {
            for (int i = 0; i < recientes.size(); i++) {
                HBox row = crearFilaCliente(recientes.get(i));
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

    private VBox crearTopDeudoresCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lTit = new Label("TOP DEUDORES");
        lTit.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);
        FontIcon ico = new FontIcon("fas-chart-bar");
        ico.setIconSize(14); ico.setIconColor(Paint.valueOf("#94A3B8"));
        header.getChildren().addAll(lTit, ico);

        List<Cliente> deudores = todosClientes.stream()
                .filter(c -> c.getSaldoUtilizado() != null && c.getSaldoUtilizado().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Cliente::getSaldoUtilizado, Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());

        if (deudores.isEmpty()) {
            card.getChildren().addAll(header, buildChartEmptyState("fas-chart-bar",
                    "Sin deudas activas",
                    "Los clientes con saldo pendiente aparecerán aquí"));
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        xAxis.setTickLabelRotation(0);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setLabel("");
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return formatearMonedaCorta(object);
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.getStyleClass().add("inventario-bar-chart");
        chart.setPrefHeight(220);
        VBox.setVgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<String> categorias = new ArrayList<>();
        Set<String> etiquetasUsadas = new HashSet<>();
        deudores.forEach(c -> {
            String nombre = normalizarNombreGrafica(c.getNombre());
            String etiqueta = abreviarEtiquetaEjeX(nombre, etiquetasUsadas);
            categorias.add(etiqueta);

            XYChart.Data<String, Number> data = new XYChart.Data<>(etiqueta, c.getSaldoUtilizado());
            data.setExtraValue(nombre);
            series.getData().add(data);
        });

        xAxis.setCategories(FXCollections.observableArrayList(categorias));
        chart.getData().add(series);
        instalarTooltipsSerieMoneda(series, "Cliente");

        card.getChildren().addAll(header, chart);
        return card;
    }

    private void instalarTooltipsSerieMoneda(XYChart.Series<String, Number> series, String tituloCategoria) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            Runnable instalarTooltip = () -> {
                Node nodo = data.getNode();
                if (nodo == null) {
                    return;
                }

                Object extra = data.getExtraValue();
                String categoria = extra instanceof String ? (String) extra : data.getXValue();
                Number valor = data.getYValue() != null ? data.getYValue() : 0;

                Tooltip tooltip = new Tooltip(
                        tituloCategoria + ": " + categoria + "\nValor: " + FMT.format(valor.doubleValue()));
                tooltip.setShowDelay(Duration.millis(120));
                Tooltip.install(nodo, tooltip);
            };

            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode == null) {
                    return;
                }
                instalarTooltip.run();
            });

            instalarTooltip.run();
        }
    }

    private String formatearMonedaCorta(Number valor) {
        if (valor == null) {
            return "$0";
        }

        double numero = valor.doubleValue();
        double abs = Math.abs(numero);

        if (abs >= 1_000_000_000d) {
            return String.format(Locale.of("es", "CO"), "$%.1fB", numero / 1_000_000_000d);
        }
        if (abs >= 1_000_000d) {
            return String.format(Locale.of("es", "CO"), "$%.1fM", numero / 1_000_000d);
        }
        if (abs >= 1_000d) {
            return String.format(Locale.of("es", "CO"), "$%.0fk", numero / 1_000d);
        }
        return String.format(Locale.of("es", "CO"), "$%.0f", numero);
    }

    private String normalizarNombreGrafica(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "Sin nombre";
        }
        return nombre.trim();
    }

    private String abreviarEtiquetaEjeX(String nombreCompleto, Set<String> etiquetasUsadas) {
        String limpio = normalizarNombreGrafica(nombreCompleto);
        String[] partes = limpio.split("\\s+");

        String base;
        if (partes.length >= 2) {
            StringBuilder iniciales = new StringBuilder();
            for (String parte : partes) {
                if (parte.isBlank()) {
                    continue;
                }

                char inicial = Character.toUpperCase(parte.charAt(0));
                if (!Character.isLetterOrDigit(inicial)) {
                    continue;
                }

                iniciales.append(inicial);
                if (iniciales.length() == 3) {
                    break;
                }
            }
            base = iniciales.isEmpty() ? "SN" : iniciales.toString();
        } else {
            String alfanumerico = limpio.replaceAll("[^\\p{L}\\p{Nd}]", "");
            if (alfanumerico.isBlank()) {
                base = "SN";
            } else if (alfanumerico.length() <= 4) {
                base = alfanumerico.toUpperCase(Locale.of("es", "CO"));
            } else {
                base = alfanumerico.substring(0, 4).toUpperCase(Locale.of("es", "CO"));
            }
        }

        String etiqueta = base;
        int sufijo = 2;
        while (etiquetasUsadas.contains(etiqueta)) {
            etiqueta = base + sufijo;
            sufijo++;
        }

        etiquetasUsadas.add(etiqueta);
        return etiqueta;
    }

    private VBox crearResumenCreditoCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lTit = new Label("RESUMEN DE CRÉDITO");
        lTit.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);
        FontIcon ico = new FontIcon("fas-credit-card");
        ico.setIconSize(14); ico.setIconColor(Paint.valueOf("#94A3B8"));
        header.getChildren().addAll(lTit, ico);

        BigDecimal creditoTotal = todosClientes.stream()
                .filter(Cliente::tieneCreditoHabilitado)
                .map(Cliente::getMontoCredito)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoUsado = todosClientes.stream()
                .map(c -> c.getSaldoUtilizado() != null ? c.getSaldoUtilizado() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoDisponible = creditoTotal.subtract(saldoUsado).max(BigDecimal.ZERO);
        long conPlazo15 = todosClientes.stream().filter(c -> c.getPlazoPago() == PlazoPago.QUINCE_DIAS).count();
        long conPlazo30 = todosClientes.stream().filter(c -> c.getPlazoPago() == PlazoPago.TREINTA_DIAS).count();

        VBox filas = new VBox(12);
        filas.getChildren().addAll(
            crearFilaResumen("fas-wallet",       "#5A6ACF", "Crédito total aprobado", FMT.format(creditoTotal)),
            crearFilaResumen("fas-minus-circle", "#DC2626", "Saldo utilizado",        FMT.format(saldoUsado)),
            crearFilaResumen("fas-check-circle", "#15803D", "Saldo disponible",       FMT.format(saldoDisponible)),
            crearFilaResumen("fas-clock",        "#D97706", "Plazo 15 días",          conPlazo15 + " clientes"),
            crearFilaResumen("fas-calendar-alt", "#7C3AED", "Plazo 30 días",          conPlazo30 + " clientes")
        );

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

    private HBox crearFilaCliente(Cliente c) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 10px; " +
                     "-fx-border-color: rgba(26,31,46,0.08); -fx-border-width: 1; " +
                     "-fx-border-radius: 10px; -fx-padding: 12 16 12 16;");
        row.setOnMouseEntered(e -> row.setStyle(row.getStyle().replace("#FDFCFA", "#F5F1EB")));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("#F5F1EB", "#FDFCFA")));

        // Avatar con inicial
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: #5A6ACF22; -fx-background-radius: 20px;");
        avatar.setMinSize(36, 36); avatar.setMaxSize(36, 36);
        String inicial = c.getNombre() != null && !c.getNombre().isBlank()
                ? c.getNombre().substring(0, 1).toUpperCase() : "?";
        Label lblIni = new Label(inicial);
        lblIni.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #5A6ACF;");
        avatar.getChildren().add(lblIni);

        // Info
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lNombre = new Label(c.getNombre() != null ? c.getNombre() : "—");
        lNombre.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1A1F2E;");
        String sub = (c.getCedula() != null ? "CC " + c.getCedula() : "Sin cédula")
                   + (c.getCelular() != null ? "  ·  " + c.getCelular() : "");
        Label lSub = new Label(sub);
        lSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
        info.getChildren().addAll(lNombre, lSub);

        row.getChildren().addAll(avatar, info);

        // Crédito disponible badge
        if (c.tieneCreditoHabilitado()) {
            BigDecimal disponible = c.getSaldoDisponible();
            String bg = disponible.compareTo(BigDecimal.ZERO) > 0 ? "#DCFCE7" : "#FEE2E2";
            String fg = disponible.compareTo(BigDecimal.ZERO) > 0 ? "#15803D" : "#DC2626";
            Label lblCredito = crearBadge("Disp. " + FMT.format(disponible), bg, fg);
            row.getChildren().add(lblCredito);
        }

        // Estado badge
        Label lblEstado = c.isActivo()
                ? crearBadge("Activo",   "#DCFCE7", "#15803D")
                : crearBadge("Inactivo", "#FEE2E2", "#DC2626");
        row.getChildren().add(lblEstado);

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // Vista: Tabla de clientes
    // ─────────────────────────────────────────────────────────────

    private void mostrarTablaClientes() {
        activarTab(tabClientes);
        contentArea.getChildren().clear();

        VBox wrapper = new VBox(16);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        wrapper.setPadding(new Insets(24, 28, 28, 28));

        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lblTit = new Label("Todos los clientes");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);

        ComboBox<String> cmbFiltro = new ComboBox<>();
        cmbFiltro.getItems().addAll("Todos", "Activos", "Inactivos", "Con crédito");
        cmbFiltro.setValue("Todos");
        cmbFiltro.getStyleClass().add("inventario-combo");

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por nombre o cédula...");
        txtBuscar.getStyleClass().add("venta-search-field");
        txtBuscar.setPrefWidth(240);

        toolbar.getChildren().addAll(lblTit, cmbFiltro, txtBuscar);

        // Tabla
        TableView<Cliente> tabla = new TableView<>();
        tabla.getStyleClass().add("inventario-table-card");
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin clientes registrados."));

        TableColumn<Cliente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNombre() != null ? d.getValue().getNombre() : "—"));
        colNombre.setMinWidth(150);

        TableColumn<Cliente, String> colCedula = new TableColumn<>("Cédula");
        colCedula.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCedula() != null ? d.getValue().getCedula() : "—"));
        colCedula.setPrefWidth(120);

        TableColumn<Cliente, String> colCelular = new TableColumn<>("Celular");
        colCelular.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCelular() != null ? d.getValue().getCelular() : "—"));
        colCelular.setPrefWidth(110);

        TableColumn<Cliente, String> colCredito = new TableColumn<>("Límite crédito");
        colCredito.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().tieneCreditoHabilitado()
                        ? FMT.format(d.getValue().getMontoCredito()) : "—"));
        colCredito.setPrefWidth(130);

        TableColumn<Cliente, String> colSaldoUsado = new TableColumn<>("Saldo usado");
        colSaldoUsado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSaldoUtilizado() != null
                        && d.getValue().getSaldoUtilizado().compareTo(BigDecimal.ZERO) > 0
                        ? FMT.format(d.getValue().getSaldoUtilizado()) : "—"));
        colSaldoUsado.setPrefWidth(120);
        colSaldoUsado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null || "—".equals(s)) { setText(s); setStyle(""); }
                else { setText(s); setStyle("-fx-font-weight: 700; -fx-text-fill: #DC2626;"); }
            }
        });

        TableColumn<Cliente, String> colEstado = new TableColumn<>("Estado");
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

        TableColumn<Cliente, String> colAccion = new TableColumn<>("");
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
                        abrirModalCliente(getTableRow().getItem());
                });
                setAlignment(Pos.CENTER);
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn); setText(null);
            }
        });
        colAccion.setMinWidth(44); colAccion.setMaxWidth(44); colAccion.setSortable(false);

        tabla.getColumns().addAll(colNombre, colCedula, colCelular, colCredito, colSaldoUsado, colEstado, colAccion);

        tabla.setRowFactory(tv -> {
            TableRow<Cliente> row = new TableRow<>();
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle("-fx-background-color: rgba(90,106,207,0.05); -fx-cursor: hand;"); });
            row.setOnMouseExited(e -> row.setStyle(""));
            row.setOnMouseClicked(e -> { if (!row.isEmpty() && row.getItem() != null) abrirModalCliente(row.getItem()); });
            return row;
        });

        List<Cliente> clientesOrdenados = todosClientes.stream()
                .sorted(Comparator.comparing(Cliente::getNombre, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        Runnable aplicarFiltro = () -> {
            String texto  = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase();
            String filtro = cmbFiltro.getValue();
            List<Cliente> resultado = clientesOrdenados.stream()
                .filter(c -> {
                    boolean matchTexto = texto.isBlank()
                            || (c.getNombre() != null && c.getNombre().toLowerCase().contains(texto))
                            || (c.getCedula() != null && c.getCedula().toLowerCase().contains(texto));
                    boolean matchFiltro = switch (filtro) {
                        case "Activos"     -> c.isActivo();
                        case "Inactivos"   -> !c.isActivo();
                        case "Con crédito" -> c.tieneCreditoHabilitado();
                        default            -> true;
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
    // Modal: Nuevo / Editar cliente
    // ─────────────────────────────────────────────────────────────

    private void abrirModalCliente(Cliente clienteExistente) {
        boolean esNuevo = (clienteExistente == null);

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
        header.setStyle("-fx-background-color: #5A6ACF; -fx-background-radius: 16px 16px 0 0; " +
                        "-fx-padding: 16 20 16 20;");
        FontIcon icoH = new FontIcon(esNuevo ? "fas-user-plus" : "fas-user-edit");
        icoH.setIconSize(16); icoH.setIconColor(Paint.valueOf("#FFFFFF"));
        Label lblH = new Label(esNuevo ? "Nuevo cliente" : "Editar cliente");
        lblH.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        HBox.setHgrow(lblH, Priority.ALWAYS);
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); " +
                      "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        header.getChildren().addAll(icoH, lblH, btnX);

        // Body
        VBox body = new VBox(12);
        body.setPadding(new Insets(24, 24, 8, 24));

        Label secDatos = new Label("DATOS PERSONALES");
        secDatos.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;");

        TextField txtNombre    = crearCampo("Nombre completo *");
        TextField txtCedula    = crearCampo("Número de cédula *");
        TextField txtCelular   = crearCampo("Celular");
        TextField txtDireccion = crearCampo("Dirección");

        Label secCredito = new Label("CRÉDITO (OPCIONAL)");
        secCredito.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E; -fx-padding: 6 0 0 0;");

        TextField txtMontoCredito = crearCampo("Monto de crédito aprobado");
        txtMontoCredito.setPromptText("Dejar vacío si no aplica");

        ComboBox<PlazoPago> cmbPlazo = new ComboBox<>();
        cmbPlazo.getItems().addAll(PlazoPago.values());
        cmbPlazo.setPromptText("Plazo de pago");
        cmbPlazo.getStyleClass().add("inventario-combo");
        cmbPlazo.setMaxWidth(Double.MAX_VALUE);
        cmbPlazo.setConverter(new StringConverter<>() {
            @Override public String toString(PlazoPago p) {
                if (p == null) return "";
                return p == PlazoPago.QUINCE_DIAS ? "15 días" : "30 días";
            }
            @Override public PlazoPago fromString(String s) { return null; }
        });

        // Pre-llenar si es edición
        if (!esNuevo) {
            txtNombre.setText(clienteExistente.getNombre());
            txtCedula.setText(clienteExistente.getCedula());
            if (clienteExistente.getCelular()   != null) txtCelular.setText(clienteExistente.getCelular());
            if (clienteExistente.getDireccion() != null) txtDireccion.setText(clienteExistente.getDireccion());
            if (clienteExistente.getMontoCredito() != null
                    && clienteExistente.getMontoCredito().compareTo(BigDecimal.ZERO) > 0)
                txtMontoCredito.setText(clienteExistente.getMontoCredito().toPlainString());
            if (clienteExistente.getPlazoPago() != null)
                cmbPlazo.setValue(clienteExistente.getPlazoPago());
        }

        body.getChildren().addAll(secDatos, txtNombre, txtCedula, txtCelular, txtDireccion,
                                   secCredito, txtMontoCredito, cmbPlazo);

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
        if (!esNuevo) {
            String toggleLabel = clienteExistente.isActivo() ? "Desactivar" : "Activar";
            String toggleColor = clienteExistente.isActivo() ? "#DC2626"    : "#15803D";
            Button btnToggle   = new Button(toggleLabel);
            btnToggle.setStyle("-fx-background-color: transparent; -fx-border-color: " + toggleColor + "; " +
                    "-fx-border-radius: 8px; -fx-background-radius: 8px; " +
                    "-fx-text-fill: " + toggleColor + "; -fx-font-size: 13px; " +
                    "-fx-padding: 8 16 8 16; -fx-cursor: hand;");
            HBox.setHgrow(btnToggle, Priority.ALWAYS);
            btnToggle.setOnAction(e -> {
                try {
                    if (clienteExistente.isActivo()) clienteService.desactivar(clienteExistente.getId());
                    else                             clienteService.activar(clienteExistente.getId());
                    cerrarModal(overlay);
                    recargarDatos();
                    mostrarTablaClientes();
                } catch (Exception ex) {
                    mostrarAlerta("Error", ex.getMessage());
                }
            });
            footer.getChildren().add(btnToggle);
        }

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 8px; " +
                "-fx-text-fill: #57534E; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");

        Button btnGuardar = new Button(esNuevo ? "Crear cliente" : "Guardar cambios");
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
            String cedula = txtCedula.getText().trim();
            if (nombre.isBlank() || cedula.isBlank()) {
                mostrarAlerta("Campos requeridos", "El nombre y la cédula son obligatorios.");
                return;
            }

            BigDecimal montoCredito = null;
            String montoTxt = txtMontoCredito.getText().trim()
                    .replace(",", "").replace("$", "").replace(".", "");
            if (!montoTxt.isBlank()) {
                try { montoCredito = new BigDecimal(montoTxt); }
                catch (NumberFormatException ex) {
                    mostrarAlerta("Valor inválido", "El monto de crédito debe ser un número.");
                    return;
                }
            }

            try {
                Cliente.ClienteBuilder builder = Cliente.builder()
                        .nombre(nombre)
                        .cedula(cedula)
                        .celular(txtCelular.getText().trim().isBlank()   ? null : txtCelular.getText().trim())
                        .direccion(txtDireccion.getText().trim().isBlank() ? null : txtDireccion.getText().trim())
                        .montoCredito(montoCredito)
                        .plazoPago(cmbPlazo.getValue())
                        .activo(esNuevo || clienteExistente.isActivo());

                if (!esNuevo) {
                    builder.id(clienteExistente.getId())
                           .saldoUtilizado(clienteExistente.getSaldoUtilizado());
                    clienteService.actualizar(builder.build());
                } else {
                    clienteService.crear(builder.build());
                }

                cerrarModal(overlay);
                recargarDatos();
                mostrarTablaClientes();

            } catch (BusinessException ex) {
                mostrarAlerta("Error de negocio", ex.getMessage());
            } catch (Exception ex) {
                mostrarAlerta("Error inesperado", "No se pudo guardar el cliente.");
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
