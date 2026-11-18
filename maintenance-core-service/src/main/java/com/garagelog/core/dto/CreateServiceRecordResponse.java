package com.garagelog.core.dto;

import com.garagelog.core.entity.ServiceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceRecordResponse {
    private UUID id;
    private UUID vehicleId;
    private LocalDate serviceDate;
    private Integer odometerAtService;
    private ServiceCategory category;
    private String workshopName;
    private BigDecimal cost;
    private String mechanicNotes;
    private Integer nextRecommendedKm;
    private String advisorAdvice;
    private OffsetDateTime createdAt;
}
