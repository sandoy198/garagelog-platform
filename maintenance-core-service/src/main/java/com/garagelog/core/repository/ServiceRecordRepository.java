package com.garagelog.core.repository;

import com.garagelog.core.entity.ServiceRecord;
import com.garagelog.core.repository.projection.TelemetryHistoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, UUID> {

    List<ServiceRecord> findByVehicleIdOrderByServiceDateAsc(UUID vehicleId);

    @Query(value = """
        WITH enriched_records AS (
            SELECT 
                s.id AS record_id,
                s.vehicle_id AS vehicle_id,
                s.service_date AS service_date,
                s.odometer_at_service AS odometer_at_service,
                s.category AS category,
                s.workshop_name AS workshop_name,
                s.cost AS cost,
                s.mechanic_notes AS mechanic_notes,
                s.next_recommended_km AS next_recommended_km,
                s.created_at AS created_at,
                v.current_odometer AS current_vehicle_odometer,
                COALESCE(s.odometer_at_service - LAG(s.odometer_at_service) OVER (
                    PARTITION BY s.vehicle_id 
                    ORDER BY s.service_date ASC, s.created_at ASC
                ), 0) AS distance_since_last_service,
                SUM(s.cost) OVER (
                    PARTITION BY s.vehicle_id 
                    ORDER BY s.service_date ASC, s.created_at ASC
                ) AS cumulative_cost
            FROM service_records s
            JOIN vehicles v ON s.vehicle_id = v.id
            WHERE s.vehicle_id = :vehicleId
        )
        SELECT 
            record_id AS "recordId",
            vehicle_id AS "vehicleId",
            service_date AS "serviceDate",
            odometer_at_service AS "odometerAtService",
            category AS "category",
            workshop_name AS "workshopName",
            cost AS "cost",
            mechanic_notes AS "mechanicNotes",
            next_recommended_km AS "nextRecommendedKm",
            distance_since_last_service AS "distanceSinceLastService",
            cumulative_cost AS "cumulativeCost",
            CASE 
                WHEN next_recommended_km IS NULL THEN 'HEALTHY'
                WHEN current_vehicle_odometer >= next_recommended_km THEN 'OVERDUE'
                WHEN (next_recommended_km - current_vehicle_odometer) <= 300 THEN 'DUE_SOON'
                ELSE 'HEALTHY'
            END AS "maintenanceStatus"
        FROM enriched_records
        ORDER BY service_date ASC, created_at ASC
        """, nativeQuery = true)
    List<TelemetryHistoryProjection> findTelemetryHistoryByVehicleId(@Param("vehicleId") UUID vehicleId);
}
