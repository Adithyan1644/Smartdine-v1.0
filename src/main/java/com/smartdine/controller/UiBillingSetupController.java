package com.smartdine.controller;

import com.smartdine.coreheart.BillingConfig;
import com.smartdine.repository.BillingConfigRepository;
import com.smartdine.service.ReceiptPrintService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UiBillingSetupController {

    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private TextField gstinField;
    @FXML private TextField footerField;
    @FXML private ComboBox<String> paperSizeBox;
    @FXML private CheckBox showTaxCheck;
    @FXML private TextArea previewArea;
    @FXML private Button saveBtn;
    @FXML private Button resetBtn;

    private final BillingConfigRepository billingConfigRepository;
    private final ReceiptPrintService receiptPrintService;

    public UiBillingSetupController(BillingConfigRepository billingConfigRepository, ReceiptPrintService receiptPrintService) {
        this.billingConfigRepository = billingConfigRepository;
        this.receiptPrintService = receiptPrintService;
    }

    @FXML
    public void initialize() {
        if (paperSizeBox != null && paperSizeBox.getItems().isEmpty()) {
            paperSizeBox.getItems().addAll("80mm", "58mm");
        }

        // Attach listeners to trigger instant live preview generation on any configuration modification
        if (nameField != null) nameField.textProperty().addListener((obs, old, val) -> generateLivePreview());
        if (addressField != null) addressField.textProperty().addListener((obs, old, val) -> generateLivePreview());
        if (gstinField != null) gstinField.textProperty().addListener((obs, old, val) -> generateLivePreview());
        if (footerField != null) footerField.textProperty().addListener((obs, old, val) -> generateLivePreview());
        if (paperSizeBox != null) paperSizeBox.valueProperty().addListener((obs, old, val) -> generateLivePreview());
        if (showTaxCheck != null) showTaxCheck.selectedProperty().addListener((obs, old, val) -> generateLivePreview());

        loadCurrentConfig();
    }

    private void loadCurrentConfig() {
        BillingConfig config = billingConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(BillingConfig::new);

        if (nameField != null) nameField.setText(config.getRestaurantName() != null ? config.getRestaurantName() : "");
        if (addressField != null) addressField.setText(config.getAddressLine1() != null ? config.getAddressLine1() : "");
        if (gstinField != null) gstinField.setText(config.getGstin() != null ? config.getGstin() : "");
        if (footerField != null) footerField.setText(config.getFooterMessage() != null ? config.getFooterMessage() : "");
        if (paperSizeBox != null) paperSizeBox.setValue(config.getPaperSize() != null ? config.getPaperSize() : "80mm");
        if (showTaxCheck != null) showTaxCheck.setSelected(config.isShowTaxDetails());

        generateLivePreview();
    }

    private void generateLivePreview() {
        BillingConfig mockConfig = new BillingConfig();
        mockConfig.setRestaurantName(nameField != null && nameField.getText() != null && !nameField.getText().isEmpty() ? nameField.getText() : "Surabhi Foods");
        mockConfig.setAddressLine1(addressField != null && addressField.getText() != null ? addressField.getText() : "");
        mockConfig.setGstin(gstinField != null && gstinField.getText() != null ? gstinField.getText() : "");
        mockConfig.setFooterMessage(footerField != null && footerField.getText() != null ? footerField.getText() : "");
        mockConfig.setPaperSize(paperSizeBox != null && paperSizeBox.getValue() != null ? paperSizeBox.getValue() : "80mm");
        mockConfig.setShowTaxDetails(showTaxCheck != null && showTaxCheck.isSelected());

        try {
            billingConfigRepository.save(mockConfig);
        } catch (Exception e) {
            System.err.println("Warning: Live preview database caching bypassed: " + e.getMessage());
        }

        Map<String, Object> orderDetails = new HashMap<>();
        orderDetails.put("billNo", "4821");
        orderDetails.put("type", "TABLE");
        orderDetails.put("tableName", "T-12");
        orderDetails.put("waiterName", "John");

        List<Map<String, Object>> mockItems = new ArrayList<>();

        Map<String, Object> item1 = new HashMap<>();
        item1.put("name", "Surabhi Forest");
        item1.put("qty", 2);
        item1.put("rate", 250);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("name", "Jeera Rice");
        item2.put("qty", 1);
        item2.put("rate", 120);

        mockItems.add(item1);
        mockItems.add(item2);

        String previewText = receiptPrintService.generateReceiptText(orderDetails, mockItems);
        if (previewArea != null) {
            previewArea.setText(previewText);
        }
    }

    @FXML
    private void handleSave() {
        BillingConfig config = billingConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(BillingConfig::new);

        if (nameField != null) config.setRestaurantName(nameField.getText());
        if (addressField != null) config.setAddressLine1(addressField.getText());
        if (gstinField != null) config.setGstin(gstinField.getText());
        if (footerField != null) config.setFooterMessage(footerField.getText());
        if (paperSizeBox != null && paperSizeBox.getValue() != null) config.setPaperSize(paperSizeBox.getValue());
        if (showTaxCheck != null) config.setShowTaxDetails(showTaxCheck.isSelected());

        if (config.getRestaurantId() == null) {
            config.setRestaurantId(UUID.fromString("9183522f-e62b-4cdc-b852-cac4b347cbc8"));
        }

        billingConfigRepository.save(config);
        generateLivePreview();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("Billing layout configuration successfully saved!");
        alert.showAndWait();
    }

    @FXML
    private void handleReset() {
        loadCurrentConfig();
    }
}
