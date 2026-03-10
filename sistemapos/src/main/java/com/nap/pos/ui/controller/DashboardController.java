package com.nap.pos.ui.controller;

import com.nap.pos.application.service.CajaService;
import com.nap.pos.application.service.ClienteService;
import com.nap.pos.application.service.NotificacionService;
import com.nap.pos.application.service.ProductoService;
import com.nap.pos.application.service.VentaService;
import com.nap.pos.domain.model.Notificacion;
import com.nap.pos.domain.model.Venta;
import com.nap.pos.domain.model.enums.EstadoVenta;
import com.nap.pos.domain.model.enums.MetodoPago;
import com.nap.pos.domain.model.enums.TipoNotificacion;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Construye y gestiona la vista del Dashboard.
 * Separado de MainWindowController para mantener cada clase con
 * una sola responsabilidad.
 */
@Component
@RequiredArgsConstructor
public class DashboardController {

    private final VentaService        ventaService;
    private final ProductoService     productoService;
    private final ClienteService      clienteService;
    private final CajaService         cajaService;
    private final NotificacionService notificacionService;

    /**
     * Construye y retorna la vista completa del dashboard.
     *
     * @param onVerHistorial acción ejecutada al presionar "Ver historial completo"
     */
    public ScrollPane buildView(Runnable onVerHistorial) {

        // ── Recopila datos ────────────────────────────────────────
        int  totalProductos = 0;
        int  sinStock       = 0;
        int  totalClientes  = 0;
        boolean cajaAbierta = false;
        List<Notificacion> notifs      = List.of();
        List<Venta>        todasVentas = List.of();

        try { totalProductos = productoService.findAllActivos().size(); }         catch (Exception ignored) {}
        try {
            sinStock = (int) productoService.findAllActivos().stream()
                    .filter(p -> p.getStock() == 0).count();
        } catch (Exception ignored) {}
        try { totalClientes = clienteService.findAll().size(); }                  catch (Exception ignored) {}
        try { cajaService.getCajaAbierta(); cajaAbierta = true; }                 catch (Exception ignored) {}
        try { notifs      = notificacionService.getNotificaciones(); }            catch (Exception ignored) {}
        try { todasVentas = ventaService.findAll(); }                             catch (Exception ignored) {}

        LocalDate hoy = LocalDate.now();
        final List<Venta> ventasHoy = todasVentas.stream()
                .filter(v -> v.getFecha() != null && v.getFecha().toLocalDate().equals(hoy))
                .filter(v -> EstadoVenta.COMPLETADA.equals(v.getEstado()))
                .collect(Collectors.toList());
        BigDecimal totalHoy = ventasHoy.stream()
                .map(Venta::getTotal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Venta> ultimasVentas = todasVentas.stream()
                .sorted(Comparator.comparing(
                        v -> v.getFecha() != null ? v.getFecha()
                                : java.time.LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());

        // ── Layout raíz ──────────────────────────────────────────
        VBox root = new VBox(24);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("dashboard-root");

        // ── Fila 1: 4 stat cards ─────────────────────────────────
        HBox cardsRow = new HBox(16);
        cardsRow.setAlignment(Pos.TOP_LEFT);

        VBox c1 = crearStatCard("Productos Activos",
                String.valueOf(totalProductos), "Total en catálogo",
                null, null, "fas-boxes", "#5A6ACF", "rgba(90,106,207,0.10)");

        VBox c2 = crearStatCard("Sin Stock",
                String.valueOf(sinStock), null,
                sinStock > 0 ? "Alerta" : "OK",
                sinStock > 0 ? "dash-badge-danger" : "dash-badge-success",
                "fas-exclamation-triangle",
                sinStock > 0 ? "#DC2626" : "#16A34A",
                sinStock > 0 ? "#FEE2E2" : "#DCFCE7");

        VBox c3 = crearStatCard("Clientes Registrados",
                String.valueOf(totalClientes), null,
                null, null, "fas-users", "#16A34A", "#DCFCE7");

        VBox c4 = crearStatCardCaja(cajaAbierta);

        for (VBox c : List.of(c1, c2, c3, c4)) {
            HBox.setHgrow(c, Priority.ALWAYS);
            cardsRow.getChildren().add(c);
        }

        // ── Fila 2: chart de ventas + alertas de stock ────────────
        HBox midRow = new HBox(16);
        midRow.setAlignment(Pos.TOP_LEFT);

        VBox chartCard = crearChartCard(ventasHoy, totalHoy);
        HBox.setHgrow(chartCard, Priority.ALWAYS);

        VBox alertsCard = crearAlertasCard(notifs);
        alertsCard.setPrefWidth(270);
        alertsCard.setMinWidth(210);
        alertsCard.setMaxWidth(310);

        midRow.getChildren().addAll(chartCard, alertsCard);

        // ── Fila 3: últimas ventas ────────────────────────────────
        VBox tableCard = crearUltimasVentasCard(ultimasVentas, onVerHistorial);

        root.getChildren().addAll(cardsRow, midRow, tableCard);

        // ── Animaciones de entrada staggered ─────────────────────
        animarEntrada(c1,         0);
        animarEntrada(c2,        80);
        animarEntrada(c3,       160);
        animarEntrada(c4,       240);
        animarEntrada(chartCard, 320);
        animarEntrada(alertsCard,380);
        animarEntrada(tableCard, 440);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dashboard-scroll");
        return scroll;
    }

    // ── Helpers de construcción de UI ────────────────────────────

    private VBox crearStatCard(String titulo, String valor,
                                String subtitulo,
                                String badgeText, String badgeStyle,
                                String icoLiteral, String icoColorHex, String boxBgHex) {
        VBox card = new VBox();
        card.getStyleClass().addAll("card", "dashboard-stat-card");

        HBox row = new HBox();
        row.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(4);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label lTitulo = new Label(titulo);
        lTitulo.getStyleClass().add("dashboard-stat-label");

        Label lValor = new Label(valor);
        lValor.getStyleClass().add("dashboard-stat-value");
        lValor.setPadding(new Insets(2, 0, 0, 0));

        left.getChildren().addAll(lTitulo, lValor);

        if (subtitulo != null) {
            Label lSub = new Label(subtitulo);
            lSub.getStyleClass().add("dashboard-stat-sub");
            left.getChildren().add(lSub);
        }
        if (badgeText != null) {
            Label badge = new Label(badgeText);
            badge.getStyleClass().addAll("dash-badge", badgeStyle);
            badge.setPadding(new Insets(4, 0, 0, 0));
            left.getChildren().add(badge);
        }

        StackPane icoBox = new StackPane();
        icoBox.getStyleClass().add("dashboard-icon-box");
        icoBox.setStyle("-fx-background-color: " + boxBgHex + ";");

        FontIcon icon = new FontIcon(icoLiteral);
        icon.setIconSize(20);
        icon.setIconColor(Paint.valueOf(icoColorHex));
        icoBox.getChildren().add(icon);

        row.getChildren().addAll(left, icoBox);
        card.getChildren().add(row);
        return card;
    }

    private VBox crearStatCardCaja(boolean abierta) {
        VBox card = new VBox();
        card.getStyleClass().addAll("card", "dashboard-stat-card");

        HBox row = new HBox();
        row.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(4);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label lTitulo = new Label("Estado de Caja");
        lTitulo.getStyleClass().add("dashboard-stat-label");

        Label badge = new Label(abierta ? "ABIERTA" : "CERRADA");
        badge.getStyleClass().addAll("dash-badge",
                abierta ? "dash-badge-caja-abierta" : "dash-badge-caja-cerrada");
        badge.setPadding(new Insets(4, 0, 0, 0));

        left.getChildren().addAll(lTitulo, badge);

        StackPane icoBox = new StackPane();
        icoBox.getStyleClass().add("dashboard-icon-box");
        icoBox.setStyle("-fx-background-color: #F1F5F9;");

        FontIcon icon = new FontIcon("fas-cash-register");
        icon.setIconSize(20);
        icon.setIconColor(Paint.valueOf("#475569"));
        icoBox.getChildren().add(icon);

        row.getChildren().addAll(left, icoBox);
        card.getChildren().add(row);
        return card;
    }

    private VBox crearChartCard(List<Venta> ventasHoy, BigDecimal totalHoy) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card", "dashboard-chart-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.BOTTOM_LEFT);

        VBox headerLeft = new VBox(4);
        HBox.setHgrow(headerLeft, Priority.ALWAYS);

        Label lLabel = new Label("VENTAS DE HOY");
        lLabel.getStyleClass().add("dashboard-chart-label");

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        Label lTotal = new Label(fmt.format(totalHoy));
        lTotal.getStyleClass().add("dashboard-chart-total");

        headerLeft.getChildren().addAll(lLabel, lTotal);
        header.getChildren().add(headerLeft);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        xAxis.setTickLabelsVisible(true);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);

