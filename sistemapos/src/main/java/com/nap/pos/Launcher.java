package com.nap.pos;

import com.nap.pos.application.service.ConfiguracionService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        // Garantiza que el directorio de datos exista antes de que Spring
        // intente conectarse a la BD (SQLite no crea directorios padres).
        var napposDir = Paths.get(System.getProperty("user.home"), ".nappos");
        Files.createDirectories(napposDir);

        // Crea mail.properties si no existe — el operador/instalador debe
        // llenar spring.mail.username y spring.mail.password con las
        // credenciales reales. Spring Boot lo carga automáticamente al inicio.
        var mailConfig = napposDir.resolve("mail.properties");
        if (!Files.exists(mailConfig)) {
            Files.writeString(mailConfig,
                "# NAP POS — Configuracion SMTP para recuperacion de contrasena\n" +
                "# Complete con las credenciales de su proveedor de correo.\n" +
                "# Este archivo NUNCA debe compartirse ni subirse a repositorios.\n" +
                "#\n" +
                "spring.mail.username=\n" +
                "spring.mail.password=\n");
        }

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
        Scene scene = new Scene(loader.load(), 820, 660);
        stage.setTitle("NAP POS — Configuración inicial");
        stage.setScene(scene);
        stage.setMinWidth(700);
        stage.setMinHeight(560);
        stage.centerOnScreen();
        stage.show();
    }

    private void mostrarVentanaPrincipal(Stage stage, String nombreTienda) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml"));
        loader.setControllerFactory(context::getBean);
        Scene scene = new Scene(loader.load(), 900, 620);
        stage.setTitle(nombreTienda + " — NAP POS");
        stage.setScene(scene);
        stage.setMinWidth(520);
        stage.setMinHeight(480);
        stage.centerOnScreen();
        stage.show();
    }

    /** Permite que el controller del wizard acceda al contexto para abrir la ventana principal. */
    public static ConfigurableApplicationContext getContext() {
        return context;
    }
}
