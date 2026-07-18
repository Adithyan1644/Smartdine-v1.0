package com.smartdine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smartdine.coreheart.Customer;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByRestaurantIdAndPhone(UUID restaurantId, String phone);
}
