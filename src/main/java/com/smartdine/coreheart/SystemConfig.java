package com.smartdine.coreheart;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Single row configuration

    @Column(name = "is_activated", nullable = false)
    private boolean activated = false;

    @Column(name = "restaurant_id")
    private UUID restaurantId;

    @Column(name = "restaurant_name")
    private String restaurantName;

    @Column(name = "activation_code")
    private String activationCode;

    @Column(name = "cgst_rate", precision = 5, scale = 2)
    private BigDecimal cgstRate = BigDecimal.valueOf(2.5); // Default 2.5%

    @Column(name = "sgst_rate", precision = 5, scale = 2)
    private BigDecimal sgstRate = BigDecimal.valueOf(2.5); // Default 2.5%

    @Column(name = "service_charge_rate", precision = 5, scale = 2)
    private BigDecimal serviceChargeRate = BigDecimal.ZERO;

    public SystemConfig() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public BigDecimal getCgstRate() {
        return cgstRate;
    }

    public void setCgstRate(BigDecimal cgstRate) {
        this.cgstRate = cgstRate;
    }

    public BigDecimal getSgstRate() {
        return sgstRate;
    }

    public void setSgstRate(BigDecimal sgstRate) {
        this.sgstRate = sgstRate;
    }

    public BigDecimal getServiceChargeRate() {
        return serviceChargeRate;
    }

    public void setServiceChargeRate(BigDecimal serviceChargeRate) {
        this.serviceChargeRate = serviceChargeRate;
    }
}
