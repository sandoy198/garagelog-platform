package com.garagelog.core.event;

import com.garagelog.core.entity.ServiceCategory;
import com.garagelog.core.entity.VehicleType;
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
    private VehicleType vehicleType;
    private LocalDate serviceDate;
    private Integer odometerAtService;
    private ServiceCategory category;
    private String workshopName;
    private BigDecimal cost;
    private String mechanicNotes;
    private Integer nextRecommendedKm;
    private String advisorMessage;
    private Instant timestamp;
}
