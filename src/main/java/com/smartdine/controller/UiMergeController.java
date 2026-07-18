package com.smartdine.controller;

import com.smartdine.coreheart.DiningTable;
import com.smartdine.coreheart.Order;
import com.smartdine.coreheart.TableStatus;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.repository.TableRepository;
import com.smartdine.service.OrderService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class UiMergeController {

    @FXML
    private VBox tablesContainer;

    @FXML
    private TextField notesField;

    @FXML
    private Button cancelBtn;

    @FXML
    private Button mergeBtn;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private OrderService orderService;

    private UiDashboardController dashboardController;
    private final List<CheckBox> checkBoxes = new ArrayList<>();

    public void setDashboardController(UiDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    public void initialize() {
        checkBoxes.clear();
        tablesContainer.getChildren().clear();

        UUID restaurantId = TenantContext.getRestaurantId();
        List<DiningTable> availableTables = tableRepository.findByRestaurantId(restaurantId);

        // Sort tables logically by number (e.g. T-01, T-02...)
        availableTables.sort((t1, t2) -> {
            try {
                String n1 = t1.getTableNumber().replaceAll("[^0-9]", "");
                String n2 = t2.getTableNumber().replaceAll("[^0-9]", "");
                return Integer.compare(Integer.parseInt(n1), Integer.parseInt(n2));
            } catch (Exception e) {
                return t1.getTableNumber().compareTo(t2.getTableNumber());
            }
        });

        for (DiningTable table : availableTables) {
            if (table.getStatus() == TableStatus.AVAILABLE) {
                // Premium Card Container
                HBox card = new HBox(12);
                card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 14; -fx-alignment: center-left; -fx-cursor: hand;");

                // CheckBox
                CheckBox cb = new CheckBox();
                cb.setUserData(table);
                cb.setStyle("-fx-cursor: hand;");
                checkBoxes.add(cb);

                // Table Title Label
                Label nameLabel = new Label("🪑 " + table.getTableNumber());
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-family: 'Inter', 'Segoe UI';");

                // Area / Section Label
                String areaStr = table.getAreaName() != null ? table.getAreaName() : "Indoor";
                Label areaLabel = new Label(areaStr);
                areaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-family: 'Inter', 'Segoe UI'; -fx-background-color: #F1F5F9; -fx-padding: 2 6; -fx-background-radius: 4px;");

                // Spacer
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                card.getChildren().addAll(cb, nameLabel, spacer, areaLabel);

                // Dynamic style switching based on selection
                cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        card.setStyle("-fx-background-color: #F0FDF4; -fx-border-color: #0A4F34; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 14; -fx-alignment: center-left; -fx-cursor: hand;");
                    } else {
                        card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 14; -fx-alignment: center-left; -fx-cursor: hand;");
                    }
                });

                // Toggle checkbox state on card click
                card.setOnMouseClicked(e -> {
                    if (e.getTarget() != cb) {
                        cb.setSelected(!cb.isSelected());
                    }
                });

                tablesContainer.getChildren().add(card);
            }
        }

        if (checkBoxes.isEmpty()) {
            VBox emptyContainer = new VBox(8);
            emptyContainer.setAlignment(Pos.CENTER);
            emptyContainer.setStyle("-fx-padding: 20;");
            
            Label noTablesLabel = new Label("No Vacant Tables Available");
            noTablesLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-font-family: 'Inter', 'Segoe UI';");
            
            Label subLabel = new Label("All tables are currently occupied or reserved.");
            subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-font-family: 'Inter', 'Segoe UI';");
            
            emptyContainer.getChildren().addAll(noTablesLabel, subLabel);
            tablesContainer.getChildren().add(emptyContainer);
            
            mergeBtn.setDisable(true);
            mergeBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: 700; -fx-padding: 10 24; -fx-background-radius: 6px;");
        } else {
            mergeBtn.setDisable(false);
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeStage(event);
    }

    @FXML
    void handleMerge(ActionEvent event) {
        List<UUID> selectedTableIds = new ArrayList<>();
        List<DiningTable> selectedTables = new ArrayList<>();

        for (CheckBox cb : checkBoxes) {
            if (cb.isSelected()) {
                DiningTable table = (DiningTable) cb.getUserData();
                selectedTableIds.add(table.getId());
                selectedTables.add(table);
            }
        }

        if (selectedTableIds.size() < 2) {
            if (dashboardController != null) {
                dashboardController.showAlert("Invalid Selection", "Please select at least two tables to merge.");
            }
            return;
        }

        try {
            String notes = notesField.getText() != null ? notesField.getText().trim() : "";
            Order mergedOrder = orderService.createMergedOrder(selectedTableIds, notes);

            if (dashboardController != null) {
                // Set the current dining table to the primary anchor
                DiningTable primaryTable = selectedTables.get(0);
                dashboardController.setCurrentDiningTable(primaryTable);
                dashboardController.setCurrentActiveOrder(mergedOrder);

                // Reload all tables and lists in the UI
                dashboardController.loadTablesToUi();
                dashboardController.loadRunningOrders();
                dashboardController.loadOrdersToUi();

                // Open the billing view with this active merge order loaded
                dashboardController.openOrderInBilling(mergedOrder);
            }

            closeStage(event);
        } catch (Exception e) {
            e.printStackTrace();
            if (dashboardController != null) {
                dashboardController.showAlert("Merge Failed", "Could not merge tables: " + e.getMessage());
            }
        }
    }

    private void closeStage(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
