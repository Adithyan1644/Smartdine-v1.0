package com.smartdine.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import javafx.event.ActionEvent;
import javafx.scene.shape.SVGPath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import netscape.javascript.JSObject;

import com.smartdine.coreheart.DiningTable;
import com.smartdine.coreheart.TableStatus;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.coreheart.Order;
import com.smartdine.coreheart.OrderStatus;
import com.smartdine.coreheart.OrderType;
import com.smartdine.coreheart.KOT;
import com.smartdine.coreheart.KOTItem;
import com.smartdine.coreheart.KOTStatus;
import com.smartdine.coreheart.MenuItem;
import com.smartdine.coreheart.Category;
import com.smartdine.coreheart.Customer;
import com.smartdine.repository.TableRepository;
import com.smartdine.repository.OrderRepository;
import com.smartdine.repository.KOTRepository;
import com.smartdine.repository.MenuRepository;
import com.smartdine.repository.CategoryRepository;
import com.smartdine.repository.CustomerRepository;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

@Component
public class UiDashboardController implements Initializable {

    // --- DASHBOARD BINDINGS ---
    @FXML
    private FlowPane tablesContainer;

    @FXML
    private VBox runningOrdersContainer;

    @FXML
    private VBox platformOrdersContainer;

    @FXML
    private VBox stockOutContainer;

    @FXML
    private Label pulseLabel;

    @FXML
    private Label pulseStatusLabel;

    @FXML
    private javafx.scene.shape.Arc pulseGaugeArc;

    @FXML
    private Label stLoadA;

    @FXML
    private Label stLoadB;

    @FXML
    private Label stLoadC;

    @FXML
    private Label dateTimeLabel;

    @FXML
    private Label todaysOrdersLabel;

    @FXML
    private Label activeOrdersLabel;

    @FXML
    private Label pendingOrdersLabel;

    // --- VIEW TOGGLE BINDINGS ---
    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox dashboardView;

    @FXML
    private VBox billingView;

    @FXML
    private Button homeBtn;

    @FXML
    private Button billingBtn;

    // --- POS BILLING BINDINGS ---
    @FXML
    private Button tablesBtn;

    @FXML
    private VBox tablesView;

    // --- ORDERS VIEW BINDINGS ---
    @FXML
    private Button ordersBtn;
    @FXML
    private VBox ordersView;
    @FXML
    private Label ordersTotalLabel;
    @FXML
    private Label ordersPreparingLabel;
    @FXML
    private Label ordersReadyLabel;
    @FXML
    private Label ordersDelayedLabel;
    @FXML
    private Label ordersCancelledLabel;
    @FXML
    private TextField ordersSearchField;
    @FXML
    private ComboBox<String> ordersStatusComboBox;
    @FXML
    private Button ordersTabAllBtn;
    @FXML
    private Button ordersTabDineInBtn;
    @FXML
    private Button ordersTabDeliveryBtn;
    @FXML
    private Button ordersTabTakeawayBtn;
    @FXML
    private Label ordersTabAllBadge;
    @FXML
    private Label ordersTabDineInBadge;
    @FXML
    private Label ordersTabDeliveryBadge;
    @FXML
    private Label ordersTabTakeawayBadge;
    @FXML
    private VBox ordersListContainer;

    // Details Pane
    @FXML
    private VBox orderDetailsPane;
    @FXML
    private Label detOrderNumberLabel;
    @FXML
    private Label detOrderMetaLabel;
    @FXML
    private Label detStatusBadgeLabel;
    @FXML
    private Label detCustomerNameLabel;
    @FXML
    private Label detTableWaiterLabel;
    @FXML
    private VBox detItemsContainer;
    @FXML
    private VBox detKitchenNotesContainer;
    @FXML
    private Label detKitchenNotesLabel;
    @FXML
    private Label detSubtotalLabel;
    @FXML
    private Label detCgstLabel;
    @FXML
    private Label detSgstLabel;
    @FXML
    private Label detGrandTotalLabel;
    @FXML
    private Button detPrintKotBtn;
    @FXML
    private Button detEditItemsBtn;
    @FXML
    private Button detActionButton;
    @FXML
    private HBox detEditSearchBox;
    @FXML
    private TextField detEditSearchField;
    @FXML
    private HBox detEditActionsContainer;
    @FXML
    private Button detCancelEditBtn;
    @FXML
    private Button detSaveChangesBtn;

    @FXML
    private VBox tablesPageAreasContainer;

    @FXML
    private Button tblFilterAllBtn;

    @FXML
    private Button tblFilterAvailableBtn;

    @FXML
    private Button tblFilterRunningBtn;

    @FXML
    private Button tblFilterBillingBtn;

    @FXML
    private Button tblFilterReservedBtn;

    @FXML
    private VBox top8ItemsContainer;

    @FXML
    private VBox frequentItemsContainer;

    @FXML
    private FlowPane modifiersContainer;

    @FXML
    private TextField menuSearchField;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private javafx.scene.layout.GridPane billingMenuGrid;

    @FXML
    private VBox cartItemsContainer;

    @FXML
    private Label cartSubtotalLabel;

    @FXML
    private Button addDiscountBtn;

    @FXML
    private HBox discountRow;

    @FXML
    private Label discountTitleLabel;

    @FXML
    private Label discountValueLabel;

    @FXML
    private Label cartCgstLabel;

    @FXML
    private Label cartSgstLabel;

    @FXML
    private Label cartGrandTotalLabel;

    @FXML
    private TextField receivedAmountField;

    @FXML
    private Label changeAmountLabel;

    @FXML
    private CheckBox chkIsPaid;

    @FXML
    private CheckBox chkSendReceipt;

    @FXML
    private Label billingDateTimeLabel;

    @FXML
    private Label cartDateLabel;

    @FXML
    private Label cartHeaderLabel;

    @FXML
    private VBox dineInMetaBox;

    @FXML
    private Button dineInTableChip;

    @FXML
    private Button dineInCustomerBtn;

    @FXML
    private Label dineInOrderNumLabel;

    @FXML
    private VBox deliveryMetaBox;

    @FXML
    private TextField deliveryPhoneField;

    @FXML
    private TextField deliveryNameField;

    @FXML
    private TextArea deliveryAddressField;

    @FXML
    private VBox pickupMetaBox;

    @FXML
    private TextField pickupPhoneField;

    @FXML
    private TextField pickupNameField;

    @FXML
    private Button tabDineInBtn;

    @FXML
    private Button tabDeliveryBtn;

    @FXML
    private Button tabPickupBtn;

    @FXML
    private Button payCashBtn;

    @FXML
    private Button payCardBtn;

    @FXML
    private Button payUpiBtn;

    @FXML
    private Button payOtherBtn;

    @FXML
    private Button paySplitBtn;

    @FXML
    private Button cancelOrderBtn;

    @FXML
    private Button saveOrderBtn;

    @FXML
    private Button saveAndPrintBtn;

    @FXML
    private Button settleBtn;

    @FXML
    private Button addNoteBtn;

    // --- KDS VIEW BINDINGS ---
    @FXML
    private VBox kdsView;

    @FXML
    private Button kdsBtn;

    @FXML
    private javafx.scene.Parent kdsNative;

    @FXML
    private UiKdsController kdsNativeController;

    // --- MENU VIEW BINDINGS ---
    @FXML
    private VBox menuView;

    @FXML
    private Button menuBtn;

    @FXML
    private javafx.scene.web.WebView menuWebView;

    // --- SPRING AUTOWIRED REPOSITORIES ---
    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private KOTRepository kotRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private com.smartdine.service.OrderService orderService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private com.smartdine.service.MenuService menuService;

    @Autowired
    private com.smartdine.repository.SystemConfigRepository systemConfigRepository;

    @Autowired
    private com.smartdine.service.ActivationService activationService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    // Keep a strong reference to prevent garbage collection of WebView bridge
    private final JavaMenuBridge javaMenuBridge = new JavaMenuBridge();