        AreaChart<String, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);
        chart.getStyleClass().add("dashboard-chart");
        chart.setPrefHeight(170);
        VBox.setVgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String[] labels = {"8am", "10am", "12pm", "2pm", "4pm", "6pm", "8pm"};
        int[]    hours   = {8,     10,     12,     14,    16,    18,    20   };
        for (int i = 0; i < labels.length; i++) {
            final int h = hours[i];
            double subtotal = ventasHoy.stream()
                    .filter(v -> v.getFecha() != null
                            && (v.getFecha().getHour() == h
                                || v.getFecha().getHour() == h + 1))
                    .mapToDouble(v -> v.getTotal() == null ? 0
                            : v.getTotal().doubleValue())
                    .sum();
            series.getData().add(new XYChart.Data<>(labels[i], subtotal));
        }
        chart.getData().add(series);

        card.getChildren().addAll(header, chart);
        return card;
    }

    private VBox crearAlertasCard(List<Notificacion> notifs) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lTitulo = new Label("Alertas de Stock");
        lTitulo.getStyleClass().add("dashboard-section-title");
        HBox.setHgrow(lTitulo, Priority.ALWAYS);

        FontIcon ico = new FontIcon("fas-boxes");
        ico.setIconSize(15);
        ico.setIconColor(Paint.valueOf("#94A3B8"));

        header.getChildren().addAll(lTitulo, ico);

        VBox lista = new VBox(0);

        List<Notificacion> stockAlerts = notifs.stream()
                .filter(n -> TipoNotificacion.STOCK_BAJO.equals(n.tipo()))
                .limit(5)
                .collect(Collectors.toList());

        if (stockAlerts.isEmpty()) {
            Label lVacio = new Label("Sin alertas de stock.");
            lVacio.getStyleClass().add("dashboard-empty");
            lista.getChildren().add(lVacio);
        } else {
            for (Notificacion n : stockAlerts) {
                HBox alertRow = new HBox(8);
                alertRow.setAlignment(Pos.CENTER_LEFT);
                alertRow.getStyleClass().add("dashboard-alert-row");

                VBox left = new VBox(2);
                HBox.setHgrow(left, Priority.ALWAYS);

                String nombre = n.titulo().startsWith("Stock bajo: ")
                        ? n.titulo().substring("Stock bajo: ".length())
                        : n.titulo();
                Label lNom = new Label(nombre);
                lNom.getStyleClass().add("dashboard-alert-name");

                String[] partes = n.mensaje().split("\\|");
                String stockActualStr = partes.length > 0
                        ? partes[0].trim().replaceAll("[^0-9]", "") : "?";
                String minStr = partes.length > 1
                        ? partes[1].trim().replaceAll("[^0-9]", "") : "?";
                Label lMsg = new Label("Mín. " + minStr);
                lMsg.getStyleClass().add("dashboard-alert-msg");

                left.getChildren().addAll(lNom, lMsg);

                Label lStock = new Label(stockActualStr);
                lStock.getStyleClass().add("dashboard-alert-stock");

                alertRow.getChildren().addAll(left, lStock);
                lista.getChildren().add(alertRow);
            }
        }

        card.getChildren().addAll(header, lista);
        return card;
    }

    private VBox crearUltimasVentasCard(List<Venta> ventas, Runnable onVerHistorial) {
        VBox card = new VBox(0);
        card.getStyleClass().add("dashboard-table-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dashboard-table-header");

        Label lTitle = new Label("Últimas Ventas");
        lTitle.getStyleClass().add("dashboard-section-title");
        HBox.setHgrow(lTitle, Priority.ALWAYS);

        Button btnVer = new Button("Ver historial completo  ›");
        btnVer.getStyleClass().add("dashboard-link-btn");
        btnVer.setOnAction(e -> onVerHistorial.run());

        header.getChildren().addAll(lTitle, btnVer);

        TableView<Venta> tabla = new TableView<>();
        tabla.getStyleClass().add("table-view");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(220);
        tabla.setPlaceholder(new Label("Sin ventas registradas."));

        TableColumn<Venta, String> colId = new TableColumn<>("#");
        colId.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setMaxWidth(60);
        colId.setMinWidth(50);

        TableColumn<Venta, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCliente() != null
                        ? d.getValue().getCliente().getNombre() : "—"));

        TableColumn<Venta, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> {
            if (d.getValue().getTotal() == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(
                    NumberFormat.getCurrencyInstance(new Locale("es", "CO"))
                            .format(d.getValue().getTotal()));
        });

        TableColumn<Venta, String> colMetodo = new TableColumn<>("Método");
        colMetodo.setCellValueFactory(d ->
                new SimpleStringProperty(formatarMetodoPago(d.getValue().getMetodoPago())));
        colMetodo.setMaxWidth(110);

        TableColumn<Venta, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(""));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Venta v = getTableRow().getItem();
                    Label badge = new Label(formatarEstadoVenta(v.getEstado()));
                    boolean esCredito = MetodoPago.CREDITO.equals(v.getMetodoPago());
                    boolean pagada    = EstadoVenta.COMPLETADA.equals(v.getEstado());
                    String style = pagada
                            ? (esCredito ? "dash-badge-credito" : "dash-badge-success")
                            : "dash-badge-danger";
                    badge.getStyleClass().addAll("dash-badge", style);
                    setGraphic(badge);
                }
            }
        });
        colEstado.setMaxWidth(110);

        TableColumn<Venta, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null
                        ? d.getValue().getFecha()
                                .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
                        : "—"));
        colHora.setMaxWidth(90);

        tabla.getColumns().addAll(colId, colCliente, colTotal, colMetodo, colEstado, colHora);
        tabla.getItems().addAll(ventas);

        card.getChildren().addAll(header, tabla);
        return card;
    }

    private void animarEntrada(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(350), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(350), node);
        slide.setFromY(20);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition entrada = new ParallelTransition(fade, slide);
        entrada.setDelay(Duration.millis(delayMs));
        entrada.play();
    }

    private String formatarMetodoPago(MetodoPago mp) {
        if (mp == null) return "—";
        return switch (mp) {
            case EFECTIVO      -> "Efectivo";
            case TRANSFERENCIA -> "Transferencia";
            case CREDITO       -> "Crédito";
        };
    }

    private String formatarEstadoVenta(EstadoVenta ev) {
        if (ev == null) return "—";
        return switch (ev) {
            case COMPLETADA -> "PAGADA";
            case ANULADA    -> "ANULADA";
        };
    }
}
