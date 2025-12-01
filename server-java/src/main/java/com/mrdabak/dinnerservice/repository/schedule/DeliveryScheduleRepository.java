package com.mrdabak.dinnerservice.repository.schedule;

import com.mrdabak.dinnerservice.model.DeliverySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryScheduleRepository extends JpaRepository<DeliverySchedule, Long> {

    boolean existsByEmployeeIdAndReturnTimeAfterAndDepartureTimeBefore(Long employeeId,
                                                                       LocalDateTime start,
                                                                       LocalDateTime end);

    List<DeliverySchedule> findByEmployeeIdAndDepartureTimeBetween(Long employeeId,
                                                                   LocalDateTime start,
                                                                   LocalDateTime end);

    List<DeliverySchedule> findByDepartureTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<DeliverySchedule> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM DeliverySchedule s " +
            "WHERE s.employeeId = :employeeId AND s.orderId <> :orderId " +
            "AND s.status <> 'CANCELLED' " +
            "AND s.returnTime > :start AND s.departureTime < :end")
    boolean existsActiveOverlap(@Param("employeeId") Long employeeId,
                                @Param("orderId") Long orderId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}

