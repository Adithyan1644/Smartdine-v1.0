package com.smartdine.coreheart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "restaurant_settings")
public class RestaurantSettings extends BaseEntity {

    @Column(name = "restaurant_id", nullable = false, unique = true)
    private UUID restaurantId;

    @Column(name = "is_delivery_charge_enabled", nullable = false)
    private boolean isDeliveryChargeEnabled = true;

    @Column(name = "default_delivery_fee", nullable = false)
    private double defaultDeliveryFee = 50.00;

    @Column(name = "is_packing_charge_enabled", nullable = false)
    private boolean isPackingChargeEnabled = true;

    @Column(name = "default_packing_fee", nullable = false)
    private double defaultPackingFee = 20.00;

    @Column(name = "is_tax_enabled", nullable = false)
    private boolean isTaxEnabled = true;

    @Column(name = "tax_rate_percentage", nullable = false)
    private double taxRatePercentage = 2.5;

    public RestaurantSettings() {}

    public RestaurantSettings(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public boolean isDeliveryChargeEnabled() {
        return isDeliveryChargeEnabled;
    }

    public void setDeliveryChargeEnabled(boolean deliveryChargeEnabled) {
        isDeliveryChargeEnabled = deliveryChargeEnabled;
    }

    public double getDefaultDeliveryFee() {
        return defaultDeliveryFee;
    }

    public void setDefaultDeliveryFee(double defaultDeliveryFee) {
        this.defaultDeliveryFee = defaultDeliveryFee;
    }

    public boolean isPackingChargeEnabled() {
        return isPackingChargeEnabled;
    }

    public void setPackingChargeEnabled(boolean packingChargeEnabled) {
        isPackingChargeEnabled = packingChargeEnabled;
    }

    public double getDefaultPackingFee() {
        return defaultPackingFee;
    }

    public void setDefaultPackingFee(double defaultPackingFee) {
        this.defaultPackingFee = defaultPackingFee;
    }

    public boolean isTaxEnabled() {
        return isTaxEnabled;
    }

    public void setTaxEnabled(boolean taxEnabled) {
        isTaxEnabled = taxEnabled;
    }

    public double getTaxRatePercentage() {
        return taxRatePercentage;
    }

    public void setTaxRatePercentage(double taxRatePercentage) {
        this.taxRatePercentage = taxRatePercentage;
    }
}
