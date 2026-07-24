package com.smartdine.controller;

import com.smartdine.coreheart.AddonItem;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.repository.AddonItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AddonController {

    @Autowired
    private AddonItemRepository addonItemRepository;

    @Autowired
    private com.smartdine.repository.MenuRepository menuRepository;

    @Autowired
    private com.smartdine.service.ActivationService activationService;

    @Autowired
    private com.smartdine.repository.SystemConfigRepository systemConfigRepository;

    private UUID getEffectiveRestaurantId(String paramId) {
        if (paramId != null && !paramId.trim().isEmpty()) {
            try {
                return UUID.fromString(paramId.trim());
            } catch (Exception ignored) {}
        }
        UUID rid = TenantContext.getRestaurantId();
        if (rid != null) return rid;

        return systemConfigRepository.findAll().stream()
                .findFirst()
                .map(com.smartdine.coreheart.SystemConfig::getRestaurantId)
                .orElse(UUID.fromString("51c83920-3cfc-44da-8ca8-d0877092a0ca"));
    }

    @GetMapping({"/addons", "/waiter/addons"})
    public ResponseEntity<List<AddonItem>> getAddons(@RequestParam(required = false) String restaurantId) {
        UUID rid = getEffectiveRestaurantId(restaurantId);
        List<AddonItem> addons = addonItemRepository.findByRestaurantId(rid);
        return ResponseEntity.ok(addons);
    }

    @PostMapping("/addons")
    public ResponseEntity<AddonItem> createAddon(@RequestBody AddonItem addon) {
        UUID rid = addon.getRestaurantId() != null ? addon.getRestaurantId() : getEffectiveRestaurantId(null);
        addon.setRestaurantId(rid);
        if (addon.getPrice() == null) {
            addon.setPrice(BigDecimal.ZERO);
        }
        AddonItem saved = addonItemRepository.saveAndFlush(addon);
        try { activationService.syncAddonsToDisk(rid); } catch (Exception ignored) {}
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/addons/{id}")
    public ResponseEntity<AddonItem> updateAddon(@PathVariable UUID id, @RequestBody AddonItem updated) {
        return addonItemRepository.findById(id).map(existing -> {
            if (updated.getName() != null) existing.setName(updated.getName());
            if (updated.getPrice() != null) existing.setPrice(updated.getPrice());
            existing.setAvailable(updated.isAvailable());
            AddonItem saved = addonItemRepository.saveAndFlush(existing);
            try { activationService.syncAddonsToDisk(existing.getRestaurantId()); } catch (Exception ignored) {}
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/addons/{id}")
    public ResponseEntity<?> deleteAddon(@PathVariable UUID id) {
        return addonItemRepository.findById(id).map(existing -> {
            UUID rid = existing.getRestaurantId();
            String nameToDelete = existing.getName() != null ? existing.getName().trim().toLowerCase() : "";
            addonItemRepository.delete(existing);
            addonItemRepository.flush();

            if (!nameToDelete.isEmpty()) {
                menuRepository.findAll().stream()
                    .filter(m -> m.getName() != null && m.getName().trim().toLowerCase().endsWith(nameToDelete))
                    .forEach(m -> {
                        m.setDeleted(true);
                        m.setAvailable(false);
                        menuRepository.save(m);
                    });
                menuRepository.flush();
            }
            try { activationService.syncAddonsToDisk(rid); } catch (Exception ignored) {}
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
