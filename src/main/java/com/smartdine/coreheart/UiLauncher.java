package com.smartdine.coreheart;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class UiLauncher extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        // Auto-kill any stale process on port 8080 before Spring Boot tries to bind.
        // This makes the app self-healing: no more "Port already in use" on re-launch.
        PortKiller.freePort();

        // Ensure the JVM exits when the JavaFX window is closed, so that
        // port 8080 is always released and is never left occupied on re-launch.
        Platform.setImplicitExit(true);

        // Register a JVM shutdown hook so the Spring context (and Tomcat) is
        // always closed gracefully even if the process is killed externally.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (springContext != null && springContext.isActive()) {
                springContext.close();
            }
        }, "spring-shutdown-hook"));

        // Boot up Spring Boot in the background with the correct classloader for scanning
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(CoreHeartApplication.class.getClassLoader());
            springContext = new SpringApplicationBuilder(CoreHeartApplication.class).run();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Retrieve the ActivationService bean to check current activation state
        com.smartdine.service.ActivationService activationService = springContext.getBean(com.smartdine.service.ActivationService.class);
        boolean activated = activationService.isSystemActivated();

        if (activated) {
            java.util.Optional<com.smartdine.coreheart.SystemConfig> configOpt = activationService.getSystemConfig();
            configOpt.ifPresent(config -> {
                if (config.getRestaurantId() != null) {
                    com.smartdine.coreheart.TenantContext.setRestaurantId(config.getRestaurantId());
                }
            });
        }

        String fxmlPath = activated ? "/ui/login.fxml" : "/ui/activation.fxml";
        String title = activated ? "SmartDine Login" : "SmartDine Setup Wizard";

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        // This line is the magic: it allows Spring to @Autowire repositories into JavaFX Controllers!
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());

        // Gracefully shut down Spring when the primary window's X button is clicked
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.setTitle(title);
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMaximized(true);
        Platform.runLater(() -> {
            primaryStage.setMaximized(true);
            try {
                com.smartdine.service.VersionCheckService versionService =
                        springContext.getBean(com.smartdine.service.VersionCheckService.class);
                versionService.checkForUpdatesAsync(primaryStage);
            } catch (Exception e) {
                System.out.println("[UiLauncher] Version check trigger note: " + e.getMessage());
            }
        });
    }

    @Override
    public void stop() throws Exception {
        // Called by JavaFX runtime on clean exits — close Spring context and release port 8080
        if (springContext != null && springContext.isActive()) {
            springContext.close();
        }
    }
}