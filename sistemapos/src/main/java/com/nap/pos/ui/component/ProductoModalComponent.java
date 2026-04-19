package com.nap.pos.ui.component;

import com.nap.pos.application.service.CategoriaService;
import com.nap.pos.application.service.ProductoService;
import com.nap.pos.application.service.ProveedorService;
import com.nap.pos.application.service.SubcategoriaService;
import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Categoria;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.Proveedor;
import com.nap.pos.domain.model.Subcategoria;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
import java.util.Locale;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ProductoModalComponent {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final SubcategoriaService subcategoriaService;
    private final ProveedorService proveedorService;
    private final CatalogoMiniModalComponent catalogoMiniModalComponent;

    private static final NumberFormat FMT_MONEDA =
            NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    public void abrirModalCrearProducto(
            StackPane rootStack,
            String nombreInicial,
            String stockPrompt,
            String stockInicial,
            Consumer<Producto> onProductoCreado,
            boolean animarExito
    ) {
        if (rootStack == null) return;

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("inventario-modal-overlay");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                cerrarModal(rootStack, overlay);
            }
        });

        VBox modal = new VBox(16);
        modal.getStyleClass().add("inventario-modal");
        modal.setMaxWidth(580);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icoModal = new FontIcon("fas-plus-circle");
        icoModal.setIconSize(20);
        icoModal.setIconColor(Paint.valueOf("#5A6ACF"));

        Label lblTitle = new Label("Nuevo Producto");
        lblTitle.getStyleClass().add("inventario-modal-title");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Button btnCerrar = new Button();
        FontIcon icoCerrar = new FontIcon("fas-times");
        icoCerrar.setIconSize(16);
        icoCerrar.setIconColor(Paint.valueOf("#78716C"));
        btnCerrar.setGraphic(icoCerrar);
        btnCerrar.getStyleClass().add("inventario-modal-close");
        btnCerrar.setOnAction(e -> cerrarModal(rootStack, overlay));

        header.getChildren().addAll(icoModal, lblTitle, btnCerrar);

        Separator sep = new Separator();

        VBox fieldNombre = crearCampo("Nombre del producto *");
        TextField txtNombre = (TextField) fieldNombre.getChildren().get(1);
        txtNombre.setPromptText("Ej: Coca-Cola 350ml");
        txtNombre.setText(nombreInicial == null ? "" : nombreInicial);

        VBox fieldCodigo = crearCampo("Código de barras *");
        TextField txtCodigo = (TextField) fieldCodigo.getChildren().get(1);
        txtCodigo.setPromptText("Ej: 7701234567890");

        HBox row1 = new HBox(14, fieldNombre, fieldCodigo);
        HBox.setHgrow(fieldNombre, Priority.ALWAYS);
        HBox.setHgrow(fieldCodigo, Priority.ALWAYS);

        VBox fieldCategoria = new VBox(6);
        Label lblCategoria = new Label("Categoría *");
        lblCategoria.getStyleClass().add("form-label");
        ComboBox<Categoria> cmbCat = new ComboBox<>();
        cmbCat.setPromptText("Seleccionar categoría");
        cmbCat.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cmbCat, Priority.ALWAYS);

        try {
            cmbCat.setItems(FXCollections.observableArrayList(categoriaService.findAllActivas()));
        } catch (Exception ignored) {}

        cmbCat.setConverter(new StringConverter<>() {
            @Override
            public String toString(Categoria c) {
                return c == null ? "" : c.getNombre();
            }

            @Override
            public Categoria fromString(String s) {
                return null;
            }
        });

        Button btnNuevaCat = new Button();
        FontIcon icoPlusCat = new FontIcon("fas-plus");
        icoPlusCat.setIconSize(11);
        icoPlusCat.setIconColor(Paint.valueOf("#5A6ACF"));
        btnNuevaCat.setGraphic(icoPlusCat);
        btnNuevaCat.getStyleClass().add("inventario-btn-masiva");

        HBox catRow = new HBox(6, cmbCat, btnNuevaCat);
        catRow.setAlignment(Pos.CENTER_LEFT);
        fieldCategoria.getChildren().addAll(lblCategoria, catRow);

        VBox fieldSubcategoria = new VBox(6);
        Label lblSubcat = new Label("Subcategoría *");
        lblSubcat.getStyleClass().add("form-label");
        ComboBox<Subcategoria> cmbSubcat = new ComboBox<>();
        cmbSubcat.setPromptText("Seleccionar subcategoría");
        cmbSubcat.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cmbSubcat, Priority.ALWAYS);
        cmbSubcat.setDisable(true);
        cmbSubcat.setConverter(new StringConverter<>() {
            @Override
            public String toString(Subcategoria s) {
                return s == null ? "" : s.getNombre();
            }

            @Override
            public Subcategoria fromString(String str) {
                return null;
            }
        });

        Button btnNuevaSubcat = new Button();
        FontIcon icoPlusSub = new FontIcon("fas-plus");
        icoPlusSub.setIconSize(11);
        icoPlusSub.setIconColor(Paint.valueOf("#5A6ACF"));
        btnNuevaSubcat.setGraphic(icoPlusSub);
        btnNuevaSubcat.getStyleClass().add("inventario-btn-masiva");
        btnNuevaSubcat.setDisable(true);

        HBox subcatRow = new HBox(6, cmbSubcat, btnNuevaSubcat);
        subcatRow.setAlignment(Pos.CENTER_LEFT);
        fieldSubcategoria.getChildren().addAll(lblSubcat, subcatRow);

        cmbCat.valueProperty().addListener((obs, old, cat) -> {
            cmbSubcat.getItems().clear();
            cmbSubcat.setValue(null);
            if (cat != null) {
                try {
                    cmbSubcat.setItems(FXCollections.observableArrayList(
                            subcategoriaService.findByCategoriaId(cat.getId())));
                    cmbSubcat.setDisable(false);
                    btnNuevaSubcat.setDisable(false);
                } catch (Exception ignored) {
                    cmbSubcat.setDisable(true);
                    btnNuevaSubcat.setDisable(true);
                }
            } else {
                cmbSubcat.setDisable(true);
                btnNuevaSubcat.setDisable(true);
            }
        });

        btnNuevaCat.setOnAction(e ->
                catalogoMiniModalComponent.abrirMiniModalCategoria(rootStack, cmbCat, cmbSubcat, btnNuevaSubcat));
        btnNuevaSubcat.setOnAction(e ->
                catalogoMiniModalComponent.abrirMiniModalSubcategoria(rootStack, cmbCat.getValue(), cmbSubcat));

        HBox row2 = new HBox(14, fieldCategoria, fieldSubcategoria);
        HBox.setHgrow(fieldCategoria, Priority.ALWAYS);
        HBox.setHgrow(fieldSubcategoria, Priority.ALWAYS);

        VBox fieldProveedor = new VBox(6);
        Label lblProv = new Label("Proveedor *");
        lblProv.getStyleClass().add("form-label");
        ComboBox<Proveedor> cmbProv = new ComboBox<>();
        cmbProv.setPromptText("Seleccionar proveedor");
        cmbProv.setMaxWidth(Double.MAX_VALUE);

        try {
            cmbProv.setItems(FXCollections.observableArrayList(proveedorService.findAllActivos()));
        } catch (Exception ignored) {}

        cmbProv.setConverter(new StringConverter<>() {
            @Override
            public String toString(Proveedor p) {
                return p == null ? "" : p.getNombre() + " (" + p.getPorcentajeGanancia() + "%)";
            }

            @Override
            public Proveedor fromString(String s) {
                return null;
            }
        });

        Label lblMargenBase = new Label();
        lblMargenBase.setStyle("-fx-font-size: 11px; -fx-text-fill: #5A6ACF; -fx-font-weight: 600;");
        lblMargenBase.setVisible(false);
        lblMargenBase.setManaged(false);
        fieldProveedor.getChildren().addAll(lblProv, cmbProv, lblMargenBase);

        VBox fieldPrecio = crearCampo("Precio de compra *");
        TextField txtPrecio = (TextField) fieldPrecio.getChildren().get(1);
        txtPrecio.setPromptText("Ej: 1500");

        HBox row3 = new HBox(14, fieldProveedor, fieldPrecio);
        HBox.setHgrow(fieldProveedor, Priority.ALWAYS);
        HBox.setHgrow(fieldPrecio, Priority.ALWAYS);

        VBox fieldStock = crearCampo("Stock inicial *");
        TextField txtStock = (TextField) fieldStock.getChildren().get(1);
        txtStock.setPromptText(stockPrompt == null || stockPrompt.isBlank() ? "Ej: 0" : stockPrompt);
        if (stockInicial != null && !stockInicial.isBlank()) {
            txtStock.setText(stockInicial);
        }

        cmbProv.valueProperty().addListener((obs, old, prov) -> {
            if (prov != null) {
                String margenStr = prov.getPorcentajeGanancia().stripTrailingZeros().toPlainString();
                lblMargenBase.setText("Margen del proveedor: " + margenStr + "%");
                lblMargenBase.setVisible(true);
                lblMargenBase.setManaged(true);
            } else {
                lblMargenBase.setVisible(false);
                lblMargenBase.setManaged(false);
            }
        });

        HBox row4 = new HBox(14, fieldStock);
        HBox.setHgrow(fieldStock, Priority.ALWAYS);

        File[] imgFileRef = {null};
        StackPane imgBox = new StackPane();
        imgBox.setStyle("-fx-background-color: #F5F1EB; -fx-background-radius: 8; " +
                        "-fx-border-color: rgba(26,31,46,0.12); -fx-border-radius: 8; -fx-border-width: 1;");
        imgBox.setMinSize(72, 72);
        imgBox.setMaxSize(72, 72);
        FontIcon icoImgPh = new FontIcon("fas-image");
        icoImgPh.setIconSize(22);
        icoImgPh.setIconColor(Paint.valueOf("#C8C2BB"));
        imgBox.getChildren().add(icoImgPh);

        FontIcon icoCamera = new FontIcon("fas-camera");
        icoCamera.setIconSize(12);
        icoCamera.setIconColor(Paint.valueOf("#5A6ACF"));
        Button btnSelImg = new Button("Seleccionar imagen", icoCamera);
        btnSelImg.getStyleClass().add("inventario-btn-masiva");
        btnSelImg.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleccionar imagen del producto");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"));
            File archivo = chooser.showOpenDialog(rootStack.getScene() != null ? rootStack.getScene().getWindow() : null);
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

        HBox previewBox = new HBox(10);
        previewBox.setAlignment(Pos.CENTER_LEFT);
        previewBox.getStyleClass().add("inventario-price-preview");

        FontIcon icoInfo = new FontIcon("fas-info-circle");
        icoInfo.setIconSize(14);
        icoInfo.setIconColor(Paint.valueOf("#5A6ACF"));

        Label lblPreview = new Label("El precio de venta se calcula automáticamente");
        lblPreview.getStyleClass().add("inventario-preview-text");

        previewBox.getChildren().addAll(icoInfo, lblPreview);

        Runnable calcularPreview = () -> {
            try {
                BigDecimal precioCompra = new BigDecimal(txtPrecio.getText().trim());
                Proveedor prov = cmbProv.getValue();
                if (prov == null) {
                    lblPreview.setText("Selecciona un proveedor para ver el precio de venta");
                    return;
                }

                BigDecimal margen = prov.getPorcentajeGanancia();
                BigDecimal factor = BigDecimal.ONE.add(margen.divide(new BigDecimal("100"), 10, java.math.RoundingMode.HALF_UP));
                BigDecimal precioVenta = precioCompra.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);

                lblPreview.setText("Precio de venta estimado: " + FMT_MONEDA.format(precioVenta)
                        + "  (margen " + margen + "%)");
                animarPulse(previewBox);
            } catch (Exception ignored) {
                lblPreview.setText("El precio de venta se calcula automáticamente");
            }
        };

        txtPrecio.textProperty().addListener((obs, o, n) -> calcularPreview.run());
        cmbProv.valueProperty().addListener((obs, o, n) -> calcularPreview.run());

        Label lblError = new Label();
        lblError.getStyleClass().add("inventario-error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);

        Separator sep2 = new Separator();

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("inventario-btn-cancelar");
        btnCancelar.setOnAction(e -> cerrarModal(rootStack, overlay));

        FontIcon icoGuardar = new FontIcon("fas-save");
        icoGuardar.setIconSize(14);
        icoGuardar.setIconColor(Paint.valueOf("#FFFFFF"));

        Button btnGuardar = new Button("Crear Producto", icoGuardar);
        btnGuardar.getStyleClass().add("inventario-btn-guardar");
        btnGuardar.setOnAction(e -> {
            lblError.setVisible(false);
            lblError.setManaged(false);

            try {
                String nombre = txtNombre.getText().trim();
                String codigo = txtCodigo.getText().trim();
                if (nombre.isEmpty() || codigo.isEmpty()) {
                    mostrarErrorModal(lblError, "El nombre y código de barras son obligatorios.");
                    return;
                }
                if (cmbCat.getValue() == null || cmbSubcat.getValue() == null) {
                    mostrarErrorModal(lblError, "Selecciona una categoría y subcategoría.");
                    return;
                }
                if (cmbProv.getValue() == null) {
                    mostrarErrorModal(lblError, "Selecciona un proveedor.");
                    return;
                }

                BigDecimal precioCompra;
                try {
                    precioCompra = new BigDecimal(txtPrecio.getText().trim());
                    if (precioCompra.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    mostrarErrorModal(lblError, "El precio de compra debe ser un número mayor a 0.");
                    return;
                }

                int stock;
                try {
                    stock = Integer.parseInt(txtStock.getText().trim());
                    if (stock < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    mostrarErrorModal(lblError, "El stock debe ser un número entero no negativo.");
                    return;
                }

                Producto producto = Producto.builder()
                        .nombre(nombre)
                        .codigoBarras(codigo)
                        .subcategoria(cmbSubcat.getValue())
                        .proveedorPrincipal(cmbProv.getValue())
                        .precioCompra(precioCompra)
                        .stock(stock)
                        .activo(true)
                        .build();
                producto.calcularPrecioVenta();

                Producto creado = productoService.crear(producto);
                guardarImagenProducto(creado, imgFileRef[0]);

                Runnable finalizar = () -> {
                    cerrarModal(rootStack, overlay);
                    if (onProductoCreado != null) {
                        onProductoCreado.accept(creado);
                    }
                };

                if (animarExito) {
                    animarExitoModal(modal, finalizar);
                } else {
                    finalizar.run();
                }

            } catch (BusinessException be) {
                mostrarErrorModal(lblError, be.getMessage());
            } catch (Exception ex) {
                mostrarErrorModal(lblError, "Error inesperado: " + ex.getMessage());
            }
        });

        actions.getChildren().addAll(btnCancelar, btnGuardar);

        modal.getChildren().addAll(header, sep, row1, row2, row3, row4,
                rowImagen, previewBox, lblError, sep2, actions);

        overlay.getChildren().add(modal);
        rootStack.getChildren().add(overlay);

        animarEntradaModal(overlay, modal);

        int delay = 60;
        for (Node child : new Node[]{row1, row2, row3, row4, rowImagen, previewBox, actions}) {
            child.setOpacity(0);
            child.setTranslateX(-8);
            FadeTransition ft = new FadeTransition(Duration.millis(250), child);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition tt = new TranslateTransition(Duration.millis(250), child);
            tt.setFromX(-8);
            tt.setToX(0);
            tt.setDelay(Duration.millis(delay));
            tt.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(ft, tt).play();
            delay += 50;
        }
    }

    private VBox crearCampo(String labelText) {
        VBox field = new VBox(6);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        TextField txt = new TextField();
        txt.setMaxWidth(Double.MAX_VALUE);
        field.getChildren().addAll(lbl, txt);
        return field;
    }

    private void guardarImagenProducto(Producto creado, File archivoImagen) {
        if (creado == null || archivoImagen == null) return;

        try {
            String ext = obtenerExtensionImg(archivoImagen.getName());
            Path carpeta = Paths.get(System.getProperty("user.home"), ".nappos", "assets", "products");
            Files.createDirectories(carpeta);
            Path destino = carpeta.resolve("producto_" + creado.getId() + "." + ext);
            Files.copy(archivoImagen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
            productoService.actualizarImagen(creado.getId(), destino.toAbsolutePath().toString());
        } catch (Exception ignored) {
            // La imagen es opcional: no bloquea la creación del producto.
        }
    }

    private String obtenerExtensionImg(String nombre) {
        int dot = nombre.lastIndexOf('.');
        return dot >= 0 ? nombre.substring(dot + 1).toLowerCase() : "png";
    }

    private void cerrarModal(StackPane rootStack, StackPane overlay) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);
        fadeOut.setOnFinished(e -> rootStack.getChildren().remove(overlay));
        fadeOut.play();
    }

    private void animarEntradaModal(StackPane overlay, VBox modal) {
        overlay.setOpacity(0);
        modal.setScaleX(0.90);
        modal.setScaleY(0.90);
        modal.setTranslateY(24);

        FadeTransition fadeOverlay = new FadeTransition(Duration.millis(250), overlay);
        fadeOverlay.setFromValue(0);
        fadeOverlay.setToValue(1);
        fadeOverlay.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleModal = new ScaleTransition(Duration.millis(320), modal);
        scaleModal.setFromX(0.90);
        scaleModal.setFromY(0.90);
        scaleModal.setToX(1);
        scaleModal.setToY(1);
        scaleModal.setInterpolator(Interpolator.SPLINE(0.25, 0.46, 0.45, 0.94));

        TranslateTransition slideModal = new TranslateTransition(Duration.millis(320), modal);
        slideModal.setFromY(24);
        slideModal.setToY(0);
        slideModal.setInterpolator(Interpolator.SPLINE(0.25, 0.46, 0.45, 0.94));

        new ParallelTransition(fadeOverlay, scaleModal, slideModal).play();
    }

    private void animarExitoModal(VBox modal, Runnable onFinished) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), modal);
        scale.setToX(1.03);
        scale.setToY(1.03);
        scale.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(100), modal);
        scaleBack.setToX(1.0);
        scaleBack.setToY(1.0);
        scaleBack.setInterpolator(Interpolator.EASE_IN);

        scale.setOnFinished(e -> {
            scaleBack.setOnFinished(e2 -> onFinished.run());
            scaleBack.play();
        });
        scale.play();
    }

    private void mostrarErrorModal(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), lbl);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition shake = new TranslateTransition(Duration.millis(50), lbl);
        shake.setFromX(0);
        shake.setByX(6);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setInterpolator(Interpolator.EASE_BOTH);

        fadeIn.setOnFinished(e -> shake.play());
        fadeIn.play();
    }

    private void animarPulse(Node node) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(120), node);
        pulse.setToX(1.02);
        pulse.setToY(1.02);
        pulse.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition back = new ScaleTransition(Duration.millis(120), node);
        back.setToX(1.0);
        back.setToY(1.0);
        back.setInterpolator(Interpolator.EASE_IN);

        pulse.setOnFinished(e -> back.play());
        pulse.play();
    }
}
