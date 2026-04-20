package com.nap.pos.ui.controller;

import com.nap.pos.application.dto.*;
import com.nap.pos.application.service.CajaService;
import com.nap.pos.application.service.GastoService;
import com.nap.pos.application.service.ProveedorService;
import com.nap.pos.application.service.ReporteService;
import com.nap.pos.application.service.VentaService;
import com.nap.pos.domain.model.enums.TipoGasto;
import com.nap.pos.domain.model.Caja;
import com.nap.pos.domain.model.DetalleVenta;
import com.nap.pos.domain.model.Proveedor;
import com.nap.pos.domain.model.Venta;
import com.nap.pos.domain.model.enums.EstadoVenta;
import com.nap.pos.domain.model.enums.MetodoPago;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReportesController {

    private final ReporteService   reporteService;
    private final CajaService      cajaService;
    private final ProveedorService proveedorService;
    private final VentaService     ventaService;
    private final GastoService     gastoService;

    private static final NumberFormat     FMT = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
    private static final DateTimeFormatter DFT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] NOMBRES_MESES = {
        "", "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    // ── Estado ────────────────────────────────────────────────────
    private List<Caja>      todasCajas       = new ArrayList<>();
    private List<Proveedor> todosProveedores = new ArrayList<>();
    private Map<Long, Venta> ventasSesionActual = new HashMap<>();
    private Map<Long, List<Venta>> comprasCreditoPorCliente = new HashMap<>();
    private List<FiltroClienteCredito> filtrosCreditoDisponibles = new ArrayList<>();
    private boolean ajustandoFiltroCreditos = false;

    // ── Refs UI ───────────────────────────────────────────────────
    private StackPane rootStack;
    private VBox      contentArea;
    private Button    tabVentas;
    private Button    tabRentabilidad;
    private Button    tabInventario;
    private Button    tabCreditos;
    private Button    tabCompras;
    private Button    tabGastos;

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

        tabVentas       = crearTab("fas-receipt",              "Ventas",        true,  () -> { animarClickTab(tabVentas);       mostrarVentas();        });
        tabRentabilidad = crearTab("fas-chart-line",           "Rentabilidad",  false, () -> { animarClickTab(tabRentabilidad); mostrarRentabilidad();   });
        tabInventario   = crearTab("fas-boxes",                "Inventario",    false, () -> { animarClickTab(tabInventario);   mostrarInventario();     });
        tabCreditos     = crearTab("fas-credit-card",          "Créditos",      false, () -> { animarClickTab(tabCreditos);     mostrarCreditos();       });
        tabCompras      = crearTab("fas-truck",                "Compras",       false, () -> { animarClickTab(tabCompras);      mostrarCompras();        });
        tabGastos       = crearTab("fas-file-invoice-dollar",  "Gastos",        false, () -> { animarClickTab(tabGastos);       mostrarGastos();         });

        bar.getChildren().addAll(tabVentas, tabRentabilidad, tabInventario, tabCreditos, tabCompras, tabGastos);
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
        List.of(tabVentas, tabRentabilidad, tabInventario, tabCreditos, tabCompras, tabGastos).forEach(t -> {
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
                cargarVentasSesionActual(sel.getId());
                resultado.getChildren().addAll(buildResultadoVentas(r, sel));
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

    private void cargarVentasSesionActual(Long cajaId) {
        try {
            ventasSesionActual = ventaService.findByCajaId(cajaId).stream()
                    .filter(v -> v.getId() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            Venta::getId,
                            v -> v,
                            (a, b) -> a,
                            HashMap::new
                    ));
        } catch (Exception e) {
            ventasSesionActual = new HashMap<>();
        }
    }

    private List<Node> buildResultadoVentas(ReporteVentasDto r, Caja cajaSesion) {
        List<Node> nodes = new ArrayList<>();

        nodes.add(buildResultHeader("Ventas — sesión " +
                (r.fechaApertura() != null ? r.fechaApertura().format(DFT) : "—"),
                () -> exportarVentasExcel(r, cajaSesion)));

        HBox kpi1 = new HBox(16);
        kpi1.getChildren().addAll(
            crearKpiCard("fas-receipt",      "#5A6ACF", "Total ventas",  String.valueOf(r.totalVentas())),
            crearKpiCard("fas-check-circle", "#15803D", "Completadas",   String.valueOf(r.ventasCompletadas())),
            crearKpiCard("fas-times-circle", "#DC2626", "Anuladas",      String.valueOf(r.ventasAnuladas()))
        );

        HBox kpi2 = new HBox(16);
        kpi2.getChildren().addAll(
            crearKpiCard("fas-dollar-sign",     "#15803D", "Total cobrado",   FMT.format(r.totalGeneral())),
            crearKpiCard("fas-money-bill-wave", "#D97706", "Contado",          FMT.format(r.totalEfectivo())),
            crearKpiCard("fas-exchange-alt",    "#7C3AED", "Transferencias",   FMT.format(r.totalTransferencia())),
            crearKpiCard("fas-credit-card",     "#DC2626", "A crédito",        FMT.format(r.totalCredito()))
        );

        nodes.add(kpi1);
        nodes.add(kpi2);

        if (cajaSesion != null) {
            BigDecimal montoInicial   = cajaSesion.getMontoInicial() != null ? cajaSesion.getMontoInicial() : BigDecimal.ZERO;
            BigDecimal totalEfectivo  = r.totalEfectivo() != null ? r.totalEfectivo() : BigDecimal.ZERO;
            BigDecimal gastosCaja     = r.totalGastosCaja() != null ? r.totalGastosCaja() : BigDecimal.ZERO;
            BigDecimal esperadoEnCaja = montoInicial.add(totalEfectivo).subtract(gastosCaja);

            HBox kpiCaja = new HBox(16);
            kpiCaja.getChildren().addAll(
                crearKpiCard("fas-coins",                "#5A6ACF", "Monto inicial",       FMT.format(montoInicial)),
                crearKpiCard("fas-file-invoice-dollar",  "#DC2626", "Gastos de caja",      FMT.format(gastosCaja)),
                crearKpiCard("fas-calculator",           "#1A1F2E", "Esperado en caja",    FMT.format(esperadoEnCaja))
            );

            if (cajaSesion.getMontoFinal() != null) {
                BigDecimal desajuste = cajaSesion.getMontoFinal().subtract(esperadoEnCaja);
                String colorDesajuste = desajuste.compareTo(BigDecimal.ZERO) < 0
                        ? "#DC2626"
                        : (desajuste.compareTo(BigDecimal.ZERO) > 0 ? "#D97706" : "#15803D");
                String signo = desajuste.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";

                kpiCaja.getChildren().addAll(
                    crearKpiCard("fas-cash-register", "#D97706", "Monto final contado", FMT.format(cajaSesion.getMontoFinal())),
                    crearKpiCard("fas-balance-scale", colorDesajuste, "Desajuste", signo + FMT.format(desajuste))
                );
            } else {
                kpiCaja.getChildren().add(
                    crearKpiCard("fas-cash-register", "#64748B", "Monto final contado", "En curso")
                );
            }

            nodes.add(kpiCaja);
        }

        if (!r.detalleVentas().isEmpty()) {
            Label lblDet = new Label("Detalle de ventas");
            lblDet.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
            nodes.add(lblDet);

            TableView<VentaDetalleDto> tablaDetalle = buildTableView();
            TableColumn<VentaDetalleDto, String> colComp = col("Comp.",
                d -> d.numeroComprobante() != null ? String.valueOf(d.numeroComprobante()) : "—", 85);
            TableColumn<VentaDetalleDto, String> colFechaHora = col("Fecha y hora",
                d -> d.fecha() != null ? d.fecha().format(DFT) : "—", 145);
            TableColumn<VentaDetalleDto, String> colCliente = col("Cliente",
                d -> d.clienteNombre() != null && !d.clienteNombre().isBlank() ? d.clienteNombre() : "Venta directa", 180);
            TableColumn<VentaDetalleDto, String> colCedula = col("Cédula",
                d -> d.clienteCedula() != null && !d.clienteCedula().isBlank() ? d.clienteCedula() : "—", 120);
            TableColumn<VentaDetalleDto, String> colMetodo = col("Método",
                d -> textoMetodoPago(d.metodoPago()), 110);
            TableColumn<VentaDetalleDto, String> colEstado = col("Estado",
                d -> textoEstadoVenta(d.estado()), 95);
            TableColumn<VentaDetalleDto, String> colTotal = col("Total",
                d -> FMT.format(d.total() != null ? d.total() : BigDecimal.ZERO), 120);

            colComp.setCellFactory(tc -> centeredCell());
            colMetodo.setCellFactory(tc -> centeredCell());
            colEstado.setCellFactory(tc -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setGraphic(null); setText(null); return; }
                    boolean anulada = "Anulada".equals(s);
                    setGraphic(crearBadge(s,
                            anulada ? "#FEE2E2" : "#DCFCE7",
                            anulada ? "#DC2626" : "#15803D"));
                    setText(null);
                }
            });
            colTotal.setCellFactory(tc -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setText(null); setStyle(""); return; }
                    VentaDetalleDto item = getTableRow() != null ? getTableRow().getItem() : null;
                    boolean anulada = item != null && EstadoVenta.ANULADA.equals(item.estado());
                    setText(s);
                    setStyle("-fx-font-weight:700;-fx-text-fill:" + (anulada ? "#DC2626" : "#15803D") + ";");
                }
            });

            tablaDetalle.setRowFactory(tv -> {
                TableRow<VentaDetalleDto> row = new TableRow<>();
                row.setOnMouseEntered(e -> {
                    if (!row.isEmpty()) row.setStyle("-fx-background-color:rgba(90,106,207,0.08);-fx-cursor:hand;");
                });
                row.setOnMouseExited(e -> row.setStyle(""));
                row.setOnMouseClicked(e -> {
                    if (row.isEmpty() || row.getItem() == null) return;
                    VentaDetalleDto item = row.getItem();
                    if (item.ventaId() == null) return;
                    Venta venta = ventasSesionActual.get(item.ventaId());
                    if (venta != null) abrirModalDetalleVenta(venta);
                });
                return row;
            });

            tablaDetalle.getColumns().addAll(colComp, colFechaHora, colCliente, colCedula, colMetodo, colEstado, colTotal);
            tablaDetalle.setItems(FXCollections.observableArrayList(r.detalleVentas()));
            nodes.add(tablaDetalle);
        }

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

        Runnable cargarResultado = () -> {
            resultado.getChildren().clear();
            Integer anioSel = comboAnio.getValue();
            if (anioSel == null) return;

            try {
                if (modoMensual[0]) {
                    Integer mesSel = comboMes.getValue();
                    if (mesSel == null) return;

                    ReporteRentabilidadDto mensual = reporteService.reporteRentabilidad(anioSel, mesSel);
                    ReporteRentabilidadAnualDto anual = reporteService.reporteRentabilidadAnual(anioSel);
                    resultado.getChildren().addAll(buildResultadoRentMensual(mensual, anual));
                } else {
                    ReporteRentabilidadAnualDto anual = reporteService.reporteRentabilidadAnual(anioSel);
                    resultado.getChildren().addAll(buildResultadoRentAnual(anual));
                }
            } catch (Exception ex) {
                resultado.getChildren().add(buildError(ex.getMessage()));
            }
            animarEntrada(resultado, 0);
        };

        btnMensual.setOnAction(e -> {
            modoMensual[0] = true;
            btnMensual.setStyle(estiloToggleActivo()); btnAnual.setStyle(estiloToggle());
            comboMes.setVisible(true); comboMes.setManaged(true);
            cargarResultado.run();
        });
        btnAnual.setOnAction(e -> {
            modoMensual[0] = false;
            btnAnual.setStyle(estiloToggleActivo()); btnMensual.setStyle(estiloToggle());
            comboMes.setVisible(false); comboMes.setManaged(false);
            cargarResultado.run();
        });

        btnGenerar.setOnAction(e -> cargarResultado.run());
        comboAnio.setOnAction(e -> cargarResultado.run());
        comboMes.setOnAction(e -> { if (modoMensual[0]) cargarResultado.run(); });

        inner.getChildren().addAll(selectorCard, resultado);
        cargarResultado.run();

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    private List<Node> buildResultadoRentMensual(ReporteRentabilidadDto r, ReporteRentabilidadAnualDto anual) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(buildResultHeader(
                "Rentabilidad — " + NOMBRES_MESES[r.mes()] + " " + r.anio(),
                () -> exportarRentabilidadMensualExcel(r)));

        String color = r.tuvoPerdida() ? "#DC2626" : "#15803D";
        String colorAjuste = r.ajusteCaja().compareTo(BigDecimal.ZERO) < 0
            ? "#DC2626"
            : (r.ajusteCaja().compareTo(BigDecimal.ZERO) > 0 ? "#D97706" : "#64748B");
        String margen = r.margenPorcentaje() != null ? r.margenPorcentaje().toPlainString() + "%" : "—";

        HBox kpi = new HBox(16);
        kpi.getChildren().addAll(
            crearKpiCard("fas-shopping-cart",           "#D97706", "Total invertido", FMT.format(r.totalInvertido())),
            crearKpiCard("fas-dollar-sign",             "#5A6ACF", "Total vendido",   FMT.format(r.totalVendido())),
            crearKpiCard("fas-file-invoice-dollar",     "#DC2626", "Gastos",          FMT.format(r.totalGastos())),
            crearKpiCard("fas-balance-scale",           colorAjuste, "Desajuste caja", FMT.format(r.ajusteCaja())),
            crearKpiCard("fas-chart-line",              color,     "Ganancia bruta",  FMT.format(r.gananciaBruta())),
            crearKpiCard("fas-percentage",              color,     "Margen",          margen)
        );
        nodes.add(kpi);

        if (r.tuvoPerdida())
            nodes.add(buildBanner("fas-exclamation-triangle",
                    "Este período tuvo pérdida. Las compras superaron los ingresos por ventas.",
                    "#FEF3C7", "#D97706"));

        VBox chartMes = buildChartCard(
                "COMPARACIÓN DEL MES SELECCIONADO",
                "fas-balance-scale",
                buildChartRentMensualComparativo(r));

        if (anual != null && anual.meses() != null && !anual.meses().isEmpty()) {
            VBox chartAnual = buildChartCard(
                    "TENDENCIA MENSUAL DEL AÑO " + r.anio(),
                    "fas-chart-bar",
                    buildChartRentTendenciaAnual(anual));
            nodes.add(buildChartsWrapRow(chartMes, chartAnual));
        } else {
            nodes.add(chartMes);
        }

        return nodes;
    }

    private List<Node> buildResultadoRentAnual(ReporteRentabilidadAnualDto r) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(buildResultHeader(
                "Rentabilidad anual — " + r.anio(),
                () -> exportarRentabilidadAnualExcel(r)));

        String color = r.tuvoPerdida() ? "#DC2626" : "#15803D";
        String colorAjuste = r.ajusteCajaTotal().compareTo(BigDecimal.ZERO) < 0
            ? "#DC2626"
            : (r.ajusteCajaTotal().compareTo(BigDecimal.ZERO) > 0 ? "#D97706" : "#64748B");
        String margen = r.margenPorcentaje() != null ? r.margenPorcentaje().toPlainString() + "%" : "—";

        HBox kpi = new HBox(16);
        kpi.getChildren().addAll(
            crearKpiCard("fas-shopping-cart",       "#D97706", "Total invertido", FMT.format(r.totalInvertido())),
            crearKpiCard("fas-dollar-sign",         "#5A6ACF", "Total vendido",   FMT.format(r.totalVendido())),
            crearKpiCard("fas-file-invoice-dollar", "#DC2626", "Total gastos",    FMT.format(r.totalGastosAnual())),
            crearKpiCard("fas-balance-scale",       colorAjuste, "Desajuste caja", FMT.format(r.ajusteCajaTotal())),
            crearKpiCard("fas-chart-line",          color,     "Ganancia bruta",  FMT.format(r.gananciaBruta())),
            crearKpiCard("fas-percentage",          color,     "Margen anual",    margen)
        );
        nodes.add(kpi);

        VBox chartTendencia = buildChartCard(
            "EVOLUCIÓN MENSUAL: VENDIDO VS INVERTIDO",
            "fas-chart-bar",
            buildChartRentTendenciaAnual(r));

        VBox chartGanancia = buildChartCard(
            "GANANCIA / PÉRDIDA POR MES",
            "fas-chart-line",
            buildChartRentGananciaMensual(r));

        nodes.add(buildChartsWrapRow(chartTendencia, chartGanancia));

        Label lbl = new Label("Desglose mensual");
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        nodes.add(lbl);

        TableView<RentabilidadMensualDto> tabla = buildTableView();
        TableColumn<RentabilidadMensualDto, String> colMes       = col("Mes",       d -> d.nombreMes(), 100);
        TableColumn<RentabilidadMensualDto, String> colInvertido = col("Invertido", d -> FMT.format(d.totalInvertido()), 120);
        TableColumn<RentabilidadMensualDto, String> colVendido   = col("Vendido",   d -> FMT.format(d.totalVendido()), 120);
        TableColumn<RentabilidadMensualDto, String> colGastos    = col("Gastos",    d -> FMT.format(d.totalGastos()), 120);
        TableColumn<RentabilidadMensualDto, String> colAjuste    = col("Desajuste", d -> FMT.format(d.ajusteCaja()), 120);
        TableColumn<RentabilidadMensualDto, String> colGanancia  = col("Ganancia",  d -> FMT.format(d.gananciaBruta()), 120);
        TableColumn<RentabilidadMensualDto, String> colEstado    = col("Estado",    d -> d.tuvoPerdida() ? "Pérdida" : "Ganancia", 90);

        colAjuste.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                RentabilidadMensualDto item = getTableRow() != null ? getTableRow().getItem() : null;
                BigDecimal ajuste = item != null && item.ajusteCaja() != null ? item.ajusteCaja() : BigDecimal.ZERO;
                String colorAjuste = ajuste.compareTo(BigDecimal.ZERO) < 0
                        ? "#DC2626"
                        : (ajuste.compareTo(BigDecimal.ZERO) > 0 ? "#D97706" : "#64748B");
                setText(s);
                setStyle("-fx-font-weight:700;-fx-text-fill:" + colorAjuste + ";");
            }
        });

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

        tabla.getColumns().addAll(colMes, colInvertido, colVendido, colGastos, colAjuste, colGanancia, colEstado);
        tabla.setItems(FXCollections.observableArrayList(r.meses()));
        nodes.add(tabla);
        return nodes;
    }

    private VBox buildChartCard(String titulo, String iconLiteral, Node chart) {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setFillWidth(true);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        FontIcon ico = new FontIcon(iconLiteral);
        ico.setIconSize(14);
        ico.setIconColor(Paint.valueOf("#94A3B8"));
        header.getChildren().addAll(lbl, ico);

        if (chart instanceof Region region) {
            region.setMinWidth(0);
            region.setMaxWidth(Double.MAX_VALUE);
        }

        VBox.setVgrow(chart, Priority.ALWAYS);
        card.getChildren().addAll(header, chart);
        return card;
    }

    private FlowPane buildChartsWrapRow(VBox... cards) {
        FlowPane wrap = new FlowPane();
        wrap.setHgap(16);
        wrap.setVgap(16);
        wrap.setPrefWrapLength(1140);
        wrap.setMaxWidth(Double.MAX_VALUE);

        for (VBox card : cards) {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            card.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                double ancho = wrap.getWidth();
                if (ancho <= 0) return 540d;

                int columnas = ancho >= 980 ? Math.min(cards.length, 2) : 1;
                double espacio = (columnas - 1) * wrap.getHgap();
                return Math.max(0d, (ancho - espacio) / columnas);
            }, wrap.widthProperty()));

            FlowPane.setMargin(card, new Insets(0));
            wrap.getChildren().add(card);
        }
        return wrap;
    }

    private void configurarAlturaChartResponsive(BarChart<String, Number> chart) {
        chart.setMinHeight(250);
        chart.setMaxHeight(Double.MAX_VALUE);

        if (contentArea == null) {
            chart.setPrefHeight(320);
            return;
        }

        chart.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> {
            double altoContenedor = contentArea.getHeight();
            if (altoContenedor <= 0) return 320d;

            double objetivo = altoContenedor * 0.40d;
            return Math.max(250d, Math.min(440d, objetivo));
        }, contentArea.heightProperty()));
    }

    private BarChart<String, Number> buildChartRentMensualComparativo(ReporteRentabilidadDto r) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        xAxis.setTickLabelRotation(0);
        xAxis.setStyle("-fx-tick-label-fill: #57534E; -fx-font-size: 10px; -fx-font-weight: 600;");
        xAxis.setCategories(FXCollections.observableArrayList("Invertido", "Vendido", "Ganancia"));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setStyle("-fx-tick-label-fill: #57534E; -fx-font-size: 10px;");
        yAxis.setTickLabelFormatter(formateadorEjeMonedaCorto());
        yAxis.setLabel("");
        configurarEscalaY(yAxis,
            List.of(r.totalInvertido().doubleValue(), r.totalVendido().doubleValue(), r.gananciaBruta().doubleValue()),
            true);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(true);
        chart.setLegendVisible(false);
        chart.getStyleClass().add("inventario-bar-chart");
        chart.getStyleClass().add("rentabilidad-grid-chart");
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setCategoryGap(14);
        chart.setBarGap(2);
        configurarAlturaChartResponsive(chart);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.getData().add(new XYChart.Data<>("Invertido", r.totalInvertido().doubleValue()));
        serie.getData().add(new XYChart.Data<>("Vendido", r.totalVendido().doubleValue()));
        serie.getData().add(new XYChart.Data<>("Ganancia", r.gananciaBruta().doubleValue()));

        chart.getData().add(serie);
        aplicarColoresComparativoMensual(serie);
        instalarTooltipsSerie(serie, data -> data.getXValue() + ": " + FMT.format(BigDecimal.valueOf(data.getYValue().doubleValue())));
        instalarEtiquetasValorBarras(serie, true);
        return chart;
    }

    private BarChart<String, Number> buildChartRentTendenciaAnual(ReporteRentabilidadAnualDto r) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        xAxis.setTickLabelRotation(0);
        xAxis.setStyle("-fx-tick-label-fill: #57534E; -fx-font-size: 10px; -fx-font-weight: 600;");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setStyle("-fx-tick-label-fill: #57534E; -fx-font-size: 10px;");
        yAxis.setTickLabelFormatter(formateadorEjeMonedaCorto());
        yAxis.setLabel("");

        List<RentabilidadMensualDto> meses = filtrarMesesConMovimiento(r.meses() != null ? r.meses() : List.of());
        List<Double> valoresEje = new ArrayList<>();
        for (RentabilidadMensualDto mes : meses) {
            valoresEje.add(mes.totalVendido().doubleValue());
            valoresEje.add(mes.totalInvertido().doubleValue());
        }
        configurarEscalaY(yAxis, valoresEje, true);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(true);
        chart.setLegendVisible(false);
        chart.getStyleClass().add("inventario-bar-chart");
        chart.getStyleClass().add("rentabilidad-grid-chart");
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setCategoryGap(12);
        chart.setBarGap(2);
        configurarAlturaChartResponsive(chart);

        XYChart.Series<String, Number> serieVendido = new XYChart.Series<>();
        serieVendido.setName("Vendido");

        XYChart.Series<String, Number> serieInvertido = new XYChart.Series<>();
        serieInvertido.setName("Invertido");

        List<String> categorias = new ArrayList<>();
        for (RentabilidadMensualDto mes : meses) {
            String nombreMes = abreviarMes(mes.nombreMes());
            categorias.add(nombreMes);
            serieVendido.getData().add(new XYChart.Data<>(nombreMes, mes.totalVendido().doubleValue()));
            serieInvertido.getData().add(new XYChart.Data<>(nombreMes, mes.totalInvertido().doubleValue()));
        }

        xAxis.setCategories(FXCollections.observableArrayList(categorias));
        chart.getData().addAll(serieVendido, serieInvertido);
        aplicarColorSerieBarras(serieVendido, "#5A6ACF");
        aplicarColorSerieBarras(serieInvertido, "#D97706");
        instalarTooltipsSerie(serieVendido,
                data -> "Vendido\nMes: " + data.getXValue() + "\nValor: " + FMT.format(BigDecimal.valueOf(data.getYValue().doubleValue())));
        instalarTooltipsSerie(serieInvertido,
                data -> "Invertido\nMes: " + data.getXValue() + "\nValor: " + FMT.format(BigDecimal.valueOf(data.getYValue().doubleValue())));
        instalarEtiquetasValorBarras(serieVendido, false);
        instalarEtiquetasValorBarras(serieInvertido, false);
        return chart;
    }

    private BarChart<String, Number> buildChartRentGananciaMensual(ReporteRentabilidadAnualDto r) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        xAxis.setTickLabelRotation(0);
        xAxis.setStyle("-fx-tick-label-fill: #57534E; -fx-font-size: 10px; -fx-font-weight: 600;");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setStyle("-fx-tick-label-fill: #57534E; -fx-font-size: 10px;");
        yAxis.setTickLabelFormatter(formateadorEjeMonedaCorto());
        yAxis.setLabel("");

        List<RentabilidadMensualDto> meses = filtrarMesesConMovimiento(r.meses() != null ? r.meses() : List.of());
        List<Double> valoresEje = new ArrayList<>();
        for (RentabilidadMensualDto mes : meses) {
            valoresEje.add(mes.gananciaBruta().doubleValue());
        }
        configurarEscalaY(yAxis, valoresEje, true);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(true);
        chart.setLegendVisible(false);
        chart.setCategoryGap(14);
        chart.setBarGap(2);
        chart.getStyleClass().add("inventario-bar-chart");
        chart.getStyleClass().add("rentabilidad-grid-chart");
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        configurarAlturaChartResponsive(chart);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        List<String> categorias = new ArrayList<>();
        for (RentabilidadMensualDto mes : meses) {
            String nombreMes = abreviarMes(mes.nombreMes());
            categorias.add(nombreMes);
            serie.getData().add(new XYChart.Data<>(nombreMes, mes.gananciaBruta().doubleValue()));
        }

        xAxis.setCategories(FXCollections.observableArrayList(categorias));
        chart.getData().add(serie);
        aplicarColoresGananciaMensual(serie);
        instalarTooltipsSerie(serie,
                data -> "Mes: " + data.getXValue() + "\nGanancia: " + FMT.format(BigDecimal.valueOf(data.getYValue().doubleValue())));
        instalarEtiquetasValorBarras(serie, false);
        return chart;
    }

        private List<RentabilidadMensualDto> filtrarMesesConMovimiento(List<RentabilidadMensualDto> meses) {
        if (meses == null || meses.isEmpty()) return List.of();

        List<RentabilidadMensualDto> activos = meses.stream()
            .filter(m -> m.totalVendido().compareTo(BigDecimal.ZERO) != 0
                || m.totalInvertido().compareTo(BigDecimal.ZERO) != 0
                || m.ajusteCaja().compareTo(BigDecimal.ZERO) != 0
                || m.gananciaBruta().compareTo(BigDecimal.ZERO) != 0)
            .toList();

        return activos.isEmpty() ? meses : activos;
        }

    private StringConverter<Number> formateadorEjeMonedaCorto() {
        return new StringConverter<>() {
            @Override
            public String toString(Number number) {
                return formatoMonedaCorta(number);
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        };
    }

    private void configurarEscalaY(NumberAxis axis, List<Double> valores, boolean incluirCero) {
        if (valores == null || valores.isEmpty()) {
            axis.setAutoRanging(false);
            axis.setLowerBound(0);
            axis.setUpperBound(10);
            axis.setTickUnit(2);
            return;
        }

        double min = valores.stream().filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).min().orElse(0d);
        double max = valores.stream().filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(0d);

        if (incluirCero) {
            min = Math.min(min, 0d);
            max = Math.max(max, 0d);
        }

        if (Double.compare(min, max) == 0) {
            double base = Math.max(Math.abs(max), 1d);
            min = incluirCero ? Math.min(0d, min - base * 0.20d) : min - base * 0.20d;
            max = max + base * 0.20d;
        }

        double rango = max - min;
        double padding = Math.max(rango * 0.15d, 1d);
        double lower = incluirCero && min >= 0d ? 0d : min - padding;
        double upper = max + padding;

        if (lower >= upper) {
            upper = lower + 10d;
        }

        axis.setAutoRanging(false);
        axis.setLowerBound(lower);
        axis.setUpperBound(upper);
        axis.setTickUnit(calcularTickUnit(upper - lower));
    }

    private double calcularTickUnit(double rango) {
        if (rango <= 0d) return 1d;
        double objetivo = rango / 5d;
        double magnitud = Math.pow(10d, Math.floor(Math.log10(objetivo)));
        double normalizado = objetivo / magnitud;

        double base;
        if (normalizado <= 1d) base = 1d;
        else if (normalizado <= 2d) base = 2d;
        else if (normalizado <= 5d) base = 5d;
        else base = 10d;

        return base * magnitud;
    }

    private String formatoMonedaCorta(Number number) {
        if (number == null) return "$0";
        double valor = number.doubleValue();
        double abs = Math.abs(valor);
        if (abs >= 1_000_000_000) return String.format("$%.1fB", valor / 1_000_000_000d);
        if (abs >= 1_000_000) return String.format("$%.1fM", valor / 1_000_000d);
        if (abs >= 1_000) return String.format("$%.0fk", valor / 1_000d);
        return String.format("$%.0f", valor);
    }

    private void instalarTooltipsSerie(
            XYChart.Series<String, Number> serie,
            java.util.function.Function<XYChart.Data<String, Number>, String> textBuilder) {
        for (XYChart.Data<String, Number> data : serie.getData()) {
            Runnable instalar = () -> {
                Node nodo = data.getNode();
                if (nodo == null) return;
                Tooltip tt = new Tooltip(textBuilder.apply(data));
                tt.getStyleClass().add("inventario-chart-tooltip");
                tt.setShowDelay(Duration.millis(120));
                Tooltip.install(nodo, tt);
            };

            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) instalar.run();
            });

            instalar.run();
        }
    }

    private void instalarEtiquetasValorBarras(XYChart.Series<String, Number> serie, boolean mostrarCeros) {
        for (XYChart.Data<String, Number> data : serie.getData()) {
            Runnable instalar = () -> {
                Node nodo = data.getNode();
                if (!(nodo instanceof StackPane barra)) return;

                double valor = data.getYValue() != null ? data.getYValue().doubleValue() : 0d;
                if (!mostrarCeros && Math.abs(valor) < 0.0001d) return;

                barra.getChildren().removeIf(child -> child.getUserData() != null && "rent-value-label".equals(child.getUserData()));

                Label lbl = new Label(formatoMonedaCorta(valor));
                lbl.setUserData("rent-value-label");
                lbl.setMouseTransparent(true);
                lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #57534E;");

                StackPane.setAlignment(lbl, valor >= 0 ? Pos.TOP_CENTER : Pos.BOTTOM_CENTER);
                StackPane.setMargin(lbl, valor >= 0 ? new Insets(-18, 0, 0, 0) : new Insets(0, 0, -18, 0));
                barra.getChildren().add(lbl);
            };

            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) instalar.run();
            });

            instalar.run();
        }
    }

    private void aplicarColoresComparativoMensual(XYChart.Series<String, Number> serie) {
        String[] colors = {"#D97706", "#5A6ACF", "#15803D"};
        for (int i = 0; i < serie.getData().size(); i++) {
            XYChart.Data<String, Number> data = serie.getData().get(i);
            String style = "-fx-bar-fill: " + colors[Math.min(i, colors.length - 1)] + ";";
            aplicarEstiloBarra(data, style);
        }
    }

    private void aplicarColoresGananciaMensual(XYChart.Series<String, Number> serie) {
        for (XYChart.Data<String, Number> data : serie.getData()) {
            double valor = data.getYValue() != null ? data.getYValue().doubleValue() : 0;
            String color = valor < 0 ? "#DC2626" : "#15803D";
            aplicarEstiloBarra(data, "-fx-bar-fill: " + color + ";");
        }
    }

    private void aplicarColorSerieBarras(XYChart.Series<String, Number> serie, String color) {
        for (XYChart.Data<String, Number> data : serie.getData()) {
            aplicarEstiloBarra(data, "-fx-bar-fill: " + color + ";");
        }
    }

    private void aplicarEstiloBarra(XYChart.Data<String, Number> data, String style) {
        Node node = data.getNode();
        if (node != null) {
            node.setStyle(style);
            return;
        }
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) newNode.setStyle(style);
        });
    }

    private String abreviarMes(String nombreMes) {
        if (nombreMes == null || nombreMes.isBlank()) return "";
        return nombreMes.length() <= 3 ? nombreMes : nombreMes.substring(0, 3);
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
        }

        List<OrdenCompraProveedorSugerida> ordenes = construirOrdenesCompraSugeridas(r);

        Label lblOc = new Label("Visualización: órdenes de compra sugeridas por proveedor");
        lblOc.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        nodes.add(lblOc);

        nodes.add(buildBanner("fas-eye",
                "Esta vista muestra qué comprar y a qué proveedor. También se exporta en la hoja 'OC sugeridas'.",
                "#EEF2FF", "#4F46E5"));

        if (ordenes.isEmpty()) {
            nodes.add(buildBanner("fas-info-circle",
                    "Aún no hay productos para generar órdenes sugeridas con el umbral actual.",
                    "#F1F5F9", "#64748B"));
        } else {
            int totalProductosSugeridos = ordenes.stream().mapToInt(o -> o.items().size()).sum();
            int totalUnidades = ordenes.stream().mapToInt(OrdenCompraProveedorSugerida::totalUnidades).sum();
            BigDecimal totalEstimado = ordenes.stream()
                    .map(OrdenCompraProveedorSugerida::totalEstimado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            HBox kpiOc = new HBox(16);
            kpiOc.getChildren().addAll(
                crearKpiCard("fas-truck-loading", "#7C3AED", "Proveedores", String.valueOf(ordenes.size())),
                crearKpiCard("fas-box-open", "#D97706", "Productos a comprar", String.valueOf(totalProductosSugeridos)),
                crearKpiCard("fas-layer-group", "#5A6ACF", "Unidades sugeridas", String.valueOf(totalUnidades)),
                crearKpiCard("fas-dollar-sign", "#15803D", "Costo estimado", FMT.format(totalEstimado))
            );
            nodes.add(kpiOc);

            nodes.add(buildBanner("fas-info-circle",
                    "Sugerencia automática basada en agotados y bajo stock. Ajusta cantidades finales antes de comprar.",
                    "#F1F5F9", "#64748B"));

            boolean tieneSinProveedor = ordenes.stream().anyMatch(o -> "Sin proveedor asignado".equals(o.proveedor()));
            if (tieneSinProveedor) {
                nodes.add(buildBanner("fas-exclamation-triangle",
                        "Hay productos sin proveedor asignado. Asigna proveedor para emitir una orden real.",
                        "#FEF3C7", "#D97706"));
            }

            for (OrdenCompraProveedorSugerida orden : ordenes) {
                nodes.add(buildCardOrdenSugeridaProveedor(orden));
            }
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

    private VBox buildCardOrdenSugeridaProveedor(OrdenCompraProveedorSugerida orden) {
        VBox card = new VBox(12);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("Proveedor: " + orden.proveedor());
        titulo.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(titulo, Priority.ALWAYS);

        String icono = "Sin proveedor asignado".equals(orden.proveedor())
                ? "fas-exclamation-triangle"
                : "fas-truck";
        String colorIcono = "Sin proveedor asignado".equals(orden.proveedor())
                ? "#D97706"
                : "#7C3AED";

        FontIcon ico = new FontIcon(icono);
        ico.setIconSize(14);
        ico.setIconColor(Paint.valueOf(colorIcono));
        header.getChildren().addAll(titulo, ico);

        Label sub = new Label(orden.items().size() + " productos · "
                + orden.totalUnidades() + " unidades · "
                + FMT.format(orden.totalEstimado()));
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");

        card.getChildren().addAll(header, sub, buildTablaOrdenSugerida(orden.items()));
        return card;
    }

    private TableView<OrdenCompraItemSugerido> buildTablaOrdenSugerida(List<OrdenCompraItemSugerido> items) {
        TableView<OrdenCompraItemSugerido> tabla = buildTableView();
        TableColumn<OrdenCompraItemSugerido, String> colNombre   = col("Producto", d -> d.nombre());
        TableColumn<OrdenCompraItemSugerido, String> colCodigo   = col("Código", d -> d.codigoBarras() != null ? d.codigoBarras() : "—", 120);
        TableColumn<OrdenCompraItemSugerido, String> colStock    = col("Stock actual", d -> String.valueOf(d.stockActual()), 95);
        TableColumn<OrdenCompraItemSugerido, String> colSugerida = col("Cant. sugerida", d -> String.valueOf(d.cantidadSugerida()), 110);
        TableColumn<OrdenCompraItemSugerido, String> colCompra   = col("P. compra", d -> FMT.format(d.precioCompra()), 120);
        TableColumn<OrdenCompraItemSugerido, String> colSubtotal = col("Subtotal estimado", d -> FMT.format(d.subtotalEstimado()), 140);

        colStock.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                OrdenCompraItemSugerido item = getTableRow() != null ? getTableRow().getItem() : null;
                boolean agotado = item != null && item.stockActual() == 0;
                setText(s);
                setStyle("-fx-font-weight:700;-fx-alignment:CENTER;-fx-text-fill:" + (agotado ? "#DC2626" : "#D97706") + ";");
            }
        });

        colSugerida.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-weight:700;-fx-alignment:CENTER;-fx-text-fill:#5A6ACF;");
            }
        });

        colSubtotal.setCellFactory(tc -> boldColorCell("#15803D"));

        tabla.getColumns().addAll(colNombre, colCodigo, colStock, colSugerida, colCompra, colSubtotal);
        tabla.setItems(FXCollections.observableArrayList(items));
        tabla.setPrefHeight(Math.min(320, 58 + (items.size() * 28)));
        return tabla;
    }

    private List<OrdenCompraProveedorSugerida> construirOrdenesCompraSugeridas(ReporteInventarioDto r) {
        List<ProductoStockDto> candidatos = new ArrayList<>();
        if (r.agotados() != null) candidatos.addAll(r.agotados());
        if (r.bajoStock() != null) candidatos.addAll(r.bajoStock());

        Map<String, ProductoStockDto> unicos = new LinkedHashMap<>();
        for (ProductoStockDto p : candidatos) {
            if (p == null) continue;
            String key = p.id() != null
                    ? "ID:" + p.id()
                    : (p.nombre() + "|" + (p.codigoBarras() != null ? p.codigoBarras() : ""));
            unicos.putIfAbsent(key, p);
        }

        Map<String, List<OrdenCompraItemSugerido>> porProveedor = new LinkedHashMap<>();
        List<ProductoStockDto> ordenados = unicos.values().stream()
                .sorted(Comparator.comparing(ProductoStockDto::proveedor, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparingInt(ProductoStockDto::stock)
                        .thenComparing(ProductoStockDto::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();

        for (ProductoStockDto p : ordenados) {
            String proveedor = normalizarProveedor(p.proveedor());
            int sugerida = calcularCantidadSugerida(p.stock(), r.umbralBajoStock());
            BigDecimal precioCompra = p.precioCompra() != null ? p.precioCompra() : BigDecimal.ZERO;
            BigDecimal subtotal = precioCompra.multiply(BigDecimal.valueOf(sugerida));

            porProveedor.computeIfAbsent(proveedor, k -> new ArrayList<>())
                    .add(new OrdenCompraItemSugerido(
                            p.nombre(),
                            p.codigoBarras(),
                            p.stock(),
                            sugerida,
                            precioCompra,
                            subtotal));
        }

        List<OrdenCompraProveedorSugerida> ordenes = new ArrayList<>();
        for (Map.Entry<String, List<OrdenCompraItemSugerido>> e : porProveedor.entrySet()) {
            List<OrdenCompraItemSugerido> items = e.getValue();
            int totalUnidades = items.stream().mapToInt(OrdenCompraItemSugerido::cantidadSugerida).sum();
            BigDecimal totalEstimado = items.stream()
                    .map(OrdenCompraItemSugerido::subtotalEstimado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ordenes.add(new OrdenCompraProveedorSugerida(e.getKey(), items, totalUnidades, totalEstimado));
        }
        return ordenes;
    }

    private int calcularCantidadSugerida(int stockActual, int umbral) {
        int umbralSeguro = Math.max(umbral, 1);
        // Objetivo: reponer al menos al doble del umbral para reducir quiebres de stock.
        int objetivoStock = Math.max(umbralSeguro * 2, umbralSeguro + 3);
        int sugerida = objetivoStock - Math.max(stockActual, 0);
        return Math.max(sugerida, 1);
    }

    private String normalizarProveedor(String proveedor) {
        if (proveedor == null || proveedor.isBlank()) return "Sin proveedor asignado";
        return proveedor.trim();
    }

    private record OrdenCompraItemSugerido(
            String nombre,
            String codigoBarras,
            int stockActual,
            int cantidadSugerida,
            BigDecimal precioCompra,
            BigDecimal subtotalEstimado
    ) {}

    private record OrdenCompraProveedorSugerida(
            String proveedor,
            List<OrdenCompraItemSugerido> items,
            int totalUnidades,
            BigDecimal totalEstimado
    ) {}

    // ─────────────────────────────────────────────────────────────
    // Tab 4 — Créditos pendientes
    // ─────────────────────────────────────────────────────────────

    private void mostrarCreditos() {
        activarTab(tabCreditos);
        contentArea.getChildren().clear();
        comprasCreditoPorCliente.clear();
        filtrosCreditoDisponibles = new ArrayList<>();

        ScrollPane scroll = buildScrollPane();
        VBox inner = new VBox(24);
        inner.setPadding(new Insets(28, 32, 32, 32));

        VBox selectorCard = buildSelectorCard("fas-credit-card", "#DC2626",
                "Reporte de créditos por cliente",
                "Genera reporte general o filtra por un cliente específico");

        ComboBox<FiltroClienteCredito> comboCliente = new ComboBox<>();
        comboCliente.getStyleClass().add("inventario-field");
        comboCliente.setMaxWidth(Double.MAX_VALUE);
        comboCliente.setPromptText("Selecciona un cliente o General...");
        comboCliente.setConverter(new StringConverter<>() {
            @Override public String toString(FiltroClienteCredito item) {
                return item != null ? textoFiltroCredito(item) : "";
            }
            @Override public FiltroClienteCredito fromString(String s) { return null; }
        });

            TextField txtBuscar = new TextField();
            txtBuscar.getStyleClass().add("inventario-field");
            txtBuscar.setPromptText("Buscar por nombre o cédula...");
            txtBuscar.setPrefWidth(280);
            txtBuscar.textProperty().addListener((obs, old, nuevo) ->
                aplicarBusquedaFiltroCreditos(comboCliente, nuevo));

        Button btnGenerar = buildBotonPrimario("Generar", "fas-play", "#DC2626");
            HBox filtros = new HBox(12, txtBuscar, comboCliente, btnGenerar);
        filtros.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(comboCliente, Priority.ALWAYS);
        selectorBody(selectorCard).getChildren().add(filtros);

        VBox resultado = new VBox(24);

        Runnable cargar = () -> {
            resultado.getChildren().clear();
            FiltroClienteCredito filtro = comboCliente.getValue() != null
                    ? comboCliente.getValue()
                    : FiltroClienteCredito.general();
            Long clienteId = filtro.esGeneral() ? null : filtro.clienteId();

            try {
                ReporteCreditosDto r = reporteService.reporteCreditos(clienteId);
                resultado.getChildren().addAll(buildResultadoCreditos(r, filtro));
            } catch (Exception ex) {
                resultado.getChildren().add(buildError(ex.getMessage()));
            }
            animarEntrada(resultado, 0);
        };

        btnGenerar.setOnAction(e -> cargar.run());

        try {
            ReporteCreditosDto base = reporteService.reporteCreditos();
            List<FiltroClienteCredito> opciones = new ArrayList<>();
            opciones.add(FiltroClienteCredito.general());
            for (ClienteCreditoDto c : base.clientes()) {
                opciones.add(FiltroClienteCredito.cliente(c.id(), c.nombre(), c.cedula()));
            }
            filtrosCreditoDisponibles = new ArrayList<>(opciones);
            comboCliente.setItems(FXCollections.observableArrayList(filtrosCreditoDisponibles));
            comboCliente.setValue(opciones.get(0));
            aplicarBusquedaFiltroCreditos(comboCliente, txtBuscar.getText());
        } catch (Exception ex) {
            resultado.getChildren().add(buildError(ex.getMessage()));
        }

        inner.getChildren().addAll(selectorCard, resultado);
        if (comboCliente.getValue() != null) btnGenerar.fire();

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    private List<Node> buildResultadoCreditos(ReporteCreditosDto r, FiltroClienteCredito filtro) {
        List<Node> nodes = new ArrayList<>();

        String titulo = filtro != null && !filtro.esGeneral()
                ? "Créditos pendientes — " + (filtro.nombre() != null ? filtro.nombre() : "Cliente")
                : "Créditos pendientes de clientes";

        nodes.add(buildResultHeader(titulo,
                () -> exportarCreditosExcel(r, filtro)));

        HBox kpi = new HBox(16);
        kpi.getChildren().addAll(
            crearKpiCard("fas-users",       "#5A6ACF", "Clientes con deuda",   String.valueOf(r.clientesConDeuda())),
            crearKpiCard("fas-dollar-sign", "#DC2626", "Deuda total pendiente", FMT.format(r.totalDeudaPendiente()))
        );
        nodes.add(kpi);

        if (r.clientesConDeuda() == 0) {
            String msg = filtro != null && !filtro.esGeneral()
                    ? "El cliente seleccionado no tiene créditos pendientes."
                    : "No hay créditos pendientes. Todos los clientes están al día.";
            nodes.add(buildBanner("fas-check-circle", msg, "#DCFCE7", "#15803D"));
            return nodes;
        }

        Label lbl = new Label("Detalle por cliente");
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        nodes.add(lbl);

        nodes.add(buildBanner("fas-info-circle",
            "Haz clic en un cliente para ver su historial de compras a crédito, y luego en cada compra para ver sus productos.",
                "#F1F5F9", "#64748B"));

        nodes.add(buildTablaCreditos(r.clientes()));
        return nodes;
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

        tabla.setRowFactory(tv -> {
            TableRow<ClienteCreditoDto> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) row.setStyle("-fx-background-color:rgba(90,106,207,0.08);-fx-cursor:hand;");
            });
            row.setOnMouseExited(e -> row.setStyle(""));
            row.setOnMouseClicked(e -> {
                if (row.isEmpty() || row.getItem() == null) return;
                ClienteCreditoDto cliente = row.getItem();
                List<Venta> compras = obtenerComprasCreditoCliente(cliente.id());
                abrirModalComprasCliente(cliente, compras);
            });
            return row;
        });

        tabla.getColumns().addAll(colNombre, colCedula, colLimite, colUsado, colDisponible);
        tabla.setItems(FXCollections.observableArrayList(clientes));
        return tabla;
    }

    private List<Venta> obtenerComprasCreditoCliente(Long clienteId) {
        if (clienteId == null) return List.of();
        if (!comprasCreditoPorCliente.containsKey(clienteId)) {
            try {
                comprasCreditoPorCliente.put(clienteId, reporteService.historialComprasCliente(clienteId));
            } catch (Exception e) {
                comprasCreditoPorCliente.put(clienteId, List.of());
            }
        }
        return comprasCreditoPorCliente.getOrDefault(clienteId, List.of());
    }

    private void abrirModalComprasCliente(ClienteCreditoDto cliente, List<Venta> compras) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");

        VBox modal = new VBox(0);
        modal.setMaxWidth(860);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 16px; " +
                "-fx-border-color: rgba(26,31,46,0.10); -fx-border-radius: 16px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 36, 0, 0, 10);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #DC2626; -fx-background-radius: 16px 16px 0 0; " +
                "-fx-padding: 16 20 16 20;");

        FontIcon icoH = new FontIcon("fas-user-clock");
        icoH.setIconSize(16);
        icoH.setIconColor(Paint.valueOf("#FFFFFF"));

        Label lblH = new Label("Historial de compras a crédito — " + (cliente.nombre() != null ? cliente.nombre() : "Cliente"));
        lblH.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        HBox.setHgrow(lblH, Priority.ALWAYS);

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); " +
                "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        header.getChildren().addAll(icoH, lblH, btnX);

        BigDecimal totalComprado = compras.stream()
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 8, 24));

        Label secResumen = new Label("RESUMEN DEL CLIENTE");
        secResumen.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;");

        VBox resumen = new VBox(8);
        resumen.setStyle("-fx-background-color: #F5F1EB; -fx-background-radius: 10px; -fx-padding: 14 16 14 16;");
        resumen.getChildren().addAll(
                crearFilaDetalleModal("fas-user", "#5A6ACF", "Cliente", cliente.nombre()),
                crearFilaDetalleModal("fas-id-card", "#78716C", "Cédula", cliente.cedula() != null ? cliente.cedula() : "—"),
                crearFilaDetalleModal("fas-receipt", "#D97706", "Compras a crédito", String.valueOf(compras.size())),
                crearFilaDetalleModal("fas-dollar-sign", "#15803D", "Total comprado", FMT.format(totalComprado)),
                crearFilaDetalleModal("fas-credit-card", "#DC2626", "Deuda pendiente", FMT.format(cliente.saldoUtilizado()))
        );

        body.getChildren().addAll(secResumen, resumen);

        if (compras.isEmpty()) {
            body.getChildren().add(buildBanner("fas-info-circle",
                    "Este cliente no tiene compras a crédito registradas.",
                    "#F1F5F9", "#64748B"));
        } else {
            Label secCompras = new Label("COMPRAS A CRÉDITO REGISTRADAS");
            secCompras.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E; -fx-padding: 4 0 0 0;");

            TableView<Venta> tabla = buildTableView();
            TableColumn<Venta, String> colFecha = col("Fecha",
                    v -> v.getFecha() != null ? v.getFecha().format(DFT) : "—", 145);
            TableColumn<Venta, String> colFactura = col("Factura",
                    v -> v.getNumeroComprobante() != null ? String.valueOf(v.getNumeroComprobante()) : "—", 90);
            TableColumn<Venta, String> colMetodo = col("Método",
                    v -> textoMetodoPago(v.getMetodoPago()), 110);
            TableColumn<Venta, String> colEstado = col("Estado",
                    v -> textoEstadoVenta(v.getEstado()), 95);
            TableColumn<Venta, String> colTotal = col("Total",
                    v -> FMT.format(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO), 120);
            TableColumn<Venta, String> colProductos = col("Productos",
                    this::resumirProductosCompra, 250);

            colFactura.setCellFactory(tc -> centeredCell());
            colMetodo.setCellFactory(tc -> centeredCell());
            colEstado.setCellFactory(tc -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setGraphic(null); setText(null); return; }
                    boolean anulada = "Anulada".equals(s);
                    setGraphic(crearBadge(s,
                            anulada ? "#FEE2E2" : "#DCFCE7",
                            anulada ? "#DC2626" : "#15803D"));
                    setText(null);
                }
            });
            colTotal.setCellFactory(tc -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setText(null); setStyle(""); return; }
                    Venta item = getTableRow() != null ? getTableRow().getItem() : null;
                    boolean anulada = item != null && EstadoVenta.ANULADA.equals(item.getEstado());
                    setText(s);
                    setStyle("-fx-font-weight:700;-fx-text-fill:" + (anulada ? "#DC2626" : "#15803D") + ";");
                }
            });

            tabla.setRowFactory(tv -> {
                TableRow<Venta> row = new TableRow<>();
                row.setOnMouseEntered(e -> {
                    if (!row.isEmpty()) row.setStyle("-fx-background-color:rgba(90,106,207,0.08);-fx-cursor:hand;");
                });
                row.setOnMouseExited(e -> row.setStyle(""));
                row.setOnMouseClicked(e -> {
                    if (row.isEmpty() || row.getItem() == null) return;
                    abrirModalDetalleVenta(row.getItem());
                });
                return row;
            });

            tabla.getColumns().addAll(colFecha, colFactura, colMetodo, colEstado, colTotal, colProductos);
            tabla.setItems(FXCollections.observableArrayList(compras));
            tabla.setPrefHeight(Math.min(420, 120 + compras.size() * 30));

            body.getChildren().addAll(secCompras, buildBanner("fas-info-circle",
                    "Haz clic sobre una compra para ver su detalle de productos.",
                    "#EEF2FF", "#4F46E5"), tabla);
        }

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroll.getStyleClass().add("inventario-root-stack");
        bodyScroll.setMaxHeight(540);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 24, 20, 24));

        Button btnExportar = buildBotonPrimario("Exportar", "fas-file-excel", "#15803D");
        btnExportar.setOnAction(e -> exportarHistorialCreditoClienteExcel(cliente, compras));

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 8px; " +
                "-fx-text-fill: #57534E; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        footer.getChildren().addAll(btnExportar, btnCerrar);

        modal.getChildren().addAll(header, bodyScroll, footer);
        overlay.getChildren().add(modal);
        StackPane.setAlignment(modal, Pos.CENTER);

        Runnable cerrar = () -> cerrarModal(overlay);
        btnX.setOnAction(e -> cerrar.run());
        btnCerrar.setOnAction(e -> cerrar.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrar.run(); });

        rootStack.getChildren().add(overlay);
        animarEntradaModal(overlay, modal);
    }

    private void exportarHistorialCreditoClienteExcel(ClienteCreditoDto cliente, List<Venta> compras) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = estiloEncabezado(wb);
            CellStyle moneda = estiloMoneda(wb);
            CellStyle normal = estiloNormal(wb);

            List<Venta> comprasSeguras = compras != null ? compras : List.of();
            BigDecimal totalComprado = comprasSeguras.stream()
                    .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            XSSFSheet resumen = wb.createSheet("Resumen cliente");
            Row hr = resumen.createRow(0);
            setCellHeader(hr, 0, "Concepto", header);
            setCellHeader(hr, 1, "Valor", header);

            String nombreCliente = cliente != null && cliente.nombre() != null && !cliente.nombre().isBlank()
                    ? cliente.nombre()
                    : "Cliente";
            String cedulaCliente = cliente != null && cliente.cedula() != null ? cliente.cedula() : "";
            BigDecimal deuda = cliente != null && cliente.saldoUtilizado() != null ? cliente.saldoUtilizado() : BigDecimal.ZERO;

            String[][] datosResumen = {
                    {"Cliente", nombreCliente},
                    {"Cédula", cedulaCliente.isBlank() ? "—" : cedulaCliente},
                    {"Compras a crédito", String.valueOf(comprasSeguras.size())},
                    {"Total comprado", FMT.format(totalComprado)},
                    {"Deuda pendiente", FMT.format(deuda)}
            };

            for (int i = 0; i < datosResumen.length; i++) {
                Row row = resumen.createRow(i + 1);
                row.createCell(0).setCellValue(datosResumen[i][0]);
                row.createCell(1).setCellValue(datosResumen[i][1]);
                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
            }
            autoSize(resumen, 2);

            XSSFSheet detalle = wb.createSheet("Compras crédito");
            Row hd = detalle.createRow(0);
            setCellHeader(hd, 0, "Fecha compra", header);
            setCellHeader(hd, 1, "Factura", header);
            setCellHeader(hd, 2, "Método", header);
            setCellHeader(hd, 3, "Estado", header);
            setCellHeader(hd, 4, "Total", header);
            setCellHeader(hd, 5, "Productos comprados", header);

            int fila = 1;
            for (Venta venta : comprasSeguras) {
                Row row = detalle.createRow(fila++);
                row.createCell(0).setCellValue(venta.getFecha() != null ? venta.getFecha().format(DFT) : "");
                row.createCell(1).setCellValue(venta.getNumeroComprobante() != null
                        ? String.valueOf(venta.getNumeroComprobante()) : "");
                row.createCell(2).setCellValue(textoMetodoPago(venta.getMetodoPago()));
                row.createCell(3).setCellValue(textoEstadoVenta(venta.getEstado()));
                row.createCell(4).setCellValue(venta.getTotal() != null ? venta.getTotal().doubleValue() : 0d);
                row.createCell(5).setCellValue(resumirProductosCompra(venta));

                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
                row.getCell(2).setCellStyle(normal);
                row.getCell(3).setCellStyle(normal);
                row.getCell(4).setCellStyle(moneda);
                row.getCell(5).setCellStyle(normal);
            }

            if (fila == 1) {
                Row row = detalle.createRow(1);
                row.createCell(0).setCellValue("Sin compras a crédito registradas para este cliente.");
                row.getCell(0).setCellStyle(normal);
            }
            autoSize(detalle, 6);

            String baseNombre = Normalizer.normalize(nombreCliente, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT)
                    .trim()
                    .replace(" ", "_")
                    .replaceAll("[^a-z0-9_]+", "");
            if (baseNombre.isBlank()) baseNombre = "cliente";

            guardarWorkbook(wb, "historial_credito_" + baseNombre + ".xlsx");
        } catch (Exception e) {
            mostrarAlertaError("No se pudo exportar historial: " + e.getMessage());
        }
    }

    private String resumirProductosCompra(Venta venta) {
        if (venta == null || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            return "—";
        }
        List<String> items = new ArrayList<>();
        for (DetalleVenta d : venta.getDetalles()) {
            if (d == null || d.getProducto() == null) continue;
            String nombre = d.getProducto().getNombre() != null ? d.getProducto().getNombre() : "Producto";
            items.add(nombre + " x" + d.getCantidad());
        }
        if (items.isEmpty()) return "—";
        if (items.size() <= 3) return String.join(", ", items);
        return String.join(", ", items.subList(0, 3)) + " +" + (items.size() - 3) + " más";
    }

    private String textoFiltroCredito(FiltroClienteCredito filtro) {
        if (filtro == null) return "General (todos los clientes con deuda)";
        if (filtro.esGeneral()) return "General (todos los clientes con deuda)";
        String ced = filtro.cedula() != null && !filtro.cedula().isBlank() ? " · CC " + filtro.cedula() : "";
        return (filtro.nombre() != null ? filtro.nombre() : "Cliente") + ced;
    }

    private void aplicarBusquedaFiltroCreditos(ComboBox<FiltroClienteCredito> comboCliente, String textoBusqueda) {
        if (comboCliente == null || ajustandoFiltroCreditos) return;
        if (filtrosCreditoDisponibles == null || filtrosCreditoDisponibles.isEmpty()) return;

        ajustandoFiltroCreditos = true;
        try {
            FiltroClienteCredito general = filtrosCreditoDisponibles.stream()
                    .filter(FiltroClienteCredito::esGeneral)
                    .findFirst()
                    .orElse(FiltroClienteCredito.general());

            String q = normalizarTextoBusquedaCredito(textoBusqueda);

            List<FiltroClienteCredito> clientesFiltrados = filtrosCreditoDisponibles.stream()
                    .filter(f -> !f.esGeneral())
                    .filter(f -> q.isBlank() || coincideBusquedaCredito(f, q))
                    .toList();

            List<FiltroClienteCredito> resultado = new ArrayList<>();
            resultado.add(general);
            resultado.addAll(clientesFiltrados);

            FiltroClienteCredito actual = comboCliente.getValue();
            comboCliente.setItems(FXCollections.observableArrayList(resultado));

            if (actual != null) {
                Optional<FiltroClienteCredito> matchActual = resultado.stream()
                        .filter(f -> esMismoFiltroCredito(f, actual))
                        .findFirst();
                if (matchActual.isPresent()) {
                    comboCliente.setValue(matchActual.get());
                    return;
                }
            }

            if (!q.isBlank() && resultado.size() > 1) {
                comboCliente.setValue(resultado.get(1));
            } else {
                comboCliente.setValue(resultado.get(0));
            }
        } finally {
            ajustandoFiltroCreditos = false;
        }
    }

    private boolean coincideBusquedaCredito(FiltroClienteCredito filtro, String busquedaNormalizada) {
        if (filtro == null || busquedaNormalizada == null || busquedaNormalizada.isBlank()) return true;
        String nombre = normalizarTextoBusquedaCredito(filtro.nombre());
        String cedula = normalizarTextoBusquedaCredito(filtro.cedula());
        return nombre.contains(busquedaNormalizada) || cedula.contains(busquedaNormalizada);
    }

    private String normalizarTextoBusquedaCredito(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String base = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return base.toLowerCase(Locale.ROOT).trim();
    }

    private boolean esMismoFiltroCredito(FiltroClienteCredito a, FiltroClienteCredito b) {
        if (a == null || b == null) return false;
        return a.esGeneral() == b.esGeneral()
                && Objects.equals(a.clienteId(), b.clienteId());
    }

    private record FiltroClienteCredito(Long clienteId, String nombre, String cedula, boolean esGeneral) {
        private static FiltroClienteCredito general() {
            return new FiltroClienteCredito(null, "General", null, true);
        }

        private static FiltroClienteCredito cliente(Long clienteId, String nombre, String cedula) {
            return new FiltroClienteCredito(clienteId, nombre, cedula, false);
        }
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

        List<CompraHistorialDto> historial = r.compras() != null ? r.compras() : List.of();
        List<ProductoCompradoDto> productos = r.productos() != null ? r.productos() : List.of();

        BigDecimal totalInvertido = r.totalInvertido() != null ? r.totalInvertido() : BigDecimal.ZERO;
        BigDecimal ticketPromedio = r.ticketPromedio() != null ? r.ticketPromedio() : BigDecimal.ZERO;

        HBox kpi = new HBox(16);
        kpi.getChildren().addAll(
            crearKpiCard("fas-file-invoice", "#7C3AED", "Órdenes de compra", String.valueOf(r.totalCompras())),
            crearKpiCard("fas-dollar-sign",  "#D97706", "Total invertido",    FMT.format(totalInvertido)),
            crearKpiCard("fas-calculator",   "#5A6ACF", "Ticket promedio",    FMT.format(ticketPromedio)),
            crearKpiCard("fas-boxes",        "#15803D", "Unidades compradas", String.valueOf(r.totalUnidades()))
        );
        nodes.add(kpi);

        HBox contexto = new HBox(16);
        contexto.getChildren().add(
            crearKpiCard("fas-clock", "#475569", "Última compra",
                r.fechaUltimaCompra() != null ? r.fechaUltimaCompra().format(DFT) : "Sin compras")
        );
        nodes.add(contexto);

        if (!productos.isEmpty()) {
            ProductoCompradoDto principal = productos.get(0);
            nodes.add(buildBanner("fas-lightbulb",
                "Producto con mayor inversión: " + principal.nombre() +
                    " (" + principal.cantidadTotal() + " uds · " + FMT.format(principal.totalInvertido()) + ")",
                "#ECFDF5", "#166534"));
        }

        if (!historial.isEmpty()) {
            Label lblHistorial = new Label("Historial de órdenes de compra");
            lblHistorial.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
            nodes.add(lblHistorial);
            nodes.add(buildBanner("fas-mouse-pointer",
                "Haz clic sobre una orden para ver el detalle de productos y costos.",
                "#F8FAFC", "#475569"));

            TableView<CompraHistorialDto> tablaHistorial = buildTableView();
            TableColumn<CompraHistorialDto, String> colFecha = col("Fecha", c -> c.fecha() != null ? c.fecha().format(DFT) : "—", 160);
            TableColumn<CompraHistorialDto, String> colFactura = col("Factura", c -> textoFacturaCompra(c.numeroFactura()), 140);
            TableColumn<CompraHistorialDto, String> colItems = col("Ítems", c -> String.valueOf(c.items()), 80);
            TableColumn<CompraHistorialDto, String> colUnidades = col("Unidades", c -> String.valueOf(c.unidades()), 90);
            TableColumn<CompraHistorialDto, String> colTotal = col("Total", c -> FMT.format(c.total() != null ? c.total() : BigDecimal.ZERO), 140);

            colItems.setCellFactory(tc -> centeredCell());
            colUnidades.setCellFactory(tc -> centeredCell());
            colTotal.setCellFactory(tc -> boldColorCell("#D97706"));

            tablaHistorial.getColumns().addAll(colFecha, colFactura, colItems, colUnidades, colTotal);
            tablaHistorial.setItems(FXCollections.observableArrayList(historial));
            tablaHistorial.setRowFactory(tv -> {
                TableRow<CompraHistorialDto> row = new TableRow<>();
                row.setOnMouseEntered(e -> {
                    if (!row.isEmpty()) row.setStyle("-fx-background-color:rgba(124,58,237,0.07);");
                });
                row.setOnMouseExited(e -> row.setStyle(""));
                row.setOnMouseClicked(e -> {
                    if (!row.isEmpty()) {
                        abrirModalDetalleCompraProveedor(row.getItem(), r.nombreProveedor());
                    }
                });
                return row;
            });
            nodes.add(tablaHistorial);
        } else {
            nodes.add(buildBanner("fas-info-circle",
                "Este proveedor no tiene compras registradas.", "#F1F5F9", "#64748B"));
        }

        if (!productos.isEmpty()) {
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
            tabla.setItems(FXCollections.observableArrayList(productos));
            nodes.add(tabla);
        }

        return nodes;
    }

    private String textoFacturaCompra(String numeroFactura) {
        return numeroFactura != null && !numeroFactura.isBlank() ? numeroFactura : "Sin número";
    }

    private void abrirModalDetalleCompraProveedor(CompraHistorialDto compra, String proveedorNombre) {
        if (compra == null) return;

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");

        VBox modal = new VBox(0);
        modal.setMaxWidth(700);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 16px; " +
                "-fx-border-color: rgba(26,31,46,0.10); -fx-border-radius: 16px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 36, 0, 0, 10);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #7C3AED; -fx-background-radius: 16px 16px 0 0; " +
                "-fx-padding: 16 20 16 20;");

        FontIcon icoH = new FontIcon("fas-truck-loading");
        icoH.setIconSize(16);
        icoH.setIconColor(Paint.valueOf("#FFFFFF"));

        String titulo = compra.numeroFactura() != null && !compra.numeroFactura().isBlank()
                ? "Factura #" + compra.numeroFactura()
                : "Detalle de compra";
        Label lblH = new Label(titulo);
        lblH.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        HBox.setHgrow(lblH, Priority.ALWAYS);

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.85); " +
                "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");

        header.getChildren().addAll(icoH, lblH, btnX);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 8, 24));

        Label secResumen = new Label("RESUMEN DE LA COMPRA");
        secResumen.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;");

        VBox resumen = new VBox(8);
        resumen.setStyle("-fx-background-color: #F5F1EB; -fx-background-radius: 10px; -fx-padding: 14 16 14 16;");
        resumen.getChildren().addAll(
                crearFilaDetalleModal("fas-truck", "#7C3AED", "Proveedor",
                        proveedorNombre != null ? proveedorNombre : "—"),
                crearFilaDetalleModal("fas-clock", "#D97706", "Fecha",
                        compra.fecha() != null ? compra.fecha().format(DFT) : "—"),
                crearFilaDetalleModal("fas-file-invoice", "#5A6ACF", "Factura",
                        textoFacturaCompra(compra.numeroFactura())),
                crearFilaDetalleModal("fas-list", "#78716C", "Ítems", String.valueOf(compra.items())),
                crearFilaDetalleModal("fas-boxes", "#7C3AED", "Unidades", String.valueOf(compra.unidades())),
                crearFilaDetalleModal("fas-dollar-sign", "#15803D", "Total",
                        FMT.format(compra.total() != null ? compra.total() : BigDecimal.ZERO))
        );

        Label secProductos = new Label("PRODUCTOS DE LA COMPRA");
        secProductos.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E; -fx-padding: 4 0 0 0;");

        TableView<CompraProductoDetalleDto> tabla = buildTableView();
        TableColumn<CompraProductoDetalleDto, String> colProducto = col("Producto", d -> d.nombreProducto());
        TableColumn<CompraProductoDetalleDto, String> colCodigo = col("Código", d -> d.codigoBarras() != null ? d.codigoBarras() : "—", 120);
        TableColumn<CompraProductoDetalleDto, String> colCantidad = col("Unidades", d -> String.valueOf(d.cantidad()), 90);
        TableColumn<CompraProductoDetalleDto, String> colPrecio = col("Costo unitario", d -> FMT.format(d.precioUnitario()), 130);
        TableColumn<CompraProductoDetalleDto, String> colSubtotal = col("Subtotal", d -> FMT.format(d.subtotal()), 130);

        colCantidad.setCellFactory(tc -> centeredCell());
        colSubtotal.setCellFactory(tc -> boldColorCell("#15803D"));

        tabla.getColumns().addAll(colProducto, colCodigo, colCantidad, colPrecio, colSubtotal);
        List<CompraProductoDetalleDto> productos = compra.productos() != null ? compra.productos() : List.of();
        tabla.setItems(FXCollections.observableArrayList(productos));
        tabla.setPrefHeight(productos.isEmpty() ? 140 : Math.min(320, 120 + productos.size() * 32));

        body.getChildren().addAll(secResumen, resumen, secProductos, tabla);

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroll.getStyleClass().add("inventario-root-stack");
        bodyScroll.setMaxHeight(520);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 24, 20, 24));

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 8px; " +
                "-fx-text-fill: #57534E; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        footer.getChildren().add(btnCerrar);

        modal.getChildren().addAll(header, bodyScroll, footer);
        overlay.getChildren().add(modal);
        StackPane.setAlignment(modal, Pos.CENTER);

        Runnable cerrar = () -> cerrarModal(overlay);
        btnX.setOnAction(e -> cerrar.run());
        btnCerrar.setOnAction(e -> cerrar.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrar.run(); });

        rootStack.getChildren().add(overlay);
        animarEntradaModal(overlay, modal);
    }

    // ─────────────────────────────────────────────────────────────
    // Excel — Exportación
    // ─────────────────────────────────────────────────────────────

    private void exportarVentasExcel(ReporteVentasDto r, Caja cajaSesion) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = estiloEncabezado(wb);
            CellStyle moneda  = estiloMoneda(wb);
            CellStyle normal  = estiloNormal(wb);

            // Hoja 1: Resumen
            XSSFSheet resumen = wb.createSheet("Resumen");
            List<String[]> datos = new ArrayList<>(List.of(
                new String[]{ "Caja ID",           String.valueOf(r.cajaId()) },
                new String[]{ "Apertura",          r.fechaApertura() != null ? r.fechaApertura().format(DFT) : "—" },
                new String[]{ "Cierre",            r.fechaCierre() != null ? r.fechaCierre().format(DFT) : "En curso" },
                new String[]{ "Total ventas",      String.valueOf(r.totalVentas()) },
                new String[]{ "Completadas",       String.valueOf(r.ventasCompletadas()) },
                new String[]{ "Anuladas",          String.valueOf(r.ventasAnuladas()) },
                new String[]{ "Total contado",     FMT.format(r.totalEfectivo()) },
                new String[]{ "Total transferencia", FMT.format(r.totalTransferencia()) },
                new String[]{ "Total crédito",     FMT.format(r.totalCredito()) },
                new String[]{ "Total general",     FMT.format(r.totalGeneral()) }
            ));

            if (cajaSesion != null) {
                BigDecimal montoInicial = cajaSesion.getMontoInicial() != null ? cajaSesion.getMontoInicial() : BigDecimal.ZERO;
                BigDecimal esperadoEnCaja = montoInicial.add(r.totalEfectivo() != null ? r.totalEfectivo() : BigDecimal.ZERO);

                datos.add(new String[]{ "Monto inicial", FMT.format(montoInicial) });
                datos.add(new String[]{ "Esperado en caja", FMT.format(esperadoEnCaja) });

                if (cajaSesion.getMontoFinal() != null) {
                    BigDecimal desajuste = cajaSesion.getMontoFinal().subtract(esperadoEnCaja);
                    String signo = desajuste.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
                    datos.add(new String[]{ "Monto final contado", FMT.format(cajaSesion.getMontoFinal()) });
                    datos.add(new String[]{ "Desajuste", signo + FMT.format(desajuste) });
                } else {
                    datos.add(new String[]{ "Monto final contado", "En curso" });
                }
            }
            Row h = resumen.createRow(0);
            setCellHeader(h, 0, "Concepto", header);
            setCellHeader(h, 1, "Valor", header);
            for (int i = 0; i < datos.size(); i++) {
                Row row = resumen.createRow(i + 1);
                row.createCell(0).setCellValue(datos.get(i)[0]);
                row.createCell(1).setCellValue(datos.get(i)[1]);
                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
            }
            autoSize(resumen, 2);

            // Hoja 2: Detalle de ventas
            XSSFSheet detalle = wb.createSheet("Detalle ventas");
            Row hd = detalle.createRow(0);
            setCellHeader(hd, 0, "Comprobante", header);
            setCellHeader(hd, 1, "Fecha y hora", header);
            setCellHeader(hd, 2, "Cliente", header);
            setCellHeader(hd, 3, "Cédula", header);
            setCellHeader(hd, 4, "Método pago", header);
            setCellHeader(hd, 5, "Estado", header);
            setCellHeader(hd, 6, "Total", header);

            int filaDetalle = 1;
            for (VentaDetalleDto v : r.detalleVentas()) {
                Row row = detalle.createRow(filaDetalle++);
                row.createCell(0).setCellValue(v.numeroComprobante() != null ? String.valueOf(v.numeroComprobante()) : "");
                row.createCell(1).setCellValue(v.fecha() != null ? v.fecha().format(DFT) : "");
                row.createCell(2).setCellValue(v.clienteNombre() != null ? v.clienteNombre() : "Venta directa");
                row.createCell(3).setCellValue(v.clienteCedula() != null ? v.clienteCedula() : "");
                row.createCell(4).setCellValue(textoMetodoPago(v.metodoPago()));
                row.createCell(5).setCellValue(textoEstadoVenta(v.estado()));
                row.createCell(6).setCellValue((v.total() != null ? v.total() : BigDecimal.ZERO).doubleValue());

                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
                row.getCell(2).setCellStyle(normal);
                row.getCell(3).setCellStyle(normal);
                row.getCell(4).setCellStyle(normal);
                row.getCell(5).setCellStyle(normal);
                row.getCell(6).setCellStyle(moneda);
            }
            autoSize(detalle, 7);

            // Hoja 3: Productos detallados por venta
            XSSFSheet productosDetallados = wb.createSheet("Productos detallados");
            Row hpd = productosDetallados.createRow(0);
            setCellHeader(hpd, 0, "Comprobante", header);
            setCellHeader(hpd, 1, "Fecha y hora", header);
            setCellHeader(hpd, 2, "Cliente", header);
            setCellHeader(hpd, 3, "Cédula", header);
            setCellHeader(hpd, 4, "Método pago", header);
            setCellHeader(hpd, 5, "Estado", header);
            setCellHeader(hpd, 6, "Producto", header);
            setCellHeader(hpd, 7, "Código", header);
            setCellHeader(hpd, 8, "Unidades", header);
            setCellHeader(hpd, 9, "Precio unitario", header);
            setCellHeader(hpd, 10, "Subtotal", header);

            CellStyle grupoVentaCentrado = wb.createCellStyle();
            grupoVentaCentrado.cloneStyleFrom(normal);
            grupoVentaCentrado.setAlignment(HorizontalAlignment.CENTER);
            grupoVentaCentrado.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFColor colorBloqueAlterno = new XSSFColor(new byte[]{(byte)247, (byte)249, (byte)255}, null);

            CellStyle grupoVentaCentradoAlt = wb.createCellStyle();
            grupoVentaCentradoAlt.cloneStyleFrom(grupoVentaCentrado);
            grupoVentaCentradoAlt.setFillForegroundColor(colorBloqueAlterno);
            grupoVentaCentradoAlt.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle normalAlt = wb.createCellStyle();
            normalAlt.cloneStyleFrom(normal);
            normalAlt.setFillForegroundColor(colorBloqueAlterno);
            normalAlt.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle monedaAlt = wb.createCellStyle();
            monedaAlt.cloneStyleFrom(moneda);
            monedaAlt.setFillForegroundColor(colorBloqueAlterno);
            monedaAlt.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            List<Venta> ventasExportar;
            try {
                ventasExportar = ventaService.findByCajaId(r.cajaId());
            } catch (Exception ex) {
                ventasExportar = new ArrayList<>(ventasSesionActual.values());
            }

            int filaProductosDetallados = 1;
            int indiceBloqueVenta = 0;
            for (Venta v : ventasExportar) {
                boolean bloqueAlterno = (indiceBloqueVenta % 2) == 1;
                CellStyle estiloGrupo = bloqueAlterno ? grupoVentaCentradoAlt : grupoVentaCentrado;
                CellStyle estiloTexto = bloqueAlterno ? normalAlt : normal;
                CellStyle estiloMoneda = bloqueAlterno ? monedaAlt : moneda;

                List<DetalleVenta> detalles = v.getDetalles() != null ? v.getDetalles() : List.of();
                if (detalles.isEmpty()) continue;

                int filaInicioVenta = filaProductosDetallados;
                for (DetalleVenta d : detalles) {
                    Row row = productosDetallados.createRow(filaProductosDetallados++);
                    row.createCell(0).setCellValue(v.getNumeroComprobante() != null ? String.valueOf(v.getNumeroComprobante()) : "");
                    row.createCell(1).setCellValue(v.getFecha() != null ? v.getFecha().format(DFT) : "");
                    row.createCell(2).setCellValue(
                            v.getCliente() != null && v.getCliente().getNombre() != null
                                    ? v.getCliente().getNombre()
                                    : "Venta directa");
                    row.createCell(3).setCellValue(
                            v.getCliente() != null && v.getCliente().getCedula() != null
                                    ? v.getCliente().getCedula()
                                    : "");
                    row.createCell(4).setCellValue(textoMetodoPago(v.getMetodoPago()));
                    row.createCell(5).setCellValue(textoEstadoVenta(v.getEstado()));
                    row.createCell(6).setCellValue(
                            d.getProducto() != null && d.getProducto().getNombre() != null
                                    ? d.getProducto().getNombre()
                                    : "");
                    row.createCell(7).setCellValue(
                            d.getProducto() != null && d.getProducto().getCodigoBarras() != null
                                    ? d.getProducto().getCodigoBarras()
                                    : "");
                    row.createCell(8).setCellValue(d.getCantidad());
                    row.createCell(9).setCellValue((d.getPrecioUnitario() != null ? d.getPrecioUnitario() : BigDecimal.ZERO).doubleValue());
                    row.createCell(10).setCellValue((d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO).doubleValue());

                    row.getCell(0).setCellStyle(estiloGrupo);
                    row.getCell(1).setCellStyle(estiloGrupo);
                    row.getCell(2).setCellStyle(estiloGrupo);
                    row.getCell(3).setCellStyle(estiloGrupo);
                    row.getCell(4).setCellStyle(estiloGrupo);
                    row.getCell(5).setCellStyle(estiloGrupo);
                    row.getCell(6).setCellStyle(estiloTexto);
                    row.getCell(7).setCellStyle(estiloTexto);
                    row.getCell(8).setCellStyle(estiloTexto);
                    row.getCell(9).setCellStyle(estiloMoneda);
                    row.getCell(10).setCellStyle(estiloMoneda);
                }

                int filaFinVenta = filaProductosDetallados - 1;
                for (int col = 0; col <= 5; col++) {
                    combinarCeldasYCentrar(productosDetallados, filaInicioVenta, filaFinVenta, col, estiloGrupo);
                }
                indiceBloqueVenta++;
            }
            autoSize(productosDetallados, 11);

            // Hoja 4: Productos vendidos
            XSSFSheet productos = wb.createSheet("Productos vendidos");
            Row hp = productos.createRow(0);
            setCellHeader(hp, 0, "Producto", header);
            setCellHeader(hp, 1, "Código", header);
            setCellHeader(hp, 2, "Unidades", header);
            setCellHeader(hp, 3, "Total generado", header);
            int filaProductos = 1;
            for (ProductoVendidoDto p : r.topProductos()) {
                Row row = productos.createRow(filaProductos++);
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
            setCellHeader(h, 4, "Desajuste caja", header);
            setCellHeader(h, 5, "Ganancia bruta", header);
            setCellHeader(h, 6, "Margen %", header);
            setCellHeader(h, 7, "Estado", header);

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(r.anio());
            row.createCell(1).setCellValue(NOMBRES_MESES[r.mes()]);
            row.createCell(2).setCellValue(r.totalInvertido().doubleValue());
            row.createCell(3).setCellValue(r.totalVendido().doubleValue());
            row.createCell(4).setCellValue(r.ajusteCaja().doubleValue());
            row.createCell(5).setCellValue(r.gananciaBruta().doubleValue());
            row.createCell(6).setCellValue(r.margenPorcentaje() != null ? r.margenPorcentaje().doubleValue() : 0);
            row.createCell(7).setCellValue(r.tuvoPerdida() ? "Pérdida" : "Ganancia");
            for (int i = 0; i <= 7; i++) row.getCell(i).setCellStyle(normal);
            autoSize(sheet, 8);

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
                { "Desajuste caja",  FMT.format(r.ajusteCajaTotal()) },
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
            setCellHeader(hm, 3, "Desajuste caja", header);
            setCellHeader(hm, 4, "Ganancia bruta", header);
            setCellHeader(hm, 5, "Estado", header);
            int fila = 1;
            for (RentabilidadMensualDto m : r.meses()) {
                Row row = meses.createRow(fila++);
                row.createCell(0).setCellValue(m.nombreMes());
                row.createCell(1).setCellValue(m.totalInvertido().doubleValue());
                row.createCell(2).setCellValue(m.totalVendido().doubleValue());
                row.createCell(3).setCellValue(m.ajusteCaja().doubleValue());
                row.createCell(4).setCellValue(m.gananciaBruta().doubleValue());
                row.createCell(5).setCellValue(m.tuvoPerdida() ? "Pérdida" : "Ganancia");
                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(moneda);
                row.getCell(2).setCellStyle(moneda);
                row.getCell(3).setCellStyle(moneda);
                row.getCell(4).setCellStyle(moneda);
                row.getCell(5).setCellStyle(normal);
            }
            autoSize(meses, 6);

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

            // Hoja 3: Órdenes sugeridas por proveedor
            List<OrdenCompraProveedorSugerida> ordenes = construirOrdenesCompraSugeridas(r);
            XSSFSheet sugeridas = wb.createSheet("OC sugeridas");
            Row hs = sugeridas.createRow(0);
            setCellHeader(hs, 0, "Proveedor", header);
            setCellHeader(hs, 1, "Producto", header);
            setCellHeader(hs, 2, "Código", header);
            setCellHeader(hs, 3, "Stock actual", header);
            setCellHeader(hs, 4, "Cantidad sugerida", header);
            setCellHeader(hs, 5, "P. compra", header);
            setCellHeader(hs, 6, "Subtotal estimado", header);

            fila = 1;
            for (OrdenCompraProveedorSugerida orden : ordenes) {
                for (OrdenCompraItemSugerido item : orden.items()) {
                    Row row = sugeridas.createRow(fila);
                    int filaExcel = fila + 1;
                    fila++;
                    row.createCell(0).setCellValue(orden.proveedor());
                    row.createCell(1).setCellValue(item.nombre());
                    row.createCell(2).setCellValue(item.codigoBarras() != null ? item.codigoBarras() : "");
                    row.createCell(3).setCellValue(item.stockActual());
                    row.createCell(4).setCellValue(item.cantidadSugerida());
                    row.createCell(5).setCellValue(item.precioCompra().doubleValue());
                    row.createCell(6).setCellFormula("E" + filaExcel + "*F" + filaExcel);

                    row.getCell(0).setCellStyle(normal);
                    row.getCell(1).setCellStyle(normal);
                    row.getCell(2).setCellStyle(normal);
                    row.getCell(3).setCellStyle(normal);
                    row.getCell(4).setCellStyle(normal);
                    row.getCell(5).setCellStyle(moneda);
                    row.getCell(6).setCellStyle(moneda);
                }
            }
            autoSize(sugeridas, 7);

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

    private void exportarCreditosExcel(ReporteCreditosDto r, FiltroClienteCredito filtro) {
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

            XSSFSheet compras = wb.createSheet("Compras clientes");
            Row hc = compras.createRow(0);
            setCellHeader(hc, 0, "Cliente", header);
            setCellHeader(hc, 1, "Cédula", header);
            setCellHeader(hc, 2, "Fecha compra", header);
            setCellHeader(hc, 3, "Factura", header);
            setCellHeader(hc, 4, "Método", header);
            setCellHeader(hc, 5, "Estado", header);
            setCellHeader(hc, 6, "Total", header);
            setCellHeader(hc, 7, "Productos comprados", header);

            fila = 1;
            for (ClienteCreditoDto c : r.clientes()) {
                List<Venta> comprasCliente = obtenerComprasCreditoCliente(c.id());
                for (Venta venta : comprasCliente) {
                    Row row = compras.createRow(fila++);
                    row.createCell(0).setCellValue(c.nombre());
                    row.createCell(1).setCellValue(c.cedula() != null ? c.cedula() : "");
                    row.createCell(2).setCellValue(venta.getFecha() != null ? venta.getFecha().format(DFT) : "");
                    row.createCell(3).setCellValue(venta.getNumeroComprobante() != null
                            ? String.valueOf(venta.getNumeroComprobante()) : "");
                    row.createCell(4).setCellValue(textoMetodoPago(venta.getMetodoPago()));
                    row.createCell(5).setCellValue(textoEstadoVenta(venta.getEstado()));
                    row.createCell(6).setCellValue(venta.getTotal() != null ? venta.getTotal().doubleValue() : 0d);
                    row.createCell(7).setCellValue(resumirProductosCompra(venta));

                    row.getCell(0).setCellStyle(normal);
                    row.getCell(1).setCellStyle(normal);
                    row.getCell(2).setCellStyle(normal);
                    row.getCell(3).setCellStyle(normal);
                    row.getCell(4).setCellStyle(normal);
                    row.getCell(5).setCellStyle(normal);
                    row.getCell(6).setCellStyle(moneda);
                    row.getCell(7).setCellStyle(normal);
                }
            }

            if (fila == 1) {
                Row row = compras.createRow(fila);
                row.createCell(0).setCellValue("Sin compras registradas para el filtro seleccionado.");
                row.getCell(0).setCellStyle(normal);
            }
            autoSize(compras, 8);

            String sufijo = "";
            if (filtro != null && !filtro.esGeneral() && filtro.nombre() != null) {
                sufijo = "_" + filtro.nombre().toLowerCase(Locale.ROOT).replace(" ", "_");
            }
            guardarWorkbook(wb, "reporte_creditos_pendientes" + sufijo + ".xlsx");
        } catch (Exception e) {
            mostrarAlertaError("No se pudo exportar: " + e.getMessage());
        }
    }

    private void exportarComprasExcel(ReporteComprasDto r) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = estiloEncabezado(wb);
            CellStyle moneda = estiloMoneda(wb);
            CellStyle normal = estiloNormal(wb);

            List<CompraHistorialDto> historial = r.compras() != null ? r.compras() : List.of();
            List<ProductoCompradoDto> productos = r.productos() != null ? r.productos() : List.of();

            BigDecimal totalInvertido = r.totalInvertido() != null ? r.totalInvertido() : BigDecimal.ZERO;
            BigDecimal ticketPromedio = r.ticketPromedio() != null ? r.ticketPromedio() : BigDecimal.ZERO;

            // Hoja 1: Resumen proveedor
            XSSFSheet res = wb.createSheet("Resumen");
            Row h = res.createRow(0);
            setCellHeader(h, 0, "Concepto", header); setCellHeader(h, 1, "Valor", header);

            List<String[]> datos = new ArrayList<>(List.of(
                    new String[]{ "Proveedor",          r.nombreProveedor() != null ? r.nombreProveedor() : "—" },
                    new String[]{ "Órdenes de compra",  String.valueOf(r.totalCompras()) },
                    new String[]{ "Total invertido",    FMT.format(totalInvertido) },
                    new String[]{ "Ticket promedio",    FMT.format(ticketPromedio) },
                    new String[]{ "Unidades compradas", String.valueOf(r.totalUnidades()) },
                    new String[]{ "Última compra",      r.fechaUltimaCompra() != null ? r.fechaUltimaCompra().format(DFT) : "Sin compras" }
            ));

            if (!productos.isEmpty()) {
                ProductoCompradoDto principal = productos.get(0);
                datos.add(new String[]{ "Producto líder por inversión", principal.nombre() });
                datos.add(new String[]{ "Inversión producto líder", FMT.format(principal.totalInvertido()) });
            }

            for (int i = 0; i < datos.size(); i++) {
                Row row = res.createRow(i + 1);
                row.createCell(0).setCellValue(datos.get(i)[0]); row.getCell(0).setCellStyle(normal);
                row.createCell(1).setCellValue(datos.get(i)[1]); row.getCell(1).setCellStyle(normal);
            }
            autoSize(res, 2);

            // Hoja 2: Historial de compras
            XSSFSheet hist = wb.createSheet("Historial compras");
            Row hh = hist.createRow(0);
            setCellHeader(hh, 0, "Fecha", header);
            setCellHeader(hh, 1, "Factura", header);
            setCellHeader(hh, 2, "Ítems", header);
            setCellHeader(hh, 3, "Unidades", header);
            setCellHeader(hh, 4, "Total", header);

            int filaHistorial = 1;
            for (CompraHistorialDto compra : historial) {
                Row row = hist.createRow(filaHistorial++);
                row.createCell(0).setCellValue(compra.fecha() != null ? compra.fecha().format(DFT) : "");
                row.createCell(1).setCellValue(textoFacturaCompra(compra.numeroFactura()));
                row.createCell(2).setCellValue(compra.items());
                row.createCell(3).setCellValue(compra.unidades());
                row.createCell(4).setCellValue((compra.total() != null ? compra.total() : BigDecimal.ZERO).doubleValue());

                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
                row.getCell(2).setCellStyle(normal);
                row.getCell(3).setCellStyle(normal);
                row.getCell(4).setCellStyle(moneda);
            }

            if (filaHistorial == 1) {
                Row row = hist.createRow(1);
                row.createCell(0).setCellValue("Sin compras registradas para este proveedor.");
                row.getCell(0).setCellStyle(normal);
            }
            autoSize(hist, 5);

            // Hoja 3: Detalle de productos por compra
            XSSFSheet det = wb.createSheet("Detalle por compra");
            Row hd = det.createRow(0);
            setCellHeader(hd, 0, "Fecha", header);
            setCellHeader(hd, 1, "Factura", header);
            setCellHeader(hd, 2, "Producto", header);
            setCellHeader(hd, 3, "Código", header);
            setCellHeader(hd, 4, "Unidades", header);
            setCellHeader(hd, 5, "Costo unitario", header);
            setCellHeader(hd, 6, "Subtotal", header);

            int filaDetalle = 1;
            for (CompraHistorialDto compra : historial) {
                List<CompraProductoDetalleDto> detalleProductos = compra.productos() != null ? compra.productos() : List.of();

                if (detalleProductos.isEmpty()) {
                    Row row = det.createRow(filaDetalle++);
                    row.createCell(0).setCellValue(compra.fecha() != null ? compra.fecha().format(DFT) : "");
                    row.createCell(1).setCellValue(textoFacturaCompra(compra.numeroFactura()));
                    row.createCell(2).setCellValue("Sin productos asociados");
                    row.createCell(3).setCellValue("");
                    row.createCell(4).setCellValue(0);
                    row.createCell(5).setCellValue(0);
                    row.createCell(6).setCellValue(0);
                    row.getCell(0).setCellStyle(normal);
                    row.getCell(1).setCellStyle(normal);
                    row.getCell(2).setCellStyle(normal);
                    row.getCell(3).setCellStyle(normal);
                    row.getCell(4).setCellStyle(normal);
                    row.getCell(5).setCellStyle(moneda);
                    row.getCell(6).setCellStyle(moneda);
                    continue;
                }

                for (CompraProductoDetalleDto d : detalleProductos) {
                    Row row = det.createRow(filaDetalle++);
                    row.createCell(0).setCellValue(compra.fecha() != null ? compra.fecha().format(DFT) : "");
                    row.createCell(1).setCellValue(textoFacturaCompra(compra.numeroFactura()));
                    row.createCell(2).setCellValue(d.nombreProducto() != null ? d.nombreProducto() : "");
                    row.createCell(3).setCellValue(d.codigoBarras() != null ? d.codigoBarras() : "");
                    row.createCell(4).setCellValue(d.cantidad());
                    row.createCell(5).setCellValue((d.precioUnitario() != null ? d.precioUnitario() : BigDecimal.ZERO).doubleValue());
                    row.createCell(6).setCellValue((d.subtotal() != null ? d.subtotal() : BigDecimal.ZERO).doubleValue());
                    row.getCell(0).setCellStyle(normal);
                    row.getCell(1).setCellStyle(normal);
                    row.getCell(2).setCellStyle(normal);
                    row.getCell(3).setCellStyle(normal);
                    row.getCell(4).setCellStyle(normal);
                    row.getCell(5).setCellStyle(moneda);
                    row.getCell(6).setCellStyle(moneda);
                }
            }

            if (filaDetalle == 1) {
                Row row = det.createRow(1);
                row.createCell(0).setCellValue("Sin detalle disponible.");
                row.getCell(0).setCellStyle(normal);
            }
            autoSize(det, 7);

            // Hoja 4: Productos agregados
            XSSFSheet prods = wb.createSheet("Productos");
            Row hp = prods.createRow(0);
            setCellHeader(hp, 0, "Producto", header);
            setCellHeader(hp, 1, "Código", header);
            setCellHeader(hp, 2, "Unidades", header);
            setCellHeader(hp, 3, "Total invertido", header);

            int filaProductos = 1;
            for (ProductoCompradoDto p : productos) {
                Row row = prods.createRow(filaProductos++);
                row.createCell(0).setCellValue(p.nombre());
                row.createCell(1).setCellValue(p.codigoBarras() != null ? p.codigoBarras() : "");
                row.createCell(2).setCellValue(p.cantidadTotal());
                row.createCell(3).setCellValue(p.totalInvertido().doubleValue());
                row.getCell(0).setCellStyle(normal);
                row.getCell(1).setCellStyle(normal);
                row.getCell(2).setCellStyle(normal);
                row.getCell(3).setCellStyle(moneda);
            }

            if (filaProductos == 1) {
                Row row = prods.createRow(1);
                row.createCell(0).setCellValue("Sin productos agregados.");
                row.getCell(0).setCellStyle(normal);
            }
            autoSize(prods, 4);

            String nombreProveedor = r.nombreProveedor() != null ? r.nombreProveedor() : "proveedor";
            String sufijo = nombreProveedor.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
            guardarWorkbook(wb, "reporte_compras_" + sufijo + ".xlsx");
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

    private void combinarCeldasYCentrar(XSSFSheet sheet, int filaInicio, int filaFin, int columna, CellStyle style) {
        if (filaFin > filaInicio) {
            sheet.addMergedRegion(new CellRangeAddress(filaInicio, filaFin, columna, columna));
        }
        for (int i = filaInicio; i <= filaFin; i++) {
            Row row = sheet.getRow(i);
            if (row == null) row = sheet.createRow(i);
            org.apache.poi.ss.usermodel.Cell cell = row.getCell(columna);
            if (cell == null) cell = row.createCell(columna);
            cell.setCellStyle(style);
        }
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

    private String textoMetodoPago(MetodoPago metodoPago) {
        if (metodoPago == null) return "—";
        return switch (metodoPago) {
            case EFECTIVO -> "Contado";
            case TRANSFERENCIA -> "Transferencia";
            case CREDITO -> "Crédito";
        };
    }

    private String textoEstadoVenta(EstadoVenta estado) {
        if (estado == null) return "—";
        return switch (estado) {
            case COMPLETADA -> "Completada";
            case ANULADA -> "Anulada";
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

    private void mostrarAlertaInfo(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void abrirModalDetalleVenta(Venta venta) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");

        VBox modal = new VBox(0);
        modal.setMaxWidth(640);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 16px; " +
            "-fx-border-color: rgba(26,31,46,0.10); -fx-border-radius: 16px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 36, 0, 0, 10);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #5A6ACF; -fx-background-radius: 16px 16px 0 0; " +
            "-fx-padding: 16 20 16 20;");

        FontIcon icoH = new FontIcon("fas-file-invoice-dollar");
        icoH.setIconSize(16);
        icoH.setIconColor(Paint.valueOf("#FFFFFF"));

        String comp = venta.getNumeroComprobante() != null
            ? "Compra #" + venta.getNumeroComprobante()
            : "Detalle de compra";
        Label lblH = new Label(comp);
        lblH.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        HBox.setHgrow(lblH, Priority.ALWAYS);

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); " +
            "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        header.getChildren().addAll(icoH, lblH, btnX);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 8, 24));

        Label secResumen = new Label("INFORMACIÓN DE LA COMPRA");
        secResumen.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;");

        VBox resumen = new VBox(8);
        resumen.setStyle("-fx-background-color: #F5F1EB; -fx-background-radius: 10px; -fx-padding: 14 16 14 16;");

        String cliente = venta.getCliente() != null && venta.getCliente().getNombre() != null
            ? venta.getCliente().getNombre()
            : "Venta directa";
        String cedula = venta.getCliente() != null && venta.getCliente().getCedula() != null
            ? venta.getCliente().getCedula()
            : "—";
        String fecha = venta.getFecha() != null ? venta.getFecha().format(DFT) : "—";

        resumen.getChildren().addAll(
            crearFilaDetalleModal("fas-user", "#5A6ACF", "Cliente", cliente),
            crearFilaDetalleModal("fas-id-card", "#78716C", "Cédula", cedula),
            crearFilaDetalleModal("fas-clock", "#D97706", "Fecha y hora", fecha),
            crearFilaDetalleModal("fas-wallet", "#5A6ACF", "Método", textoMetodoPago(venta.getMetodoPago())),
            crearFilaDetalleModal("fas-flag", "#7C3AED", "Estado", textoEstadoVenta(venta.getEstado())),
            crearFilaDetalleModal("fas-dollar-sign", "#15803D", "Total", FMT.format(venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO))
        );

        Label secProductos = new Label("PRODUCTOS COMPRADOS");
        secProductos.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E; -fx-padding: 4 0 0 0;");

        TableView<DetalleVenta> tabla = buildTableView();
        TableColumn<DetalleVenta, String> colProducto = col("Producto",
            d -> d.getProducto() != null && d.getProducto().getNombre() != null ? d.getProducto().getNombre() : "—");
        TableColumn<DetalleVenta, String> colCodigo = col("Código",
            d -> d.getProducto() != null && d.getProducto().getCodigoBarras() != null ? d.getProducto().getCodigoBarras() : "—", 120);
        TableColumn<DetalleVenta, String> colCantidad = col("Unidades", d -> String.valueOf(d.getCantidad()), 90);
        TableColumn<DetalleVenta, String> colPrecio = col("Precio", d -> FMT.format(d.getPrecioUnitario()), 120);
        TableColumn<DetalleVenta, String> colSubtotal = col("Subtotal", d -> FMT.format(d.getSubtotal()), 130);

        colCantidad.setCellFactory(tc -> centeredCell());
        colSubtotal.setCellFactory(tc -> boldColorCell("#15803D"));

        tabla.getColumns().addAll(colProducto, colCodigo, colCantidad, colPrecio, colSubtotal);
        List<DetalleVenta> detalles = venta.getDetalles() != null ? venta.getDetalles() : List.of();
        tabla.setItems(FXCollections.observableArrayList(detalles));
        tabla.setPrefHeight(detalles.isEmpty() ? 140 : Math.min(300, 120 + detalles.size() * 32));

        body.getChildren().addAll(secResumen, resumen, secProductos, tabla);

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroll.getStyleClass().add("inventario-root-stack");
        bodyScroll.setMaxHeight(520);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 24, 20, 24));

        Button btnImprimir = buildBotonPrimario("Imprimir factura", "fas-print", "#15803D");
        boolean ventaCompletada = EstadoVenta.COMPLETADA.equals(venta.getEstado());
        if (!ventaCompletada) {
            btnImprimir.setDisable(true);
            btnImprimir.setTooltip(new Tooltip("Solo las ventas completadas se pueden imprimir."));
        } else {
            btnImprimir.setOnAction(e -> imprimirFacturaDesdeModal(venta));
        }

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 8px; " +
            "-fx-text-fill: #57534E; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        footer.getChildren().addAll(btnImprimir, btnCerrar);

        modal.getChildren().addAll(header, bodyScroll, footer);
        overlay.getChildren().add(modal);
        StackPane.setAlignment(modal, Pos.CENTER);

        Runnable cerrar = () -> cerrarModal(overlay);
        btnX.setOnAction(e -> cerrar.run());
        btnCerrar.setOnAction(e -> cerrar.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrar.run(); });

        rootStack.getChildren().add(overlay);
        animarEntradaModal(overlay, modal);
    }

    private void imprimirFacturaDesdeModal(Venta venta) {
        if (venta == null || venta.getId() == null) {
            mostrarAlertaError("No se pudo identificar la venta a imprimir.");
            return;
        }

        BigDecimal montoRecibido = null;
        if (MetodoPago.EFECTIVO.equals(venta.getMetodoPago())) {
            String sugerido = venta.getTotal() != null ? venta.getTotal().toPlainString() : "";
            TextInputDialog dialog = new TextInputDialog(sugerido);
            dialog.setTitle("Imprimir factura");
            dialog.setHeaderText("Venta en efectivo");
            dialog.setContentText("Monto recibido (opcional):");

            Optional<String> respuesta = dialog.showAndWait();
            if (respuesta.isEmpty()) return;

            String textoMonto = respuesta.get().trim();
            if (!textoMonto.isBlank()) {
                try {
                    montoRecibido = parseMontoMoneda(textoMonto);
                } catch (NumberFormatException ex) {
                    mostrarAlertaError("Ingresa un monto válido para imprimir la factura.");
                    return;
                }

                if (venta.getTotal() != null && montoRecibido.compareTo(venta.getTotal()) < 0) {
                    mostrarAlertaError("El monto recibido no puede ser menor al total de la venta.");
                    return;
                }
            }
        }

        try {
            ventaService.reimprimirTicket(venta.getId(), montoRecibido);
            mostrarAlertaInfo("Impresión", "Factura generada correctamente.");
        } catch (Exception ex) {
            mostrarAlertaError(ex.getMessage() != null ? ex.getMessage() : "No se pudo imprimir la factura.");
        }
    }

    private BigDecimal parseMontoMoneda(String texto) {
        String raw = texto.trim()
                .replace(" ", "")
                .replace(",", "")
                .replace("$", "")
                .replace(".", "");
        if (raw.isBlank()) {
            throw new NumberFormatException("Monto vacío");
        }
        BigDecimal monto = new BigDecimal(raw);
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new NumberFormatException("Monto negativo");
        }
        return monto;
    }

    private HBox crearFilaDetalleModal(String icono, String color, String titulo, String valor) {
        HBox fila = new HBox(10);
        fila.setAlignment(Pos.CENTER_LEFT);

        FontIcon ico = new FontIcon(icono);
        ico.setIconSize(12);
        ico.setIconColor(Paint.valueOf(color));

        Label lblTit = new Label(titulo + ":");
        lblTit.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #57534E;");

        Label lblVal = new Label(valor != null ? valor : "—");
        lblVal.setStyle("-fx-font-size: 12px; -fx-text-fill: #1A1F2E;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        fila.getChildren().addAll(ico, lblTit, spacer, lblVal);
        return fila;
    }

    private void cerrarModal(StackPane overlay) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), overlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);
        fadeOut.setOnFinished(e -> rootStack.getChildren().remove(overlay));
        fadeOut.play();
    }

    private void animarEntradaModal(StackPane overlay, VBox modal) {
        overlay.setOpacity(0);
        modal.setScaleX(0.94);
        modal.setScaleY(0.94);
        modal.setTranslateY(20);

        FadeTransition fadeOverlay = new FadeTransition(Duration.millis(220), overlay);
        fadeOverlay.setFromValue(0);
        fadeOverlay.setToValue(1);
        fadeOverlay.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleModal = new ScaleTransition(Duration.millis(260), modal);
        scaleModal.setFromX(0.94);
        scaleModal.setFromY(0.94);
        scaleModal.setToX(1);
        scaleModal.setToY(1);
        scaleModal.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slideModal = new TranslateTransition(Duration.millis(260), modal);
        slideModal.setFromY(20);
        slideModal.setToY(0);
        slideModal.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fadeOverlay, scaleModal, slideModal).play();
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

    // ─────────────────────────────────────────────────────────────
    // Tab Gastos
    // ─────────────────────────────────────────────────────────────

    private void mostrarGastos() {
        activarTab(tabGastos);
        contentArea.getChildren().clear();

        ScrollPane scroll = buildScrollPane();
        VBox inner = new VBox(24);
        inner.setPadding(new Insets(28, 32, 32, 32));

        VBox selectorCard = buildSelectorCard("fas-file-invoice-dollar", "#DC2626",
                "Reporte consolidado de gastos operativos",
                "Compras registradas como gastos y pagos de facturas / servicios");

        Button btnGenerar = buildBotonPrimario("Generar reporte", "fas-chart-bar", "#DC2626");
        selectorBody(selectorCard).getChildren().add(btnGenerar);

        VBox resultado = new VBox(24);

        Runnable generarReporte = () -> {
            resultado.getChildren().clear();
            try {
                ReporteGastosDto r = reporteService.reporteGastos();
                resultado.getChildren().addAll(buildResultadoGastos(r));
            } catch (Exception ex) {
                resultado.getChildren().add(buildError(ex.getMessage()));
            }
            animarEntrada(resultado, 0);
        };

        btnGenerar.setOnAction(e -> generarReporte.run());

        inner.getChildren().addAll(selectorCard, resultado);

        // Carga automática
        generarReporte.run();

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    @SuppressWarnings("unchecked")
    private List<Node> buildResultadoGastos(ReporteGastosDto r) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(buildResultHeader("Reporte de gastos operativos", () -> exportarGastosExcel(r)));

        // ── KPI resumen ───────────────────────────────────────────
        HBox kpiTop = new HBox(16);
        kpiTop.getChildren().addAll(
            crearKpiCard("fas-file-invoice-dollar", "#DC2626", "Total registros",    String.valueOf(r.totalRegistros())),
            crearKpiCard("fas-dollar-sign",         "#1A1F2E", "Gasto total",        FMT.format(r.montoTotalGastos())),
            crearKpiCard("fas-shopping-bag",        "#5A6ACF", "Compras (gastos)",   String.valueOf(r.totalComprasGasto())),
            crearKpiCard("fas-file-alt",            "#7C3AED", "Pagos",              String.valueOf(r.totalPagos()))
        );
        nodes.add(kpiTop);

        HBox kpiMontos = new HBox(16);
        kpiMontos.getChildren().addAll(
            crearKpiCard("fas-shopping-bag",    "#5A6ACF", "Total en compras",       FMT.format(r.montoComprasGasto())),
            crearKpiCard("fas-receipt",         "#7C3AED", "Total en pagos",         FMT.format(r.montoPagos())),
            crearKpiCard("fas-cash-register",   "#D97706", "Pagado desde caja",      FMT.format(r.montoFuenteCaja())),
            crearKpiCard("fas-exchange-alt",    "#15803D", "Pagado por transferencia", FMT.format(r.montoFuenteTransferencia()))
        );
        nodes.add(kpiMontos);

        // ── Tabla compras-gasto ───────────────────────────────────
        List<GastoDetalleDto> comprasGasto = r.detalles().stream()
                .filter(g -> TipoGasto.COMPRA_GASTO.equals(g.tipo()))
                .toList();
        List<GastoDetalleDto> pagos = r.detalles().stream()
                .filter(g -> TipoGasto.PAGO.equals(g.tipo()))
                .toList();

        if (!comprasGasto.isEmpty()) {
            Label lblCompras = new Label("Compras registradas como gasto");
            lblCompras.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#1A1F2E;");
            nodes.add(lblCompras);
            nodes.add(buildTablaGastos(comprasGasto));
        }

        if (!pagos.isEmpty()) {
            Label lblPagos = new Label("Pagos de facturas y servicios");
            lblPagos.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#1A1F2E;");
            nodes.add(lblPagos);
            nodes.add(buildTablaGastos(pagos));
        }

        if (r.totalRegistros() == 0) {
            nodes.add(buildBanner("fas-info-circle",
                    "No hay gastos registrados todavía. Regístralos en el módulo de Gastos.",
                    "#ECEEF7", "#5A6ACF"));
        }

        return nodes;
    }

    private TableView<GastoDetalleDto> buildTablaGastos(List<GastoDetalleDto> lista) {
        TableView<GastoDetalleDto> tabla = buildTableView();
        tabla.setPrefHeight(Math.min(lista.size() * 42 + 44, 380));

        tabla.getColumns().addAll(
            col("Fecha",      g -> g.fecha() != null ? g.fecha().format(DFT) : "—", 140),
            col("Concepto",   g -> g.concepto(), 190),
            col("Categoría",  g -> g.categoria() != null ? g.categoria() : "—", 110),
            col("Fuente",     g -> g.fuentePago() != null
                    ? (g.fuentePago().name().equals("CAJA") ? "Caja" : "Transferencia") : "—", 110),
            col("Proveedor / Entidad", g -> g.proveedor() != null ? g.proveedor() : "—", 140),
            col("Referencia", g -> g.referencia() != null ? g.referencia() : "—", 100),
            col("Monto",      g -> FMT.format(g.monto() != null ? g.monto() : java.math.BigDecimal.ZERO), 120)
        );

        TableColumn<GastoDetalleDto, String> colMonto =
                (TableColumn<GastoDetalleDto, String>) tabla.getColumns().get(6);
        colMonto.setCellFactory(tc -> boldColorCell("#DC2626"));

        tabla.getItems().addAll(lista);
        return tabla;
    }

    private void exportarGastosExcel(ReporteGastosDto r) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Guardar reporte de gastos");
        fc.setInitialFileName("reporte_gastos.xlsx");
        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        java.io.File file = fc.showSaveDialog(contentArea.getScene().getWindow());
        if (file == null) return;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Gastos");
            String[] headers = {"Fecha", "Tipo", "Fuente", "Concepto", "Categoría", "Proveedor/Entidad", "Referencia", "Monto", "Notas", "Usuario"};

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 220, 38, 38}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 1;
            for (GastoDetalleDto g : r.detalles()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(g.fecha() != null ? g.fecha().format(dtf) : "");
                row.createCell(1).setCellValue(g.tipo() == TipoGasto.COMPRA_GASTO ? "Compra" : "Pago");
                row.createCell(2).setCellValue(g.fuentePago() != null
                        ? (g.fuentePago().name().equals("CAJA") ? "Caja" : "Transferencia") : "");
                row.createCell(3).setCellValue(g.concepto() != null ? g.concepto() : "");
                row.createCell(4).setCellValue(g.categoria() != null ? g.categoria() : "");
                row.createCell(5).setCellValue(g.proveedor() != null ? g.proveedor() : "");
                row.createCell(6).setCellValue(g.referencia() != null ? g.referencia() : "");
                row.createCell(7).setCellValue(g.monto() != null ? g.monto().doubleValue() : 0);
                row.createCell(8).setCellValue(g.notas() != null ? g.notas() : "");
                row.createCell(9).setCellValue(g.usuarioNombre() != null ? g.usuarioNombre() : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                wb.write(fos);
            }

            mostrarAlerta("Exportado", "El reporte se guardó en:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo exportar el archivo: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
