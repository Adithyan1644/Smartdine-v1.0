package com.smartdine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartdine.coreheart.ModifierGroup;
import com.smartdine.coreheart.ModifierOption;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.repository.ModifierGroupRepository;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/menu/modifier-groups")
public class ModifierController {

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    // Create a new Modifier Group
    @PostMapping
    public ModifierGroup createModifierGroup(@RequestBody ModifierGroup group) {
        UUID restaurantId = TenantContext.getRestaurantId();
        group.setRestaurantId(restaurantId);
        if (group.getOptions() != null) {
            for (ModifierOption option : group.getOptions()) {
                option.setRestaurantId(restaurantId);
            }
        }
        return modifierGroupRepository.save(group);
    }

    // Get all Modifier Groups for the waiter/admin
    @GetMapping
    public List<ModifierGroup> getModifierGroups() {
        UUID restaurantId = TenantContext.getRestaurantId();
        return modifierGroupRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
    }

    // Get only Global Modifier Groups
    @GetMapping("/global")
    public List<ModifierGroup> getGlobalModifierGroups() {
        UUID restaurantId = TenantContext.getRestaurantId();
        return modifierGroupRepository.findByRestaurantIdAndIsGlobalTrueAndIsDeletedFalse(restaurantId);
    }
}
