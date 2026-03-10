package com.nap.pos.ui.controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Diálogo reutilizable para seleccionar un ícono de la biblioteca
 * FontAwesome Solid (Ikonli). Muestra un grid con búsqueda en tiempo real
 * y devuelve el literal seleccionado (ej. "fas-tshirt").
 *
 * Uso:
 *   Optional<String> icono = iconPickerDialog.mostrar(ownerWindow, iconoActual);
 *   icono.ifPresent(literal -> categoria.setIcono(literal));
 */
@Component
public class IconPickerDialog {

    // Lista inmutable de todos los íconos disponibles (se carga una sola vez)
    private static final List<FontAwesomeSolid> TODOS =
            Arrays.stream(FontAwesomeSolid.values())
                  .sorted((a, b) -> a.getDescription().compareTo(b.getDescription()))
                  .toList();

    // ── Estado de la sesión (se reinicia en cada llamada a mostrar()) ─────
    private String    literalSeleccionado;
    private VBox      celdaActiva;
    private FlowPane  gridIconos;
    private FontIcon  previewIcono;
    private Label     lblPreviewNombre;

    // ── Arrastre de ventana ───────────────────────────────────────────────
    private double dragX, dragY;

    // ─────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Abre el diálogo modal y bloquea hasta que el usuario seleccione o cancele.
     *
     * @param owner       Ventana padre (para centrar y bloquear)
     * @param iconoActual Literal del ícono actual (puede ser null)
     * @return            Optional con el literal elegido, o empty si se canceló
     */
    public Optional<String> mostrar(Window owner, String iconoActual) {
        // Reiniciar estado
        this.literalSeleccionado = iconoActual;
        this.celdaActiva         = null;
        this.gridIconos          = null;
        this.previewIcono        = null;
        this.lblPreviewNombre    = null;

        String[] resultado = {null};

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);

        VBox root = construirRaiz(stage, resultado);

