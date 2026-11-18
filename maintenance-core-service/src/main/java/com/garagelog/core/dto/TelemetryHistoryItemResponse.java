package com.garagelog.core.dto;

import com.garagelog.core.repository.projection.TelemetryHistoryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryHistoryItemResponse {
    private UUID recordId;
    private UUID vehicleId;
    private LocalDate serviceDate;
    private Integer odometerAtService;
    private String category;
    private String workshopName;
    private BigDecimal cost;
    private String mechanicNotes;
    private Integer nextRecommendedKm;
    private Integer distanceSinceLastService;
    private BigDecimal cumulativeCost;
    private String maintenanceStatus;

    public static TelemetryHistoryItemResponse fromProjection(TelemetryHistoryProjection proj) {
        return TelemetryHistoryItemResponse.builder()
                .recordId(proj.getRecordId())
                .vehicleId(proj.getVehicleId())
                .serviceDate(proj.getServiceDate())
                .odometerAtService(proj.getOdometerAtService())
                .category(proj.getCategory())
                .workshopName(proj.getWorkshopName())
                .cost(proj.getCost())
                .mechanicNotes(proj.getMechanicNotes())
                .nextRecommendedKm(proj.getNextRecommendedKm())
                .distanceSinceLastService(proj.getDistanceSinceLastService())
                .cumulativeCost(proj.getCumulativeCost())
                .maintenanceStatus(proj.getMaintenanceStatus())
                .build();
    }
}
