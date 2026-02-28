package com.mektos.pos.ui.controller;

import com.mektos.pos.application.service.ConfiguracionService;
import com.mektos.pos.domain.model.ConfiguracionTienda;
import com.mektos.pos.domain.model.enums.TipoPersona;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
@RequiredArgsConstructor
public class SetupWizardController {

    private final ConfiguracionService configuracionService;

    // ── Navegación de pasos ──────────────────────────────────────────────
    @FXML private VBox paso1;
    @FXML private VBox paso2;
    @FXML private VBox paso3;

    // ── Indicadores de paso ──────────────────────────────────────────────
    @FXML private Label indicador1;
    @FXML private Label indicador2;
    @FXML private Label indicador3;

    // ── Botones de navegación ────────────────────────────────────────────
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;

    // ── Paso 1: tipo de persona ──────────────────────────────────────────
    @FXML private VBox tarjetaNatural;
    @FXML private VBox tarjetaJuridica;
    @FXML private RadioButton rbNatural;
    @FXML private RadioButton rbJuridica;

    // ── Paso 2: datos básicos ────────────────────────────────────────────
    @FXML private TextField txtNombreTienda;
    @FXML private VBox grupoNatural;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtCedula;
    @FXML private VBox grupoJuridica;
    @FXML private TextField txtRazonSocial;
    @FXML private TextField txtNit;

    // ── Paso 3: logo y dirección ─────────────────────────────────────────
    @FXML private ImageView imgLogo;
    @FXML private Label lblRutaLogo;
    @FXML private TextArea txtDireccion;

    // ── Estado interno ───────────────────────────────────────────────────
    private int pasoActual = 0;
    private TipoPersona tipoSeleccionado = TipoPersona.NATURAL;
    private String rutaLogoSeleccionada;

    private final ToggleGroup grupoTipo = new ToggleGroup();

    @FXML
    public void initialize() {
        rbNatural.setToggleGroup(grupoTipo);
        rbJuridica.setToggleGroup(grupoTipo);
        rbNatural.setSelected(true);
        actualizarGrupoCampos();
        mostrarPaso(0);
    }

    // ── Handlers paso 1 ─────────────────────────────────────────────────

    @FXML
    public void onNatural() {
        tipoSeleccionado = TipoPersona.NATURAL;
        rbNatural.setSelected(true);
        actualizarTarjetas();
        actualizarGrupoCampos();
    }

    @FXML
    public void onJuridica() {
        tipoSeleccionado = TipoPersona.JURIDICA;
        rbJuridica.setSelected(true);
        actualizarTarjetas();
        actualizarGrupoCampos();
    }

    // ── Handlers de navegación ───────────────────────────────────────────

    @FXML
    public void onSiguiente() {
        if (!validarPasoActual()) return;
        mostrarPaso(pasoActual + 1);
    }

    @FXML
    public void onAnterior() {
        mostrarPaso(pasoActual - 1);
    }

    // ── Handler logo ─────────────────────────────────────────────────────

    @FXML
    public void onSeleccionarLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar logo de la tienda");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File archivoOrigen = chooser.showOpenDialog(imgLogo.getScene().getWindow());
        if (archivoOrigen == null) return;

