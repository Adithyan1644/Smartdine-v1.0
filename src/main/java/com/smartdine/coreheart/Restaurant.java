package com.smartdine.coreheart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

@Entity
@Table(name = "restaurants", indexes = {
    @Index(name = "idx_restaurant_sync_code", columnList = "sync_code", unique = true)
})
public class Restaurant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "sync_code", nullable = false, unique = true)
    private String syncCode;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "active_local_ip")
    private String activeLocalIp;

    public Restaurant() {}

    public Restaurant(String name, String syncCode, boolean isActive) {
        this.name = name;
        this.syncCode = syncCode;
        this.isActive = isActive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSyncCode() {
        return syncCode;
    }

    public void setSyncCode(String syncCode) {
        this.syncCode = syncCode;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getActiveLocalIp() {
        return activeLocalIp;
    }

    public void setActiveLocalIp(String activeLocalIp) {
        this.activeLocalIp = activeLocalIp;
    }
}
