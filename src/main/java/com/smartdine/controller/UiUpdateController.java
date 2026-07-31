package com.smartdine.controller;

import com.smartdine.coreheart.Restaurant;
import com.smartdine.coreheart.SystemConfig;
import com.smartdine.repository.RestaurantRepository;
import com.smartdine.repository.SystemConfigRepository;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

/**
 * Spring-managed JavaFX controller for the Software Update Prompt dialog.
 *
 * Environment Routing Logic:
 *   - Resolves isTest by reading the local system_config → joining to restaurants.
 *   - isTest = true  → downloads from the DEV GCS bucket (/dev/ channel).
 *   - isTest = false → downloads from the PROD GCS bucket (/prod/ channel).
 *
 * Update Sequence (handleUpdate):
 *   1. Hide button box, show progress bar.
 *   2. Spawn a virtual-thread Task<Void> to stream-download the JAR.
 *   3. On success → invoke update.bat → Platform.exit() → System.exit(0).
 */
@Component
public class UiUpdateController {

    // ── GCS update channel endpoints ──────────────────────────────────────────
    private static final String GCS_DEV_URL  =
            "https://storage.googleapis.com/smartdine-saas-updates/dev/smartdine-heart.jar";
    private static final String GCS_PROD_URL =
            "https://storage.googleapis.com/smartdine-saas-updates/prod/smartdine-heart.jar";

    private static final String TEMP_JAR_PATH = "C:\\SmartDine\\temp\\smartdine-heart.jar";
    private static final String UPDATE_BAT    = "C:\\SmartDine\\update.bat";

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private Label       descriptionLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label       statusLabel;
    @FXML private HBox        buttonBox;
    @FXML private Button      laterBtn;
    @FXML private Button      updateBtn;

    // ── Spring dependencies ───────────────────────────────────────────────────
    private final SystemConfigRepository systemConfigRepository;
    private final RestaurantRepository   restaurantRepository;

    private Stage dialogStage;

    public UiUpdateController(SystemConfigRepository systemConfigRepository,
                               RestaurantRepository restaurantRepository) {
        this.systemConfigRepository = systemConfigRepository;
        this.restaurantRepository   = restaurantRepository;
    }

    /** Called by the launcher to inject the dialog's Stage for close-on-later. */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    // ── FXML Handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleLater() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleUpdate() {
        // 1. Transition UI to download mode
        buttonBox.setVisible(false);
        progressBar.setVisible(true);
        statusLabel.setVisible(true);
        descriptionLabel.setText(
            "Downloading software updates. Please do not close the application or turn off your PC..."
        );

        // 2. Ensure the temp directory exists before starting the stream
        File tempDir = new File("C:\\SmartDine\\temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // 3. Resolve isTest via system_config → restaurant join (Answer to Q1)
        boolean isTestSystem = resolveIsTestFlag();

        final String targetUrl = isTestSystem ? GCS_DEV_URL : GCS_PROD_URL;
        System.out.println("[UiUpdateController] Downloading from "
                + (isTestSystem ? "DEV" : "PROD") + " channel: " + targetUrl);

        // 4. Background download task with byte-level progress reporting
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Connecting to update server...");
                URL url = new URI(targetUrl).toURL();
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(30_000);
                long fileSize = connection.getContentLengthLong();

                try (InputStream in              = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream fileOutputStream = new FileOutputStream(TEMP_JAR_PATH)) {

                    byte[] buffer       = new byte[8192];
                    int    bytesRead;
                    long   totalBytes   = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                        if (fileSize > 0) {
                            updateProgress(totalBytes, fileSize);
                            long pct = (totalBytes * 100) / fileSize;
                            updateMessage("Downloading... " + pct + "%  ("
                                    + (totalBytes / 1024) + " KB / "
                                    + (fileSize   / 1024) + " KB)");
                        }
                    }
                    fileOutputStream.flush();
                }
                updateMessage("Download complete! Preparing file swap...");
                return null;
            }
        };

        // Bind progress bar to task progress
        progressBar.progressProperty().bind(downloadTask.progressProperty());
        statusLabel.textProperty().bind(downloadTask.messageProperty());

        downloadTask.setOnSucceeded(event -> {
            statusLabel.textProperty().unbind();
            statusLabel.setText("Launching update agent — the application will restart automatically.");
            executeUpgradeAgent();
        });

        downloadTask.setOnFailed(event -> {
            // Unbind before changing text
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();

            Throwable ex = downloadTask.getException();
            System.err.println("[UiUpdateController] Download failed: " + ex.getMessage());

            Platform.runLater(() -> {
                progressBar.setVisible(false);
                statusLabel.setText("Download failed: " + ex.getMessage()
                        + " — Please check your internet connection and try again.");
                buttonBox.setVisible(true);
            });
        });

        // Use a virtual thread for the download (Java 21)
        Thread.ofVirtual().name("smartdine-updater").start(downloadTask);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Resolves the isTest flag by reading the local SystemConfig record
     * and joining to the corresponding Restaurant entity.
     * Defaults to false (PROD channel) if any step fails.
     */
    private boolean resolveIsTestFlag() {
        try {
            SystemConfig config = systemConfigRepository.findAll()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No SystemConfig record found."));

            UUID restaurantId = config.getRestaurantId();
            Restaurant restaurant = restaurantRepository.findByRestaurantId(restaurantId)
                    .orElseThrow(() -> new IllegalStateException(
                            "No Restaurant found for ID: " + restaurantId));

            boolean isTest = restaurant.isTest();
            System.out.println("[UiUpdateController] isTest resolved = " + isTest
                    + " for restaurant: " + restaurant.getName());
            return isTest;

        } catch (Exception e) {
            System.err.println("[UiUpdateController] WARNING: Could not resolve isTest flag. "
                    + "Defaulting to PROD release channel. Reason: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes the Windows service swapper batch script in a detached process,
     * then cleanly terminates the JVM.
     */
    private void executeUpgradeAgent() {
        try {
            String command = "powershell -Command \"Start-Process cmd -ArgumentList '/c', 'C:\\SmartDine\\update.bat' -Verb RunAs\"";
            Runtime.getRuntime().exec(command);
            // Small pause to let cmd.exe start before JVM exit
            Thread.sleep(500);
        } catch (Exception e) {
            System.err.println("[UiUpdateController] Failed to launch update.bat: " + e.getMessage());
        } finally {
            Platform.exit();
            System.exit(0);
        }
    }
}