        try {
            // Copia el archivo a una carpeta gestionada por la app.
            // Así el logo nunca se pierde si el usuario mueve o borra el archivo original.
            String extension = obtenerExtension(archivoOrigen.getName());
            Path carpetaDestino = Paths.get(
                    System.getProperty("user.home"), ".mektos", "assets");
            Files.createDirectories(carpetaDestino);

            Path destino = carpetaDestino.resolve("logo." + extension);
            Files.copy(archivoOrigen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

            rutaLogoSeleccionada = destino.toAbsolutePath().toString();
            imgLogo.setImage(new Image(destino.toUri().toString()));
            lblRutaLogo.setText("logo." + extension + "   ✓ guardado en datos de la aplicación");
        } catch (IOException e) {
            mostrarError("No se pudo guardar el logo: " + e.getMessage());
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        if (punto >= 0 && punto < nombreArchivo.length() - 1) {
            return nombreArchivo.substring(punto + 1).toLowerCase();
        }
        return "png";
    }

    // ── Handler finalizar ────────────────────────────────────────────────

    @FXML
    public void onFinalizar() {
        if (!validarPasoActual()) return;

        ConfiguracionTienda config = ConfiguracionTienda.builder()
                .id(1L)
                .tipoPersona(tipoSeleccionado)
                .nombreTienda(txtNombreTienda.getText().trim())
                .nombre(tipoSeleccionado == TipoPersona.NATURAL
                        ? txtNombre.getText().trim() : null)
                .apellido(tipoSeleccionado == TipoPersona.NATURAL
                        ? txtApellido.getText().trim() : null)
                .cedula(tipoSeleccionado == TipoPersona.NATURAL
                        ? txtCedula.getText().trim() : null)
                .razonSocial(tipoSeleccionado == TipoPersona.JURIDICA
                        ? txtRazonSocial.getText().trim() : null)
                .nit(tipoSeleccionado == TipoPersona.JURIDICA
                        ? txtNit.getText().trim() : null)
                .direccion(txtDireccion.getText().trim())
                .rutaLogo(rutaLogoSeleccionada)
                .build();

        configuracionService.guardar(config);

        Stage wizardStage = (Stage) paso1.getScene().getWindow();
        wizardStage.close();

        // Abre la ventana principal con el nombre de la tienda recién configurado
        try {
            Stage mainStage = new Stage();
            // TODO: reemplazar por FXMLLoader cuando main_window.fxml esté listo
            mainStage.setTitle(config.getNombreTienda() + " — NAP POS");
            mainStage.setWidth(1280);
            mainStage.setHeight(800);
            mainStage.centerOnScreen();
            mainStage.show();
        } catch (Exception e) {
            mostrarError("Error al abrir la ventana principal: " + e.getMessage());
        }
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private void mostrarPaso(int paso) {
        pasoActual = paso;

        setVisible(paso1, paso == 0);
        setVisible(paso2, paso == 1);
        setVisible(paso3, paso == 2);

        btnAnterior.setVisible(paso > 0);

        if (paso == 2) {
            btnSiguiente.setText("Finalizar ✓");
            btnSiguiente.setOnAction(e -> onFinalizar());
        } else {
            btnSiguiente.setText("Siguiente →");
            btnSiguiente.setOnAction(e -> onSiguiente());
        }

        actualizarIndicadores(paso);
    }

    private void actualizarIndicadores(int paso) {
        indicador1.getStyleClass().setAll(paso >= 0 ? "indicador-activo" : "indicador-inactivo");
        indicador2.getStyleClass().setAll(paso >= 1 ? "indicador-activo" : "indicador-inactivo");
        indicador3.getStyleClass().setAll(paso >= 2 ? "indicador-activo" : "indicador-inactivo");
    }

    private void actualizarTarjetas() {
        tarjetaNatural.getStyleClass().remove("tarjeta-seleccionada");
        tarjetaJuridica.getStyleClass().remove("tarjeta-seleccionada");
        if (tipoSeleccionado == TipoPersona.NATURAL) {
            tarjetaNatural.getStyleClass().add("tarjeta-seleccionada");
        } else {
            tarjetaJuridica.getStyleClass().add("tarjeta-seleccionada");
        }
    }

    private void actualizarGrupoCampos() {
        boolean esNatural = tipoSeleccionado == TipoPersona.NATURAL;
        setVisible(grupoNatural, esNatural);
        setVisible(grupoJuridica, !esNatural);
    }

    private void setVisible(VBox node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private boolean validarPasoActual() {
        return switch (pasoActual) {
            case 0 -> true; // solo elegir tipo, siempre válido
            case 1 -> validarPaso2();
            case 2 -> validarPaso3();
            default -> true;
        };
    }

    private boolean validarPaso2() {
        if (txtNombreTienda.getText().isBlank()) {
            mostrarError("El nombre de la tienda es obligatorio.");
            return false;
        }
        if (tipoSeleccionado == TipoPersona.NATURAL) {
            if (txtNombre.getText().isBlank()
                    || txtApellido.getText().isBlank()
                    || txtCedula.getText().isBlank()) {
                mostrarError("Nombre, apellido y cédula son obligatorios.");
                return false;
            }
        } else {
            if (txtRazonSocial.getText().isBlank() || txtNit.getText().isBlank()) {
                mostrarError("Razón social y NIT son obligatorios.");
                return false;
            }
        }
        return true;
    }

    private boolean validarPaso3() {
        if (txtDireccion.getText().isBlank()) {
            mostrarError("La dirección del negocio es obligatoria.");
            return false;
        }
        return true;
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Datos incompletos");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
