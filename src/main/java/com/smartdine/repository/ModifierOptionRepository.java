package com.smartdine.repository;

import com.smartdine.coreheart.ModifierOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ModifierOptionRepository extends JpaRepository<ModifierOption, UUID> {
}
