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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
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

    // ── Sidebar stepper ──────────────────────────────────────────────────
    @FXML private HBox hboxStep1;
    @FXML private HBox hboxStep2;
    @FXML private HBox hboxStep3;
    @FXML private HBox hboxStep4;

    @FXML private Label indicador1;
    @FXML private Label indicador2;
    @FXML private Label indicador3;
    @FXML private Label indicador4;

    @FXML private Label lblStep1Title;
    @FXML private Label lblStep2Title;
    @FXML private Label lblStep3Title;
    @FXML private Label lblStep4Title;

    @FXML private Label lblStep1Sub;

    @FXML private Label lblProgreso;
    @FXML private Label lblPorcentaje;
    @FXML private ProgressBar pbProgreso;

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
    @FXML private ComboBox<String> cmbMesInventario;
    @FXML private ComboBox<Integer> cmbDiaInventario;

    // ── Paso 4: credenciales del administrador ──────────────────────
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private PasswordField txtPasswordConfirm;
    @FXML private TextField txtPasswordConfirmVisible;
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

        // ── Filtros de entrada en tiempo real ────────────────────────────
        // Solo letras, espacios y tildes (nombres/apellidos)
        txtNombre.setTextFormatter(filtroLetras(100));
        txtApellido.setTextFormatter(filtroLetras(100));
        txtRepLegalNombre.setTextFormatter(filtroLetras(100));
        txtRepLegalApellido.setTextFormatter(filtroLetras(100));

        // Solo dígitos (cédula colombiana: hasta 10-12 dígitos)
        txtCedula.setTextFormatter(filtroDigitos(12));

        // NIT colombiano: dígitos + guión + dígito verificación (ej: 900123456-1)
        txtNit.setTextFormatter(filtroNit(15));

        // Teléfono: solo dígitos (celular colombiano: 10 dígitos)
        txtTelefono.setTextFormatter(filtroDigitos(10));

        // Correo: sin espacios, máx 100 caracteres
        txtCorreo.setTextFormatter(filtroSinEspacios(100));

        // Prefijo comprobante: alfanumérico + guión, máx 10
        txtPrefijo.setTextFormatter(filtroPrefijo(10));

        // Username: alfanumérico, punto, guión bajo — sin espacios ni tildes
        txtUsername.setTextFormatter(filtroUsername(30));

        // Nombre de tienda: longitud máxima
        txtNombreTienda.setTextFormatter(filtroLongitud(150));

        // Razón social: longitud máxima
        txtRazonSocial.setTextFormatter(filtroLongitud(200));

        // Spinners: proteger contra texto inválido en modo editable
        protegerSpinner(spnStockMinimo);
        protegerSpinner(spnIva);
        protegerSpinner(spnGanancia);
        protegerSpinner(spnNumeroInicial);

        // ── Inventario anual: ComboBox mes + día ────────────────────────
        String[] meses = {"", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        cmbMesInventario.setItems(FXCollections.observableArrayList(meses));
        cmbMesInventario.setValue(""); // vacío = no configurado
        cmbDiaInventario.setItems(FXCollections.observableArrayList(generarDias(31)));
        cmbDiaInventario.setValue(null);
        // Al cambiar el mes, ajustar los días disponibles
        cmbMesInventario.setOnAction(e -> actualizarDiasInventario());

        // ── Toggle visibilidad de contraseñas ───────────────────────────
        sincronizarPasswordToggle(txtPassword, txtPasswordVisible);
        sincronizarPasswordToggle(txtPasswordConfirm, txtPasswordConfirmVisible);

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

        // Crear usuario administrador con su nombre real desde el inicio
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String adminNombre   = (tipoSeleccionado == TipoPersona.NATURAL)
                ? txtNombre.getText().trim()
                : txtRepLegalNombre.getText().trim();
        String adminApellido = (tipoSeleccionado == TipoPersona.NATURAL)
                ? txtApellido.getText().trim()
                : txtRepLegalApellido.getText().trim();
        var admin = usuarioService.crear(username, password, Rol.ADMIN,
                adminNombre.isEmpty() ? null : adminNombre,
                adminApellido.isEmpty() ? null : adminApellido);

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
                .mesInventarioAnual(obtenerMesInventario())
                .diaInventarioAnual(cmbDiaInventario.getValue())
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
            loginStage.setMinWidth(520);
            loginStage.setMinHeight(480);
            loginStage.centerOnScreen();
            loginStage.show();
        } catch (Exception e) {
            mostrarError("Error al abrir la pantalla de login: " + e.getMessage());
        }
    }

    // ── Filtros (TextFormatter factories) ───────────────────────────────

    /** Solo letras Unicode (tildes, ñ, etc.) y espacios. */
    private TextFormatter<String> filtroLetras(int maxLen) {
        return new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String newText = change.getControlNewText();
                if (newText.length() > maxLen || !newText.matches("[\\p{L} ]*")) return null;
            }
            return change;
        });
    }

    /** Solo dígitos, con longitud máxima. */
    private TextFormatter<String> filtroDigitos(int maxLen) {
        return new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String newText = change.getControlNewText();
                if (newText.length() > maxLen || !newText.matches("[0-9]*")) return null;
            }
            return change;
        });
    }

    /** NIT colombiano: dígitos + un guión opcional + dígito de verificación. */
    private TextFormatter<String> filtroNit(int maxLen) {
        return new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String newText = change.getControlNewText();
                if (newText.length() > maxLen || !newText.matches("[0-9\\-]*")) return null;
                // Máximo un guión
                if (newText.chars().filter(c -> c == '-').count() > 1) return null;
            }
            return change;
        });
    }

    /** Sin espacios (útil para correo). */
    private TextFormatter<String> filtroSinEspacios(int maxLen) {
        return new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String newText = change.getControlNewText();
                if (newText.length() > maxLen || newText.contains(" ")) return null;
            }
            return change;
        });
    }

    /** Prefijo de comprobante: alfanumérico + guión. */
    private TextFormatter<String> filtroPrefijo(int maxLen) {
        return new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String newText = change.getControlNewText();
                if (newText.length() > maxLen || !newText.matches("[A-Za-z0-9\\-]*")) return null;
            }
            return change;
        });
    }

    /** Username: minúsculas, dígitos, punto, guión bajo. */
    private TextFormatter<String> filtroUsername(int maxLen) {
        return new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String newText = change.getControlNewText();
                if (newText.length() > maxLen || !newText.matches("[a-z0-9._]*")) return null;
            }
            return change;
        });
    }

    /** Solo longitud máxima, cualquier carácter. */
    private TextFormatter<String> filtroLongitud(int maxLen) {
        return new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                if (change.getControlNewText().length() > maxLen) return null;
            }
            return change;
        });
    }

    /** Protege un Spinner editable contra texto no numérico. */
    private void protegerSpinner(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.getEditor().setTextFormatter(new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String newText = change.getControlNewText();
                if (!newText.matches("[0-9]*") || newText.length() > 6) return null;
            }
            return change;
        }));
        // Al perder foco, restaurar valor si quedó vacío o inválido
        spinner.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                try {
                    int val = Integer.parseInt(spinner.getEditor().getText());
                    spinner.getValueFactory().setValue(val);
                } catch (NumberFormatException e) {
                    spinner.getEditor().setText(String.valueOf(spinner.getValue()));
                }
            }
        });
    }

    // ── Inventario anual helpers ───────────────────────────────────────

    private static final String[] NOMBRES_MESES = {
        "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    /** Genera lista de días: null + 1..max */
    private java.util.List<Integer> generarDias(int max) {
        java.util.List<Integer> dias = new java.util.ArrayList<>();
        dias.add(null); // opción vacía
        for (int i = 1; i <= max; i++) dias.add(i);
        return dias;
    }

    /** Ajusta los días disponibles según el mes seleccionado. */
    private void actualizarDiasInventario() {
        String mes = cmbMesInventario.getValue();
        if (mes == null || mes.isEmpty()) {
            cmbDiaInventario.setItems(FXCollections.observableArrayList(generarDias(31)));
            cmbDiaInventario.setValue(null);
            return;
        }
        int maxDias = switch (mes) {
            case "Febrero" -> 29; // permitir 29 para años bisiestos
            case "Abril", "Junio", "Septiembre", "Noviembre" -> 30;
            default -> 31;
        };
        Integer diaActual = cmbDiaInventario.getValue();
        cmbDiaInventario.setItems(FXCollections.observableArrayList(generarDias(maxDias)));
        if (diaActual != null && diaActual <= maxDias) {
            cmbDiaInventario.setValue(diaActual);
        } else {
            cmbDiaInventario.setValue(null);
        }
    }

    /** Convierte el nombre del mes seleccionado a su número (1-12), o null. */
    private Integer obtenerMesInventario() {
        String mes = cmbMesInventario.getValue();
        if (mes == null || mes.isEmpty()) return null;
        for (int i = 1; i < NOMBRES_MESES.length; i++) {
            if (NOMBRES_MESES[i].equals(mes)) return i;
        }
        return null;
    }

    // ── Toggle visibilidad de contraseña ─────────────────────────────────

    /**
     * Sincroniza un PasswordField con un TextField "espejo" visible.
     * Ambos comparten el mismo texto; uno está oculto siempre.
     */
    private void sincronizarPasswordToggle(PasswordField hidden, TextField visible) {
        visible.setVisible(false);
        visible.setManaged(false);
        hidden.textProperty().bindBidirectional(visible.textProperty());
    }

    @FXML
    private void onTogglePassword() {
        toggleVisibilidad(txtPassword, txtPasswordVisible);
    }

    @FXML
    private void onTogglePasswordConfirm() {
        toggleVisibilidad(txtPasswordConfirm, txtPasswordConfirmVisible);
    }

    private void toggleVisibilidad(PasswordField hidden, TextField visible) {
        boolean mostrar = !visible.isVisible();
        hidden.setVisible(!mostrar);
        hidden.setManaged(!mostrar);
        visible.setVisible(mostrar);
        visible.setManaged(mostrar);
        // Dar foco al campo activo
        if (mostrar) visible.requestFocus();
        else hidden.requestFocus();
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
        Label[] indicadores = {indicador1, indicador2, indicador3, indicador4};
        Label[] titulos = {lblStep1Title, lblStep2Title, lblStep3Title, lblStep4Title};

        for (int i = 0; i < 4; i++) {
            if (i < paso) {
                // Completado
                indicadores[i].getStyleClass().setAll("indicador-completado");
                indicadores[i].setText("\u2713");
                titulos[i].getStyleClass().setAll("wiz-step-title-completed");
            } else if (i == paso) {
                // Activo
                indicadores[i].getStyleClass().setAll("indicador-activo");
                indicadores[i].setText(String.valueOf(i + 1));
                titulos[i].getStyleClass().setAll("wiz-step-title-active");
            } else {
                // Inactivo
                indicadores[i].getStyleClass().setAll("indicador-inactivo");
                indicadores[i].setText(String.valueOf(i + 1));
                titulos[i].getStyleClass().setAll("wiz-step-title");
            }
        }

        // Subtítulo "PASO ACTUAL" solo en el paso actual
        lblStep1Sub.setVisible(paso == 0);
        lblStep1Sub.setManaged(paso == 0);

        // Progreso
        double progreso = (paso + 1) * 0.25;
        pbProgreso.setProgress(progreso);
        lblPorcentaje.setText((int) (progreso * 100) + "%");
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
        // Validar formato de correo si se ingresó
        String correo = txtCorreo.getText().trim();
        if (!correo.isEmpty() && !correo.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            mostrarError("El correo electrónico no tiene un formato válido.");
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
