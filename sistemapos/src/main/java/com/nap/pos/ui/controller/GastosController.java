package com.nap.pos.ui.controller;

import com.nap.pos.application.service.GastoService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Gasto;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.FuenteGasto;
import com.nap.pos.domain.model.enums.TipoGasto;
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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GastosController {

    private final GastoService gastoService;

    private static final NumberFormat      FMT = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
    private static final DateTimeFormatter DFT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Estado ────────────────────────────────────────────────────
    private Usuario usuarioActual;
    private List<Gasto> comprasGasto   = new ArrayList<>();
    private List<Gasto> pagos          = new ArrayList<>();

    // ── Refs UI ───────────────────────────────────────────────────
    private StackPane rootStack;
    private VBox      contentArea;
    private Button    tabCompras;
    private Button    tabPagos;
    private Button    btnAccion;

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

        contentArea = new VBox();
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        root.getChildren().addAll(buildTabBar(), contentArea);
        rootStack.getChildren().add(root);

        mostrarCompras();
        return rootStack;
    }

    private void recargarDatos() {
        try {
            comprasGasto = gastoService.findByTipo(TipoGasto.COMPRA_GASTO);
            pagos        = gastoService.findByTipo(TipoGasto.PAGO);
        } catch (Exception e) {
            comprasGasto = new ArrayList<>();
            pagos        = new ArrayList<>();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Tab bar
    // ─────────────────────────────────────────────────────────────

    private HBox buildTabBar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("inventario-tab-bar");
        bar.setPadding(new Insets(0, 20, 0, 20));
        bar.setMinHeight(52);

        FontIcon icoCompras = new FontIcon("fas-shopping-bag");
        icoCompras.setIconSize(14);
        icoCompras.setIconColor(Paint.valueOf("#5A6ACF"));
        tabCompras = new Button("Compras", icoCompras);
        tabCompras.getStyleClass().addAll("inventario-tab", "inventario-tab-active");
        tabCompras.setOnAction(e -> { animarClickTab(tabCompras); mostrarCompras(); });

        FontIcon icoPagos = new FontIcon("fas-file-invoice-dollar");
        icoPagos.setIconSize(14);
        icoPagos.setIconColor(Paint.valueOf("#78716C"));
        tabPagos = new Button("Pagos", icoPagos);
        tabPagos.getStyleClass().add("inventario-tab");
        tabPagos.setOnAction(e -> { animarClickTab(tabPagos); mostrarPagos(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Botón de acción contextual: cambia según la pestaña activa
        FontIcon icoAccion = new FontIcon("fas-plus");
        icoAccion.setIconSize(13);
        icoAccion.setIconColor(Paint.valueOf("#FFFFFF"));
        btnAccion = new Button("Registrar Compra", icoAccion);
        btnAccion.getStyleClass().add("btn-primario");
        btnAccion.setStyle(
            "-fx-padding: 8 18 8 18; -fx-font-size: 13px; -fx-font-weight: 600;" +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        );
        btnAccion.setOnAction(e -> abrirModalNuevoGasto(TipoGasto.COMPRA_GASTO));

        bar.getChildren().addAll(tabCompras, tabPagos, spacer, btnAccion);
        return bar;
    }

    private void activarTab(Button activo) {
        tabCompras.getStyleClass().remove("inventario-tab-active");
        tabPagos.getStyleClass().remove("inventario-tab-active");
        activo.getStyleClass().add("inventario-tab-active");

        String inactivo = "#78716C";
        String actColor = "#5A6ACF";
        ((FontIcon) tabCompras.getGraphic()).setIconColor(Paint.valueOf(activo == tabCompras ? actColor : inactivo));
        ((FontIcon) tabPagos.getGraphic()).setIconColor(Paint.valueOf(activo == tabPagos ? actColor : inactivo));

        // Actualiza el botón de acción según la pestaña activa
        if (activo == tabCompras) {
            btnAccion.setText("Registrar Compra");
            btnAccion.setOnAction(e -> abrirModalNuevoGasto(TipoGasto.COMPRA_GASTO));
        } else {
            btnAccion.setText("Registrar Pago");
            btnAccion.setOnAction(e -> abrirModalNuevoGasto(TipoGasto.PAGO));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Vista: Compras (gastos tipo COMPRA_GASTO)
    // ─────────────────────────────────────────────────────────────

    private void mostrarCompras() {
        activarTab(tabCompras);
        recargarDatos();
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dashboard-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox inner = new VBox(28);
        inner.setPadding(new Insets(28, 32, 32, 32));

        // ── KPI cards ─────────────────────────────────────────────
        BigDecimal totalGastadoCompras = comprasGasto.stream()
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long categoriaDistintas = comprasGasto.stream()
                .filter(g -> g.getCategoria() != null && !g.getCategoria().isBlank())
                .map(Gasto::getCategoria)
                .distinct().count();

        BigDecimal promedioCompra = comprasGasto.isEmpty() ? BigDecimal.ZERO
                : totalGastadoCompras.divide(BigDecimal.valueOf(comprasGasto.size()), 2, java.math.RoundingMode.HALF_UP);

        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
            crearKpiCard("fas-shopping-bag",    "#5A6ACF", "Total compras",      String.valueOf(comprasGasto.size())),
            crearKpiCard("fas-dollar-sign",     "#DC2626", "Total gastado",      FMT.format(totalGastadoCompras)),
            crearKpiCard("fas-tags",            "#D97706", "Categorías",         String.valueOf(categoriaDistintas)),
            crearKpiCard("fas-chart-line",      "#15803D", "Promedio por compra", FMT.format(promedioCompra))
        );

        // ── Gráfica gasto mensual ─────────────────────────────────
        HBox chartsRow = new HBox(16);
        VBox chartGasto      = crearGastoMensualCard(comprasGasto, "GASTO EN COMPRAS", "#5A6ACF");
        VBox chartCategoria  = crearTopCategoriasCard(comprasGasto, "TOP CATEGORÍAS");
        HBox.setHgrow(chartGasto,     Priority.ALWAYS);
        HBox.setHgrow(chartCategoria, Priority.ALWAYS);
        chartsRow.getChildren().addAll(chartGasto, chartCategoria);

        // ── Recientes ─────────────────────────────────────────────
        Label lblRecientes = new Label("Compras recientes");
        lblRecientes.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        VBox listaRecientes = crearListaRecientes(comprasGasto, "fas-shopping-bag", "#5A6ACF");

        // ── Historial completo ────────────────────────────────────
        Label lblHistorial = new Label("Historial completo");
        lblHistorial.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        TableView<Gasto> tabla = crearTablaGastos(comprasGasto);

        inner.getChildren().addAll(
                kpiRow, new Separator(),
                chartsRow, new Separator(),
                lblRecientes, listaRecientes,
                lblHistorial, tabla
        );

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    // ─────────────────────────────────────────────────────────────
    // Vista: Pagos (gastos tipo PAGO)
    // ─────────────────────────────────────────────────────────────

    private void mostrarPagos() {
        activarTab(tabPagos);
        recargarDatos();
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dashboard-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox inner = new VBox(28);
        inner.setPadding(new Insets(28, 32, 32, 32));

        // ── KPI cards ─────────────────────────────────────────────
        BigDecimal totalPagado = pagos.stream()
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long categoriasPagos = pagos.stream()
                .filter(g -> g.getCategoria() != null && !g.getCategoria().isBlank())
                .map(Gasto::getCategoria)
                .distinct().count();

        BigDecimal promedioPago = pagos.isEmpty() ? BigDecimal.ZERO
                : totalPagado.divide(BigDecimal.valueOf(pagos.size()), 2, java.math.RoundingMode.HALF_UP);

        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
            crearKpiCard("fas-file-invoice-dollar", "#7C3AED", "Total pagos",      String.valueOf(pagos.size())),
            crearKpiCard("fas-dollar-sign",         "#DC2626", "Total pagado",     FMT.format(totalPagado)),
            crearKpiCard("fas-tags",                "#D97706", "Categorías",       String.valueOf(categoriasPagos)),
            crearKpiCard("fas-chart-line",          "#15803D", "Promedio por pago", FMT.format(promedioPago))
        );

        // ── Gráfica pago mensual ──────────────────────────────────
        HBox chartsRow = new HBox(16);
        VBox chartPagos      = crearGastoMensualCard(pagos, "PAGOS MENSUALES", "#7C3AED");
        VBox chartCategoria  = crearTopCategoriasCard(pagos, "CATEGORÍAS DE PAGOS");
        HBox.setHgrow(chartPagos,     Priority.ALWAYS);
        HBox.setHgrow(chartCategoria, Priority.ALWAYS);
        chartsRow.getChildren().addAll(chartPagos, chartCategoria);

        // ── Recientes ─────────────────────────────────────────────
        Label lblRecientes = new Label("Pagos recientes");
        lblRecientes.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        VBox listaRecientes = crearListaRecientes(pagos, "fas-file-invoice-dollar", "#7C3AED");

        // ── Historial completo ────────────────────────────────────
        Label lblHistorial = new Label("Historial completo");
        lblHistorial.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        TableView<Gasto> tabla = crearTablaGastos(pagos);

        inner.getChildren().addAll(
                kpiRow, new Separator(),
                chartsRow, new Separator(),
                lblRecientes, listaRecientes,
                lblHistorial, tabla
        );

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    // ─────────────────────────────────────────────────────────────
    // Modal: Nuevo Gasto / Pago
    // ─────────────────────────────────────────────────────────────

    private void abrirModalNuevoGasto(TipoGasto tipo) {
        boolean esCompra = tipo == TipoGasto.COMPRA_GASTO;
        String tituloModal = esCompra ? "Registrar Compra (Gasto)" : "Registrar Pago";
        String iconModal   = esCompra ? "fas-shopping-bag" : "fas-file-invoice-dollar";
        String colorModal  = esCompra ? "#5A6ACF" : "#7C3AED";

        String[] categoriasCompra = {"Suministros", "Papelería", "Limpieza", "Herramientas", "Mantenimiento", "Tecnología", "Otro"};
        String[] categoriasPago   = {"Arriendo", "Electricidad", "Agua", "Internet", "Teléfono", "Nómina", "Impuesto", "Seguro", "Otro"};
        String[] categorias = esCompra ? categoriasCompra : categoriasPago;

        // ── Overlay ──────────────────────────────────────────────
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(25,32,48,0.55);");
        overlay.setAlignment(Pos.CENTER);

        // ── Panel modal — ancho responsivo ligado al overlay ──────
        VBox panel = new VBox(20);
        panel.setMinWidth(300);
        // Se adapta al overlay: máximo 500 px o el ancho disponible - 64 px de margen
        panel.prefWidthProperty().bind(
            Bindings.when(overlay.widthProperty().subtract(64).lessThan(500))
                    .then(overlay.widthProperty().subtract(64))
                    .otherwise(500.0)
        );
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setPadding(new Insets(28, 32, 28, 32));
        panel.setStyle(
            "-fx-background-color: #FDFCFA;" +
            "-fx-background-radius: 14px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 28, 0, 0, 8);"
        );

        // Header modal
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: " + colorModal + "22; -fx-background-radius: 10px;");
        icoCircle.setMinSize(40, 40);
        icoCircle.setMaxSize(40, 40);
        FontIcon icoH = new FontIcon(iconModal);
        icoH.setIconSize(18);
        icoH.setIconColor(Paint.valueOf(colorModal));
        icoCircle.getChildren().add(icoH);

        VBox titulos = new VBox(2);
        Label lblTitulo = new Label(tituloModal);
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        Label lblSub = new Label(esCompra ? "Compra que se registra como gasto operativo" : "Pago de factura, servicio o cuota");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");
        titulos.getChildren().addAll(lblTitulo, lblSub);

        Region spacerH = new Region();
        HBox.setHgrow(spacerH, Priority.ALWAYS);

        Button btnX = new Button("✕");
        btnX.setStyle(
            "-fx-background-color: transparent; -fx-font-size: 15px;" +
            "-fx-text-fill: #94A3B8; -fx-cursor: hand; -fx-padding: 4 8;"
        );

        header.getChildren().addAll(icoCircle, titulos, spacerH, btnX);

        // Formulario
        VBox form = new VBox(14);

        // Concepto
        VBox grpConcepto = buildCampo("Concepto *", "Ej: Compra de papel, pago de electricidad...");
        TextField txtConcepto = (TextField) ((VBox) grpConcepto).getChildren().get(1);

        // Categoría
        VBox grpCategoria = new VBox(6);
        Label lblCat = new Label("Categoría *");
        lblCat.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");
        ComboBox<String> cbCategoria = new ComboBox<>(FXCollections.observableArrayList(categorias));
        cbCategoria.setPromptText("Selecciona una categoría");
        cbCategoria.setMaxWidth(Double.MAX_VALUE);
        cbCategoria.setStyle("-fx-font-size: 13px;");
        grpCategoria.getChildren().addAll(lblCat, cbCategoria);

        // Monto
        VBox grpMonto = buildCampo("Monto *", "0.00");
        TextField txtMonto = (TextField) ((VBox) grpMonto).getChildren().get(1);

        // Proveedor / Entidad y Referencia — fila horizontal que respeta el ancho del panel
        HBox fila2 = new HBox(12);
        fila2.setFillHeight(true);

        VBox grpProveedor = buildCampo(esCompra ? "Proveedor / Tienda" : "Entidad / Empresa", "Nombre opcional");
        TextField txtProveedor = (TextField) ((VBox) grpProveedor).getChildren().get(1);
        txtProveedor.setMinWidth(0);
        txtProveedor.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(grpProveedor, Priority.ALWAYS);

        VBox grpReferencia = buildCampo("No. Factura / Ref.", "Referencia opcional");
        TextField txtReferencia = (TextField) ((VBox) grpReferencia).getChildren().get(1);
        txtReferencia.setMinWidth(0);
        txtReferencia.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(grpReferencia, Priority.ALWAYS);

        fila2.getChildren().addAll(grpProveedor, grpReferencia);

        // Fuente de pago
        VBox grpFuente = new VBox(6);
        Label lblFuente = new Label("Fuente de pago *");
        lblFuente.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");
        ComboBox<FuenteGasto> cbFuente = new ComboBox<>(FXCollections.observableArrayList(FuenteGasto.values()));
        cbFuente.setPromptText("¿De dónde salió el dinero?");
        cbFuente.setMaxWidth(Double.MAX_VALUE);
        cbFuente.setStyle("-fx-font-size: 13px;");
        cbFuente.setConverter(new StringConverter<>() {
            @Override public String toString(FuenteGasto f) {
                if (f == null) return "";
                return f == FuenteGasto.CAJA ? "Caja" : "Transferencia";
            }
            @Override public FuenteGasto fromString(String s) { return null; }
        });
        grpFuente.getChildren().addAll(lblFuente, cbFuente);

        // Notas
        VBox grpNotas = new VBox(6);
        Label lblNotas = new Label("Notas");
        lblNotas.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");
        TextArea taNotas = new TextArea();
        taNotas.setPromptText("Observaciones adicionales...");
        taNotas.setPrefRowCount(4);
        taNotas.setWrapText(true);
        taNotas.setStyle("-fx-font-size: 13px; -fx-background-color: #EDE9E2; -fx-background-radius: 8px; -fx-border-color: transparent;");
        grpNotas.getChildren().addAll(lblNotas, taNotas);

        form.getChildren().addAll(grpConcepto, grpCategoria, grpMonto, fila2, grpFuente, grpNotas);

        // Label error
        Label lblError = new Label();
        lblError.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");
        lblError.setWrapText(true);
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Botones
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #4B5563;" +
            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 18;" +
            "-fx-border-color: rgba(26,31,46,0.15); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;"
        );

        FontIcon icoGuardar = new FontIcon("fas-save");
        icoGuardar.setIconSize(13);
        icoGuardar.setIconColor(Paint.valueOf("#FFFFFF"));
        Button btnGuardar = new Button("Guardar", icoGuardar);
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 13px;");

        botones.getChildren().addAll(btnCancelar, btnGuardar);

        panel.getChildren().addAll(header, new Separator(), form, lblError, botones);

        // El panel scroll si el contenido es demasiado alto para la ventana
        ScrollPane panelScroll = new ScrollPane(panel);
        panelScroll.setFitToWidth(true);
        panelScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panelScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        panelScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        panelScroll.setMinWidth(300);
        panelScroll.prefWidthProperty().bind(panel.prefWidthProperty());
        panelScroll.maxWidthProperty().bind(panel.prefWidthProperty());
        panelScroll.maxHeightProperty().bind(overlay.heightProperty().subtract(48));

        overlay.getChildren().add(panelScroll);

        // Cerrar
        Runnable cerrar = () -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), overlay);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(ev -> rootStack.getChildren().remove(overlay));
            ft.play();
        };

        btnX.setOnAction(e -> cerrar.run());
        btnCancelar.setOnAction(e -> cerrar.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrar.run(); });

        btnGuardar.setOnAction(e -> {
            lblError.setVisible(false);
            lblError.setManaged(false);

            String concepto   = txtConcepto.getText().trim();
            String categoria  = cbCategoria.getValue();
            String montoTxt   = txtMonto.getText().trim().replace(",", ".");
            String proveedor  = txtProveedor.getText().trim();
            String referencia = txtReferencia.getText().trim();
            String notas      = taNotas.getText().trim();

            FuenteGasto fuentePago = cbFuente.getValue();

            if (concepto.isBlank())  { mostrarError(lblError, "El concepto es obligatorio."); return; }
            if (categoria == null)   { mostrarError(lblError, "Selecciona una categoría."); return; }
            if (fuentePago == null)  { mostrarError(lblError, "Selecciona la fuente de pago."); return; }

            BigDecimal monto;
            try {
                monto = new BigDecimal(montoTxt);
                if (monto.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                mostrarError(lblError, "Ingresa un monto válido mayor a cero.");
                return;
            }

            try {
                gastoService.registrar(
                    usuarioActual.getId(), tipo, fuentePago, concepto, categoria, monto,
                    proveedor.isBlank() ? null : proveedor,
                    referencia.isBlank() ? null : referencia,
                    notas.isBlank() ? null : notas
                );
                cerrar.run();
                if (tipo == TipoGasto.COMPRA_GASTO) mostrarCompras();
                else mostrarPagos();
            } catch (BusinessException ex) {
                mostrarError(lblError, ex.getMessage());
            } catch (Exception ex) {
                mostrarError(lblError, "Error al guardar. Intenta de nuevo.");
            }
        });

        // Animación entrada
        rootStack.getChildren().add(overlay);
        overlay.setOpacity(0);
        panelScroll.setScaleX(0.93);
        panelScroll.setScaleY(0.93);

        FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
        ft.setFromValue(0);
        ft.setToValue(1);

        ScaleTransition st = new ScaleTransition(Duration.millis(220), panelScroll);
        st.setFromX(0.93);
        st.setToX(1.0);
        st.setFromY(0.93);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, st).play();
    }

    private void mostrarError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private VBox buildCampo(String labelText, String prompt) {
        VBox grp = new VBox(6);
        VBox.setVgrow(grp, Priority.NEVER);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");
        TextField txt = new TextField();
        txt.setPromptText(prompt);
        txt.setMaxWidth(Double.MAX_VALUE);
        txt.setMinWidth(0);
        txt.setStyle(
            "-fx-font-size: 13px; -fx-background-color: #EDE9E2;" +
            "-fx-background-radius: 8px; -fx-border-color: transparent; -fx-padding: 8 12;"
        );
        grp.getChildren().addAll(lbl, txt);
        return grp;
    }

    // ─────────────────────────────────────────────────────────────
    // Componentes UI reutilizables
    // ─────────────────────────────────────────────────────────────

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

    private VBox crearGastoMensualCard(List<Gasto> lista, String titulo, String color) {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox headerChart = new HBox(8);
        headerChart.setAlignment(Pos.CENTER_LEFT);
        Label lTit = new Label(titulo);
        lTit.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);
        FontIcon ico = new FontIcon("fas-chart-bar");
        ico.setIconSize(14);
        ico.setIconColor(Paint.valueOf("#94A3B8"));
        headerChart.getChildren().addAll(lTit, ico);

        DateTimeFormatter mesYFmt = DateTimeFormatter.ofPattern("MMM yy", Locale.of("es", "CO"));
        YearMonth ahora = YearMonth.now();
        Map<String, BigDecimal> gastoMes = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            String mes = capitalizarEtiqueta(ahora.minusMonths(i).format(mesYFmt));
            gastoMes.put(mes, BigDecimal.ZERO);
        }
        for (Gasto g : lista) {
            if (g.getFecha() == null) continue;
            String mes = capitalizarEtiqueta(YearMonth.from(g.getFecha()).format(mesYFmt));
            if (gastoMes.containsKey(mes)) {
                gastoMes.merge(mes, g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO, BigDecimal::add);
            }
        }

        boolean sinDatos = gastoMes.values().stream().allMatch(v -> v.compareTo(BigDecimal.ZERO) == 0);
        if (sinDatos) {
            card.getChildren().addAll(headerChart, buildChartEmptyState("fas-calendar-alt",
                    "Sin registros recientes", "Registra gastos para ver el historial mensual"));
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        xAxis.setCategories(FXCollections.observableArrayList(gastoMes.keySet()));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setLabel("");
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number n) { return formatearMonedaCorta(n); }
            @Override public Number fromString(String s) { return 0; }
        });

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.getStyleClass().add("inventario-bar-chart");
        chart.setPrefHeight(220);
        VBox.setVgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        gastoMes.forEach((mes, valor) -> series.getData().add(new XYChart.Data<>(mes, valor.doubleValue())));
        chart.getData().add(series);

        card.getChildren().addAll(headerChart, chart);
        return card;
    }

    private VBox crearTopCategoriasCard(List<Gasto> lista, String titulo) {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox headerChart = new HBox(8);
        headerChart.setAlignment(Pos.CENTER_LEFT);
        Label lTit = new Label(titulo);
        lTit.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);
        FontIcon ico = new FontIcon("fas-tags");
        ico.setIconSize(14);
        ico.setIconColor(Paint.valueOf("#94A3B8"));
        headerChart.getChildren().addAll(lTit, ico);

        Map<String, BigDecimal> porCategoria = lista.stream()
                .filter(g -> g.getCategoria() != null && !g.getCategoria().isBlank() && g.getMonto() != null)
                .collect(Collectors.groupingBy(
                        Gasto::getCategoria,
                        Collectors.reducing(BigDecimal.ZERO, Gasto::getMonto, BigDecimal::add)));

        if (porCategoria.isEmpty()) {
            card.getChildren().addAll(headerChart, buildChartEmptyState("fas-tags",
                    "Sin categorías registradas", "Las categorías aparecerán aquí cuando registres gastos"));
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setLabel("");
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number n) { return formatearMonedaCorta(n); }
            @Override public Number fromString(String s) { return 0; }
        });

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.getStyleClass().add("inventario-bar-chart");
        chart.setPrefHeight(220);
        VBox.setVgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<String> etiquetas = new ArrayList<>();
        porCategoria.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(6)
                .forEach(entry -> {
                    String cat = entry.getKey().length() > 8 ? entry.getKey().substring(0, 7) + "." : entry.getKey();
                    etiquetas.add(cat);
                    series.getData().add(new XYChart.Data<>(cat, entry.getValue().doubleValue()));
                });
        xAxis.setCategories(FXCollections.observableArrayList(etiquetas));
        chart.getData().add(series);

        card.getChildren().addAll(headerChart, chart);
        return card;
    }

    private VBox crearListaRecientes(List<Gasto> lista, String iconLit, String color) {
        VBox box = new VBox(8);
        List<Gasto> recientes = lista.stream()
                .sorted(Comparator.comparing(Gasto::getFecha, Comparator.reverseOrder()))
                .limit(8)
                .toList();

        if (recientes.isEmpty()) {
            Label lv = new Label("Sin registros todavía.");
            lv.setStyle("-fx-font-size: 13px; -fx-text-fill: #A8A29E;");
            lv.setPadding(new Insets(12, 0, 0, 0));
            box.getChildren().add(lv);
        } else {
            for (int i = 0; i < recientes.size(); i++) {
                HBox row = crearFilaGasto(recientes.get(i), iconLit, color);
                box.getChildren().add(row);
                animarEntrada(row, i * 30);
            }
        }
        return box;
    }

    private HBox crearFilaGasto(Gasto g, String iconLit, String color) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
            "-fx-background-color: #FDFCFA; -fx-background-radius: 10px;" +
            "-fx-border-color: rgba(26,31,46,0.08); -fx-border-width: 1;" +
            "-fx-border-radius: 10px; -fx-padding: 12 16 12 16;"
        );
        row.setOnMouseEntered(e -> row.setStyle(row.getStyle().replace("#FDFCFA", "#F5F1EB")));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("#F5F1EB", "#FDFCFA")));

        StackPane ico = new StackPane();
        ico.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 8px;");
        ico.setMinSize(34, 34);
        ico.setMaxSize(34, 34);
        FontIcon fi = new FontIcon(iconLit);
        fi.setIconSize(14);
        fi.setIconColor(Paint.valueOf(color));
        ico.getChildren().add(fi);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lConcepto = new Label(g.getConcepto());
        lConcepto.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1A1F2E;");

        String sub = g.getCategoria() != null ? g.getCategoria() : "";
        if (g.getProveedor() != null && !g.getProveedor().isBlank()) {
            sub = sub.isBlank() ? g.getProveedor() : sub + " · " + g.getProveedor();
        }
        Label lSub = new Label(sub.isBlank() ? "Sin categoría" : sub);
        lSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716C;");
        info.getChildren().addAll(lConcepto, lSub);

        VBox montoBox = new VBox(2);
        montoBox.setAlignment(Pos.CENTER_RIGHT);

        Label lMonto = new Label(FMT.format(g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO));
        lMonto.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");

        Label lFecha = new Label(g.getFecha() != null ? g.getFecha().format(DFT) : "");
        lFecha.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
        montoBox.getChildren().addAll(lMonto, lFecha);

        row.getChildren().addAll(ico, info, montoBox);
        return row;
    }

    @SuppressWarnings("unchecked")
    private TableView<Gasto> crearTablaGastos(List<Gasto> lista) {
        TableView<Gasto> tabla = new TableView<>();
        tabla.getStyleClass().add("inventario-table");
        tabla.setPrefHeight(320);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(buildPlaceholder("Sin registros para mostrar"));

        TableColumn<Gasto, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFecha() != null ? c.getValue().getFecha().format(DFT) : ""));
        colFecha.setPrefWidth(140);

        TableColumn<Gasto, String> colConcepto = new TableColumn<>("Concepto");
        colConcepto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConcepto()));
        colConcepto.setPrefWidth(200);

        TableColumn<Gasto, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategoria() != null ? c.getValue().getCategoria() : "—"));
        colCategoria.setPrefWidth(120);

        TableColumn<Gasto, String> colProveedor = new TableColumn<>("Proveedor / Entidad");
        colProveedor.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProveedor() != null ? c.getValue().getProveedor() : "—"));
        colProveedor.setPrefWidth(150);

        TableColumn<Gasto, String> colRef = new TableColumn<>("Referencia");
        colRef.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReferencia() != null ? c.getValue().getReferencia() : "—"));
        colRef.setPrefWidth(110);

        TableColumn<Gasto, String> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(c -> new SimpleStringProperty(
                FMT.format(c.getValue().getMonto() != null ? c.getValue().getMonto() : BigDecimal.ZERO)));
        colMonto.setPrefWidth(120);
        colMonto.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Gasto, Void> colAcciones = new TableColumn<>("");
        colAcciones.setPrefWidth(60);
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnDel = new Button();
            {
                FontIcon icoD = new FontIcon("fas-trash-alt");
                icoD.setIconSize(12);
                icoD.setIconColor(Paint.valueOf("#DC2626"));
                btnDel.setGraphic(icoD);
                btnDel.setStyle(
                    "-fx-background-color: #FEE2E2; -fx-background-radius: 6px;" +
                    "-fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 5 7;"
                );
                btnDel.setOnAction(e -> {
                    Gasto g = getTableView().getItems().get(getIndex());
                    confirmarEliminar(g);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDel);
            }
        });

        tabla.getItems().addAll(lista.stream()
                .sorted(Comparator.comparing(Gasto::getFecha, Comparator.reverseOrder()))
                .toList());
        tabla.getColumns().addAll(colFecha, colConcepto, colCategoria, colProveedor, colRef, colMonto, colAcciones);
        return tabla;
    }

    private void confirmarEliminar(Gasto g) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar registro");
        alert.setHeaderText(null);
        alert.setContentText("¿Eliminar el gasto \"" + g.getConcepto() + "\" por " + FMT.format(g.getMonto()) + "?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    gastoService.eliminar(g.getId());
                    if (g.getTipo() == TipoGasto.COMPRA_GASTO) mostrarCompras();
                    else mostrarPagos();
                } catch (Exception ex) {
                    mostrarAlerta("Error", "No se pudo eliminar el registro.");
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Utilidades UI
    // ─────────────────────────────────────────────────────────────

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

    private Label buildPlaceholder(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #A8A29E;");
        return l;
    }

    private String capitalizarEtiqueta(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        return texto.substring(0, 1).toUpperCase(Locale.of("es", "CO")) + texto.substring(1);
    }

    private String formatearMonedaCorta(Number valor) {
        if (valor == null) return "$0";
        double numero = valor.doubleValue();
        double abs = Math.abs(numero);
        if (abs >= 1_000_000_000d) return String.format(Locale.of("es", "CO"), "$%.1fB", numero / 1_000_000_000d);
        if (abs >= 1_000_000d)     return String.format(Locale.of("es", "CO"), "$%.1fM", numero / 1_000_000d);
        if (abs >= 1_000d)         return String.format(Locale.of("es", "CO"), "$%.0fk", numero / 1_000d);
        return String.format(Locale.of("es", "CO"), "$%.0f", numero);
    }

    private void animarEntrada(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(12);

        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));
        ft.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), node);
        tt.setFromY(12);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));
        tt.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, tt).play();
    }

    private void animarClickTab(Button btn) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), btn);
        st.setFromX(1.0);
        st.setToX(0.94);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
