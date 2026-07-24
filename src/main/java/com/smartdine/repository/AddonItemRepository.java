package com.smartdine.repository;

import com.smartdine.coreheart.AddonItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AddonItemRepository extends JpaRepository<AddonItem, UUID> {
    List<AddonItem> findByRestaurantId(UUID restaurantId);
    List<AddonItem> findByRestaurantIdAndIsAvailableTrue(UUID restaurantId);
}
