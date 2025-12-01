package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.dto.ReservationChangeRequestDecisionDto;
import com.mrdabak.dinnerservice.dto.ReservationChangeRequestResponseDto;
import com.mrdabak.dinnerservice.model.OrderChangeRequestStatus;
import com.mrdabak.dinnerservice.service.OrderChangeRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/change-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationChangeRequestController {

    private final OrderChangeRequestService orderChangeRequestService;

    public AdminReservationChangeRequestController(OrderChangeRequestService orderChangeRequestService) {
        this.orderChangeRequestService = orderChangeRequestService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationChangeRequestResponseDto>> listChangeRequests(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        OrderChangeRequestStatus targetStatus = parseStatus(status);
        LocalDate fromDate = from != null && !from.isBlank() ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null && !to.isBlank() ? LocalDate.parse(to) : null;
        List<ReservationChangeRequestResponseDto> responses =
                orderChangeRequestService.getAdminRequests(targetStatus, fromDate, toDate);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ReservationChangeRequestResponseDto> approveChangeRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody(required = false) ReservationChangeRequestDecisionDto decisionDto) {
        ReservationChangeRequestResponseDto response = orderChangeRequestService.approve(requestId, decisionDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ReservationChangeRequestResponseDto> rejectChangeRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody(required = false) ReservationChangeRequestDecisionDto decisionDto) {
        ReservationChangeRequestResponseDto response = orderChangeRequestService.reject(requestId, decisionDto);
        return ResponseEntity.ok(response);
    }

    private OrderChangeRequestStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OrderChangeRequestStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("지원하지 않는 변경 요청 상태입니다: " + raw);
        }
    }
}

