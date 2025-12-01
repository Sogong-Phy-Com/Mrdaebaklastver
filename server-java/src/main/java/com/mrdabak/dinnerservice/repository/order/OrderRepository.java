package com.mrdabak.dinnerservice.repository.order;

import com.mrdabak.dinnerservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    List<Order> findByStatus(String status);
    
    @Query("SELECT o FROM Order o WHERE o.deliveryTime LIKE :datePattern%")
    List<Order> findByDeliveryTimeStartingWith(@Param("datePattern") String datePattern);
    
    @Query(value = "SELECT * FROM orders WHERE delivery_time >= :start AND delivery_time < :end", nativeQuery = true)
    List<Order> findByDeliveryTimeBetweenNative(@Param("start") String start, @Param("end") String end);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.deliveryTime = :deliveryTime AND o.deliveryAddress = :deliveryAddress")
    List<Order> findByUserIdAndDeliveryTimeAndDeliveryAddress(@Param("userId") Long userId, @Param("deliveryTime") String deliveryTime, @Param("deliveryAddress") String deliveryAddress);
}

