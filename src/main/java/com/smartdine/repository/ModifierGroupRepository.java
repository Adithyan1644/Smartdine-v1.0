package com.smartdine.repository;

import com.smartdine.coreheart.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {
    List<ModifierGroup> findByRestaurantIdAndIsDeletedFalse(UUID restaurantId);
    List<ModifierGroup> findByRestaurantIdAndIsGlobalTrueAndIsDeletedFalse(UUID restaurantId);

    @Query("SELECT mg.isGlobal FROM ModifierGroup mg JOIN mg.options o WHERE o.id = :optionId")
    Boolean isOptionGlobal(@Param("optionId") UUID optionId);
}
