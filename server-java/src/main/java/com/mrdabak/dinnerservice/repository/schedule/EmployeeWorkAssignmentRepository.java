package com.mrdabak.dinnerservice.repository.schedule;

import com.mrdabak.dinnerservice.model.EmployeeWorkAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeWorkAssignmentRepository extends JpaRepository<EmployeeWorkAssignment, Long> {
    
    List<EmployeeWorkAssignment> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
    
    List<EmployeeWorkAssignment> findByWorkDate(LocalDate workDate);
    
    @Query("SELECT e FROM EmployeeWorkAssignment e WHERE e.workDate = :workDate AND e.taskType = :taskType")
    List<EmployeeWorkAssignment> findByWorkDateAndTaskType(@Param("workDate") LocalDate workDate, @Param("taskType") String taskType);
    
    @Query("SELECT e FROM EmployeeWorkAssignment e WHERE e.employeeId = :employeeId AND e.workDate >= :startDate AND e.workDate <= :endDate")
    List<EmployeeWorkAssignment> findByEmployeeIdAndWorkDateBetween(@Param("employeeId") Long employeeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    void deleteByWorkDateAndEmployeeIdAndTaskType(LocalDate workDate, Long employeeId, String taskType);
    
    void deleteByWorkDate(LocalDate workDate);
}

