package com.smartdine.controller;

import com.smartdine.coreheart.KOT;
import com.smartdine.coreheart.KOTItem;
import com.smartdine.coreheart.KOTStatus;
import com.smartdine.coreheart.MenuItem;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.repository.KOTRepository;
import com.smartdine.repository.MenuRepository;
import com.smartdine.repository.OrderRepository;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class UiKdsController implements Initializable {

    // --- FXML UI BOUND CONTROLS ---
    @FXML private Label activeTicketsLabel;
    @FXML private Label avgPrepTimeLabel;
    @FXML private Label readyItemsLabel;
    @FXML private Label delayedOrdersLabel;
    @FXML private Label completedTodayLabel;

    @FXML private ComboBox<String> stationFilter;
    @FXML private TextField searchField;

    @FXML private Button allTabBtn;
    @FXML private Label allCountBadge;
    @FXML private Button pendingTabBtn;
    @FXML private Label pendingCountBadge;
    @FXML private Button preparingTabBtn;
    @FXML private Label preparingCountBadge;
    @FXML private Button readyTabBtn;
    @FXML private Label readyCountBadge;
    @FXML private Button delayedTabBtn;
    @FXML private Label delayedCountBadge;

    @FXML private FlowPane kotCardsContainer;

    @FXML private Arc pulseArc;
    @FXML private Label pulsePercentageLabel;
    @FXML private Label pulseStatusLabel;

    @FXML private VBox cumulativeQueueVBox;
    @FXML private ListView<MenuItem> stockToggleListView;

    @Autowired
    private KOTRepository kotRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private com.smartdine.repository.SystemConfigRepository systemConfigRepository;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private UiDashboardController dashboardController;

    private Timeline clockTimeline;
    private String activeTab = "ALL"; // ALL, PENDING, PREPARING, READY, DELAYED

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize Station filters
        stationFilter.getItems().setAll("All Stations", "Hot Kitchen", "Pantry", "Beverages");
        stationFilter.setValue("All Stations");
        
        // Listeners for filters
        stationFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshKdsData());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshKdsData());

        // Setup custom cell factory for Today's Menu Stock controls
        stockToggleListView.setCellFactory(lv -> new ListCell<MenuItem>() {
            @Override
            protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding: 6 0;");

                    // Veg/Non-veg indicator badge
                    StackPane vegBox = new StackPane();
                    vegBox.setPrefSize(16, 16);
                    vegBox.setMinSize(16, 16);
                    vegBox.setMaxSize(16, 16);
                    vegBox.setStyle(item.isVeg() 
                        ? "-fx-border-color: #10B981; -fx-border-width: 1.5; -fx-border-radius: 2; -fx-padding: 3;" 
                        : "-fx-border-color: #EF4444; -fx-border-width: 1.5; -fx-border-radius: 2; -fx-padding: 3;");
                    
                    javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(3);
                    dot.setStyle(item.isVeg() ? "-fx-fill: #10B981;" : "-fx-fill: #EF4444;");
                    vegBox.getChildren().add(dot);

                    Label name = new Label(item.getName());
                    name.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 13px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button toggleBtn = new Button(item.isAvailable() ? "In Stock" : "Stock Out");
                    if (item.isAvailable()) {
                        toggleBtn.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 6; -fx-border-color: #15803D; -fx-border-radius: 6; -fx-cursor: hand;");
                    } else {
                        toggleBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 6; -fx-border-color: #EF4444; -fx-border-radius: 6; -fx-cursor: hand;");
                    }

                    toggleBtn.setOnAction(e -> {
                        boolean nextState = !item.isAvailable();
                        item.setAvailable(nextState);
                        MenuItem saved = menuRepository.saveAndFlush(item);
                        refreshStockList();
                        if (dashboardController != null) {
                            dashboardController.broadcastMenuUpdate(saved);
                            dashboardController.refreshMenuAndStockViews();
                        }
                    });

                    row.getChildren().addAll(vegBox, name, spacer, toggleBtn);
                    setGraphic(row);
                }
            }
        });

        // Initialize active badge class
        allCountBadge.getStyleClass().setAll("kds-badge-active");
        pendingCountBadge.getStyleClass().setAll("kds-badge-inactive");
        preparingCountBadge.getStyleClass().setAll("kds-badge-inactive");
        readyCountBadge.getStyleClass().setAll("kds-badge-inactive");
        delayedCountBadge.getStyleClass().setAll("kds-badge-delayed-inactive");

        // Start a native Timeline to keep KDS data fresh and update card ages
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> refreshKdsData()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();

        refreshKdsData();
    }

    // --- TAB CLICK ACTION HANDLER ---
    @FXML
    public void handleTabClick(javafx.event.ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        
        // Remove active class from all buttons
        allTabBtn.getStyleClass().remove("active");
        pendingTabBtn.getStyleClass().remove("active");
        preparingTabBtn.getStyleClass().remove("active");
        readyTabBtn.getStyleClass().remove("active");
        delayedTabBtn.getStyleClass().remove("active");

        // Add active class to clicked button
        clickedBtn.getStyleClass().add("active");

        // Swap styles of badges to match active tab
        allCountBadge.getStyleClass().setAll("kds-badge-inactive");
        pendingCountBadge.getStyleClass().setAll("kds-badge-inactive");
        preparingCountBadge.getStyleClass().setAll("kds-badge-inactive");
        readyCountBadge.getStyleClass().setAll("kds-badge-inactive");
        delayedCountBadge.getStyleClass().setAll("kds-badge-delayed-inactive");

        if (clickedBtn == allTabBtn) {
            activeTab = "ALL";
            allCountBadge.getStyleClass().setAll("kds-badge-active");
        } else if (clickedBtn == pendingTabBtn) {
            activeTab = "PENDING";
            pendingCountBadge.getStyleClass().setAll("kds-badge-active");
        } else if (clickedBtn == preparingTabBtn) {
            activeTab = "PREPARING";
            preparingCountBadge.getStyleClass().setAll("kds-badge-active");
        } else if (clickedBtn == readyTabBtn) {
            activeTab = "READY";
            readyCountBadge.getStyleClass().setAll("kds-badge-active");
        } else if (clickedBtn == delayedTabBtn) {
            activeTab = "DELAYED";
            delayedCountBadge.getStyleClass().setAll("kds-badge-delayed-active");
        }

        refreshKdsData();
    }

    // --- MAIN REFRESH DATA PIPELINE ---
    public void refreshKdsData() {
        Platform.runLater(() -> {
            UUID restaurantId = systemConfigRepository.findAll().stream()
                    .findFirst()
                    .map(com.smartdine.coreheart.SystemConfig::getRestaurantId)
                    .orElse(null);
            if (restaurantId == null) {
                restaurantId = TenantContext.getRestaurantId();
            }
            if (restaurantId == null) {
                restaurantId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
            }

            // 1. Fetch All Active KOTs (PENDING, PREPARING, and READY)
            List<KOT> allKots = kotRepository.findByRestaurantIdAndOverallStatusIn(
                    restaurantId, 
                    List.of(KOTStatus.PENDING, KOTStatus.PREPARING, KOTStatus.READY, KOTStatus.SERVED)
            );

            // Filter to only show today's tickets
            java.time.LocalDate today = java.time.LocalDate.now();
            allKots = allKots.stream()
                .filter(k -> k.getCreatedAt() != null && k.getCreatedAt().toLocalDate().isEqual(today))
                .collect(Collectors.toList());

            // Filter KOTs based on Station Filter ComboBox
            String selectedStation = stationFilter.getValue();
            if (selectedStation == null) {
                selectedStation = "All Stations";
            }
            final String finalStation = selectedStation;

            List<KOT> stationFilteredKots = allKots;
            if (!"All Stations".equals(finalStation)) {
                stationFilteredKots = allKots.stream()
                    .filter(k -> k.getItems().stream().anyMatch(item -> getStationForItem(item).equalsIgnoreCase(finalStation)))
                    .collect(Collectors.toList());
            }

            // 2. Calculate dynamic tab/badge counters (before search filters)
            long totalActiveCount = stationFilteredKots.stream().filter(k -> k.getOverallStatus() != KOTStatus.SERVED && k.getOverallStatus() != KOTStatus.CANCELLED).count();
            long pendingCount = stationFilteredKots.stream().filter(k -> k.getOverallStatus() == KOTStatus.PENDING).count();
            long prepCount = stationFilteredKots.stream().filter(k -> k.getOverallStatus() == KOTStatus.PREPARING).count();
            long readyCount = stationFilteredKots.stream().filter(k -> k.getOverallStatus() == KOTStatus.READY).count();
            long delayedCount = stationFilteredKots.stream()
                .filter(k -> k.getOverallStatus() != KOTStatus.READY && k.getOverallStatus() != KOTStatus.SERVED && k.getOverallStatus() != KOTStatus.CANCELLED 
                             && k.getCreatedAt() != null && ChronoUnit.MINUTES.between(k.getCreatedAt(), LocalDateTime.now()) > 10)
                .count();
            long completedCount = stationFilteredKots.stream().filter(k -> k.getOverallStatus() == KOTStatus.SERVED).count();

            allCountBadge.setText(String.valueOf(totalActiveCount));
            pendingCountBadge.setText(String.valueOf(pendingCount));
            preparingCountBadge.setText(String.valueOf(prepCount));
            readyCountBadge.setText(String.valueOf(readyCount));
            delayedCountBadge.setText(String.valueOf(delayedCount));

            // Update Header Stats
            activeTicketsLabel.setText(String.valueOf(pendingCount + prepCount));
            delayedOrdersLabel.setText(String.valueOf(delayedCount));
            readyItemsLabel.setText(String.valueOf(readyCount));
            completedTodayLabel.setText(String.valueOf(completedCount)); 

            // Compute Average Prep Time from READY and SERVED orders today
            List<KOT> preparedKots = stationFilteredKots.stream()
                .filter(k -> k.getOverallStatus() == KOTStatus.READY || k.getOverallStatus() == KOTStatus.SERVED)
                .collect(Collectors.toList());
            if (preparedKots.isEmpty()) {
                avgPrepTimeLabel.setText("0.0");
            } else {
                double avgMin = preparedKots.stream()
                    .filter(k -> k.getCreatedAt() != null && k.getUpdatedAt() != null)
                    .mapToLong(k -> {
                        long diff = ChronoUnit.SECONDS.between(k.getCreatedAt(), k.getUpdatedAt());
                        return diff > 0 ? diff : 0;
                    })
                    .average()
                    .orElse(0.0) / 60.0;
                avgPrepTimeLabel.setText(String.format(Locale.US, "%.1f", avgMin));
            }

            // Update Kitchen Pulse Gauge (Max Capacity = 8 active tickets)
            long currentPendingAndPrep = pendingCount + prepCount;
            int pulsePercent = Math.min((int) Math.round((currentPendingAndPrep / 8.0) * 100), 100);
            pulsePercentageLabel.setText(pulsePercent + "%");
            pulseArc.setLength(-1.8 * pulsePercent); // -1.8 scales 0-100% to a 0-180 degree Arc

            if (pulsePercent < 40) {
                pulseStatusLabel.setText("OPTIMAL LOAD");
                pulseStatusLabel.setStyle("-fx-text-fill: #10B981;");
                pulseArc.setStyle("-fx-fill: transparent; -fx-stroke: #10B981; -fx-stroke-width: 10; -fx-stroke-line-cap: round;");
            } else if (pulsePercent <= 78) {
                pulseStatusLabel.setText("MEDIUM LOAD");
                pulseStatusLabel.setStyle("-fx-text-fill: #F59E0B;");
                pulseArc.setStyle("-fx-fill: transparent; -fx-stroke: #F59E0B; -fx-stroke-width: 10; -fx-stroke-line-cap: round;");
            } else {
                pulseStatusLabel.setText("CRITICAL LOAD");
                pulseStatusLabel.setStyle("-fx-text-fill: #EF4444;");
                pulseArc.setStyle("-fx-fill: transparent; -fx-stroke: #EF4444; -fx-stroke-width: 10; -fx-stroke-line-cap: round;");
            }

            // 3. Build Cumulative Dish Prep Queue
            Map<String, Integer> cumulativeCounts = new HashMap<>();
            for (KOT kot : stationFilteredKots) {
                if (kot.getOverallStatus() == KOTStatus.PENDING || kot.getOverallStatus() == KOTStatus.PREPARING) {
                    for (KOTItem item : kot.getItems()) {
                        if (item.getItemStatus() != KOTStatus.READY && item.getItemStatus() != KOTStatus.SERVED && item.getItemStatus() != KOTStatus.CANCELLED) {
                            String name = item.getItemName();
                            cumulativeCounts.put(name, cumulativeCounts.getOrDefault(name, 0) + item.getQuantity());
                        }
                    }
                }
            }

            List<Map.Entry<String, Integer>> sortedCumulative = cumulativeCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

            cumulativeQueueVBox.getChildren().clear();
            if (sortedCumulative.isEmpty()) {
                Label emptyLabel = new Label("No items in queue");
                emptyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-padding: 10;");
                cumulativeQueueVBox.getChildren().add(emptyLabel);
            } else {
                for (Map.Entry<String, Integer> entry : sortedCumulative) {
                    HBox row = new HBox();
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px; -fx-padding: 8 12;");
                    
                    Label dishName = new Label(entry.getKey());
                    dishName.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 13px;");
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    Label qtyBadge = new Label("x" + entry.getValue());
                    qtyBadge.setStyle("-fx-background-color: #0F172A; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 12px; -fx-padding: 2 8;");
                    
                    row.getChildren().addAll(dishName, spacer, qtyBadge);
                    cumulativeQueueVBox.getChildren().add(row);
                }
            }

            // 4. Render Active KOT Cards in the Grid
            String searchQuery = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

            List<KOT> gridKots = stationFilteredKots.stream()
                .filter(k -> k.getOverallStatus() != KOTStatus.SERVED && k.getOverallStatus() != KOTStatus.CANCELLED)
                .filter(k -> {
                    // Tab status filter
                    if ("PENDING".equals(activeTab)) return k.getOverallStatus() == KOTStatus.PENDING;
                    if ("PREPARING".equals(activeTab)) return k.getOverallStatus() == KOTStatus.PREPARING;
                    if ("READY".equals(activeTab)) return k.getOverallStatus() == KOTStatus.READY;
                    if ("DELAYED".equals(activeTab)) {
                        return k.getOverallStatus() != KOTStatus.READY && k.getCreatedAt() != null && ChronoUnit.MINUTES.between(k.getCreatedAt(), LocalDateTime.now()) > 10;
                    }
                    return true; // "ALL"
                })
                .filter(k -> {
                    // Search string filter
                    if (searchQuery.isEmpty()) return true;
                    String table = k.getTableName() != null ? k.getTableName().toLowerCase() : "";
                    String num = k.getKotNumber() != null ? k.getKotNumber().toLowerCase() : "";
                    boolean matchItems = k.getItems().stream().anyMatch(item -> item.getItemName().toLowerCase().contains(searchQuery));
                    return table.contains(searchQuery) || num.contains(searchQuery) || matchItems;
                })
                .sorted((k1, k2) -> {
                    int w1 = k1.getOverallStatus() == KOTStatus.PENDING ? 0 : (k1.getOverallStatus() == KOTStatus.PREPARING ? 1 : 2);
                    int w2 = k2.getOverallStatus() == KOTStatus.PENDING ? 0 : (k2.getOverallStatus() == KOTStatus.PREPARING ? 1 : 2);
                    if (w1 != w2) {
                        return Integer.compare(w1, w2);
                    }
                    LocalDateTime t1 = k1.getCreatedAt();
                    LocalDateTime t2 = k2.getCreatedAt();
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return t2.compareTo(t1);
                })
                .collect(Collectors.toList());

            kotCardsContainer.getChildren().clear();
            if (gridKots.isEmpty()) {
                VBox emptyBox = new VBox(8);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPrefSize(400, 200);
                emptyBox.setStyle("-fx-border-color: #E2E8F0; -fx-border-style: dashed; -fx-border-width: 1.5px; -fx-border-radius: 12; -fx-background-color: white; -fx-background-radius: 12; -fx-padding: 30;");
                
                SVGPath clipboardOff = new SVGPath();
                clipboardOff.setContent("M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2");
                clipboardOff.setFill(javafx.scene.paint.Color.TRANSPARENT);
                clipboardOff.setStroke(javafx.scene.paint.Color.web("#94A3B8"));
                clipboardOff.setStrokeWidth(2.0);
                clipboardOff.setScaleX(1.4);
                clipboardOff.setScaleY(1.4);

                Label noTicketsLabel = new Label("No Kitchen Tickets Found");
                noTicketsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-padding: 10 0 0 0;");
                
                Label subtitleLabel = new Label("Change the active filters or check search query.");
                subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

                emptyBox.getChildren().addAll(clipboardOff, noTicketsLabel, subtitleLabel);
                kotCardsContainer.getChildren().add(emptyBox);
            } else {
                for (KOT kot : gridKots) {
                    kotCardsContainer.getChildren().add(buildKotCard(kot, finalStation));
                }
            }

            // 5. Load Today's Menu Stock controls
            refreshStockList();
        });
    }

    private VBox buildKotCard(KOT kot, String selectedStation) {
        VBox card = new VBox(12);
        card.setPrefSize(250, 290);
        
        long elapsedMinutes = 0;
        long elapsedSec = 0;
        if (kot.getCreatedAt() != null) {
            boolean isCompleted = kot.getOverallStatus() == KOTStatus.READY || kot.getOverallStatus() == KOTStatus.SERVED;
            LocalDateTime endPoint = (isCompleted && kot.getUpdatedAt() != null) ? kot.getUpdatedAt() : LocalDateTime.now();
            elapsedSec = ChronoUnit.SECONDS.between(kot.getCreatedAt(), endPoint);
            if (elapsedSec < 0) elapsedSec = 0;
            elapsedMinutes = elapsedSec / 60;
        }

        // Color-code card border based on age & status
        boolean isDelayed = elapsedMinutes > 10 && kot.getOverallStatus() != KOTStatus.READY;
        String topColor = isDelayed ? "#EF4444" : (kot.getOverallStatus() == KOTStatus.READY ? "#10B981" : (kot.getOverallStatus() == KOTStatus.PREPARING ? "#2563EB" : "#FB923C"));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: " + topColor + " #E2E8F0 #E2E8F0 #E2E8F0; -fx-border-width: 4 1 1 1; -fx-padding: 14; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 5, 0, 0, 2);");

        // Card Header: Table/Source & Age Timer
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        String sourceText = kot.getTableName() != null && !kot.getTableName().isEmpty() ? kot.getTableName() : "Dine-In";
        try {
            if (kot.getOrderId() != null) {
                com.smartdine.coreheart.Order order = orderRepository.findById(kot.getOrderId()).orElse(null);
                if (order != null && order.getTableName() != null && order.getTableName().startsWith("MERGE ")) {
                    sourceText = order.getTableName();
                }
            }
        } catch (Exception ignored) {}
        Label sourceLabel = new Label(sourceText);
        sourceLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #0F172A;");
        
        // Timer display formatted nicely
        long minutes = elapsedSec / 60;
        long seconds = elapsedSec % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        Label timer = new Label(timeStr);
        timer.setStyle("-fx-font-family: 'Segoe UI', monospace; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-padding: 2 6; -fx-border-radius: 4; -fx-background-radius: 4;");
        if (isDelayed) {
            timer.setStyle(timer.getStyle() + " -fx-text-fill: #EF4444; -fx-background-color: #FEE2E2; -fx-border-color: #FCA5A5;");
        } else {
            timer.setStyle(timer.getStyle() + " -fx-text-fill: #10B981;");
        }

        header.getChildren().addAll(sourceLabel, new Region(), timer);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        // KOT ID Subtitle & Status Badge Row
        HBox subHeader = new HBox();
        subHeader.setAlignment(Pos.CENTER_LEFT);
        subHeader.setStyle("-fx-padding: 0 0 4 0; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-border-style: dashed;");

        Label kotNum = new Label("KOT: " + kot.getKotNumber());
        kotNum.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(kot.getOverallStatus().toString());
        if (isDelayed) {
            statusBadge.setText("DELAYED");
            statusBadge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-font-size: 9px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 4;");
        } else if (kot.getOverallStatus() == KOTStatus.READY) {
            statusBadge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-size: 9px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 4;");
        } else if (kot.getOverallStatus() == KOTStatus.PREPARING) {
            statusBadge.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-font-size: 9px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 4;");
        } else {
            statusBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #EA580C; -fx-font-size: 9px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 4;");
        }

        subHeader.getChildren().addAll(kotNum, spacer, statusBadge);

        // Card Body: Items List (filter based on selected station)
        VBox itemsBox = new VBox(8);
        List<KOTItem> itemsToShow = kot.getItems();
        if (!"All Stations".equals(selectedStation)) {
            itemsToShow = itemsToShow.stream()
                .filter(item -> getStationForItem(item).equalsIgnoreCase(selectedStation))
                .collect(Collectors.toList());
        }

        for (KOTItem item : itemsToShow) {
            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            CheckBox cb = new CheckBox();
            cb.setSelected(item.getItemStatus() == KOTStatus.READY || item.getItemStatus() == KOTStatus.SERVED || item.getItemStatus() == KOTStatus.CANCELLED);
            cb.setDisable(item.getItemStatus() == KOTStatus.CANCELLED || item.getItemStatus() == KOTStatus.SERVED);
            if (item.getItemStatus() == KOTStatus.CANCELLED || item.getItemStatus() == KOTStatus.SERVED) {
                cb.setStyle("-fx-opacity: 0.55; -fx-cursor: default;");
            } else {
                cb.setStyle("-fx-cursor: hand;");
            }

            Text itemText = new Text(item.getQuantity() + "x  " + item.getItemName());
            if (item.getItemStatus() == KOTStatus.CANCELLED) {
                itemText.setStrikethrough(true);
                itemText.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-fill: #94A3B8; -fx-opacity: 0.6;");
                
                Text removedText = new Text(" [Removed]");
                removedText.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-fill: #EF4444;");
                
                itemRow.getChildren().addAll(cb, itemText, removedText);
            } else {
                itemText.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-fill: #1E293B;");
                String statusLabel = "";
                String statusColor = "#10B981";
                if (item.getItemStatus() == KOTStatus.SERVED) {
                    itemText.setStrikethrough(true);
                    itemText.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-fill: #0F766E;");
                    statusLabel = " [Served]";
                    statusColor = "#0F766E";
                } else if (item.getItemStatus() == KOTStatus.READY) {
                    itemText.setStrikethrough(true);
                    itemText.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-fill: #10B981;");
                    statusLabel = " [Ready]";
                    statusColor = "#10B981";
                } else {
                    statusLabel = " [Added]";
                    statusColor = "#2563EB";
                }
                
                Text stateText = new Text(statusLabel);
                stateText.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-fill: " + statusColor + ";");
                
                itemRow.getChildren().addAll(cb, itemText, stateText);
            }

            cb.setOnAction(event -> {
                boolean cooked = cb.isSelected();
                item.setItemStatus(cooked ? KOTStatus.READY : KOTStatus.PREPARING);

                boolean allCooked = kot.getItems().stream()
                    .allMatch(ki -> ki.getItemStatus() == KOTStatus.READY || ki.getItemStatus() == KOTStatus.SERVED || ki.getItemStatus() == KOTStatus.CANCELLED);
                
                if (allCooked) {
                    kot.setOverallStatus(KOTStatus.READY);
                } else {
                    kot.setOverallStatus(KOTStatus.PREPARING);
                }

                kotRepository.save(kot);
                refreshKdsData();
            });

            if (item.getSpecialInstruction() != null && !item.getSpecialInstruction().trim().isEmpty()) {
                VBox itemCol = new VBox(2);
                itemCol.getChildren().add(itemRow);
                Label notesLabel = new Label("  ↳ " + item.getSpecialInstruction());
                notesLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #EF4444; -fx-font-weight: bold;");
                itemCol.getChildren().add(notesLabel);
                itemsBox.getChildren().add(itemCol);
            } else {
                itemsBox.getChildren().add(itemRow);
            }
        }

        ScrollPane scrollPane = new ScrollPane(itemsBox);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Card Footer: Action Button
        Button actionBtn = new Button();
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setDisable(kot.getOverallStatus() == KOTStatus.SERVED || kot.getOverallStatus() == KOTStatus.CANCELLED);
        
        if (kot.getOverallStatus() == KOTStatus.PENDING) {
            actionBtn.setText("START COOKING");
            if (actionBtn.isDisable()) {
                actionBtn.setStyle("-fx-background-color: #FB923C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: default; -fx-font-size: 12px; -fx-opacity: 0.55;");
            } else {
                actionBtn.setStyle("-fx-background-color: #FB923C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand; -fx-font-size: 12px;");
            }
        } else if (kot.getOverallStatus() == KOTStatus.PREPARING) {
            actionBtn.setText("MARK READY");
            if (actionBtn.isDisable()) {
                actionBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: default; -fx-font-size: 12px; -fx-opacity: 0.55;");
            } else {
                actionBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand; -fx-font-size: 12px;");
            }
        } else {
            actionBtn.setText("SERVE ORDER");
            if (actionBtn.isDisable()) {
                actionBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: default; -fx-font-size: 12px; -fx-opacity: 0.55;");
            } else {
                actionBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand; -fx-font-size: 12px;");
            }
        }
        
        actionBtn.setOnAction(event -> {
            if (kot.getOverallStatus() == KOTStatus.PENDING) {
                kot.setOverallStatus(KOTStatus.PREPARING);
            } else if (kot.getOverallStatus() == KOTStatus.PREPARING) {
                kot.setOverallStatus(KOTStatus.READY);
                kot.getItems().forEach(ki -> {
                    if (ki.getItemStatus() != KOTStatus.READY && ki.getItemStatus() != KOTStatus.SERVED && ki.getItemStatus() != KOTStatus.CANCELLED) {
                        ki.setItemStatus(KOTStatus.READY);
                    }
                });
            } else if (kot.getOverallStatus() == KOTStatus.READY) {
                kot.setOverallStatus(KOTStatus.SERVED);
                kot.getItems().forEach(ki -> {
                    if (ki.getItemStatus() != KOTStatus.CANCELLED) {
                        ki.setItemStatus(KOTStatus.SERVED);
                    }
                });
            }
            kotRepository.save(kot);
            refreshKdsData();
        });

        card.getChildren().addAll(header, subHeader, scrollPane, actionBtn);
        return card;
    }

    private void refreshStockList() {
        UUID restaurantId = TenantContext.getRestaurantId();
        if (restaurantId == null) {
            restaurantId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        }
        List<MenuItem> todaysMenu = menuRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
            .stream()
            .filter(MenuItem::isTodaysMenu)
            .sorted(Comparator.comparing(MenuItem::getName))
            .collect(Collectors.toList());
        
        // Prevent clearing selection or reset during periodic updates
        List<MenuItem> currentList = new ArrayList<>(stockToggleListView.getItems());
        if (!currentList.equals(todaysMenu)) {
            stockToggleListView.getItems().setAll(todaysMenu);
        }
    }

    private String getStationForItem(KOTItem item) {
        String station = "Hot Kitchen";
        try {
            MenuItem mi = null;
            if (item.getMenuItemId() != null) {
                mi = menuRepository.findById(item.getMenuItemId()).orElse(null);
            }
            if (mi != null && mi.getCategoryName() != null) {
                String cat = mi.getCategoryName().toLowerCase();
                if (cat.contains("beverage") || cat.contains("drink") || cat.contains("juice") || cat.contains("soda") || cat.contains("shake")) {
                    station = "Beverages";
                } else if (cat.contains("dessert") || cat.contains("side") || cat.contains("salad") || cat.contains("sauce") || cat.contains("soup")) {
                    station = "Pantry";
                }
            }
        } catch (Exception e) {}
        return station;
    }

    public void stopTimeline() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
    }
}
