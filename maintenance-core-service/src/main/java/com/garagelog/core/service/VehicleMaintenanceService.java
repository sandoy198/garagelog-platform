package com.garagelog.core.service;

import com.garagelog.core.dto.CategoryCostBreakdown;
import com.garagelog.core.dto.CostAnalyticsResponse;
import com.garagelog.core.dto.CreateServiceRecordRequest;
import com.garagelog.core.dto.CreateServiceRecordResponse;
import com.garagelog.core.dto.TelemetryHistoryItemResponse;
import com.garagelog.core.dto.VehicleResponse;
import com.garagelog.core.entity.ServiceCategory;
import com.garagelog.core.entity.ServiceRecord;
import com.garagelog.core.entity.Vehicle;
import com.garagelog.core.entity.VehicleType;
import com.garagelog.core.event.ServiceEventPublisher;
import com.garagelog.core.event.ServiceLoggedEvent;
import com.garagelog.core.exception.BadRequestException;
import com.garagelog.core.exception.ResourceNotFoundException;
import com.garagelog.core.repository.ServiceRecordRepository;
import com.garagelog.core.repository.VehicleRepository;
import com.garagelog.core.repository.projection.TelemetryHistoryProjection;
import com.garagelog.core.strategy.MaintenanceAdvisorStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VehicleMaintenanceService {

    private static final String LATEST_STATUS_PREFIX = "vehicle:latest-status:";

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final Map<VehicleType, MaintenanceAdvisorStrategy> advisorStrategies;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ServiceEventPublisher serviceEventPublisher;

    public VehicleMaintenanceService(
            VehicleRepository vehicleRepository,
            ServiceRecordRepository serviceRecordRepository,
            List<MaintenanceAdvisorStrategy> strategies,
            RedisTemplate<String, Object> redisTemplate,
            ServiceEventPublisher serviceEventPublisher) {
        this.vehicleRepository = vehicleRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.advisorStrategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MaintenanceAdvisorStrategy::getSupportedType,
                        Function.identity()
                ));
        this.redisTemplate = redisTemplate;
        this.serviceEventPublisher = serviceEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(VehicleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(UUID vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);
        return VehicleResponse.fromEntity(vehicle);
    }

    @Transactional
    @CacheEvict(value = "vehicle_cost_analytics", key = "#vehicleId")
    public CreateServiceRecordResponse createServiceRecord(UUID vehicleId, CreateServiceRecordRequest request) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        if (request.getOdometerAtService() < vehicle.getCurrentOdometer()) {
            throw new BadRequestException(String.format(
                    "Odometer servis (%d km) tidak boleh lebih kecil dari odometer tercatat kendaraan saat ini (%d km)",
                    request.getOdometerAtService(), vehicle.getCurrentOdometer()
            ));
        }

        vehicle.setCurrentOdometer(request.getOdometerAtService());
        vehicleRepository.save(vehicle);

        ServiceRecord record = ServiceRecord.builder()
                .vehicle(vehicle)
                .serviceDate(request.getServiceDate())
                .odometerAtService(request.getOdometerAtService())
                .category(request.getCategory())
                .workshopName(request.getWorkshopName())
                .cost(request.getCost())
                .mechanicNotes(request.getMechanicNotes())
                .nextRecommendedKm(request.getNextRecommendedKm())
                .createdAt(OffsetDateTime.now())
                .build();

        ServiceRecord savedRecord = serviceRecordRepository.save(record);

        MaintenanceAdvisorStrategy advisor = advisorStrategies.get(vehicle.getVehicleType());
        String advice = (advisor != null)
                ? advisor.evaluateNextServiceAdvice(vehicle, savedRecord)
                : "Jadwal servis standar berlaku untuk tipe kendaraan ini.";

        ServiceLoggedEvent event = ServiceLoggedEvent.builder()
                .eventId(UUID.randomUUID())
                .recordId(savedRecord.getId())
                .vehicleId(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .brandModel(vehicle.getBrandModel())
                .vehicleType(vehicle.getVehicleType())
                .serviceDate(savedRecord.getServiceDate())
                .odometerAtService(savedRecord.getOdometerAtService())
                .category(savedRecord.getCategory())
                .workshopName(savedRecord.getWorkshopName())
                .cost(savedRecord.getCost())
                .mechanicNotes(savedRecord.getMechanicNotes())
                .nextRecommendedKm(savedRecord.getNextRecommendedKm())
                .advisorMessage(advice)
                .timestamp(Instant.now())
                .build();

        Map<String, Object> latestStatusSummary = new HashMap<>();
        latestStatusSummary.put("vehicleId", vehicle.getId().toString());
        latestStatusSummary.put("plateNumber", vehicle.getPlateNumber());
        latestStatusSummary.put("brandModel", vehicle.getBrandModel());
        latestStatusSummary.put("currentOdometer", vehicle.getCurrentOdometer());
        latestStatusSummary.put("lastServiceDate", savedRecord.getServiceDate().toString());
        latestStatusSummary.put("lastCategory", savedRecord.getCategory().name());
        latestStatusSummary.put("advisorAdvice", advice);
        latestStatusSummary.put("updatedAt", Instant.now().toString());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                serviceEventPublisher.publishServiceLogged(event);
                redisTemplate.opsForValue().set(
                        LATEST_STATUS_PREFIX + vehicle.getId(),
                        latestStatusSummary,
                        Duration.ofHours(24)
                );
                log.info("Updated Redis latest status for vehicleId={}", vehicle.getId());
            }
        });

        return CreateServiceRecordResponse.builder()
                .id(savedRecord.getId())
                .vehicleId(vehicle.getId())
                .serviceDate(savedRecord.getServiceDate())
                .odometerAtService(savedRecord.getOdometerAtService())
                .category(savedRecord.getCategory())
                .workshopName(savedRecord.getWorkshopName())
                .cost(savedRecord.getCost())
                .mechanicNotes(savedRecord.getMechanicNotes())
                .nextRecommendedKm(savedRecord.getNextRecommendedKm())
                .advisorAdvice(advice)
                .createdAt(savedRecord.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TelemetryHistoryItemResponse> getTelemetryHistory(UUID vehicleId) {
        findVehicleOrThrow(vehicleId);
        List<TelemetryHistoryProjection> projections = serviceRecordRepository.findTelemetryHistoryByVehicleId(vehicleId);
        return projections.stream()
                .map(TelemetryHistoryItemResponse::fromProjection)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "vehicle_cost_analytics", key = "#vehicleId")
    public CostAnalyticsResponse getCostAnalytics(UUID vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);
        List<ServiceRecord> records = serviceRecordRepository.findByVehicleIdOrderByServiceDateAsc(vehicleId);

        if (records.isEmpty()) {
            return CostAnalyticsResponse.builder()
                    .vehicleId(vehicle.getId())
                    .plateNumber(vehicle.getPlateNumber())
                    .brandModel(vehicle.getBrandModel())
                    .totalCost(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .averageCostPerService(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalRecordedServices(0)
                    .totalDistanceTraveledKm(0)
                    .costPerKm(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .categoryBreakdowns(List.of())
                    .build();
        }

        DoubleSummaryStatistics overallStats = records.stream()
                .collect(Collectors.summarizingDouble(r -> r.getCost().doubleValue()));

        double totalCostVal = overallStats.getSum();
        double avgCostVal = overallStats.getAverage();
        int totalRecords = (int) overallStats.getCount();

        Map<ServiceCategory, DoubleSummaryStatistics> categoryStats = records.stream()
                .collect(Collectors.groupingBy(
                        ServiceRecord::getCategory,
                        Collectors.summarizingDouble(r -> r.getCost().doubleValue())
                ));

        List<CategoryCostBreakdown> categoryBreakdowns = categoryStats.entrySet().stream()
                .map(entry -> {
                    double catTotal = entry.getValue().getSum();
                    long catCount = entry.getValue().getCount();
                    double percentage = totalCostVal > 0 ? (catTotal / totalCostVal) * 100.0 : 0.0;
                    return CategoryCostBreakdown.builder()
                            .category(entry.getKey())
                            .totalCost(BigDecimal.valueOf(catTotal).setScale(2, RoundingMode.HALF_UP))
                            .transactionCount(catCount)
                            .percentageOfTotal(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(CategoryCostBreakdown::getTotalCost).reversed())
                .collect(Collectors.toList());

        int minOdo = records.stream()
                .mapToInt(ServiceRecord::getOdometerAtService)
                .min()
                .orElse(0);

        int maxOdo = Math.max(
                vehicle.getCurrentOdometer(),
                records.stream()
                        .mapToInt(ServiceRecord::getOdometerAtService)
                        .max()
                        .orElse(0)
        );

        int totalDistanceTraveled = Math.max(0, maxOdo - minOdo);
        double costPerKmVal = totalDistanceTraveled > 0 ? (totalCostVal / totalDistanceTraveled) : 0.0;

        return CostAnalyticsResponse.builder()
                .vehicleId(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .brandModel(vehicle.getBrandModel())
                .totalCost(BigDecimal.valueOf(totalCostVal).setScale(2, RoundingMode.HALF_UP))
                .averageCostPerService(BigDecimal.valueOf(avgCostVal).setScale(2, RoundingMode.HALF_UP))
                .totalRecordedServices(totalRecords)
                .totalDistanceTraveledKm(totalDistanceTraveled)
                .costPerKm(BigDecimal.valueOf(costPerKmVal).setScale(2, RoundingMode.HALF_UP))
                .categoryBreakdowns(categoryBreakdowns)
                .build();
    }

    private Vehicle findVehicleOrThrow(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Kendaraan dengan ID " + vehicleId + " tidak ditemukan"));
    }
}
