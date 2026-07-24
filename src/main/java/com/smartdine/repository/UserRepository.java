package com.smartdine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartdine.coreheart.AppUser;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {
    
    // For Admin login via Web/Desktop (Case-insensitive)
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    // For Staff login via PIN (Multi-tenant safe)
    java.util.List<AppUser> findByPinAndRestaurantId(String pin, UUID restaurantId);

    // Fetch active staff list for client applications
    java.util.List<AppUser> findByRestaurantIdAndRoleAndIsActiveTrue(UUID restaurantId, com.smartdine.coreheart.UserRole role);

    // Fetch all staff list regardless of active state
    java.util.List<AppUser> findByRestaurantIdAndRole(UUID restaurantId, com.smartdine.coreheart.UserRole role);
}