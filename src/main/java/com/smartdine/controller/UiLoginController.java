package com.smartdine.controller;

import com.smartdine.dto.AuthResponse;
import com.smartdine.dto.LoginRequest;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.service.AuthService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class UiLoginController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ApplicationContext springContext;

    @FXML
    private StackPane rootPane;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginBtn;

    @FXML
    public void initialize() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty()) {
            showError("Username cannot be empty.");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            showError("Password cannot be empty.");
            return;
        }

        loginBtn.setDisable(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Run authentication check asynchronously on a virtual thread
        Thread.ofVirtual().start(() -> {
            try {
                LoginRequest loginReq = new LoginRequest();
                loginReq.setUsername(username.trim());
                loginReq.setPassword(password);

                AuthResponse response = authService.authenticateUser(loginReq);

                Platform.runLater(() -> {
                    try {
                        // Bind local tenant context
                        TenantContext.setRestaurantId(response.getRestaurantId());

                        // Transition stage to POS dashboard
                        Stage stage = (Stage) rootPane.getScene().getWindow();
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/dashboard.fxml"));
                        loader.setControllerFactory(springContext::getBean);

                        Parent root = loader.load();
                        Scene scene = new Scene(root);
                        stage.setScene(scene);
                        stage.setTitle("SMARTDINE BILLER STATION");
                        stage.show();
                    } catch (Exception e) {
                        loginBtn.setDisable(false);
                        showError("Failed to launch dashboard: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    // Extract clean message if present
                    String msg = e.getMessage() != null ? e.getMessage() : "Invalid credentials";
                    showError(msg);
                });
            }
        });
    }

    @FXML
    public void handleGoToSetup(ActionEvent event) {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/activation.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("SMARTDINE Setup Wizard");
            stage.show();
        } catch (Exception e) {
            showError("Failed to open setup wizard: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
