package com.nap.pos.ui.controller;

import com.nap.pos.application.service.ConfiguracionService;
import com.nap.pos.application.service.UsuarioService;
import com.nap.pos.domain.model.ConfiguracionTienda;
import com.nap.pos.domain.model.enums.RegimenTributario;
import com.nap.pos.domain.model.enums.Rol;
import com.nap.pos.domain.model.enums.TipoPersona;
import javafx.collections.FXCollections;
import com.nap.pos.Launcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;

@Component
@RequiredArgsConstructor
public class SetupWizardController {

    private final ConfiguracionService configuracionService;
    private final UsuarioService usuarioService;

    // ── Navegación de pasos ──────────────────────────────────────────────
    @FXML private VBox paso1;
    @FXML private VBox paso2;
    @FXML private VBox paso3;
    @FXML private VBox paso4;

    // ── Indicadores de paso ──────────────────────────────────────────────
    @FXML private Label indicador1;
    @FXML private Label indicador2;
    @FXML private Label indicador3;
    @FXML private Label indicador4;

    // ── Botones de navegación ────────────────────────────────────────────
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;

    // ── Paso 1: tipo de persona ──────────────────────────────────────────
    @FXML private VBox tarjetaNatural;
    @FXML private VBox tarjetaJuridica;
    @FXML private RadioButton rbNatural;
    @FXML private RadioButton rbJuridica;

    // ── Paso 2: datos básicos + contacto ────────────────────────────────
    @FXML private TextField txtNombreTienda;
    @FXML private VBox grupoNatural;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtCedula;
    @FXML private VBox grupoJuridica;
    @FXML private TextField txtRazonSocial;
    @FXML private TextField txtNit;
    @FXML private TextField txtRepLegalNombre;
    @FXML private TextField txtRepLegalApellido;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;

    // ── Paso 3: logo, dirección y configuración operativa ───────────────
    @FXML private ImageView imgLogo;
    @FXML private Label lblRutaLogo;
    @FXML private TextArea txtDireccion;
    @FXML private Spinner<Integer> spnStockMinimo;
    @FXML private ComboBox<RegimenTributario> cmbRegimen;
    @FXML private VBox vboxIva;
    @FXML private Label lblInfoIva;
    @FXML private Spinner<Integer> spnIva;
    @FXML private CheckBox chkPrecioConIvaIncluido;
    @FXML private Spinner<Integer> spnGanancia;
    @FXML private TextField txtPrefijo;
    @FXML private Spinner<Integer> spnNumeroInicial;
    @FXML private DatePicker dpFechaInventario;

