package com.mrdabak.dinnerservice.repository;

import com.mrdabak.dinnerservice.model.DinnerMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DinnerMenuItemRepository extends JpaRepository<DinnerMenuItem, Long> {
    List<DinnerMenuItem> findByDinnerTypeId(Long dinnerTypeId);
}




