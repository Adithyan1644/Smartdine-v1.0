package com.smartdine.repository;

import com.smartdine.coreheart.BillingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingConfigRepository extends JpaRepository<BillingConfig, Long> {

    Optional<BillingConfig> findFirstByOrderByIdAsc();

    Optional<BillingConfig> findByRestaurantId(UUID restaurantId);
}
