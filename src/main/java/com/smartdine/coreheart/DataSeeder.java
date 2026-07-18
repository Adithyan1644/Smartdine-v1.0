package com.smartdine.coreheart;

import com.smartdine.service.MdnsService;
import com.smartdine.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private MdnsService mdnsService;

    // We keep the Restaurant ID fixed so your Waiter phone never loses sync!
    private UUID REST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @Override
    public void run(String... args) {
        // Check if system is activated. If not, do not seed default mock data.
        boolean activated = systemConfigRepository.findAll().stream().anyMatch(SystemConfig::isActivated);
        if (!activated) {
            System.out.println("⚠️ [DataSeeder] System is not activated. Dynamic seeding will happen on activation.");
            return;
        }

        // Trigger mDNS register on startup if already activated
        SystemConfig config = systemConfigRepository.findAll().stream().findFirst().orElse(null);
        if (config != null) {
            REST_ID = config.getRestaurantId();
            TenantContext.setRestaurantId(config.getRestaurantId());
            mdnsService.registerService(config.getRestaurantId());
        }

        // 1. Seed Restaurant Admin
        if (userRepository.findByUsername("admin").isEmpty()) {
            AppUser admin = new AppUser();
            admin.setRestaurantId(REST_ID); // Fixed Restaurant ID
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.ADMIN);
            admin.setFullName("Super Admin");
            admin.setActive(true);
            admin.setDeleted(false);
            userRepository.save(admin);
            System.out.println("✅ Admin seeded successfully.");
        }

        // Seed Waiter Staff (matching Waiter App default PIN 4022)
        if (userRepository.findByUsername("waiter").isEmpty()) {
            AppUser waiter = new AppUser();
            waiter.setRestaurantId(REST_ID);
            waiter.setUsername("waiter");
            waiter.setPassword(passwordEncoder.encode("waiter123"));
            waiter.setRole(UserRole.WAITER);
            waiter.setFullName("Arjun Mehta");
            waiter.setPin("4022");
            waiter.setActive(true);
            waiter.setDeleted(false);
            userRepository.save(waiter);
            System.out.println("✅ Waiter seeded successfully.");
        }

        // Seed Kitchen Staff (matching KDS App default PIN 5050)
        if (userRepository.findByUsername("kitchen").isEmpty()) {
            AppUser kitchen = new AppUser();
            kitchen.setRestaurantId(REST_ID);
            kitchen.setUsername("kitchen");
            kitchen.setPassword(passwordEncoder.encode("kitchen123"));
            kitchen.setRole(UserRole.KITCHEN);
            kitchen.setFullName("Main Kitchen");
            kitchen.setPin("5050");
            kitchen.setActive(true);
            kitchen.setDeleted(false);
            userRepository.save(kitchen);
            System.out.println("✅ Kitchen staff seeded successfully.");
        }

        // Seeding of categories, tables, and menus has been disabled for professional live configuration.
    }
}