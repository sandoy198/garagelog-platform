package com.garagelog.core.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface TelemetryHistoryProjection {
    UUID getRecordId();
    UUID getVehicleId();
    LocalDate getServiceDate();
    Integer getOdometerAtService();
    String getCategory();
    String getWorkshopName();
    BigDecimal getCost();
    String getMechanicNotes();
    Integer getNextRecommendedKm();
    Integer getDistanceSinceLastService();
    BigDecimal getCumulativeCost();
    String getMaintenanceStatus();
}
