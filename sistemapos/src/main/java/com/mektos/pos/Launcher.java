package com.mektos.pos;

import com.mektos.pos.application.service.ConfiguracionService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Launcher extends Application {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        Application.launch(Launcher.class, args);
    }

    /**
     * Se ejecuta en un hilo de fondo antes de start().
     * Aquí se inicializa el contexto Spring (puede tardar unos segundos).
     */
    @Override
    public void init() throws Exception {
        context = SpringApplication.run(Launcher.class);
    }

    /**
     * Se ejecuta en el hilo de la aplicación JavaFX.
     * Decide qué ventana mostrar según el estado de configuración.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        ConfiguracionService configuracionService = context.getBean(ConfiguracionService.class);
        if (configuracionService.esPrimeraEjecucion()) {
            mostrarWizard(primaryStage);
        } else {
            String nombreTienda = configuracionService.obtener().getNombreTienda();
            mostrarVentanaPrincipal(primaryStage, nombreTienda);
        }
    }

    @Override
    public void stop() throws Exception {
        if (context != null) {
            context.close();
        }
    }

    private void mostrarWizard(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/setup_wizard.fxml"));
        // El controller es un bean Spring → se inyectan dependencias automáticamente
        loader.setControllerFactory(context::getBean);
        Scene scene = new Scene(loader.load(), 820, 580);
        stage.setTitle("NAP POS — Configuración inicial");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    private void mostrarVentanaPrincipal(Stage stage, String nombreTienda) throws Exception {
        // TODO: cargar main_window.fxml cuando esté implementado
        stage.setTitle(nombreTienda + " — NAP POS");
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.centerOnScreen();
        stage.show();
    }

    /** Permite que el controller del wizard acceda al contexto para abrir la ventana principal. */
    public static ConfigurableApplicationContext getContext() {
        return context;
    }
}
