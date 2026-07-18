package com.smartdine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartdine.coreheart.Order;
import com.smartdine.coreheart.OrderStatus;

import java.util.List;
import java.util.UUID;
import java.util.Collection;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    // For Dashboard: Show only active/running orders
    List<Order> findByRestaurantIdAndStatusNot(UUID restaurantId, OrderStatus status);

    // Exclude multiple statuses (e.g. PAID and CANCELLED)
    List<Order> findByRestaurantIdAndStatusNotIn(UUID restaurantId, Collection<OrderStatus> statuses);
    
    // For Platform View: Show Zomato/Swiggy orders
    List<Order> findByRestaurantIdAndSourceNot(UUID restaurantId, String source);

    // For optimization: Fetch only today's orders directly
    List<Order> findByRestaurantIdAndStartedAtAfter(UUID restaurantId, java.time.LocalDateTime startOfToday);
}