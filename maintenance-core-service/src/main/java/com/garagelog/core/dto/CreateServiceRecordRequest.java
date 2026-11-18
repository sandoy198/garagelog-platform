package com.garagelog.core.dto;

import com.garagelog.core.entity.ServiceCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceRecordRequest {

    @NotNull(message = "serviceDate is required")
    private LocalDate serviceDate;

    @NotNull(message = "odometerAtService is required")
    @Min(value = 0, message = "odometerAtService must be positive or zero")
    private Integer odometerAtService;

    @NotNull(message = "category is required")
    private ServiceCategory category;

    @NotBlank(message = "workshopName cannot be blank")
    private String workshopName;

    @NotNull(message = "cost is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "cost cannot be negative")
    private BigDecimal cost;

    private String mechanicNotes;

    private Integer nextRecommendedKm;
}
