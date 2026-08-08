package com.smartdine.service;

import com.smartdine.coreheart.Category;
import com.smartdine.coreheart.MenuItem;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.repository.CategoryRepository;
import com.smartdine.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private com.smartdine.controller.TunnelWebSocketHandler tunnelWebSocketHandler;

    @Transactional
    public MenuItem saveItem(MenuItem item) {
        // 1. Assign Restaurant ID from Token
        item.setRestaurantId(TenantContext.getRestaurantId());

        // 2. Fetch the actual Category from DB to get its Name
        if (item.getCategory() != null && item.getCategory().getId() != null) {
            Category cat = categoryRepository.findById(item.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            
            // 3. SET THE NAME MANUALLY (This fixes the NULL issue)
            item.setCategoryName(cat.getName());
        }

        MenuItem savedItem = menuRepository.save(item);

        // Broadcast the update to all waiters/clients locally
        try {
            String topic = "/topic/menu/" + savedItem.getRestaurantId().toString();
            messagingTemplate.convertAndSend(topic, savedItem);
            System.out.println("✅ Broadcasted menu item update: " + savedItem.getName() + " to topic " + topic);
        } catch (Exception e) {
            System.err.println("❌ Failed to broadcast menu item update: " + e.getMessage());
        }

        // Direction B: Push config update down Cloud Tunnel to local restaurant PC
        if (tunnelWebSocketHandler != null) {
            tunnelWebSocketHandler.sendConfigUpdate(savedItem.getRestaurantId(), "MENU_ITEM", savedItem);
        }

        return savedItem;
    }

    @Transactional
    public MenuItem toggleAvailability(UUID id, boolean available) {
        MenuItem item = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        item.setAvailable(available);
        MenuItem saved = menuRepository.saveAndFlush(item);
        try {
            String topic = "/topic/menu/" + saved.getRestaurantId().toString();
            messagingTemplate.convertAndSend(topic, saved);
            System.out.println("✅ Broadcasted menu item availability update: " + saved.getName() + " to topic " + topic);
        } catch (Exception e) {
            System.err.println("❌ Failed to broadcast availability update: " + e.getMessage());
        }

        if (tunnelWebSocketHandler != null) {
            tunnelWebSocketHandler.sendConfigUpdate(saved.getRestaurantId(), "MENU_ITEM", saved);
        }

        return saved;
    }

    @Transactional
    public MenuItem toggleTodaysMenu(UUID id, boolean active) {
        MenuItem item = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        item.setTodaysMenu(active);
        MenuItem saved = menuRepository.saveAndFlush(item);
        try {
            String topic = "/topic/menu/" + saved.getRestaurantId().toString();
            messagingTemplate.convertAndSend(topic, saved);
            System.out.println("✅ Broadcasted today's menu toggle: " + saved.getName() + " active=" + active);
        } catch (Exception e) {
            System.err.println("❌ Failed to broadcast today's menu toggle: " + e.getMessage());
        }

        if (tunnelWebSocketHandler != null) {
            tunnelWebSocketHandler.sendConfigUpdate(saved.getRestaurantId(), "MENU_ITEM", saved);
        }

        return saved;
    }

    @Transactional
    public boolean deleteItem(String idParam) {
        if (idParam == null || idParam.trim().isEmpty()) {
            return false;
        }

        String trimmedId = idParam.trim();
        UUID targetUuid = null;
        try {
            targetUuid = UUID.fromString(trimmedId);
        } catch (Exception ignored) {}

        MenuItem item = null;
        if (targetUuid != null) {
            item = menuRepository.findById(targetUuid).orElse(null);
        }

        // Fallback: search database by string ID, shortCode, or item name for tenant
        if (item == null) {
            UUID rid = TenantContext.getRestaurantId();
            if (rid != null) {
                java.util.List<MenuItem> allItems = menuRepository.findByRestaurantIdAndIsDeletedFalse(rid);
                item = allItems.stream()
                        .filter(m -> (m.getId() != null && m.getId().toString().equalsIgnoreCase(trimmedId))
                                || (m.getShortCode() != null && m.getShortCode().equalsIgnoreCase(trimmedId))
                                || (m.getName() != null && m.getName().equalsIgnoreCase(trimmedId)))
                        .findFirst()
                        .orElse(null);
            }
        }

        // If item not found in DB (e.g. transient frontend item or already removed), return true so UI updates cleanly
        if (item == null) {
            return true;
        }

        UUID restaurantId = item.getRestaurantId();
        menuRepository.delete(item);
        menuRepository.flush();

        if (restaurantId != null) {
            try {
                String topic = "/topic/menu/" + restaurantId.toString();
                messagingTemplate.convertAndSend(topic, item);
                System.out.println("✅ Broadcasted menu item deletion: " + item.getName() + " to topic " + topic);
            } catch (Exception e) {
                System.err.println("❌ Failed to broadcast menu item deletion: " + e.getMessage());
            }

            if (tunnelWebSocketHandler != null) {
                tunnelWebSocketHandler.sendConfigUpdate(restaurantId, "MENU_ITEM_DELETED", item);
            }
        }

        return true;
    }
}