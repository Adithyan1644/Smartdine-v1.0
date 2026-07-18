package com.smartdine.repository;

import java.util.List;
import java.util.UUID;

import com.smartdine.coreheart.DiningTable;

public interface TableRepository extends org.springframework.data.jpa.repository.JpaRepository<DiningTable, java.util.UUID> {
    java.util.List<DiningTable> findByRestaurantIdOrderByTableNumberAsc(java.util.UUID restaurantId);
    List<DiningTable> findByRestaurantId(UUID restaurantId);
}
