package com.nap.pos.ui.controller;

import com.nap.pos.application.dto.*;
import com.nap.pos.application.service.CajaService;
import com.nap.pos.application.service.ProveedorService;
import com.nap.pos.application.service.ReporteService;
import com.nap.pos.domain.model.Caja;
import com.nap.pos.domain.model.Proveedor;
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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ReportesController {

    private final ReporteService   reporteService;
    private final CajaService      cajaService;
    private final ProveedorService proveedorService;

    private static final NumberFormat     FMT = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
    private static final DateTimeFormatter DFT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] NOMBRES_MESES = {
        "", "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    // ── Estado ────────────────────────────────────────────────────
    private List<Caja>      todasCajas       = new ArrayList<>();
    private List<Proveedor> todosProveedores = new ArrayList<>();

    // ── Refs UI ───────────────────────────────────────────────────
    private StackPane rootStack;
    private VBox      contentArea;
    private Button    tabVentas;
    private Button    tabRentabilidad;
    private Button    tabInventario;
    private Button    tabCreditos;
    private Button    tabCompras;

    // ─────────────────────────────────────────────────────────────
    // Punto de entrada
    // ─────────────────────────────────────────────────────────────

    public Node buildView() {
        recargarDatos();

        rootStack = new StackPane();
        rootStack.getStyleClass().add("inventario-root-stack");

        VBox root = new VBox(0);
        root.getStyleClass().add("inventario-root");

        contentArea = new VBox();
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        root.getChildren().addAll(buildTabBar(), contentArea);
        rootStack.getChildren().add(root);

        mostrarVentas();
        return rootStack;
    }

    private void recargarDatos() {
        try { todasCajas = cajaService.findAll(); }
        catch (Exception e) { todasCajas = new ArrayList<>(); }

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

        tabVentas       = crearTab("fas-receipt",     "Ventas",        true,  () -> { animarClickTab(tabVentas);       mostrarVentas();        });
        tabRentabilidad = crearTab("fas-chart-line",  "Rentabilidad",  false, () -> { animarClickTab(tabRentabilidad); mostrarRentabilidad();   });
        tabInventario   = crearTab("fas-boxes",       "Inventario",    false, () -> { animarClickTab(tabInventario);   mostrarInventario();     });
        tabCreditos     = crearTab("fas-credit-card", "Créditos",      false, () -> { animarClickTab(tabCreditos);     mostrarCreditos();       });
        tabCompras      = crearTab("fas-truck",       "Compras",       false, () -> { animarClickTab(tabCompras);      mostrarCompras();        });

        bar.getChildren().addAll(tabVentas, tabRentabilidad, tabInventario, tabCreditos, tabCompras);
        return bar;
    }

    private Button crearTab(String icon, String texto, boolean activo, Runnable accion) {
        FontIcon ico = new FontIcon(icon);
        ico.setIconSize(14);
        ico.setIconColor(Paint.valueOf(activo ? "#5A6ACF" : "#78716C"));
        Button btn = new Button(texto, ico);
        btn.getStyleClass().add("inventario-tab");
        if (activo) btn.getStyleClass().add("inventario-tab-active");
        btn.setOnAction(e -> accion.run());
        return btn;
    }

    private void activarTab(Button activo) {
        List.of(tabVentas, tabRentabilidad, tabInventario, tabCreditos, tabCompras).forEach(t -> {
            t.getStyleClass().remove("inventario-tab-active");
            if (t.getGraphic() instanceof FontIcon fi) fi.setIconColor(Paint.valueOf("#78716C"));
        });
        activo.getStyleClass().add("inventario-tab-active");
        if (activo.getGraphic() instanceof FontIcon fi) fi.setIconColor(Paint.valueOf("#5A6ACF"));
    }

    // ─────────────────────────────────────────────────────────────
    // Tab 1 — Ventas por Caja
    // ─────────────────────────────────────────────────────────────

    private void mostrarVentas() {
        activarTab(tabVentas);
        contentArea.getChildren().clear();

        ScrollPane scroll = buildScrollPane();
        VBox inner = new VBox(24);
        inner.setPadding(new Insets(28, 32, 32, 32));

        VBox selectorCard = buildSelectorCard("fas-cash-register", "#5A6ACF",
                "Reporte de ventas por sesión de caja",
                "Elige una sesión para ver el detalle de ingresos y productos vendidos");

        ComboBox<Caja> comboCaja = new ComboBox<>();
        comboCaja.getStyleClass().add("inventario-field");
        comboCaja.setMaxWidth(Double.MAX_VALUE);
        comboCaja.setPromptText("Selecciona una caja...");

        List<Caja> cajasOrdenadas = todasCajas.stream()
                .filter(c -> c.getFechaApertura() != null)
                .sorted(Comparator.comparing(Caja::getFechaApertura, Comparator.reverseOrder()))
                .toList();
        comboCaja.setItems(FXCollections.observableArrayList(cajasOrdenadas));
        comboCaja.setConverter(new StringConverter<>() {
            @Override public String toString(Caja c) {
                if (c == null) return "";
                String estado = c.estaAbierta() ? "En curso" : "Cerrada";
                return (c.getFechaApertura() != null ? c.getFechaApertura().format(DFT) : "—") + "  ·  " + estado;
            }
            @Override public Caja fromString(String s) { return null; }
        });

        cajasOrdenadas.stream().filter(Caja::estaAbierta).findFirst()
                .ifPresent(comboCaja::setValue);

        VBox resultado = new VBox(24);

        comboCaja.setOnAction(e -> {
            Caja sel = comboCaja.getValue();
            if (sel == null) return;
            resultado.getChildren().clear();
            try {
                ReporteVentasDto r = reporteService.reporteVentasPorCaja(sel.getId());
                resultado.getChildren().addAll(buildResultadoVentas(r));
            } catch (Exception ex) {
                resultado.getChildren().add(buildError(ex.getMessage()));
            }
            animarEntrada(resultado, 0);
        });

        selectorBody(selectorCard).getChildren().add(comboCaja);
        inner.getChildren().addAll(selectorCard, resultado);

        if (comboCaja.getValue() != null) comboCaja.getOnAction().handle(null);

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    private List<Node> buildResultadoVentas(ReporteVentasDto r) {
        List<Node> nodes = new ArrayList<>();

        nodes.add(buildResultHeader("Ventas — sesión " +
                (r.fechaApertura() != null ? r.fechaApertura().format(DFT) : "—"),
                () -> exportarVentasExcel(r)));

        HBox kpi1 = new HBox(16);
        kpi1.getChildren().addAll(
            crearKpiCard("fas-receipt",      "#5A6ACF", "Total ventas",  String.valueOf(r.totalVentas())),
            crearKpiCard("fas-check-circle", "#15803D", "Completadas",   String.valueOf(r.ventasCompletadas())),
            crearKpiCard("fas-times-circle", "#DC2626", "Anuladas",      String.valueOf(r.ventasAnuladas()))
        );

        HBox kpi2 = new HBox(16);
        kpi2.getChildren().addAll(
            crearKpiCard("fas-dollar-sign",     "#15803D", "Total cobrado",   FMT.format(r.totalGeneral())),
            crearKpiCard("fas-money-bill-wave", "#D97706", "Efectivo",         FMT.format(r.totalEfectivo())),
            crearKpiCard("fas-exchange-alt",    "#7C3AED", "Transferencias",   FMT.format(r.totalTransferencia())),
            crearKpiCard("fas-credit-card",     "#DC2626", "A crédito",        FMT.format(r.totalCredito()))
        );

        nodes.add(kpi1);
        nodes.add(kpi2);

        if (!r.topProductos().isEmpty()) {
            Label lbl = new Label("Productos más vendidos");
            lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
            nodes.add(lbl);

            TableView<ProductoVendidoDto> tabla = buildTableView();
            TableColumn<ProductoVendidoDto, String> colNombre   = col("Producto",       d -> d.nombre());
            TableColumn<ProductoVendidoDto, String> colCodigo   = col("Código",         d -> d.codigoBarras() != null ? d.codigoBarras() : "—", 120);
            TableColumn<ProductoVendidoDto, String> colCantidad = col("Unidades",       d -> String.valueOf(d.cantidadVendida()), 90);
            TableColumn<ProductoVendidoDto, String> colTotal    = col("Total generado", d -> FMT.format(d.totalGenerado()), 140);
            colCantidad.setCellFactory(tc -> centeredCell());
            colTotal.setCellFactory(tc -> boldColorCell("#15803D"));
            tabla.getColumns().addAll(colNombre, colCodigo, colCantidad, colTotal);
            tabla.setItems(FXCollections.observableArrayList(r.topProductos()));
            nodes.add(tabla);
        }

        return nodes;
    }

    // ─────────────────────────────────────────────────────────────
    // Tab 2 — Rentabilidad
    // ─────────────────────────────────────────────────────────────

    private void mostrarRentabilidad() {
        activarTab(tabRentabilidad);
        contentArea.getChildren().clear();

        ScrollPane scroll = buildScrollPane();
        VBox inner = new VBox(24);
        inner.setPadding(new Insets(28, 32, 32, 32));

        VBox selectorCard = buildSelectorCard("fas-chart-line", "#15803D",
                "Análisis de rentabilidad",
                "Compara ingresos por ventas contra inversión en compras");

        HBox controles = new HBox(12);
        controles.setAlignment(Pos.CENTER_LEFT);

        Button btnMensual = new Button("Mensual");
        btnMensual.setStyle(estiloToggleActivo());
        Button btnAnual = new Button("Anual");
        btnAnual.setStyle(estiloToggle());

        int anioActual = LocalDateTime.now().getYear();
        int mesActual  = LocalDateTime.now().getMonthValue();

        ComboBox<Integer> comboAnio = new ComboBox<>();
        comboAnio.getStyleClass().add("inventario-field");
        comboAnio.getItems().addAll(anioActual, anioActual - 1, anioActual - 2);
        comboAnio.setValue(anioActual);

        ComboBox<Integer> comboMes = new ComboBox<>();
        comboMes.getStyleClass().add("inventario-field");
        comboMes.setConverter(new StringConverter<>() {
            @Override public String toString(Integer m)   { return m != null ? NOMBRES_MESES[m] : ""; }
            @Override public Integer fromString(String s) { return null; }
        });
        for (int i = 1; i <= 12; i++) comboMes.getItems().add(i);
        comboMes.setValue(mesActual);

        Button btnGenerar = buildBotonPrimario("Generar", "fas-play", "#5A6ACF");

        controles.getChildren().addAll(btnMensual, btnAnual, comboAnio, comboMes, btnGenerar);
        selectorBody(selectorCard).getChildren().add(controles);

        VBox resultado = new VBox(24);
        final boolean[] modoMensual = { true };

        btnMensual.setOnAction(e -> {
            modoMensual[0] = true;
            btnMensual.setStyle(estiloToggleActivo()); btnAnual.setStyle(estiloToggle());
            comboMes.setVisible(true); comboMes.setManaged(true);
        });
        btnAnual.setOnAction(e -> {
            modoMensual[0] = false;
            btnAnual.setStyle(estiloToggleActivo()); btnMensual.setStyle(estiloToggle());
            comboMes.setVisible(false); comboMes.setManaged(false);
        });

        btnGenerar.setOnAction(e -> {
            resultado.getChildren().clear();
            try {
                if (modoMensual[0]) {
                    ReporteRentabilidadDto r = reporteService.reporteRentabilidad(
                            comboAnio.getValue(), comboMes.getValue());
                    resultado.getChildren().addAll(buildResultadoRentMensual(r));
                } else {
                    ReporteRentabilidadAnualDto r = reporteService.reporteRentabilidadAnual(comboAnio.getValue());
                    resultado.getChildren().addAll(buildResultadoRentAnual(r));
                }
            } catch (Exception ex) {
                resultado.getChildren().add(buildError(ex.getMessage()));
            }
            animarEntrada(resultado, 0);
        });

        inner.getChildren().addAll(selectorCard, resultado);
        btnGenerar.fire();

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    private List<Node> buildResultadoRentMensual(ReporteRentabilidadDto r) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(buildResultHeader(
                "Rentabilidad — " + NOMBRES_MESES[r.mes()] + " " + r.anio(),
                () -> exportarRentabilidadMensualExcel(r)));

        String color = r.tuvoPerdida() ? "#DC2626" : "#15803D";
        String margen = r.margenPorcentaje() != null ? r.margenPorcentaje().toPlainString() + "%" : "—";

        HBox kpi = new HBox(16);
        kpi.getChildren().addAll(
            crearKpiCard("fas-shopping-cart", "#D97706", "Total invertido", FMT.format(r.totalInvertido())),
            crearKpiCard("fas-dollar-sign",   "#5A6ACF", "Total vendido",   FMT.format(r.totalVendido())),
            crearKpiCard("fas-chart-line",    color,     "Ganancia bruta",  FMT.format(r.gananciaBruta())),
            crearKpiCard("fas-percentage",    color,     "Margen",          margen)
        );
        nodes.add(kpi);

        if (r.tuvoPerdida())
            nodes.add(buildBanner("fas-exclamation-triangle",
                    "Este período tuvo pérdida. Las compras superaron los ingresos por ventas.",
                    "#FEF3C7", "#D97706"));

        return nodes;
    }

    private List<Node> buildResultadoRentAnual(ReporteRentabilidadAnualDto r) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(buildResultHeader(
                "Rentabilidad anual — " + r.anio(),
                () -> exportarRentabilidadAnualExcel(r)));

        String color = r.tuvoPerdida() ? "#DC2626" : "#15803D";
        String margen = r.margenPorcentaje() != null ? r.margenPorcentaje().toPlainString() + "%" : "—";

        HBox kpi = new HBox(16);
        kpi.getChildren().addAll(
            crearKpiCard("fas-shopping-cart", "#D97706", "Total invertido", FMT.format(r.totalInvertido())),
            crearKpiCard("fas-dollar-sign",   "#5A6ACF", "Total vendido",   FMT.format(r.totalVendido())),
            crearKpiCard("fas-chart-line",    color,     "Ganancia bruta",  FMT.format(r.gananciaBruta())),
            crearKpiCard("fas-percentage",    color,     "Margen anual",    margen)
        );
        nodes.add(kpi);

        Label lbl = new Label("Desglose mensual");
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        nodes.add(lbl);

        TableView<RentabilidadMensualDto> tabla = buildTableView();
        TableColumn<RentabilidadMensualDto, String> colMes       = col("Mes",       d -> d.nombreMes(), 100);
        TableColumn<RentabilidadMensualDto, String> colInvertido = col("Invertido", d -> FMT.format(d.totalInvertido()), 130);
        TableColumn<RentabilidadMensualDto, String> colVendido   = col("Vendido",   d -> FMT.format(d.totalVendido()), 130);
        TableColumn<RentabilidadMensualDto, String> colGanancia  = col("Ganancia",  d -> FMT.format(d.gananciaBruta()), 130);
        TableColumn<RentabilidadMensualDto, String> colEstado    = col("Estado",    d -> d.tuvoPerdida() ? "Pérdida" : "Ganancia", 90);

        colGanancia.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                RentabilidadMensualDto item = getTableRow() != null ? getTableRow().getItem() : null;
                setText(s);
                setStyle("-fx-font-weight:700;-fx-text-fill:" + (item != null && item.tuvoPerdida() ? "#DC2626" : "#15803D") + ";");
            }
        });
        colEstado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                boolean perdida = "Pérdida".equals(s);
                setGraphic(crearBadge(s, perdida ? "#FEE2E2" : "#DCFCE7", perdida ? "#DC2626" : "#15803D"));
                setText(null);
            }
        });

        tabla.getColumns().addAll(colMes, colInvertido, colVendido, colGanancia, colEstado);
        tabla.setItems(FXCollections.observableArrayList(r.meses()));
        nodes.add(tabla);
        return nodes;
    }

    // ─────────────────────────────────────────────────────────────
    // Tab 3 — Inventario / Alertas de Stock
    // ─────────────────────────────────────────────────────────────

    private void mostrarInventario() {
        activarTab(tabInventario);
        contentArea.getChildren().clear();

        ScrollPane scroll = buildScrollPane();
        VBox inner = new VBox(24);
        inner.setPadding(new Insets(28, 32, 32, 32));

        VBox selectorCard = buildSelectorCard("fas-boxes", "#D97706",
                "Alertas de stock",
                "Productos agotados y con existencias por debajo del umbral mínimo");

        HBox umbralRow = new HBox(12);
        umbralRow.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Umbral de bajo stock:");
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #57534E;");
        Spinner<Integer> spinner = new Spinner<>(1, 100, 5);
        spinner.setEditable(true);
        spinner.setPrefWidth(80);
        Button btnGenerar = buildBotonPrimario("Generar", "fas-play", "#D97706");

        umbralRow.getChildren().addAll(lbl, spinner, btnGenerar);
        selectorBody(selectorCard).getChildren().add(umbralRow);

        VBox resultado = new VBox(24);

        btnGenerar.setOnAction(e -> {
            resultado.getChildren().clear();
            try {
                ReporteInventarioDto r = reporteService.reporteInventario(spinner.getValue());
                resultado.getChildren().addAll(buildResultadoInventario(r));
            } catch (Exception ex) {
                resultado.getChildren().add(buildError(ex.getMessage()));
            }
            animarEntrada(resultado, 0);
        });

        inner.getChildren().addAll(selectorCard, resultado);
        btnGenerar.fire();

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    private List<Node> buildResultadoInventario(ReporteInventarioDto r) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(buildResultHeader("Reporte de inventario — umbral ≤ " + r.umbralBajoStock(),
                () -> exportarInventarioExcel(r)));

        HBox kpi = new HBox(16);
        kpi.getChildren().addAll(
            crearKpiCard("fas-boxes",              "#5A6ACF", "Productos activos", String.valueOf(r.totalProductosActivos())),
            crearKpiCard("fas-times-circle",       "#DC2626", "Agotados",          String.valueOf(r.productosAgotados())),
            crearKpiCard("fas-exclamation-circle", "#D97706", "Bajo stock",        String.valueOf(r.productosBajoStock()))
        );
        nodes.add(kpi);

        if (r.productosAgotados() == 0 && r.productosBajoStock() == 0) {
            nodes.add(buildBanner("fas-check-circle",
                    "¡Excelente! No hay productos agotados ni con bajo stock.",
                    "#DCFCE7", "#15803D"));
            return nodes;
        }

        if (!r.agotados().isEmpty()) {
            Label lblAg = new Label("Productos agotados (" + r.productosAgotados() + ")");
            lblAg.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
            nodes.add(lblAg);
            nodes.add(buildTablaStock(r.agotados()));
        }

        if (!r.bajoStock().isEmpty()) {
            Label lblBs = new Label("Bajo stock — ≤ " + r.umbralBajoStock() + " unidades (" + r.productosBajoStock() + ")");
            lblBs.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #D97706;");
            nodes.add(lblBs);
            nodes.add(buildTablaStock(r.bajoStock()));
        }

        return nodes;
    }

    private TableView<ProductoStockDto> buildTablaStock(List<ProductoStockDto> items) {
        TableView<ProductoStockDto> tabla = buildTableView();
        TableColumn<ProductoStockDto, String> colNombre    = col("Producto",   d -> d.nombre());
        TableColumn<ProductoStockDto, String> colCodigo    = col("Código",     d -> d.codigoBarras() != null ? d.codigoBarras() : "—", 120);
        TableColumn<ProductoStockDto, String> colProveedor = col("Proveedor",  d -> d.proveedor(), 130);
        TableColumn<ProductoStockDto, String> colStock     = col("Stock",      d -> String.valueOf(d.stock()), 70);
        TableColumn<ProductoStockDto, String> colCompra    = col("P. Compra",  d -> FMT.format(d.precioCompra()), 120);
        TableColumn<ProductoStockDto, String> colVenta     = col("P. Venta",   d -> FMT.format(d.precioVenta()), 120);

        colStock.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                ProductoStockDto item = getTableRow() != null ? getTableRow().getItem() : null;
                setText(s);
                setStyle("-fx-font-weight:700;-fx-alignment:CENTER;-fx-text-fill:" +
                        (item != null && item.stock() == 0 ? "#DC2626" : "#D97706") + ";");
            }
        });

        tabla.getColumns().addAll(colNombre, colCodigo, colProveedor, colStock, colCompra, colVenta);
        tabla.setItems(FXCollections.observableArrayList(items));
        return tabla;
    }

    // ─────────────────────────────────────────────────────────────
    // Tab 4 — Créditos pendientes
    // ─────────────────────────────────────────────────────────────

    private void mostrarCreditos() {
        activarTab(tabCreditos);
        contentArea.getChildren().clear();

        ScrollPane scroll = buildScrollPane();
        VBox inner = new VBox(24);
        inner.setPadding(new Insets(28, 32, 32, 32));

        try {
            ReporteCreditosDto r = reporteService.reporteCreditos();

            inner.getChildren().add(buildResultHeader("Créditos pendientes de clientes",
                    () -> exportarCreditosExcel(r)));

            HBox kpi = new HBox(16);
            kpi.getChildren().addAll(
                crearKpiCard("fas-users",       "#5A6ACF", "Clientes con deuda",    String.valueOf(r.clientesConDeuda())),
                crearKpiCard("fas-dollar-sign", "#DC2626", "Deuda total pendiente",  FMT.format(r.totalDeudaPendiente()))
            );
            inner.getChildren().add(kpi);

            if (r.clientesConDeuda() == 0) {
                inner.getChildren().add(buildBanner("fas-check-circle",
                        "No hay créditos pendientes. Todos los clientes están al día.",
                        "#DCFCE7", "#15803D"));
            } else {
                Label lbl = new Label("Detalle por cliente");
                lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
                inner.getChildren().addAll(lbl, buildTablaCreditos(r.clientes()));
            }

        } catch (Exception ex) {
            inner.getChildren().add(buildError(ex.getMessage()));
        }

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    private TableView<ClienteCreditoDto> buildTablaCreditos(List<ClienteCreditoDto> clientes) {
        TableView<ClienteCreditoDto> tabla = buildTableView();
        TableColumn<ClienteCreditoDto, String> colNombre     = col("Cliente",    d -> d.nombre());
        TableColumn<ClienteCreditoDto, String> colCedula     = col("Cédula",     d -> d.cedula() != null ? d.cedula() : "—", 110);
        TableColumn<ClienteCreditoDto, String> colLimite     = col("Límite",     d -> FMT.format(d.limiteCredito()), 120);
        TableColumn<ClienteCreditoDto, String> colUsado      = col("Utilizado",  d -> FMT.format(d.saldoUtilizado()), 120);
        TableColumn<ClienteCreditoDto, String> colDisponible = col("Disponible", d -> FMT.format(d.saldoDisponible()), 120);

        colUsado.setCellFactory(tc -> boldColorCell("#DC2626"));
        colDisponible.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                ClienteCreditoDto item = getTableRow() != null ? getTableRow().getItem() : null;
                setText(s);
                boolean agotado = item != null && item.saldoDisponible().compareTo(BigDecimal.ZERO) <= 0;
                setStyle("-fx-font-weight:700;-fx-text-fill:" + (agotado ? "#DC2626" : "#15803D") + ";");
            }
        });

        tabla.getColumns().addAll(colNombre, colCedula, colLimite, colUsado, colDisponible);
        tabla.setItems(FXCollections.observableArrayList(clientes));
        return tabla;
    }

    // ─────────────────────────────────────────────────────────────
    // Tab 5 — Compras por Proveedor
    // ─────────────────────────────────────────────────────────────

    private void mostrarCompras() {
        activarTab(tabCompras);
        contentArea.getChildren().clear();

        ScrollPane scroll = buildScrollPane();
        VBox inner = new VBox(24);
        inner.setPadding(new Insets(28, 32, 32, 32));

        VBox selectorCard = buildSelectorCard("fas-truck", "#7C3AED",
                "Reporte de compras por proveedor",
                "Historial de compras y productos adquiridos a cada proveedor");

        ComboBox<Proveedor> comboProveedor = new ComboBox<>();
        comboProveedor.getStyleClass().add("inventario-field");
        comboProveedor.setMaxWidth(Double.MAX_VALUE);
        comboProveedor.setPromptText("Selecciona un proveedor...");
        comboProveedor.setItems(FXCollections.observableArrayList(todosProveedores));
        comboProveedor.setConverter(new StringConverter<>() {
            @Override public String toString(Proveedor p) {
                if (p == null) return "";
                String nit = p.getNit() != null && !p.getNit().isBlank() ? "  ·  NIT " + p.getNit() : "";
                return p.getNombre() + nit;
            }
            @Override public Proveedor fromString(String s) { return null; }
        });

        selectorBody(selectorCard).getChildren().add(comboProveedor);

        VBox resultado = new VBox(24);

        comboProveedor.setOnAction(e -> {
            Proveedor sel = comboProveedor.getValue();
            if (sel == null) return;
            resultado.getChildren().clear();
            try {
                ReporteComprasDto r = reporteService.reporteComprasPorProveedor(sel.getId());
                resultado.getChildren().addAll(buildResultadoCompras(r));
            } catch (Exception ex) {
                resultado.getChildren().add(buildError(ex.getMessage()));
            }
            animarEntrada(resultado, 0);
        });

        if (!todosProveedores.isEmpty()) comboProveedor.setValue(todosProveedores.get(0));

        inner.getChildren().addAll(selectorCard, resultado);

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    private List<Node> buildResultadoCompras(ReporteComprasDto r) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(buildResultHeader("Compras — " + r.nombreProveedor(),
                () -> exportarComprasExcel(r)));

        HBox kpi = new HBox(16);
        kpi.getChildren().addAll(
            crearKpiCard("fas-file-invoice", "#7C3AED", "Órdenes de compra", String.valueOf(r.totalCompras())),
            crearKpiCard("fas-dollar-sign",  "#D97706", "Total invertido",    FMT.format(r.totalInvertido()))
        );
        nodes.add(kpi);

        if (!r.productos().isEmpty()) {
            Label lbl = new Label("Productos adquiridos");
            lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
            nodes.add(lbl);

            TableView<ProductoCompradoDto> tabla = buildTableView();
            TableColumn<ProductoCompradoDto, String> colNombre   = col("Producto",        d -> d.nombre());
            TableColumn<ProductoCompradoDto, String> colCodigo   = col("Código",           d -> d.codigoBarras() != null ? d.codigoBarras() : "—", 120);
            TableColumn<ProductoCompradoDto, String> colCantidad = col("Unidades",         d -> String.valueOf(d.cantidadTotal()), 90);
            TableColumn<ProductoCompradoDto, String> colTotal    = col("Total invertido",  d -> FMT.format(d.totalInvertido()), 140);
            colCantidad.setCellFactory(tc -> centeredCell());
            colTotal.setCellFactory(tc -> boldColorCell("#D97706"));
            tabla.getColumns().addAll(colNombre, colCodigo, colCantidad, colTotal);
            tabla.setItems(FXCollections.observableArrayList(r.productos()));
            nodes.add(tabla);
        } else {
            nodes.add(buildBanner("fas-info-circle",
                    "Este proveedor no tiene compras registradas.", "#F1F5F9", "#64748B"));
        }

        return nodes;
    }

    // ─────────────────────────────────────────────────────────────
    // Excel — Exportación
    // ─────────────────────────────────────────────────────────────

    private void exportarVentasExcel(ReporteVentasDto r) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = estiloEncabezado(wb);
            CellStyle moneda  = estiloMoneda(wb);
            CellStyle normal  = estiloNormal(wb);

            // Hoja 1: Resumen
            XSSFSheet resumen = wb.createSheet("Resumen");
            String[][] datos = {
                { "Caja ID",           String.valueOf(r.cajaId()) },
                { "Apertura",          r.fechaApertura() != null ? r.fechaApertura().format(DFT) : "—" },
                { "Cierre",            r.fechaCierre() != null ? r.fechaCierre().format(DFT) : "En curso" },
                { "Total ventas",      String.valueOf(r.totalVentas()) },
                { "Completadas",       String.valueOf(r.ventasCompletadas()) },
                { "Anuladas",          String.valueOf(r.ventasAnuladas()) },
                { "Total efectivo",    FMT.format(r.totalEfectivo()) },
                { "Total transferencia", FMT.format(r.totalTransferencia()) },
                { "Total crédito",     FMT.format(r.totalCredito()) },
                { "Total general",     FMT.format(r.totalGeneral()) }
            };
            Row h = resumen.createRow(0);
            setCellHeader(h, 0, "Concepto", header);
            setCellHeader(h, 1, "Valor", header);
            for (int i = 0; i < datos.length; i++) {
                Row row = resumen.createRow(i + 1);
                row.createCell(0).setCellValue(datos[i][0]);
                row.createCell(1).setCellValue(datos[i][1]);
                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
            }
            autoSize(resumen, 2);

            // Hoja 2: Productos vendidos
            XSSFSheet productos = wb.createSheet("Productos vendidos");
            Row hp = productos.createRow(0);
            setCellHeader(hp, 0, "Producto", header);
            setCellHeader(hp, 1, "Código", header);
            setCellHeader(hp, 2, "Unidades", header);
            setCellHeader(hp, 3, "Total generado", header);
            int fila = 1;
            for (ProductoVendidoDto p : r.topProductos()) {
                Row row = productos.createRow(fila++);
                row.createCell(0).setCellValue(p.nombre());
                row.createCell(1).setCellValue(p.codigoBarras() != null ? p.codigoBarras() : "");
                row.createCell(2).setCellValue(p.cantidadVendida());
                row.createCell(3).setCellValue(p.totalGenerado().doubleValue());
                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
                row.getCell(2).setCellStyle(normal);
                row.getCell(3).setCellStyle(moneda);
            }
            autoSize(productos, 4);

            guardarWorkbook(wb, "reporte_ventas_caja_" + r.cajaId() + ".xlsx");
        } catch (Exception e) {
            mostrarAlertaError("No se pudo exportar el archivo: " + e.getMessage());
        }
    }

    private void exportarRentabilidadMensualExcel(ReporteRentabilidadDto r) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = estiloEncabezado(wb);
            CellStyle normal = estiloNormal(wb);

            XSSFSheet sheet = wb.createSheet("Rentabilidad Mensual");
            Row h = sheet.createRow(0);
            setCellHeader(h, 0, "Año", header);
            setCellHeader(h, 1, "Mes", header);
            setCellHeader(h, 2, "Total invertido", header);
            setCellHeader(h, 3, "Total vendido", header);
            setCellHeader(h, 4, "Ganancia bruta", header);
            setCellHeader(h, 5, "Margen %", header);
            setCellHeader(h, 6, "Estado", header);

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(r.anio());
            row.createCell(1).setCellValue(NOMBRES_MESES[r.mes()]);
            row.createCell(2).setCellValue(r.totalInvertido().doubleValue());
            row.createCell(3).setCellValue(r.totalVendido().doubleValue());
            row.createCell(4).setCellValue(r.gananciaBruta().doubleValue());
            row.createCell(5).setCellValue(r.margenPorcentaje() != null ? r.margenPorcentaje().doubleValue() : 0);
            row.createCell(6).setCellValue(r.tuvoPerdida() ? "Pérdida" : "Ganancia");
            for (int i = 0; i <= 6; i++) row.getCell(i).setCellStyle(normal);
            autoSize(sheet, 7);

            guardarWorkbook(wb, "rentabilidad_" + NOMBRES_MESES[r.mes()].toLowerCase() + "_" + r.anio() + ".xlsx");
        } catch (Exception e) {
            mostrarAlertaError("No se pudo exportar: " + e.getMessage());
        }
    }

    private void exportarRentabilidadAnualExcel(ReporteRentabilidadAnualDto r) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = estiloEncabezado(wb);
            CellStyle moneda = estiloMoneda(wb);
            CellStyle normal = estiloNormal(wb);

            // Hoja 1: Resumen anual
            XSSFSheet resumen = wb.createSheet("Resumen " + r.anio());
            Row h = resumen.createRow(0);
            setCellHeader(h, 0, "Concepto", header); setCellHeader(h, 1, "Valor", header);
            String[][] datos = {
                { "Año",             String.valueOf(r.anio()) },
                { "Total invertido", FMT.format(r.totalInvertido()) },
                { "Total vendido",   FMT.format(r.totalVendido()) },
                { "Ganancia bruta",  FMT.format(r.gananciaBruta()) },
                { "Margen %",        r.margenPorcentaje() != null ? r.margenPorcentaje().toPlainString() + "%" : "—" },
                { "Estado",          r.tuvoPerdida() ? "Pérdida" : "Ganancia" }
            };
            for (int i = 0; i < datos.length; i++) {
                Row row = resumen.createRow(i + 1);
                row.createCell(0).setCellValue(datos[i][0]); row.getCell(0).setCellStyle(normal);
                row.createCell(1).setCellValue(datos[i][1]); row.getCell(1).setCellStyle(normal);
            }
            autoSize(resumen, 2);

            // Hoja 2: Desglose mensual
            XSSFSheet meses = wb.createSheet("Desglose mensual");
            Row hm = meses.createRow(0);
            setCellHeader(hm, 0, "Mes", header);
            setCellHeader(hm, 1, "Total invertido", header);
            setCellHeader(hm, 2, "Total vendido", header);
            setCellHeader(hm, 3, "Ganancia bruta", header);
            setCellHeader(hm, 4, "Estado", header);
            int fila = 1;
            for (RentabilidadMensualDto m : r.meses()) {
                Row row = meses.createRow(fila++);
                row.createCell(0).setCellValue(m.nombreMes());
                row.createCell(1).setCellValue(m.totalInvertido().doubleValue());
                row.createCell(2).setCellValue(m.totalVendido().doubleValue());
                row.createCell(3).setCellValue(m.gananciaBruta().doubleValue());
                row.createCell(4).setCellValue(m.tuvoPerdida() ? "Pérdida" : "Ganancia");
                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(moneda);
                row.getCell(2).setCellStyle(moneda);
                row.getCell(3).setCellStyle(moneda);
                row.getCell(4).setCellStyle(normal);
            }
            autoSize(meses, 5);

            guardarWorkbook(wb, "rentabilidad_anual_" + r.anio() + ".xlsx");
        } catch (Exception e) {
            mostrarAlertaError("No se pudo exportar: " + e.getMessage());
        }
    }

    private void exportarInventarioExcel(ReporteInventarioDto r) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = estiloEncabezado(wb);
            CellStyle moneda = estiloMoneda(wb);
            CellStyle normal = estiloNormal(wb);

            String[] cols = { "Producto", "Código", "Proveedor", "Stock", "P. Compra", "P. Venta" };

            XSSFSheet agotados = wb.createSheet("Agotados");
            Row ha = agotados.createRow(0);
            for (int i = 0; i < cols.length; i++) setCellHeader(ha, i, cols[i], header);
            int fila = 1;
            for (ProductoStockDto p : r.agotados()) {
                Row row = agotados.createRow(fila++);
                llenarFilaStock(row, p, normal, moneda);
            }
            autoSize(agotados, cols.length);

            XSSFSheet bajoStock = wb.createSheet("Bajo Stock");
            Row hb = bajoStock.createRow(0);
            for (int i = 0; i < cols.length; i++) setCellHeader(hb, i, cols[i], header);
            fila = 1;
            for (ProductoStockDto p : r.bajoStock()) {
                Row row = bajoStock.createRow(fila++);
                llenarFilaStock(row, p, normal, moneda);
            }
            autoSize(bajoStock, cols.length);

            guardarWorkbook(wb, "reporte_inventario_stock.xlsx");
        } catch (Exception e) {
            mostrarAlertaError("No se pudo exportar: " + e.getMessage());
        }
    }

    private void llenarFilaStock(Row row, ProductoStockDto p, CellStyle normal, CellStyle moneda) {
        row.createCell(0).setCellValue(p.nombre());
        row.createCell(1).setCellValue(p.codigoBarras() != null ? p.codigoBarras() : "");
        row.createCell(2).setCellValue(p.proveedor());
        row.createCell(3).setCellValue(p.stock());
        row.createCell(4).setCellValue(p.precioCompra().doubleValue());
        row.createCell(5).setCellValue(p.precioVenta().doubleValue());
        row.getCell(0).setCellStyle(normal);
        row.getCell(1).setCellStyle(normal);
        row.getCell(2).setCellStyle(normal);
        row.getCell(3).setCellStyle(normal);
        row.getCell(4).setCellStyle(moneda);
        row.getCell(5).setCellStyle(moneda);
    }

    private void exportarCreditosExcel(ReporteCreditosDto r) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = estiloEncabezado(wb);
            CellStyle moneda = estiloMoneda(wb);
            CellStyle normal = estiloNormal(wb);

            XSSFSheet sheet = wb.createSheet("Créditos pendientes");
            Row h = sheet.createRow(0);
            setCellHeader(h, 0, "Cliente",    header);
            setCellHeader(h, 1, "Cédula",     header);
            setCellHeader(h, 2, "Límite",     header);
            setCellHeader(h, 3, "Utilizado",  header);
            setCellHeader(h, 4, "Disponible", header);

            int fila = 1;
            for (ClienteCreditoDto c : r.clientes()) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(c.nombre());
                row.createCell(1).setCellValue(c.cedula() != null ? c.cedula() : "");
                row.createCell(2).setCellValue(c.limiteCredito().doubleValue());
                row.createCell(3).setCellValue(c.saldoUtilizado().doubleValue());
                row.createCell(4).setCellValue(c.saldoDisponible().doubleValue());
                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
                row.getCell(2).setCellStyle(moneda);
                row.getCell(3).setCellStyle(moneda);
                row.getCell(4).setCellStyle(moneda);
            }
            autoSize(sheet, 5);

            guardarWorkbook(wb, "reporte_creditos_pendientes.xlsx");
        } catch (Exception e) {
            mostrarAlertaError("No se pudo exportar: " + e.getMessage());
        }
    }

    private void exportarComprasExcel(ReporteComprasDto r) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = estiloEncabezado(wb);
            CellStyle moneda = estiloMoneda(wb);
            CellStyle normal = estiloNormal(wb);

            // Hoja 1: Resumen proveedor
            XSSFSheet res = wb.createSheet("Resumen");
            Row h = res.createRow(0);
            setCellHeader(h, 0, "Concepto", header); setCellHeader(h, 1, "Valor", header);
            String[][] datos = {
                { "Proveedor",       r.nombreProveedor() },
                { "Total compras",   String.valueOf(r.totalCompras()) },
                { "Total invertido", FMT.format(r.totalInvertido()) }
            };
            for (int i = 0; i < datos.length; i++) {
                Row row = res.createRow(i + 1);
                row.createCell(0).setCellValue(datos[i][0]); row.getCell(0).setCellStyle(normal);
                row.createCell(1).setCellValue(datos[i][1]); row.getCell(1).setCellStyle(normal);
            }
            autoSize(res, 2);

            // Hoja 2: Productos
            XSSFSheet prods = wb.createSheet("Productos");
            Row hp = prods.createRow(0);
            setCellHeader(hp, 0, "Producto",       header);
            setCellHeader(hp, 1, "Código",          header);
            setCellHeader(hp, 2, "Unidades",        header);
            setCellHeader(hp, 3, "Total invertido", header);
            int fila = 1;
            for (ProductoCompradoDto p : r.productos()) {
                Row row = prods.createRow(fila++);
                row.createCell(0).setCellValue(p.nombre());
                row.createCell(1).setCellValue(p.codigoBarras() != null ? p.codigoBarras() : "");
                row.createCell(2).setCellValue(p.cantidadTotal());
                row.createCell(3).setCellValue(p.totalInvertido().doubleValue());
                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
                row.getCell(2).setCellStyle(normal);
                row.getCell(3).setCellStyle(moneda);
            }
            autoSize(prods, 4);

            guardarWorkbook(wb, "reporte_compras_" + r.nombreProveedor().toLowerCase().replace(" ", "_") + ".xlsx");
        } catch (Exception e) {
            mostrarAlertaError("No se pudo exportar: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers Excel
    // ─────────────────────────────────────────────────────────────

    private void guardarWorkbook(XSSFWorkbook wb, String nombreSugerido) throws Exception {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte Excel");
        fc.setInitialFileName(nombreSugerido);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (.xlsx)", "*.xlsx"));
        File archivo = fc.showSaveDialog(rootStack.getScene().getWindow());
        if (archivo == null) return;
        try (FileOutputStream out = new FileOutputStream(archivo)) {
            wb.write(out);
        }
    }

    private CellStyle estiloEncabezado(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)90, (byte)106, (byte)207}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle estiloNormal(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle estiloMoneda(XSSFWorkbook wb) {
        CellStyle style = estiloNormal(wb);
        DataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("$#,##0.00"));
        return style;
    }

    private void setCellHeader(Row row, int col, String texto, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(texto);
        cell.setCellStyle(style);
        row.setHeight((short) 400);
    }

    private void autoSize(XSSFSheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) sheet.autoSizeColumn(i);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers UI
    // ─────────────────────────────────────────────────────────────

    /** Barra título + botón exportar que encabeza cada sección de resultados */
    private HBox buildResultHeader(String titulo, Runnable onExportar) {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(titulo);
        lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        FontIcon ico = new FontIcon("fas-file-excel");
        ico.setIconSize(13); ico.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnExportar = new Button("Exportar Excel", ico);
        btnExportar.setStyle("-fx-background-color: #15803D; -fx-background-radius: 8px; " +
                "-fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 600; " +
                "-fx-padding: 7 14 7 14; -fx-cursor: hand;");
        btnExportar.setOnAction(e -> onExportar.run());
        btnExportar.setOnMouseEntered(e -> btnExportar.setStyle(btnExportar.getStyle()
                .replace("#15803D", "#166534")));
        btnExportar.setOnMouseExited(e -> btnExportar.setStyle(btnExportar.getStyle()
                .replace("#166534", "#15803D")));

        bar.getChildren().addAll(lbl, btnExportar);
        return bar;
    }

    private VBox crearKpiCard(String icon, String color, String titulo, String valor) {
        VBox card = new VBox(8);
        card.getStyleClass().add("inventario-stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color:" + color + "22;-fx-background-radius:8px;");
        icoCircle.setMinSize(36, 36); icoCircle.setMaxSize(36, 36);
        FontIcon ico = new FontIcon(icon);
        ico.setIconSize(16); ico.setIconColor(Paint.valueOf(color));
        icoCircle.getChildren().add(ico);

        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-font-size:20px;-fx-font-weight:700;-fx-text-fill:#1A1F2E;");
        lblValor.setWrapText(true);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size:12px;-fx-text-fill:#78716C;");

        card.getChildren().addAll(icoCircle, lblValor, lblTitulo);
        return card;
    }

    private VBox buildSelectorCard(String icon, String color, String titulo, String sub) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#FDFCFA;-fx-background-radius:14px;" +
                      "-fx-border-color:rgba(26,31,46,0.10);-fx-border-radius:14px;" +
                      "-fx-border-width:1;-fx-padding:20 24 20 24;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color:" + color + "22;-fx-background-radius:10px;");
        icoCircle.setMinSize(44, 44); icoCircle.setMaxSize(44, 44);
        FontIcon ico = new FontIcon(icon);
        ico.setIconSize(18); ico.setIconColor(Paint.valueOf(color));
        icoCircle.getChildren().add(ico);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblTit = new Label(titulo);
        lblTit.setStyle("-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:#1A1F2E;");
        Label lblSub = new Label(sub);
        lblSub.setStyle("-fx-font-size:12px;-fx-text-fill:#78716C;");
        info.getChildren().addAll(lblTit, lblSub);

        header.getChildren().addAll(icoCircle, info);
        VBox body = new VBox(10);
        card.getChildren().addAll(header, body);
        return card;
    }

    /** Devuelve el VBox body de una selectorCard */
    private VBox selectorBody(VBox card) {
        return (VBox) card.getChildren().get(1);
    }

    private HBox buildBanner(String icon, String msg, String bg, String fg) {
        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:10px;-fx-padding:14 18 14 18;");
        FontIcon ico = new FontIcon(icon);
        ico.setIconSize(16); ico.setIconColor(Paint.valueOf(fg));
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + fg + ";-fx-wrap-text:true;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        banner.getChildren().addAll(ico, lbl);
        return banner;
    }

    private HBox buildError(String msg) {
        return buildBanner("fas-exclamation-circle", "Error: " + msg, "#FEE2E2", "#DC2626");
    }

    private Label crearBadge(String texto, String bg, String fg) {
        Label l = new Label(texto);
        l.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:6px;" +
                   "-fx-text-fill:" + fg + ";-fx-font-size:11px;-fx-font-weight:600;-fx-padding:3 8 3 8;");
        return l;
    }

    private Button buildBotonPrimario(String texto, String icon, String color) {
        FontIcon ico = new FontIcon(icon);
        ico.setIconSize(12); ico.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btn = new Button(texto, ico);
        btn.setStyle("-fx-background-color:" + color + ";-fx-background-radius:8px;" +
                "-fx-text-fill:#FFFFFF;-fx-font-size:13px;" +
                "-fx-padding:8 18 8 18;-fx-cursor:hand;-fx-font-weight:600;");
        return btn;
    }

    private ScrollPane buildScrollPane() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("inventario-root-stack");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private <T> TableView<T> buildTableView() {
        TableView<T> tabla = new TableView<>();
        tabla.getStyleClass().add("inventario-table-card");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin datos disponibles."));
        tabla.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle("-fx-background-color:rgba(90,106,207,0.05);"); });
            row.setOnMouseExited(e -> row.setStyle(""));
            return row;
        });
        return tabla;
    }

    /** Columna simple con extractor lambda, sin ancho fijo */
    private <T> TableColumn<T, String> col(String titulo,
            java.util.function.Function<T, String> extractor) {
        TableColumn<T, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleStringProperty(extractor.apply(d.getValue())));
        return c;
    }

    /** Columna simple con ancho preferido */
    private <T> TableColumn<T, String> col(String titulo,
            java.util.function.Function<T, String> extractor, int prefWidth) {
        TableColumn<T, String> c = col(titulo, extractor);
        c.setPrefWidth(prefWidth);
        return c;
    }

    private <T> TableCell<T, String> centeredCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                setStyle(empty ? "" : "-fx-alignment:CENTER;");
            }
        };
    }

    private <T> TableCell<T, String> boldColorCell(String color) {
        return new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                setStyle(empty ? "" : "-fx-font-weight:700;-fx-text-fill:" + color + ";");
            }
        };
    }

    private String estiloToggle() {
        return "-fx-background-color:#EDE9E2;-fx-background-radius:8px;" +
               "-fx-text-fill:#57534E;-fx-font-size:13px;-fx-padding:7 16 7 16;-fx-cursor:hand;";
    }

    private String estiloToggleActivo() {
        return "-fx-background-color:#ECEEF7;-fx-background-radius:8px;" +
               "-fx-text-fill:#5A6ACF;-fx-font-size:13px;-fx-font-weight:700;-fx-padding:7 16 7 16;-fx-cursor:hand;";
    }

    private void mostrarAlertaError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error de exportación");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
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
}
