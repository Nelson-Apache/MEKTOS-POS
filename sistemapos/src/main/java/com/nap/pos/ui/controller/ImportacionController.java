package com.nap.pos.ui.controller;

import com.nap.pos.application.dto.importacion.*;
import com.nap.pos.application.service.ImportacionService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

/**
 * Controlador del wizard de importación masiva (productos y clientes).
 * 5 pasos: selección → mapeo → validación → duplicados → resultado.
 */
@Component
@RequiredArgsConstructor
public class ImportacionController {

    private final ImportacionService importacionService;

    // ── Estado interno ──────────────────────────────────────────────────
    private TipoEntidad tipoEntidad = TipoEntidad.PRODUCTO;
    private File archivoSeleccionado;
    private List<String> encabezados;
    private Map<CampoImportacion, ComboBox<String>> comboBoxMapeo;
    private Map<CampoImportacion, Integer> mapeoActual;
    private ObservableList<DuplicadoViewModel> duplicadosObservable;
    private ObservableList<CategoriaSubcategoriaViewModel> categoriasObservable;
    private boolean mostroCategorias = false;
    private int pasoActual = 0;

    // ── Nodos FXML ──────────────────────────────────────────────────────
    @FXML private StackPane stackPanePasos;
    @FXML private VBox paso0, paso1, paso2, paso3Cat, paso3, paso4;
    @FXML private VBox cardProductos, cardClientes;
    @FXML private Label lblArchivo, lblTituloPaso;
    @FXML private Label ind1, ind2, ind3, ind4;
    @FXML private VBox contenedorMapeo;
    @FXML private ProgressIndicator progressValidacion;
    @FXML private VBox panelErrores;
    @FXML private Label lblContadorErrores;
    @FXML private TableView<FilaError> tablaErrores;
    @FXML private TableView<CategoriaSubcategoriaViewModel> tablaCategorias;
    @FXML private TableView<DuplicadoViewModel> tablaDuplicados;
    @FXML private Label lblImportados, lblActualizados, lblOmitidos;
    @FXML private Button btnAnterior, btnSiguiente, btnDescargarPlantilla, btnDetectarAuto;

    @FXML
    public void initialize() {
        configurarTablaErrores();
        configurarTablaCategorias();
        configurarTablaDuplicados();
        cardProductos.getStyleClass().add("tarjeta-seleccionada");
        actualizarUI();
    }

    // ── Eventos de selección (paso 0) ───────────────────────────────────

    @FXML
    private void onSeleccionarProductos() {
        tipoEntidad = TipoEntidad.PRODUCTO;
        cardProductos.getStyleClass().add("tarjeta-seleccionada");
        cardClientes.getStyleClass().remove("tarjeta-seleccionada");
    }

    @FXML
    private void onSeleccionarClientes() {
        tipoEntidad = TipoEntidad.CLIENTE;
        cardClientes.getStyleClass().add("tarjeta-seleccionada");
        cardProductos.getStyleClass().remove("tarjeta-seleccionada");
    }

