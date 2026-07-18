package com.smartdine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartdine.coreheart.Category;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByRestaurantId(UUID restaurantId);
}