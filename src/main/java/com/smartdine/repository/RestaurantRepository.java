package com.smartdine.repository;

import com.smartdine.coreheart.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    Optional<Restaurant> findBySyncCodeAndIsDeletedFalse(String syncCode);
    Optional<Restaurant> findByWaiterSyncCode(String waiterSyncCode);
    Optional<Restaurant> findByBillerSyncCode(String billerSyncCode);
    Optional<Restaurant> findByRestaurantId(UUID restaurantId);
    Optional<Restaurant> findByNameIgnoreCase(String name);
}
