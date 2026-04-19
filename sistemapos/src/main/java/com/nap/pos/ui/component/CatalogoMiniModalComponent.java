package com.nap.pos.ui.component;

import com.nap.pos.application.service.CategoriaService;
import com.nap.pos.application.service.SubcategoriaService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.model.Subcategoria;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
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

@Component
@RequiredArgsConstructor
public class CatalogoMiniModalComponent {

    private final CategoriaService categoriaService;
    private final SubcategoriaService subcategoriaService;

    public void abrirMiniModalCategoria(
            StackPane rootStack,
            ComboBox<Categoria> cmbCat,
            ComboBox<Subcategoria> cmbSubcat,
            Button btnNuevaSubcat
    ) {
        if (rootStack == null) return;

        StackPane overlay = crearOverlay(rootStack);
        VBox modal = crearModalBase();

        HBox hdr = new HBox(10);
        hdr.setAlignment(Pos.CENTER_LEFT);

        FontIcon icoT = new FontIcon("fas-tag");
        icoT.setIconSize(15);
        icoT.setIconColor(Paint.valueOf("#5A6ACF"));

        Label lblTit = new Label("Nueva Categoría");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);

        Button btnX = new Button();
        FontIcon icoX = new FontIcon("fas-times");
        icoX.setIconSize(13);
        icoX.setIconColor(Paint.valueOf("#78716C"));
        btnX.setGraphic(icoX);
        btnX.getStyleClass().add("inventario-modal-close");
        btnX.setOnAction(e -> cerrarModal(rootStack, overlay));

        hdr.getChildren().addAll(icoT, lblTit, btnX);

        VBox fieldNom = crearCampo("Nombre de la categoría *", "Ej: Ropa, Electrónica...");
        TextField txtNom = (TextField) fieldNom.getChildren().get(1);

        Label lblErr = new Label();
        lblErr.getStyleClass().add("inventario-error-label");
        lblErr.setVisible(false);
        lblErr.setManaged(false);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> cerrarModal(rootStack, overlay));

        FontIcon icoSave = new FontIcon("fas-save");
        icoSave.setIconSize(13);
        icoSave.setIconColor(Paint.valueOf("#FFFFFF"));

        Button btnGuardar = new Button("Crear", icoSave);
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setOnAction(e -> {
            String nombre = txtNom.getText().trim();
            if (nombre.isEmpty()) {
                mostrarErrorModal(lblErr, "El nombre es obligatorio.");
                return;
            }
            try {
                Categoria nueva = Categoria.builder().nombre(nombre).activo(true).build();
                Categoria creada = categoriaService.crear(nueva);

                cmbCat.getItems().add(creada);
                cmbCat.setValue(creada);
                cmbSubcat.getItems().clear();
                cmbSubcat.setValue(null);
                cmbSubcat.setDisable(false);
                btnNuevaSubcat.setDisable(false);

                cerrarModal(rootStack, overlay);
            } catch (BusinessException be) {
                mostrarErrorModal(lblErr, be.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblErr, "Error al crear la categoría.");
            }
        });

        actions.getChildren().addAll(btnCancelar, btnGuardar);

        modal.getChildren().addAll(hdr, new Separator(), fieldNom, lblErr, actions);
        mostrarModal(rootStack, overlay, modal);
    }

    public void abrirMiniModalSubcategoria(
            StackPane rootStack,
            Categoria categoria,
            ComboBox<Subcategoria> cmbSubcat
    ) {
        if (rootStack == null || categoria == null) return;

        StackPane overlay = crearOverlay(rootStack);
        VBox modal = crearModalBase();

        HBox hdr = new HBox(10);
        hdr.setAlignment(Pos.CENTER_LEFT);

        FontIcon icoT = new FontIcon("fas-tag");
        icoT.setIconSize(15);
        icoT.setIconColor(Paint.valueOf("#D97706"));

        Label lblTit = new Label("Nueva Subcategoría");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1A1F2E;");
        HBox.setHgrow(lblTit, Priority.ALWAYS);

        Button btnX = new Button();
        FontIcon icoX = new FontIcon("fas-times");
        icoX.setIconSize(13);
        icoX.setIconColor(Paint.valueOf("#78716C"));
        btnX.setGraphic(icoX);
        btnX.getStyleClass().add("inventario-modal-close");
        btnX.setOnAction(e -> cerrarModal(rootStack, overlay));

        hdr.getChildren().addAll(icoT, lblTit, btnX);

        Label lblCatInfo = new Label("Categoría: " + categoria.getNombre());
        lblCatInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C; " +
                "-fx-background-color: #F5F1EB; -fx-background-radius: 6; -fx-padding: 6 10;");

        VBox fieldNom = crearCampo("Nombre de la subcategoría *", "Ej: Camisas, Zapatos...");
        TextField txtNom = (TextField) fieldNom.getChildren().get(1);

        Label lblErr = new Label();
        lblErr.getStyleClass().add("inventario-error-label");
        lblErr.setVisible(false);
        lblErr.setManaged(false);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> cerrarModal(rootStack, overlay));

        FontIcon icoSave = new FontIcon("fas-save");
        icoSave.setIconSize(13);
        icoSave.setIconColor(Paint.valueOf("#FFFFFF"));

        Button btnGuardar = new Button("Crear", icoSave);
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setOnAction(e -> {
            String nombre = txtNom.getText().trim();
            if (nombre.isEmpty()) {
                mostrarErrorModal(lblErr, "El nombre es obligatorio.");
                return;
            }
            try {
                Subcategoria nueva = Subcategoria.builder().nombre(nombre).categoria(categoria).activo(true).build();
                Subcategoria creada = subcategoriaService.crear(nueva);

                cmbSubcat.getItems().add(creada);
                cmbSubcat.setValue(creada);

                cerrarModal(rootStack, overlay);
            } catch (BusinessException be) {
                mostrarErrorModal(lblErr, be.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblErr, "Error al crear la subcategoría.");
            }
        });

        actions.getChildren().addAll(btnCancelar, btnGuardar);

        modal.getChildren().addAll(hdr, new Separator(), lblCatInfo, fieldNom, lblErr, actions);
        mostrarModal(rootStack, overlay, modal);
    }

    private StackPane crearOverlay(StackPane rootStack) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                cerrarModal(rootStack, overlay);
            }
        });
        return overlay;
    }

    private VBox crearModalBase() {
        VBox modal = new VBox(16);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(380);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setPadding(new Insets(22));
        return modal;
    }

    private VBox crearCampo(String labelText, String promptText) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");

        TextField txt = new TextField();
        txt.setPromptText(promptText);
        txt.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(6);
        box.getChildren().addAll(lbl, txt);
        return box;
    }

    private void mostrarModal(StackPane rootStack, StackPane overlay, VBox modal) {
        overlay.getChildren().add(modal);
        StackPane.setAlignment(modal, Pos.CENTER);
        rootStack.getChildren().add(overlay);

        overlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), overlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        modal.setTranslateY(12);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), modal);
        tt.setFromY(12);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        tt.play();
    }

    private void cerrarModal(StackPane rootStack, StackPane overlay) {
        FadeTransition ft = new FadeTransition(Duration.millis(160), overlay);
        ft.setToValue(0);
        ft.setOnFinished(e -> rootStack.getChildren().remove(overlay));
        ft.play();
    }

    private void mostrarErrorModal(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
        animarEntrada(lbl);
    }

    private void animarEntrada(Node node) {
        node.setOpacity(0);
        node.setTranslateY(12);

        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setFromY(12);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }
}
