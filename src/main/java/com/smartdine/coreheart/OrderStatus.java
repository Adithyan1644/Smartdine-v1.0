package com.smartdine.coreheart;

public enum OrderStatus {
    OPEN,       // Active order, food being served
    BILLED,     // Bill printed, waiting for payment
    PAID,       // Fully settled
    CANCELLED   // Order voided
}