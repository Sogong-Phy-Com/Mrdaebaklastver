package com.mrdabak.dinnerservice.repository.order;

import com.mrdabak.dinnerservice.model.OrderChangeRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderChangeRequestItemRepository extends JpaRepository<OrderChangeRequestItem, Long> {
    List<OrderChangeRequestItem> findByChangeRequestId(Long changeRequestId);
    
    void deleteByChangeRequestId(Long changeRequestId);
}

