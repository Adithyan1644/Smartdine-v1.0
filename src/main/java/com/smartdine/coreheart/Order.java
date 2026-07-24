package com.smartdine.coreheart;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    private String orderNumber; // e.g., "#1087" from your UI

    @Enumerated(EnumType.STRING)
    private OrderType type;

    private String source; // "DIRECT", "ZOMATO", "SWIGGY"

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.OPEN;

    // Link to Table (Nullable for Delivery/Pickup)
    private UUID tableId;
    private String tableName; // e.g., "Table 12"

    // CRM Data (Optional as discussed)
    private String customerPhone;
    private String customerName;

    // Financial Data (Matches your UI exactly)
    @Column(precision = 10, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal cgst = BigDecimal.ZERO; // 2.5%

    @Column(precision = 10, scale = 2)
    private BigDecimal sgst = BigDecimal.ZERO; // 2.5%

    @Column(precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    // Payment Logic
    private String paymentMode; // CASH, CARD, UPI, SPLIT
    private BigDecimal receivedAmount = BigDecimal.ZERO;
    private BigDecimal changeAmount = BigDecimal.ZERO; // e.g., ₹128.50 in your UI

    private String notes;
    private String cancelReason;

    // Tracking Timer
    @Column(name = "started_at")
    private LocalDateTime startedAt = LocalDateTime.now(); 
    
    private LocalDateTime settledAt;

    @Column(name = "merged_table_ids", length = 1000)
    private String mergedTableIds;

    // Constructors
    public Order() {}

    public Order(String orderNumber, OrderType type, String source, OrderStatus status, UUID tableId, String tableName, 
                 String customerPhone, String customerName, BigDecimal subTotal, BigDecimal cgst, BigDecimal sgst, 
                 BigDecimal discount, BigDecimal grandTotal, String paymentMode, BigDecimal receivedAmount, 
                 BigDecimal changeAmount, String notes, LocalDateTime startedAt, LocalDateTime settledAt) {
        this.orderNumber = orderNumber;
        this.type = type;
        this.source = source;
        this.status = status;
        this.tableId = tableId;
        this.tableName = tableName;
        this.customerPhone = customerPhone;
        this.customerName = customerName;
        this.subTotal = subTotal;
        this.cgst = cgst;
        this.sgst = sgst;
        this.discount = discount;
        this.grandTotal = grandTotal;
        this.paymentMode = paymentMode;
        this.receivedAmount = receivedAmount;
        this.changeAmount = changeAmount;
        this.notes = notes;
        this.startedAt = startedAt;
        this.settledAt = settledAt;
    }

    // Getters & Setters
    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public UUID getTableId() {
        return tableId;
    }

    public void setTableId(UUID tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }

    public BigDecimal getCgst() {
        return cgst;
    }

    public void setCgst(BigDecimal cgst) {
        this.cgst = cgst;
    }

    public BigDecimal getSgst() {
        return sgst;
    }

    public void setSgst(BigDecimal sgst) {
        this.sgst = sgst;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public BigDecimal getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    public String getMergedTableIds() {
        return mergedTableIds;
    }

    public void setMergedTableIds(String mergedTableIds) {
        this.mergedTableIds = mergedTableIds;
    }

    @Column(name = "packing_fee", precision = 10, scale = 2)
    private BigDecimal packingFee = BigDecimal.ZERO;

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getPackingFee() {
        return packingFee;
    }

    public void setPackingFee(BigDecimal packingFee) {
        this.packingFee = packingFee;
    }

    @Column(name = "is_priority")
    private Boolean priority = Boolean.FALSE;

    @PostLoad
    private void onLoad() {
        if (this.priority == null) {
            this.priority = Boolean.FALSE;
        }
    }

    public boolean isPriority() {
        return Boolean.TRUE.equals(priority);
    }

    public boolean getPriority() {
        return Boolean.TRUE.equals(priority);
    }

    public void setPriority(Boolean priority) {
        this.priority = priority != null ? priority : Boolean.FALSE;
    }
}