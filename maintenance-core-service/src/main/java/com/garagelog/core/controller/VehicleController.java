package com.garagelog.core.controller;

import com.garagelog.core.dto.CostAnalyticsResponse;
import com.garagelog.core.dto.TelemetryHistoryItemResponse;
import com.garagelog.core.dto.VehicleResponse;
import com.garagelog.core.service.VehicleMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleMaintenanceService vehicleMaintenanceService;

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleMaintenanceService.getAllVehicles());
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleMaintenanceService.getVehicleById(vehicleId));
    }

    @GetMapping("/{vehicleId}/telemetry-history")
    public ResponseEntity<List<TelemetryHistoryItemResponse>> getTelemetryHistory(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleMaintenanceService.getTelemetryHistory(vehicleId));
    }

    @GetMapping("/{vehicleId}/cost-analytics")
    public ResponseEntity<CostAnalyticsResponse> getCostAnalytics(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(vehicleMaintenanceService.getCostAnalytics(vehicleId));
    }
}