    public UUID getActiveRestaurantId() {
        return systemConfigRepository.findAll().stream()
                .findFirst()
                .map(com.smartdine.coreheart.SystemConfig::getRestaurantId)
                .orElse(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
    }

    // --- STATE VARIABLES ---
    private Timeline autoRefreshTimeline;
    private final java.util.concurrent.atomic.AtomicBoolean isRefreshing = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean isBillingRefreshing = new java.util.concurrent.atomic.AtomicBoolean(false);

    // POS Cart structures
    private List<CartItem> cartList = new ArrayList<>();
    private String selectedPaymentMode = null;
    private OrderType selectedOrderType = OrderType.DINE_IN;
    private List<String> activeModifiers = new ArrayList<>(); // Modifiers applied to the next added item
    private CartItem selectedCartItem = null;
    private double discountValue = 0.0;
    private boolean isDiscountPercentage = false; // true if %, false if fixed amount
    private boolean shouldScrollToBottom = false;
    private String activeTableFilter = "All";

    // Track active session states
    private DiningTable currentDiningTable = null;
    private Order currentActiveOrder = null;

    // Customer CRM State
    private String currentCustomerPhone = "";
    private String currentCustomerName = "";
    private String currentCustomerNotes = "";

    // Orders page state
    private Order selectedOrder = null;
    private String ordersSearchQuery = "";
    private String ordersStatusFilter = "All Statuses";
    private String ordersActiveTab = "ALL"; // ALL, DINE_IN, DELIVERY, PICK_UP

    // Nested Helper Class for Cart items
    public static class CartItem {
        private MenuItem item;
        private int quantity;
        private int savedQuantity = 0;
        private List<String> modifiers = new ArrayList<>();
        private String notes = "";

        public CartItem(MenuItem item, int quantity) {
            this.item = item;
            this.quantity = quantity;
        }

        public MenuItem getItem() {
            return item;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public int getSavedQuantity() {
            return savedQuantity;
        }

        public void setSavedQuantity(int savedQuantity) {
            this.savedQuantity = savedQuantity;
        }

        public List<String> getModifiers() {
            return modifiers;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set default tenant context for the Biller PC
        TenantContext.setRestaurantId(TenantContext.getRestaurantId());

        if (dineInCustomerBtn != null) {
            dineInCustomerBtn.setOnAction(e -> openCustomerDetailsDialog());
        }

        cleanupMockOrders();

        loadTablesToUi();
        loadRunningOrders();
        loadStockOut();
        loadPlatformStats();
        loadPlatformOrders();

        // Setup Search/ComboBox listeners for POS Billing Screen
        if (menuSearchField != null) {
            menuSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                populateMenuGrid();
            });
            menuSearchField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) { // Focus gained
                    javafx.application.Platform.runLater(() -> {
                        if (menuSearchField.getText() != null && !menuSearchField.getText().isEmpty()) {
                            menuSearchField.clear();
                        }
                    });
                }
            });
        }

        if (categoryComboBox != null) {
            categoryComboBox.getItems().setAll("All Categories", "Starters", "Main Course", "Breads", "Sides",
                    "Desserts");
            categoryComboBox.setValue("All Categories");
            categoryComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                populateMenuGrid();
            });
        }

        if (receivedAmountField != null) {
            receivedAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
                updateCalculations();
            });
            receivedAmountField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) { // Focus gained
                    String text = receivedAmountField.getText().trim();
                    if (text.equals("0.00") || text.equals("0.0") || text.equals("0")) {
                        receivedAmountField.setText("");
                    }
                } else { // Focus lost
                    String text = receivedAmountField.getText().trim();
                    if (text.isEmpty()) {
                        receivedAmountField.setText("0.00");
                    } else {
                        try {
                            double val = Double.parseDouble(text);
                            receivedAmountField.setText(String.format("%.2f", val));
                        } catch (NumberFormatException e) {
                            // keep as is
                        }
                    }
                }
            });
        }

        // Setup Search/ComboBox listeners for Orders Screen
        if (ordersSearchField != null) {
            ordersSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                ordersSearchQuery = newValue.trim().toLowerCase();
                loadOrdersToUi();
            });
        }

        if (ordersStatusComboBox != null) {
            ordersStatusComboBox.getItems().setAll("All Statuses", "Preparing", "Ready", "Settled", "Cancelled");
            ordersStatusComboBox.setValue("All Statuses");
            ordersStatusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                ordersStatusFilter = newValue;
                loadOrdersToUi();
            });
        }

        if (detEditSearchField != null) {
            detEditSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                showMenuSearchSuggestions(newVal);
            });
        }
        if (detCancelEditBtn != null) {
            detCancelEditBtn.setOnAction(e -> handleCancelEditClick());
        }
        if (detSaveChangesBtn != null) {
            detSaveChangesBtn.setOnAction(e -> handleSaveChangesClick());
        }

        // Setup auto-refresh timeline running every 3 seconds
        autoRefreshTimeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(3),
                        event -> handleRefresh()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        if (menuWebView != null) {
            menuWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    try {
                        JSObject window = (JSObject) menuWebView.getEngine().executeScript("window");
                        window.setMember("javaApp", javaMenuBridge);
                        System.out.println("✅ Registered JavaMenuBridge successfully");
                        menuWebView.getEngine().executeScript("refreshUI();");
                    } catch (Exception e) {
                        System.out.println("Failed to register JavaMenuBridge: " + e.getMessage());
                    }
                }
            });
            try {
                menuWebView.getEngine().load(getClass().getResource("/ui/menu.html").toExternalForm());
            } catch (Exception e) {
                System.out.println("Failed to load Menu HTML: " + e.getMessage());
            }
        }

        if (dineInTableChip != null) {
            dineInTableChip.setOnAction(e -> openTableSelectionDialog());
        }

        // Initialize dynamic tab listeners
        if (tabDineInBtn != null && tabDeliveryBtn != null && tabPickupBtn != null) {
            tabDineInBtn.setOnAction(e -> {
                if (selectedOrderType != OrderType.DINE_IN) {
                    resetBillingSessionState();
                }
                handleTabSelection(OrderType.DINE_IN);
                showTablesView();
            });
            tabDeliveryBtn.setOnAction(e -> {
                if (selectedOrderType != OrderType.DELIVERY) {
                    resetBillingSessionState();
                }
                handleTabSelection(OrderType.DELIVERY);
            });
            tabPickupBtn.setOnAction(e -> {
                if (selectedOrderType != OrderType.PICK_UP) {
                    resetBillingSessionState();
                }
                handleTabSelection(OrderType.PICK_UP);
            });
        }

        // Focus & auto-calculation listeners for receivedAmountField
        if (receivedAmountField != null) {
            receivedAmountField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    String text = receivedAmountField.getText().trim();
                    if (text.equals("0.00") || text.equals("0.0") || text.equals("0")) {
                        receivedAmountField.clear();
                    }
                } else {
                    String text = receivedAmountField.getText().trim();
                    if (text.isEmpty()) {
                        receivedAmountField.setText("0.00");
                    } else {
                        try {
                            java.math.BigDecimal val = new java.math.BigDecimal(text);
                            receivedAmountField.setText(String.format("%.2f", val.doubleValue()));
                        } catch (Exception ex) {
                            receivedAmountField.setText("0.00");
                        }
                    }
                    updateCalculations();
                }
            });

            receivedAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
                updateCalculations();
            });
        }

        // Background lookup listeners for phone numbers
        if (deliveryPhoneField != null) {
            deliveryPhoneField.textProperty().addListener((observable, oldValue, newValue) -> {
                String phone = newValue.trim();
                if (phone.length() == 10 && phone.matches("\\d+")) {
                    Thread lookupThread = new Thread(() -> {
                        try {
                            Thread.sleep(300);
                            CustomerInfo info = MOCK_CUSTOMERS.get(phone);
                            if (info != null) {
                                Platform.runLater(() -> {
                                    deliveryNameField.setText(info.name);
                                    deliveryAddressField.setText(info.address);
                                    deliveryNameField
                                            .setStyle(deliveryNameField.getStyle() + "; -fx-border-color: #10B981;");
                                    deliveryAddressField
                                            .setStyle(deliveryAddressField.getStyle() + "; -fx-border-color: #10B981;");
                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(1000);
                                        } catch (Exception ex) {
                                        }
                                        Platform.runLater(() -> {
                                            deliveryNameField.setStyle(
                                                    "-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 6 8; -fx-font-size: 13px;");
                                            deliveryAddressField.setStyle(
                                                    "-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 4; -fx-font-size: 13px;");
                                        });
                                    }).start();
                                });
                            }
                        } catch (InterruptedException e) {
                            // ignored
                        }
                    });
                    lookupThread.start();
                }
            });
        }

        if (pickupPhoneField != null) {
            pickupPhoneField.textProperty().addListener((observable, oldValue, newValue) -> {
                String phone = newValue.trim();
                if (phone.length() == 10 && phone.matches("\\d+")) {
                    Thread lookupThread = new Thread(() -> {
                        try {
                            Thread.sleep(300);
                            CustomerInfo info = MOCK_CUSTOMERS.get(phone);
                            if (info != null) {
                                Platform.runLater(() -> {
                                    pickupNameField.setText(info.name);
                                    pickupNameField
                                            .setStyle(pickupNameField.getStyle() + "; -fx-border-color: #10B981;");
                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(1000);
                                        } catch (Exception ex) {
                                        }
                                        Platform.runLater(() -> {
                                            pickupNameField.setStyle(
                                                    "-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 6 8; -fx-font-size: 13px;");
                                        });
                                    }).start();
                                });
                            }
                        } catch (InterruptedException e) {
                            // ignored
                        }
                    });
                    lookupThread.start();
                }
            });
        }

        if (cartItemsContainer != null) {
            cartItemsContainer.heightProperty().addListener((observable, oldHeight, newHeight) -> {
                if (shouldScrollToBottom) {
                    shouldScrollToBottom = false;
                    scrollToBottom();
                }
            });
        }
    }

    @FXML
    private void logRefresh(String message) {
        try (java.io.FileWriter fw = new java.io.FileWriter("billing_refresh.log", true);
             java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
            pw.println(java.time.LocalDateTime.now() + " - " + message);
        } catch (Exception e) {
            // ignore
        }
    }

    public void handleRefresh() {
        logRefresh("handleRefresh: dashboardView=" + dashboardView.isVisible() +
                   ", tablesView=" + (tablesView != null && tablesView.isVisible()) +
                   ", ordersView=" + (ordersView != null && ordersView.isVisible()) +
                   ", kdsView=" + (kdsView != null && kdsView.isVisible()) +
                   ", billingView=" + (billingView != null && billingView.isVisible()));
        // Only refresh dashboard elements if dashboard view is active to prevent
        // database calls overriding POS screen state
        if (dashboardView.isVisible()) {
            refreshDashboardAsync();
        } else if (tablesView != null && tablesView.isVisible()) {
            loadTablesPageData();
        } else if (ordersView != null && ordersView.isVisible()) {
            if (!isEditingOrderItems) {
                loadOrdersToUi();
            }
        } else if (kdsView != null && kdsView.isVisible()) {
            if (kdsNativeController != null) {
                kdsNativeController.refreshKdsData();
            }
        } else if (billingView != null && billingView.isVisible()) {
            handleBillingViewRefresh();
        }
    }

    private void refreshDashboardAsync() {
        if (!isRefreshing.compareAndSet(false, true)) {
            return;
        }
        UUID restaurantId = TenantContext.getRestaurantId();
        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(restaurantId);
            try {
                // 1. Fetch tables
                var tablesList = tableRepository.findByRestaurantId(restaurantId);

                // 2. Fetch active orders
                var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                        java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));

                // 3. Fetch active KOTs for load gauge
                List<KOT> activeKots = new ArrayList<>();
                try {
                    activeKots = kotRepository.findByRestaurantIdAndOverallStatusIn(
                            restaurantId,
                            List.of(KOTStatus.PENDING, KOTStatus.PREPARING));
                } catch (Exception e) {
                    System.out.println("Error fetching active KOTs: " + e.getMessage());
                }
                java.time.LocalDate today = java.time.LocalDate.now();
                activeKots = activeKots.stream()
                        .filter(k -> k.getCreatedAt() != null && k.getCreatedAt().toLocalDate().isEqual(today))
                        .collect(java.util.stream.Collectors.toList());

                // 4. Pre-fetch KOTs in bulk for active orders to avoid N+1 queries
                List<UUID> orderIds = activeOrders.stream().map(Order::getId).collect(java.util.stream.Collectors.toList());
                List<KOT> allOrderKots = orderIds.isEmpty() ? new ArrayList<>() : kotRepository.findByOrderIdIn(orderIds);
                java.util.Map<UUID, List<KOT>> kotsMap = allOrderKots.stream()
                        .collect(java.util.stream.Collectors.groupingBy(KOT::getOrderId));

                // 5. Fetch unavailable menu items
                var menuItems = menuRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
                List<MenuItem> unavailableItems = menuItems.stream()
                        .filter(item -> !item.isAvailable())
                        .toList();

                // 6. Fetch platform stats (using optimized query for today's orders)
                java.time.LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();
                var platformTodayOrders = orderRepository.findByRestaurantIdAndStartedAtAfter(restaurantId, startOfToday);

                // Update UI on JavaFX thread
                final var finalTablesList = tablesList;
                final var finalActiveOrders = activeOrders;
                final var finalActiveKots = activeKots;
                final var finalUnavailableItems = unavailableItems;
                final var finalPlatformTodayOrders = platformTodayOrders;

                Platform.runLater(() -> {
                    try {
                        renderTablesToUiSync(finalTablesList, finalActiveOrders, finalActiveKots);
                        renderRunningOrdersSync(finalActiveOrders, kotsMap);
                        renderStockOutSync(finalUnavailableItems);
                        renderPlatformStatsSync(finalActiveOrders, finalPlatformTodayOrders);
                        renderPlatformOrdersSync(finalActiveOrders, kotsMap);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("Error in refreshDashboardAsync: " + e.getMessage());
            } finally {
                isRefreshing.set(false);
                TenantContext.clear();
            }
        });
    }

    // --- VIEW SWITCHING ACTION HANDLERS ---
    @FXML
    public void showHomeView() {
        Platform.runLater(() -> {
            dashboardView.setVisible(true);
            dashboardView.setManaged(true);

            billingView.setVisible(false);
            billingView.setManaged(false);

            if (tablesView != null) {
                tablesView.setVisible(false);
                tablesView.setManaged(false);
            }
            if (ordersView != null) {
                ordersView.setVisible(false);
                ordersView.setManaged(false);
            }
            if (kdsView != null) {
                kdsView.setVisible(false);
                kdsView.setManaged(false);
            }
            if (menuView != null) {
                menuView.setVisible(false);
                menuView.setManaged(false);
            }

            homeBtn.getStyleClass().add("active");
            billingBtn.getStyleClass().remove("active");
            if (tablesBtn != null) {
                tablesBtn.getStyleClass().remove("active");
            }
            if (ordersBtn != null) {
                ordersBtn.getStyleClass().remove("active");
            }
            if (kdsBtn != null) {
                kdsBtn.getStyleClass().remove("active");
            }
            if (menuBtn != null) {
                menuBtn.getStyleClass().remove("active");
            }

            // Refresh dashboard immediately when coming back
            loadTablesToUi();
            loadRunningOrders();
            loadStockOut();
            loadPlatformStats();
            loadPlatformOrders();
        });
    }

    @FXML
    public void showBillingView() {
        Platform.runLater(() -> {
            billingView.setVisible(true);
            billingView.setManaged(true);

            dashboardView.setVisible(false);
            dashboardView.setManaged(false);

            if (tablesView != null) {
                tablesView.setVisible(false);
                tablesView.setManaged(false);
            }
            if (ordersView != null) {
                ordersView.setVisible(false);
                ordersView.setManaged(false);
            }
            if (kdsView != null) {
                kdsView.setVisible(false);
                kdsView.setManaged(false);
            }
            if (menuView != null) {
                menuView.setVisible(false);
                menuView.setManaged(false);
            }

            billingBtn.getStyleClass().add("active");
            homeBtn.getStyleClass().remove("active");
            if (tablesBtn != null) {
                tablesBtn.getStyleClass().remove("active");
            }
            if (ordersBtn != null) {
                ordersBtn.getStyleClass().remove("active");
            }
            if (kdsBtn != null) {
                kdsBtn.getStyleClass().remove("active");
            }
            if (menuBtn != null) {
                menuBtn.getStyleClass().remove("active");
            }

            // Update dates on the POS Billing screen
            try {
                LocalDateTime now = LocalDateTime.now();
                java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter
                        .ofPattern("EEEE, d MMMM yyyy · hh:mm a");
                if (billingDateTimeLabel != null) {
                    billingDateTimeLabel.setText(now.format(format) + " · Main Outlet");
                }
                if (cartDateLabel != null) {
                    cartDateLabel.setText(now.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")));
                }
            } catch (Exception e) {
                System.out.println("Failed to update billing date: " + e.getMessage());
            }

            // Populate all static panels & grid list
            populateTop8();
            populateFrequentlyOrdered();
            populateModifiersUi();
            populateMenuGrid();
            populateCart();
            updateCalculations();
            updateBillingPageControlState();
        });
    }

    @FXML
    public void showTablesView() {
        Platform.runLater(() -> {
            if (tablesView != null) {
                tablesView.setVisible(true);
                tablesView.setManaged(true);
            }

            dashboardView.setVisible(false);
            dashboardView.setManaged(false);

            billingView.setVisible(false);
            billingView.setManaged(false);

            if (ordersView != null) {
                ordersView.setVisible(false);
                ordersView.setManaged(false);
            }
            if (kdsView != null) {
                kdsView.setVisible(false);
                kdsView.setManaged(false);
            }
            if (menuView != null) {
                menuView.setVisible(false);
                menuView.setManaged(false);
            }

            if (tablesBtn != null) {
                tablesBtn.getStyleClass().add("active");
            }
            homeBtn.getStyleClass().remove("active");
            billingBtn.getStyleClass().remove("active");
            if (ordersBtn != null) {
                ordersBtn.getStyleClass().remove("active");
            }
            if (kdsBtn != null) {
                kdsBtn.getStyleClass().remove("active");
            }
            if (menuBtn != null) {
                menuBtn.getStyleClass().remove("active");
            }

            loadTablesPageData();
        });
    }

    @FXML
    public void showOrdersView() {
        Platform.runLater(() -> {
            if (ordersView != null) {
                ordersView.setVisible(true);
                ordersView.setManaged(true);
            }

            dashboardView.setVisible(false);
            dashboardView.setManaged(false);

            billingView.setVisible(false);
            billingView.setManaged(false);

            if (tablesView != null) {
                tablesView.setVisible(false);
                tablesView.setManaged(false);
            }
            if (kdsView != null) {
                kdsView.setVisible(false);
                kdsView.setManaged(false);
            }
            if (menuView != null) {
                menuView.setVisible(false);
                menuView.setManaged(false);
            }

            if (ordersBtn != null) {
                ordersBtn.getStyleClass().add("active");
            }
            homeBtn.getStyleClass().remove("active");
            billingBtn.getStyleClass().remove("active");
            if (tablesBtn != null) {
                tablesBtn.getStyleClass().remove("active");
            }
            if (kdsBtn != null) {
                kdsBtn.getStyleClass().remove("active");
            }
            if (menuBtn != null) {
                menuBtn.getStyleClass().remove("active");
            }

            // ensureMockOrdersExist();
            loadOrdersToUi();
        });
    }

    @FXML
    public void showKdsView() {
        Platform.runLater(() -> {
            if (kdsView != null) {
                kdsView.setVisible(true);
                kdsView.setManaged(true);
                if (kdsNativeController != null) {
                    kdsNativeController.refreshKdsData();
                }
            }

            dashboardView.setVisible(false);
            dashboardView.setManaged(false);

            billingView.setVisible(false);
            billingView.setManaged(false);

            if (tablesView != null) {
                tablesView.setVisible(false);
                tablesView.setManaged(false);
            }
            if (ordersView != null) {
                ordersView.setVisible(false);
                ordersView.setManaged(false);
            }
            if (menuView != null) {
                menuView.setVisible(false);
                menuView.setManaged(false);
            }

            if (kdsBtn != null) {
                kdsBtn.getStyleClass().add("active");
            }
            homeBtn.getStyleClass().remove("active");
            billingBtn.getStyleClass().remove("active");
            if (tablesBtn != null) {
                tablesBtn.getStyleClass().remove("active");
            }
            if (ordersBtn != null) {
                ordersBtn.getStyleClass().remove("active");
            }
            if (menuBtn != null) {
                menuBtn.getStyleClass().remove("active");
            }
        });
    }

    @FXML
    public void showMenuView() {
        Platform.runLater(() -> {
            if (menuView != null) {
                menuView.setVisible(true);
                menuView.setManaged(true);
            }

            dashboardView.setVisible(false);
            dashboardView.setManaged(false);

            billingView.setVisible(false);
            billingView.setManaged(false);

            if (tablesView != null) {
                tablesView.setVisible(false);
                tablesView.setManaged(false);
            }
            if (ordersView != null) {
                ordersView.setVisible(false);
                ordersView.setManaged(false);
            }
            if (kdsView != null) {
                kdsView.setVisible(false);
                kdsView.setManaged(false);
            }

            if (menuBtn != null) {
                menuBtn.getStyleClass().add("active");
            }
            homeBtn.getStyleClass().remove("active");
            billingBtn.getStyleClass().remove("active");
            if (tablesBtn != null) {
                tablesBtn.getStyleClass().remove("active");
            }
            if (ordersBtn != null) {
                ordersBtn.getStyleClass().remove("active");
            }
            if (kdsBtn != null) {
                kdsBtn.getStyleClass().remove("active");
            }
        });
    }

    @FXML
    private void handleTableFilter(ActionEvent event) {
        Button btn = (Button) event.getSource();
        String text = btn.getText();
        String filterName = text.split(" ")[0];
        activeTableFilter = filterName;

        if (tblFilterAllBtn != null)
            tblFilterAllBtn.getStyleClass().remove("active");
        if (tblFilterAvailableBtn != null)
            tblFilterAvailableBtn.getStyleClass().remove("active");
        if (tblFilterRunningBtn != null)
            tblFilterRunningBtn.getStyleClass().remove("active");
        if (tblFilterBillingBtn != null)
            tblFilterBillingBtn.getStyleClass().remove("active");
        if (tblFilterReservedBtn != null)
            tblFilterReservedBtn.getStyleClass().remove("active");

        btn.getStyleClass().add("active");

        renderTablesPageContent();
    }

    private void ensureTableExists(String number, int capacity, String area, TableStatus status) {
        try {
            var list = tableRepository.findByRestaurantId(TenantContext.getRestaurantId());
            boolean exists = list.stream().anyMatch(t -> t.getTableNumber().equalsIgnoreCase(number));
            if (!exists) {
                DiningTable t = new DiningTable();
                t.setId(java.util.UUID.nameUUIDFromBytes(number.getBytes()));
                t.setRestaurantId(TenantContext.getRestaurantId());
                t.setTableNumber(number);
                t.setCapacity(capacity);
                t.setAreaName(area);
                t.setStatus(status);
                tableRepository.save(t);
            }
        } catch (Exception e) {
            System.out.println("Failed to ensure table: " + e.getMessage());
        }
    }

    private void loadTablesPageData() {
        UUID restaurantId = TenantContext.getRestaurantId();
        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(restaurantId);
            try {
                var tablesList = tableRepository.findByRestaurantId(restaurantId);
                var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                        java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));

                // Pre-fetch KOTs in bulk for all active orders to avoid N+1 queries in render
                List<UUID> orderIds = activeOrders.stream().map(Order::getId).collect(java.util.stream.Collectors.toList());
                List<KOT> allOrderKots = orderIds.isEmpty() ? new ArrayList<>() : kotRepository.findByOrderIdIn(orderIds);
                java.util.Map<UUID, List<KOT>> kotsMap = allOrderKots.stream()
                        .collect(java.util.stream.Collectors.groupingBy(KOT::getOrderId));

                long totalCount = tablesList.size();
                long availableCount = 0;
                long runningCount = 0;
                long billingCount = 0;
                long reservedCount = 0;

                for (DiningTable table : tablesList) {
                    if (table.getStatus() == TableStatus.AVAILABLE) {
                        availableCount++;
                    } else {
                        Order activeOrder = activeOrders.stream()
                                .filter(o -> o.getTableId() != null && o.getTableId().equals(table.getId()))
                                .findFirst()
                                .orElse(null);

                        boolean isBilling = false;
                        if (activeOrder != null) {
                            isBilling = (activeOrder.getStatus() == OrderStatus.BILLED);
                        } else {
                            isBilling = ("T-02".equalsIgnoreCase(table.getTableNumber())
                                    || "T-05".equalsIgnoreCase(table.getTableNumber()));
                        }

                        if (isBilling) {
                            billingCount++;
                        } else {
                            runningCount++;
                        }
                    }
                }

                final long finalTotalCount = totalCount;
                final long finalAvailableCount = availableCount;
                final long finalRunningCount = runningCount;
                final long finalBillingCount = billingCount;
                final long finalReservedCount = reservedCount;
                final var finalTablesList = tablesList;
                final var finalActiveOrders = activeOrders;

                Platform.runLater(() -> {
                    try {
                        if (tblFilterAllBtn != null)
                            tblFilterAllBtn.setText("All (" + finalTotalCount + ")");
                        if (tblFilterAvailableBtn != null)
                            tblFilterAvailableBtn.setText("Available (" + finalAvailableCount + ")");
                        if (tblFilterRunningBtn != null)
                            tblFilterRunningBtn.setText("Running (" + finalRunningCount + ")");
                        if (tblFilterBillingBtn != null)
                            tblFilterBillingBtn.setText("Billing (" + finalBillingCount + ")");
                        if (tblFilterReservedBtn != null)
                            tblFilterReservedBtn.setText("Reserved (" + finalReservedCount + ")");
                        
                        renderTablesPageContentSync(finalTablesList, finalActiveOrders, kotsMap);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.out.println("Failed to update filter numbers: " + e.getMessage());
            } finally {
                TenantContext.clear();
            }
        });
    }

    private void renderTablesPageContent() {
        loadTablesPageData();
    }

    private void renderTablesPageContentSync(List<DiningTable> tablesList, List<Order> activeOrders, java.util.Map<UUID, List<KOT>> kotsMap) {
        if (tablesPageAreasContainer == null)
            return;
        tablesPageAreasContainer.getChildren().clear();

        try {
            List<DiningTable> sortedTablesList = new ArrayList<>(tablesList);
            sortedTablesList.sort((t1, t2) -> compareTableNumbers(t1.getTableNumber(), t2.getTableNumber()));

            List<DiningTable> filteredTables = new ArrayList<>();
            for (DiningTable table : sortedTablesList) {
                boolean matchesFilter = false;

                if ("All".equalsIgnoreCase(activeTableFilter)) {
                    matchesFilter = true;
                } else if ("Available".equalsIgnoreCase(activeTableFilter)) {
                    matchesFilter = (table.getStatus() == TableStatus.AVAILABLE);
                } else if ("Running".equalsIgnoreCase(activeTableFilter)) {
                    if (table.getStatus() != TableStatus.AVAILABLE) {
                        Order activeOrder = activeOrders.stream()
                                .filter(o -> o.getTableId() != null && o.getTableId().equals(table.getId()))
                                .findFirst()
                                .orElse(null);
                        boolean isBilling = false;
                        if (activeOrder != null) {
                            isBilling = (activeOrder.getStatus() == OrderStatus.BILLED);
                        } else {
                            isBilling = ("T-02".equalsIgnoreCase(table.getTableNumber())
                                    || "T-05".equalsIgnoreCase(table.getTableNumber()));
                        }
                        matchesFilter = !isBilling;
                    }
                } else if ("Billing".equalsIgnoreCase(activeTableFilter)) {
                    if (table.getStatus() != TableStatus.AVAILABLE) {
                        Order activeOrder = activeOrders.stream()
                                .filter(o -> o.getTableId() != null && o.getTableId().equals(table.getId()))
                                .findFirst()
                                .orElse(null);
                        boolean isBilling = false;
                        if (activeOrder != null) {
                            isBilling = (activeOrder.getStatus() == OrderStatus.BILLED);
                        } else {
                            isBilling = ("T-02".equalsIgnoreCase(table.getTableNumber())
                                    || "T-05".equalsIgnoreCase(table.getTableNumber()));
                        }
                        matchesFilter = isBilling;
                    }
                } else if ("Reserved".equalsIgnoreCase(activeTableFilter)) {
                    matchesFilter = false;
                }

                if (matchesFilter) {
                    filteredTables.add(table);
                }
            }

            java.util.Map<String, List<DiningTable>> grouped = filteredTables.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            t -> {
                                String area = t.getAreaName();
                                if (area == null)
                                    return "AC Area";
                                if (!area.toLowerCase().contains("area")) {
                                    return area + " Area";
                                }
                                return area;
                            },
                            java.util.LinkedHashMap::new,
                            java.util.stream.Collectors.toList()));

            for (var entry : grouped.entrySet()) {
                String areaName = entry.getKey();
                List<DiningTable> areaTables = entry.getValue();
                areaTables.sort((t1, t2) -> {
                    boolean ready1 = isTableReadyToBill(t1, activeOrders);
                    boolean ready2 = isTableReadyToBill(t2, activeOrders);
                    if (ready1 && !ready2)
                        return -1;
                    if (!ready1 && ready2)
                        return 1;
                    return compareTableNumbers(t1.getTableNumber(), t2.getTableNumber());
                });

                HBox headerBox = new HBox();
                headerBox.setAlignment(Pos.CENTER_LEFT);
                headerBox.setSpacing(8);

                Region verticalLine = new Region();
                verticalLine.getStyleClass().add("area-header-line");

                Label areaLabel = new Label(areaName);
                areaLabel.getStyleClass().add("area-header-title");

                headerBox.getChildren().addAll(verticalLine, areaLabel);

                FlowPane cardsGrid = new FlowPane();
                cardsGrid.setHgap(15.0);
                cardsGrid.setVgap(15.0);

                for (DiningTable table : areaTables) {
                    double price = 0.0;
                    String stageText = "Clean and available for seating";
                    String statusLabelText = "Available";
                    String statusBadgeStyleClass = "available";
                    String timeText = "";
                    int itemsCount = 0;

                    Order activeOrder = activeOrders.stream()
                            .filter(o -> (o.getTableId() != null && o.getTableId().equals(table.getId())) ||
                                    (o.getMergedTableIds() != null
                                            && o.getMergedTableIds().contains(table.getId().toString())))
                            .findFirst()
                            .orElse(null);

                    if (activeOrder == null && table.getStatus() == TableStatus.AVAILABLE) {
                        statusLabelText = "Available";
                        statusBadgeStyleClass = "available";
                        stageText = "Clean and available for seating";
                    } else {
                        if (activeOrder != null) {
                            price = calculateOrderAmountFallback(activeOrder);
                            int duration = (int) java.time.Duration
                                    .between(getOrderStartTimeFallback(activeOrder), LocalDateTime.now())
                                    .toMinutes();
                            timeText = duration + "m";

                            try {
                                List<KOT> orderKots = kotsMap.getOrDefault(activeOrder.getId(), new ArrayList<>());
                                for (KOT kot : orderKots) {
                                    for (KOTItem item : kot.getItems()) {
                                        itemsCount += item.getQuantity();
                                    }
                                }
                            } catch (Exception e) {
                            }
                            if (itemsCount == 0)
                                itemsCount = 2;

                            if (activeOrder.getStatus() == OrderStatus.BILLED) {
                                statusLabelText = "Billing";
                                statusBadgeStyleClass = "billing";
                                if (activeOrder.getMergedTableIds() != null
                                        && !activeOrder.getMergedTableIds().isEmpty()) {
                                    stageText = activeOrder.getTableName();
                                } else {
                                    stageText = itemsCount + " items • Ready to clear";
                                }
                                timeText = "";
                            } else {
                                if (duration < 25) {
                                    statusLabelText = "Prep";
                                    statusBadgeStyleClass = "prep";
                                    if (activeOrder.getMergedTableIds() != null
                                            && !activeOrder.getMergedTableIds().isEmpty()) {
                                        stageText = activeOrder.getTableName();
                                    } else {
                                        stageText = itemsCount + " items • Pending Kitchen";
                                    }
                                } else {
                                    statusLabelText = "Running";
                                    statusBadgeStyleClass = "running";
                                    if (activeOrder.getMergedTableIds() != null
                                            && !activeOrder.getMergedTableIds().isEmpty()) {
                                        stageText = activeOrder.getTableName();
                                    } else {
                                        stageText = itemsCount + " items • Serving main course";
                                    }
                                }
                            }
                        } else {
                            if ("T-01".equalsIgnoreCase(table.getTableNumber())) {
                                price = 1200.0;
                                itemsCount = 3;
                                stageText = "3 items • Pending Kitchen";
                                statusLabelText = "Prep";
                                statusBadgeStyleClass = "prep";
                                timeText = "12m";
                            } else if ("T-02".equalsIgnoreCase(table.getTableNumber())) {
                                price = 1890.0;
                                itemsCount = 5;
                                stageText = "5 items • Ready to clear";
                                statusLabelText = "Billing";
                                statusBadgeStyleClass = "billing";
                                timeText = "";
                            } else if ("T-05".equalsIgnoreCase(table.getTableNumber())) {
                                price = 950.0;
                                itemsCount = 2;
                                stageText = "2 items • Ready to clear";
                                statusLabelText = "Billing";
                                statusBadgeStyleClass = "billing";
                                timeText = "";
                            } else if ("T-08".equalsIgnoreCase(table.getTableNumber())) {
                                price = 3450.0;
                                itemsCount = 8;
                                stageText = "8 items • Serving main course";
                                statusLabelText = "Running";
                                statusBadgeStyleClass = "running";
                                timeText = "45m";
                            } else if ("G-02".equalsIgnoreCase(table.getTableNumber())) {
                                price = 2100.0;
                                itemsCount = 4;
                                stageText = "4 items • Pending Kitchen";
                                statusLabelText = "Prep";
                                statusBadgeStyleClass = "prep";
                                timeText = "22m";
                            } else {
                                price = 1500.0;
                                stageText = "3 items • In progress";
                                statusLabelText = "Running";
                                statusBadgeStyleClass = "running";
                                timeText = "15m";
                            }
                        }
                    }

                    VBox card = new VBox();
                    card.setPrefSize(230, 135);
                    card.setMinSize(230, 135);
                    card.setMaxSize(230, 135);
                    card.getStyleClass().addAll("tables-page-card", statusBadgeStyleClass);
                    card.setSpacing(8);

                    HBox topRow = new HBox();
                    topRow.setAlignment(Pos.CENTER_LEFT);
                    topRow.setSpacing(6);

                    Label titleLabel = new Label(table.getTableNumber());
                    titleLabel.getStyleClass().add("tables-page-card-title");
                    topRow.getChildren().add(titleLabel);

                    if (table.getStatus() != TableStatus.AVAILABLE) {
                        SVGPath peopleIcon = new SVGPath();
                        peopleIcon
                                .setContent("M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2 M9 7a4 4 0 100-8 4 4 0 000 8");
                        peopleIcon.setStyle("-fx-fill: transparent; -fx-stroke: #64748B; -fx-stroke-width: 1.5;");
                        peopleIcon.setScaleX(0.7);
                        peopleIcon.setScaleY(0.7);
                        topRow.getChildren().add(peopleIcon);
                    }

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    topRow.getChildren().add(spacer);

                    if (table.getStatus() != TableStatus.AVAILABLE) {
                        if (!timeText.isEmpty()) {
                            HBox timeBox = new HBox();
                            timeBox.setAlignment(Pos.CENTER_LEFT);
                            timeBox.setSpacing(4);

                            SVGPath clockIcon = new SVGPath();
                            clockIcon.setContent("M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z");
                            clockIcon
                                    .setStyle("-fx-fill: transparent; -fx-stroke: #64748B; -fx-stroke-width: 1.5;");
                            clockIcon.setScaleX(0.7);
                            clockIcon.setScaleY(0.7);

                            Label timeLabel = new Label(timeText);
                            timeLabel.setStyle(
                                    "-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

                            timeBox.getChildren().addAll(clockIcon, timeLabel);
                            topRow.getChildren().add(timeBox);
                        } else {
                            Label doneLabel = new Label("Done");
                            doneLabel.setStyle(
                                    "-fx-font-size: 11px; -fx-text-fill: #10B981; -fx-font-weight: bold;");
                            topRow.getChildren().add(doneLabel);
                        }
                    }

                    Label stageLabel = new Label(stageText);
                    stageLabel.getStyleClass().add("tables-page-card-meta");
                    stageLabel.setWrapText(true);
                    stageLabel.setPrefHeight(40);
                    stageLabel.setMinHeight(40);
                    if (activeOrder != null && activeOrder.getMergedTableIds() != null
                            && !activeOrder.getMergedTableIds().isEmpty()) {
                        stageLabel.setStyle(
                                "-fx-alignment: center; -fx-font-weight: 800; -fx-text-fill: #1E293B; -fx-font-size: 15px;");
                    }

                    HBox bottomRow = new HBox();
                    bottomRow.setAlignment(Pos.CENTER_LEFT);

                    if (price > 0.0) {
                        Label priceLabel = new Label(String.format("₹%,.0f", price));
                        priceLabel.getStyleClass().add("tables-page-card-price");
                        bottomRow.getChildren().add(priceLabel);
                    }

                    Region bottomSpacer = new Region();
                    HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
                    bottomRow.getChildren().add(bottomSpacer);

                    HBox badge = new HBox();
                    badge.getStyleClass().addAll("tables-page-status-badge", statusBadgeStyleClass);
                    badge.setAlignment(Pos.CENTER);
                    badge.setSpacing(5);

                    Region dot = new Region();
                    dot.getStyleClass().addAll("tables-page-badge-dot", statusBadgeStyleClass);
                    dot.setPrefSize(6, 6);
                    dot.setMinSize(6, 6);
                    dot.setMaxSize(6, 6);

                    Label badgeLabel = new Label(statusLabelText);
                    badgeLabel.getStyleClass().addAll("tables-page-badge-text", statusBadgeStyleClass);

                    badge.getChildren().addAll(dot, badgeLabel);
                    bottomRow.getChildren().add(badge);

                    card.getChildren().addAll(topRow, stageLabel, bottomRow);

                    card.setOnMouseClicked(clickEvent -> {
                        try {
                            // Clear L1 cache to evict cached state
                            try {
                                jakarta.persistence.EntityManager em = applicationContext.getBean(jakarta.persistence.EntityManager.class);
                                if (em != null) {
                                    em.clear();
                                }
                            } catch (Exception ex) {
                                // ignore
                            }

                            currentDiningTable = table;
                            selectedOrderType = OrderType.DINE_IN;

                            // Find active running order for the table
                            UUID restaurantId = TenantContext.getRestaurantId();
                            var tableActiveOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                                    java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));
                            Order clickActiveOrder = tableActiveOrders.stream()
                                    .filter(o -> (o.getTableId() != null && o.getTableId().equals(table.getId())) ||
                                            (o.getMergedTableIds() != null
                                                    && o.getMergedTableIds().contains(table.getId().toString())))
                                    .findFirst()
                                    .orElse(null);

                            if (clickActiveOrder != null) {
                                currentActiveOrder = clickActiveOrder;
                                if (clickActiveOrder.getTableId() != null) {
                                    currentDiningTable = tableRepository.findById(clickActiveOrder.getTableId())
                                            .orElse(table);
                                }
                                loadOrderItemsToCart(clickActiveOrder);
                            } else {
                                currentActiveOrder = null;
                                cartList.clear();
                            }

                            // Update meta views
                            if (dineInTableChip != null) {
                                if (clickActiveOrder != null && clickActiveOrder.getMergedTableIds() != null
                                        && !clickActiveOrder.getMergedTableIds().isEmpty()) {
                                    dineInTableChip.setText(clickActiveOrder.getTableName());
                                } else {
                                    dineInTableChip.setText(currentDiningTable.getTableNumber());
                                }
                            }
                            if (dineInOrderNumLabel != null) {
                                dineInOrderNumLabel
                                        .setText(clickActiveOrder != null ? clickActiveOrder.getOrderNumber()
                                                : "New Session");
                            }
                            if (clickActiveOrder != null) {
                                currentCustomerPhone = clickActiveOrder.getCustomerPhone() != null
                                        ? clickActiveOrder.getCustomerPhone()
                                        : "";
                                currentCustomerName = clickActiveOrder.getCustomerName() != null
                                        ? clickActiveOrder.getCustomerName()
                                        : "";
                                currentCustomerNotes = clickActiveOrder.getNotes() != null
                                        ? clickActiveOrder.getNotes()
                                        : "";
                            } else {
                                currentCustomerPhone = "";
                                currentCustomerName = "";
                                currentCustomerNotes = "";
                            }
                            updateCustomerButtonState();

                            // Handle switching visual tabs
                            handleTabSelection(OrderType.DINE_IN);

                            if (cartItemsContainer != null) {
                                boolean isBilled = clickActiveOrder != null
                                        && clickActiveOrder.getStatus() == OrderStatus.BILLED;
                                if (table.getStatus() == TableStatus.PAYMENT_PENDING || isBilled) {
                                    cartItemsContainer.setDisable(true);
                                } else {
                                    cartItemsContainer.setDisable(false);
                                }
                            }

                            showBillingView();
                        } catch (Exception ex) {
                            System.out.println("Failed to click table: " + ex.getMessage());
                        }
                    });
                    card.setStyle("-fx-cursor: hand;");

                    cardsGrid.getChildren().add(card);
                }

                tablesPageAreasContainer.getChildren().addAll(headerBox, cardsGrid);
            }
        } catch (Exception ex) {
            System.out.println("Error rendering tables page: " + ex.getMessage());
        }
    }

    private List<MenuItem> getAllMenuItemsForBilling() {
        List<MenuItem> list = new ArrayList<>();
        try {
            list.addAll(menuRepository.findByRestaurantIdAndIsDeletedFalse(TenantContext.getRestaurantId()));
        } catch (Exception e) {
            System.out.println("Failed to fetch from DB: " + e.getMessage());
        }

        return list;
    }

    private void forceOrAddMock(List<MenuItem> list, String name, String code, double price, boolean isVeg,
            String category) {
        MenuItem item = resolveOrCreateMenuItem(name, code, price, isVeg, category);
        list.removeIf(i -> i.getName().equalsIgnoreCase(name));
        list.add(item);
    }

    private MenuItem resolveOrCreateMenuItem(String name, String code, double price, boolean isVeg,
            String categoryName) {
        UUID restaurantId = TenantContext.getRestaurantId();
        UUID nameBasedId = java.util.UUID.nameUUIDFromBytes(name.getBytes());
        try {
            // Check if it exists in DB by ID first
            Optional<MenuItem> dbItemOpt = menuRepository.findById(nameBasedId);
            if (dbItemOpt.isPresent()) {
                return dbItemOpt.get();
            }

            // Also check by name case-insensitive under this restaurant
            Optional<MenuItem> dbItemByNameOpt = menuRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
                    .stream()
                    .filter(item -> item.getName().equalsIgnoreCase(name))
                    .findFirst();
            if (dbItemByNameOpt.isPresent()) {
                return dbItemByNameOpt.get();
            }

            // Create new MenuItem with the specific name-based UUID to ensure consistency
            MenuItem newItem = new MenuItem();
            newItem.setId(nameBasedId); // Force ID to match name-based UUID
            newItem.setRestaurantId(restaurantId);
            newItem.setName(name);
            newItem.setShortCode(code);
            newItem.setPrice(java.math.BigDecimal.valueOf(price));
            newItem.setVeg(isVeg);
            newItem.setAvailable(true);
            newItem.setCategoryName(categoryName);

            // Find or create category in DB to satisfy foreign key constraints
            try {
                Category cat = categoryRepository.findAll().stream()
                        .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                        .findFirst()
                        .orElseGet(() -> {
                            List<Category> all = categoryRepository.findAll();
                            if (!all.isEmpty()) {
                                return all.get(0);
                            }
                            Category newCat = new Category();
                            newCat.setRestaurantId(restaurantId);
                            newCat.setName(categoryName);
                            return categoryRepository.save(newCat);
                        });
                newItem.setCategory(cat);
                newItem.setCategoryName(cat.getName());
            } catch (Exception catEx) {
                System.out.println("Failed to resolve/create Category: " + catEx.getMessage());
            }

            UUID categoryId = newItem.getCategory() != null ? newItem.getCategory().getId() : null;
            String sql = "INSERT INTO menu_items (id, restaurant_id, name, short_code, price, is_veg, is_available, is_todays_menu, category_name, category_id, is_deleted, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, true, ?, ?, false, NOW(), NOW())";
            jdbcTemplate.update(sql,
                    nameBasedId,
                    restaurantId,
                    name,
                    code,
                    java.math.BigDecimal.valueOf(price),
                    isVeg,
                    true,
                    categoryName,
                    categoryId);
            System.out.println(
                    "✅ Dynamically created and saved menu item via JDBC: " + name + " with ID: " + nameBasedId);
            return newItem;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("CRITICAL: Failed to save MenuItem to database: " + e.getMessage());
            Platform.runLater(() -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Database Save Error");
                    alert.setHeaderText("Failed to seed MenuItem: " + name);
                    alert.setContentText(e.toString());
                    alert.showAndWait();
                } catch (Exception dialogEx) {
                    // Ignore if UI thread issue
                }
            });

            MenuItem fallback = new MenuItem();
            fallback.setId(nameBasedId);
            fallback.setRestaurantId(restaurantId);
            fallback.setName(name);
            fallback.setShortCode(code);
            fallback.setPrice(java.math.BigDecimal.valueOf(price));
            fallback.setVeg(isVeg);
            fallback.setAvailable(true);
            fallback.setCategoryName(categoryName);
            return fallback;
        }
    }

    private void populateTop8() {
        if (top8ItemsContainer == null)
            return;
        top8ItemsContainer.getChildren().clear();

        String[] names = {
                "Butter Chicken Masala", "Paneer Tikka", "Veg Biryani",
                "Hakka Noodles", "Chilli Paneer", "Garlic Naan",
                "Schezwan Noodles", "Masala Dosa"
        };
        String[] codes = { "BCM", "PT", "VB", "HN", "CP", "GN", "SN", "MD" };
        double[] prices = { 480, 220, 280, 200, 240, 90, 220, 120 };
        boolean[] isVegs = { false, true, true, true, true, true, false, true };

        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            HBox row = new HBox();
            row.getStyleClass().add("top-8-row");
            row.setSpacing(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label rankLabel = new Label(String.valueOf(i + 1));
            rankLabel.getStyleClass().add("top-8-num");

            Label nameLabel = new Label(names[i]);
            nameLabel.getStyleClass().add("top-8-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label codeLabel = new Label(codes[i]);
            codeLabel.getStyleClass().add("top-8-code");

            Button addBtn = new Button("+");
            addBtn.getStyleClass().add("qty-btn");
            addBtn.setStyle(
                    "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-weight: bold; -fx-cursor: hand;");
            addBtn.setOnAction(e -> {
                MenuItem item = resolveOrCreateMenuItem(names[idx], codes[idx], prices[idx], isVegs[idx],
                        isVegs[idx] ? "Veg" : "Non-Veg");
                handleAddMenuItem(item);
            });

            row.getChildren().addAll(rankLabel, nameLabel, spacer, codeLabel, addBtn);
            top8ItemsContainer.getChildren().add(row);
        }
    }

    private void populateFrequentlyOrdered() {
        if (frequentItemsContainer == null)
            return;
        frequentItemsContainer.getChildren().clear();

        String[] names = { "Paneer Tikka", "Veg Manchurian", "Chilli Paneer", "Jeera Rice" };
        String[] codes = { "PT", "VM", "CP", "JR" };
        double[] prices = { 220, 200, 240, 140 };
        boolean[] isVegs = { true, true, true, true };

        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            HBox row = new HBox();
            row.getStyleClass().add("top-8-row");
            row.setSpacing(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(names[i] + " (" + codes[i] + ")");
            nameLabel.getStyleClass().add("top-8-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button addBtn = new Button("+");
            addBtn.getStyleClass().add("qty-btn");
            addBtn.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-weight: bold;");
            addBtn.setOnAction(e -> {
                MenuItem item = resolveOrCreateMenuItem(names[idx], codes[idx], prices[idx], isVegs[idx],
                        isVegs[idx] ? "Veg" : "Non-Veg");
                handleAddMenuItem(item);
            });

            row.getChildren().addAll(nameLabel, spacer, addBtn);
            frequentItemsContainer.getChildren().add(row);
        }
    }

    private void populateModifiersUi() {
        if (modifiersContainer == null)
            return;
        modifiersContainer.getChildren().clear();

        String[] mods = { "Less Spicy", "Extra Spicy", "Extra Gravy", "Dry" };
        for (String mod : mods) {
            boolean isActive = false;
            if (selectedCartItem != null) {
                isActive = selectedCartItem.getModifiers().contains(mod);
            } else {
                isActive = activeModifiers.contains(mod);
            }

            Button chip = new Button(isActive ? "✓ " + mod : mod);
            chip.getStyleClass().add("modifier-chip");
            if (isActive) {
                chip.getStyleClass().add("active");
            }

            final boolean activeVal = isActive;
            chip.setOnAction(e -> {
                if (selectedCartItem != null) {
                    if (activeVal) {
                        selectedCartItem.getModifiers().remove(mod);
                    } else {
                        if (!selectedCartItem.getModifiers().contains(mod)) {
                            selectedCartItem.getModifiers().add(mod);
                        }
                    }
                    populateCart();
                } else {
                    if (activeVal) {
                        activeModifiers.remove(mod);
                    } else {
                        activeModifiers.add(mod);
                    }
                }
                populateModifiersUi();
            });
            modifiersContainer.getChildren().add(chip);
        }
    }

    private void showModifierDialog(CartItem ci) {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Customize " + ci.getItem().getName());
        dialog.setHeaderText("Add modifiers & allergy info for: " + ci.getItem().getName());

        javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialogPane.setStyle("-fx-background-color: #FFFFFF;");

        javafx.scene.control.ButtonType cancelItemType = new javafx.scene.control.ButtonType("Cancel Item",
                javafx.scene.control.ButtonBar.ButtonData.LEFT);
        boolean isRunning = currentActiveOrder != null;
        KOTItem matchingItem = null;
        KOT matchingKot = null;
        if (isRunning) {
            try {
                List<KOT> kots = kotRepository.findByOrderId(currentActiveOrder.getId());
                for (KOT kot : kots) {
                    for (KOTItem ki : kot.getItems()) {
                        if (ki.getMenuItemId().equals(ci.getItem().getId())
                                && ki.getItemStatus() != KOTStatus.CANCELLED) {
                            matchingItem = ki;
                            matchingKot = kot;
                            break;
                        }
                    }
                    if (matchingItem != null)
                        break;
                }
            } catch (Exception e) {
                System.out.println("Error matching KOT item for cancellation: " + e.getMessage());
            }
        }

        if (matchingItem != null && matchingKot != null) {
            dialogPane.getButtonTypes().add(0, cancelItemType);
        }

        javafx.scene.control.Button okBtn = (javafx.scene.control.Button) dialogPane
                .lookupButton(javafx.scene.control.ButtonType.OK);
        javafx.scene.control.Button cancelBtn = (javafx.scene.control.Button) dialogPane
                .lookupButton(javafx.scene.control.ButtonType.CANCEL);
        javafx.scene.control.Button cancelItemBtn = (matchingItem != null)
                ? (javafx.scene.control.Button) dialogPane.lookupButton(cancelItemType)
                : null;

        if (okBtn != null) {
            okBtn.setStyle(
                    "-fx-background-color: #0A4F34; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;");
        }
        if (cancelBtn != null) {
            cancelBtn.setStyle(
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 6px;");
        }
        if (cancelItemBtn != null) {
            cancelItemBtn.setStyle(
                    "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        }

        VBox vbox = new VBox(12);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-pref-width: 380px; -fx-background-color: #FFFFFF;");

        Label modHeader = new Label("Quick Modifiers");
        modHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        FlowPane chipsPane = new FlowPane();
        chipsPane.setHgap(8);
        chipsPane.setVgap(8);

        String[] mods = { "Less Spicy", "Extra Spicy", "Extra Gravy", "Dry" };
        List<String> tempMods = new ArrayList<>(ci.getModifiers());

        for (String mod : mods) {
            javafx.scene.control.Button chip = new javafx.scene.control.Button(
                    tempMods.contains(mod) ? "✓ " + mod : mod);
            if (tempMods.contains(mod)) {
                chip.setStyle(
                        "-fx-background-color: #F0FDF4; -fx-border-color: #10B981; -fx-text-fill: #047857; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-padding: 5 12; -fx-font-weight: bold; -fx-cursor: hand;");
            } else {
                chip.setStyle(
                        "-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-text-fill: #475569; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-padding: 5 12; -fx-font-weight: bold; -fx-cursor: hand;");
            }

            chip.setOnAction(e -> {
                if (tempMods.contains(mod)) {
                    tempMods.remove(mod);
                    chip.setText(mod);
                    chip.setStyle(
                            "-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-text-fill: #475569; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-padding: 5 12; -fx-font-weight: bold; -fx-cursor: hand;");
                } else {
                    tempMods.add(mod);
                    chip.setText("✓ " + mod);
                    chip.setStyle(
                            "-fx-background-color: #F0FDF4; -fx-border-color: #10B981; -fx-text-fill: #047857; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-padding: 5 12; -fx-font-weight: bold; -fx-cursor: hand;");
                }
            });
            chipsPane.getChildren().add(chip);
        }

        Label noteHeader = new Label("Allergy & Special Instructions");
        noteHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        javafx.scene.control.TextField noteField = new javafx.scene.control.TextField(ci.getNotes());
        noteField.setPromptText("e.g. Peanut allergy, no onions, extra hot");
        noteField.setStyle(
                "-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8; -fx-font-size: 13px;");

        vbox.getChildren().addAll(modHeader, chipsPane, new javafx.scene.control.Separator(), noteHeader, noteField);
        dialogPane.setContent(vbox);

        final KOTItem finalMatchingItem = matchingItem;
        final KOT finalMatchingKot = matchingKot;

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                ci.getModifiers().clear();
                ci.getModifiers().addAll(tempMods);
                ci.setNotes(noteField.getText().trim());
                populateCart();
                populateModifiersUi();
            } else if (btn == cancelItemType) {
                if (finalMatchingItem != null && finalMatchingKot != null) {
                    performItemCancellation(finalMatchingItem, finalMatchingKot, currentActiveOrder);
                }
            }
        });
    }

    private void populateMenuGrid() {
        if (billingMenuGrid == null)
            return;
        billingMenuGrid.getChildren().clear();

        List<MenuItem> allItems = getAllMenuItemsForBilling();
        String searchTxt = menuSearchField != null ? menuSearchField.getText().trim().toLowerCase() : "";
        String categoryVal = categoryComboBox != null ? categoryComboBox.getValue() : "All Categories";

        int col = 0;
        int rowIdx = 0;
        for (MenuItem item : allItems) {
            // Search filter match
            if (!searchTxt.isEmpty()) {
                boolean matchName = item.getName().toLowerCase().contains(searchTxt);
                boolean matchCode = item.getShortCode() != null
                        && item.getShortCode().toLowerCase().contains(searchTxt);
                if (!matchName && !matchCode)
                    continue;
            }

            // Category filter match
            if (categoryVal != null && !categoryVal.equals("All Categories")) {
                String catName = item.getCategoryName();
                if (catName == null)
                    continue;
                if (categoryVal.equals("Starters") && !catName.equalsIgnoreCase("Starters"))
                    continue;
                if (categoryVal.equals("Main Course") && !catName.equalsIgnoreCase("Main Course")
                        && !catName.equalsIgnoreCase("mains"))
                    continue;
                if (categoryVal.equals("Breads") && !catName.equalsIgnoreCase("Breads"))
                    continue;
                if (categoryVal.equals("Sides") && !catName.equalsIgnoreCase("Sides"))
                    continue;
                if (categoryVal.equals("Desserts") && !catName.equalsIgnoreCase("Desserts"))
                    continue;
            }

            // Card Builder
            VBox card = new VBox();
            card.getStyleClass().add("menu-grid-card");
            card.setSpacing(6);
            card.setMaxWidth(Double.MAX_VALUE);

            HBox topRow = new HBox();
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(item.getShortCode() != null ? item.getShortCode() : "ITEM");
            badge.getStyleClass().add("menu-card-badge");
            if (item.isVeg()) {
                badge.setStyle("-fx-background-color: #1E5144;");
            } else {
                badge.setStyle("-fx-background-color: #B91C1C;");
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox indicatorBox = new HBox();
            indicatorBox.getStyleClass().add("indicator-box");
            if (item.isVeg()) {
                indicatorBox.getStyleClass().add("veg");
                Region vegDot = new Region();
                vegDot.getStyleClass().add("veg-indicator");
                indicatorBox.getChildren().add(vegDot);
            } else {
                indicatorBox.getStyleClass().add("nonveg");
                Region nvDot = new Region();
                nvDot.getStyleClass().add("nonveg-indicator");
                indicatorBox.getChildren().add(nvDot);
            }

            topRow.getChildren().addAll(badge, spacer, indicatorBox);

            Label nameLabel = new Label(item.getName());
            nameLabel.getStyleClass().add("menu-card-name");
            nameLabel.setWrapText(true);
            nameLabel.setPrefHeight(36);
            nameLabel.setMinHeight(36);

            HBox bottomRow = new HBox();
            bottomRow.setAlignment(Pos.CENTER_LEFT);

            Label priceLabel = new Label(String.format("₹%.2f", item.getPrice().doubleValue()));
            priceLabel.getStyleClass().add("menu-card-price");

            Region bSpacer = new Region();
            HBox.setHgrow(bSpacer, Priority.ALWAYS);

            Button addBtn = new Button("+");
            addBtn.getStyleClass().add("menu-card-add-btn");
            addBtn.setOnAction(e -> handleAddMenuItem(item));

            if (!item.isTodaysMenu() || !item.isAvailable()) {
                card.setOpacity(0.40);
                addBtn.setText("Not Available");
                addBtn.setDisable(true);
                addBtn.setPrefWidth(96);
                addBtn.setMinWidth(96);
                addBtn.setMaxWidth(96);
                addBtn.setStyle(
                        "-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-font-size: 10px; -fx-border-color: transparent; -fx-alignment: center; -fx-padding: 0;");
            }

            bottomRow.getChildren().addAll(priceLabel, bSpacer, addBtn);

            card.getChildren().addAll(topRow, nameLabel, bottomRow);
            billingMenuGrid.add(card, col, rowIdx);

            col++;
            if (col == 3) {
                col = 0;
                rowIdx++;
            }
        }
    }

    private void handleAddMenuItem(MenuItem item) {
        if (!item.isTodaysMenu() || !item.isAvailable()) {
            return;
        }

        // ── Cart-splitting logic ──────────────────────────────────────────
        // We only merge into an existing cart line if BOTH the menu item
        // AND the active modifiers match exactly. If modifiers differ (or
        // the existing line already has a special-instruction note), we add
        // a brand-new line so the biller can apply different customisations
        // to each portion (e.g. "3× Spicy" and "2× Less Spicy" for the
        // same dish).
        CartItem existing = cartList.stream()
                .filter(ci -> {
                    if (!ci.getItem().getName().equalsIgnoreCase(item.getName()))
                        return false;
                    // Modifier sets must match exactly
                    boolean modsMatch = ci.getModifiers().size() == activeModifiers.size()
                            && ci.getModifiers().containsAll(activeModifiers);
                    // Notes must be empty (a noted line is always kept separate)
                    boolean notesEmpty = (ci.getNotes() == null || ci.getNotes().trim().isEmpty());
                    return modsMatch && notesEmpty;
                })
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + 1);
            selectedCartItem = existing;
        } else {
            CartItem ci = new CartItem(item, 1);
            if (!activeModifiers.isEmpty()) {
                ci.getModifiers().addAll(activeModifiers);
            }
            cartList.add(ci);
            selectedCartItem = ci;
        }

        // Reset active modifier choices after every add
        activeModifiers.clear();
        populateModifiersUi();

        updateCalculations();
        shouldScrollToBottom = true;
        populateCart();

        if (billingMenuGrid != null) {
            billingMenuGrid.requestFocus();
        }
    }

    private void populateCart() {
        if (cartItemsContainer == null)
            return;
        cartItemsContainer.getChildren().clear();

        for (CartItem ci : cartList) {
            HBox row = new HBox();
            row.getStyleClass().add("cart-item-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-cursor: hand;");

            int totalQty = ci.getQuantity();
            int savedQty = ci.getSavedQuantity();
            int newQty = totalQty - savedQty;

            if (ci == selectedCartItem) {
                row.setStyle(row.getStyle()
                        + " -fx-background-color: #E2E8F0; -fx-background-radius: 6px; -fx-border-color: #94A3B8; -fx-border-width: 1px; -fx-border-radius: 6px;");
            } else if (savedQty == 0) {
                // Completely new item: highlight in soft mint green with light green border
                row.setStyle(row.getStyle()
                        + " -fx-background-color: #ECFDF5; -fx-background-radius: 6px; -fx-border-color: #A7F3D0; -fx-border-width: 1px; -fx-border-radius: 6px;");
            } else if (newQty > 0) {
                // Mixed item: has new quantity added to a saved item: highlight in soft
                // yellow/gold border
                row.setStyle(row.getStyle()
                        + " -fx-background-color: #FFFDF5; -fx-background-radius: 6px; -fx-border-color: #FCD34D; -fx-border-width: 1px; -fx-border-radius: 6px;");
            } else {
                // Purely saved item: standard transparent/light-gray style
                row.setStyle(row.getStyle()
                        + " -fx-background-color: #F8FAFC; -fx-background-radius: 6px; -fx-border-color: #E2E8F0; -fx-border-width: 1px; -fx-border-radius: 6px;");
            }

            row.setOnMouseClicked(e -> {
                if (selectedCartItem == ci) {
                    selectedCartItem = null;
                } else {
                    selectedCartItem = ci;
                }
                populateCart();
                populateModifiersUi();
            });

            // Name Column (width 180px base, grows to fill)
            VBox nameBox = new VBox();
            nameBox.setPrefWidth(180);
            javafx.scene.layout.HBox.setHgrow(nameBox, javafx.scene.layout.Priority.ALWAYS);
            nameBox.setMaxWidth(Double.MAX_VALUE);
            nameBox.setSpacing(2);

            HBox titleLine = new HBox();
            titleLine.setAlignment(Pos.CENTER_LEFT);
            titleLine.setSpacing(4);
            Region indicator = new Region();
            if (ci.getItem().isVeg()) {
                indicator.getStyleClass().add("veg-indicator");
            } else {
                indicator.getStyleClass().add("nonveg-indicator");
            }
            Label nameLabel = new Label(ci.getItem().getName());
            nameLabel.getStyleClass().add("cart-item-name");
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
            nameLabel.setOnMouseClicked(e -> {
                selectedCartItem = ci;
                populateCart();
                populateModifiersUi();
                showModifierDialog(ci);
                e.consume();
            });
            titleLine.getChildren().addAll(indicator, nameLabel);
            nameBox.getChildren().add(titleLine);

            if (!ci.getModifiers().isEmpty()) {
                Label modsLabel = new Label(String.join(", ", ci.getModifiers()));
                modsLabel.getStyleClass().add("cart-item-mods");
                nameBox.getChildren().add(modsLabel);
            }

            if (ci.getNotes() != null && !ci.getNotes().trim().isEmpty()) {
                Label notesLabel = new Label("Note: " + ci.getNotes());
                notesLabel.setStyle(
                        "-fx-font-size: 12px; -fx-text-fill: #EF4444; -fx-font-style: italic; -fx-font-weight: bold;");
                nameBox.getChildren().add(notesLabel);
            }

            // Quantity Control (width 100px)
            HBox qtyBox = new HBox();
            qtyBox.setPrefWidth(100);
            qtyBox.setMaxWidth(100);
            qtyBox.setAlignment(Pos.CENTER);
            qtyBox.setSpacing(5);

            Button minusBtn = new Button("-");
            minusBtn.getStyleClass().add("qty-btn");
            minusBtn.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);
            if (totalQty <= savedQty) {
                // Cannot decrease below saved quantity
                minusBtn.setDisable(true);
                minusBtn.setStyle("-fx-opacity: 0.3; -fx-cursor: default;");
            } else {
                minusBtn.setOnAction(e -> {
                    if (ci.getQuantity() > 1) {
                        ci.setQuantity(ci.getQuantity() - 1);
                    } else {
                        cartList.remove(ci);
                        if (selectedCartItem == ci) {
                            selectedCartItem = null;
                        }
                    }
                    updateCalculations();
                    populateCart();
                    populateModifiersUi();
                });
            }

            HBox qtyLabelBox = new HBox();
            qtyLabelBox.setAlignment(Pos.CENTER);
            qtyLabelBox.setSpacing(2);
            if (savedQty > 0 && newQty > 0) {
                Label savedLbl = new Label(String.valueOf(savedQty));
                savedLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
                Label plusSign = new Label("+");
                plusSign.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
                Label newLbl = new Label(String.valueOf(newQty));
                newLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
                qtyLabelBox.getChildren().addAll(savedLbl, plusSign, newLbl);
            } else if (savedQty > 0) {
                Label savedLbl = new Label(String.valueOf(savedQty));
                savedLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
                qtyLabelBox.getChildren().add(savedLbl);
            } else {
                Label newLbl = new Label(String.valueOf(totalQty));
                newLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #059669;");
                qtyLabelBox.getChildren().add(newLbl);
            }

            Button plusBtn = new Button("+");
            plusBtn.getStyleClass().add("qty-btn");
            plusBtn.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);
            plusBtn.setOnAction(e -> {
                ci.setQuantity(ci.getQuantity() + 1);
                updateCalculations();
                populateCart();
            });

            qtyBox.getChildren().addAll(minusBtn, qtyLabelBox, plusBtn);

            // Price Column (width 90px)
            Label priceLabel = new Label(String.format("₹%.2f", ci.getItem().getPrice().doubleValue()));
            priceLabel.setPrefWidth(90);
            priceLabel.setMaxWidth(90);
            priceLabel.setAlignment(Pos.CENTER_RIGHT);
            priceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

            // Amount & Delete (width 120px)
            HBox amtBox = new HBox();
            amtBox.setPrefWidth(120);
            amtBox.setMaxWidth(120);
            amtBox.setAlignment(Pos.CENTER_RIGHT);
            amtBox.setSpacing(6);

            double amt = ci.getItem().getPrice().doubleValue() * ci.getQuantity();
            Label amtLabel = new Label(String.format("₹%.2f", amt));
            amtLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

            Button trashBtn = new Button();
            trashBtn.getStyleClass().add("qty-btn");
            trashBtn.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

            javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
            trashIcon.setContent("M3 6h18 M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2");
            trashIcon.setFill(javafx.scene.paint.Color.TRANSPARENT);
            trashIcon.setStrokeWidth(1.5);
            trashIcon.setScaleX(0.7);
            trashIcon.setScaleY(0.7);
            trashBtn.setGraphic(trashIcon);

            if (savedQty > 0) {
                if (newQty > 0) {
                    // Clicking trash reverts the item back to its saved quantity (removing newly
                    // added ones)
                    trashIcon.setStroke(javafx.scene.paint.Color.web("#D97706")); // Amber color for revert
                    trashBtn.setStyle("-fx-background-color: #FFFBEB; -fx-text-fill: #D97706; -fx-cursor: hand;");
                    trashBtn.setOnAction(e -> {
                        ci.setQuantity(savedQty);
                        updateCalculations();
                        populateCart();
                        populateModifiersUi();
                    });
                } else {
                    // Already saved and no new additions to remove
                    trashIcon.setStroke(javafx.scene.paint.Color.web("#94A3B8")); // Muted gray
                    trashBtn.setStyle(
                            "-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-opacity: 0.3; -fx-cursor: default;");
                    trashBtn.setDisable(true);
                }
            } else {
                // Completely new item: normal delete behavior
                trashIcon.setStroke(javafx.scene.paint.Color.web("#EF4444")); // Red color
                trashBtn.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #EF4444; -fx-cursor: hand;");
                trashBtn.setOnAction(e -> {
                    cartList.remove(ci);
                    if (selectedCartItem == ci) {
                        selectedCartItem = null;
                    }
                    updateCalculations();
                    populateCart();
                    populateModifiersUi();
                });
            }

            amtBox.getChildren().addAll(amtLabel, trashBtn);

            row.getChildren().addAll(nameBox, qtyBox, priceLabel, amtBox);
            cartItemsContainer.getChildren().add(row);
        }
    }

    private void updateCalculations() {
        java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
        for (CartItem ci : cartList) {
            java.math.BigDecimal price = ci.getItem().getPrice();
            if (price == null)
                price = java.math.BigDecimal.ZERO;
            subtotal = subtotal.add(price.multiply(java.math.BigDecimal.valueOf(ci.getQuantity())));
        }

        java.math.BigDecimal discountAmt = java.math.BigDecimal.ZERO;
        if (discountValue > 0.0 && subtotal.compareTo(java.math.BigDecimal.ZERO) > 0) {
            if (isDiscountPercentage) {
                discountAmt = subtotal.multiply(java.math.BigDecimal.valueOf(discountValue / 100.0));
            } else {
                discountAmt = java.math.BigDecimal.valueOf(discountValue);
            }
            if (discountAmt.compareTo(subtotal) > 0) {
                discountAmt = subtotal;
            }
        }

        if (discountRow != null) {
            if (discountAmt.compareTo(java.math.BigDecimal.ZERO) > 0) {
                discountRow.setVisible(true);
                discountRow.setManaged(true);
                if (discountTitleLabel != null) {
                    if (isDiscountPercentage) {
                        discountTitleLabel.setText(String.format("Discount (%.0f%%):", discountValue));
                    } else {
                        discountTitleLabel.setText("Discount:");
                    }
                }
                if (discountValueLabel != null) {
                    discountValueLabel.setText(String.format("-₹%.2f", discountAmt.doubleValue()));
                }
            } else {
                discountRow.setVisible(false);
                discountRow.setManaged(false);
            }
        }

        java.math.BigDecimal taxableSubtotal = subtotal.subtract(discountAmt);
        java.math.BigDecimal taxRate = java.math.BigDecimal.valueOf(0.025); // 2.5%
        java.math.BigDecimal cgst = taxableSubtotal.multiply(taxRate);
        java.math.BigDecimal sgst = taxableSubtotal.multiply(taxRate);
        java.math.BigDecimal grandTotal = taxableSubtotal.add(cgst).add(sgst);

        if (cartSubtotalLabel != null)
            cartSubtotalLabel.setText(String.format("₹%.2f", subtotal.doubleValue()));
        if (cartCgstLabel != null)
            cartCgstLabel.setText(String.format("₹%.2f", cgst.doubleValue()));
        if (cartSgstLabel != null)
            cartSgstLabel.setText(String.format("₹%.2f", sgst.doubleValue()));
        if (cartGrandTotalLabel != null)
            cartGrandTotalLabel.setText(String.format("₹%.2f", grandTotal.doubleValue()));

        // Calculate Change amount using BigDecimal
        java.math.BigDecimal received = java.math.BigDecimal.ZERO;
        if (receivedAmountField != null) {
            try {
                String text = receivedAmountField.getText().trim();
                if (!text.isEmpty()) {
                    received = new java.math.BigDecimal(text);
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        java.math.BigDecimal change = received.subtract(grandTotal);
        if (change.compareTo(java.math.BigDecimal.ZERO) < 0) {
            change = java.math.BigDecimal.ZERO;
        }

        if (changeAmountLabel != null) {
            changeAmountLabel.setText(String.format("₹%.2f", change.doubleValue()));
        }
    }

    // --- PAYMENT ACTION HANDLERS ---
    private void setPaymentMode(String mode, Button activeBtn) {
        selectedPaymentMode = mode;
        payCashBtn.getStyleClass().remove("active");
        payCardBtn.getStyleClass().remove("active");
        payUpiBtn.getStyleClass().remove("active");
        payOtherBtn.getStyleClass().remove("active");
        paySplitBtn.getStyleClass().remove("active");

        payCashBtn.setStyle("");
        payCardBtn.setStyle("");
        payUpiBtn.setStyle("");
        payOtherBtn.setStyle("");
        paySplitBtn.setStyle("");

        activeBtn.getStyleClass().add("active");

        if (receivedAmountField != null) {
            if ("CASH".equals(mode)) {
                receivedAmountField.setDisable(false);
            } else {
                receivedAmountField.setDisable(true);
                receivedAmountField.setText("0.00");
            }
        }
        updateCalculations();
    }

    @FXML
    public void handlePayCash() {
        setPaymentMode("CASH", payCashBtn);
    }

    @FXML
    public void handlePayCard() {
        setPaymentMode("CARD", payCardBtn);
    }

    @FXML
    public void handlePayUpi() {
        setPaymentMode("UPI", payUpiBtn);
    }

    @FXML
    public void handlePayOther() {
        setPaymentMode("OTHER", payOtherBtn);
    }

    @FXML
    public void handlePaySplit() {
        setPaymentMode("SPLIT", paySplitBtn);
    }

    @FXML
    public void handleDiscountDialog() {
        if (cartList.isEmpty()) {
            showAlert("Cart is Empty", "Cannot add discount to an empty cart.");
            return;
        }

        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Apply Discount");
        dialog.setHeaderText("Choose discount type and enter the value:");

        javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialogPane.setStyle("-fx-background-color: #FFFFFF;");

        javafx.scene.control.Button okBtn = (javafx.scene.control.Button) dialogPane
                .lookupButton(javafx.scene.control.ButtonType.OK);
        javafx.scene.control.Button cancelBtn = (javafx.scene.control.Button) dialogPane
                .lookupButton(javafx.scene.control.ButtonType.CANCEL);
        if (okBtn != null) {
            okBtn.setStyle(
                    "-fx-background-color: #0A4F34; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;");
        }
        if (cancelBtn != null) {
            cancelBtn.setStyle(
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 6px;");
        }

        VBox vbox = new VBox(12);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-pref-width: 320px; -fx-background-color: #FFFFFF;");

        Label typeHeader = new Label("Discount Type");
        typeHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        ToggleGroup group = new ToggleGroup();
        RadioButton percentRadio = new RadioButton("Percentage (%)");
        percentRadio.setToggleGroup(group);
        percentRadio.setSelected(isDiscountPercentage);
        percentRadio.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        RadioButton amountRadio = new RadioButton("Fixed Amount (₹)");
        amountRadio.setToggleGroup(group);
        amountRadio.setSelected(!isDiscountPercentage);
        amountRadio.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        HBox radioBox = new HBox(15, percentRadio, amountRadio);

        Label valueHeader = new Label("Discount Value");
        valueHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        javafx.scene.control.TextField valueField = new javafx.scene.control.TextField(
                discountValue > 0.0 ? String.format("%.2f", discountValue) : "");
        valueField.setPromptText("e.g. 10 for 10% or 50 for ₹50");
        valueField.setStyle(
                "-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8; -fx-font-size: 13px;");

        javafx.scene.control.Button removeBtn = new javafx.scene.control.Button("Remove Discount");
        removeBtn.setStyle(
                "-fx-background-color: #FEF2F2; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 6 12;");
        removeBtn.setOnAction(e -> {
            discountValue = 0.0;
            isDiscountPercentage = false;
            dialog.close();
            updateCalculations();
        });

        vbox.getChildren().addAll(typeHeader, radioBox, new javafx.scene.control.Separator(), valueHeader, valueField);
        if (discountValue > 0.0) {
            vbox.getChildren().addAll(new javafx.scene.control.Separator(), removeBtn);
        }
        dialogPane.setContent(vbox);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    double val = Double.parseDouble(valueField.getText().trim());
                    if (val < 0.0) {
                        showAlert("Invalid Discount", "Discount value cannot be negative.");
                        return;
                    }
                    discountValue = val;
                    isDiscountPercentage = percentRadio.isSelected();
                    updateCalculations();
                } catch (NumberFormatException e) {
                    if (!valueField.getText().trim().isEmpty()) {
                        showAlert("Invalid Number", "Please enter a valid numeric value.");
                    }
                }
            }
        });
    }

    // --- POS ACTION BUTTON HANDLERS ---
    @FXML
    public void handleCancelOrder() {
        if (currentActiveOrder != null) {
            String orderNumStr = currentActiveOrder.getOrderNumber() != null ? currentActiveOrder.getOrderNumber()
                    : "#" + currentActiveOrder.getId().toString().substring(0, 4);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Cancel Order");
            dialog.setHeaderText("Specify the reason for cancelling order: " + orderNumStr);

            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialogPane.setStyle("-fx-background-color: #FFFFFF;");

            Button okBtn = (Button) dialogPane.lookupButton(ButtonType.OK);
            okBtn.setText("Cancel Order");
            okBtn.setDisable(true);
            okBtn.setStyle(
                    "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-opacity: 0.5;");

            Button cancelBtn = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
            cancelBtn.setText("Go Back");
            cancelBtn.setStyle(
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 6px;");

            TextArea reasonInput = new TextArea();
            reasonInput.setPromptText("Enter reason for cancellation here...");
            reasonInput.setPrefRowCount(3);
            reasonInput.setWrapText(true);
            reasonInput.setStyle(
                    "-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 6 8; -fx-font-size: 14px;");

            reasonInput.textProperty().addListener((observable, oldValue, newValue) -> {
                boolean isBlank = newValue == null || newValue.trim().isEmpty();
                okBtn.setDisable(isBlank);
                if (isBlank) {
                    okBtn.setStyle(
                            "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-opacity: 0.5;");
                } else {
                    okBtn.setStyle(
                            "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-opacity: 1.0; -fx-cursor: hand;");
                }
            });

            VBox vbox = new VBox(10);
            vbox.setPadding(new javafx.geometry.Insets(10));
            vbox.getChildren().addAll(reasonInput);
            dialogPane.setContent(vbox);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String reason = reasonInput.getText().trim();

                try {
                    // Update order in DB
                    currentActiveOrder.setStatus(OrderStatus.CANCELLED);
                    currentActiveOrder.setCancelReason(reason);
                    orderRepository.save(currentActiveOrder);

                    // Cancel all KOTs associated with the order
                    List<KOT> kots = kotRepository.findByOrderId(currentActiveOrder.getId());
                    for (KOT kot : kots) {
                        kot.setOverallStatus(KOTStatus.CANCELLED);
                        for (KOTItem ki : kot.getItems()) {
                            ki.setItemStatus(KOTStatus.CANCELLED);
                        }
                        KOT savedKot = kotRepository.save(kot);

                        // Broadcast to KDS via WebSockets
                        if (messagingTemplate != null) {
                            try {
                                String kitchenTopic = "/topic/kitchen/" + currentActiveOrder.getRestaurantId();
                                messagingTemplate.convertAndSend(kitchenTopic, savedKot);
                            } catch (Exception wsEx) {
                                System.err.println("❌ Failed to broadcast cancelled KOT: " + wsEx.getMessage());
                            }
                        }
                    }

                    // Refresh KDS local view
                    if (kdsNativeController != null) {
                        kdsNativeController.refreshKdsData();
                    }

                    // Free up dining table if applicable
                    if (selectedOrderType == OrderType.DINE_IN && currentDiningTable != null) {
                        currentDiningTable.setStatus(TableStatus.AVAILABLE);
                        currentDiningTable.setTotalAmount(0.0);
                        currentDiningTable.setDurationMinutes(0);
                        tableRepository.save(currentDiningTable);

                        // Broadcast table status update
                        if (messagingTemplate != null) {
                            try {
                                String tableTopic = "/topic/tables/" + currentActiveOrder.getRestaurantId();
                                java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
                                wsPayload.put("id", currentDiningTable.getId().toString());
                                wsPayload.put("status", TableStatus.AVAILABLE.name());
                                wsPayload.put("totalAmount", 0.0);
                                wsPayload.put("durationMinutes", 0);
                                messagingTemplate.convertAndSend(tableTopic, wsPayload);

                                // Cascade to all merged tables if present
                                if (currentActiveOrder.getMergedTableIds() != null
                                        && !currentActiveOrder.getMergedTableIds().trim().isEmpty()) {
                                    for (String idStr : currentActiveOrder.getMergedTableIds().split(",")) {
                                        UUID otherId = UUID.fromString(idStr.trim());
                                        if (!otherId.equals(currentDiningTable.getId())) {
                                            DiningTable otherTable = tableRepository.findById(otherId).orElse(null);
                                            if (otherTable != null) {
                                                otherTable.setStatus(TableStatus.AVAILABLE);
                                                otherTable.setTotalAmount(0.0);
                                                otherTable.setDurationMinutes(0);
                                                tableRepository.save(otherTable);

                                                java.util.Map<String, Object> otherPayload = new java.util.HashMap<>();
                                                otherPayload.put("id", otherTable.getId().toString());
                                                otherPayload.put("status", TableStatus.AVAILABLE.name());
                                                otherPayload.put("totalAmount", 0.0);
                                                otherPayload.put("durationMinutes", 0);
                                                messagingTemplate.convertAndSend(tableTopic, otherPayload);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception wsEx) {
                                System.err.println("❌ Failed to broadcast table status: " + wsEx.getMessage());
                            }
                        }
                    }

                    showAlert("Order Cancelled", "Order " + orderNumStr + " has been successfully cancelled.");

                    resetBillingSessionState();

                    // Automatically return to dashboard after cancellation
                    showHomeView();

                } catch (Exception ex) {
                    showAlert("Cancellation Error", "Could not cancel order: " + ex.getMessage());
                }
            }
        } else {
            resetBillingSessionState();

            // Automatically return to dashboard
            showHomeView();
        }
    }

    @FXML
    public void handleCreateNewOrder() {
        boolean hasUnsaved = false;
        for (CartItem ci : cartList) {
            if (ci.getQuantity() > ci.getSavedQuantity()) {
                hasUnsaved = true;
                break;
            }
        }

        if (hasUnsaved) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Discard");
            confirmAlert.setHeaderText("Discard Unsaved Changes?");
            confirmAlert.setContentText(
                    "You have unsaved items in your cart. Are you sure you want to discard them and create a new order?");
            java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        resetBillingSessionState();
        handleTabSelection(OrderType.DINE_IN);
        showBillingView();
    }

    @FXML
    public void handleSaveOrder() {
        if (cartList.isEmpty()) {
            showAlert("Cart is Empty", "Please add items to cart before saving.");
            return;
        }

        if (selectedOrderType == OrderType.DINE_IN) {
            if (currentDiningTable == null) {
                showAlert("No Table Selected", "Please select a dining table first.");
                return;
            }

            try {
                // Find or create the active order for the table
                UUID restaurantId = TenantContext.getRestaurantId();
                var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                        java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));
                Order activeOrder = activeOrders.stream()
                        .filter(o -> o.getTableId() != null && o.getTableId().equals(currentDiningTable.getId()))
                        .findFirst()
                        .orElse(null);

                // Fetch existing KOT items to compute the delta (new items to send to kitchen).
                // Key = menuItemId + "|" + specialInstruction so that two lines for the same
                // dish but different customisations are tracked independently.
                java.util.Map<String, Integer> savedQuantities = new java.util.HashMap<>();
                if (activeOrder != null) {
                    List<KOT> kots = kotRepository.findByOrderId(activeOrder.getId());
                    for (KOT kot : kots) {
                        for (KOTItem item : kot.getItems()) {
                            if (item.getItemStatus() == KOTStatus.CANCELLED)
                                continue;
                            String key = item.getMenuItemId().toString() + "|" +
                                    (item.getSpecialInstruction() != null ? item.getSpecialInstruction().trim() : "");
                            savedQuantities.put(key, savedQuantities.getOrDefault(key, 0) + item.getQuantity());
                        }
                    }
                }

                // Compute the delta per (item + customisation) line
                List<com.smartdine.dto.KOTItemRequest> deltaItems = new ArrayList<>();
                for (CartItem ci : cartList) {
                    String specialInstruction = ci.getNotes() != null ? ci.getNotes() : "";
                    if (!ci.getModifiers().isEmpty()) {
                        String modsString = String.join(", ", ci.getModifiers());
                        specialInstruction = specialInstruction.isEmpty() ? modsString
                                : specialInstruction + " (" + modsString + ")";
                    }
                    String key = ci.getItem().getId().toString() + "|" + specialInstruction.trim();
                    int savedQty = savedQuantities.getOrDefault(key, 0);
                    int deltaQty = ci.getQuantity() - savedQty;
                    if (deltaQty > 0) {
                        com.smartdine.dto.KOTItemRequest reqItem = new com.smartdine.dto.KOTItemRequest();
                        reqItem.setMenuItemId(ci.getItem().getId());
                        reqItem.setQuantity(deltaQty);
                        reqItem.setSpecialInstruction(specialInstruction);
                        deltaItems.add(reqItem);
                    }
                }

                if (deltaItems.isEmpty()) {
                    showAlert("No New Items", "All items in the cart are already sent to the kitchen.");
                    return;
                }

                com.smartdine.dto.OrderRequest orderRequest = new com.smartdine.dto.OrderRequest();
                orderRequest.setTableId(currentDiningTable.getId());
                orderRequest.setItems(deltaItems);
                orderRequest.setNotes(currentCustomerNotes != null ? currentCustomerNotes : "");

                // Process the KOT via OrderService
                KOT kot = orderService.processNewKOT(orderRequest);

                // Update newly created order with customer CRM details
                try {
                    Order newlyCreatedOrder = orderRepository.findById(kot.getOrderId()).orElse(null);
                    if (newlyCreatedOrder != null) {
                        newlyCreatedOrder.setCustomerPhone(currentCustomerPhone);
                        newlyCreatedOrder.setCustomerName(currentCustomerName);
                        if (currentCustomerNotes != null && !currentCustomerNotes.isEmpty()) {
                            newlyCreatedOrder.setNotes(currentCustomerNotes);
                        }
                        // Revert status to OPEN since new items were added after pre-billing
                        if (newlyCreatedOrder.getStatus() == OrderStatus.BILLED) {
                            newlyCreatedOrder.setStatus(OrderStatus.OPEN);
                        }
                        orderRepository.save(newlyCreatedOrder);
                        currentActiveOrder = newlyCreatedOrder;
                    }
                } catch (Exception ex) {
                    System.out.println("Error saving customer info to newly created order: " + ex.getMessage());
                }

                // Reload the table data to sync UI
                currentDiningTable = tableRepository.findById(currentDiningTable.getId()).orElse(currentDiningTable);
                // Revert table status to RUNNING since new items were added after pre-billing
                if (currentDiningTable.getStatus() == TableStatus.PAYMENT_PENDING) {
                    currentDiningTable.setStatus(TableStatus.RUNNING);
                    tableRepository.save(currentDiningTable);

                    // Cascade to other merged tables
                    if (currentActiveOrder != null && currentActiveOrder.getMergedTableIds() != null
                            && !currentActiveOrder.getMergedTableIds().trim().isEmpty()) {
                        for (String idStr : currentActiveOrder.getMergedTableIds().split(",")) {
                            UUID otherId = UUID.fromString(idStr.trim());
                            if (!otherId.equals(currentDiningTable.getId())) {
                                DiningTable otherTable = tableRepository.findById(otherId).orElse(null);
                                if (otherTable != null) {
                                    otherTable.setStatus(TableStatus.RUNNING);
                                    tableRepository.save(otherTable);
                                }
                            }
                        }
                    }
                }
                loadTablesToUi();

                resetBillingSessionState();
                showHomeView();

                showAlert("KOT Saved & Sent", "KOT " + kot.getKotNumber() + " sent to kitchen!");
            } catch (Exception e) {
                showAlert("Error Saving Order", "Failed to save KOT order: " + e.getMessage());
            }
        } else {
            // For Delivery and Pick Up, save order session directly
            handleSaveDeliveryOrPickupOrder();
        }
    }

    private void handleSaveDeliveryOrPickupOrder() {
        if (selectedOrderType == OrderType.DELIVERY) {
            String phone = (deliveryPhoneField != null) ? deliveryPhoneField.getText().trim() : "";
            String name = (deliveryNameField != null) ? deliveryNameField.getText().trim() : "";
            String address = (deliveryAddressField != null) ? deliveryAddressField.getText().trim() : "";

            if (phone.isEmpty() || name.isEmpty() || address.isEmpty()) {
                showAlert("Missing Customer Details", "Please fill in Phone, Name, and Address for Delivery.");
                return;
            }
            if (phone.length() != 10 || !phone.matches("\\d+")) {
                showAlert("Invalid Phone Number", "Please enter a valid 10-digit phone number.");
                return;
            }
        } else if (selectedOrderType == OrderType.PICK_UP) {
            String phone = (pickupPhoneField != null) ? pickupPhoneField.getText().trim() : "";
            if (!phone.isEmpty()) {
                if (phone.length() != 10 || !phone.matches("\\d+")) {
                    showAlert("Invalid Phone Number", "Please enter a valid 10-digit phone number.");
                    return;
                }
            }
        }

        try {
            java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
            for (CartItem ci : cartList) {
                subtotal = subtotal
                        .add(ci.getItem().getPrice().multiply(java.math.BigDecimal.valueOf(ci.getQuantity())));
            }

            java.math.BigDecimal discountAmt = java.math.BigDecimal.ZERO;
            if (discountValue > 0.0 && subtotal.compareTo(java.math.BigDecimal.ZERO) > 0) {
                if (isDiscountPercentage) {
                    discountAmt = subtotal.multiply(java.math.BigDecimal.valueOf(discountValue / 100.0));
                } else {
                    discountAmt = java.math.BigDecimal.valueOf(discountValue);
                }
            }
            java.math.BigDecimal taxableSubtotal = subtotal.subtract(discountAmt);
            java.math.BigDecimal taxRate = java.math.BigDecimal.valueOf(0.025);
            java.math.BigDecimal cgst = taxableSubtotal.multiply(taxRate);
            java.math.BigDecimal sgst = taxableSubtotal.multiply(taxRate);
            java.math.BigDecimal grandTotalVal = taxableSubtotal.add(cgst).add(sgst);

            Order order;
            boolean isNew = true;
            if (currentActiveOrder != null && currentActiveOrder.getType() != OrderType.DINE_IN) {
                order = currentActiveOrder;
                isNew = false;
            } else {
                order = new Order();
                order.setRestaurantId(TenantContext.getRestaurantId());
                order.setStatus(OrderStatus.OPEN);
                order.setType(selectedOrderType);
                order.setSource("DIRECT");
                order.setTableId(null);
                order.setTableName(null);
                order.setOrderNumber("#" + (System.currentTimeMillis() % 10000));
            }

            if (selectedOrderType == OrderType.DELIVERY) {
                order.setCustomerPhone(deliveryPhoneField.getText().trim());
                order.setCustomerName(deliveryNameField.getText().trim());
                order.setTableName("Delivery: " + deliveryAddressField.getText().trim());
            } else {
                order.setCustomerPhone(pickupPhoneField.getText().trim());
                String pName = pickupNameField.getText().trim();
                order.setCustomerName(pName.isEmpty() ? "Walk-In Pickup" : pName);
                order.setTableName("Pickup");
            }

            order.setSubTotal(subtotal);
            order.setCgst(cgst);
            order.setSgst(sgst);
            order.setDiscount(discountAmt);
            order.setGrandTotal(grandTotalVal);
            if (isNew) {
                order.setStartedAt(LocalDateTime.now());
            }
            // Revert status to OPEN since new items were added after pre-billing
            if (order.getStatus() == OrderStatus.BILLED) {
                order.setStatus(OrderStatus.OPEN);
            }

            Order savedOrder = orderRepository.save(order);

            // Fetch existing KOT items to compute the delta.
            // Use composite key (menuItemId + "|" + specialInstruction) so that
            // two lines for the same dish with different customisations are
            // treated as independent when computing what is new.
            java.util.Map<String, Integer> savedQuantities = new java.util.HashMap<>();
            if (!isNew) {
                List<KOT> kots = kotRepository.findByOrderId(savedOrder.getId());
                for (KOT kot : kots) {
                    for (KOTItem item : kot.getItems()) {
                        if (item.getItemStatus() == KOTStatus.CANCELLED)
                            continue;
                        String key = item.getMenuItemId().toString() + "|" +
                                (item.getSpecialInstruction() != null ? item.getSpecialInstruction().trim() : "");
                        savedQuantities.put(key, savedQuantities.getOrDefault(key, 0) + item.getQuantity());
                    }
                }
            }

            // Compute delta items per (item + customisation) line
            List<KOTItem> deltaKOTItems = new ArrayList<>();
            for (CartItem ci : cartList) {
                String specialInstruction = ci.getNotes() != null ? ci.getNotes() : "";
                if (ci.getModifiers() != null && !ci.getModifiers().isEmpty()) {
                    String modsString = String.join(", ", ci.getModifiers());
                    specialInstruction = specialInstruction.isEmpty() ? modsString
                            : specialInstruction + " (" + modsString + ")";
                }
                String key = ci.getItem().getId().toString() + "|" + specialInstruction.trim();
                int savedQty = savedQuantities.getOrDefault(key, 0);
                int deltaQty = ci.getQuantity() - savedQty;
                if (deltaQty > 0) {
                    KOTItem ki = new KOTItem();
                    ki.setRestaurantId(savedOrder.getRestaurantId());
                    ki.setMenuItemId(ci.getItem().getId());
                    ki.setItemName(ci.getItem().getName());
                    ki.setQuantity(deltaQty);
                    ki.setSpecialInstruction(specialInstruction);
                    ki.setItemStatus(KOTStatus.PENDING);
                    deltaKOTItems.add(ki);
                }
            }

            if (!deltaKOTItems.isEmpty()) {
                KOT kot = new KOT();
                kot.setRestaurantId(savedOrder.getRestaurantId());
                kot.setOrderId(savedOrder.getId());
                kot.setTableId(UUID.randomUUID()); // Dummy non-null tableId for DB constraint
                kot.setTableName(savedOrder.getTableName());
                kot.setKotNumber("KOT-" + (System.currentTimeMillis() % 100000));
                kot.setOverallStatus(KOTStatus.PENDING);
                kot.setNotes("");
                kot.setItems(deltaKOTItems);
                KOT savedKot = kotRepository.save(kot);

                // Broadcast to WebSockets for Flutter KDS real-time update
                if (messagingTemplate != null) {
                    try {
                        String kitchenTopic = "/topic/kitchen/" + savedOrder.getRestaurantId();
                        messagingTemplate.convertAndSend(kitchenTopic, savedKot);
                        System.out.println("📢 WebSocket Delivery/Pickup KOT Broadcast sent to topic: " + kitchenTopic);
                    } catch (Exception wsEx) {
                        System.err.println("❌ Failed to broadcast Delivery/Pickup KOT: " + wsEx.getMessage());
                    }
                }
            }

            // Sync with local JavaFX KDS view
            if (kdsNativeController != null) {
                kdsNativeController.refreshKdsData();
            }

            resetBillingSessionState();
            showHomeView();

            showAlert("Order Saved", selectedOrderType.name() + " order saved & sent to kitchen!");
        } catch (Exception e) {
            showAlert("Error Saving Order", "Failed to save order: " + e.getMessage());
        }
    }

    @FXML
    public void handleSaveAndPrint() {
        if (cartList.isEmpty()) {
            showAlert("Cart is Empty", "Please add items to cart before saving.");
            return;
        }
        if (selectedOrderType == OrderType.DINE_IN && currentDiningTable == null) {
            showAlert("No Table Selected", "Please select a dining table first.");
            return;
        }

        handleSaveOrder();

        if (currentActiveOrder != null) {
            triggerThermalReceiptPrinting(currentActiveOrder);
        } else {
            showAlert("Receipt Printed", "Receipt sent to printer!");
        }
    }

    @FXML
    public void handleSettleAndSave() {
        if (cartList.isEmpty()) {
            showAlert("Cart is Empty", "Please add some items to the cart before settling.");
            return;
        }

        // Validate customer details for Delivery and Takeaway
        if (selectedOrderType == OrderType.DELIVERY) {
            String phone = (deliveryPhoneField != null) ? deliveryPhoneField.getText().trim() : "";
            String name = (deliveryNameField != null) ? deliveryNameField.getText().trim() : "";
            String address = (deliveryAddressField != null) ? deliveryAddressField.getText().trim() : "";

            if (phone.isEmpty() || name.isEmpty() || address.isEmpty()) {
                showAlert("Missing Customer Details", "Please fill in Phone, Name, and Address for Delivery.");
                return;
            }
            if (phone.length() != 10 || !phone.matches("\\d+")) {
                showAlert("Invalid Phone Number", "Please enter a valid 10-digit phone number.");
                return;
            }
        } else if (selectedOrderType == OrderType.PICK_UP) {
            String phone = (pickupPhoneField != null) ? pickupPhoneField.getText().trim() : "";
            if (!phone.isEmpty()) {
                if (phone.length() != 10 || !phone.matches("\\d+")) {
                    showAlert("Invalid Phone Number", "Please enter a valid 10-digit phone number.");
                    return;
                }
            }
        }

        // Verify if any items are still preparing in the kitchen to avoid miss-billing
        UUID orderIdToCheck = null;
        if (currentActiveOrder != null) {
            orderIdToCheck = currentActiveOrder.getId();
        } else if (selectedOrderType == OrderType.DINE_IN && currentDiningTable != null) {
            UUID restaurantId = TenantContext.getRestaurantId();
            var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                    java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));
            Order tableOrder = activeOrders.stream()
                    .filter(o -> o.getTableId() != null && o.getTableId().equals(currentDiningTable.getId()))
                    .findFirst()
                    .orElse(null);
            if (tableOrder != null) {
                orderIdToCheck = tableOrder.getId();
            }
        }

        if (orderIdToCheck != null) {
            List<KOT> kots = kotRepository.findByOrderId(orderIdToCheck);
            if (kots != null) {
                for (KOT kot : kots) {
                    if (kot.getItems() != null) {
                        for (KOTItem item : kot.getItems()) {
                            if (item.getItemStatus() == KOTStatus.PENDING
                                    || item.getItemStatus() == KOTStatus.PREPARING) {
                                showAlert("Cannot Settle Order", "Food is still being prepared: '" + item.getItemName()
                                        + "' is " + item.getItemStatus().name().toLowerCase() + " in the kitchen.");
                                return;
                            }
                        }
                    }
                }
            }
        }

        if (currentActiveOrder != null && currentActiveOrder.getStatus() == OrderStatus.OPEN) {

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Warning: Order Not Billed");
            if (selectedOrderType == OrderType.DINE_IN && currentDiningTable != null) {
                confirmAlert.setHeaderText(
                        "Please bill the order for Table " + currentDiningTable.getTableNumber() + " first.");
                confirmAlert.setContentText(
                        "This order is still in OPEN status and has not been billed yet. Clicking OK will bill the order (lock items, print bill, and set table to Payment Pending) to avoid accidental direct settlement.");
            } else {
                confirmAlert.setHeaderText("Please bill this " + selectedOrderType.name() + " order first.");
                confirmAlert.setContentText(
                        "This order is still in OPEN status and has not been billed yet. Clicking OK will bill the order (lock items, print bill) to avoid accidental direct settlement.");
            }

            javafx.scene.control.DialogPane dialogPane = confirmAlert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #FFFFFF;");
            Button okBtn = (Button) dialogPane.lookupButton(ButtonType.OK);
            Button cancelBtn = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
            if (okBtn != null) {
                okBtn.setStyle(
                        "-fx-background-color: #D97706; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;");
            }
            if (cancelBtn != null) {
                cancelBtn.setStyle(
                        "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 6px;");
            }

            Optional<ButtonType> confirmResult = confirmAlert.showAndWait();
            if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) {
                return;
            }

            try {
                currentActiveOrder.setStatus(OrderStatus.BILLED);

                java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
                for (CartItem ci : cartList) {
                    subtotal = subtotal
                            .add(ci.getItem().getPrice().multiply(java.math.BigDecimal.valueOf(ci.getQuantity())));
                }

                java.math.BigDecimal discountAmt = java.math.BigDecimal.ZERO;
                if (discountValue > 0.0 && subtotal.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    if (isDiscountPercentage) {
                        discountAmt = subtotal.multiply(java.math.BigDecimal.valueOf(discountValue / 100.0));
                    } else {
                        discountAmt = java.math.BigDecimal.valueOf(discountValue);
                    }
                    if (discountAmt.compareTo(subtotal) > 0) {
                        discountAmt = subtotal;
                    }
                }

                java.math.BigDecimal taxableSubtotal = subtotal.subtract(discountAmt);
                java.math.BigDecimal taxRate = java.math.BigDecimal.valueOf(0.025);
                java.math.BigDecimal cgst = taxableSubtotal.multiply(taxRate);
                java.math.BigDecimal sgst = taxableSubtotal.multiply(taxRate);
                java.math.BigDecimal grandTotalVal = taxableSubtotal.add(cgst).add(sgst);

                currentActiveOrder.setSubTotal(subtotal);
                currentActiveOrder.setCgst(cgst);
                currentActiveOrder.setSgst(sgst);
                currentActiveOrder.setDiscount(discountAmt);
                currentActiveOrder.setGrandTotal(grandTotalVal);

                orderRepository.save(currentActiveOrder);

                // Save delta KOT items for Delivery/Takeaway (non-Dine-In) when billing
                if (selectedOrderType != OrderType.DINE_IN) {
                    java.util.Map<String, Integer> savedQuantities = new java.util.HashMap<>();
                    List<KOT> kots = kotRepository.findByOrderId(currentActiveOrder.getId());
                    for (KOT kot : kots) {
                        for (KOTItem item : kot.getItems()) {
                            if (item.getItemStatus() == KOTStatus.CANCELLED)
                                continue;
                            String key = item.getMenuItemId().toString() + "|" +
                                    (item.getSpecialInstruction() != null ? item.getSpecialInstruction().trim() : "");
                            savedQuantities.put(key, savedQuantities.getOrDefault(key, 0) + item.getQuantity());
                        }
                    }

                    List<KOTItem> deltaKOTItems = new ArrayList<>();
                    for (CartItem ci : cartList) {
                        String specialInstruction = ci.getNotes() != null ? ci.getNotes() : "";
                        if (ci.getModifiers() != null && !ci.getModifiers().isEmpty()) {
                            String modsString = String.join(", ", ci.getModifiers());
                            specialInstruction = specialInstruction.isEmpty() ? modsString
                                    : specialInstruction + " (" + modsString + ")";
                        }
                        String key = ci.getItem().getId().toString() + "|" + specialInstruction.trim();
                        int savedQty = savedQuantities.getOrDefault(key, 0);
                        int deltaQty = ci.getQuantity() - savedQty;
                        if (deltaQty > 0) {
                            KOTItem ki = new KOTItem();
                            ki.setRestaurantId(currentActiveOrder.getRestaurantId());
                            ki.setMenuItemId(ci.getItem().getId());
                            ki.setItemName(ci.getItem().getName());
                            ki.setQuantity(deltaQty);
                            ki.setSpecialInstruction(specialInstruction);
                            ki.setItemStatus(KOTStatus.PENDING);
                            deltaKOTItems.add(ki);
                        }
                    }

                    if (!deltaKOTItems.isEmpty()) {
                        KOT kot = new KOT();
                        kot.setRestaurantId(currentActiveOrder.getRestaurantId());
                        kot.setOrderId(currentActiveOrder.getId());
                        kot.setTableId(UUID.randomUUID());
                        kot.setTableName(currentActiveOrder.getTableName());
                        kot.setKotNumber("KOT-" + (System.currentTimeMillis() % 100000));
                        kot.setOverallStatus(KOTStatus.PENDING);
                        kot.setNotes("");
                        kot.setItems(deltaKOTItems);
                        KOT savedKot = kotRepository.save(kot);

                        if (messagingTemplate != null) {
                            try {
                                String kitchenTopic = "/topic/kitchen/" + currentActiveOrder.getRestaurantId();
                                messagingTemplate.convertAndSend(kitchenTopic, savedKot);
                            } catch (Exception wsEx) {
                                System.err.println("❌ Failed to broadcast KOT: " + wsEx.getMessage());
                            }
                        }
                    }
                }

                if (selectedOrderType == OrderType.DINE_IN && currentDiningTable != null) {
                    currentDiningTable.setStatus(TableStatus.PAYMENT_PENDING);
                    tableRepository.save(currentDiningTable);

                    // Broadcast table status update
                    if (messagingTemplate != null) {
                        try {
                            String tableTopic = "/topic/tables/" + currentActiveOrder.getRestaurantId();
                            java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
                            wsPayload.put("id", currentDiningTable.getId().toString());
                            wsPayload.put("status", TableStatus.PAYMENT_PENDING.name());
                            wsPayload.put("totalAmount", currentDiningTable.getTotalAmount());
                            if (currentActiveOrder.getStartedAt() != null) {
                                wsPayload.put("durationMinutes", (int) java.time.Duration
                                        .between(currentActiveOrder.getStartedAt(), java.time.LocalDateTime.now())
                                        .toMinutes());
                            } else {
                                wsPayload.put("durationMinutes", 0);
                            }
                            messagingTemplate.convertAndSend(tableTopic, wsPayload);

                            // Cascade to all merged tables if present
                            if (currentActiveOrder.getMergedTableIds() != null
                                    && !currentActiveOrder.getMergedTableIds().trim().isEmpty()) {
                                for (String idStr : currentActiveOrder.getMergedTableIds().split(",")) {
                                    UUID otherId = UUID.fromString(idStr.trim());
                                    if (!otherId.equals(currentDiningTable.getId())) {
                                        DiningTable otherTable = tableRepository.findById(otherId).orElse(null);
                                        if (otherTable != null) {
                                            otherTable.setStatus(TableStatus.PAYMENT_PENDING);
                                            otherTable.setTotalAmount(currentDiningTable.getTotalAmount());
                                            tableRepository.save(otherTable);

                                            java.util.Map<String, Object> otherPayload = new java.util.HashMap<>();
                                            otherPayload.put("id", otherTable.getId().toString());
                                            otherPayload.put("status", TableStatus.PAYMENT_PENDING.name());
                                            otherPayload.put("totalAmount", currentDiningTable.getTotalAmount());
                                            if (currentActiveOrder.getStartedAt() != null) {
                                                otherPayload
                                                        .put("durationMinutes",
                                                                (int) java.time.Duration
                                                                        .between(currentActiveOrder.getStartedAt(),
                                                                                java.time.LocalDateTime.now())
                                                                        .toMinutes());
                                            } else {
                                                otherPayload.put("durationMinutes", 0);
                                            }
                                            messagingTemplate.convertAndSend(tableTopic, otherPayload);
                                        }
                                    }
                                }
                            }
                        } catch (Exception wsEx) {
                            System.err.println("❌ Failed to broadcast table status: " + wsEx.getMessage());
                        }
                    }
                }

                triggerThermalReceiptPrinting(currentActiveOrder);

                // Mark all cart items as saved so the colour coding updates correctly
                for (CartItem ci : cartList) {
                    ci.setSavedQuantity(ci.getQuantity());
                }

                String orderNum = currentActiveOrder.getOrderNumber();
                showAlert("Order Billed", "Order " + orderNum + " billed successfully.");

                updateBillingPageControlState();
                loadTablesToUi();

            } catch (Exception e) {
                showAlert("Error Billing Order", "Could not bill order: " + e.getMessage());
            }
            return;
        }

        try {
            // Validate that a payment mode is selected before settling
            if (selectedPaymentMode == null || selectedPaymentMode.trim().isEmpty() || "NONE".equalsIgnoreCase(selectedPaymentMode)) {
                showAlert("Payment Mode Required", "Please select a payment mode (Cash, Card, UPI, etc.) before settling.");
                return;
            }

            Alert settleConfirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            settleConfirmAlert.setTitle("Confirm Settlement");
            if (selectedOrderType == OrderType.DINE_IN && currentDiningTable != null) {
                settleConfirmAlert.setHeaderText(
                        "Settle and close payment for Table " + currentDiningTable.getTableNumber() + "?");
                settleConfirmAlert
                        .setContentText("This will mark the order as PAID and set the table status back to Available.");
            } else {
                settleConfirmAlert.setHeaderText("Settle and close this " + selectedOrderType.name() + " order?");
                settleConfirmAlert
                        .setContentText("This will mark the order as PAID (Settled) and close the billing session.");
            }

            javafx.scene.control.DialogPane dialogPane = settleConfirmAlert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #FFFFFF;");
            Button okBtn = (Button) dialogPane.lookupButton(ButtonType.OK);
            Button cancelBtn = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
            if (okBtn != null) {
                okBtn.setStyle(
                        "-fx-background-color: #0A4F34; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;");
            }
            if (cancelBtn != null) {
                cancelBtn.setStyle(
                        "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 6px;");
            }

            Optional<ButtonType> confirmResult = settleConfirmAlert.showAndWait();
            if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) {
                return;
            }

            java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
            for (CartItem ci : cartList) {
                subtotal = subtotal
                        .add(ci.getItem().getPrice().multiply(java.math.BigDecimal.valueOf(ci.getQuantity())));
            }

            java.math.BigDecimal discountAmt = java.math.BigDecimal.ZERO;
            if (discountValue > 0.0 && subtotal.compareTo(java.math.BigDecimal.ZERO) > 0) {
                if (isDiscountPercentage) {
                    discountAmt = subtotal.multiply(java.math.BigDecimal.valueOf(discountValue / 100.0));
                } else {
                    discountAmt = java.math.BigDecimal.valueOf(discountValue);
                }
                if (discountAmt.compareTo(subtotal) > 0) {
                    discountAmt = subtotal;
                }
            }

            java.math.BigDecimal taxableSubtotal = subtotal.subtract(discountAmt);
            java.math.BigDecimal taxRate = java.math.BigDecimal.valueOf(0.025);
            java.math.BigDecimal cgst = taxableSubtotal.multiply(taxRate);
            java.math.BigDecimal sgst = taxableSubtotal.multiply(taxRate);
            java.math.BigDecimal grandTotalVal = taxableSubtotal.add(cgst).add(sgst);

            Order order;
            if (selectedOrderType == OrderType.DINE_IN && currentDiningTable != null) {
                UUID restaurantId = TenantContext.getRestaurantId();
                var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                        java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));
                order = activeOrders.stream()
                        .filter(o -> o.getTableId() != null && o.getTableId().equals(currentDiningTable.getId()))
                        .findFirst()
                        .orElse(null);

                if (order == null) {
                    order = new Order();
                    order.setRestaurantId(restaurantId);
                    order.setTableId(currentDiningTable.getId());
                    order.setTableName(currentDiningTable.getTableNumber());
                    order.setType(OrderType.DINE_IN);
                    order.setSource("DIRECT");
                }
            } else {
                if (currentActiveOrder != null && currentActiveOrder.getType() != OrderType.DINE_IN) {
                    order = currentActiveOrder;
                } else {
                    order = new Order();
                    order.setRestaurantId(TenantContext.getRestaurantId());
                    order.setType(selectedOrderType);
                    order.setSource("DIRECT");
                    order.setTableId(null);
                    order.setTableName(null);
                    order.setOrderNumber("#" + (System.currentTimeMillis() % 10000));
                }
            }

            order.setStatus(OrderStatus.PAID);
            order.setSubTotal(subtotal);
            order.setCgst(cgst);
            order.setSgst(sgst);
            order.setDiscount(discountAmt);
            order.setGrandTotal(grandTotalVal);

            if (selectedOrderType == OrderType.DINE_IN) {
                order.setCustomerName(
                        "Guest Table " + (currentDiningTable != null ? currentDiningTable.getTableNumber() : ""));
            } else if (selectedOrderType == OrderType.DELIVERY) {
                order.setCustomerPhone(deliveryPhoneField.getText().trim());
                order.setCustomerName(deliveryNameField.getText().trim());
                order.setTableName("Delivery: " + deliveryAddressField.getText().trim());
            } else {
                order.setCustomerPhone(pickupPhoneField.getText().trim());
                String pName = pickupNameField.getText().trim();
                order.setCustomerName(pName.isEmpty() ? "Walk-In Pickup" : pName);
                order.setTableName("Pickup");
            }

            double rec = 0.0;
            try {
                rec = Double.parseDouble(receivedAmountField.getText().trim());
            } catch (Exception e) {
            }
            order.setReceivedAmount(java.math.BigDecimal.valueOf(rec));

            java.math.BigDecimal change = java.math.BigDecimal.valueOf(rec).subtract(grandTotalVal);
            order.setChangeAmount(change.compareTo(java.math.BigDecimal.ZERO) > 0 ? change : java.math.BigDecimal.ZERO);
            order.setPaymentMode(selectedPaymentMode);
            order.setSettledAt(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);

            // For Delivery and Takeaway (Pickup), create a KOT ticket for the delta cart
            // items so they show in KDS
            if (selectedOrderType != OrderType.DINE_IN) {
                // Fetch existing KOT items to compute delta
                java.util.Map<UUID, Integer> savedQuantities = new java.util.HashMap<>();
                List<KOT> kots = kotRepository.findByOrderId(savedOrder.getId());
                for (KOT kot : kots) {
                    for (KOTItem item : kot.getItems()) {
                        savedQuantities.put(item.getMenuItemId(),
                                savedQuantities.getOrDefault(item.getMenuItemId(), 0) + item.getQuantity());
                    }
                }

                List<KOTItem> deltaKOTItems = new ArrayList<>();
                for (CartItem ci : cartList) {
                    int savedQty = savedQuantities.getOrDefault(ci.getItem().getId(), 0);
                    int deltaQty = ci.getQuantity() - savedQty;
                    if (deltaQty > 0) {
                        KOTItem ki = new KOTItem();
                        ki.setRestaurantId(savedOrder.getRestaurantId());
                        ki.setMenuItemId(ci.getItem().getId());
                        ki.setItemName(ci.getItem().getName());
                        ki.setQuantity(deltaQty);

                        String specialInstruction = ci.getNotes() != null ? ci.getNotes() : "";
                        if (ci.getModifiers() != null && !ci.getModifiers().isEmpty()) {
                            String modsString = String.join(", ", ci.getModifiers());
                            if (specialInstruction.isEmpty()) {
                                specialInstruction = modsString;
                            } else {
                                specialInstruction = specialInstruction + " (" + modsString + ")";
                            }
                        }
                        ki.setSpecialInstruction(specialInstruction);
                        ki.setItemStatus(KOTStatus.PENDING);
                        deltaKOTItems.add(ki);
                    }
                }

                if (!deltaKOTItems.isEmpty()) {
                    KOT kot = new KOT();
                    kot.setRestaurantId(savedOrder.getRestaurantId());
                    kot.setOrderId(savedOrder.getId());
                    kot.setTableId(UUID.randomUUID()); // Dummy non-null tableId for DB constraint
                    kot.setTableName(savedOrder.getTableName());
                    kot.setKotNumber("KOT-" + (System.currentTimeMillis() % 100000));
                    kot.setOverallStatus(KOTStatus.PENDING);
                    kot.setNotes("");
                    kot.setItems(deltaKOTItems);
                    KOT savedKot = kotRepository.save(kot);

                    // Broadcast WebSocket notification for KDS
                    if (messagingTemplate != null) {
                        try {
                            String kitchenTopic = "/topic/kitchen/" + savedOrder.getRestaurantId();
                            messagingTemplate.convertAndSend(kitchenTopic, savedKot);
                            System.out.println("📢 WebSocket Delivery/Pickup settled KOT Broadcast sent to topic: "
                                    + kitchenTopic);
                        } catch (Exception wsEx) {
                            System.err.println("❌ Failed to broadcast delivery/pickup KOT: " + wsEx.getMessage());
                        }
                    }

                    // Refresh KDS UI
                    if (kdsNativeController != null) {
                        kdsNativeController.refreshKdsData();
                    }
                }
            }

            // Trigger printer
            triggerThermalReceiptPrinting(order);

            // Set table back to AVAILABLE
            if (selectedOrderType == OrderType.DINE_IN && currentDiningTable != null) {
                currentDiningTable.setStatus(TableStatus.AVAILABLE);
                tableRepository.save(currentDiningTable);

                // Broadcast table status update
                if (messagingTemplate != null) {
                    try {
                        String tableTopic = "/topic/tables/" + order.getRestaurantId();
                        java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
                        wsPayload.put("id", currentDiningTable.getId().toString());
                        wsPayload.put("status", TableStatus.AVAILABLE.name());
                        wsPayload.put("totalAmount", 0.0);
                        wsPayload.put("durationMinutes", 0);
                        messagingTemplate.convertAndSend(tableTopic, wsPayload);

                        // Cascade to all merged tables if present
                        if (order.getMergedTableIds() != null && !order.getMergedTableIds().trim().isEmpty()) {
                            for (String idStr : order.getMergedTableIds().split(",")) {
                                UUID otherId = UUID.fromString(idStr.trim());
                                if (!otherId.equals(currentDiningTable.getId())) {
                                    DiningTable otherTable = tableRepository.findById(otherId).orElse(null);
                                    if (otherTable != null) {
                                        otherTable.setStatus(TableStatus.AVAILABLE);
                                        otherTable.setTotalAmount(0.0);
                                        otherTable.setDurationMinutes(0);
                                        tableRepository.save(otherTable);

                                        java.util.Map<String, Object> otherPayload = new java.util.HashMap<>();
                                        otherPayload.put("id", otherTable.getId().toString());
                                        otherPayload.put("status", TableStatus.AVAILABLE.name());
                                        otherPayload.put("totalAmount", 0.0);
                                        otherPayload.put("durationMinutes", 0);
                                        messagingTemplate.convertAndSend(tableTopic, otherPayload);
                                    }
                                }
                            }
                        }
                    } catch (Exception wsEx) {
                        System.err.println("❌ Failed to broadcast table status: " + wsEx.getMessage());
                    }
                }
            }

            resetBillingSessionState();

            showAlert("Settlement Successful", "Order settled successfully!");

            // Automatically return to dashboard after settlement
            showHomeView();
        } catch (Exception e) {
            showAlert("Error Settling Order", "Could not complete payment settlement: " + e.getMessage());
        }
    }

    private void resetBillingSessionState() {
        currentActiveOrder = null;
        currentDiningTable = null;
        cartList.clear();
        selectedCartItem = null;
        discountValue = 0.0;
        isDiscountPercentage = false;
        
        clearPaymentModeSelection();
        
        activeModifiers.clear();
        populateModifiersUi();
        currentCustomerPhone = "";
        currentCustomerName = "";
        currentCustomerNotes = "";
        if (deliveryPhoneField != null)
            deliveryPhoneField.clear();
        if (deliveryNameField != null)
            deliveryNameField.clear();
        if (deliveryAddressField != null)
            deliveryAddressField.clear();
        if (pickupPhoneField != null)
            pickupPhoneField.clear();
        if (pickupNameField != null)
            pickupNameField.clear();
        if (dineInTableChip != null) {
            dineInTableChip.setText("Select Table");
        }
        if (cartHeaderLabel != null) {
            cartHeaderLabel.setText("Preparing Order (No Table Selected)");
        }
        updateCustomerButtonState();
        updateCalculations();
        populateCart();
    }

    private void clearPaymentModeSelection() {
        selectedPaymentMode = null;
        if (payCashBtn != null) {
            payCashBtn.getStyleClass().remove("active");
            payCashBtn.setStyle("");
        }
        if (payCardBtn != null) {
            payCardBtn.getStyleClass().remove("active");
            payCardBtn.setStyle("");
        }
        if (payUpiBtn != null) {
            payUpiBtn.getStyleClass().remove("active");
            payUpiBtn.setStyle("");
        }
        if (payOtherBtn != null) {
            payOtherBtn.getStyleClass().remove("active");
            payOtherBtn.setStyle("");
        }
        if (paySplitBtn != null) {
            paySplitBtn.getStyleClass().remove("active");
            paySplitBtn.setStyle("");
        }

        if (receivedAmountField != null) {
            receivedAmountField.setText("0.00");
            receivedAmountField.setDisable(true);
        }
        updateCalculations();
    }

    private void updateBillingPageControlState() {
        if (selectedOrderType == OrderType.DINE_IN
                && currentDiningTable != null
                && currentActiveOrder == null) {

            // "Taking new Dine-In order" (Draft / Not Saved yet)
            if (settleBtn != null) {
                settleBtn.setText("Settle & Save ›");
                settleBtn.setDisable(true);
                settleBtn.setStyle(
                        "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: null;");
            }

            // Disable other settlement controls
            if (payCashBtn != null)
                payCashBtn.setDisable(true);
            if (payCardBtn != null)
                payCardBtn.setDisable(true);
            if (payUpiBtn != null)
                payUpiBtn.setDisable(true);
            if (payOtherBtn != null)
                payOtherBtn.setDisable(true);
            if (paySplitBtn != null)
                paySplitBtn.setDisable(true);
            if (receivedAmountField != null)
                receivedAmountField.setDisable(true);
            if (chkIsPaid != null)
                chkIsPaid.setDisable(true);
            if (chkSendReceipt != null)
                chkSendReceipt.setDisable(true);
            if (addDiscountBtn != null)
                addDiscountBtn.setDisable(true);
            if (addNoteBtn != null)
                addNoteBtn.setDisable(true);

            if (cancelOrderBtn != null) {
                cancelOrderBtn.setDisable(true);
                cancelOrderBtn.setStyle(
                        "-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: null;");
            }
            if (saveOrderBtn != null) {
                saveOrderBtn.setDisable(false);
                // Highlight Save Order button in green to guide the user to send/save it first
                saveOrderBtn.setStyle(
                        "-fx-background-color: #10B981; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(16, 185, 129, 0.4), 8, 0, 0, 0);");
            }
            if (saveAndPrintBtn != null) {
                saveAndPrintBtn.setDisable(true);
                saveAndPrintBtn.setStyle(
                        "-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: null;");
            }

        } else if (selectedOrderType == OrderType.DINE_IN
                && currentDiningTable != null
                && currentActiveOrder != null
                && currentActiveOrder.getStatus() == OrderStatus.OPEN) {

            // "Bill the Order" mode (Running Order)
            if (settleBtn != null) {
                settleBtn.setDisable(false);
                settleBtn.setText("Bill the Order ›");
                // Highlight button using vibrant amber/orange with subtle dropshadow glow
                settleBtn.setStyle(
                        "-fx-background-color: #D97706; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(217, 119, 6, 0.4), 8, 0, 0, 0);");
            }

            // Disable other settlement controls
            if (payCashBtn != null)
                payCashBtn.setDisable(true);
            if (payCardBtn != null)
                payCardBtn.setDisable(true);
            if (payUpiBtn != null)
                payUpiBtn.setDisable(true);
            if (payOtherBtn != null)
                payOtherBtn.setDisable(true);
            if (paySplitBtn != null)
                paySplitBtn.setDisable(true);
            if (receivedAmountField != null)
                receivedAmountField.setDisable(true);
            if (chkIsPaid != null)
                chkIsPaid.setDisable(true);
            if (chkSendReceipt != null)
                chkSendReceipt.setDisable(true);
            if (addDiscountBtn != null)
                addDiscountBtn.setDisable(true);
            if (addNoteBtn != null)
                addNoteBtn.setDisable(true);
            if (cancelOrderBtn != null) {
                cancelOrderBtn.setDisable(false);
                // Highlight button using vibrant red with subtle dropshadow glow to match "Bill
                // the Order" and "Save Order" prominence
                cancelOrderBtn.setStyle(
                        "-fx-background-color: #EF4444; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(239, 68, 68, 0.4), 8, 0, 0, 0);");
            }
            if (saveOrderBtn != null) {
                saveOrderBtn.setDisable(false);
                // Highlight button using vibrant emerald green with subtle dropshadow glow to
                // match "Bill the Order" prominence
                saveOrderBtn.setStyle(
                        "-fx-background-color: #10B981; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(16, 185, 129, 0.4), 8, 0, 0, 0);");
            }
            if (saveAndPrintBtn != null) {
                saveAndPrintBtn.setDisable(true);
                saveAndPrintBtn.setStyle(
                        "-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: null;");
            }

        } else if ((selectedOrderType == OrderType.DELIVERY || selectedOrderType == OrderType.PICK_UP)
                && currentActiveOrder == null) {

            // ── Delivery / Pickup DRAFT (no saved order yet) ──────────────────────
            // Mirror the Dine-In draft state: only Save Order is active.
            // Everything else is locked to prevent accidental settlement misclicks.
            if (settleBtn != null) {
                settleBtn.setText("Settle & Save ›");
                settleBtn.setDisable(true);
                settleBtn.setStyle(
                        "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: null;");
            }
            if (payCashBtn != null)
                payCashBtn.setDisable(true);
            if (payCardBtn != null)
                payCardBtn.setDisable(true);
            if (payUpiBtn != null)
                payUpiBtn.setDisable(true);
            if (payOtherBtn != null)
                payOtherBtn.setDisable(true);
            if (paySplitBtn != null)
                paySplitBtn.setDisable(true);
            if (receivedAmountField != null)
                receivedAmountField.setDisable(true);
            if (chkIsPaid != null)
                chkIsPaid.setDisable(true);
            if (chkSendReceipt != null)
                chkSendReceipt.setDisable(true);
            if (addDiscountBtn != null)
                addDiscountBtn.setDisable(true);
            if (addNoteBtn != null)
                addNoteBtn.setDisable(true);
            if (cancelOrderBtn != null) {
                cancelOrderBtn.setDisable(true);
                cancelOrderBtn.setStyle(
                        "-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: null;");
            }
            if (saveOrderBtn != null) {
                saveOrderBtn.setDisable(false);
                // Highlight Save Order in green to guide the user
                saveOrderBtn.setStyle(
                        "-fx-background-color: #10B981; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(16, 185, 129, 0.4), 8, 0, 0, 0);");
            }
            if (saveAndPrintBtn != null) {
                saveAndPrintBtn.setDisable(true);
                saveAndPrintBtn.setStyle(
                        "-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: null;");
            }

        } else if ((selectedOrderType == OrderType.DELIVERY || selectedOrderType == OrderType.PICK_UP)
                && currentActiveOrder != null
                && currentActiveOrder.getStatus() == OrderStatus.OPEN) {

            // ── Delivery / Pickup RUNNING (order saved, OPEN) ─────────────────────
            // Mirror the Dine-In running state: Cancel (red) + Save Order (green) + Bill
            // the Order (amber) active.
            if (settleBtn != null) {
                settleBtn.setDisable(false);
                settleBtn.setText("Bill the Order ›");
                settleBtn.setStyle(
                        "-fx-background-color: #D97706; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(217, 119, 6, 0.4), 8, 0, 0, 0);");
            }
            // Disable settlement controls
            if (payCashBtn != null)
                payCashBtn.setDisable(true);
            if (payCardBtn != null)
                payCardBtn.setDisable(true);
            if (payUpiBtn != null)
                payUpiBtn.setDisable(true);
            if (payOtherBtn != null)
                payOtherBtn.setDisable(true);
            if (paySplitBtn != null)
                paySplitBtn.setDisable(true);
            if (receivedAmountField != null)
                receivedAmountField.setDisable(true);
            if (chkIsPaid != null)
                chkIsPaid.setDisable(true);
            if (chkSendReceipt != null)
                chkSendReceipt.setDisable(true);
            if (addDiscountBtn != null)
                addDiscountBtn.setDisable(true);
            if (addNoteBtn != null)
                addNoteBtn.setDisable(true);
            if (cancelOrderBtn != null) {
                cancelOrderBtn.setDisable(false);
                cancelOrderBtn.setStyle(
                        "-fx-background-color: #EF4444; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(239, 68, 68, 0.4), 8, 0, 0, 0);");
            }
            if (saveOrderBtn != null) {
                saveOrderBtn.setDisable(false);
                saveOrderBtn.setStyle(
                        "-fx-background-color: #10B981; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(16, 185, 129, 0.4), 8, 0, 0, 0);");
            }
            if (saveAndPrintBtn != null) {
                saveAndPrintBtn.setDisable(true);
                saveAndPrintBtn.setStyle(
                        "-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: null;");
            }

        } else {
            // ── Normal "Settle & Save" mode (Dine-In BILLED / all other states) ──
            if (settleBtn != null) {
                settleBtn.setDisable(false);
                settleBtn.setText("Settle & Save ›");
                settleBtn.setStyle(
                        "-fx-background-color: #0A4F34; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: white; -fx-effect: null;");
            }
            // Re-enable all settlement controls
            if (payCashBtn != null)
                payCashBtn.setDisable(false);
            if (payCardBtn != null)
                payCardBtn.setDisable(false);
            if (payUpiBtn != null)
                payUpiBtn.setDisable(false);
            if (payOtherBtn != null)
                payOtherBtn.setDisable(false);
            if (paySplitBtn != null)
                paySplitBtn.setDisable(false);
            if (receivedAmountField != null)
                receivedAmountField.setDisable(!"CASH".equals(selectedPaymentMode));
            if (chkIsPaid != null)
                chkIsPaid.setDisable(false);
            if (chkSendReceipt != null)
                chkSendReceipt.setDisable(false);
            if (addDiscountBtn != null)
                addDiscountBtn.setDisable(false);
            if (addNoteBtn != null)
                addNoteBtn.setDisable(false);
            if (cancelOrderBtn != null) {
                cancelOrderBtn.setDisable(false);
                cancelOrderBtn.setStyle(""); // Restore default secondary style
            }
            if (saveOrderBtn != null) {
                saveOrderBtn.setDisable(false);
                saveOrderBtn.setStyle(
                        "-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 1px; -fx-text-fill: #1E293B; -fx-padding: 7 12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-effect: null;");
            }
            if (saveAndPrintBtn != null) {
                saveAndPrintBtn.setDisable(false);
                saveAndPrintBtn.setStyle("");
            }
        }
    }

    private void openTableSelectionDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Select Dining Table");
        dialog.setHeaderText("Choose a Dining Table for this Session");

        // Set the button types
        ButtonType selectButtonType = new ButtonType("Select Table", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> tableComboBox = new ComboBox<>();
        tableComboBox.setPromptText("Select Table");

        // Fetch tables from database
        UUID restaurantId = TenantContext.getRestaurantId();
        List<DiningTable> tables = tableRepository.findByRestaurantId(restaurantId);

        // Sort tables logically by table number
        tables.sort((t1, t2) -> {
            try {
                // Try integer comparison if they are numbers
                int num1 = Integer.parseInt(t1.getTableNumber().replaceAll("\\D+", ""));
                int num2 = Integer.parseInt(t2.getTableNumber().replaceAll("\\D+", ""));
                return Integer.compare(num1, num2);
            } catch (Exception e) {
                return t1.getTableNumber().compareToIgnoreCase(t2.getTableNumber());
            }
        });

        java.util.Map<String, DiningTable> tableMap = new java.util.HashMap<>();
        for (DiningTable t : tables) {
            // Only show AVAILABLE tables, or the current table for pre-selection
            if (t.getStatus() != TableStatus.AVAILABLE
                    && (currentDiningTable == null || !t.getId().equals(currentDiningTable.getId()))) {
                continue;
            }
            String displayName = t.getTableNumber() + " - " + t.getAreaName() + " [" + t.getStatus().name() + "]";
            tableComboBox.getItems().add(displayName);
            tableMap.put(displayName, t);

            // Pre-select current active table if set
            if (currentDiningTable != null && t.getId().equals(currentDiningTable.getId())) {
                tableComboBox.setValue(displayName);
            }
        }

        grid.add(new Label("Available Tables:"), 0, 0);
        grid.add(tableComboBox, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the combobox by default
        Platform.runLater(() -> tableComboBox.requestFocus());

        java.util.Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == selectButtonType) {
            String selectedStr = tableComboBox.getValue();
            if (selectedStr != null) {
                DiningTable selectedTable = tableMap.get(selectedStr);
                if (selectedTable != null) {
                    DiningTable oldTable = currentDiningTable;
                    currentDiningTable = selectedTable;
                    if (dineInTableChip != null) {
                        dineInTableChip.setText(selectedTable.getTableNumber());
                    }
                    if (cartHeaderLabel != null) {
                        cartHeaderLabel.setText("Preparing Order for " + selectedTable.getTableNumber());
                    }

                    // Check if there is an active running order for this table
                    var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(
                            restaurantId,
                            java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));
                    Order activeOrder = activeOrders.stream()
                            .filter(o -> o.getTableId() != null && o.getTableId().equals(selectedTable.getId()))
                            .findFirst()
                            .orElse(null);

                    if (activeOrder != null) {
                        // Load and merge with existing running order for this table
                        currentActiveOrder = activeOrder;
                        loadOrderItemsToCart(activeOrder, true);
                    } else {
                        // If we are transferring an existing active running order to an available table
                        if (oldTable != null && !oldTable.getId().equals(selectedTable.getId())
                                && currentActiveOrder != null) {
                            // Update order details in database
                            currentActiveOrder.setTableId(selectedTable.getId());
                            currentActiveOrder.setTableName("Table " + selectedTable.getTableNumber());
                            orderRepository.save(currentActiveOrder);

                            // Update KOTs associated with this order
                            List<KOT> kots = kotRepository.findByOrderId(currentActiveOrder.getId());
                            if (kots != null) {
                                for (KOT kot : kots) {
                                    kot.setTableId(selectedTable.getId());
                                    kot.setTableName("Table " + selectedTable.getTableNumber());
                                    kotRepository.save(kot);
                                }
                            }

                            // Vacate old table
                            oldTable.setStatus(TableStatus.AVAILABLE);
                            tableRepository.save(oldTable);

                            // Mark new table as occupied
                            if (currentActiveOrder.getStatus() == OrderStatus.BILLED) {
                                selectedTable.setStatus(TableStatus.PAYMENT_PENDING);
                            } else {
                                selectedTable.setStatus(TableStatus.RUNNING);
                            }
                            tableRepository.save(selectedTable);

                            // Refresh database status locally
                            loadTablesToUi();
                            loadRunningOrders();
                        } else {
                            // Start fresh draft for this table, keeping any current cart items
                            currentActiveOrder = null;
                            if (cartList.isEmpty()) {
                                discountValue = 0.0;
                                isDiscountPercentage = false;
                                if (receivedAmountField != null)
                                    receivedAmountField.setText("0.00");
                                activeModifiers.clear();
                                populateModifiersUi();
                            }
                        }
                    }
                    updateCustomerButtonState();
                    updateCalculations();
                    populateCart();
                    updateBillingPageControlState();
                }
            }
        }
    }

    private void updateCustomerButtonState() {
        if (dineInCustomerBtn != null) {
            if (currentCustomerPhone != null && !currentCustomerPhone.isEmpty()) {
                dineInCustomerBtn.setText(currentCustomerName);
                dineInCustomerBtn.setStyle(
                        "-fx-background-color: #ECFDF5; -fx-border-color: #10B981; -fx-text-fill: #047857; -fx-font-weight: bold;");
            } else {
                dineInCustomerBtn.setText("Add Customer");
                dineInCustomerBtn.setStyle(""); // Default styles
            }
        }
    }

    private void openCustomerDetailsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Customer Details");
        dialog.setHeaderText("Add or Track Customer Details");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save & Link", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField phoneField = new TextField();
        phoneField.setPromptText("Enter Phone Number");
        phoneField.setText(currentCustomerPhone);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter Customer Name");
        nameField.setText(currentCustomerName);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter preferences/notes...");
        notesArea.setText(currentCustomerNotes);
        notesArea.setPrefRowCount(3);

        Label visitCountLabel = new Label("New Customer");
        visitCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748B;");

        // Add a lookup listener to phone number field to track repeat customers!
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> {
            String cleanPhone = newValue != null ? newValue.trim() : "";
            if (cleanPhone.length() >= 10) {
                // Query customerRepository for existing customer profile
                try {
                    Optional<Customer> existingOpt = customerRepository
                            .findByRestaurantIdAndPhone(TenantContext.getRestaurantId(), cleanPhone);
                    if (existingOpt.isPresent()) {
                        Customer cust = existingOpt.get();
                        Platform.runLater(() -> {
                            nameField.setText(cust.getName());
                            notesArea.setText(cust.getNotes());
                            visitCountLabel.setText("Repeat Customer (Visits: " + cust.getVisitCount() + ")");
                            visitCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E3A8A;"); // Dark blue for
                                                                                                        // repeat
                                                                                                        // customer
                        });
                    } else {
                        Platform.runLater(() -> {
                            visitCountLabel.setText("New Customer");
                            visitCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748B;");
                        });
                    }
                } catch (Exception ex) {
                    System.out.println("Error looking up customer: " + ex.getMessage());
                }
            }
        });

        // Trigger listener on open if phone number is already set
        if (currentCustomerPhone != null && currentCustomerPhone.trim().length() >= 10) {
            String cleanPhone = currentCustomerPhone.trim();
            try {
                Optional<Customer> existingOpt = customerRepository
                        .findByRestaurantIdAndPhone(TenantContext.getRestaurantId(), cleanPhone);
                if (existingOpt.isPresent()) {
                    Customer cust = existingOpt.get();
                    visitCountLabel.setText("Repeat Customer (Visits: " + cust.getVisitCount() + ")");
                    visitCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E3A8A;");
                }
            } catch (Exception ex) {
                // Ignore
            }
        }

        grid.add(new Label("Phone Number:"), 0, 0);
        grid.add(phoneField, 1, 0);
        grid.add(new Label("Customer Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Notes/Preferences:"), 0, 2);
        grid.add(notesArea, 1, 2);
        grid.add(new Label("Status:"), 0, 3);
        grid.add(visitCountLabel, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Styling the dialog pane
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/ui/dashboard.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("custom-dialog-pane");

        // Request focus on phone field by default
        Platform.runLater(() -> phoneField.requestFocus());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String finalPhone = phoneField.getText().trim();
            String finalName = nameField.getText().trim();
            String finalNotes = notesArea.getText().trim();

            if (!finalPhone.isEmpty()) {
                currentCustomerPhone = finalPhone;
                currentCustomerName = finalName.isEmpty() ? "Customer (" + finalPhone + ")" : finalName;
                currentCustomerNotes = finalNotes;

                // Create or update customer profile in the DB
                try {
                    UUID restaurantId = TenantContext.getRestaurantId();
                    Optional<Customer> existingOpt = customerRepository.findByRestaurantIdAndPhone(restaurantId,
                            finalPhone);
                    Customer cust;
                    if (existingOpt.isPresent()) {
                        cust = existingOpt.get();
                        cust.setName(finalName);
                        cust.setNotes(finalNotes);
                        cust.setVisitCount(cust.getVisitCount() + 1);
                    } else {
                        cust = new Customer(finalPhone, finalName, finalNotes);
                        cust.setRestaurantId(restaurantId);
                        cust.setVisitCount(1);
                    }
                    customerRepository.save(cust);
                } catch (Exception ex) {
                    System.out.println("Error saving customer profile: " + ex.getMessage());
                }

                // If there is an active order, update and save it immediately
                if (currentActiveOrder != null) {
                    try {
                        currentActiveOrder.setCustomerPhone(currentCustomerPhone);
                        currentActiveOrder.setCustomerName(currentCustomerName);
                        currentActiveOrder.setNotes(currentCustomerNotes);
                        orderRepository.save(currentActiveOrder);
                    } catch (Exception ex) {
                        System.out.println("Error updating active order customer details: " + ex.getMessage());
                    }
                }

                updateCustomerButtonState();
            }
        }
    }

    public void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // --- ORIGINAL DASHBOARD LOGIC ---
    private void saveFallbackTable(String number, int capacity, String area) {
        try {
            DiningTable table = new DiningTable();
            table.setRestaurantId(TenantContext.getRestaurantId());
            table.setTableNumber(number);
            table.setCapacity(capacity);
            table.setAreaName(area);
            table.setStatus(TableStatus.AVAILABLE);
            tableRepository.save(table);
        } catch (Exception e) {
            System.out.println("Failed to save fallback table: " + e.getMessage());
        }
    }

    private double calculateOrderAmountFallback(Order order) {
        if (order.getGrandTotal() != null && order.getGrandTotal().doubleValue() > 0.0) {
            return order.getGrandTotal().doubleValue();
        }

        double calculatedTotal = 0.0;
        try {
            List<KOT> orderKots = kotRepository.findByOrderId(order.getId());
            for (KOT kot : orderKots) {
                for (KOTItem item : kot.getItems()) {
                    MenuItem menuItem = menuRepository.findById(item.getMenuItemId()).orElse(null);
                    if (menuItem != null && menuItem.getPrice() != null) {
                        calculatedTotal += menuItem.getPrice().doubleValue() * item.getQuantity();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error calculating fallback total: " + e.getMessage());
        }

        return calculatedTotal > 0.0 ? (calculatedTotal * 1.05) : 0.0;
    }

    private LocalDateTime getOrderStartTimeFallback(Order order) {
        LocalDateTime startTime = order.getStartedAt();
        if (startTime == null) {
            startTime = order.getCreatedAt();
        }
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        return startTime;
    }

    private boolean isTableReadyToBill(DiningTable table, List<Order> activeOrders) {
        if (table.getStatus() == TableStatus.AVAILABLE) {
            return false;
        }
        if (table.getStatus() == TableStatus.PAYMENT_PENDING) {
            return true;
        }
        if (activeOrders != null) {
            Order activeOrder = activeOrders.stream()
                    .filter(o -> (o.getTableId() != null && o.getTableId().equals(table.getId())) ||
                            (o.getMergedTableIds() != null && o.getMergedTableIds().contains(table.getId().toString())))
                    .findFirst()
                    .orElse(null);
            if (activeOrder != null) {
                return activeOrder.getStatus() == OrderStatus.BILLED;
            }
        }
        return false;
    }

    private int compareTableNumbers(String s1, String s2) {
        if (s1 == null && s2 == null)
            return 0;
        if (s1 == null)
            return -1;
        if (s2 == null)
            return 1;

        String[] parts1 = s1.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        String[] parts2 = s2.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        int minLength = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < minLength; i++) {
            String p1 = parts1[i];
            String p2 = parts2[i];

            if (Character.isDigit(p1.charAt(0)) && Character.isDigit(p2.charAt(0))) {
                try {
                    int n1 = Integer.parseInt(p1);
                    int n2 = Integer.parseInt(p2);
                    if (n1 != n2) {
                        return Integer.compare(n1, n2);
                    }
                } catch (NumberFormatException e) {
                    int cmp = p1.compareTo(p2);
                    if (cmp != 0)
                        return cmp;
                }
            } else {
                int cmp = p1.compareToIgnoreCase(p2);
                if (cmp != 0)
                    return cmp;
            }
        }
        return Integer.compare(parts1.length, parts2.length);
    }

    public void loadTablesToUi() {
        UUID restaurantId = getActiveRestaurantId();
        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(restaurantId);
            try {
                var tablesList = tableRepository.findByRestaurantId(restaurantId);

                if (tablesList.isEmpty()) {
                    saveFallbackTable("T-01", 4, "AC Area");
                    saveFallbackTable("T-02", 2, "AC Area");
                    saveFallbackTable("T-03", 4, "AC Area");
                    saveFallbackTable("T-04", 6, "AC Area");
                    saveFallbackTable("T-05", 2, "Garden");
                    saveFallbackTable("T-06", 4, "Garden");
                    saveFallbackTable("T-07", 4, "Garden");
                    saveFallbackTable("T-08", 8, "Garden");
                    tablesList = tableRepository.findByRestaurantId(restaurantId);
                }

                var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                        java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));

                List<KOT> activeKots = new ArrayList<>();
                try {
                    activeKots = kotRepository.findByRestaurantIdAndOverallStatusIn(
                            restaurantId,
                            List.of(KOTStatus.PENDING, KOTStatus.PREPARING));
                } catch (Exception e) {
                    System.out.println("Error fetching active KOTs: " + e.getMessage());
                }

                // Filter to only show today's tickets to match KDS Page
                java.time.LocalDate today = java.time.LocalDate.now();
                List<KOT> finalActiveKots = activeKots.stream()
                        .filter(k -> k.getCreatedAt() != null && k.getCreatedAt().toLocalDate().isEqual(today))
                        .collect(java.util.stream.Collectors.toList());

                final var finalTablesList = tablesList;
                final var finalActiveOrders = activeOrders;

                Platform.runLater(() -> {
                    try {
                        renderTablesToUiSync(finalTablesList, finalActiveOrders, finalActiveKots);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } finally {
                TenantContext.clear();
            }
        });
    }

    private void renderTablesToUiSync(List<DiningTable> tablesList, List<Order> activeOrders, List<KOT> activeKots) {
        tablesContainer.getChildren().clear();

        int activeKCount = activeKots.size();
        int loadPercent = Math.min((int) Math.round((activeKCount / 8.0) * 100), 100);

        pulseLabel.setText(loadPercent + "%");
        if (pulseGaugeArc != null) {
            double length = -(loadPercent * 180.0) / 100.0;
            pulseGaugeArc.setLength(length);
            pulseGaugeArc.getStyleClass().removeAll("st-low", "st-mod", "st-high");
            if (loadPercent < 40) {
                pulseGaugeArc.getStyleClass().add("st-low");
            } else if (loadPercent <= 78) {
                pulseGaugeArc.getStyleClass().add("st-mod");
            } else {
                pulseGaugeArc.getStyleClass().add("st-high");
            }
        }
        if (pulseStatusLabel != null) {
            if (loadPercent < 40) {
                pulseStatusLabel.setText("OPTIMAL LOAD");
                pulseStatusLabel.setStyle("-fx-text-fill: #10B981;");
            } else if (loadPercent <= 78) {
                pulseStatusLabel.setText("MEDIUM LOAD");
                pulseStatusLabel.setStyle("-fx-text-fill: #F59E0B;");
            } else {
                pulseStatusLabel.setText("CRITICAL LOAD");
                pulseStatusLabel.setStyle("-fx-text-fill: #EF4444;");
            }
        }

        // Update KDS-style station load badges
        if (stLoadA != null) {
            stLoadA.getStyleClass().removeAll("st-low", "st-mod", "st-high");
            if (loadPercent < 40) {
                stLoadA.setText("Optimal");
                stLoadA.getStyleClass().add("st-low");
            } else if (loadPercent <= 78) {
                stLoadA.setText("Medium");
                stLoadA.getStyleClass().add("st-mod");
            } else {
                stLoadA.setText("High Load");
                stLoadA.getStyleClass().add("st-high");
            }
        }
        if (stLoadB != null) {
            stLoadB.getStyleClass().removeAll("st-low", "st-mod", "st-high");
            if (activeKCount <= 1) {
                stLoadB.setText("Optimal");
                stLoadB.getStyleClass().add("st-low");
            } else if (activeKCount <= 3) {
                stLoadB.setText("Medium");
                stLoadB.getStyleClass().add("st-mod");
            } else {
                stLoadB.setText("High Load");
                stLoadB.getStyleClass().add("st-high");
            }
        }
        if (stLoadC != null) {
            stLoadC.getStyleClass().removeAll("st-low", "st-mod", "st-high");
            if (activeKCount <= 2) {
                stLoadC.setText("Optimal");
                stLoadC.getStyleClass().add("st-low");
            } else if (activeKCount <= 4) {
                stLoadC.setText("Medium");
                stLoadC.getStyleClass().add("st-mod");
            } else {
                stLoadC.setText("High Load");
                stLoadC.getStyleClass().add("st-high");
            }
        }

        List<DiningTable> sortedTablesList = new ArrayList<>(tablesList);
        sortedTablesList.sort((t1, t2) -> {
            boolean ready1 = isTableReadyToBill(t1, activeOrders);
            boolean ready2 = isTableReadyToBill(t2, activeOrders);
            if (ready1 && !ready2)
                return -1;
            if (!ready1 && ready2)
                return 1;
            return compareTableNumbers(t1.getTableNumber(), t2.getTableNumber());
        });

        for (DiningTable table : sortedTablesList) {
            final DiningTable finalTable = table;
            Order activeOrder = activeOrders.stream()
                    .filter(o -> (o.getTableId() != null && o.getTableId().equals(finalTable.getId())) ||
                            (o.getMergedTableIds() != null
                                    && o.getMergedTableIds().contains(finalTable.getId().toString())))
                    .findFirst()
                    .orElse(null);

            double displayAmount = 0.0;
            int durationMinutes = 0;

            if (activeOrder != null) {
                displayAmount = calculateOrderAmountFallback(activeOrder);
                LocalDateTime startTime = getOrderStartTimeFallback(activeOrder);
                durationMinutes = (int) java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
            }

            TableStatus effectiveStatus = table.getStatus();
            if (activeOrder != null) {
                if (activeOrder.getStatus() == OrderStatus.BILLED) {
                    effectiveStatus = TableStatus.PAYMENT_PENDING;
                } else {
                    effectiveStatus = TableStatus.RUNNING;
                }
            }

            VBox tableCard = new VBox();
            tableCard.setMinWidth(120);
            tableCard.prefWidthProperty().bind(tablesContainer.widthProperty().subtract(24).divide(2));
            tableCard.setPrefHeight(175);
            tableCard.getStyleClass().addAll("table-card", effectiveStatus.name().toLowerCase());
            tableCard.setSpacing(6);

            HBox topRow = new HBox();
            topRow.setAlignment(Pos.CENTER_LEFT);

            String titleText = table.getTableNumber();
            if (activeOrder != null && activeOrder.getMergedTableIds() != null
                    && !activeOrder.getMergedTableIds().isEmpty()) {
                List<String> mergedNames = new ArrayList<>();
                String[] ids = activeOrder.getMergedTableIds().split(",");
                for (String idStr : ids) {
                    try {
                        UUID tid = UUID.fromString(idStr.trim());
                        tablesList.stream()
                                .filter(t -> t.getId().equals(tid))
                                .findFirst()
                                .ifPresent(t -> mergedNames.add(t.getTableNumber()));
                    } catch (Exception ignored) {
                    }
                }
                mergedNames.sort((a, b) -> {
                    try {
                        int num1 = Integer.parseInt(a.replaceAll("\\D+", ""));
                        int num2 = Integer.parseInt(b.replaceAll("\\D+", ""));
                        return Integer.compare(num1, num2);
                    } catch (Exception e) {
                        return a.compareToIgnoreCase(b);
                    }
                });
                titleText = activeOrder.getTableName() + " (" + String.join(" + ", mergedNames) + ")";
            }
            Label nameLabel = new Label("🪑 " + titleText);
            nameLabel.getStyleClass().add("table-title");
            nameLabel.setTooltip(new javafx.scene.control.Tooltip(titleText));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox badge = new HBox();
            badge.getStyleClass().addAll("status-badge", effectiveStatus.name().toLowerCase());
            badge.setAlignment(Pos.CENTER);
            badge.setSpacing(4);

            Region dot = new Region();
            dot.getStyleClass().addAll("badge-dot", effectiveStatus.name().toLowerCase());

            String statusText = switch (effectiveStatus) {
                case AVAILABLE -> "Available";
                case RUNNING -> "Running";
                case PAYMENT_PENDING -> "Running (Pre-billed)";
                case PAID -> "Paid";
            };
            Label statusLabel = new Label(statusText);
            statusLabel.getStyleClass().add("badge-text");

            badge.getChildren().addAll(dot, statusLabel);
            topRow.getChildren().addAll(nameLabel, spacer, badge);

            String metaText;
            if (effectiveStatus == TableStatus.AVAILABLE) {
                metaText = (table.getAreaName() != null ? table.getAreaName() : "Indoor");
            } else {
                String waiterName = switch (table.getTableNumber()) {
                    case "T-02", "Table 2" -> "Waiter: Amit";
                    case "T-03", "Table 3" -> "Waiter: Priya";
                    case "T-06", "Table 6" -> "Waiter: Raju";
                    default -> "Waiter: Staff";
                };
                metaText = waiterName;
            }
            Label metaLabel = new Label(metaText);
            metaLabel.getStyleClass().add("table-meta");

            Region verticalSpacer = new Region();
            VBox.setVgrow(verticalSpacer, Priority.ALWAYS);

            HBox bottomRow = new HBox();
            bottomRow.setAlignment(Pos.CENTER_LEFT);

            if (effectiveStatus == TableStatus.AVAILABLE) {
                Label actionLabel = new Label("Tap to start order");
                actionLabel.getStyleClass().add("table-action-text");
                bottomRow.getChildren().add(actionLabel);
            } else {
                Label priceLabel = new Label("₹" + (int) displayAmount);
                priceLabel.getStyleClass().add("table-price");

                Region bottomSpacer = new Region();
                HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

                HBox timerBox = new HBox();
                timerBox.setAlignment(Pos.CENTER_RIGHT);
                timerBox.setSpacing(3);
                Label clockIcon = new Label("🕒");
                clockIcon.setStyle("-fx-font-size: 11px;");
                Label durationLabel = new Label(durationMinutes + " min");
                durationLabel.getStyleClass().add("table-timer");
                timerBox.getChildren().addAll(clockIcon, durationLabel);

                bottomRow.getChildren().addAll(priceLabel, bottomSpacer, timerBox);
            }

            tableCard.setCursor(javafx.scene.Cursor.HAND);
            tableCard.setOnMouseClicked(clickEvent -> {
                try {
                    // Clear L1 cache to evict cached state
                    try {
                        jakarta.persistence.EntityManager em = applicationContext.getBean(jakarta.persistence.EntityManager.class);
                        if (em != null) {
                            em.clear();
                        }
                    } catch (Exception ex) {
                        // ignore
                    }

                    selectedOrderType = OrderType.DINE_IN;

                    // Find active running order for the table
                    var tableActiveOrders = orderRepository.findByRestaurantIdAndStatusNotIn(
                            TenantContext.getRestaurantId(),
                            java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));
                    Order runningOrder = tableActiveOrders.stream()
                            .filter(o -> (o.getTableId() != null && o.getTableId().equals(finalTable.getId())) ||
                                    (o.getMergedTableIds() != null
                                            && o.getMergedTableIds().contains(finalTable.getId().toString())))
                            .findFirst()
                            .orElse(null);

                    if (runningOrder != null) {
                        currentActiveOrder = runningOrder;
                        currentDiningTable = tableRepository.findById(runningOrder.getTableId()).orElse(finalTable);
                        loadOrderItemsToCart(runningOrder);
                        clearPaymentModeSelection();
                    } else {
                        currentDiningTable = finalTable;
                        currentActiveOrder = null;
                        cartList.clear();
                        clearPaymentModeSelection();
                    }

                    // Update meta views
                    if (dineInTableChip != null) {
                        dineInTableChip.setText(runningOrder != null && runningOrder.getMergedTableIds() != null
                                && !runningOrder.getMergedTableIds().isEmpty() ? runningOrder.getTableName()
                                        : finalTable.getTableNumber());
                    }
                    if (dineInOrderNumLabel != null) {
                        dineInOrderNumLabel
                                .setText(runningOrder != null ? runningOrder.getOrderNumber() : "New Session");
                    }
                    if (runningOrder != null) {
                        currentCustomerPhone = runningOrder.getCustomerPhone() != null
                                ? runningOrder.getCustomerPhone()
                                : "";
                        currentCustomerName = runningOrder.getCustomerName() != null
                                ? runningOrder.getCustomerName()
                                : "";
                        currentCustomerNotes = runningOrder.getNotes() != null ? runningOrder.getNotes() : "";
                    } else {
                        currentCustomerPhone = "";
                        currentCustomerName = "";
                        currentCustomerNotes = "";
                    }
                    updateCustomerButtonState();

                    // Handle switching visual tabs
                    handleTabSelection(OrderType.DINE_IN);

                    if (cartItemsContainer != null) {
                        cartItemsContainer.setDisable(false);
                    }

                    // Switch to the billing view
                    showBillingView();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            tableCard.getChildren().addAll(topRow, metaLabel, verticalSpacer, bottomRow);
            tablesContainer.getChildren().add(tableCard);
        }
    }

    /**
     * Opens the billing view pre-loaded with an existing pickup/delivery order.
     * Mirrors the table-card click flow but for non-dine-in order types.
     */
    public void openOrderInBilling(Order order) {
        Platform.runLater(() -> {
            try {
                currentActiveOrder = order;
                selectedOrderType = order.getType() != null ? order.getType() : OrderType.PICK_UP;
                if (selectedOrderType == OrderType.DINE_IN && order.getTableId() != null) {
                    currentDiningTable = tableRepository.findById(order.getTableId()).orElse(null);
                    if (dineInTableChip != null && currentDiningTable != null) {
                        dineInTableChip
                                .setText(order.getMergedTableIds() != null && !order.getMergedTableIds().isEmpty()
                                        ? order.getTableName()
                                        : currentDiningTable.getTableNumber());
                    }
                } else {
                    currentDiningTable = null;
                    if (dineInTableChip != null) {
                        dineInTableChip.setText("Select Table");
                    }
                }

                // Load the order's items into the cart
                loadOrderItemsToCart(order);

                // Populate customer fields based on order type
                currentCustomerName = order.getCustomerName() != null ? order.getCustomerName() : "";
                currentCustomerPhone = order.getCustomerPhone() != null ? order.getCustomerPhone() : "";
                currentCustomerNotes = order.getNotes() != null ? order.getNotes() : "";

                // Switch the billing pane tab to the correct order type
                handleTabSelection(selectedOrderType);

                clearPaymentModeSelection();

                // Populate delivery/pickup name and phone fields if present
                if (selectedOrderType == OrderType.DELIVERY) {
                    if (deliveryNameField != null)
                        deliveryNameField.setText(currentCustomerName);
                    if (deliveryPhoneField != null)
                        deliveryPhoneField.setText(currentCustomerPhone);
                    if (deliveryAddressField != null) {
                        String addr = order.getTableName() != null ? order.getTableName().replace("Delivery: ", "")
                                : "";
                        deliveryAddressField.setText(addr);
                    }
                } else if (selectedOrderType == OrderType.PICK_UP) {
                    if (pickupNameField != null)
                        pickupNameField.setText(currentCustomerName);
                    if (pickupPhoneField != null)
                        pickupPhoneField.setText(currentCustomerPhone);
                }

                updateCustomerButtonState();

                // Allow item editing only if order is not fully paid/settled
                if (cartItemsContainer != null) {
                    cartItemsContainer.setDisable(order.getStatus() == OrderStatus.PAID);
                }

                showBillingView();
            } catch (Exception ex) {
                System.out.println("Failed to open order in billing: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    public void loadRunningOrders() {
        UUID restaurantId = TenantContext.getRestaurantId();
        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(restaurantId);
            try {
                var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                        java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));

                List<Order> localOrders = activeOrders.stream()
                        .filter(o -> o.getSource() == null || !(o.getSource().toUpperCase().contains("ZOMATO") ||
                                o.getSource().toUpperCase().contains("SWIGGY") ||
                                o.getSource().toUpperCase().contains("ONLINE")))
                        .sorted((o1, o2) -> getOrderStartTimeFallback(o2).compareTo(getOrderStartTimeFallback(o1)))
                        .toList();

                List<UUID> orderIds = localOrders.stream().map(Order::getId).toList();
                List<KOT> allKots = orderIds.isEmpty() ? new ArrayList<>() : kotRepository.findByOrderIdIn(orderIds);
                java.util.Map<UUID, List<KOT>> kotsMap = allKots.stream()
                        .collect(java.util.stream.Collectors.groupingBy(KOT::getOrderId));

                Platform.runLater(() -> {
                    try {
                        renderRunningOrdersSync(localOrders, kotsMap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } finally {
                TenantContext.clear();
            }
        });
    }

    private void renderRunningOrdersSync(List<Order> localOrders, java.util.Map<UUID, List<KOT>> kotsMap) {
        runningOrdersContainer.getChildren().clear();

        if (localOrders.isEmpty()) {
            Label emptyLabel = new Label("No running orders");
            emptyLabel.setStyle(
                    "-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-alignment: center; -fx-padding: 20 0 0 0;");
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            runningOrdersContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Order order : localOrders) {
            List<KOT> kots = kotsMap.getOrDefault(order.getId(), new ArrayList<>());
            List<String> itemStrings = new ArrayList<>();
            for (KOT kot : kots) {
                for (KOTItem item : kot.getItems()) {
                    itemStrings.add(item.getItemName() + " x" + item.getQuantity());
                }
            }
            String itemsSummary = itemStrings.isEmpty() ? "No items" : String.join(", ", itemStrings);

            LocalDateTime startTime = getOrderStartTimeFallback(order);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            String timeStr = startTime.format(formatter);

            String typeStr = order.getType() != null ? order.getType().name() : "DINE_IN";
            double amount = calculateOrderAmountFallback(order);

            String statusStr = switch (order.getStatus()) {
                case OPEN -> "Pending";
                case BILLED -> "Delayed";
                default -> "Pending";
            };

            long minutes = java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
            long seconds = java.time.Duration.between(startTime, LocalDateTime.now()).toSeconds() % 60;
            String durationStr = String.format("%02d:%02d", minutes, seconds);

            addOrderCard(
                    order,
                    order.getOrderNumber() != null ? order.getOrderNumber()
                            : "#" + order.getId().toString().substring(0, 4),
                    order.getCustomerName() != null && !order.getCustomerName().trim().isEmpty()
                            ? order.getCustomerName()
                            : (order.getTableName() != null ? order.getTableName() : "Walk-in"),
                    itemsSummary,
                    timeStr,
                    typeStr,
                    amount,
                    statusStr,
                    durationStr);
        }
    }

    private void addOrderCard(Order sourceOrder, String id, String customer, String items, String time, String type,
            double amount,
            String status, String duration) {
        HBox card = new HBox();
        card.getStyleClass().add("order-card");
        if (type != null) {
            card.getStyleClass().add(type.toLowerCase());
        }
        card.setAlignment(Pos.CENTER_LEFT);
        card.setSpacing(10);

        VBox iconBox = new VBox();
        iconBox.getStyleClass().add("order-icon-badge");
        if (type != null) {
            iconBox.getStyleClass().add(type.toLowerCase());
        }
        iconBox.setAlignment(Pos.CENTER);

        javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
        path.setFill(javafx.scene.paint.Color.TRANSPARENT);
        path.setStrokeWidth(2.0);
        path.setScaleX(0.8);
        path.setScaleY(0.8);

        String strokeColor = "#10B981"; // default green for PICK_UP
        String svgContent = "M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z M3 6h18 M16 10a4 4 0 01-8 0"; // Shopping
                                                                                                          // Bag

        if ("DINE_IN".equalsIgnoreCase(type)) {
            strokeColor = "#3B82F6"; // Blue
            svgContent = "M3 10h18v2H3zm3 2h2v7H6zm10 0h2v7h-2z M12 3a3 3 0 100 6 3 3 0 000-6z"; // Table
        } else if ("DELIVERY".equalsIgnoreCase(type)) {
            strokeColor = "#EA580C"; // Orange
            svgContent = "M2 17h2a3 3 0 006 0h4a3 3 0 006 0h2v-6l-3-4H9l-2 4H2zm3 1.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm11 0a1.5 1.5 0 110-3 1.5 1.5 0 010 3z M12 8h3v3h-3z"; // Truck
        }

        path.setContent(svgContent);
        path.setStroke(javafx.scene.paint.Color.web(strokeColor));
        iconBox.getChildren().add(path);

        VBox centerBox = new VBox();
        centerBox.setSpacing(2);
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setSpacing(6);

        Label customerLabel = new Label(customer);
        customerLabel.getStyleClass().add("order-customer");

        Label ticketLabel = new Label(id);
        ticketLabel.getStyleClass().add("order-ticket-id");

        titleBox.getChildren().addAll(customerLabel, ticketLabel);

        Label itemsLabel = new Label(items);
        itemsLabel.getStyleClass().add("order-items");
        itemsLabel.setMaxWidth(Double.MAX_VALUE);
        itemsLabel.setWrapText(true);

        HBox metaBox = new HBox();
        metaBox.setAlignment(Pos.CENTER_LEFT);
        metaBox.setSpacing(8);

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("order-time");

        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().addAll("order-type-badge", type.toLowerCase());

        metaBox.getChildren().addAll(timeLabel, typeLabel);

        centerBox.getChildren().addAll(titleBox, itemsLabel, metaBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightBox = new VBox();
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setSpacing(3);
        rightBox.setMinWidth(85);
        rightBox.setPrefWidth(85);

        Label priceLabel = new Label("₹" + (int) amount);
        priceLabel.getStyleClass().add("order-price");

        HBox badge = new HBox();
        badge.getStyleClass().addAll("status-badge", status.toLowerCase());
        badge.setAlignment(Pos.CENTER);
        badge.setSpacing(4);

        Region dot = new Region();
        dot.getStyleClass().addAll("badge-dot", status.toLowerCase());

        Label badgeLabel = new Label(status);
        badgeLabel.getStyleClass().add("badge-text");

        badge.getChildren().addAll(dot, badgeLabel);

        HBox timerBox = new HBox();
        timerBox.setAlignment(Pos.CENTER_RIGHT);
        timerBox.setSpacing(3);
        Label clock = new Label("🕒");
        clock.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748B;");
        Label durationLabel = new Label(duration);
        durationLabel.getStyleClass().add("order-timer");
        timerBox.getChildren().addAll(clock, durationLabel);

        rightBox.getChildren().addAll(priceLabel, badge, timerBox);

        card.getChildren().addAll(iconBox, centerBox, spacer, rightBox);

        // Make card clickable: open billing view with this order loaded
        card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(e -> openOrderInBilling(sourceOrder));

        runningOrdersContainer.getChildren().add(card);
    }

    private void loadPlatformOrders() {
        UUID restaurantId = TenantContext.getRestaurantId();
        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(restaurantId);
            try {
                var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                        java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));

                List<Order> platformOrders = activeOrders.stream()
                        .filter(o -> o.getSource() != null && (o.getSource().toUpperCase().contains("ZOMATO") ||
                                o.getSource().toUpperCase().contains("SWIGGY") ||
                                o.getSource().toUpperCase().contains("ONLINE")))
                        .sorted((o1, o2) -> getOrderStartTimeFallback(o2).compareTo(getOrderStartTimeFallback(o1)))
                        .toList();

                List<UUID> orderIds = platformOrders.stream().map(Order::getId).toList();
                List<KOT> allKots = orderIds.isEmpty() ? new ArrayList<>() : kotRepository.findByOrderIdIn(orderIds);
                java.util.Map<UUID, List<KOT>> kotsMap = allKots.stream()
                        .collect(java.util.stream.Collectors.groupingBy(KOT::getOrderId));

                Platform.runLater(() -> {
                    try {
                        renderPlatformOrdersSync(platformOrders, kotsMap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } finally {
                TenantContext.clear();
            }
        });
    }

    private void renderPlatformOrdersSync(List<Order> platformOrders, java.util.Map<UUID, List<KOT>> kotsMap) {
        platformOrdersContainer.getChildren().clear();

        if (platformOrders.isEmpty()) {
            Label emptyLabel = new Label("No active platform orders");
            emptyLabel.setStyle(
                    "-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-alignment: center; -fx-padding: 20 0 0 0;");
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            platformOrdersContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Order order : platformOrders) {
            List<KOT> kots = kotsMap.getOrDefault(order.getId(), new ArrayList<>());
            List<String> itemStrings = new ArrayList<>();
            for (KOT kot : kots) {
                for (KOTItem item : kot.getItems()) {
                    itemStrings.add(item.getItemName() + " x" + item.getQuantity());
                }
            }
            String itemsSummary = itemStrings.isEmpty() ? "No items" : String.join(", ", itemStrings);

            LocalDateTime startTime = getOrderStartTimeFallback(order);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            String timeStr = startTime.format(formatter);

            String sourceStr = order.getSource() != null ? order.getSource().toUpperCase() : "ZOMATO";
            double amount = calculateOrderAmountFallback(order);

            String statusStr = switch (order.getStatus()) {
                case OPEN -> "Pending";
                case BILLED -> "Delayed";
                default -> "Pending";
            };

            long minutes = java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
            long seconds = java.time.Duration.between(startTime, LocalDateTime.now()).toSeconds() % 60;
            String durationStr = String.format("%02d:%02d", minutes, seconds);

            addPlatformOrderCard(
                    order,
                    order.getOrderNumber() != null ? order.getOrderNumber()
                            : "#" + order.getId().toString().substring(0, 4),
                    order.getCustomerName() != null && !order.getCustomerName().trim().isEmpty()
                            ? order.getCustomerName()
                            : "Online Customer",
                    itemsSummary,
                    timeStr,
                    sourceStr,
                    amount,
                    statusStr,
                    durationStr);
        }
    }

    private void addPlatformOrderCard(Order sourceOrder, String id, String customer, String items, String time,
            String source,
            double amount, String status, String duration) {
        HBox card = new HBox();
        card.getStyleClass().add("order-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setSpacing(10);

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);

        Label iconLabel = new Label();
        iconLabel.getStyleClass().add("platform-icon");

        if (source.contains("ZOMATO")) {
            iconLabel.setText("z");
            iconLabel.getStyleClass().add("zomato");
            iconLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center;");
        } else if (source.contains("SWIGGY")) {
            iconLabel.setText("s");
            iconLabel.getStyleClass().add("swiggy");
            iconLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center;");
        } else {
            iconLabel.getStyleClass().add("other");
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent("M3 3h7v7H3zm11 0h7v7h-7zm0 11h7v7h-7zM3 14h7v7H3z");
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStroke(javafx.scene.paint.Color.web("#059669"));
            path.setStrokeWidth(2.0);
            path.setScaleX(0.75);
            path.setScaleY(0.75);
            iconLabel.setGraphic(path);
        }
        iconBox.getChildren().add(iconLabel);

        VBox centerBox = new VBox();
        centerBox.setSpacing(2);
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        Label titleLabel = new Label(id + " · " + customer);
        titleLabel.getStyleClass().add("order-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setWrapText(false);

        Label itemsLabel = new Label(items);
        itemsLabel.getStyleClass().add("order-items");
        itemsLabel.setMaxWidth(Double.MAX_VALUE);
        itemsLabel.setWrapText(false);

        Label metaLabel = new Label(
                time + " · " + (source.substring(0, 1).toUpperCase() + source.substring(1).toLowerCase()));
        metaLabel.getStyleClass().add("order-meta");
        metaLabel.setMaxWidth(Double.MAX_VALUE);
        metaLabel.setWrapText(false);

        centerBox.getChildren().addAll(titleLabel, itemsLabel, metaLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightBox = new VBox();
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setSpacing(3);
        rightBox.setMinWidth(85);
        rightBox.setPrefWidth(85);

        Label priceLabel = new Label("₹" + (int) amount);
        priceLabel.getStyleClass().add("order-price");

        HBox badge = new HBox();
        badge.getStyleClass().addAll("status-badge", status.toLowerCase());
        badge.setAlignment(Pos.CENTER);
        badge.setSpacing(4);

        Region dot = new Region();
        dot.getStyleClass().addAll("badge-dot", status.toLowerCase());

        Label badgeLabel = new Label(status);
        badgeLabel.getStyleClass().add("badge-text");

        badge.getChildren().addAll(dot, badgeLabel);

        HBox timerBox = new HBox();
        timerBox.setAlignment(Pos.CENTER_RIGHT);
        timerBox.setSpacing(3);
        Label clock = new Label("🕒");
        clock.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748B;");
        Label durationLabel = new Label(duration);
        durationLabel.getStyleClass().add("order-timer");
        timerBox.getChildren().addAll(clock, durationLabel);

        rightBox.getChildren().addAll(priceLabel, badge, timerBox);

        card.getChildren().addAll(iconBox, centerBox, spacer, rightBox);

        // Make card clickable: open billing view with this order loaded
        card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(e -> openOrderInBilling(sourceOrder));

        platformOrdersContainer.getChildren().add(card);
    }

    private void loadStockOut() {
        UUID restaurantId = TenantContext.getRestaurantId();
        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(restaurantId);
            try {
                var menuItems = menuRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
                List<MenuItem> unavailableItems = menuItems.stream()
                        .filter(item -> !item.isAvailable())
                        .toList();

                Platform.runLater(() -> {
                    try {
                        renderStockOutSync(unavailableItems);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } finally {
                TenantContext.clear();
            }
        });
    }

    private void renderStockOutSync(List<MenuItem> unavailableItems) {
        stockOutContainer.getChildren().clear();

        if (unavailableItems.isEmpty()) {
            Label emptyLabel = new Label("All items are in stock");
            emptyLabel.setStyle(
                    "-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-style: italic; -fx-padding: 10 0 0 0;");
            stockOutContainer.getChildren().add(emptyLabel);
            return;
        }

        for (MenuItem item : unavailableItems) {
            addStockRow(item.getName(), 0);
        }
    }

    private void addStockRow(String name, int quantity) {
        HBox row = new HBox();
        row.getStyleClass().add("stock-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("stock-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badgeLabel = new Label(String.valueOf(quantity));
        badgeLabel.getStyleClass().add("stock-badge");
        if (quantity == 0) {
            badgeLabel.getStyleClass().add("empty");
        }

        row.getChildren().addAll(nameLabel, spacer, badgeLabel);
        stockOutContainer.getChildren().add(row);
    }

    private void loadPlatformStats() {
        UUID restaurantId = TenantContext.getRestaurantId();
        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(restaurantId);
            try {
                var activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId,
                        java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));

                List<Order> platformActiveOrders = activeOrders.stream()
                        .filter(o -> o.getSource() != null && (o.getSource().toUpperCase().contains("ZOMATO") ||
                                o.getSource().toUpperCase().contains("SWIGGY") ||
                                o.getSource().toUpperCase().contains("ONLINE")))
                        .toList();

                java.time.LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();
                // Optimized query: only fetch today's orders
                var todaysOrders = orderRepository.findByRestaurantIdAndStartedAtAfter(restaurantId, startOfToday);
                List<Order> platformTodayOrders = todaysOrders.stream()
                        .filter(o -> o.getSource() != null && (o.getSource().toUpperCase().contains("ZOMATO") ||
                                o.getSource().toUpperCase().contains("SWIGGY") ||
                                o.getSource().toUpperCase().contains("ONLINE")))
                        .toList();

                Platform.runLater(() -> {
                    try {
                        renderPlatformStatsSync(platformActiveOrders, platformTodayOrders);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } finally {
                TenantContext.clear();
            }
        });
    }

    private void renderPlatformStatsSync(List<Order> platformActiveOrders, List<Order> platformTodayOrders) {
        try {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                    .ofPattern("EEEE, d MMMM yyyy · hh:mm a");
            dateTimeLabel.setText(now.format(formatter) + " · Main Outlet · Last updated just now");
        } catch (Exception e) {
            System.out.println("Failed to update date/time: " + e.getMessage());
        }

        try {
            long activeCount = platformActiveOrders.size();
            long pendingCount = platformActiveOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.OPEN)
                    .count();
            long todayCount = platformTodayOrders.size();

            todaysOrdersLabel.setText(String.valueOf(todayCount));
            activeOrdersLabel.setText(String.valueOf(activeCount));
            pendingOrdersLabel.setText(String.valueOf(pendingCount));
        } catch (Exception e) {
            System.out.println("Failed to render platform stats: " + e.getMessage());
        }
    }

    private void cleanupMockOrders() {
        try {
            List<String> mockOrderNumbers = List.of("#1089", "#1088", "#1087", "#1086", "#1085", "#1084", "#1083");
            List<Order> allOrders = orderRepository.findAll();
            for (Order order : allOrders) {
                if (mockOrderNumbers.contains(order.getOrderNumber())) {
                    List<KOT> kots = kotRepository.findByOrderId(order.getId());
                    kotRepository.deleteAll(kots);
                    orderRepository.delete(order);
                    System.out.println("🗑️ Cleaned up mock order: " + order.getOrderNumber());
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to clean up mock orders: " + e.getMessage());
        }
    }

    // --- DATABASE SEEDER FOR MOCK ORDERS ---
    private void ensureMockOrdersExist() {
        try {
            var existingOrders = orderRepository.findAll();
            boolean has1089 = existingOrders.stream().anyMatch(o -> "#1089".equals(o.getOrderNumber()));
            if (!has1089) {
                // Order #1089
                Order o1089 = new Order();
                o1089.setRestaurantId(TenantContext.getRestaurantId());
                o1089.setOrderNumber("#1089");
                o1089.setType(OrderType.DINE_IN);
                o1089.setSource("DIRECT");
                o1089.setStatus(OrderStatus.OPEN);
                o1089.setTableName("Table 12");
                o1089.setCustomerName("Adithyan");
                o1089.setSubTotal(new BigDecimal("830.00"));
                o1089.setCgst(new BigDecimal("20.75"));
                o1089.setSgst(new BigDecimal("20.75"));
                o1089.setGrandTotal(new BigDecimal("871.50"));
                o1089.setStartedAt(LocalDateTime.now().minusMinutes(8));
                o1089 = orderRepository.save(o1089);

                KOT kot1089 = new KOT();
                kot1089.setKotNumber("KOT-1089");
                kot1089.setOrderId(o1089.getId());
                kot1089.setTableId(UUID.randomUUID());
                kot1089.setTableName("Table 12");
                kot1089.setOverallStatus(KOTStatus.PREPARING);
                kot1089.setNotes("Less Spicy on Paneer Tikka. Veg Biryani without onions.");
                kot1089.setRestaurantId(TenantContext.getRestaurantId());

                KOTItem item1 = new KOTItem(UUID.randomUUID(), "Paneer Tikka", 1, "Less Spicy", KOTStatus.PREPARING);
                item1.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item2 = new KOTItem(UUID.randomUUID(), "Veg Biryani", 1, "No Onion", KOTStatus.PREPARING);
                item2.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item3 = new KOTItem(UUID.randomUUID(), "Garlic Naan", 2, "", KOTStatus.PREPARING);
                item3.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item4 = new KOTItem(UUID.randomUUID(), "Masala Papad", 1, "", KOTStatus.PREPARING);
                item4.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item5 = new KOTItem(UUID.randomUUID(), "Coke (Can)", 2, "", KOTStatus.PREPARING);
                item5.setRestaurantId(TenantContext.getRestaurantId());

                kot1089.getItems().addAll(List.of(item1, item2, item3, item4, item5));
                kotRepository.save(kot1089);

                // Order #1088
                Order o1088 = new Order();
                o1088.setRestaurantId(TenantContext.getRestaurantId());
                o1088.setOrderNumber("#1088");
                o1088.setType(OrderType.DELIVERY);
                o1088.setSource("SWIGGY");
                o1088.setStatus(OrderStatus.BILLED);
                o1088.setCustomerName("Neha S.");
                o1088.setSubTotal(new BigDecimal("580.00"));
                o1088.setCgst(new BigDecimal("14.50"));
                o1088.setSgst(new BigDecimal("14.50"));
                o1088.setGrandTotal(new BigDecimal("609.00"));
                o1088.setStartedAt(LocalDateTime.now().minusMinutes(15));
                o1088 = orderRepository.save(o1088);

                KOT kot1088 = new KOT();
                kot1088.setKotNumber("KOT-1088");
                kot1088.setOrderId(o1088.getId());
                kot1088.setTableId(UUID.randomUUID());
                kot1088.setTableName("Swiggy");
                kot1088.setOverallStatus(KOTStatus.READY);
                kot1088.setRestaurantId(TenantContext.getRestaurantId());

                KOTItem item6 = new KOTItem(UUID.randomUUID(), "Veg Biryani", 1, "", KOTStatus.READY);
                item6.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item7 = new KOTItem(UUID.randomUUID(), "Paneer Butter Masala", 1, "", KOTStatus.READY);
                item7.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item8 = new KOTItem(UUID.randomUUID(), "Garlic Naan", 1, "", KOTStatus.READY);
                item8.setRestaurantId(TenantContext.getRestaurantId());

                kot1088.getItems().addAll(List.of(item6, item7, item8));
                kotRepository.save(kot1088);

                // Order #1087
                Order o1087 = new Order();
                o1087.setRestaurantId(TenantContext.getRestaurantId());
                o1087.setOrderNumber("#1087");
                o1087.setType(OrderType.PICK_UP);
                o1087.setSource("DIRECT");
                o1087.setStatus(OrderStatus.PAID);
                o1087.setTableName("Counter 2");
                o1087.setCustomerName("Vikram");
                o1087.setSubTotal(new BigDecimal("340.00"));
                o1087.setCgst(new BigDecimal("8.50"));
                o1087.setSgst(new BigDecimal("8.50"));
                o1087.setGrandTotal(new BigDecimal("357.00"));
                o1087.setStartedAt(LocalDateTime.now().minusMinutes(25));
                o1087 = orderRepository.save(o1087);

                KOT kot1087 = new KOT();
                kot1087.setKotNumber("KOT-1087");
                kot1087.setOrderId(o1087.getId());
                kot1087.setTableId(UUID.randomUUID());
                kot1087.setTableName("Counter 2");
                kot1087.setOverallStatus(KOTStatus.SERVED);
                kot1087.setRestaurantId(TenantContext.getRestaurantId());

                KOTItem item9 = new KOTItem(UUID.randomUUID(), "Hakka Noodles", 1, "", KOTStatus.SERVED);
                item9.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item10 = new KOTItem(UUID.randomUUID(), "French Fries", 1, "", KOTStatus.SERVED);
                item10.setRestaurantId(TenantContext.getRestaurantId());

                kot1087.getItems().addAll(List.of(item9, item10));
                kotRepository.save(kot1087);

                // Order #1086
                Order o1086 = new Order();
                o1086.setRestaurantId(TenantContext.getRestaurantId());
                o1086.setOrderNumber("#1086");
                o1086.setType(OrderType.DINE_IN);
                o1086.setSource("DIRECT");
                o1086.setStatus(OrderStatus.OPEN);
                o1086.setTableName("Table 3");
                o1086.setCustomerName("Arjun");
                o1086.setSubTotal(new BigDecimal("1060.00"));
                o1086.setCgst(new BigDecimal("26.50"));
                o1086.setSgst(new BigDecimal("26.50"));
                o1086.setGrandTotal(new BigDecimal("1113.00"));
                o1086.setStartedAt(LocalDateTime.now().minusMinutes(12));
                o1086 = orderRepository.save(o1086);

                KOT kot1086 = new KOT();
                kot1086.setKotNumber("KOT-1086");
                kot1086.setOrderId(o1086.getId());
                kot1086.setTableId(UUID.randomUUID());
                kot1086.setTableName("Table 3");
                kot1086.setOverallStatus(KOTStatus.PREPARING);
                kot1086.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item11 = new KOTItem(UUID.randomUUID(), "Butter Chicken Masala", 2, "", KOTStatus.PREPARING);
                item11.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item12 = new KOTItem(UUID.randomUUID(), "Garlic Naan", 1, "", KOTStatus.PREPARING);
                item12.setRestaurantId(TenantContext.getRestaurantId());
                kot1086.getItems().addAll(List.of(item11, item12));
                kotRepository.save(kot1086);

                // Order #1085
                Order o1085 = new Order();
                o1085.setRestaurantId(TenantContext.getRestaurantId());
                o1085.setOrderNumber("#1085");
                o1085.setType(OrderType.DELIVERY);
                o1085.setSource("ZOMATO");
                o1085.setStatus(OrderStatus.OPEN);
                o1085.setCustomerName("Sarah M.");
                o1085.setSubTotal(new BigDecimal("620.00"));
                o1085.setCgst(new BigDecimal("15.50"));
                o1085.setSgst(new BigDecimal("15.50"));
                o1085.setGrandTotal(new BigDecimal("651.00"));
                o1085.setStartedAt(LocalDateTime.now().minusMinutes(32));
                o1085 = orderRepository.save(o1085);

                KOT kot1085 = new KOT();
                kot1085.setKotNumber("KOT-1085");
                kot1085.setOrderId(o1085.getId());
                kot1085.setTableId(UUID.randomUUID());
                kot1085.setTableName("Zomato");
                kot1085.setOverallStatus(KOTStatus.PREPARING);
                kot1085.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item13 = new KOTItem(UUID.randomUUID(), "Chilli Paneer", 2, "", KOTStatus.PREPARING);
                item13.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item14 = new KOTItem(UUID.randomUUID(), "French Fries", 1, "", KOTStatus.PREPARING);
                item14.setRestaurantId(TenantContext.getRestaurantId());
                kot1085.getItems().addAll(List.of(item13, item14));
                kotRepository.save(kot1085);

                // Order #1084
                Order o1084 = new Order();
                o1084.setRestaurantId(TenantContext.getRestaurantId());
                o1084.setOrderNumber("#1084");
                o1084.setType(OrderType.DINE_IN);
                o1084.setSource("DIRECT");
                o1084.setStatus(OrderStatus.PAID);
                o1084.setTableName("Table 5");
                o1084.setCustomerName("Rahul");
                o1084.setSubTotal(new BigDecimal("840.00"));
                o1084.setCgst(new BigDecimal("21.00"));
                o1084.setSgst(new BigDecimal("21.00"));
                o1084.setGrandTotal(new BigDecimal("882.00"));
                o1084.setStartedAt(LocalDateTime.now().minusMinutes(48));
                o1084 = orderRepository.save(o1084);

                KOT kot1084 = new KOT();
                kot1084.setKotNumber("KOT-1084");
                kot1084.setOrderId(o1084.getId());
                kot1084.setTableId(UUID.randomUUID());
                kot1084.setTableName("Table 5");
                kot1084.setOverallStatus(KOTStatus.SERVED);
                kot1084.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item15 = new KOTItem(UUID.randomUUID(), "Veg Manchurian", 3, "", KOTStatus.SERVED);
                item15.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item16 = new KOTItem(UUID.randomUUID(), "French Fries", 2, "", KOTStatus.SERVED);
                item16.setRestaurantId(TenantContext.getRestaurantId());
                kot1084.getItems().addAll(List.of(item15, item16));
                kotRepository.save(kot1084);

                // Order #1083
                Order o1083 = new Order();
                o1083.setRestaurantId(TenantContext.getRestaurantId());
                o1083.setOrderNumber("#1083");
                o1083.setType(OrderType.PICK_UP);
                o1083.setSource("DIRECT");
                o1083.setStatus(OrderStatus.CANCELLED);
                o1083.setTableName("Counter 1");
                o1083.setCustomerName("Deepa");
                o1083.setSubTotal(new BigDecimal("200.00"));
                o1083.setCgst(new BigDecimal("5.00"));
                o1083.setSgst(new BigDecimal("5.00"));
                o1083.setGrandTotal(new BigDecimal("210.00"));
                o1083.setStartedAt(LocalDateTime.now().minusHours(1));
                o1083 = orderRepository.save(o1083);

                KOT kot1083 = new KOT();
                kot1083.setKotNumber("KOT-1083");
                kot1083.setOrderId(o1083.getId());
                kot1083.setTableId(UUID.randomUUID());
                kot1083.setTableName("Counter 1");
                kot1083.setOverallStatus(KOTStatus.CANCELLED);
                kot1083.setRestaurantId(TenantContext.getRestaurantId());
                KOTItem item17 = new KOTItem(UUID.randomUUID(), "Spring Rolls", 1, "", KOTStatus.CANCELLED);
                item17.setRestaurantId(TenantContext.getRestaurantId());
                kot1083.getItems().add(item17);
                kotRepository.save(kot1083);
            }
        } catch (Exception e) {
            System.out.println("Failed to seed mock orders: " + e.getMessage());
        }
    }

    @FXML
    public void loadOrdersToUi() {
        UUID restaurantId = TenantContext.getRestaurantId();
        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(restaurantId);
            try {
                // Fetch only today's orders for the restaurant
                LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();
                var allOrders = orderRepository.findByRestaurantIdAndStartedAtAfter(restaurantId, startOfToday).stream()
                        .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt())) // descending
                        .toList();

                Platform.runLater(() -> {
                    try {
                        renderOrdersToUiSync(allOrders);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.out.println("Failed to load orders: " + e.getMessage());
            } finally {
                TenantContext.clear();
            }
        });
    }

    private void renderOrdersToUiSync(List<Order> allOrders) {
        try {
            // Compute Stats
            long totalOrders = allOrders.size();
            long preparing = 0;
            long ready = 0;
            long delayed = 0;
            long cancelled = 0;

            long dineInCount = 0;
            long deliveryCount = 0;
            long takeawayCount = 0;

            for (Order o : allOrders) {
                // Count by type
                if (o.getType() == OrderType.DINE_IN)
                    dineInCount++;
                else if (o.getType() == OrderType.DELIVERY)
                    deliveryCount++;
                else if (o.getType() == OrderType.PICK_UP)
                    takeawayCount++;

                // Count by status
                if (o.getStatus() == OrderStatus.CANCELLED) {
                    cancelled++;
                } else if (o.getStatus() == OrderStatus.PAID) {
                    // Settled
                } else {
                    // Active
                    if (o.getStartedAt() != null
                            && o.getStartedAt().isBefore(LocalDateTime.now().minusMinutes(15))) {
                        delayed++;
                    }
                    if (o.getStatus() == OrderStatus.BILLED) {
                        ready++;
                    } else {
                        preparing++;
                    }
                }
            }

            // Update Stat labels
            if (ordersTotalLabel != null)
                ordersTotalLabel.setText(String.valueOf(totalOrders));
            if (ordersPreparingLabel != null)
                ordersPreparingLabel.setText(String.valueOf(preparing));
            if (ordersReadyLabel != null)
                ordersReadyLabel.setText(String.valueOf(ready));
            if (ordersDelayedLabel != null)
                ordersDelayedLabel.setText(String.valueOf(delayed));
            if (ordersCancelledLabel != null)
                ordersCancelledLabel.setText(String.valueOf(cancelled));

            // Update Tab badges
            if (ordersTabAllBadge != null)
                ordersTabAllBadge.setText(String.valueOf(totalOrders));
            if (ordersTabDineInBadge != null)
                ordersTabDineInBadge.setText(String.valueOf(dineInCount));
            if (ordersTabDeliveryBadge != null)
                ordersTabDeliveryBadge.setText(String.valueOf(deliveryCount));
            if (ordersTabTakeawayBadge != null)
                ordersTabTakeawayBadge.setText(String.valueOf(takeawayCount));

            // Filter list
            List<Order> filtered = new ArrayList<>();
            for (Order o : allOrders) {
                // 1. Tab Filter
                if ("DINE_IN".equals(ordersActiveTab) && o.getType() != OrderType.DINE_IN)
                    continue;
                if ("DELIVERY".equals(ordersActiveTab) && o.getType() != OrderType.DELIVERY)
                    continue;
                if ("TAKEAWAY".equals(ordersActiveTab) && o.getType() != OrderType.PICK_UP)
                    continue;

                // 2. Search query filter
                if (!ordersSearchQuery.isEmpty()) {
                    boolean matchNum = o.getOrderNumber() != null
                            && o.getOrderNumber().toLowerCase().contains(ordersSearchQuery);
                    boolean matchName = o.getCustomerName() != null
                            && o.getCustomerName().toLowerCase().contains(ordersSearchQuery);
                    boolean matchTbl = o.getTableName() != null
                            && o.getTableName().toLowerCase().contains(ordersSearchQuery);
                    if (!matchNum && !matchName && !matchTbl)
                        continue;
                }

                // 3. Status dropdown filter
                if (!"All Statuses".equals(ordersStatusFilter)) {
                    String st = getMappedStatusString(o);
                    if (!ordersStatusFilter.equalsIgnoreCase(st))
                        continue;
                }

                filtered.add(o);
            }

            // Rebuild Left side table list
            if (ordersListContainer != null) {
                ordersListContainer.getChildren().clear();

                if (filtered.isEmpty()) {
                    Label emptyLabel = new Label("No orders found");
                    emptyLabel.setStyle(
                            "-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-padding: 20 0 0 0; -fx-alignment: center;");
                    emptyLabel.setMaxWidth(Double.MAX_VALUE);
                    ordersListContainer.getChildren().add(emptyLabel);
                } else {
                    for (Order order : filtered) {
                        GridPane row = createOrderRow(order);
                        ordersListContainer.getChildren().add(row);
                    }
                }
            }

            // Handle detail pane selection
            if (selectedOrder == null && !filtered.isEmpty()) {
                selectedOrder = filtered.get(0);
            }
            if (selectedOrder != null) {
                final UUID selId = selectedOrder.getId();
                Order freshOrder = allOrders.stream().filter(o -> o.getId().equals(selId)).findFirst()
                        .orElse(selectedOrder);
                selectedOrder = freshOrder;
                loadOrderDetails(selectedOrder);
            } else {
                if (orderDetailsPane != null) {
                    orderDetailsPane.setVisible(false);
                    orderDetailsPane.setManaged(false);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to load orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getMappedStatusString(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED)
            return "Cancelled";
        if (order.getStatus() == OrderStatus.PAID)
            return "Settled";
        if (order.getStatus() == OrderStatus.BILLED)
            return "Ready";
        return "Preparing";
    }

    private String getTimeElapsed(LocalDateTime startedAt) {
        if (startedAt == null)
            return "Just now";
        long mins = java.time.Duration.between(startedAt, LocalDateTime.now()).toMinutes();
        if (mins < 1)
            return "Just now";
        if (mins < 60)
            return mins + " mins ago";
        long hours = mins / 60;
        if (hours == 1)
            return "1 hour ago";
        return hours + " hours ago";
    }

    private GridPane createOrderRow(Order order) {
        GridPane row = new GridPane();
        row.getStyleClass().add("order-row");
        row.setHgap(10);
        row.setPadding(new Insets(12, 10, 12, 10));
        row.setCursor(javafx.scene.Cursor.HAND);

        // Column Constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(12.0);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(22.0);
        col2.setHgrow(Priority.ALWAYS);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(23.0);
        col3.setHgrow(Priority.ALWAYS);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(18.0);
        col4.setHgrow(Priority.ALWAYS);
        ColumnConstraints col5 = new ColumnConstraints();
        col5.setPercentWidth(15.0);
        col5.setHgrow(Priority.ALWAYS);
        ColumnConstraints col6 = new ColumnConstraints();
        col6.setPercentWidth(10.0);
        col6.setHgrow(Priority.ALWAYS);
        row.getColumnConstraints().addAll(col1, col2, col3, col4, col5, col6);

        // Selected State highlight
        if (selectedOrder != null && selectedOrder.getId().equals(order.getId())) {
            row.getStyleClass().add("selected");
        }

        // Col 1: Order ID
        Label idLbl = new Label(order.getOrderNumber() != null ? order.getOrderNumber()
                : "#" + order.getId().toString().substring(0, 4));
        idLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 14px;");
        GridPane.setColumnIndex(idLbl, 0);

        // Col 2: Source / Table
        HBox sourceBox = new HBox();
        sourceBox.setAlignment(Pos.CENTER_LEFT);
        sourceBox.setSpacing(8);

        SVGPath sourceIcon = getSourceIcon(order.getType(), order.getSource());
        Label srcLbl = new Label();
        srcLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 13px;");

        if (order.getType() == OrderType.DINE_IN) {
            srcLbl.setText(order.getTableName() != null ? order.getTableName() : "Table");
        } else if (order.getSource() != null && order.getSource().toUpperCase().contains("SWIGGY")) {
            srcLbl.setText("Swiggy");
        } else if (order.getSource() != null && order.getSource().toUpperCase().contains("ZOMATO")) {
            srcLbl.setText("Zomato");
        } else {
            srcLbl.setText(order.getTableName() != null ? order.getTableName() : "Takeaway");
        }
        sourceBox.getChildren().addAll(sourceIcon, srcLbl);
        GridPane.setColumnIndex(sourceBox, 1);

        // Col 3: Customer Name
        Label custLbl = new Label(order.getCustomerName() != null ? order.getCustomerName() : "Walk-in");
        custLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
        GridPane.setColumnIndex(custLbl, 2);

        // Col 4: Status
        HBox statusPill = new HBox();
        statusPill.setAlignment(Pos.CENTER_LEFT);

        HBox pillBg = new HBox();
        pillBg.setAlignment(Pos.CENTER);
        pillBg.setSpacing(5);
        pillBg.setPadding(new Insets(3, 8, 3, 8));

        Region dot = new Region();
        dot.setPrefWidth(6);
        dot.setPrefHeight(6);
        dot.setMinWidth(6);
        dot.setMinHeight(6);
        dot.setMaxWidth(6);
        dot.setMaxHeight(6);
        dot.setStyle("-fx-background-radius: 50%;");

        Label statusLbl = new Label();
        statusLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        String statusStr = getMappedStatusString(order);
        if ("Preparing".equals(statusStr)) {
            pillBg.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 12px;");
            dot.setStyle("-fx-background-color: #1D4ED8; -fx-background-radius: 50%;");
            statusLbl.setText("Preparing");
            statusLbl.setStyle("-fx-text-fill: #1D4ED8; -fx-font-weight: bold;");
        } else if ("Ready".equals(statusStr)) {
            pillBg.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 12px;");
            dot.setStyle("-fx-background-color: #D97706; -fx-background-radius: 50%;");
            statusLbl.setText("Ready");
            statusLbl.setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
        } else if ("Settled".equals(statusStr)) {
            pillBg.setStyle("-fx-background-color: #E6F4EA; -fx-background-radius: 12px;");
            dot.setStyle("-fx-background-color: #15803D; -fx-background-radius: 50%;");
            statusLbl.setText("Settled");
            statusLbl.setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold;");
        } else {
            pillBg.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 12px;");
            dot.setStyle("-fx-background-color: #B91C1C; -fx-background-radius: 50%;");
            statusLbl.setText("Cancelled");
            statusLbl.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");
        }
        pillBg.getChildren().addAll(dot, statusLbl);
        statusPill.getChildren().add(pillBg);
        GridPane.setColumnIndex(statusPill, 3);

        // Col 5: Time Elapsed
        HBox timeBox = new HBox();
        timeBox.setAlignment(Pos.CENTER_LEFT);
        timeBox.setSpacing(5);
        Label clockIcon = new Label("🕒");
        clockIcon.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        Label elapsedLbl = new Label(getTimeElapsed(order.getStartedAt()));
        elapsedLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        timeBox.getChildren().addAll(clockIcon, elapsedLbl);
        GridPane.setColumnIndex(timeBox, 4);

        // Col 6: Amount
        Label amtLbl = new Label(
                String.format("₹%.2f", order.getGrandTotal() != null ? order.getGrandTotal().doubleValue() : 0.0));
        amtLbl.setAlignment(Pos.CENTER_RIGHT);
        amtLbl.setMaxWidth(Double.MAX_VALUE);
        amtLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 14px;");
        GridPane.setColumnIndex(amtLbl, 5);

        row.getChildren().addAll(idLbl, sourceBox, custLbl, statusPill, timeBox, amtLbl);

        row.setOnMouseClicked(e -> {
            selectedOrder = order;
            loadOrdersToUi();
        });

        if (selectedOrder != null && selectedOrder.getId().equals(order.getId())) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                row.setStyle(
                        "-fx-background-color: #FEE2E2; -fx-border-color: transparent transparent #F1F5F9 #EF4444; -fx-border-width: 0 0 1 4;");
            }
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            row.setStyle(
                    "-fx-background-color: #FEF2F2; -fx-border-color: transparent transparent #F1F5F9 #EF4444; -fx-border-width: 0 0 1 4;");
        }

        return row;
    }

    private SVGPath getSourceIcon(OrderType type, String source) {
        SVGPath svg = new SVGPath();
        svg.setStyle(
                "-fx-fill: transparent; -fx-stroke: #475569; -fx-stroke-width: 1.6; -fx-stroke-line-cap: round; -fx-stroke-line-join: round;");

        if (type == OrderType.DINE_IN) {
            // Dine-in chair/table icon
            svg.setContent("M4 19V11H2V9h20v2h-2v8h-2v-8H6v8H4zm6-14h4v4h-4V5z");
        } else if (source != null
                && (source.toUpperCase().contains("SWIGGY") || source.toUpperCase().contains("ZOMATO"))) {
            // Delivery scooter carrying box icon
            svg.setContent(
                    "M19 15c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zM5 8h4v4H5zm14-1H11v3h2.2l1.8 2.5h3v-2.3L15.6 9c-.3-.6-1-.9-1.6-.9zM3 13h15v1H3z");
        } else {
            // Takeaway shopping bag icon
            svg.setContent(
                    "M19 6h-2c0-2.76-2.24-5-5-5S7 3.24 7 6H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H9c0-1.66 1.34-3 3-3zm7 17H5V8h14v12z");
        }
        return svg;
    }

    @FXML
    public void handleOrdersTabAll() {
        setOrdersActiveTab("ALL", ordersTabAllBtn);
    }

    @FXML
    public void handleOrdersTabDineIn() {
        setOrdersActiveTab("DINE_IN", ordersTabDineInBtn);
    }

    @FXML
    public void handleOrdersTabDelivery() {
        setOrdersActiveTab("DELIVERY", ordersTabDeliveryBtn);
    }

    @FXML
    public void handleOrdersTabTakeaway() {
        setOrdersActiveTab("PICK_UP", ordersTabTakeawayBtn);
    }

    private void setOrdersActiveTab(String tab, Button activeBtn) {
        ordersActiveTab = tab;
        ordersTabAllBtn.getStyleClass().remove("active");
        ordersTabDineInBtn.getStyleClass().remove("active");
        ordersTabDeliveryBtn.getStyleClass().remove("active");
        ordersTabTakeawayBtn.getStyleClass().remove("active");
        activeBtn.getStyleClass().add("active");
        loadOrdersToUi();
    }

    private void loadOrderDetails(Order order) {
        Platform.runLater(() -> {
            try {
                if (orderDetailsPane == null)
                    return;

                // If selecting a different order, exit edit mode
                if (isEditingOrderItems && currentEditingOrder != null
                        && !currentEditingOrder.getId().equals(order.getId())) {
                    isEditingOrderItems = false;
                }

                orderDetailsPane.setVisible(true);
                orderDetailsPane.setManaged(true);

                // Set Header details
                if (detOrderNumberLabel != null) {
                    String orderNumStr = "";
                    if (order.getOrderNumber() != null) {
                        orderNumStr = order.getOrderNumber();
                        if (!orderNumStr.startsWith("#")) {
                            orderNumStr = "#" + orderNumStr;
                        }
                    } else {
                        orderNumStr = "#" + order.getId().toString().substring(0, 4);
                    }
                    detOrderNumberLabel.setText("Order " + orderNumStr);
                }

                String orderTypeStr = formatOrderType(order.getType());
                if (detOrderMetaLabel != null) {
                    String timeElapsed = getTimeElapsed(order.getStartedAt());
                    if (timeElapsed.endsWith(" ago")) {
                        timeElapsed = timeElapsed.substring(0, timeElapsed.length() - 4);
                    }
                    detOrderMetaLabel.setText("Placed at " + timeElapsed + " · " + orderTypeStr);
                }

                // Header status badge
                if (detStatusBadgeLabel != null) {
                    String statusStr = getMappedStatusString(order);
                    detStatusBadgeLabel.setText("• " + statusStr);
                    detStatusBadgeLabel.getStyleClass().removeAll("preparing", "ready", "settled", "cancelled");
                    detStatusBadgeLabel.getStyleClass().add(statusStr.toLowerCase());
                }

                // Customer info
                if (detCustomerNameLabel != null) {
                    detCustomerNameLabel
                            .setText(order.getCustomerName() != null ? order.getCustomerName() : "Walk-in Customer");
                }

                if (detTableWaiterLabel != null) {
                    String tableWaiter = "";
                    if (order.getType() == OrderType.DINE_IN) {
                        tableWaiter = (order.getTableName() != null ? order.getTableName() : "Table")
                                + " · Rajesh (Staff)";
                    } else {
                        tableWaiter = (order.getSource() != null ? order.getSource() : "Walk-in") + " · Counter";
                    }
                    detTableWaiterLabel.setText(tableWaiter);
                }

                // Calculations
                if (isEditingOrderItems) {
                    updateEditableTotals();
                } else {
                    if (detSubtotalLabel != null)
                        detSubtotalLabel.setText(String.format("₹%.2f",
                                order.getSubTotal() != null ? order.getSubTotal().doubleValue() : 0.0));
                    if (detCgstLabel != null)
                        detCgstLabel.setText(
                                String.format("₹%.2f", order.getCgst() != null ? order.getCgst().doubleValue() : 0.0));
                    if (detSgstLabel != null)
                        detSgstLabel.setText(
                                String.format("₹%.2f", order.getSgst() != null ? order.getSgst().doubleValue() : 0.0));
                    if (detGrandTotalLabel != null)
                        detGrandTotalLabel.setText(String.format("₹%.2f",
                                order.getGrandTotal() != null ? order.getGrandTotal().doubleValue() : 0.0));
                }

                // Kitchen Notes block
                List<KOT> kots = kotRepository.findByOrderId(order.getId());
                String notes = "";
                for (KOT kot : kots) {
                    if (kot.getNotes() != null && !kot.getNotes().trim().isEmpty()) {
                        notes = kot.getNotes();
                        break;
                    }
                }
                if (detKitchenNotesContainer != null && detKitchenNotesLabel != null) {
                    if (order.getStatus() == OrderStatus.CANCELLED && order.getCancelReason() != null
                            && !order.getCancelReason().trim().isEmpty()) {
                        detKitchenNotesLabel.setText("Reason: " + order.getCancelReason());
                        detKitchenNotesContainer.setVisible(true);
                        detKitchenNotesContainer.setManaged(true);
                        detKitchenNotesContainer.setStyle(
                                "-fx-background-color: #FEF2F2; -fx-border-color: #FCA5A5 transparent #FCA5A5 transparent; -fx-border-width: 1 0 1 0;");
                        detKitchenNotesLabel
                                .setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 15px;");
                    } else if (!notes.isEmpty()) {
                        detKitchenNotesLabel.setText(notes);
                        detKitchenNotesContainer.setVisible(true);
                        detKitchenNotesContainer.setManaged(true);
                        detKitchenNotesContainer.setStyle(""); // Reset to default CSS
                        detKitchenNotesLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #1E293B;");
                    } else {
                        detKitchenNotesContainer.setVisible(false);
                        detKitchenNotesContainer.setManaged(false);
                        detKitchenNotesContainer.setStyle(""); // Reset to default CSS
                        detKitchenNotesLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #1E293B;");
                    }
                }

                // Show/hide search box based on editing mode
                if (detEditSearchBox != null) {
                    detEditSearchBox.setVisible(isEditingOrderItems);
                    detEditSearchBox.setManaged(isEditingOrderItems);
                }

                // Items list populate
                if (detItemsContainer != null) {
                    detItemsContainer.getChildren().clear();

                    if (isEditingOrderItems) {
                        for (EditableItem tempItem : tempEditingItems) {
                            HBox itemRow = createEditableDetailItemRow(tempItem);
                            detItemsContainer.getChildren().add(itemRow);
                        }
                    } else {
                        boolean hasItems = false;
                        for (KOT kot : kots) {
                            for (KOTItem item : kot.getItems()) {
                                hasItems = true;
                                HBox itemRow = createDetailItemRow(item, kot, order);
                                detItemsContainer.getChildren().add(itemRow);
                            }
                        }

                        // Fallback to show items if no KOT/Items found
                        if (!hasItems) {
                            Label fallbackLbl = new Label("No items in this order.");
                            fallbackLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-alignment: center;");
                            detItemsContainer.getChildren().add(fallbackLbl);
                        }
                    }
                }

                // Footer Action button logic based on status and edit mode
                if (isEditingOrderItems) {
                    // Hide original buttons
                    if (detPrintKotBtn != null) {
                        detPrintKotBtn.getParent().setVisible(false);
                        detPrintKotBtn.getParent().setManaged(false);
                    }
                    if (detActionButton != null) {
                        detActionButton.setVisible(false);
                        detActionButton.setManaged(false);
                    }
                    // Show edit actions
                    if (detEditActionsContainer != null) {
                        detEditActionsContainer.setVisible(true);
                        detEditActionsContainer.setManaged(true);
                    }
                } else {
                    // Show original buttons
                    if (detPrintKotBtn != null) {
                        detPrintKotBtn.getParent().setVisible(true);
                        detPrintKotBtn.getParent().setManaged(true);
                        detPrintKotBtn.setDisable(order.getStatus() == OrderStatus.CANCELLED);
                    }
                    if (detEditItemsBtn != null) {
                        detEditItemsBtn.setDisable(order.getStatus() == OrderStatus.CANCELLED);
                        detEditItemsBtn.setOnAction(e -> startEditingOrder(order));
                    }

                    if (detActionButton != null) {
                        String statusStr = getMappedStatusString(order);
                        if ("Preparing".equals(statusStr)) {
                            detActionButton.setText("Mark as Ready");
                            detActionButton.setVisible(true);
                            detActionButton.setManaged(true);
                            detActionButton.setOnAction(e -> handleMarkAsReadyClick(order));
                        } else if ("Ready".equals(statusStr)) {
                            detActionButton.setText("Settle Order");
                            detActionButton.setVisible(true);
                            detActionButton.setManaged(true);
                            detActionButton.setOnAction(e -> handleSettleOrderClick(order));
                        } else {
                            detActionButton.setVisible(false);
                            detActionButton.setManaged(false);
                        }
                    }
                    // Hide edit actions
                    if (detEditActionsContainer != null) {
                        detEditActionsContainer.setVisible(false);
                        detEditActionsContainer.setManaged(false);
                    }
                }

            } catch (Exception e) {
                System.out.println("Failed to load order details: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private String formatOrderType(OrderType type) {
        if (type == OrderType.DINE_IN)
            return "Dine-In";
        if (type == OrderType.DELIVERY)
            return "Delivery";
        if (type == OrderType.PICK_UP)
            return "Takeaway";
        return "Order";
    }

    private HBox createDetailItemRow(KOTItem item, KOT kot, Order order) {
        String name = item.getItemName();
        int qty = item.getQuantity();
        String instructions = item.getSpecialInstruction();

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle(
                "-fx-border-color: transparent transparent #E2E8F0 transparent; -fx-border-style: dashed; -fx-border-width: 0 0 1 0;");

        // Column 1: Item Name & Instruction (width HGrow)
        VBox nameBox = new VBox();
        nameBox.setSpacing(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label nameLbl;
        if (item.getItemStatus() == KOTStatus.CANCELLED) {
            nameLbl = new Label(name + " (Cancelled)");
            nameLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-style: italic;");
        } else {
            nameLbl = new Label(name);
            nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 14px;");
        }
        nameBox.getChildren().add(nameLbl);

        if (instructions != null && !instructions.trim().isEmpty()) {
            Label insLbl = new Label(instructions);
            if (item.getItemStatus() == KOTStatus.CANCELLED) {
                insLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-style: italic;");
            } else {
                insLbl.setStyle("-fx-text-fill: #B91C1C; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
            nameBox.getChildren().add(insLbl);
        }

        // Column 2: Qty (width 50)
        Label qtyLbl = new Label(String.valueOf(qty));
        qtyLbl.setMinWidth(28);
        qtyLbl.setMinHeight(28);
        qtyLbl.setMaxWidth(28);
        qtyLbl.setMaxHeight(28);
        qtyLbl.setAlignment(Pos.CENTER);
        if (item.getItemStatus() == KOTStatus.CANCELLED) {
            qtyLbl.setStyle(
                    "-fx-background-color: #F1F5F9; -fx-background-radius: 6px; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-font-size: 13px;");
        } else {
            qtyLbl.setStyle(
                    "-fx-background-color: #F1F5F9; -fx-background-radius: 6px; -fx-text-fill: #1E293B; -fx-font-weight: bold; -fx-font-size: 13px;");
        }

        HBox qtyBox = new HBox(qtyLbl);
        qtyBox.setPrefWidth(50);
        qtyBox.setAlignment(Pos.CENTER);

        // Column 3: Price (width 90)
        double price = getMenuItemPriceFallback(name);
        Label priceLbl = new Label(String.format("₹%.2f", price));
        priceLbl.setPrefWidth(90);
        priceLbl.setAlignment(Pos.CENTER_RIGHT);
        if (item.getItemStatus() == KOTStatus.CANCELLED) {
            priceLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
        } else {
            priceLbl.setStyle("-fx-text-fill: #5C7C6D; -fx-font-size: 13px;");
        }

        // Column 4: Amount (width 90)
        Label amtLbl = new Label(String.format("₹%.2f", price * qty));
        amtLbl.setPrefWidth(90);
        amtLbl.setAlignment(Pos.CENTER_RIGHT);
        if (item.getItemStatus() == KOTStatus.CANCELLED) {
            amtLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            amtLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 14px;");
        }

        row.getChildren().addAll(nameBox, qtyBox, priceLbl, amtLbl);

        // Column 5: Cancel Action Button (only if order is active and item is not
        // already cancelled)
        boolean canCancelItem = order.getStatus() != OrderStatus.PAID
                && order.getStatus() != OrderStatus.CANCELLED
                && item.getItemStatus() != KOTStatus.CANCELLED;

        if (canCancelItem) {
            Button cancelItemBtn = new Button();
            cancelItemBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 0 0 10;");

            SVGPath crossIcon = new SVGPath();
            crossIcon.setContent("M18 6L6 18M6 6l12 12");
            crossIcon.setStyle("-fx-stroke: #EF4444; -fx-stroke-width: 2; -fx-stroke-line-cap: round;");
            cancelItemBtn.setGraphic(crossIcon);

            cancelItemBtn.setOnAction(e -> performItemCancellation(item, kot, order));

            row.getChildren().add(cancelItemBtn);
        } else {
            // Spacer to keep layout aligned
            Region spacer = new Region();
            spacer.setPrefWidth(24);
            row.getChildren().add(spacer);
        }

        return row;
    }

    private KOTStatus resolveOverallKotStatus(KOT kot) {
        boolean allCancelled = true;
        boolean anyPreparing = false;
        boolean anyPending = false;
        boolean anyReady = false;
        boolean anyServed = false;

        for (KOTItem item : kot.getItems()) {
            if (item.getItemStatus() != KOTStatus.CANCELLED) {
                allCancelled = false;
                if (item.getItemStatus() == KOTStatus.PREPARING) {
                    anyPreparing = true;
                } else if (item.getItemStatus() == KOTStatus.PENDING) {
                    anyPending = true;
                } else if (item.getItemStatus() == KOTStatus.READY) {
                    anyReady = true;
                } else if (item.getItemStatus() == KOTStatus.SERVED) {
                    anyServed = true;
                }
            }
        }

        if (allCancelled) {
            return KOTStatus.CANCELLED;
        } else if (anyPending) {
            return KOTStatus.PENDING;
        } else if (anyPreparing) {
            return KOTStatus.PREPARING;
        } else if (anyReady) {
            return KOTStatus.READY;
        } else if (anyServed) {
            return KOTStatus.SERVED;
        }
        return KOTStatus.PENDING;
    }

    private void performItemCancellation(KOTItem item, KOT kot, Order order) {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Cancel Item");
        dialog.setHeaderText("Reason for cancelling " + item.getItemName() + "?");

        javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialogPane.setStyle("-fx-background-color: #FFFFFF;");

        javafx.scene.control.Button okBtn = (javafx.scene.control.Button) dialogPane
                .lookupButton(javafx.scene.control.ButtonType.OK);
        okBtn.setText("Cancel Item");
        okBtn.setDisable(true);
        okBtn.setStyle(
                "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-opacity: 0.5;");

        javafx.scene.control.Button cancelBtn = (javafx.scene.control.Button) dialogPane
                .lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelBtn.setText("Go Back");
        cancelBtn.setStyle(
                "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 6px;");

        javafx.scene.control.TextArea reasonInput = new javafx.scene.control.TextArea();
        reasonInput.setPromptText("Enter reason for item cancellation...");
        reasonInput.setPrefRowCount(3);
        reasonInput.setWrapText(true);
        reasonInput.setStyle(
                "-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 6 8; -fx-font-size: 14px;");

        reasonInput.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean isBlank = newVal == null || newVal.trim().isEmpty();
            okBtn.setDisable(isBlank);
            if (isBlank) {
                okBtn.setStyle(
                        "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-opacity: 0.5;");
            } else {
                okBtn.setStyle(
                        "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-opacity: 1.0; -fx-cursor: hand;");
            }
        });

        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));
        vbox.getChildren().add(reasonInput);
        dialogPane.setContent(vbox);

        java.util.Optional<javafx.scene.control.ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            String reason = reasonInput.getText().trim();
            try {
                item.setItemStatus(KOTStatus.CANCELLED);
                if (item.getSpecialInstruction() == null || item.getSpecialInstruction().isEmpty()) {
                    item.setSpecialInstruction("Cancelled: " + reason);
                } else {
                    item.setSpecialInstruction(item.getSpecialInstruction() + " (Cancelled: " + reason + ")");
                }

                kot.setOverallStatus(resolveOverallKotStatus(kot));
                KOT savedKot = kotRepository.save(kot);

                if (messagingTemplate != null) {
                    try {
                        String kitchenTopic = "/topic/kitchen/" + order.getRestaurantId();
                        messagingTemplate.convertAndSend(kitchenTopic, savedKot);
                    } catch (Exception wsEx) {
                        System.err.println("Failed to broadcast updated KOT: " + wsEx.getMessage());
                    }
                }

                double itemPrice = getMenuItemPriceFallback(item.getItemName());
                java.math.BigDecimal itemCost = java.math.BigDecimal.valueOf(itemPrice * item.getQuantity());

                java.math.BigDecimal newSubTotal = order.getSubTotal().subtract(itemCost);
                if (newSubTotal.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    newSubTotal = java.math.BigDecimal.ZERO;
                }
                order.setSubTotal(newSubTotal);

                java.math.BigDecimal taxRate = java.math.BigDecimal.valueOf(0.025);
                java.math.BigDecimal cgst = newSubTotal.multiply(taxRate);
                java.math.BigDecimal sgst = newSubTotal.multiply(taxRate);
                order.setCgst(cgst);
                order.setSgst(sgst);
                order.setGrandTotal(newSubTotal.add(cgst).add(sgst));

                List<KOT> allKots = kotRepository.findByOrderId(order.getId());
                boolean hasActiveItems = false;
                for (KOT k : allKots) {
                    for (KOTItem ki : k.getItems()) {
                        if (ki.getItemStatus() != KOTStatus.CANCELLED) {
                            hasActiveItems = true;
                            break;
                        }
                    }
                }

                boolean orderFullyCancelled = !hasActiveItems;
                if (orderFullyCancelled) {
                    order.setStatus(OrderStatus.CANCELLED);
                    order.setCancelReason("All items were cancelled");
                }
                orderRepository.save(order);

                if (orderFullyCancelled && order.getType() == OrderType.DINE_IN && currentDiningTable != null) {
                    currentDiningTable.setStatus(TableStatus.AVAILABLE);
                    currentDiningTable.setTotalAmount(0.0);
                    currentDiningTable.setDurationMinutes(0);
                    tableRepository.save(currentDiningTable);

                    if (messagingTemplate != null) {
                        try {
                            String tableTopic = "/topic/tables/" + order.getRestaurantId();
                            java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
                            wsPayload.put("id", currentDiningTable.getId().toString());
                            wsPayload.put("status", TableStatus.AVAILABLE.name());
                            wsPayload.put("totalAmount", 0.0);
                            wsPayload.put("durationMinutes", 0);
                            messagingTemplate.convertAndSend(tableTopic, wsPayload);

                            // Cascade to other merged tables
                            if (order.getMergedTableIds() != null && !order.getMergedTableIds().trim().isEmpty()) {
                                for (String idStr : order.getMergedTableIds().split(",")) {
                                    UUID otherId = UUID.fromString(idStr.trim());
                                    if (!otherId.equals(currentDiningTable.getId())) {
                                        DiningTable otherTable = tableRepository.findById(otherId).orElse(null);
                                        if (otherTable != null) {
                                            otherTable.setStatus(TableStatus.AVAILABLE);
                                            otherTable.setTotalAmount(0.0);
                                            otherTable.setDurationMinutes(0);
                                            tableRepository.save(otherTable);

                                            java.util.Map<String, Object> otherPayload = new java.util.HashMap<>();
                                            otherPayload.put("id", otherTable.getId().toString());
                                            otherPayload.put("status", TableStatus.AVAILABLE.name());
                                            otherPayload.put("totalAmount", 0.0);
                                            otherPayload.put("durationMinutes", 0);
                                            messagingTemplate.convertAndSend(tableTopic, otherPayload);
                                        }
                                    }
                                }
                            }
                        } catch (Exception wsEx) {
                            System.err.println("Failed to broadcast table status: " + wsEx.getMessage());
                        }
                    }

                    cartList.clear();
                    selectedCartItem = null;
                    discountValue = 0.0;
                    isDiscountPercentage = false;
                    if (receivedAmountField != null)
                        receivedAmountField.setText("0.00");
                    activeModifiers.clear();
                    populateModifiersUi();
                    currentCustomerPhone = "";
                    currentCustomerName = "";
                    currentCustomerNotes = "";
                    currentActiveOrder = null;
                    currentDiningTable = null;
                    updateCustomerButtonState();
                    updateCalculations();
                    populateCart();
                } else if (order.getType() == OrderType.DINE_IN && currentDiningTable != null) {
                    currentDiningTable.setTotalAmount(order.getGrandTotal().doubleValue());
                    tableRepository.save(currentDiningTable);

                    if (messagingTemplate != null) {
                        try {
                            String tableTopic = "/topic/tables/" + order.getRestaurantId();
                            java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
                            wsPayload.put("id", currentDiningTable.getId().toString());
                            wsPayload.put("status", currentDiningTable.getStatus().name());
                            wsPayload.put("totalAmount", order.getGrandTotal().doubleValue());
                            if (order.getStartedAt() != null) {
                                wsPayload.put("durationMinutes", (int) java.time.Duration
                                        .between(order.getStartedAt(), java.time.LocalDateTime.now()).toMinutes());
                            } else {
                                wsPayload.put("durationMinutes", 0);
                            }
                            messagingTemplate.convertAndSend(tableTopic, wsPayload);
                        } catch (Exception wsEx) {
                            System.err.println("Failed to broadcast table status: " + wsEx.getMessage());
                        }
                    }
                }

                loadTablesToUi();
                loadRunningOrders();
                loadPlatformOrders();
                loadOrdersToUi();
                if (!orderFullyCancelled) {
                    loadOrderDetails(order);
                    if (currentActiveOrder != null && currentActiveOrder.getId().equals(order.getId())) {
                        loadOrderItemsToCart(order);
                        populateCart();
                    }
                } else {
                    if (detItemsContainer != null) {
                        detItemsContainer.getChildren().clear();
                    }
                    if (detKitchenNotesContainer != null) {
                        detKitchenNotesContainer.setVisible(false);
                        detKitchenNotesContainer.setManaged(false);
                    }
                }

                showAlert("Item Cancelled", item.getItemName() + " has been cancelled.");

            } catch (Exception ex) {
                showAlert("Error", "Could not cancel item: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private double getMenuItemPriceFallback(String itemName) {
        try {
            var itemOpt = menuRepository.findAll().stream().filter(item -> item.getName().equalsIgnoreCase(itemName))
                    .findFirst();
            if (itemOpt.isPresent()) {
                return itemOpt.get().getPrice().doubleValue();
            }
        } catch (Exception e) {
        }

        if (itemName.contains("Paneer Tikka"))
            return 220.00;
        if (itemName.contains("Veg Biryani"))
            return 280.00;
        if (itemName.contains("Garlic Naan"))
            return 90.00;
        if (itemName.contains("Masala Papad"))
            return 60.00;
        if (itemName.contains("Coke"))
            return 45.00;
        if (itemName.contains("Butter Chicken"))
            return 480.00;
        if (itemName.contains("Paneer Butter"))
            return 260.00;
        if (itemName.contains("Hakka Noodles"))
            return 200.00;
        if (itemName.contains("French Fries"))
            return 120.00;
        if (itemName.contains("Chilli Paneer"))
            return 240.00;
        if (itemName.contains("Veg Manchurian"))
            return 200.00;
        if (itemName.contains("Spring Rolls"))
            return 160.00;

        return 100.00;
    }

    private void handleMarkAsReadyClick(Order order) {
        try {
            order.setStatus(OrderStatus.BILLED);
            orderRepository.save(order);

            List<KOT> kots = kotRepository.findByOrderId(order.getId());
            for (KOT kot : kots) {
                kot.setOverallStatus(KOTStatus.READY);
                for (KOTItem item : kot.getItems()) {
                    item.setItemStatus(KOTStatus.READY);
                }
                kotRepository.save(kot);
            }

            showAlert("Order Status Updated", "Order " + order.getOrderNumber() + " is marked as READY.");
            loadOrdersToUi();
        } catch (Exception e) {
            showAlert("Error Updating Order", "Failed to update status: " + e.getMessage());
        }
    }

    private void handleSettleOrderClick(Order order) {
        try {
            order.setStatus(OrderStatus.PAID);
            order.setSettledAt(LocalDateTime.now());
            orderRepository.save(order);

            List<KOT> kots = kotRepository.findByOrderId(order.getId());
            for (KOT kot : kots) {
                kot.setOverallStatus(KOTStatus.SERVED);
                for (KOTItem item : kot.getItems()) {
                    item.setItemStatus(KOTStatus.SERVED);
                }
                kotRepository.save(kot);
            }

            showAlert("Order Settled", "Order " + order.getOrderNumber() + " has been settled successfully.");
            loadOrdersToUi();
        } catch (Exception e) {
            showAlert("Error Settling Order", "Failed to settle: " + e.getMessage());
        }
    }

    // State for order editing
    private boolean isEditingOrderItems = false;
    private Order currentEditingOrder = null;
    private List<EditableItem> tempEditingItems = new ArrayList<>();
    private javafx.scene.control.ContextMenu searchSuggestionsPopup = null;

    private static class EditableItem {
        UUID kotItemId; // null if newly added
        UUID kotId; // KOT to which this item belongs
        String name;
        int quantity;
        BigDecimal unitPrice;
        String instructions;

        EditableItem(UUID kotItemId, UUID kotId, String name, int quantity, BigDecimal unitPrice, String instructions) {
            this.kotItemId = kotItemId;
            this.kotId = kotId;
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.instructions = instructions;
        }
    }

    private void startEditingOrder(Order order) {
        currentEditingOrder = order;
        isEditingOrderItems = true;
        tempEditingItems.clear();

        try {
            List<KOT> kots = kotRepository.findByOrderId(order.getId());
            for (KOT kot : kots) {
                for (KOTItem item : kot.getItems()) {
                    double price = getMenuItemPriceFallback(item.getItemName());
                    tempEditingItems.add(new EditableItem(
                            item.getId(),
                            kot.getId(),
                            item.getItemName(),
                            item.getQuantity(),
                            BigDecimal.valueOf(price),
                            item.getSpecialInstruction()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadOrderDetails(order);
    }

    private HBox createEditableDetailItemRow(EditableItem editableItem) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle(
                "-fx-border-color: transparent transparent #E2E8F0 transparent; -fx-border-style: dashed; -fx-border-width: 0 0 1 0;");

        // Column 1: Item Name & Instruction (width HGrow)
        VBox nameBox = new VBox();
        nameBox.setSpacing(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label nameLbl = new Label(editableItem.name);
        nameLbl.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 14px; -fx-font-family: 'Outfit', 'Inter', 'Segoe UI', sans-serif;");
        nameBox.getChildren().add(nameLbl);

        if (editableItem.instructions != null && !editableItem.instructions.trim().isEmpty()) {
            Label insLbl = new Label(editableItem.instructions);
            insLbl.setStyle(
                    "-fx-text-fill: #B91C1C; -fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Outfit', 'Inter', 'Segoe UI', sans-serif;");
            nameBox.getChildren().add(insLbl);
        }

        // Column 2: Qty Adjusters (width 120 approx)
        HBox qtyAdjusterBox = new HBox();
        qtyAdjusterBox.setSpacing(10);
        qtyAdjusterBox.setAlignment(Pos.CENTER);

        // Minus Button
        Button minusBtn = new Button();
        minusBtn.setText("—");
        minusBtn.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #1E293B; -fx-font-weight: bold; -fx-font-size: 11px; -fx-min-width: 28; -fx-min-height: 28; -fx-max-width: 28; -fx-max-height: 28; -fx-cursor: hand;");

        Label qtyLbl = new Label(String.valueOf(editableItem.quantity));
        qtyLbl.setStyle(
                "-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 14px; -fx-min-width: 20; -fx-alignment: center; -fx-font-family: 'Outfit', 'Inter', 'Segoe UI', sans-serif;");

        // Plus Button
        Button plusBtn = new Button();
        plusBtn.setText("+");
        plusBtn.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #1E293B; -fx-font-weight: bold; -fx-font-size: 11px; -fx-min-width: 28; -fx-min-height: 28; -fx-max-width: 28; -fx-max-height: 28; -fx-cursor: hand;");

        qtyAdjusterBox.getChildren().addAll(minusBtn, qtyLbl, plusBtn);

        // Column 3: Unit Price (width 90)
        Label priceLbl = new Label(String.format("₹%.2f", editableItem.unitPrice.doubleValue()));
        priceLbl.setPrefWidth(90);
        priceLbl.setAlignment(Pos.CENTER_RIGHT);
        priceLbl.setStyle(
                "-fx-text-fill: #5C8271; -fx-font-size: 13px; -fx-font-family: 'Outfit', 'Inter', 'Segoe UI', sans-serif;");

        // Column 4: Total Amount (width 90)
        Label amtLbl = new Label(String.format("₹%.2f", editableItem.unitPrice.doubleValue() * editableItem.quantity));
        amtLbl.setPrefWidth(90);
        amtLbl.setAlignment(Pos.CENTER_RIGHT);
        amtLbl.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 14px; -fx-font-family: 'Outfit', 'Inter', 'Segoe UI', sans-serif;");

        // Column 5: Trash Button (width 40)
        Button trashBtn = new Button();
        trashBtn.setStyle(
                "-fx-background-color: transparent; -fx-padding: 0; -fx-min-width: 32; -fx-min-height: 32; -fx-cursor: hand;");
        SVGPath trashIcon = new SVGPath();
        trashIcon.setContent("M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2");
        trashIcon.setStyle("-fx-fill: transparent; -fx-stroke: #EF4444; -fx-stroke-width: 1.6;");
        trashIcon.setScaleX(0.95);
        trashIcon.setScaleY(0.95);
        trashBtn.setGraphic(trashIcon);

        trashBtn.setOnAction(e -> {
            tempEditingItems.remove(editableItem);
            detItemsContainer.getChildren().remove(row);
            updateEditableTotals();
        });

        minusBtn.setOnAction(e -> {
            if (editableItem.quantity > 1) {
                editableItem.quantity--;
                qtyLbl.setText(String.valueOf(editableItem.quantity));
                amtLbl.setText(String.format("₹%.2f", editableItem.unitPrice.doubleValue() * editableItem.quantity));
                updateEditableTotals();
            }
        });
        plusBtn.setOnAction(e -> {
            editableItem.quantity++;
            qtyLbl.setText(String.valueOf(editableItem.quantity));
            amtLbl.setText(String.format("₹%.2f", editableItem.unitPrice.doubleValue() * editableItem.quantity));
            updateEditableTotals();
        });

        row.getChildren().addAll(nameBox, qtyAdjusterBox, priceLbl, amtLbl, trashBtn);
        return row;
    }

    private void updateEditableTotals() {
        double subtotal = 0.0;
        for (EditableItem item : tempEditingItems) {
            subtotal += item.unitPrice.doubleValue() * item.quantity;
        }
        double cgst = subtotal * 0.025;
        double sgst = subtotal * 0.025;
        double total = subtotal + cgst + sgst;

        if (detSubtotalLabel != null)
            detSubtotalLabel.setText(String.format("₹%.2f", subtotal));
        if (detCgstLabel != null)
            detCgstLabel.setText(String.format("₹%.2f", cgst));
        if (detSgstLabel != null)
            detSgstLabel.setText(String.format("₹%.2f", sgst));
        if (detGrandTotalLabel != null)
            detGrandTotalLabel.setText(String.format("₹%.2f", total));
    }

    private void showMenuSearchSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (searchSuggestionsPopup != null) {
                searchSuggestionsPopup.hide();
            }
            return;
        }

        List<MenuItem> matching = new ArrayList<>();
        try {
            List<MenuItem> allItems = menuRepository.findAll();
            for (MenuItem item : allItems) {
                if (item.isAvailable() && item.getName().toLowerCase().contains(query.toLowerCase())) {
                    matching.add(item);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch menu items: " + e.getMessage());
        }

        if (matching.isEmpty()) {
            if (searchSuggestionsPopup != null) {
                searchSuggestionsPopup.hide();
            }
            return;
        }

        if (searchSuggestionsPopup == null) {
            searchSuggestionsPopup = new javafx.scene.control.ContextMenu();
        }
        searchSuggestionsPopup.getItems().clear();

        for (MenuItem item : matching) {
            javafx.scene.control.MenuItem popItem = new javafx.scene.control.MenuItem(
                    item.getName() + " - ₹" + item.getPrice());
            popItem.setOnAction(e -> {
                addNewItemToEditList(item);
                detEditSearchField.clear();
            });
            searchSuggestionsPopup.getItems().add(popItem);
        }

        searchSuggestionsPopup.show(detEditSearchField, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void addNewItemToEditList(MenuItem item) {
        for (EditableItem tempItem : tempEditingItems) {
            if (tempItem.name.equalsIgnoreCase(item.getName())) {
                tempItem.quantity++;
                refreshEditItemsUi();
                return;
            }
        }

        tempEditingItems.add(new EditableItem(
                null,
                null,
                item.getName(),
                1,
                item.getPrice(),
                ""));
        refreshEditItemsUi();
    }

    private void refreshEditItemsUi() {
        if (detItemsContainer != null) {
            detItemsContainer.getChildren().clear();
            for (EditableItem tempItem : tempEditingItems) {
                HBox row = createEditableDetailItemRow(tempItem);
                detItemsContainer.getChildren().add(row);
            }
            updateEditableTotals();
        }
    }

    private void handleCancelEditClick() {
        isEditingOrderItems = false;
        if (searchSuggestionsPopup != null) {
            searchSuggestionsPopup.hide();
        }
        loadOrderDetails(currentEditingOrder);
    }

    private void handleSaveChangesClick() {
        if (currentEditingOrder == null)
            return;

        try {
            List<KOT> kots = kotRepository.findByOrderId(currentEditingOrder.getId());
            if (kots.isEmpty()) {
                KOT newKot = new KOT();
                newKot.setOrderId(currentEditingOrder.getId());
                newKot.setKotNumber("KOT-" + currentEditingOrder.getOrderNumber());
                newKot.setTableId(currentEditingOrder.getTableId() != null ? currentEditingOrder.getTableId()
                        : UUID.randomUUID());
                newKot.setTableName(currentEditingOrder.getTableName());
                newKot.setOverallStatus(KOTStatus.PENDING);
                kots.add(newKot);
            }

            KOT primaryKot = kots.get(0);
            primaryKot.getItems().clear();

            double subtotalVal = 0.0;
            for (EditableItem tempItem : tempEditingItems) {
                KOTItem kotItem = new KOTItem();
                UUID menuItemId = null;
                try {
                    MenuItem mi = resolveOrCreateMenuItem(tempItem.name,
                            tempItem.name.substring(0, Math.min(tempItem.name.length(), 3)).toUpperCase(),
                            tempItem.unitPrice.doubleValue(),
                            true,
                            "Starters");
                    menuItemId = mi.getId();
                } catch (Exception e) {
                    menuItemId = UUID.randomUUID();
                }

                kotItem.setMenuItemId(menuItemId);
                kotItem.setItemName(tempItem.name);
                kotItem.setQuantity(tempItem.quantity);
                kotItem.setSpecialInstruction(tempItem.instructions);
                kotItem.setItemStatus(KOTStatus.PENDING);
                primaryKot.getItems().add(kotItem);

                subtotalVal += tempItem.unitPrice.doubleValue() * tempItem.quantity;
            }

            kotRepository.save(primaryKot);
            for (int i = 1; i < kots.size(); i++) {
                KOT otherKot = kots.get(i);
                otherKot.getItems().clear();
                kotRepository.save(otherKot);
            }

            double cgstVal = subtotalVal * 0.025;
            double sgstVal = subtotalVal * 0.025;
            double grandTotalVal = subtotalVal + cgstVal + sgstVal;

            currentEditingOrder.setSubTotal(BigDecimal.valueOf(subtotalVal));
            currentEditingOrder.setCgst(BigDecimal.valueOf(cgstVal));
            currentEditingOrder.setSgst(BigDecimal.valueOf(sgstVal));
            currentEditingOrder.setGrandTotal(BigDecimal.valueOf(grandTotalVal));

            orderRepository.save(currentEditingOrder);

            showAlert("Order Updated", "Order items updated successfully.");

            isEditingOrderItems = false;
            loadOrdersToUi();
            loadOrderDetails(currentEditingOrder);
        } catch (Exception e) {
            showAlert("Error Saving Changes", "Failed to save order changes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- HTML JS BRIDGES FOR LIVE DATABASE SYNC ---

    public class JavaMenuBridge {
        public String getMenuItemsJson() {
            try {
                TenantContext.setRestaurantId(TenantContext.getRestaurantId());
                List<MenuItem> items = getAllMenuItemsForBilling();
                List<java.util.Map<String, Object>> result = new ArrayList<>();
                for (MenuItem item : items) {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("dbId", item.getId().toString());
                    m.put("id", item.getShortCode() != null ? item.getShortCode() : "ITEM");
                    m.put("name", item.getName());
                    m.put("category", item.getCategoryName() != null ? item.getCategoryName() : "Mains");
                    m.put("price", item.getPrice() != null ? item.getPrice().doubleValue() : 0.0);
                    m.put("veg", item.isVeg());
                    m.put("isTodaysMenu", item.isTodaysMenu());
                    m.put("inStock", item.isAvailable());
                    result.add(m);
                }
                return new ObjectMapper().writeValueAsString(result);
            } catch (Exception e) {
                System.out.println("Error getting menu items in bridge: " + e.getMessage());
                return "[]";
            }
        }

        public String saveMenuItemJson(String json) {
            try {
                TenantContext.setRestaurantId(TenantContext.getRestaurantId());
                ObjectMapper mapper = new ObjectMapper();
                java.util.Map<String, Object> map = mapper.readValue(json, java.util.Map.class);

                String dbIdStr = (String) map.get("dbId");
                MenuItem item;
                if (dbIdStr != null && !dbIdStr.isEmpty()) {
                    item = menuRepository.findById(UUID.fromString(dbIdStr)).orElse(new MenuItem());
                } else {
                    item = new MenuItem();
                    item.setRestaurantId(TenantContext.getRestaurantId());
                }

                item.setName((String) map.get("name"));
                item.setShortCode((String) map.get("id"));

                Object priceObj = map.get("price");
                if (priceObj instanceof Number) {
                    item.setPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
                } else if (priceObj instanceof String) {
                    item.setPrice(new BigDecimal((String) priceObj));
                }

                if (map.get("veg") != null) {
                    item.setVeg((Boolean) map.get("veg"));
                }
                if (map.get("isTodaysMenu") != null) {
                    item.setTodaysMenu((Boolean) map.get("isTodaysMenu"));
                }
                if (map.get("inStock") != null) {
                    item.setAvailable((Boolean) map.get("inStock"));
                }

                String categoryName = (String) map.get("category");
                item.setCategoryName(categoryName);

                MenuItem saved = menuRepository.saveAndFlush(item);
                broadcastMenuUpdate(saved);

                refreshMenuAndStockViews();

                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("dbId", saved.getId().toString());
                response.put("id", saved.getShortCode());
                response.put("name", saved.getName());
                response.put("category", saved.getCategoryName());
                response.put("price", saved.getPrice().doubleValue());
                response.put("veg", saved.isVeg());
                response.put("isTodaysMenu", saved.isTodaysMenu());
                response.put("inStock", saved.isAvailable());

                return mapper.writeValueAsString(response);
            } catch (Exception e) {
                System.out.println("Error saving menu item in bridge: " + e.getMessage());
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        }

        public boolean deleteMenuItem(String dbIdStr) {
            try {
                TenantContext.setRestaurantId(TenantContext.getRestaurantId());
                UUID id = UUID.fromString(dbIdStr);
                menuRepository.findById(id).ifPresent(item -> {
                    item.setDeleted(true);
                    item.setTodaysMenu(false); // Signal apps to remove it from display lists
                    MenuItem saved = menuRepository.saveAndFlush(item);
                    broadcastMenuUpdate(saved);
                });
                refreshMenuAndStockViews();
                return true;
            } catch (Exception e) {
                System.out.println("Error deleting menu item in bridge: " + e.getMessage());
                return false;
            }
        }

        public void toggleTodayStatus(String dbIdStr, boolean active) {
            try {
                TenantContext.setRestaurantId(TenantContext.getRestaurantId());
                UUID id = UUID.fromString(dbIdStr);
                menuService.toggleTodaysMenu(id, active);
                refreshMenuAndStockViews();
            } catch (Exception e) {
                System.out.println("Error toggling today status in bridge: " + e.getMessage());
            }
        }

        public void toggleStockStatus(String dbIdStr, boolean inStock) {
            try {
                TenantContext.setRestaurantId(TenantContext.getRestaurantId());
                UUID id = UUID.fromString(dbIdStr);
                menuService.toggleAvailability(id, inStock);
                refreshMenuAndStockViews();
            } catch (Exception e) {
                System.out.println("Error toggling stock status in bridge: " + e.getMessage());
            }
        }
    }

    // --- CUSTOMER LOOKUP HELPERS ---
    private static class CustomerInfo {
        String name;
        String address;

        CustomerInfo(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }

    private static final java.util.Map<String, CustomerInfo> MOCK_CUSTOMERS = java.util.Map.of(
            "9876543210", new CustomerInfo("Adithyan", "123 Green Glen Layout, Outer Ring Road, Bangalore"),
            "9998887776", new CustomerInfo("Neha S.", "Flat 402, Elite Heights, Sector 15, HSR Layout"),
            "8887776665", new CustomerInfo("Vikram", "Prestige Tech Park, Marathahalli, Bangalore"));

    private void handleTabSelection(OrderType type) {
        selectedOrderType = type;

        // Reset active style classes
        tabDineInBtn.getStyleClass().remove("active");
        tabDeliveryBtn.getStyleClass().remove("active");
        tabPickupBtn.getStyleClass().remove("active");

        // Set default styles
        tabDineInBtn.setStyle("");
        tabDeliveryBtn.setStyle("");
        tabPickupBtn.setStyle("");

        if (type == OrderType.DINE_IN) {
            tabDineInBtn.getStyleClass().add("active");
            if (dineInMetaBox != null) {
                dineInMetaBox.setVisible(true);
                dineInMetaBox.setManaged(true);
            }
            if (deliveryMetaBox != null) {
                deliveryMetaBox.setVisible(false);
                deliveryMetaBox.setManaged(false);
            }
            if (pickupMetaBox != null) {
                pickupMetaBox.setVisible(false);
                pickupMetaBox.setManaged(false);
            }
            if (cartHeaderLabel != null) {
                cartHeaderLabel.setText(
                        currentDiningTable != null ? "Preparing Order for " + currentDiningTable.getTableNumber()
                                : "Preparing Order (No Table Selected)");
            }
        } else if (type == OrderType.DELIVERY) {
            tabDeliveryBtn.getStyleClass().add("active");
            if (dineInMetaBox != null) {
                dineInMetaBox.setVisible(false);
                dineInMetaBox.setManaged(false);
            }
            if (deliveryMetaBox != null) {
                deliveryMetaBox.setVisible(true);
                deliveryMetaBox.setManaged(true);
            }
            if (pickupMetaBox != null) {
                pickupMetaBox.setVisible(false);
                pickupMetaBox.setManaged(false);
            }
            if (cartHeaderLabel != null) {
                cartHeaderLabel.setText("Delivery Checkout");
            }
        } else if (type == OrderType.PICK_UP) {
            tabPickupBtn.getStyleClass().add("active");
            if (dineInMetaBox != null) {
                dineInMetaBox.setVisible(false);
                dineInMetaBox.setManaged(false);
            }
            if (deliveryMetaBox != null) {
                deliveryMetaBox.setVisible(false);
                deliveryMetaBox.setManaged(false);
            }
            if (pickupMetaBox != null) {
                pickupMetaBox.setVisible(true);
                pickupMetaBox.setManaged(true);
            }
            if (cartHeaderLabel != null) {
                cartHeaderLabel.setText("Pickup Checkout");
            }
        }

        // Clean fields
        if (deliveryPhoneField != null)
            deliveryPhoneField.clear();
        if (deliveryNameField != null)
            deliveryNameField.clear();
        if (deliveryAddressField != null)
            deliveryAddressField.clear();
        if (pickupPhoneField != null)
            pickupPhoneField.clear();
        if (pickupNameField != null)
            pickupNameField.clear();

        // Unlock cart status
        if (cartItemsContainer != null) {
            cartItemsContainer.setDisable(false);
        }

        updateCalculations();
        populateCart();
        updateBillingPageControlState();
    }

    private void loadOrderItemsToCart(Order order) {
        loadOrderItemsToCart(order, false);
    }

    private void loadOrderItemsToCart(Order order, boolean merge) {
        if (!merge) {
            cartList.clear();
        }
        try {
            List<CartItem> dbCartList = getDatabaseCartItems(order);
            for (CartItem dbItem : dbCartList) {
                CartItem existing = cartList.stream()
                        .filter(ci -> {
                            if (!ci.getItem().getId().equals(dbItem.getItem().getId()))
                                return false;
                            boolean notesMatch = (ci.getNotes() == null ? "" : ci.getNotes().trim())
                                    .equals(dbItem.getNotes() == null ? "" : dbItem.getNotes().trim());
                            boolean modsMatch = ci.getModifiers().size() == dbItem.getModifiers().size()
                                    && ci.getModifiers().containsAll(dbItem.getModifiers());
                            return notesMatch && modsMatch;
                        })
                        .findFirst()
                        .orElse(null);

                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + dbItem.getQuantity());
                    existing.setSavedQuantity(existing.getSavedQuantity() + dbItem.getSavedQuantity());
                } else {
                    cartList.add(dbItem);
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading order items: " + e.getMessage());
        }
    }

    private void triggerThermalReceiptPrinting(Order order) {
        try {
            System.out.println("🖨️ Printing receipt for order: " + order.getOrderNumber());

            // Build the receipt text
            StringBuilder receipt = new StringBuilder();
            receipt.append("      SURABHI SMARTDINE      \n");
            receipt.append("-----------------------------\n");
            receipt.append("Date: ").append(LocalDateTime.now().toString().substring(0, 16).replace("T", " "))
                    .append("\n");
            if (order.getType() == OrderType.DINE_IN) {
                receipt.append("Table: ").append(order.getTableName() != null ? order.getTableName() : "N/A")
                        .append("\n");
            } else if (order.getType() == OrderType.DELIVERY) {
                receipt.append("Type: Delivery\n");
                receipt.append("Cust: ").append(order.getCustomerName()).append("\n");
                receipt.append("Phone: ").append(order.getCustomerPhone()).append("\n");
            } else {
                receipt.append("Type: Pickup\n");
                receipt.append("Cust: ").append(order.getCustomerName()).append("\n");
            }
            receipt.append("-----------------------------\n");
            for (CartItem ci : cartList) {
                receipt.append(String.format("%-18s x%d\n", ci.getItem().getName(), ci.getQuantity()));
                receipt.append(String.format("                %8.2f\n",
                        ci.getItem().getPrice().doubleValue() * ci.getQuantity()));
            }
            receipt.append("-----------------------------\n");
            double subTotalVal = order.getSubTotal() != null ? order.getSubTotal().doubleValue() : 0.0;
            double discountVal = order.getDiscount() != null ? order.getDiscount().doubleValue() : 0.0;
            double cgstVal = order.getCgst() != null ? order.getCgst().doubleValue() : 0.0;
            double sgstVal = order.getSgst() != null ? order.getSgst().doubleValue() : 0.0;
            double grandTotalVal = order.getGrandTotal() != null ? order.getGrandTotal().doubleValue() : 0.0;
            double receivedVal = order.getReceivedAmount() != null ? order.getReceivedAmount().doubleValue() : 0.0;
            double changeVal = order.getChangeAmount() != null ? order.getChangeAmount().doubleValue() : 0.0;
            String payModeStr = order.getPaymentMode() != null ? order.getPaymentMode() : "PENDING";

            receipt.append(String.format("Subtotal:       %8.2f\n", subTotalVal));
            if (discountVal > 0.0) {
                receipt.append(String.format("Discount:      -%8.2f\n", discountVal));
            }
            receipt.append(String.format("CGST (2.5%):    %8.2f\n", cgstVal));
            receipt.append(String.format("SGST (2.5%):    %8.2f\n", sgstVal));
            receipt.append("-----------------------------\n");
            receipt.append(String.format("GRAND TOTAL:    %8.2f\n", grandTotalVal));
            receipt.append(String.format("Paid (%s):     %8.2f\n", payModeStr, receivedVal));
            receipt.append(String.format("Change:         %8.2f\n", changeVal));
            receipt.append("-----------------------------\n");
            receipt.append("    Thank you for dining!    \n\n\n\n");

            String printText = receipt.toString();

            // Run in thread to not block UI
            Thread printThread = new Thread(() -> {
                try {
                    javax.print.PrintService service = javax.print.PrintServiceLookup.lookupDefaultPrintService();
                    if (service != null) {
                        javax.print.DocPrintJob job = service.createPrintJob();
                        javax.print.DocFlavor flavor = javax.print.DocFlavor.INPUT_STREAM.AUTOSENSE;
                        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(printText.getBytes());
                        javax.print.Doc doc = new javax.print.SimpleDoc(bais, flavor, null);
                        job.print(doc, null);
                        System.out.println("✅ Sent receipt to default printer: " + service.getName());
                    } else {
                        System.out.println("⚠️ No default printer configured.");
                    }
                } catch (Exception ex) {
                    System.out.println("❌ Printer Error: " + ex.getMessage());
                }
            });
            printThread.start();
        } catch (Exception e) {
            System.out.println("Failed to trigger printer: " + e.getMessage());
        }
    }

    public void refreshMenuAndStockViews() {
        Platform.runLater(() -> {
            populateMenuGrid();
            loadStockOut();
            if (kdsNativeController != null) {
                kdsNativeController.refreshKdsData();
            }
            if (menuWebView != null) {
                try {
                    menuWebView.getEngine().executeScript("refreshUI();");
                } catch (Exception e) {
                    System.out.println("Failed to refresh WebView: " + e.getMessage());
                }
            }
        });
    }

    public void broadcastMenuUpdate(MenuItem item) {
        if (messagingTemplate != null && item != null) {
            java.util.UUID restId = item.getRestaurantId();
            if (restId == null) {
                restId = TenantContext.getRestaurantId();
            }
            try {
                String topic = "/topic/menu/" + restId.toString();
                messagingTemplate.convertAndSend(topic, item);
                System.out
                        .println("✅ Broadcasted menu update from controller: " + item.getName() + " to topic " + topic);
            } catch (Exception e) {
                System.err.println("❌ Failed to broadcast menu update from controller: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleMergeTablesDialog(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/merge_dialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            javafx.scene.Parent root = loader.load();

            UiMergeController controller = loader.getController();
            controller.setDashboardController(this);

            Stage stage = new Stage();
            stage.setTitle("Merge Tables");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open merge dialog: " + e.getMessage());
        }
    }

    public DiningTable getCurrentDiningTable() {
        return currentDiningTable;
    }

    public void setCurrentDiningTable(DiningTable currentDiningTable) {
        this.currentDiningTable = currentDiningTable;
    }

    public Order getCurrentActiveOrder() {
        return currentActiveOrder;
    }

    public void setCurrentActiveOrder(Order currentActiveOrder) {
        this.currentActiveOrder = currentActiveOrder;
    }

    private javafx.scene.control.ScrollPane findScrollPane(javafx.scene.Node node) {
        if (node == null)
            return null;
        javafx.scene.Parent parent = node.getParent();
        while (parent != null) {
            if (parent instanceof javafx.scene.control.ScrollPane) {
                return (javafx.scene.control.ScrollPane) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void scrollToBottom() {
        if (cartItemsContainer == null)
            return;
        Platform.runLater(() -> {
            javafx.scene.control.ScrollPane sp = findScrollPane(cartItemsContainer);
            if (sp != null) {
                sp.layout();
                sp.setVvalue(1.0);
            }
        });
    }

    private void handleBillingViewRefresh() {
        if (!isBillingRefreshing.compareAndSet(false, true)) {
            return;
        }
        logRefresh(">>> handleBillingViewRefresh: selectedOrderType=" + selectedOrderType + ", currentDiningTable=" + (currentDiningTable == null ? "null" : currentDiningTable.getTableNumber()) + ", currentActiveOrder=" + (currentActiveOrder == null ? "null" : currentActiveOrder.getOrderNumber()));
        
        UUID restaurantId = TenantContext.getRestaurantId();
        if (restaurantId == null) {
            restaurantId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        }
        final UUID finalRestaurantId = restaurantId;

        // Capture relevant session states locally so they are thread-safe and consistent
        final OrderType localOrderType = selectedOrderType;
        final DiningTable localTable = currentDiningTable;
        final Order localActiveOrder = currentActiveOrder;

        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(finalRestaurantId);
            try {
                if (localOrderType == OrderType.DINE_IN && localTable != null) {
                    // Fetch latest table state using JDBC to avoid cache
                    String tableStatusStr = null;
                    try {
                        tableStatusStr = jdbcTemplate.queryForObject(
                                "SELECT status FROM dining_tables WHERE id = ?",
                                String.class,
                                localTable.getId());
                    } catch (Exception e) {
                        logRefresh("Error querying table status: " + e.getMessage());
                    }
                    
                    logRefresh("Table status from DB: " + tableStatusStr);
                    final String finalTableStatusStr = tableStatusStr;
                    
                    // Query active orders for the dining table using JDBC to ensure fresh status
                    UUID tableOrderId = null;
                    String tableOrderNum = null;
                    try {
                        List<java.util.Map<String, Object>> activeOrdersRows = jdbcTemplate.queryForList(
                                "SELECT id, order_number FROM orders WHERE restaurant_id = ? AND table_id = ? AND status NOT IN ('PAID', 'CANCELLED')",
                                finalRestaurantId,
                                localTable.getId());
                        logRefresh("Active orders count from DB: " + activeOrdersRows.size());
                        if (!activeOrdersRows.isEmpty()) {
                            Object idObj = activeOrdersRows.get(0).get("id");
                            if (idObj instanceof UUID) {
                                tableOrderId = (UUID) idObj;
                            } else {
                                tableOrderId = UUID.fromString(idObj.toString());
                            }
                            tableOrderNum = (String) activeOrdersRows.get(0).get("order_number");
                            logRefresh("Found active tableOrderId from DB: " + tableOrderId + ", tableOrderNum: " + tableOrderNum);
                        }
                    } catch (Exception e) {
                        logRefresh("Error querying active orders via JDBC: " + e.getMessage());
                    }

                    final UUID finalTableOrderId = tableOrderId;
                    final String finalTableOrderNum = tableOrderNum;

                    if (finalTableOrderId != null) {
                        logRefresh("finalTableOrderId != null: currentActiveOrder=" + (localActiveOrder == null ? "null" : localActiveOrder.getId()));
                        if (localActiveOrder == null || !localActiveOrder.getId().equals(finalTableOrderId)) {
                            logRefresh("Current active order does not match tableOrderId! hasUnsavedChanges=" + hasUnsavedChanges());
                            if (!hasUnsavedChanges()) {
                                // Clear L1 cache to evict cached state
                                try {
                                    jakarta.persistence.EntityManager em = applicationContext.getBean(jakarta.persistence.EntityManager.class);
                                    if (em != null) {
                                        em.clear();
                                        logRefresh("Cleared L1 Cache successfully for order reload");
                                    }
                                } catch (Exception ex) {
                                    logRefresh("Error clearing L1 cache: " + ex.getMessage());
                                }
                                Order tableOrder = orderRepository.findById(finalTableOrderId).orElse(null);
                                if (tableOrder != null) {
                                    List<CartItem> dbCartList = getDatabaseCartItems(tableOrder);
                                    Platform.runLater(() -> {
                                        if (finalTableStatusStr != null) {
                                            currentDiningTable.setStatus(TableStatus.valueOf(finalTableStatusStr));
                                        }
                                        currentActiveOrder = tableOrder;
                                        cartList.clear();
                                        for (CartItem ci : dbCartList) {
                                            cartList.add(ci);
                                        }
                                        logRefresh("Reloaded active table order. Cart list size: " + cartList.size());
                                        if (dineInOrderNumLabel != null) {
                                            dineInOrderNumLabel.setText(finalTableOrderNum);
                                        }
                                        if (dineInTableChip != null) {
                                            dineInTableChip.setText(tableOrder.getMergedTableIds() != null && !tableOrder.getMergedTableIds().isEmpty()
                                                    ? tableOrder.getTableName()
                                                    : localTable.getTableNumber());
                                        }
                                        populateCart();
                                        updateCalculations();
                                        updateBillingPageControlState();
                                    });
                                }
                                return;
                            }
                        }
                    } else {
                        logRefresh("finalTableOrderId is null. currentActiveOrder=" + (localActiveOrder == null ? "null" : localActiveOrder.getId()));
                        if (localActiveOrder != null) {
                            logRefresh("Current active order exists but tableOrderId is null! Voiding session on UI.");
                            Platform.runLater(() -> {
                                showAlert("Order Status Updated", "This order has been paid or cancelled on another terminal.");
                                resetBillingSessionState();
                                showHomeView();
                            });
                            return;
                        }
                    }

                    if (localActiveOrder == null) {
                        Platform.runLater(() -> {
                            if (finalTableStatusStr != null) {
                                currentDiningTable.setStatus(TableStatus.valueOf(finalTableStatusStr));
                            }
                        });
                        return;
                    }

                    // Check if active order status changed or if it was deleted using JDBC
                    String dbOrderStatusStr = null;
                    try {
                        dbOrderStatusStr = jdbcTemplate.queryForObject(
                                "SELECT status FROM orders WHERE id = ?",
                                String.class,
                                localActiveOrder.getId());
                    } catch (Exception e) {
                        logRefresh("Error querying active order status via JDBC: " + e.getMessage());
                    }

                    logRefresh("dbOrderStatusStr: " + dbOrderStatusStr);
                    if (dbOrderStatusStr == null || "PAID".equals(dbOrderStatusStr) || "CANCELLED".equals(dbOrderStatusStr)) {
                        logRefresh("Order paid or cancelled! Redirecting to home.");
                        Platform.runLater(() -> {
                            showAlert("Order Status Updated", "This order has been paid or cancelled on another terminal.");
                            resetBillingSessionState();
                            showHomeView();
                        });
                        return;
                    }

                    final String finalDbOrderStatusStr = dbOrderStatusStr;
                    List<CartItem> dbCartList = getDatabaseCartItems(localActiveOrder);
                    logRefresh("dbCartList size: " + dbCartList.size());
                    
                    Order dbOrder = orderRepository.findById(localActiveOrder.getId()).orElse(null);

                    Platform.runLater(() -> {
                        try {
                            if (finalTableStatusStr != null) {
                                currentDiningTable.setStatus(TableStatus.valueOf(finalTableStatusStr));
                            }
                            if (finalDbOrderStatusStr != null) {
                                currentActiveOrder.setStatus(OrderStatus.valueOf(finalDbOrderStatusStr));
                            }
                            
                            boolean stateChanged = isDatabaseStateChanged(dbCartList);
                            logRefresh("stateChanged (UI thread): " + stateChanged);
                            if (stateChanged && dbOrder != null) {
                                // Clear L1 cache to evict cached state
                                try {
                                    jakarta.persistence.EntityManager em = applicationContext.getBean(jakarta.persistence.EntityManager.class);
                                    if (em != null) {
                                        em.clear();
                                        logRefresh("Cleared L1 Cache for merging DB changes");
                                    }
                                } catch (Exception ex) {
                                    logRefresh("Error clearing L1 Cache: " + ex.getMessage());
                                }
                                logRefresh("Found fresh dbOrder from repository. Merging changes...");
                                mergeDatabaseChanges(dbOrder, dbCartList);
                            }
                        } catch (Exception e) {
                            logRefresh("Error updating billing view refresh UI: " + e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                logRefresh("CRITICAL ERROR inside handleBillingViewRefresh async: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isBillingRefreshing.set(false);
                TenantContext.clear();
            }
        });
    }

    private boolean hasUnsavedChanges() {
        for (CartItem ci : cartList) {
            if (ci.getQuantity() != ci.getSavedQuantity()) {
                return true;
            }
        }
        return false;
    }

    private List<CartItem> getDatabaseCartItems(Order order) {
        List<CartItem> dbCartList = new ArrayList<>();
        try {
            String sql = "SELECT ki.menu_item_id, ki.quantity, ki.special_instruction, ki.item_status " +
                         "FROM kot_items ki " +
                         "JOIN kots k ON ki.kot_id = k.id " +
                         "WHERE k.order_id = ?";
            
            List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(sql, order.getId());
            for (java.util.Map<String, Object> row : rows) {
                String statusStr = (String) row.get("item_status");
                if ("CANCELLED".equals(statusStr)) {
                    continue;
                }
                
                Object menuItemIdObj = row.get("menu_item_id");
                UUID menuItemId;
                if (menuItemIdObj instanceof UUID) {
                    menuItemId = (UUID) menuItemIdObj;
                } else {
                    menuItemId = UUID.fromString(menuItemIdObj.toString());
                }
                
                MenuItem menuItem = menuRepository.findById(menuItemId).orElse(null);
                if (menuItem == null)
                    continue;

                int qty = ((Number) row.get("quantity")).intValue();
                String rawSpec = row.get("special_instruction") != null ? ((String) row.get("special_instruction")).trim() : "";
                String parsedNotes = "";
                List<String> parsedMods = new ArrayList<>();
                if (!rawSpec.isEmpty()) {
                    if (rawSpec.contains("(") && rawSpec.contains(")")) {
                        int openIdx = rawSpec.indexOf("(");
                        int closeIdx = rawSpec.lastIndexOf(")");
                        parsedNotes = rawSpec.substring(0, openIdx).trim();
                        String modsStr = rawSpec.substring(openIdx + 1, closeIdx).trim();
                        if (!modsStr.isEmpty()) {
                            parsedMods.addAll(List.of(modsStr.split(",\\s*")));
                        }
                    } else {
                        parsedNotes = rawSpec;
                    }
                }

                final String finalNotes = parsedNotes;
                final List<String> finalMods = parsedMods;
                CartItem existing = dbCartList.stream()
                        .filter(ci -> {
                            if (!ci.getItem().getId().equals(menuItem.getId()))
                                return false;
                            boolean notesMatch = (ci.getNotes() == null ? "" : ci.getNotes().trim())
                                    .equals(finalNotes);
                            boolean modsMatch = ci.getModifiers().size() == finalMods.size()
                                    && ci.getModifiers().containsAll(finalMods);
                            return notesMatch && modsMatch;
                        })
                        .findFirst()
                        .orElse(null);

                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + qty);
                    existing.setSavedQuantity(existing.getSavedQuantity() + qty);
                } else {
                    CartItem ci = new CartItem(menuItem, qty);
                    ci.setSavedQuantity(qty);
                    ci.setNotes(parsedNotes);
                    ci.getModifiers().addAll(parsedMods);
                    dbCartList.add(ci);
                }
            }
        } catch (Exception e) {
            System.out.println("Error getting database cart items via JDBC: " + e.getMessage());
            e.printStackTrace();
        }
        return dbCartList;
    }

    private boolean isDatabaseStateChanged(List<CartItem> dbCartList) {
        List<CartItem> savedLocalItems = cartList.stream()
                .filter(ci -> ci.getSavedQuantity() > 0)
                .collect(java.util.stream.Collectors.toList());

        logRefresh("isDatabaseStateChanged: dbCartList size=" + dbCartList.size() + ", savedLocalItems size=" + savedLocalItems.size());
        for (CartItem localItem : savedLocalItems) {
            logRefresh("  Local Saved Item: " + localItem.getItem().getName() + ", Qty=" + localItem.getQuantity() + ", SavedQty=" + localItem.getSavedQuantity());
        }

        if (dbCartList.size() != savedLocalItems.size()) {
            logRefresh("Sizes differ! dbCartList.size=" + dbCartList.size() + " vs savedLocalItems.size=" + savedLocalItems.size() + ". Returning true");
            return true;
        }

        for (CartItem dbItem : dbCartList) {
            CartItem localMatch = savedLocalItems.stream()
                    .filter(ci -> ci.getItem().getId().equals(dbItem.getItem().getId())
                            && (ci.getNotes() == null ? "" : ci.getNotes().trim()).equals(dbItem.getNotes() == null ? "" : dbItem.getNotes().trim())
                            && ci.getModifiers().size() == dbItem.getModifiers().size()
                            && ci.getModifiers().containsAll(dbItem.getModifiers()))
                    .findFirst()
                    .orElse(null);

            if (localMatch == null) {
                logRefresh("No local match found for DB item " + dbItem.getItem().getName() + "! Returning true");
                return true;
            }
            if (localMatch.getSavedQuantity() != dbItem.getQuantity()) {
                logRefresh("Saved quantity differs for " + dbItem.getItem().getName() + "! localSaved=" + localMatch.getSavedQuantity() + ", dbQty=" + dbItem.getQuantity() + ". Returning true");
                return true;
            }
        }

        logRefresh("isDatabaseStateChanged: No changes detected. Returning false");
        return false;
    }

    private void mergeDatabaseChanges(Order dbOrder, List<CartItem> dbCartList) {
        currentActiveOrder = dbOrder;

        java.util.Set<CartItem> matchedLocalItems = new java.util.HashSet<>();

        for (CartItem dbItem : dbCartList) {
            CartItem localMatch = cartList.stream()
                    .filter(ci -> ci.getItem().getId().equals(dbItem.getItem().getId())
                            && (ci.getNotes() == null ? "" : ci.getNotes().trim()).equals(dbItem.getNotes() == null ? "" : dbItem.getNotes().trim())
                            && ci.getModifiers().size() == dbItem.getModifiers().size()
                            && ci.getModifiers().containsAll(dbItem.getModifiers()))
                    .findFirst()
                    .orElse(null);

            if (localMatch != null) {
                matchedLocalItems.add(localMatch);
                int diff = localMatch.getQuantity() - localMatch.getSavedQuantity();
                localMatch.setSavedQuantity(dbItem.getQuantity());
                localMatch.setQuantity(Math.max(0, dbItem.getQuantity() + diff));
            } else {
                CartItem newItem = new CartItem(dbItem.getItem(), dbItem.getQuantity());
                newItem.setSavedQuantity(dbItem.getQuantity());
                newItem.setNotes(dbItem.getNotes());
                newItem.getModifiers().addAll(dbItem.getModifiers());
                cartList.add(newItem);
                matchedLocalItems.add(newItem);
            }
        }

        List<CartItem> toRemove = new ArrayList<>();
        for (CartItem localItem : cartList) {
            if (localItem.getSavedQuantity() > 0 && !matchedLocalItems.contains(localItem)) {
                int diff = localItem.getQuantity() - localItem.getSavedQuantity();
                if (diff == 0) {
                    toRemove.add(localItem);
                } else {
                    localItem.setSavedQuantity(0);
                    localItem.setQuantity(Math.max(0, diff));
                }
            }
        }
        cartList.removeAll(toRemove);

        cartList.removeIf(ci -> ci.getQuantity() <= 0);

        Platform.runLater(() -> {
            if (dineInOrderNumLabel != null && currentActiveOrder != null) {
                dineInOrderNumLabel.setText(currentActiveOrder.getOrderNumber());
            }
            if (dineInTableChip != null && currentDiningTable != null && currentActiveOrder != null) {
                dineInTableChip.setText(currentActiveOrder.getMergedTableIds() != null && !currentActiveOrder.getMergedTableIds().isEmpty()
                        ? currentActiveOrder.getTableName()
                        : currentDiningTable.getTableNumber());
            }
            populateCart();
            updateCalculations();
            updateBillingPageControlState();
        });
    }

    @FXML
    public void handleLogout(javafx.event.ActionEvent event) {
        try {
            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }
            Stage stage = (Stage) dashboardView.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/login.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            javafx.scene.Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("SMARTDINE LOGIN");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Logout Failed", "Error logging out: " + e.getMessage());
        }
    }

    @FXML
    public void handleSyncFromCloud(javafx.event.ActionEvent event) {
        com.smartdine.coreheart.SystemConfig config = systemConfigRepository.findAll().stream().findFirst().orElse(null);
        if (config == null || !config.isActivated()) {
            showAlert("Sync Failed", "System is not activated yet. Please activate first.");
            return;
        }

        String activationCode = config.getActivationCode();
        if (activationCode == null || activationCode.trim().isEmpty()) {
            showAlert("Sync Failed", "Activation code is missing or empty.");
            return;
        }

        javafx.scene.control.Button sourceBtn = (javafx.scene.control.Button) event.getSource();
        String originalText = sourceBtn.getText();
        sourceBtn.setText("⏳ Syncing...");
        sourceBtn.setDisable(true);

        UUID restaurantId = getActiveRestaurantId();
        CompletableFuture.runAsync(() -> {
            TenantContext.setRestaurantId(restaurantId);
            try {
                activationService.syncMenuAndTables(activationCode);
                
                Platform.runLater(() -> {
                    sourceBtn.setText(originalText);
                    sourceBtn.setDisable(false);
                    
                    // Refresh all views
                    loadTablesToUi();
                    loadRunningOrders();
                    loadStockOut();
                    loadPlatformStats();
                    loadPlatformOrders();
                    
                    // Refresh menu grid / combos in Billing View
                    try {
                        if (menuWebView != null && menuWebView.getEngine() != null) {
                            menuWebView.getEngine().executeScript("refreshUI();");
                        }
                    } catch (Exception ignored) {}
                    
                    showAlert("Sync Success", "Menu items and table layouts synchronized successfully!");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    sourceBtn.setText(originalText);
                    sourceBtn.setDisable(false);
                    showAlert("Sync Failed", "Error synchronizing configuration: " + e.getMessage());
                });
            } finally {
                TenantContext.clear();
            }
        });
    }
}