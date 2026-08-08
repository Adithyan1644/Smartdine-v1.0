package com.smartdine.controller;

import com.smartdine.coreheart.AddonItem;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.repository.AddonItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private com.smartdine.repository.RestaurantRepository restaurantRepository;

    @Autowired
    private com.smartdine.repository.SystemConfigRepository systemConfigRepository;

    private UUID getEffectiveRestaurantId(String paramId, String syncCodeParam) {
        // 1. Prioritize syncCodeParam if present, as syncCode is the canonical merchant identifier across Cloud and POS
        if (syncCodeParam != null && !syncCodeParam.trim().isEmpty()) {
            try {
                String code = syncCodeParam.trim();
                com.smartdine.coreheart.Restaurant r = restaurantRepository.findByBillerSyncCode(code)
                        .or(() -> restaurantRepository.findBySyncCodeAndIsDeletedFalse(code))
                        .orElse(null);
                if (r != null) {
                    UUID rid = r.getRestaurantId() != null ? r.getRestaurantId() : r.getId();
                    if (rid != null) return rid;
                }
            } catch (Exception ignored) {}
        }

        // 2. Try paramId
        if (paramId != null && !paramId.trim().isEmpty()) {
            String code = paramId.trim();
            // Try paramId as syncCode (e.g. SD-115386)
            try {
                com.smartdine.coreheart.Restaurant r = restaurantRepository.findByBillerSyncCode(code)
                        .or(() -> restaurantRepository.findBySyncCodeAndIsDeletedFalse(code))
                        .orElse(null);
                if (r != null) {
                    UUID rid = r.getRestaurantId() != null ? r.getRestaurantId() : r.getId();
                    if (rid != null) return rid;
                }
            } catch (Exception ignored) {}

            // Try paramId as UUID matching an existing Restaurant in database
            try {
                UUID parsedUuid = UUID.fromString(code);
                com.smartdine.coreheart.Restaurant r = restaurantRepository.findById(parsedUuid)
                        .or(() -> restaurantRepository.findByRestaurantId(parsedUuid))
                        .orElse(null);
                if (r != null) {
                    UUID rid = r.getRestaurantId() != null ? r.getRestaurantId() : r.getId();
                    if (rid != null) return rid;
                }
                // If valid UUID but no Restaurant found in DB yet, return parsedUuid as fallback
                return parsedUuid;
            } catch (Exception ignored) {}
        }

        // 3. Try TenantContext
        UUID rid = TenantContext.getRestaurantId();
        if (rid != null) return rid;

        throw new IllegalArgumentException(
                "restaurantId is required — could not resolve from request or session");
    }

    private List<AddonItem> findAllAddonsForRestaurant(UUID rid, String syncCodeParam) {
        List<AddonItem> addons = new ArrayList<>(addonItemRepository.findByRestaurantId(rid));
        if (syncCodeParam != null && !syncCodeParam.trim().isEmpty()) {
            try {
                com.smartdine.coreheart.Restaurant r = restaurantRepository.findByBillerSyncCode(syncCodeParam.trim())
                        .or(() -> restaurantRepository.findBySyncCodeAndIsDeletedFalse(syncCodeParam.trim()))
                        .orElse(null);
                if (r != null) {
                    UUID altId = (r.getId() != null && !r.getId().equals(rid)) ? r.getId()
                              : (r.getRestaurantId() != null && !r.getRestaurantId().equals(rid)) ? r.getRestaurantId() : null;
                    if (altId != null) {
                        List<AddonItem> altAddons = addonItemRepository.findByRestaurantId(altId);
                        for (AddonItem alt : altAddons) {
                            if (addons.stream().noneMatch(a -> a.getId() != null && a.getId().equals(alt.getId()))) {
                                addons.add(alt);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return addons;
    }

    @GetMapping({"/addons", "/waiter/addons"})
    public ResponseEntity<List<AddonItem>> getAddons(
            @RequestParam(required = false) String restaurantId,
            @RequestParam(required = false) String syncCode,
            @RequestHeader(name = "X-Sync-Code", required = false) String syncHeader) {
        String effectiveCode = (syncCode != null && !syncCode.trim().isEmpty()) ? syncCode : syncHeader;
        UUID rid = getEffectiveRestaurantId(restaurantId, effectiveCode);
        List<AddonItem> addons = findAllAddonsForRestaurant(rid, effectiveCode);
        return ResponseEntity.ok(addons);
    }

    @PostMapping("/addons")
    public ResponseEntity<AddonItem> createAddon(
            @RequestBody AddonItem addon,
            @RequestParam(required = false) String restaurantId,
            @RequestParam(required = false) String syncCode,
            @RequestHeader(name = "X-Sync-Code", required = false) String syncHeader) {
        String effectiveCode = (syncCode != null && !syncCode.trim().isEmpty()) ? syncCode : syncHeader;
        String idParam = (addon.getRestaurantId() != null) ? addon.getRestaurantId().toString() : restaurantId;
        UUID rid = getEffectiveRestaurantId(idParam, effectiveCode);

        if (addon.getPrice() == null) {
            addon.setPrice(BigDecimal.ZERO);
        }

        // Upsert by name across all ID aliases to prevent duplicate rows for the same restaurant
        List<AddonItem> existingList = findAllAddonsForRestaurant(rid, effectiveCode);
        AddonItem targetItem = existingList.stream()
                .filter(a -> a.getName() != null && a.getName().trim().equalsIgnoreCase(addon.getName().trim()))
                .findFirst()
                .orElse(addon);

        targetItem.setRestaurantId(rid);
        targetItem.setName(addon.getName().trim());
        targetItem.setPrice(addon.getPrice());
        targetItem.setAvailable(addon.isAvailable());

        AddonItem saved = addonItemRepository.saveAndFlush(targetItem);
        try { activationService.syncAddonsToDisk(rid); } catch (Exception ignored) {}
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/addons/{id}")
    public ResponseEntity<AddonItem> updateAddon(@PathVariable String id, @RequestBody AddonItem updated) {
        UUID targetUuid = null;
        try { targetUuid = UUID.fromString(id); } catch (Exception ignored) {}
        if (targetUuid == null) return ResponseEntity.notFound().build();
        return addonItemRepository.findById(targetUuid).map(existing -> {
            if (updated.getName() != null) existing.setName(updated.getName());
            if (updated.getPrice() != null) existing.setPrice(updated.getPrice());
            existing.setAvailable(updated.isAvailable());
            AddonItem saved = addonItemRepository.saveAndFlush(existing);
            try { activationService.syncAddonsToDisk(existing.getRestaurantId()); } catch (Exception ignored) {}
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping({"/addons/by-name", "/addons/delete-by-name"})
    public ResponseEntity<?> deleteAddonByName(
            @RequestParam("name") String name,
            @RequestParam(name = "restaurantId", required = false) String restaurantIdParam,
            @RequestParam(name = "syncCode", required = false) String syncCodeParam,
            @RequestHeader(name = "X-Sync-Code", required = false) String syncHeader) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Addon name parameter is required");
        }
        String effectiveCode = (syncCodeParam != null && !syncCodeParam.trim().isEmpty()) ? syncCodeParam : syncHeader;
        UUID rid = getEffectiveRestaurantId(restaurantIdParam, effectiveCode);
        String targetName = name.trim();
        List<AddonItem> addons = findAllAddonsForRestaurant(rid, effectiveCode);
        boolean deletedAny = false;
        for (AddonItem item : addons) {
            if (item.getName() != null && item.getName().trim().equalsIgnoreCase(targetName)) {
                addonItemRepository.delete(item);
                deletedAny = true;
            }
        }
        if (deletedAny) {
            addonItemRepository.flush();
            try { activationService.syncAddonsToDisk(rid); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/addons/{id}")
    public ResponseEntity<?> deleteAddon(
            @PathVariable String id,
            @RequestParam(name = "restaurantId", required = false) String restaurantIdParam,
            @RequestParam(name = "syncCode", required = false) String syncCodeParam,
            @RequestHeader(name = "X-Sync-Code", required = false) String syncHeader) {
        if (id == null || id.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Addon ID or name is required");
        }
        String effectiveCode = (syncCodeParam != null && !syncCodeParam.trim().isEmpty()) ? syncCodeParam : syncHeader;
        String trimmedId = id.trim();
        UUID targetUuid = null;
        try {
            targetUuid = UUID.fromString(trimmedId);
        } catch (Exception ignored) {}

        if (targetUuid != null) {
            final UUID finalUuid = targetUuid;
            Optional<AddonItem> found = addonItemRepository.findById(finalUuid);
            if (found.isPresent()) {
                AddonItem existing = found.get();
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
            }
        }

        // Fallback: search and delete by name or string ID matching across all restaurant ID aliases
        UUID rid = getEffectiveRestaurantId(restaurantIdParam, effectiveCode);
        List<AddonItem> addons = findAllAddonsForRestaurant(rid, effectiveCode);
        boolean deletedAny = false;
        for (AddonItem item : addons) {
            if (item.getName() != null && (item.getName().trim().equalsIgnoreCase(trimmedId)
                    || (item.getId() != null && item.getId().toString().equalsIgnoreCase(trimmedId)))) {
                addonItemRepository.delete(item);
                deletedAny = true;
            }
        }
        if (deletedAny) {
            addonItemRepository.flush();
            try { activationService.syncAddonsToDisk(rid); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok().build();
    }
}
