package com.garagelog.diagnostic.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLoggedEvent {
    private UUID eventId;
    private UUID recordId;
    private UUID vehicleId;
    private String plateNumber;
    private String brandModel;
    private String vehicleType;
    private LocalDate serviceDate;
    private Integer odometerAtService;
    private String category;
    private String workshopName;
    private BigDecimal cost;
    private String mechanicNotes;
    private Integer nextRecommendedKm;
    private String advisorMessage;
    private Instant timestamp;
}
