package com.smartdine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdine.controller.UiUpdateController;
import com.smartdine.coreheart.Restaurant;
import com.smartdine.coreheart.SystemConfig;
import com.smartdine.repository.RestaurantRepository;
import com.smartdine.repository.SystemConfigRepository;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for background OTA version checking.
 *
 * Current App Version: 1.0.0
 * Checks GCS release channel version.json:
 *   DEV  Channel → https://storage.googleapis.com/smartdine-saas-updates/dev/version.json
 *   PROD Channel → https://storage.googleapis.com/smartdine-saas-updates/prod/version.json
 *
 * If remote version > CURRENT_VERSION (e.g. 1.0.1 > 1.0.0),
 * opens the update_prompt.fxml dialog on the JavaFX UI thread.
 */
@Service
public class VersionCheckService {

    public static final String CURRENT_VERSION = "1.0.4";

    public static String getLocalVersion() {
        try {
            java.io.File vFile = new java.io.File("C:/SmartDine/version.json");
            if (vFile.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                Map<?, ?> map = mapper.readValue(vFile, Map.class);
                if (map.get("version") != null) {
                    return map.get("version").toString().trim();
                }
            }
        } catch (Exception ignored) {}
        try {
            java.io.File vFileLocal = new java.io.File("version.json");
            if (vFileLocal.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                Map<?, ?> map = mapper.readValue(vFileLocal, Map.class);
                if (map.get("version") != null) {
                    return map.get("version").toString().trim();
                }
            }
        } catch (Exception ignored) {}
        return CURRENT_VERSION;
    }

    private static final String DEV_VERSION_URL =
            "https://storage.googleapis.com/smartdine-saas-updates/dev/version.json";
    private static final String PROD_VERSION_URL =
            "https://storage.googleapis.com/smartdine-saas-updates/prod/version.json";

    private final SystemConfigRepository systemConfigRepository;
    private final RestaurantRepository   restaurantRepository;
    private final ApplicationContext       applicationContext;
    private final ObjectMapper             objectMapper = new ObjectMapper();

    public VersionCheckService(SystemConfigRepository systemConfigRepository,
                               RestaurantRepository restaurantRepository,
                               ApplicationContext applicationContext) {
        this.systemConfigRepository = systemConfigRepository;
        this.restaurantRepository   = restaurantRepository;
        this.applicationContext     = applicationContext;
    }

    /**
     * Initiates asynchronous OTA version check on a Java 21 virtual thread.
     * Non-blocking — UI launches immediately without delay.
     */
    public void checkForUpdatesAsync(Stage ownerStage) {
        Thread.ofVirtual().name("version-checker").start(() -> {
            try {
                // Short sleep to allow main UI window to render first
                Thread.sleep(2500);

                boolean isTest = resolveIsTestFlag();
                String targetUrl = isTest ? DEV_VERSION_URL : PROD_VERSION_URL;
                System.out.println("[VersionCheckService] Checking update channel: "
                        + (isTest ? "DEV" : "PROD") + " → " + targetUrl);

                URL url = new URI(targetUrl).toURL();
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                try (InputStream in = conn.getInputStream()) {
                    Map<?, ?> remoteData = objectMapper.readValue(in, Map.class);
                    String remoteVersion = remoteData.get("version") != null
                            ? remoteData.get("version").toString().trim()
                            : "1.0.0";

                    String currentLocalVersion = getLocalVersion();

                    System.out.println("[VersionCheckService] Local Version: " + currentLocalVersion
                            + " | Remote Version: " + remoteVersion);

                    if (isNewerVersion(remoteVersion, currentLocalVersion)) {
                        System.out.println("[VersionCheckService] 🚀 New update found ("
                                + remoteVersion + ")! Opening update dialog...");
                        Platform.runLater(() -> showUpdateDialog(ownerStage));
                    } else {
                        System.out.println("[VersionCheckService] ✅ Software is up to date (v"
                                + currentLocalVersion + ").");
                    }
                }
            } catch (Exception e) {
                System.out.println("[VersionCheckService] Version check info: "
                        + e.getMessage() + " (Software continues on current version).");
            }
        });
    }

    /** Helper to compare version strings like "1.0.1" vs "1.0.0" */
    private boolean isNewerVersion(String remote, String current) {
        if (remote == null || current == null) return false;
        String[] rParts = remote.split("\\.");
        String[] cParts = current.split("\\.");
        int length = Math.max(rParts.length, cParts.length);

        for (int i = 0; i < length; i++) {
            int rVal = i < rParts.length ? Integer.parseInt(rParts[i].replaceAll("[^0-9]", "")) : 0;
            int cVal = i < cParts.length ? Integer.parseInt(cParts[i].replaceAll("[^0-9]", "")) : 0;
            if (rVal > cVal) return true;
            if (rVal < cVal) return false;
        }
        return false;
    }

    /** Resolves isTest flag from local system_config → restaurant join */
    private boolean resolveIsTestFlag() {
        try {
            SystemConfig config = systemConfigRepository.findAll().stream().findFirst().orElse(null);
            if (config == null) return false;
            UUID restaurantId = config.getRestaurantId();
            Restaurant restaurant = restaurantRepository.findByRestaurantId(restaurantId).orElse(null);
            return restaurant != null && restaurant.isTest();
        } catch (Exception e) {
            return false;
        }
    }

    /** Displays the update_prompt.fxml dialog on JavaFX UI thread */
    private void showUpdateDialog(Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/update_prompt.fxml"));
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();
            UiUpdateController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (ownerStage != null) {
                dialogStage.initOwner(ownerStage);
            }
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle("SmartDine Software Update");

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            if (controller != null) {
                controller.setDialogStage(dialogStage);
            }

            dialogStage.show();
        } catch (Exception e) {
            System.err.println("[VersionCheckService] Failed to open update dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
