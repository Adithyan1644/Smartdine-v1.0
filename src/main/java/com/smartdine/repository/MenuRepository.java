package com.smartdine.repository;

import com.smartdine.coreheart.MenuItem;

public interface MenuRepository extends org.springframework.data.jpa.repository.JpaRepository<MenuItem, java.util.UUID> {
    java.util.List<MenuItem> findByRestaurantIdAndIsDeletedFalse(java.util.UUID restaurantId);
}