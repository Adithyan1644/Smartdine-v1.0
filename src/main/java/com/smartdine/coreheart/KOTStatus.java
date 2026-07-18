package com.smartdine.coreheart;

public enum KOTStatus {
    PENDING,    // Sent to kitchen, waiting for chef
    PREPARING,  // Chef started cooking
    READY,      // Food cooked, waiting for waiter
    SERVED,     // Delivered to table
    CANCELLED   // Order voided
}