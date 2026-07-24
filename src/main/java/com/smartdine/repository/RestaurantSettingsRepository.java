package com.smartdine.repository;

import com.smartdine.coreheart.RestaurantSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantSettingsRepository extends JpaRepository<RestaurantSettings, UUID> {
    Optional<RestaurantSettings> findByRestaurantId(UUID restaurantId);
}
