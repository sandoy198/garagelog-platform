package com.garagelog.core.dto;

import com.garagelog.core.entity.Vehicle;
import com.garagelog.core.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private UUID id;
    private String plateNumber;
    private String brandModel;
    private VehicleType vehicleType;
    private Integer currentOdometer;
    private OffsetDateTime createdAt;

    public static VehicleResponse fromEntity(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .brandModel(vehicle.getBrandModel())
                .vehicleType(vehicle.getVehicleType())
                .currentOdometer(vehicle.getCurrentOdometer())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}
