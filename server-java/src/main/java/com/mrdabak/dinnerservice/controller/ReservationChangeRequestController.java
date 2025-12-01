package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.dto.ReservationChangeRequestCreateDto;
import com.mrdabak.dinnerservice.dto.ReservationChangeRequestResponseDto;
import com.mrdabak.dinnerservice.service.OrderChangeRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReservationChangeRequestController {

    private final OrderChangeRequestService orderChangeRequestService;

    public ReservationChangeRequestController(OrderChangeRequestService orderChangeRequestService) {
        this.orderChangeRequestService = orderChangeRequestService;
    }

    @PostMapping("/reservations/{reservationId}/change-requests")
    public ResponseEntity<?> createChangeRequest(@PathVariable Long reservationId,
                                                 @Valid @RequestBody ReservationChangeRequestCreateDto requestDto,
                                                 Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            ReservationChangeRequestResponseDto responseDto =
                    orderChangeRequestService.createChangeRequest(reservationId, userId, requestDto);
            return ResponseEntity.ok(Map.of(
                    "message", "예약 변경 요청이 접수되었습니다.",
                    "change_request", responseDto
            ));
        } catch (RuntimeException e) {
            // 처리 중인 변경 요청이 이미 존재하는 경우 400 Bad Request로 반환
            if (e.getMessage() != null && e.getMessage().contains("처리 중인 예약 변경 요청이 이미 존재합니다")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", e.getMessage(),
                        "message", e.getMessage()
                ));
            }
            // 기타 RuntimeException은 400으로 반환
            if (e.getMessage() != null && (e.getMessage().contains("권한이 없습니다") ||
                    e.getMessage().contains("찾을 수 없습니다") ||
                    e.getMessage().contains("변경 가능 기한이 지났습니다") ||
                    e.getMessage().contains("수정할 수 없습니다"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", e.getMessage(),
                        "message", e.getMessage()
                ));
            }
            // 예상치 못한 RuntimeException은 500으로 반환
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage() != null ? e.getMessage() : "알 수 없는 오류가 발생했습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage() != null ? e.getMessage() : "알 수 없는 오류가 발생했습니다."
            ));
        }
    }

    @GetMapping("/reservations/{reservationId}/change-requests")
    public ResponseEntity<List<ReservationChangeRequestResponseDto>> getChangeRequests(@PathVariable Long reservationId,
                                                                                       Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<ReservationChangeRequestResponseDto> responses =
                orderChangeRequestService.getRequestsForOrder(reservationId, userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/change-requests/{requestId}")
    public ResponseEntity<ReservationChangeRequestResponseDto> getChangeRequestDetail(@PathVariable Long requestId,
                                                                                      Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ReservationChangeRequestResponseDto response =
                orderChangeRequestService.getRequestDetail(requestId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * PENDING 상태의 변경 요청을 수정합니다.
     * PUT /api/reservations/{reservationId}/change-requests/{changeRequestId}
     */
    @PutMapping("/reservations/{reservationId}/change-requests/{changeRequestId}")
    public ResponseEntity<?> updateChangeRequest(@PathVariable Long reservationId,
                                                 @PathVariable Long changeRequestId,
                                                 @Valid @RequestBody ReservationChangeRequestCreateDto requestDto,
                                                 Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            ReservationChangeRequestResponseDto responseDto =
                    orderChangeRequestService.updateChangeRequest(changeRequestId, userId, requestDto);
            return ResponseEntity.ok(Map.of(
                    "message", "변경 요청이 수정되었습니다. 관리자 승인 전까지는 다시 변경할 수 있습니다.",
                    "change_request", responseDto
            ));
        } catch (RuntimeException e) {
            // 클라이언트 오류 (권한 없음, 이미 처리됨, 변경 불가 등)
            if (e.getMessage() != null && (e.getMessage().contains("권한이 없습니다") ||
                    e.getMessage().contains("이미 처리된") ||
                    e.getMessage().contains("변경 가능 기한이 지났습니다") ||
                    e.getMessage().contains("수정할 수 없습니다"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", e.getMessage(),
                        "message", e.getMessage()
                ));
            }
            // 예상치 못한 RuntimeException은 500으로 반환
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage() != null ? e.getMessage() : "알 수 없는 오류가 발생했습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage() != null ? e.getMessage() : "알 수 없는 오류가 발생했습니다."
            ));
        }
    }
}