    // ── Paso 4: credenciales del administrador ──────────────────────────
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtPasswordConfirm;
    @FXML private Label lblPasswordError;

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
        spnStockMinimo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999, 5));
        spnIva.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 19));
        spnGanancia.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 500, 30));
        spnNumeroInicial.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999_999, 1));
        txtPrefijo.setText("FAC-");
        cmbRegimen.setItems(FXCollections.observableArrayList(RegimenTributario.values()));
        cmbRegimen.setValue(RegimenTributario.REGIMEN_ORDINARIO);
        // vboxIva arranca visible porque el régimen por defecto es ORDINARIO
        vboxIva.setVisible(true);
        vboxIva.setManaged(true);
        actualizarGrupoCampos();
        mostrarPaso(0);
    }

    // ── Handler régimen tributario ───────────────────────────────────────

    @FXML
    public void onRegimenChanged() {
        RegimenTributario regimen = cmbRegimen.getValue();
        boolean esResponsableIva = regimen != RegimenTributario.NO_RESPONSABLE_IVA;

        // vboxIva visible para Ordinario y RST — ambos cobran IVA
        vboxIva.setVisible(esResponsableIva);
        vboxIva.setManaged(esResponsableIva);

        String infoTexto = switch (regimen) {
            case NO_RESPONSABLE_IVA ->
                "En este regimen no cobras IVA a tus clientes. El IVA que pagas en " +
                "tus compras se trata como un costo del negocio y se incluye directamente " +
                "en el precio de los productos. El precio de venta es el precio final.";
            case REGIMEN_SIMPLE ->
                "En el RST (Regimen Simple de Tributacion) pagas un impuesto " +
                "unificado, pero el IVA sigue siendo un impuesto aparte. Si tu " +
                "actividad es tienda pequena, minimercado o peluqueria (Grupo 1, " +
                "Art. 908 ET), o eres persona natural con ingresos < 3.500 UVT, " +
                "NO eres responsable de IVA - deja el porcentaje en 0 %. En caso " +
                "contrario debes discriminar el IVA en tus ventas. El RST exige " +
                "siempre factura electronica o POS electronico.";
            default -> "";
        };

        lblInfoIva.setText(infoTexto);
        lblInfoIva.setVisible(!infoTexto.isEmpty());
        lblInfoIva.setManaged(!infoTexto.isEmpty());

        if (!infoTexto.isEmpty()) {
            FontIcon infoIcon = FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, 16);
            infoIcon.getStyleClass().add("info-regimen-icon");
            lblInfoIva.setGraphic(infoIcon);
        } else {
            lblInfoIva.setGraphic(null);
        }
    }

    // ── Handlers paso 1 ─────────────────────────────────────────────────

    @FXML
    public void onNatural() {
        tipoSeleccionado = TipoPersona.NATURAL;
        rbNatural.setSelected(true);
        actualizarTarjetas();
        actualizarGrupoCampos();
        cmbRegimen.setItems(FXCollections.observableArrayList(RegimenTributario.values()));
    }

    @FXML
    public void onJuridica() {
        tipoSeleccionado = TipoPersona.JURIDICA;
        rbJuridica.setSelected(true);
        actualizarTarjetas();
        actualizarGrupoCampos();
        // Las personas jurídicas no pueden ser "No responsable de IVA"
        cmbRegimen.setItems(FXCollections.observableArrayList(
                RegimenTributario.REGIMEN_ORDINARIO,
                RegimenTributario.REGIMEN_SIMPLE));
        cmbRegimen.setValue(RegimenTributario.REGIMEN_ORDINARIO);
        onRegimenChanged();
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
                    System.getProperty("user.home"), ".nappos", "assets");
            Files.createDirectories(carpetaDestino);

            Path destino = carpetaDestino.resolve("logo." + extension);
            Files.copy(archivoOrigen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

            rutaLogoSeleccionada = destino.toAbsolutePath().toString();
            imgLogo.setImage(new Image(destino.toUri().toString()));
            FontIcon okIcon = FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 14);
            okIcon.getStyleClass().add("logo-ok-icon");
            lblRutaLogo.setGraphic(okIcon);
            lblRutaLogo.setText("logo." + extension + " — guardado en datos de la aplicacion");
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

        // Crear usuario administrador primero (la config lo referencia)
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        var admin = usuarioService.crear(username, password, Rol.ADMIN);

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
                .representanteLegalNombre(tipoSeleccionado == TipoPersona.JURIDICA
                        ? txtRepLegalNombre.getText().trim() : null)
                .representanteLegalApellido(tipoSeleccionado == TipoPersona.JURIDICA
                        ? txtRepLegalApellido.getText().trim() : null)
                .telefono(txtTelefono.getText().trim())
                .correo(txtCorreo.getText().isBlank() ? null : txtCorreo.getText().trim())
                .direccion(txtDireccion.getText().trim())
                .rutaLogo(rutaLogoSeleccionada)
                .stockMinimoGlobal(spnStockMinimo.getValue())
                .regimenTributario(cmbRegimen.getValue())
                // IVA aplica tanto para Régimen Ordinario como para RST
                .ivaPorDefecto(cmbRegimen.getValue() != RegimenTributario.NO_RESPONSABLE_IVA
                        ? spnIva.getValue() : 0)
                .precioConIvaIncluido(chkPrecioConIvaIncluido.isSelected())
                .porcentajeGananciaGlobal(spnGanancia.getValue())
                .prefijoComprobante(txtPrefijo.getText().trim())
                .numeroInicialComprobante(spnNumeroInicial.getValue())
                .fechaInventarioAnual(dpFechaInventario.getValue())
                .propietarioId(admin.getId())
                .build();

        configuracionService.guardar(config);

        Stage wizardStage = (Stage) paso1.getScene().getWindow();
        wizardStage.close();

        // Abre la pantalla de login (la tienda ya está configurada)
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(Launcher.getContext()::getBean);
            Stage loginStage = new Stage();
            loginStage.setTitle(config.getNombreTienda() + " — NAP POS");
            loginStage.setScene(new javafx.scene.Scene(loader.load(), 900, 620));
            loginStage.setResizable(false);
            loginStage.centerOnScreen();
            loginStage.show();
        } catch (Exception e) {
            mostrarError("Error al abrir la pantalla de login: " + e.getMessage());
        }
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private void mostrarPaso(int paso) {
        pasoActual = paso;

        setVisible(paso1, paso == 0);
        setVisible(paso2, paso == 1);
        setVisible(paso3, paso == 2);
        setVisible(paso4, paso == 3);

        btnAnterior.setVisible(paso > 0);

        // Al entrar al paso 4, generar el username por defecto
        if (paso == 3) {
            generarUsernameAutomatico();
        }

        if (paso == 3) {
            btnSiguiente.setText("Finalizar");
            FontIcon checkIcon = FontIcon.of(FontAwesomeSolid.CHECK, 13);
            checkIcon.getStyleClass().add("btn-primario-icon");
            btnSiguiente.setGraphic(checkIcon);
            btnSiguiente.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            btnSiguiente.setOnAction(e -> onFinalizar());
        } else {
            btnSiguiente.setText("Siguiente");
            FontIcon arrowIcon = FontIcon.of(FontAwesomeSolid.ARROW_RIGHT, 13);
            arrowIcon.getStyleClass().add("btn-primario-icon");
            btnSiguiente.setGraphic(arrowIcon);
            btnSiguiente.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            btnSiguiente.setOnAction(e -> onSiguiente());
        }

        actualizarIndicadores(paso);
    }

    private void actualizarIndicadores(int paso) {
        indicador1.getStyleClass().setAll(paso >= 0 ? "indicador-activo" : "indicador-inactivo");
        indicador2.getStyleClass().setAll(paso >= 1 ? "indicador-activo" : "indicador-inactivo");
        indicador3.getStyleClass().setAll(paso >= 2 ? "indicador-activo" : "indicador-inactivo");
        indicador4.getStyleClass().setAll(paso >= 3 ? "indicador-activo" : "indicador-inactivo");
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
            case 3 -> validarPaso4();
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
            if (txtRepLegalNombre.getText().isBlank()
                    || txtRepLegalApellido.getText().isBlank()) {
                mostrarError("El nombre y apellido del representante legal son obligatorios.");
                return false;
            }
        }
        if (txtTelefono.getText().isBlank()) {
            mostrarError("El teléfono de contacto es obligatorio.");
            return false;
        }
        return true;
    }

    private boolean validarPaso3() {
        if (txtDireccion.getText().isBlank()) {
            mostrarError("La dirección del negocio es obligatoria.");
            return false;
        }
        if (txtPrefijo.getText().isBlank()) {
            mostrarError("El prefijo de comprobante es obligatorio.");
            return false;
        }
        return true;
    }

    private boolean validarPaso4() {
        lblPasswordError.setVisible(false);
        lblPasswordError.setManaged(false);

        String username = txtUsername.getText().trim();
        if (username.isBlank()) {
            mostrarErrorPassword("El nombre de usuario es obligatorio.");
            return false;
        }
        if (username.length() < 3) {
            mostrarErrorPassword("El nombre de usuario debe tener al menos 3 caracteres.");
            return false;
        }
        String password = txtPassword.getText();
        if (password.isBlank()) {
            mostrarErrorPassword("La contraseña es obligatoria.");
            return false;
        }
        if (password.length() < 6) {
            mostrarErrorPassword("La contraseña debe tener al menos 6 caracteres.");
            return false;
        }
        if (!password.equals(txtPasswordConfirm.getText())) {
            mostrarErrorPassword("Las contraseñas no coinciden.");
            return false;
        }
        return true;
    }

    /**
     * Genera el username por defecto a partir del nombre y apellido.
     * Para persona natural: inicial del nombre + apellido (ej: nmolina).
     * Para persona jurídica: inicial del nombre + apellido del representante legal.
     * Solo se genera si el campo está vacío (no sobreescribe si el usuario ya lo cambió).
     */
    private void generarUsernameAutomatico() {
        if (!txtUsername.getText().isBlank()) return; // no sobreescribir si ya tiene valor

        String nombre;
        String apellido;

        if (tipoSeleccionado == TipoPersona.NATURAL) {
            nombre = txtNombre.getText().trim();
            apellido = txtApellido.getText().trim();
        } else {
            nombre = txtRepLegalNombre.getText().trim();
            apellido = txtRepLegalApellido.getText().trim();
        }

        if (!nombre.isEmpty() && !apellido.isEmpty()) {
            String raw = nombre.charAt(0) + apellido;
            // Normalizar: quitar tildes (é→e, ñ→n, etc.) y caracteres especiales
            String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")       // quitar diacríticos
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "");    // solo alfanuméricos
            txtUsername.setText(normalized);
        }
    }

    private void mostrarErrorPassword(String mensaje) {
        lblPasswordError.setText(mensaje);
        lblPasswordError.setVisible(true);
        lblPasswordError.setManaged(true);
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Datos incompletos");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
