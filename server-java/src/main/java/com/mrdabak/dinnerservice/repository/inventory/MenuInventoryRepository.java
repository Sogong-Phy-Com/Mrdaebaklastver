package com.mrdabak.dinnerservice.repository.inventory;

import com.mrdabak.dinnerservice.model.MenuInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MenuInventoryRepository extends JpaRepository<MenuInventory, Long> {
    Optional<MenuInventory> findByMenuItemId(Long menuItemId);
}

