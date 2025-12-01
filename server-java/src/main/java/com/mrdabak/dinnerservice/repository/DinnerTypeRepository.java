package com.mrdabak.dinnerservice.repository;

import com.mrdabak.dinnerservice.model.DinnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DinnerTypeRepository extends JpaRepository<DinnerType, Long> {
}




