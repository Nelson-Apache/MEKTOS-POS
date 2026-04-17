package com.nap.pos.ui.controller;

import com.nap.pos.application.service.CajaService;
import com.nap.pos.application.service.VentaService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Caja;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.Venta;
import com.nap.pos.domain.model.enums.EstadoVenta;
import com.nap.pos.domain.model.enums.MetodoPago;
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
import javafx.scene.control.Button;
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
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CajaController {

    private final CajaService  cajaService;
    private final VentaService ventaService;

    private static final NumberFormat     FMT = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
    private static final DateTimeFormatter DFT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Estado ────────────────────────────────────────────────────
    private Usuario    usuarioActual;
    private Runnable   onEstadoCajaChanged;
    private Caja       cajaActual;
    private List<Caja> todasCajas        = new ArrayList<>();
    private List<Venta> ventasCajaActual = new ArrayList<>();

    // ── Refs UI ───────────────────────────────────────────────────
    private StackPane rootStack;
    private VBox      contentArea;
    private Button    tabEstado;
    private Button    tabHistorial;
    private Button    btnAccionCaja;

    // ─────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────

    public Node buildView(Usuario usuario, Runnable onEstadoCajaChanged) {
        this.usuarioActual        = usuario;
        this.onEstadoCajaChanged  = onEstadoCajaChanged;
        recargarEstado();

        rootStack = new StackPane();
        rootStack.getStyleClass().add("inventario-root-stack");

        VBox root = new VBox(0);
        root.getStyleClass().add("inventario-root");

        HBox tabBar = buildTabBar();

        contentArea = new VBox();
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        root.getChildren().addAll(tabBar, contentArea);
        rootStack.getChildren().add(root);

        mostrarEstado();
        return rootStack;
    }

    private void recargarEstado() {
        try { cajaActual = cajaService.getCajaAbierta(); }
        catch (Exception e) { cajaActual = null; }

        try { todasCajas = cajaService.findAll(); }
        catch (Exception e) { todasCajas = new ArrayList<>(); }

        ventasCajaActual = new ArrayList<>();
        if (cajaActual != null) {
            try { ventasCajaActual = ventaService.findByCajaId(cajaActual.getId()); }
            catch (Exception e) { ventasCajaActual = new ArrayList<>(); }
        }
    }

    private void actualizarBtnAccion() {
        if (btnAccionCaja == null) return;
        if (cajaActual != null) {
            FontIcon ico = new FontIcon("fas-lock");
            ico.setIconSize(13); ico.setIconColor(Paint.valueOf("#FFFFFF"));
            btnAccionCaja.setText("Cerrar Caja");
            btnAccionCaja.setGraphic(ico);
            btnAccionCaja.setStyle(
                    "-fx-background-color: #DC2626; -fx-background-radius: 8px; " +
                    "-fx-text-fill: #FFFFFF; -fx-font-size: 13px; " +
                    "-fx-padding: 8 18 8 18; -fx-cursor: hand; -fx-font-weight: 600;");
            btnAccionCaja.setOnAction(e -> abrirModalCerrarCaja());
        } else {
            FontIcon ico = new FontIcon("fas-lock-open");
            ico.setIconSize(13); ico.setIconColor(Paint.valueOf("#FFFFFF"));
            btnAccionCaja.setText("Abrir Caja");
            btnAccionCaja.setGraphic(ico);
            btnAccionCaja.setStyle(
                    "-fx-background-color: #15803D; -fx-background-radius: 8px; " +
                    "-fx-text-fill: #FFFFFF; -fx-font-size: 13px; " +
                    "-fx-padding: 8 18 8 18; -fx-cursor: hand; -fx-font-weight: 600;");
            btnAccionCaja.setOnAction(e -> abrirModalAbrirCaja());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Tab bar
    // ─────────────────────────────────────────────────────────────

    private HBox buildTabBar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("inventario-tab-bar");
        bar.setPadding(new Insets(14, 28, 10, 28));

        FontIcon icoEst = new FontIcon("fas-cash-register");
        icoEst.setIconSize(14);
        icoEst.setIconColor(Paint.valueOf("#5A6ACF"));
        tabEstado = new Button("Estado", icoEst);
        tabEstado.getStyleClass().addAll("inventario-tab", "inventario-tab-active");
        tabEstado.setOnAction(e -> { animarClickTab(tabEstado); mostrarEstado(); });

        FontIcon icoHist = new FontIcon("fas-history");
        icoHist.setIconSize(14);
        icoHist.setIconColor(Paint.valueOf("#78716C"));
        tabHistorial = new Button("Historial", icoHist);
        tabHistorial.getStyleClass().add("inventario-tab");
        tabHistorial.setOnAction(e -> { animarClickTab(tabHistorial); mostrarHistorial(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnAccionCaja = new Button();
        actualizarBtnAccion();

        bar.getChildren().addAll(tabEstado, tabHistorial, spacer, btnAccionCaja);
        return bar;
    }

    private void activarTab(Button activo) {
        tabEstado.getStyleClass().remove("inventario-tab-active");
        tabHistorial.getStyleClass().remove("inventario-tab-active");
        activo.getStyleClass().add("inventario-tab-active");

        String inactivo = "#78716C";
        String actColor = "#5A6ACF";
        ((FontIcon) tabEstado.getGraphic()).setIconColor(Paint.valueOf(activo == tabEstado    ? actColor : inactivo));
        ((FontIcon) tabHistorial.getGraphic()).setIconColor(Paint.valueOf(activo == tabHistorial ? actColor : inactivo));
    }

    // ─────────────────────────────────────────────────────────────
    // Vista: Estado actual de la caja
    // ─────────────────────────────────────────────────────────────

    private void mostrarEstado() {
        activarTab(tabEstado);
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("inventario-root-stack");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox inner = new VBox(24);
        inner.setPadding(new Insets(28, 32, 32, 32));

        if (cajaActual != null) {
            // ── Caja abierta ────────────────────────────────────
            inner.getChildren().add(crearCardCajaAbierta());

            List<Venta> completadas = ventasCajaActual.stream()
                    .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                    .collect(Collectors.toList());

            BigDecimal totalVentas = completadas.stream()
                    .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal enEfectivo = completadas.stream()
                    .filter(v -> v.getMetodoPago() == MetodoPago.EFECTIVO)
                    .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal enTransferencia = completadas.stream()
                    .filter(v -> v.getMetodoPago() == MetodoPago.TRANSFERENCIA)
                    .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal enCredito = completadas.stream()
                    .filter(v -> v.getMetodoPago() == MetodoPago.CREDITO)
                    .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            HBox kpiRow = new HBox(16);
            kpiRow.getChildren().addAll(
                crearKpiCard("fas-receipt",         "#5A6ACF", "Ventas en sesión",  String.valueOf(completadas.size())),
                crearKpiCard("fas-dollar-sign",     "#15803D", "Total cobrado",     FMT.format(totalVentas)),
                crearKpiCard("fas-money-bill-wave", "#D97706", "Efectivo",          FMT.format(enEfectivo)),
                crearKpiCard("fas-exchange-alt",    "#7C3AED", "Transferencias",    FMT.format(enTransferencia))
            );
            if (enCredito.compareTo(BigDecimal.ZERO) > 0) {
                kpiRow.getChildren().add(crearKpiCard("fas-credit-card", "#DC2626", "A crédito", FMT.format(enCredito)));
            }

            Label lblUlt = new Label("Ventas en esta sesión");
            lblUlt.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

            VBox listaVentas = new VBox(8);
            List<Venta> ultimas = ventasCajaActual.stream()
                    .sorted(Comparator.comparing(Venta::getFecha, Comparator.reverseOrder()))
                    .limit(10)
                    .collect(Collectors.toList());

            if (ultimas.isEmpty()) {
                Label lv = new Label("Sin ventas en esta sesión todavía.");
                lv.setStyle("-fx-font-size: 13px; -fx-text-fill: #A8A29E;");
                lv.setPadding(new Insets(8, 0, 0, 0));
                listaVentas.getChildren().add(lv);
            } else {
                for (int i = 0; i < ultimas.size(); i++) {
                    HBox row = crearFilaVenta(ultimas.get(i));
                    listaVentas.getChildren().add(row);
                    animarEntrada(row, i * 25);
                }
            }

            inner.getChildren().addAll(new Separator(), kpiRow, new Separator(), lblUlt, listaVentas);

        } else {
            // ── Caja cerrada ────────────────────────────────────
            inner.getChildren().add(crearCardCajaCerrada());

            todasCajas.stream()
                    .filter(c -> c.getFechaCierre() != null)
                    .max(Comparator.comparing(Caja::getFechaCierre))
                    .ifPresent(ultima -> {
                        Label lblUlt = new Label("Última sesión cerrada");
                        lblUlt.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
                        inner.getChildren().addAll(new Separator(), lblUlt, crearResumenCaja(ultima));
                    });
        }

        scroll.setContent(inner);
        contentArea.getChildren().add(scroll);
        animarEntrada(inner, 0);
    }

    private VBox crearCardCajaAbierta() {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #F0FDF4; -fx-background-radius: 14px; " +
                      "-fx-border-color: #86EFAC; -fx-border-radius: 14px; -fx-border-width: 1; " +
                      "-fx-padding: 20 24 20 24;");

        HBox top = new HBox(14);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: #15803D22; -fx-background-radius: 12px;");
        icoCircle.setMinSize(52, 52); icoCircle.setMaxSize(52, 52);
        FontIcon ico = new FontIcon("fas-cash-register");
        ico.setIconSize(22); ico.setIconColor(Paint.valueOf("#15803D"));
        icoCircle.getChildren().add(ico);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblEstado = new Label("Caja ABIERTA");
        lblEstado.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #15803D;");
        String apertura = cajaActual.getFechaApertura() != null
                ? "Apertura: " + cajaActual.getFechaApertura().format(DFT) : "Apertura: —";
        String usr = cajaActual.getUsuario() != null && cajaActual.getUsuario().getNombreCompleto() != null
                ? "  ·  " + cajaActual.getUsuario().getNombreCompleto() : "";
        Label lblInfo = new Label(apertura + usr);
        lblInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #166534;");
        info.getChildren().addAll(lblEstado, lblInfo);

        VBox montoBox = new VBox(2);
        montoBox.setAlignment(Pos.CENTER_RIGHT);
        Label lblMonto = new Label(FMT.format(
                cajaActual.getMontoInicial() != null ? cajaActual.getMontoInicial() : BigDecimal.ZERO));
        lblMonto.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #15803D;");
        Label lblMontoSub = new Label("Monto inicial");
        lblMontoSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #166534;");
        montoBox.getChildren().addAll(lblMonto, lblMontoSub);

        top.getChildren().addAll(icoCircle, info, montoBox);
        card.getChildren().add(top);
        return card;
    }

    private VBox crearCardCajaCerrada() {
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 14px; " +
                      "-fx-border-color: rgba(26,31,46,0.10); -fx-border-radius: 14px; -fx-border-width: 1; " +
                      "-fx-padding: 48 24 48 24;");
        card.setAlignment(Pos.CENTER);

        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 40px;");
        icoCircle.setMinSize(80, 80); icoCircle.setMaxSize(80, 80);
        FontIcon ico = new FontIcon("fas-lock");
        ico.setIconSize(30); ico.setIconColor(Paint.valueOf("#A8A29E"));
        icoCircle.getChildren().add(ico);

        Label lblTitulo = new Label("No hay caja abierta");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        Label lblSub = new Label("Abre una caja para comenzar a registrar ventas.");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #A8A29E;");

        card.getChildren().addAll(icoCircle, lblTitulo, lblSub);
        return card;
    }

    private VBox crearResumenCaja(Caja caja) {
        VBox card = new VBox(14);
        card.getStyleClass().add("inventario-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lTit = new Label("RESUMEN DE SESIÓN");
        lTit.getStyleClass().add("inventario-card-title");
        HBox.setHgrow(lTit, Priority.ALWAYS);
        FontIcon ico = new FontIcon("fas-file-invoice-dollar");
        ico.setIconSize(14); ico.setIconColor(Paint.valueOf("#94A3B8"));
        header.getChildren().addAll(lTit, ico);

        List<Venta> ventas;
        try { ventas = ventaService.findByCajaId(caja.getId()); }
        catch (Exception e) { ventas = List.of(); }

        long totalVentas = ventas.stream().filter(v -> v.getEstado() == EstadoVenta.COMPLETADA).count();
        BigDecimal totalCobrado = ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal efectivo = ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA && v.getMetodoPago() == MetodoPago.EFECTIVO)
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VBox filas = new VBox(10);
        BigDecimal montoIni = caja.getMontoInicial() != null ? caja.getMontoInicial() : BigDecimal.ZERO;
        filas.getChildren().addAll(
            crearFilaResumen("fas-calendar",        "#5A6ACF", "Apertura",             caja.getFechaApertura() != null ? caja.getFechaApertura().format(DFT) : "—"),
            crearFilaResumen("fas-calendar-check",  "#78716C", "Cierre",               caja.getFechaCierre()   != null ? caja.getFechaCierre().format(DFT)   : "—"),
            crearFilaResumen("fas-receipt",         "#D97706", "Ventas completadas",   totalVentas + " ventas"),
            crearFilaResumen("fas-dollar-sign",     "#15803D", "Total cobrado",        FMT.format(totalCobrado)),
            crearFilaResumen("fas-coins",           "#5A6ACF", "Monto inicial",        FMT.format(montoIni)),
            crearFilaResumen("fas-coins",           "#D97706", "Monto final contado",  caja.getMontoFinal() != null ? FMT.format(caja.getMontoFinal()) : "—")
        );

        if (caja.getMontoFinal() != null) {
            BigDecimal esperado  = montoIni.add(efectivo);
            BigDecimal diferencia = caja.getMontoFinal().subtract(esperado);
            String difColor = diferencia.compareTo(BigDecimal.ZERO) >= 0 ? "#15803D" : "#DC2626";
            String signo    = diferencia.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            filas.getChildren().add(
                crearFilaResumen("fas-balance-scale", difColor, "Diferencia", signo + FMT.format(diferencia)));
        }

        card.getChildren().addAll(header, filas);
        return card;
    }

    private HBox crearFilaVenta(Venta v) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 10px; " +
                     "-fx-border-color: rgba(26,31,46,0.08); -fx-border-width: 1; " +
                     "-fx-border-radius: 10px; -fx-padding: 12 16 12 16;");
        row.setOnMouseEntered(e -> row.setStyle(row.getStyle().replace("#FDFCFA", "#F5F1EB")));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("#F5F1EB", "#FDFCFA")));

        boolean anulada  = v.getEstado() == EstadoVenta.ANULADA;
        String icoColor  = anulada ? "#DC2626" : "#15803D";

        StackPane ico = new StackPane();
        ico.setStyle("-fx-background-color: " + icoColor + "22; -fx-background-radius: 8px;");
        ico.setMinSize(34, 34); ico.setMaxSize(34, 34);
        FontIcon fi = new FontIcon(anulada ? "fas-times-circle" : "fas-check-circle");
        fi.setIconSize(14); fi.setIconColor(Paint.valueOf(icoColor));
        ico.getChildren().add(fi);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        String cliente = v.getCliente() != null ? v.getCliente().getNombre() : "Venta directa";
        Label lCliente = new Label(cliente);
        lCliente.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1A1F2E;");
        String metodo = v.getMetodoPago() != null ? switch (v.getMetodoPago()) {
            case EFECTIVO      -> "Efectivo";
            case TRANSFERENCIA -> "Transferencia";
            case CREDITO       -> "Crédito";
        } : "—";
        Label lMeta = new Label(metodo + (v.getFecha() != null ? "  ·  " + v.getFecha().format(DFT) : ""));
        lMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A29E;");
        info.getChildren().addAll(lCliente, lMeta);

        Label lTotal = new Label(anulada ? "ANULADA"
                : FMT.format(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO));
        lTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + icoColor + ";");

        row.getChildren().addAll(ico, info, lTotal);
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // Vista: Historial de cajas
    // ─────────────────────────────────────────────────────────────

    private void mostrarHistorial() {
        activarTab(tabHistorial);
        contentArea.getChildren().clear();

        VBox wrapper = new VBox(16);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        wrapper.setPadding(new Insets(24, 28, 28, 28));

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Label lblTit = new Label("Historial de cajas");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);
        toolbar.getChildren().add(lblTit);

        TableView<Caja> tabla = new TableView<>();
        tabla.getStyleClass().add("inventario-table-card");
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin registros de caja."));

        TableColumn<Caja, String> colApertura = new TableColumn<>("Apertura");
        colApertura.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaApertura() != null ? d.getValue().getFechaApertura().format(DFT) : "—"));
        colApertura.setPrefWidth(130);

        TableColumn<Caja, String> colCierre = new TableColumn<>("Cierre");
        colCierre.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaCierre() != null ? d.getValue().getFechaCierre().format(DFT) : "En curso"));
        colCierre.setPrefWidth(130);

        TableColumn<Caja, String> colUsuario = new TableColumn<>("Usuario");
        colUsuario.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUsuario() != null ? d.getValue().getUsuario().getNombreCompleto() : "—"));
        colUsuario.setPrefWidth(130);

        TableColumn<Caja, String> colInicial = new TableColumn<>("Monto inicial");
        colInicial.setCellValueFactory(d -> new SimpleStringProperty(
                FMT.format(d.getValue().getMontoInicial() != null ? d.getValue().getMontoInicial() : BigDecimal.ZERO)));
        colInicial.setPrefWidth(120);

        TableColumn<Caja, String> colFinal = new TableColumn<>("Monto final");
        colFinal.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMontoFinal() != null ? FMT.format(d.getValue().getMontoFinal()) : "—"));
        colFinal.setPrefWidth(120);
        colFinal.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null || "—".equals(s)) { setText(s); setStyle(""); return; }
                Caja caja = getTableRow() != null ? (Caja) getTableRow().getItem() : null;
                boolean deficit = caja != null
                        && caja.getMontoFinal() != null
                        && caja.getMontoInicial() != null
                        && caja.getMontoFinal().compareTo(caja.getMontoInicial()) < 0;
                setText(s);
                setStyle("-fx-font-weight: 700; -fx-text-fill: " + (deficit ? "#DC2626" : "#15803D") + ";");
            }
        });

        TableColumn<Caja, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().estaAbierta() ? "Abierta" : "Cerrada"));
        colEstado.setPrefWidth(90);
        colEstado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                boolean abierta = "Abierta".equals(s);
                Label badge = crearBadge(s,
                        abierta ? "#DCFCE7" : "#F1F5F9",
                        abierta ? "#15803D" : "#64748B");
                setGraphic(badge); setText(null);
            }
        });

        TableColumn<Caja, String> colVer = new TableColumn<>("");
        colVer.setCellValueFactory(d -> new SimpleStringProperty(""));
        colVer.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                FontIcon ico = new FontIcon("fas-eye");
                ico.setIconSize(13); ico.setIconColor(Paint.valueOf("#5A6ACF"));
                btn.setGraphic(ico);
                btn.getStyleClass().add("prod-row-arrow");
                btn.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null)
                        abrirModalDetalleCaja(getTableRow().getItem());
                });
                setAlignment(Pos.CENTER);
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn); setText(null);
            }
        });
        colVer.setMinWidth(44); colVer.setMaxWidth(44); colVer.setSortable(false);

        tabla.getColumns().addAll(colApertura, colCierre, colUsuario, colInicial, colFinal, colEstado, colVer);

        tabla.setRowFactory(tv -> {
            TableRow<Caja> row = new TableRow<>();
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle("-fx-background-color: rgba(90,106,207,0.05); -fx-cursor: hand;"); });
            row.setOnMouseExited(e -> row.setStyle(""));
            row.setOnMouseClicked(e -> { if (!row.isEmpty() && row.getItem() != null) abrirModalDetalleCaja(row.getItem()); });
            return row;
        });

        List<Caja> cajasOrdenadas = todasCajas.stream()
                .filter(c -> c.getFechaApertura() != null)
                .sorted(Comparator.comparing(Caja::getFechaApertura, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        tabla.setItems(FXCollections.observableArrayList(cajasOrdenadas));

        wrapper.getChildren().addAll(toolbar, tabla);
        contentArea.getChildren().add(wrapper);
    }

    // ─────────────────────────────────────────────────────────────
    // Modal: Abrir Caja
    // ─────────────────────────────────────────────────────────────

    private void abrirModalAbrirCaja() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");

        VBox modal = new VBox(0);
        modal.setMaxWidth(420);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 16px; " +
                       "-fx-border-color: rgba(26,31,46,0.10); -fx-border-radius: 16px; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 36, 0, 0, 10);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #15803D; -fx-background-radius: 16px 16px 0 0; " +
                        "-fx-padding: 16 20 16 20;");
        FontIcon icoH = new FontIcon("fas-lock-open");
        icoH.setIconSize(16); icoH.setIconColor(Paint.valueOf("#FFFFFF"));
        Label lblH = new Label("Abrir caja");
        lblH.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        HBox.setHgrow(lblH, Priority.ALWAYS);
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); " +
                      "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        header.getChildren().addAll(icoH, lblH, btnX);

        // Body
        VBox body = new VBox(14);
        body.setPadding(new Insets(24, 24, 8, 24));

        Label lblDesc = new Label("Ingresa el monto de efectivo con el que inicia la caja.");
        lblDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #57534E; -fx-wrap-text: true;");

        Label secMonto = new Label("MONTO INICIAL EN CAJA");
        secMonto.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;");

        TextField txtMonto = crearCampo("Ej: 100000");
        txtMonto.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");
        lblError.setVisible(false); lblError.setManaged(false);

        body.getChildren().addAll(lblDesc, secMonto, txtMonto, lblError);

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroll.getStyleClass().add("inventario-root-stack");
        bodyScroll.setMaxHeight(280);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 24, 20, 24));

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 8px; " +
                "-fx-text-fill: #57534E; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");

        Button btnAbrir = new Button("Abrir caja");
        btnAbrir.setStyle("-fx-background-color: #15803D; -fx-background-radius: 8px; " +
                "-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 8 20 8 20; " +
                "-fx-font-weight: 600; -fx-cursor: hand;");

        footer.getChildren().addAll(btnCancelar, btnAbrir);
        modal.getChildren().addAll(header, bodyScroll, footer);
        overlay.getChildren().add(modal);
        StackPane.setAlignment(modal, Pos.CENTER);

        Runnable cerrar = () -> cerrarModal(overlay);
        btnX.setOnAction(e -> cerrar.run());
        btnCancelar.setOnAction(e -> cerrar.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrar.run(); });
        txtMonto.setOnAction(e -> btnAbrir.fire());

        btnAbrir.setOnAction(e -> {
            String raw = txtMonto.getText().trim().replace(",", "").replace("$", "").replace(".", "");
            BigDecimal monto;
            try { monto = raw.isBlank() ? BigDecimal.ZERO : new BigDecimal(raw); }
            catch (NumberFormatException ex) {
                lblError.setText("Ingresa un monto válido."); lblError.setVisible(true); lblError.setManaged(true); return;
            }
            try {
                cajaService.abrirCaja(monto, usuarioActual);
            } catch (BusinessException ex) {
                lblError.setText(ex.getMessage()); lblError.setVisible(true); lblError.setManaged(true);
                return;
            } catch (Exception ex) {
                lblError.setText("Error al abrir la caja."); lblError.setVisible(true); lblError.setManaged(true);
                return;
            }
            cerrar.run();
            recargarEstado();
            actualizarBtnAccion();
            mostrarEstado();
            if (onEstadoCajaChanged != null) onEstadoCajaChanged.run();
        });

        rootStack.getChildren().add(overlay);
        animarEntradaModal(overlay, modal);
    }

    // ─────────────────────────────────────────────────────────────
    // Modal: Cerrar Caja
    // ─────────────────────────────────────────────────────────────

    private void abrirModalCerrarCaja() {
        if (cajaActual == null) return;

        List<Venta> completadas = ventasCajaActual.stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .collect(Collectors.toList());

        BigDecimal totalVentas = completadas.stream()
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal enEfectivo = completadas.stream()
                .filter(v -> v.getMetodoPago() == MetodoPago.EFECTIVO)
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoInicial  = cajaActual.getMontoInicial() != null ? cajaActual.getMontoInicial() : BigDecimal.ZERO;
        BigDecimal esperadoEnCaja = montoInicial.add(enEfectivo);

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");

        VBox modal = new VBox(0);
        modal.setMaxWidth(460);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 16px; " +
                       "-fx-border-color: rgba(26,31,46,0.10); -fx-border-radius: 16px; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 36, 0, 0, 10);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #DC2626; -fx-background-radius: 16px 16px 0 0; " +
                        "-fx-padding: 16 20 16 20;");
        FontIcon icoH = new FontIcon("fas-lock");
        icoH.setIconSize(16); icoH.setIconColor(Paint.valueOf("#FFFFFF"));
        Label lblH = new Label("Cerrar caja");
        lblH.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        HBox.setHgrow(lblH, Priority.ALWAYS);
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); " +
                      "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        header.getChildren().addAll(icoH, lblH, btnX);

        // Body
        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 8, 24));

        Label secResumen = new Label("RESUMEN DE LA SESIÓN");
        secResumen.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;");

        VBox resumen = new VBox(8);
        resumen.setStyle("-fx-background-color: #F5F1EB; -fx-background-radius: 10px; -fx-padding: 14 16 14 16;");
        resumen.getChildren().addAll(
            crearFilaResumen("fas-receipt",         "#5A6ACF", "Ventas completadas",  completadas.size() + " ventas"),
            crearFilaResumen("fas-dollar-sign",     "#15803D", "Total cobrado",       FMT.format(totalVentas)),
            crearFilaResumen("fas-money-bill-wave", "#D97706", "Efectivo en ventas",  FMT.format(enEfectivo)),
            crearFilaResumen("fas-coins",           "#5A6ACF", "Monto inicial",       FMT.format(montoInicial)),
            crearFilaResumen("fas-calculator",      "#1A1F2E", "Esperado en caja",    FMT.format(esperadoEnCaja))
        );

        Label secConteo = new Label("MONTO CONTADO EN CAJA");
        secConteo.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A8A29E; -fx-padding: 4 0 0 0;");

        TextField txtContado = crearCampo("Ej: 150000");
        txtContado.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");

        // Diferencia en tiempo real
        HBox difRow = new HBox(8);
        difRow.setAlignment(Pos.CENTER_LEFT);
        Label lblDifTxt = new Label("Diferencia:");
        lblDifTxt.setStyle("-fx-font-size: 13px; -fx-text-fill: #57534E;");
        Label lblDifVal = new Label("—");
        lblDifVal.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;");
        difRow.getChildren().addAll(lblDifTxt, lblDifVal);

        txtContado.textProperty().addListener((obs, o, n) -> {
            String raw = (n == null ? "" : n).trim().replace(",", "").replace("$", "").replace(".", "");
            if (raw.isBlank()) {
                lblDifVal.setText("—"); lblDifVal.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;"); return;
            }
            try {
                BigDecimal contado   = new BigDecimal(raw);
                BigDecimal dif       = contado.subtract(esperadoEnCaja);
                String     signo     = dif.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                String     difColor  = dif.compareTo(BigDecimal.ZERO) == 0 ? "#15803D"
                                     : dif.compareTo(BigDecimal.ZERO)  > 0 ? "#D97706" : "#DC2626";
                lblDifVal.setText(signo + FMT.format(dif));
                lblDifVal.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + difColor + ";");
            } catch (NumberFormatException ignored) {
                lblDifVal.setText("—"); lblDifVal.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #A8A29E;");
            }
        });

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");
        lblError.setVisible(false); lblError.setManaged(false);

        body.getChildren().addAll(secResumen, resumen, secConteo, txtContado, difRow, lblError);

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroll.getStyleClass().add("inventario-root-stack");
        bodyScroll.setMaxHeight(420);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 24, 20, 24));

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 8px; " +
                "-fx-text-fill: #57534E; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");

        Button btnCerrar = new Button("Confirmar cierre");
        btnCerrar.setStyle("-fx-background-color: #DC2626; -fx-background-radius: 8px; " +
                "-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 8 20 8 20; " +
                "-fx-font-weight: 600; -fx-cursor: hand;");

        footer.getChildren().addAll(btnCancelar, btnCerrar);
        modal.getChildren().addAll(header, bodyScroll, footer);
        overlay.getChildren().add(modal);
        StackPane.setAlignment(modal, Pos.CENTER);

        Runnable cerrar = () -> cerrarModal(overlay);
        btnX.setOnAction(e -> cerrar.run());
        btnCancelar.setOnAction(e -> cerrar.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) cerrar.run(); });
        txtContado.setOnAction(e -> btnCerrar.fire());

        btnCerrar.setOnAction(e -> {
            String raw = txtContado.getText().trim().replace(",", "").replace("$", "").replace(".", "");
            BigDecimal montoFinal;
            try { montoFinal = raw.isBlank() ? BigDecimal.ZERO : new BigDecimal(raw); }
            catch (NumberFormatException ex) {
                lblError.setText("Ingresa un monto válido."); lblError.setVisible(true); lblError.setManaged(true); return;
            }
            try {
                cajaService.cerrarCaja(montoFinal);
            } catch (BusinessException ex) {
                lblError.setText(ex.getMessage()); lblError.setVisible(true); lblError.setManaged(true);
                return;
            } catch (Exception ex) {
                lblError.setText("Error al cerrar la caja."); lblError.setVisible(true); lblError.setManaged(true);
                return;
            }
            cerrar.run();
            recargarEstado();
            actualizarBtnAccion();
            mostrarEstado();
            if (onEstadoCajaChanged != null) onEstadoCajaChanged.run();
        });

        rootStack.getChildren().add(overlay);
        animarEntradaModal(overlay, modal);
    }

    // ─────────────────────────────────────────────────────────────
    // Modal: Detalle de una caja
    // ─────────────────────────────────────────────────────────────

    private void abrirModalDetalleCaja(Caja caja) {
        List<Venta> ventas;
        try { ventas = ventaService.findByCajaId(caja.getId()); }
        catch (Exception e) { ventas = List.of(); }
        final List<Venta> ventasFinal = ventas;

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");

        VBox modal = new VBox(0);
        modal.setMaxWidth(560);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setStyle("-fx-background-color: #FDFCFA; -fx-background-radius: 16px; " +
                       "-fx-border-color: rgba(26,31,46,0.10); -fx-border-radius: 16px; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 36, 0, 0, 10);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        String headerColor = caja.estaAbierta() ? "#15803D" : "#5A6ACF";
        header.setStyle("-fx-background-color: " + headerColor + "; -fx-background-radius: 16px 16px 0 0; " +
                        "-fx-padding: 16 20 16 20;");
        FontIcon icoH = new FontIcon("fas-file-invoice-dollar");
        icoH.setIconSize(16); icoH.setIconColor(Paint.valueOf("#FFFFFF"));
        String fechaStr = caja.getFechaApertura() != null ? caja.getFechaApertura().format(DFT) : "—";
        Label lblH = new Label("Sesión: " + fechaStr);
        lblH.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        HBox.setHgrow(lblH, Priority.ALWAYS);
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); " +
                      "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        header.getChildren().addAll(icoH, lblH, btnX);

        // Body
        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 8, 24));

        long completadasCnt = ventasFinal.stream().filter(v -> v.getEstado() == EstadoVenta.COMPLETADA).count();
        long anuladasCnt    = ventasFinal.stream().filter(v -> v.getEstado() == EstadoVenta.ANULADA).count();
        BigDecimal totalCobrado = ventasFinal.stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VBox resumen = new VBox(8);
        resumen.setStyle("-fx-background-color: #F5F1EB; -fx-background-radius: 10px; -fx-padding: 12 16 12 16;");
        BigDecimal montoIni = caja.getMontoInicial() != null ? caja.getMontoInicial() : BigDecimal.ZERO;
        resumen.getChildren().addAll(
            crearFilaResumen("fas-calendar",       "#5A6ACF", "Apertura",             caja.getFechaApertura() != null ? caja.getFechaApertura().format(DFT) : "—"),
            crearFilaResumen("fas-calendar-check", "#78716C", "Cierre",               caja.getFechaCierre()   != null ? caja.getFechaCierre().format(DFT)   : "En curso"),
            crearFilaResumen("fas-check-circle",   "#15803D", "Ventas completadas",   completadasCnt + " ventas"),
            crearFilaResumen("fas-times-circle",   "#DC2626", "Ventas anuladas",      anuladasCnt    + " ventas"),
            crearFilaResumen("fas-dollar-sign",    "#15803D", "Total cobrado",        FMT.format(totalCobrado)),
            crearFilaResumen("fas-coins",          "#5A6ACF", "Monto inicial",        FMT.format(montoIni))
        );
        if (caja.getMontoFinal() != null)
            resumen.getChildren().add(crearFilaResumen("fas-coins", "#D97706", "Monto final contado", FMT.format(caja.getMontoFinal())));

        Label lblVentas = new Label("Ventas de esta sesión");
        lblVentas.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E; -fx-padding: 4 0 0 0;");

        VBox listaVentas = new VBox(6);
        if (ventasFinal.isEmpty()) {
            Label lv = new Label("Sin ventas en esta sesión.");
            lv.setStyle("-fx-font-size: 12px; -fx-text-fill: #A8A29E;");
            listaVentas.getChildren().add(lv);
        } else {
            ventasFinal.stream()
                    .sorted(Comparator.comparing(Venta::getFecha, Comparator.reverseOrder()))
                    .forEach(v -> listaVentas.getChildren().add(crearFilaVenta(v)));
        }

        body.getChildren().addAll(resumen, lblVentas, listaVentas);

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroll.getStyleClass().add("inventario-root-stack");
        bodyScroll.setMaxHeight(460);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 24, 16, 24));
        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #EDE9E2; -fx-background-radius: 8px; " +
                "-fx-text-fill: #57534E; -fx-font-size: 13px; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
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
    // Helpers UI
    // ─────────────────────────────────────────────────────────────

    private VBox crearKpiCard(String iconLiteral, String color, String titulo, String valor) {
        VBox card = new VBox(8);
        card.getStyleClass().add("inventario-stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        StackPane icoCircle = new StackPane();
        icoCircle.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 8px;");
        icoCircle.setMinSize(36, 36); icoCircle.setMaxSize(36, 36);
        FontIcon ico = new FontIcon(iconLiteral);
        ico.setIconSize(16); ico.setIconColor(Paint.valueOf(color));
        icoCircle.getChildren().add(ico);

        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");

        card.getChildren().addAll(icoCircle, lblValor, lblTitulo);
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
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
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