        // Arrastre de ventana desde el header
        root.getChildren().stream().findFirst().ifPresent(header -> {
            header.setOnMousePressed(e -> { dragX = e.getSceneX(); dragY = e.getSceneY(); });
            header.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() - dragX);
                stage.setY(e.getScreenY() - dragY);
            });
        });

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            Objects.requireNonNull(
                getClass().getResource("/css/styles.css")).toExternalForm());

        // ESC cancela el diálogo
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });

        stage.setScene(scene);
        stage.centerOnScreen();

        // Animación de entrada: se inicia justo después de que la ventana se muestra
        root.setOpacity(0);
        root.setScaleX(0.95);
        root.setScaleY(0.95);

        stage.setOnShown(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(180), root);
            fade.setToValue(1);
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), root);
            scale.setToX(1.0); scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);
            fade.play();
            scale.play();
        });

        stage.showAndWait();

        return Optional.ofNullable(resultado[0]);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Construcción de la interfaz
    // ─────────────────────────────────────────────────────────────────────

    private VBox construirRaiz(Stage stage, String[] resultado) {
        VBox root = new VBox(0);
        root.setStyle(
            "-fx-background-color: #FDFCFA;" +
            "-fx-background-radius: 14px;" +
            "-fx-border-radius: 14px;"
        );
        root.setEffect(new DropShadow(36, 0, 8, Color.color(0, 0, 0, 0.30)));
        root.setPrefWidth(680);
        root.setPrefHeight(560);

        VBox.setVgrow(root, Priority.ALWAYS);
        root.getChildren().addAll(
            construirHeader(stage),
            construirBusqueda(),
            construirGrid(),
            construirFooter(stage, resultado)
        );
        return root;
    }

    /** Header oscuro con título, subtítulo y botón de cierre */
    private HBox construirHeader(Stage stage) {
        HBox h = new HBox(10);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(16, 20, 16, 20));
        h.setStyle(
            "-fx-background-color: #192030;" +
            "-fx-background-radius: 14px 14px 0 0;" +
            "-fx-border-color: transparent transparent rgba(240,237,232,0.10) transparent;" +
            "-fx-border-width: 0 0 1 0;" +
            "-fx-cursor: move;"
        );

        FontIcon titleIco = new FontIcon("fas-icons");
        titleIco.setIconSize(15);
        titleIco.setIconColor(Paint.valueOf("#7A88DB"));

        VBox titleGroup = new VBox(2);
        Label lTit = new Label("Seleccionar ícono");
        lTit.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #F0EDE8;");
        Label lSub = new Label(TODOS.size() + " íconos  ·  FontAwesome Solid  ·  Ikonli");
        lSub.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(240,237,232,0.40);");
        titleGroup.getChildren().addAll(lTit, lSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button();
        FontIcon closeIco = new FontIcon("fas-times");
        closeIco.setIconSize(13);
        closeIco.setIconColor(Paint.valueOf("rgba(240,237,232,0.45)"));
        btnClose.setGraphic(closeIco);
        btnClose.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                          "-fx-cursor: hand; -fx-padding: 5 7 5 7; -fx-effect: none;" +
                          "-fx-background-radius: 6;");
        btnClose.setOnAction(e -> stage.close());
        btnClose.setOnMouseEntered(e -> {
            btnClose.setStyle("-fx-background-color: rgba(220,38,38,0.65); -fx-border-color: transparent;" +
                              "-fx-cursor: hand; -fx-padding: 5 7 5 7; -fx-effect: none;" +
                              "-fx-background-radius: 6;");
            closeIco.setIconColor(Paint.valueOf("#FFFFFF"));
        });
        btnClose.setOnMouseExited(e -> {
            btnClose.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                              "-fx-cursor: hand; -fx-padding: 5 7 5 7; -fx-effect: none;" +
                              "-fx-background-radius: 6;");
            closeIco.setIconColor(Paint.valueOf("rgba(240,237,232,0.45)"));
        });

        h.getChildren().addAll(titleIco, titleGroup, spacer, btnClose);
        return h;
    }

    /** Barra de búsqueda con filtro en tiempo real */
    private HBox construirBusqueda() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 8, 16));
        bar.setStyle("-fx-background-color: #F5F1EB;");

        StackPane wrap = new StackPane();
        wrap.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(wrap, Priority.ALWAYS);

        TextField field = new TextField();
        field.setPromptText("Buscar ícono... (ej: home, user, box, car, truck)");
        field.getStyleClass().add("venta-search-field");
        field.setMaxWidth(Double.MAX_VALUE);
        field.setStyle("-fx-padding: 9 12 9 34;");

        FontIcon sIco = new FontIcon("fas-search");
        sIco.setIconSize(13);
        sIco.setIconColor(Paint.valueOf("#A8A29E"));
        StackPane.setMargin(sIco, new Insets(0, 0, 0, 11));

        field.focusedProperty().addListener((obs, o, f) ->
            sIco.setIconColor(Paint.valueOf(f ? "#5A6ACF" : "#A8A29E")));

        field.textProperty().addListener((obs, o, texto) -> filtrarGrid(texto));

        wrap.getChildren().addAll(field, sIco);
        bar.getChildren().add(wrap);
        return bar;
    }

    /** Grid principal con todos los íconos */
    private ScrollPane construirGrid() {
        gridIconos = new FlowPane();
        gridIconos.setHgap(6);
        gridIconos.setVgap(6);
        gridIconos.setPadding(new Insets(14, 14, 14, 14));
        gridIconos.setStyle("-fx-background-color: #F5F1EB;");

        ScrollPane sc = new ScrollPane(gridIconos);
        sc.setFitToWidth(true);
        sc.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sc.setStyle(
            "-fx-background-color: #F5F1EB; -fx-background: #F5F1EB;" +
            "-fx-border-color: transparent;"
        );
        sc.getStyleClass().add("dashboard-scroll");
        VBox.setVgrow(sc, Priority.ALWAYS);

        // Cargar todos los íconos con el actual pre-seleccionado
        poblarGrid(TODOS);
        return sc;
    }

    /** Footer con preview del ícono seleccionado y botones de acción */
    private HBox construirFooter(Stage stage, String[] resultado) {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(12, 16, 14, 16));
        footer.setStyle(
            "-fx-background-color: #FDFCFA;" +
            "-fx-background-radius: 0 0 14px 14px;" +
            "-fx-border-color: rgba(26,31,46,0.08) transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;"
        );

        // Preview del ícono seleccionado
        StackPane preview = new StackPane();
        preview.setMinWidth(38); preview.setMinHeight(38);
        preview.setMaxWidth(38); preview.setMaxHeight(38);
        preview.setStyle("-fx-background-color: #ECEEF7; -fx-background-radius: 9px;");

        previewIcono = new FontIcon();
        previewIcono.setIconSize(20);
        previewIcono.setIconColor(Paint.valueOf("#5A6ACF"));
        preview.getChildren().add(previewIcono);

        // Nombre del ícono seleccionado
        lblPreviewNombre = new Label("Ninguno seleccionado");
        lblPreviewNombre.setStyle("-fx-font-size: 12px; -fx-text-fill: #A8A29E;");

        // Mostrar el ícono actual si existe
        if (literalSeleccionado != null && !literalSeleccionado.isBlank()) {
            actualizarPreview(literalSeleccionado);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Botón Limpiar (quitar ícono)
        Button btnLimpiar = new Button("Quitar ícono");
        FontIcon limpIco = new FontIcon("fas-ban");
        limpIco.setIconSize(11);
        limpIco.setIconColor(Paint.valueOf("#DC2626"));
        btnLimpiar.setGraphic(limpIco);
        btnLimpiar.setContentDisplay(ContentDisplay.LEFT);
        btnLimpiar.setGraphicTextGap(6);
        btnLimpiar.getStyleClass().add("btn-secundario");
        btnLimpiar.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");
        btnLimpiar.setOnAction(e -> {
            resultado[0] = "";         // vacío = sin ícono
            stage.close();
        });

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");
        btnCancelar.setOnAction(e -> stage.close()); // resultado[0] queda null

        Button btnUsar = new Button("Usar ícono");
        FontIcon checkIco = new FontIcon("fas-check");
        checkIco.setIconSize(11);
        checkIco.setIconColor(Paint.valueOf("#FFFFFF"));
        btnUsar.setGraphic(checkIco);
        btnUsar.setContentDisplay(ContentDisplay.RIGHT);
        btnUsar.setGraphicTextGap(7);
        btnUsar.getStyleClass().add("btn-primario");
        btnUsar.setDisable(literalSeleccionado == null || literalSeleccionado.isBlank());
        btnUsar.setOnAction(e -> {
            resultado[0] = literalSeleccionado;
            stage.close();
        });

        // Habilitar/deshabilitar "Usar ícono" según selección
        lblPreviewNombre.textProperty().addListener((obs, o, n) ->
            btnUsar.setDisable(
                literalSeleccionado == null || literalSeleccionado.isBlank()));

        footer.getChildren().addAll(preview, lblPreviewNombre, spacer,
                                    btnLimpiar, btnCancelar, btnUsar);
        return footer;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lógica del grid
    // ─────────────────────────────────────────────────────────────────────

    private void poblarGrid(List<FontAwesomeSolid> iconos) {
        gridIconos.getChildren().clear();
        celdaActiva = null;

        if (iconos.isEmpty()) {
            Label lV = new Label("No se encontraron íconos con ese nombre.");
            lV.setStyle("-fx-font-size: 13px; -fx-text-fill: #A8A29E; -fx-font-style: italic;");
            lV.setPadding(new Insets(24, 0, 0, 0));
            gridIconos.getChildren().add(lV);
            return;
        }

        for (FontAwesomeSolid ikon : iconos) {
            VBox celda = crearCelda(ikon);
            gridIconos.getChildren().add(celda);

            // Pre-seleccionar si coincide con el ícono actual
            if (ikon.getDescription().equals(literalSeleccionado)) {
                seleccionarCelda(celda, ikon.getDescription());
            }
        }
    }

    private void filtrarGrid(String texto) {
        String q = texto == null ? "" : texto.trim().toLowerCase();

        List<FontAwesomeSolid> filtrados = q.isEmpty()
            ? TODOS
            : TODOS.stream()
                .filter(i -> i.getDescription().toLowerCase().contains(q))
                .toList();

        poblarGrid(filtrados);
    }

    private VBox crearCelda(FontAwesomeSolid ikon) {
        String literal = ikon.getDescription();
        String nombre  = literal.replace("fas-", "").replace("-", " ");

        // ── StackPane con el ícono ───────────────────────────────
        StackPane iconBox = new StackPane();
        iconBox.setMinWidth(40); iconBox.setMinHeight(40);
        iconBox.setMaxWidth(40); iconBox.setMaxHeight(40);
        iconBox.setStyle("-fx-background-color: #ECEEF7; -fx-background-radius: 8px;");

        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(19);
        icon.setIconColor(Paint.valueOf("#5A6ACF"));
        iconBox.getChildren().add(icon);

        // ── Label con el nombre ──────────────────────────────────
        Label lNombre = new Label(nombre);
        lNombre.setStyle(
            "-fx-font-size: 9px; -fx-text-fill: #A8A29E; " +
            "-fx-max-width: 64px; -fx-alignment: CENTER;"
        );
        lNombre.setMaxWidth(64);
        lNombre.setWrapText(false);
        lNombre.setEllipsisString("…");

        // ── Contenedor de la celda ───────────────────────────────
        VBox celda = new VBox(5, iconBox, lNombre);
        celda.setAlignment(Pos.CENTER);
        celda.setPadding(new Insets(8));
        celda.setMinWidth(74); celda.setPrefWidth(74); celda.setMaxWidth(74);
        celda.setMinHeight(74); celda.setMaxHeight(74);
        celda.setStyle(
            "-fx-background-color: #FDFCFA;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-color: transparent;" +
            "-fx-border-radius: 10px;" +
            "-fx-border-width: 1.5px;" +
            "-fx-cursor: hand;"
        );

        aplicarHoverCelda(celda, icon, lNombre);

        celda.setOnMouseClicked(e -> {
            seleccionarCelda(celda, literal);
            animarSeleccion(celda);
        });

        return celda;
    }

    /** Aplica los handlers de hover sin afectar el estado de selección */
    private void aplicarHoverCelda(VBox celda, FontIcon icon, Label lNombre) {
        celda.setOnMouseEntered(e -> {
            if (celda != celdaActiva) {
                celda.setStyle(
                    "-fx-background-color: #ECEEF7;" +
                    "-fx-background-radius: 10px;" +
                    "-fx-border-color: rgba(90,106,207,0.30);" +
                    "-fx-border-radius: 10px;" +
                    "-fx-border-width: 1.5px;" +
                    "-fx-cursor: hand;"
                );
                icon.setIconColor(Paint.valueOf("#4A58BF"));
                ScaleTransition sc = new ScaleTransition(Duration.millis(100), celda);
                sc.setToX(1.07); sc.setToY(1.07);
                sc.play();
            }
        });
        celda.setOnMouseExited(e -> {
            if (celda != celdaActiva) {
                aplicarEstiloCeldaNormal(celda, icon, lNombre);
                ScaleTransition sc = new ScaleTransition(Duration.millis(80), celda);
                sc.setToX(1.0); sc.setToY(1.0);
                sc.play();
            }
        });
    }

    /** Selecciona visualmente una celda y actualiza el estado global */
    private void seleccionarCelda(VBox celda, String literal) {
        // Restaurar la celda anterior
        if (celdaActiva != null && celdaActiva != celda) {
            VBox anterior = celdaActiva;
            // Buscar el ícono dentro de la celda anterior
            anterior.getChildren().forEach(n -> {
                if (n instanceof StackPane sp) {
                    sp.getChildren().forEach(c -> {
                        if (c instanceof FontIcon fi) fi.setIconColor(Paint.valueOf("#5A6ACF"));
                    });
                    sp.setStyle("-fx-background-color: #ECEEF7; -fx-background-radius: 8px;");
                }
                if (n instanceof Label l) {
                    l.setStyle(
                        "-fx-font-size: 9px; -fx-text-fill: #A8A29E; " +
                        "-fx-max-width: 64px; -fx-alignment: CENTER;"
                    );
                }
            });
            anterior.setStyle(
                "-fx-background-color: #FDFCFA;" +
                "-fx-background-radius: 10px;" +
                "-fx-border-color: transparent;" +
                "-fx-border-radius: 10px;" +
                "-fx-border-width: 1.5px;" +
                "-fx-cursor: hand;"
            );
            anterior.setScaleX(1.0);
            anterior.setScaleY(1.0);
        }

        // Marcar la nueva celda como activa
        celdaActiva = celda;
        literalSeleccionado = literal;

        celda.setStyle(
            "-fx-background-color: #ECEEF7;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-color: #5A6ACF;" +
            "-fx-border-radius: 10px;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        );
        // Actualizar icono y label dentro de la celda
        celda.getChildren().forEach(n -> {
            if (n instanceof StackPane sp) {
                sp.setStyle("-fx-background-color: #5A6ACF; -fx-background-radius: 8px;");
                sp.getChildren().forEach(c -> {
                    if (c instanceof FontIcon fi) fi.setIconColor(Paint.valueOf("#FFFFFF"));
                });
            }
            if (n instanceof Label l) {
                l.setStyle(
                    "-fx-font-size: 9px; -fx-text-fill: #3C48AA; -fx-font-weight: 700;" +
                    "-fx-max-width: 64px; -fx-alignment: CENTER;"
                );
            }
        });

        actualizarPreview(literal);
    }

    private void aplicarEstiloCeldaNormal(VBox celda, FontIcon icon, Label lNombre) {
        celda.setStyle(
            "-fx-background-color: #FDFCFA;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-color: transparent;" +
            "-fx-border-radius: 10px;" +
            "-fx-border-width: 1.5px;" +
            "-fx-cursor: hand;"
        );
        icon.setIconColor(Paint.valueOf("#5A6ACF"));
        lNombre.setStyle(
            "-fx-font-size: 9px; -fx-text-fill: #A8A29E; " +
            "-fx-max-width: 64px; -fx-alignment: CENTER;"
        );
    }

    /** Actualiza el preview en el footer */
    private void actualizarPreview(String literal) {
        if (previewIcono == null || lblPreviewNombre == null) return;
        try {
            previewIcono.setIconLiteral(literal);
            String nombre = literal.replace("fas-", "").replace("-", " ");
            // Capitalizar primera letra
            lblPreviewNombre.setText(
                Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1));
            lblPreviewNombre.setStyle("-fx-font-size: 12px; -fx-text-fill: #1A1F2E; -fx-font-weight: 600;");
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // Animaciones
    // ─────────────────────────────────────────────────────────────────────

    /** Rebote suave al seleccionar una celda */
    private void animarSeleccion(Node node) {
        ScaleTransition sc = new ScaleTransition(Duration.millis(80), node);
        sc.setFromX(1.0); sc.setFromY(1.0);
        sc.setToX(1.14); sc.setToY(1.14);
        sc.setAutoReverse(true);
        sc.setCycleCount(2);
        sc.setInterpolator(Interpolator.EASE_BOTH);
        sc.play();
    }
}
