package com.smartdine.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartdine.coreheart.*;
import com.smartdine.repository.*;

@Service
public class CloudDatabaseSeederService {

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Transactional
    public void seedDefaultRestaurantData(UUID restaurantId) {
        if (restaurantId == null) return;

        System.out.println("🌱 [CloudDatabaseSeeder] Seeding default restaurant data for tenant: " + restaurantId);

        // 1. Seed Baseline Dining Tables (AC Area & Garden Area)
        List<DiningTable> existingTables = tableRepository.findByRestaurantId(restaurantId);
        if (existingTables.isEmpty()) {
            String[] acTables = {"T-01", "T-02", "T-03", "T-04"};
            for (String tNum : acTables) {
                DiningTable table = new DiningTable();
                table.setRestaurantId(restaurantId);
                table.setTableNumber(tNum);
                table.setAreaName("AC Area");
                table.setCapacity(4);
                table.setStatus(TableStatus.AVAILABLE);
                tableRepository.save(table);
            }

            String[] gardenTables = {"G-01", "G-02", "G-03", "G-04"};
            for (String tNum : gardenTables) {
                DiningTable table = new DiningTable();
                table.setRestaurantId(restaurantId);
                table.setTableNumber(tNum);
                table.setAreaName("Garden Area");
                table.setCapacity(4);
                table.setStatus(TableStatus.AVAILABLE);
                tableRepository.save(table);
            }
            System.out.println("✅ [CloudDatabaseSeeder] Seeded 8 dining tables (AC Area & Garden Area).");
        }

        // 2. Seed Baseline Menu Categories
        List<Category> existingCats = categoryRepository.findByRestaurantId(restaurantId);
        if (existingCats.isEmpty()) {
            String[] defaultCategories = {"Beverages", "Starters", "Main Course", "Desserts"};
            for (String catName : defaultCategories) {
                Category cat = new Category();
                cat.setRestaurantId(restaurantId);
                cat.setName(catName);
                categoryRepository.save(cat);
            }
            System.out.println("✅ [CloudDatabaseSeeder] Seeded 4 menu categories.");
        }

        // 3. Seed Baseline Menu Items
        List<MenuItem> existingMenuItems = menuRepository.findByRestaurantId(restaurantId);
        if (existingMenuItems.isEmpty()) {
            List<Category> categories = categoryRepository.findByRestaurantId(restaurantId);

            Category bevCat = categories.stream().filter(c -> "Beverages".equalsIgnoreCase(c.getName())).findFirst().orElse(null);
            Category starterCat = categories.stream().filter(c -> "Starters".equalsIgnoreCase(c.getName())).findFirst().orElse(null);
            Category mainCat = categories.stream().filter(c -> "Main Course".equalsIgnoreCase(c.getName())).findFirst().orElse(null);
            Category dessertCat = categories.stream().filter(c -> "Desserts".equalsIgnoreCase(c.getName())).findFirst().orElse(null);

            // Item 1: Filter Coffee
            MenuItem fc = new MenuItem();
            fc.setRestaurantId(restaurantId);
            fc.setName("Filter Coffee");
            fc.setShortCode("FC");
            fc.setCategoryName("Beverages");
            fc.setCategory(bevCat);
            fc.setPrice(new BigDecimal("30.00"));
            fc.setVeg(true);
            fc.setAvailable(true);
            menuRepository.save(fc);

            // Item 2: Fresh Lime Soda
            MenuItem fls = new MenuItem();
            fls.setRestaurantId(restaurantId);
            fls.setName("Fresh Lime Soda");
            fls.setShortCode("FLS");
            fls.setCategoryName("Beverages");
            fls.setCategory(bevCat);
            fls.setPrice(new BigDecimal("50.00"));
            fls.setVeg(true);
            fls.setAvailable(true);
            menuRepository.save(fls);

            // Item 3: Veg Frankie
            MenuItem vf = new MenuItem();
            vf.setRestaurantId(restaurantId);
            vf.setName("Veg Frankie");
            vf.setShortCode("VF");
            vf.setCategoryName("Starters");
            vf.setCategory(starterCat);
            vf.setPrice(new BigDecimal("90.00"));
            vf.setVeg(true);
            vf.setAvailable(true);
            menuRepository.save(vf);

            // Item 4: Chicken Tikka
            MenuItem ct = new MenuItem();
            ct.setRestaurantId(restaurantId);
            ct.setName("Chicken Tikka");
            ct.setShortCode("CT");
            ct.setCategoryName("Starters");
            ct.setCategory(starterCat);
            ct.setPrice(new BigDecimal("180.00"));
            ct.setVeg(false);
            ct.setAvailable(true);
            menuRepository.save(ct);

            // Item 5: Masala Dosa
            MenuItem md = new MenuItem();
            md.setRestaurantId(restaurantId);
            md.setName("Masala Dosa");
            md.setShortCode("MD");
            md.setCategoryName("Main Course");
            md.setCategory(mainCat);
            md.setPrice(new BigDecimal("120.00"));
            md.setVeg(true);
            md.setAvailable(true);
            menuRepository.save(md);

            // Item 6: Butter Chicken
            MenuItem bc = new MenuItem();
            bc.setRestaurantId(restaurantId);
            bc.setName("Butter Chicken");
            bc.setShortCode("BC");
            bc.setCategoryName("Main Course");
            bc.setCategory(mainCat);
            bc.setPrice(new BigDecimal("240.00"));
            bc.setVeg(false);
            bc.setAvailable(true);
            menuRepository.save(bc);

            // Item 7: Chocolate Brownie
            MenuItem cb = new MenuItem();
            cb.setRestaurantId(restaurantId);
            cb.setName("Chocolate Brownie");
            cb.setShortCode("CB");
            cb.setCategoryName("Desserts");
            cb.setCategory(dessertCat);
            cb.setPrice(new BigDecimal("110.00"));
            cb.setVeg(true);
            cb.setAvailable(true);
            menuRepository.save(cb);

            System.out.println("✅ [CloudDatabaseSeeder] Seeded 7 baseline menu items.");
        }

        // 4. Seed Baseline Waiters
        List<AppUser> existingWaiters = userRepository.findByRestaurantIdAndRole(restaurantId, UserRole.WAITER);
        if (existingWaiters.isEmpty()) {
            String shortId = restaurantId.toString().substring(0, 6);

            AppUser rahul = new AppUser();
            rahul.setRestaurantId(restaurantId);
            rahul.setFullName("Rahul (Waiter)");
            rahul.setUsername("rahul_" + shortId);
            rahul.setPassword("1234");
            rahul.setPin("1234");
            rahul.setRole(UserRole.WAITER);
            rahul.setActive(true);
            userRepository.save(rahul);

            AppUser priya = new AppUser();
            priya.setRestaurantId(restaurantId);
            priya.setFullName("Priya (Waiter)");
            priya.setUsername("priya_" + shortId);
            priya.setPassword("5678");
            priya.setPin("5678");
            priya.setRole(UserRole.WAITER);
            priya.setActive(true);
            userRepository.save(priya);

            System.out.println("✅ [CloudDatabaseSeeder] Seeded 2 baseline waiters (Rahul & Priya).");
        }
    }

    /**
     * Seeds all existing restaurants in the database that currently have 0 menu items.
     */
    @Transactional
    public void seedAllUnseededRestaurants() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        for (Restaurant r : restaurants) {
            if (r.getRestaurantId() != null) {
                List<MenuItem> items = menuRepository.findByRestaurantId(r.getRestaurantId());
                if (items.isEmpty()) {
                    System.out.println("🔍 [CloudDatabaseSeeder] Found unseeded restaurant: " + r.getName() + " (" + r.getSyncCode() + ")");
                    seedDefaultRestaurantData(r.getRestaurantId());
                }
            }
        }
    }
}
