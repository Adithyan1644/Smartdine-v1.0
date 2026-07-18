package com.smartdine.coreheart;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "modifier_groups")
public class ModifierGroup extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "is_global")
    private boolean isGlobal = false;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "modifier_group_id")
    private List<ModifierOption> options = new ArrayList<>();

    // Constructor
    public ModifierGroup() {}

    // Getters & Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public List<ModifierOption> getOptions() {
        return options;
    }

    public void setOptions(List<ModifierOption> options) {
        this.options = options;
    }
}
