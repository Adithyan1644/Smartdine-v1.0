package com.smartdine.coreheart;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "modifier_options")
public class ModifierOption extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    // Constructor
    public ModifierOption() {}

    // Getters & Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
