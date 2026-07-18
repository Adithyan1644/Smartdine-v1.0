package com.smartdine.repository;



import com.smartdine.coreheart.KOT;
import com.smartdine.coreheart.KOTStatus;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KOTRepository extends JpaRepository<KOT, UUID> {
    
    // For Kitchen Display System (KDS): Find all tickets that are not finished
    List<KOT> findByRestaurantIdAndOverallStatusIn(UUID restaurantId, List<KOTStatus> statuses);
    List<KOT> findByOrderId(UUID orderId);
    List<KOT> findByOrderIdIn(java.util.Collection<UUID> orderIds);
}