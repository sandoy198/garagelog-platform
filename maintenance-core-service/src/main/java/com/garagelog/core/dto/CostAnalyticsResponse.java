package com.garagelog.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostAnalyticsResponse {
    private UUID vehicleId;
    private String plateNumber;
    private String brandModel;
    private BigDecimal totalCost;
    private BigDecimal averageCostPerService;
    private Integer totalRecordedServices;
    private Integer totalDistanceTraveledKm;
    private BigDecimal costPerKm;
    private List<CategoryCostBreakdown> categoryBreakdowns;
}
