package com.garagelog.core.controller;

import com.garagelog.core.dto.CreateServiceRecordRequest;
import com.garagelog.core.dto.CreateServiceRecordResponse;
import com.garagelog.core.service.IdempotencyService;
import com.garagelog.core.service.VehicleMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class ServiceRecordController {

    private final VehicleMaintenanceService vehicleMaintenanceService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/{vehicleId}/records")
    public ResponseEntity<CreateServiceRecordResponse> createRecord(
            @PathVariable UUID vehicleId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateServiceRecordRequest request) {
        CreateServiceRecordResponse response = idempotencyService.executeIdempotent(
                idempotencyKey,
                () -> vehicleMaintenanceService.createServiceRecord(vehicleId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
