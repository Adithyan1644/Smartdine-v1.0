package com.smartdine.controller;

import com.smartdine.service.ActivationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class UiActivationController {

    @Autowired
    private ActivationService activationService;

    @Autowired
    private ApplicationContext springContext;

    @FXML
    private StackPane rootPane;

    @FXML
    private VBox step1Container;

    @FXML
    private VBox step2Container;

    @FXML
    private TextField syncCodeField;

    @FXML
    private TextField gatewayUrlField;

    @FXML
    private VBox loadingContainer;

    @FXML
    private Label statusLabel;

    @FXML
    private Label errorLabel1;

    @FXML
    private Button activateBtn;

    // Step 2 Fields
    @FXML
    private TextField adminUserField;

    @FXML
    private PasswordField adminPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField adminPinField;

    @FXML
    private Label errorLabel2;

    @FXML
    private Button completeBtn;

    @FXML
    public void initialize() {
        // Set default gateway URL to the live Express API Server for strong sync integration
        if (gatewayUrlField != null) {
            gatewayUrlField.setText("http://localhost:8080/api/public/provision");
        }

        // Apply numeric filter & max length limit (4 digits) for POS PIN
        if (adminPinField != null) {
            adminPinField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    adminPinField.setText(newValue.replaceAll("[^\\d]", ""));
                }
                if (adminPinField.getText().length() > 4) {
                    adminPinField.setText(adminPinField.getText().substring(0, 4));
                }
            });
        }
    }

    @FXML
    public void handleActivation(ActionEvent event) {
        String code = syncCodeField.getText();
        String gatewayUrl = gatewayUrlField.getText();

        if (code == null || code.trim().isEmpty()) {
            showError(errorLabel1, "Sync code cannot be empty. Enter the generated sync code from the website.");
            return;
        }

        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            gatewayUrl = "http://localhost:8080/api/public/provision";
        }

        final String finalCode = code.trim();
        final String finalGateway = gatewayUrl.trim();

        // Show spinner, hide buttons/errors
        loadingContainer.setVisible(true);
        loadingContainer.setManaged(true);
        errorLabel1.setVisible(false);
        errorLabel1.setManaged(false);
        activateBtn.setDisable(true);

        // Run network/DB operation asynchronously in a virtual thread to prevent UI freezing
        Thread.ofVirtual().start(() -> {
            try {
                Platform.runLater(() -> statusLabel.setText("Connecting to Cloud Gateway..."));
                Thread.sleep(800); // Aesthetic pause to let the user see progress

                Platform.runLater(() -> statusLabel.setText("Downloading table map & menu configuration..."));
                activationService.activateSystem(finalCode, finalGateway);
                Thread.sleep(800);

                Platform.runLater(() -> {
                    loadingContainer.setVisible(false);
                    loadingContainer.setManaged(false);
                    step1Container.setVisible(false);
                    step1Container.setManaged(false);
                    step2Container.setVisible(true);
                    step2Container.setManaged(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingContainer.setVisible(false);
                    loadingContainer.setManaged(false);
                    activateBtn.setDisable(false);
                    showError(errorLabel1, e.getMessage());
                });
            }
        });
    }

    @FXML
    public void handleCompleteSetup(ActionEvent event) {
        String username = adminUserField.getText();
        String password = adminPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String pin = adminPinField.getText();

        if (username == null || username.trim().isEmpty()) {
            showError(errorLabel2, "Admin username cannot be empty.");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            showError(errorLabel2, "Password cannot be empty.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError(errorLabel2, "Passwords do not match.");
            return;
        }
        if (pin == null || pin.length() != 4) {
            showError(errorLabel2, "POS PIN must be exactly 4 digits.");
            return;
        }

        completeBtn.setDisable(true);
        errorLabel2.setVisible(false);
        errorLabel2.setManaged(false);

        Thread.ofVirtual().start(() -> {
            try {
                // Save manager account & fire up JmDNS
                activationService.setupManagerAccount(username, password, pin);
                Thread.sleep(600); // Aesthetic pause

                Platform.runLater(() -> {
                    try {
                        // Switch scene to main POS dashboard
                        Stage stage = (Stage) rootPane.getScene().getWindow();
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/dashboard.fxml"));
                        loader.setControllerFactory(springContext::getBean);

                        Parent root = loader.load();
                        Scene scene = new Scene(root);
                        stage.setScene(scene);
                        stage.setTitle("SMARTDINE BILLER STATION");
                        stage.show();
                    } catch (Exception e) {
                        completeBtn.setDisable(false);
                        showError(errorLabel2, "Failed to load dashboard: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    completeBtn.setDisable(false);
                    showError(errorLabel2, "Error setting credentials: " + e.getMessage());
                });
            }
        });
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }
}