    @FXML
    private void onSeleccionarArchivo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo de importación");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel y CSV", "*.xlsx", "*.xls", "*.csv"),
                new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("CSV", "*.csv")
        );
        File archivo = fc.showOpenDialog(stackPanePasos.getScene().getWindow());
        if (archivo != null) {
            archivoSeleccionado = archivo;
            lblArchivo.setText(archivo.getName());
        }
    }

    @FXML
    private void onDescargarPlantilla() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar plantilla");
        String nombre = tipoEntidad == TipoEntidad.PRODUCTO
                ? "plantilla_productos.xlsx" : "plantilla_clientes.xlsx";
        fc.setInitialFileName(nombre);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));

        File destino = fc.showSaveDialog(stackPanePasos.getScene().getWindow());
        if (destino == null) return;

        try {
            if (tipoEntidad == TipoEntidad.PRODUCTO) {
                importacionService.generarPlantillaProductos(destino);
            } else {
                importacionService.generarPlantillaClientes(destino);
            }
            mostrarInfo("Plantilla guardada en:\n" + destino.getAbsolutePath());
        } catch (Exception e) {
            mostrarError("No se pudo generar la plantilla: " + e.getMessage());
        }
    }

    // ── Eventos de navegación ───────────────────────────────────────────

    @FXML
    private void onSiguiente() {
        switch (pasoActual) {
            case 0 -> avanzarDesdePaso0();
            case 1 -> avanzarDesdePaso1();
            case 3 -> avanzarDesdePaso3Cat();   // categorías nuevas → duplicados o importar
            case 4 -> ejecutarImportacion();
            case 5 -> cerrar();
        }
    }

    @FXML
    private void onAnterior() {
        switch (pasoActual) {
            case 1 -> mostrarPaso(0);
            case 2 -> mostrarPaso(1);
            case 3 -> mostrarPaso(1);
            case 4 -> mostrarPaso(mostroCategorias ? 3 : 1);
        }
    }

    @FXML
    private void onDetectarAuto() {
        if (encabezados == null || comboBoxMapeo == null) return;
        Map<CampoImportacion, Integer> detectado =
                importacionService.autoDetectarMapeo(encabezados, tipoEntidad);

        for (Map.Entry<CampoImportacion, ComboBox<String>> entry : comboBoxMapeo.entrySet()) {
            Integer idx = detectado.get(entry.getKey());
            if (idx != null && idx < encabezados.size()) {
                entry.getValue().setValue(encabezados.get(idx));
            } else {
                entry.getValue().setValue("(No mapear)");
            }
        }
    }

    // ── Eventos de categorías nuevas (paso 3) ──────────────────────────

    @FXML
    private void onSeleccionarTodasCategorias() {
        if (categoriasObservable != null) {
            categoriasObservable.forEach(c -> c.setCrear(true));
            tablaCategorias.refresh();
        }
    }

    @FXML
    private void onDeseleccionarTodasCategorias() {
        if (categoriasObservable != null) {
            categoriasObservable.forEach(c -> c.setCrear(false));
            tablaCategorias.refresh();
        }
    }

    // ── Eventos de duplicados (paso 4) ──────────────────────────────────

    @FXML
    private void onMarcarTodosActualizar() {
        if (duplicadosObservable != null) {
            duplicadosObservable.forEach(d -> d.setAccion("Actualizar"));
            tablaDuplicados.refresh();
        }
    }

    @FXML
    private void onMarcarTodosOmitir() {
        if (duplicadosObservable != null) {
            duplicadosObservable.forEach(d -> d.setAccion("Omitir"));
            tablaDuplicados.refresh();
        }
    }

    // ── Lógica de navegación ────────────────────────────────────────────

    private void avanzarDesdePaso0() {
        if (archivoSeleccionado == null) {
            mostrarError("Seleccione un archivo antes de continuar.");
            return;
        }
        try {
            encabezados = importacionService.leerEncabezados(archivoSeleccionado);
        } catch (Exception e) {
            mostrarError("No se pudo leer el archivo: " + e.getMessage());
            return;
        }
        if (encabezados.isEmpty()) {
            mostrarError("El archivo está vacío o no tiene encabezados.");
            return;
        }
        construirUIMapeo();
        aplicarAutoDeteccion();
        mostrarPaso(1);
    }

    private void avanzarDesdePaso1() {
        mapeoActual = recogerMapeoDeComboBoxes();
        mostrarPaso(2);

        // Validar (sincrónico — los archivos son pequeños en desktop)
        List<FilaError> errores = tipoEntidad == TipoEntidad.PRODUCTO
                ? importacionService.validarProductos(archivoSeleccionado, mapeoActual)
                : importacionService.validarClientes(archivoSeleccionado, mapeoActual);

        progressValidacion.setVisible(false);
        progressValidacion.setManaged(false);

        if (!errores.isEmpty()) {
            mostrarErroresDeValidacion(errores);
            return;
        }

        // Sin errores → para productos, verificar si hay categorías/subcategorías nuevas
        if (tipoEntidad == TipoEntidad.PRODUCTO) {
            List<CategoriaSubcategoriaNueva> nuevas =
                    importacionService.detectarCategoriasNuevas(archivoSeleccionado, mapeoActual);
            if (!nuevas.isEmpty()) {
                categoriasObservable = FXCollections.observableArrayList(
                        nuevas.stream().map(CategoriaSubcategoriaViewModel::new).toList());
                tablaCategorias.setItems(categoriasObservable);
                mostroCategorias = true;
                mostrarPaso(3);
                return;
            }
        }

        mostroCategorias = false;
        irADuplicadosOImportar();
    }

    private void avanzarDesdePaso3Cat() {
        List<CategoriaSubcategoriaNueva> aCrear = categoriasObservable.stream()
                .filter(CategoriaSubcategoriaViewModel::isCrear)
                .map(CategoriaSubcategoriaViewModel::getDatos)
                .toList();
        if (!aCrear.isEmpty()) {
            try {
                importacionService.crearCategoriasYSubcategorias(aCrear);
            } catch (Exception e) {
                mostrarError("Error al crear categorías/subcategorías:\n" + e.getMessage());
                return;
            }
        }
        irADuplicadosOImportar();
    }

    private void irADuplicadosOImportar() {
        List<DuplicadoEncontrado> duplicados = tipoEntidad == TipoEntidad.PRODUCTO
                ? importacionService.detectarDuplicadosProductos(archivoSeleccionado, mapeoActual)
                : importacionService.detectarDuplicadosClientes(archivoSeleccionado, mapeoActual);

        if (duplicados.isEmpty()) {
            ejecutarImportacion();
        } else {
            duplicadosObservable = FXCollections.observableArrayList(
                    duplicados.stream().map(DuplicadoViewModel::new).toList());
            tablaDuplicados.setItems(duplicadosObservable);
            mostrarPaso(4);
        }
    }

    private void mostrarErroresDeValidacion(List<FilaError> errores) {
        lblContadorErrores.setText(errores.size() + " error(es) encontrado(s) — corrija el archivo e intente nuevamente");
        tablaErrores.setItems(FXCollections.observableArrayList(errores));
        panelErrores.setVisible(true);
        panelErrores.setManaged(true);
        // Dejar solo el botón ← Volver
        btnSiguiente.setVisible(false);
        btnSiguiente.setManaged(false);
        btnAnterior.setVisible(true);
        btnAnterior.setManaged(true);
        btnAnterior.setText("← Volver al mapeo");
    }

    private void ejecutarImportacion() {
        Set<Integer> filasAActualizar = new HashSet<>();
        if (duplicadosObservable != null) {
            for (DuplicadoViewModel vm : duplicadosObservable) {
                if ("Actualizar".equals(vm.getAccion())) {
                    filasAActualizar.add(vm.getNumeroFila());
                }
            }
        }
        try {
            ResultadoImportacion resultado = tipoEntidad == TipoEntidad.PRODUCTO
                    ? importacionService.importarProductos(archivoSeleccionado, mapeoActual, filasAActualizar)
                    : importacionService.importarClientes(archivoSeleccionado, mapeoActual, filasAActualizar);

            lblImportados.setText("✅  " + resultado.importados() + " registro(s) nuevo(s) importado(s)");
            lblActualizados.setText("🔄  " + resultado.actualizados() + " registro(s) actualizado(s)");
            lblOmitidos.setText("⏭  " + resultado.omitidos() + " registro(s) omitido(s)");
            mostrarPaso(5);

        } catch (Exception e) {
            mostrarError("Error durante la importación:\n" + e.getMessage());
        }
    }

    private void cerrar() {
        stackPanePasos.getScene().getWindow().hide();
    }

    // ── Construcción dinámica de la pantalla de mapeo ───────────────────

    private void construirUIMapeo() {
        contenedorMapeo.getChildren().clear();
        comboBoxMapeo = new LinkedHashMap<>();

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");
        grid.setPadding(new Insets(8, 0, 8, 0));

        ColumnConstraints colLabel = new ColumnConstraints(220);
        ColumnConstraints colCombo = new ColumnConstraints(300);
        colCombo.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(colLabel, colCombo);

        // Fila cabecera
        Label hCampo   = new Label("Campo del sistema");
        Label hColumna = new Label("Columna del archivo");
        hCampo.getStyleClass().add("form-label");
        hColumna.getStyleClass().add("form-label");
        grid.addRow(0, hCampo, hColumna);

        List<String> opciones = new ArrayList<>();
        opciones.add("(No mapear)");
        opciones.addAll(encabezados);

        int row = 1;
        for (CampoImportacion campo : CampoImportacion.values()) {
            if (campo.getTipo() != tipoEntidad) continue;

            String texto = campo.isRequerido()
                    ? "* " + campo.getLabel()
                    : campo.getLabel();
            Label lbl = new Label(texto);
            lbl.getStyleClass().add("form-label");
            if (campo.isRequerido()) {
                lbl.setStyle("-fx-text-fill: #1A365D; -fx-font-weight: bold;");
            }

            ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(opciones));
            combo.setValue("(No mapear)");
            combo.setMaxWidth(Double.MAX_VALUE);

            grid.addRow(row++, lbl, combo);
            comboBoxMapeo.put(campo, combo);
        }
        contenedorMapeo.getChildren().add(grid);
    }

    private void aplicarAutoDeteccion() {
        if (encabezados == null || comboBoxMapeo == null) return;
        Map<CampoImportacion, Integer> detectado =
                importacionService.autoDetectarMapeo(encabezados, tipoEntidad);

        for (Map.Entry<CampoImportacion, ComboBox<String>> entry : comboBoxMapeo.entrySet()) {
            Integer idx = detectado.get(entry.getKey());
            if (idx != null && idx < encabezados.size()) {
                entry.getValue().setValue(encabezados.get(idx));
            }
        }
    }

    private Map<CampoImportacion, Integer> recogerMapeoDeComboBoxes() {
        Map<CampoImportacion, Integer> mapeo = new LinkedHashMap<>();
        for (Map.Entry<CampoImportacion, ComboBox<String>> entry : comboBoxMapeo.entrySet()) {
            String val = entry.getValue().getValue();
            if (val != null && !val.equals("(No mapear)")) {
                int idx = encabezados.indexOf(val);
                if (idx >= 0) mapeo.put(entry.getKey(), idx);
            }
        }
        return mapeo;
    }

    // ── Gestión de pasos ────────────────────────────────────────────────

    private void mostrarPaso(int paso) {
        pasoActual = paso;
        if (paso == 2) {
            progressValidacion.setVisible(true);
            progressValidacion.setManaged(true);
            panelErrores.setVisible(false);
            panelErrores.setManaged(false);
        }
        actualizarUI();
    }

    private void actualizarUI() {
        // pasoActual: 0=selección, 1=mapeo, 2=validación, 3=categorías, 4=duplicados, 5=resultado
        VBox[] pasos = {paso0, paso1, paso2, paso3Cat, paso3, paso4};
        for (int i = 0; i < pasos.length; i++) {
            boolean activo = (i == pasoActual);
            pasos[i].setVisible(activo);
            pasos[i].setManaged(activo);
        }
        actualizarIndicadores();
        actualizarBotones();
    }

    private void actualizarIndicadores() {
        Label[] indicadores = {ind1, ind2, ind3, ind4};
        int activo = switch (pasoActual) {
            case 0       -> 0;
            case 1, 2   -> 1;
            case 3, 4   -> 2;
            default      -> 3;   // paso 5 (resultado)
        };
        for (int i = 0; i < indicadores.length; i++) {
            if (i <= activo) {
                indicadores[i].getStyleClass().remove("indicador-inactivo");
                if (!indicadores[i].getStyleClass().contains("indicador-activo"))
                    indicadores[i].getStyleClass().add("indicador-activo");
            } else {
                indicadores[i].getStyleClass().remove("indicador-activo");
                if (!indicadores[i].getStyleClass().contains("indicador-inactivo"))
                    indicadores[i].getStyleClass().add("indicador-inactivo");
            }
        }
    }

    private void actualizarBotones() {
        btnAnterior.setVisible(true);
        btnAnterior.setManaged(true);
        btnAnterior.setText("← Anterior");
        btnSiguiente.setVisible(true);
        btnSiguiente.setManaged(true);
        btnDescargarPlantilla.setVisible(false);
        btnDescargarPlantilla.setManaged(false);
        btnDetectarAuto.setVisible(false);
        btnDetectarAuto.setManaged(false);

        switch (pasoActual) {
            case 0 -> {
                btnAnterior.setVisible(false);
                btnAnterior.setManaged(false);
                btnDescargarPlantilla.setVisible(true);
                btnDescargarPlantilla.setManaged(true);
                btnSiguiente.setText("Siguiente →");
            }
            case 1 -> {
                btnDetectarAuto.setVisible(true);
                btnDetectarAuto.setManaged(true);
                btnSiguiente.setText("Validar →");
            }
            case 2 -> {
                btnAnterior.setVisible(false);
                btnAnterior.setManaged(false);
                btnSiguiente.setVisible(false);
                btnSiguiente.setManaged(false);
            }
            case 3 -> btnSiguiente.setText("Crear y continuar →");
            case 4 -> btnSiguiente.setText("Importar ✓");
            case 5 -> {
                btnAnterior.setVisible(false);
                btnAnterior.setManaged(false);
                btnSiguiente.setText("Cerrar");
            }
        }
    }

    // ── Configuración de tablas ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void configurarTablaCategorias() {
        TableColumn<CategoriaSubcategoriaViewModel, Boolean> colCrear = new TableColumn<>("Crear");
        colCrear.setCellValueFactory(d -> d.getValue().crearProperty().asObject());
        colCrear.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        getTableView().getItems().get(getIndex()).setCrear(cb.isSelected());
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setGraphic(null);
                } else {
                    cb.setSelected(getTableView().getItems().get(getIndex()).isCrear());
                    setGraphic(cb);
                }
            }
        });
        colCrear.setPrefWidth(60);

        TableColumn<CategoriaSubcategoriaViewModel, String> colCat = new TableColumn<>("Categoría");
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreCategoria()));
        colCat.setPrefWidth(260);

        TableColumn<CategoriaSubcategoriaViewModel, String> colSub = new TableColumn<>("Subcategoría");
        colSub.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreSubcategoria()));
        colSub.setPrefWidth(300);

        tablaCategorias.getColumns().addAll(colCrear, colCat, colSub);
        tablaCategorias.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @SuppressWarnings("unchecked")
    private void configurarTablaErrores() {
        TableColumn<FilaError, Integer> colFila = new TableColumn<>("Fila");
        colFila.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().numeroFila()).asObject());
        colFila.setPrefWidth(55);

        TableColumn<FilaError, String> colCampo = new TableColumn<>("Campo");
        colCampo.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().campo()));
        colCampo.setPrefWidth(180);

        TableColumn<FilaError, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().motivo()));
        colMotivo.setPrefWidth(450);

        tablaErrores.getColumns().addAll(colFila, colCampo, colMotivo);
        tablaErrores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @SuppressWarnings("unchecked")
    private void configurarTablaDuplicados() {
        TableColumn<DuplicadoViewModel, Integer> colFila = new TableColumn<>("Fila");
        colFila.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getNumeroFila()).asObject());
        colFila.setPrefWidth(50);

        TableColumn<DuplicadoViewModel, String> colId = new TableColumn<>("Identificador");
        colId.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getIdentificador()));
        colId.setPrefWidth(130);

        TableColumn<DuplicadoViewModel, String> colActual = new TableColumn<>("En sistema");
        colActual.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDatosActuales()));
        colActual.setPrefWidth(220);

        TableColumn<DuplicadoViewModel, String> colNuevo = new TableColumn<>("En archivo");
        colNuevo.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDatosNuevos()));
        colNuevo.setPrefWidth(220);

        TableColumn<DuplicadoViewModel, String> colAccion = new TableColumn<>("Acción");
        colAccion.setCellValueFactory(d -> d.getValue().accionProperty());
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> combo =
                    new ComboBox<>(FXCollections.observableArrayList("Actualizar", "Omitir"));
            {
                combo.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        getTableView().getItems().get(getIndex()).setAccion(combo.getValue());
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setGraphic(null);
                } else {
                    combo.setValue(getTableView().getItems().get(getIndex()).getAccion());
                    setGraphic(combo);
                }
            }
        });
        colAccion.setPrefWidth(120);

        tablaDuplicados.getColumns().addAll(colFila, colId, colActual, colNuevo, colAccion);
        tablaDuplicados.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    // ── Alerts ──────────────────────────────────────────────────────────

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // ── ViewModel para la tabla de categorías nuevas ────────────────────

    public static class CategoriaSubcategoriaViewModel {
        private final CategoriaSubcategoriaNueva datos;
        private final SimpleBooleanProperty crear = new SimpleBooleanProperty(true);

        public CategoriaSubcategoriaViewModel(CategoriaSubcategoriaNueva datos) { this.datos = datos; }

        public SimpleBooleanProperty crearProperty()  { return crear; }
        public boolean isCrear()                      { return crear.get(); }
        public void    setCrear(boolean v)            { crear.set(v); }
        public String  getNombreCategoria()           { return datos.nombreCategoria(); }
        public String  getNombreSubcategoria()        { return datos.nombreSubcategoria(); }
        public CategoriaSubcategoriaNueva getDatos()  { return datos; }
    }

    // ── ViewModel para la tabla de duplicados ───────────────────────────

    public static class DuplicadoViewModel {
        private final DuplicadoEncontrado datos;
        private final SimpleStringProperty accion = new SimpleStringProperty("Actualizar");

        public DuplicadoViewModel(DuplicadoEncontrado datos) { this.datos = datos; }

        public int    getNumeroFila()     { return datos.numeroFila(); }
        public String getIdentificador()  { return datos.identificador(); }
        public String getDatosActuales()  { return datos.datosActuales(); }
        public String getDatosNuevos()    { return datos.datosNuevos(); }
        public SimpleStringProperty accionProperty() { return accion; }
        public String getAccion()         { return accion.get(); }
        public void   setAccion(String a) { accion.set(a); }
    }
}
