package com.garagelog.diagnostic.consumer;

import com.garagelog.diagnostic.document.ServiceDiagnosticDocument;
import com.garagelog.diagnostic.event.ServiceLoggedEvent;
import com.garagelog.diagnostic.repository.ServiceDiagnosticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceEventConsumer {

    private static final BigDecimal URGENT_COST_THRESHOLD = new BigDecimal("500000");
    private static final List<String> URGENT_KEYWORDS = List.of(
            "aus", "getar", "bocor", "urgent", "rusak", "soh"
    );

    private final ServiceDiagnosticRepository diagnosticRepository;

    @KafkaListener(topics = "garagelog.service.logged", groupId = "diagnostic-search-group")
    public void consumeServiceLoggedEvent(ServiceLoggedEvent event) {
        log.info("Consumed ServiceLoggedEvent: eventId={}, recordId={}, vehicleId={}, plate={}",
                event.getEventId(), event.getRecordId(), event.getVehicleId(), event.getPlateNumber());

        boolean isUrgent = evaluateUrgency(event);

        ServiceDiagnosticDocument document = ServiceDiagnosticDocument.builder()
                .recordId(event.getRecordId().toString())
                .vehicleId(event.getVehicleId().toString())
                .plateNumber(event.getPlateNumber())
                .brandModel(event.getBrandModel())
                .vehicleType(event.getVehicleType())
                .serviceDate(event.getServiceDate())
                .odometerAtService(event.getOdometerAtService())
                .category(event.getCategory())
                .workshopName(event.getWorkshopName())
                .cost(event.getCost())
                .mechanicNotes(event.getMechanicNotes())
                .advisorMessage(event.getAdvisorMessage())
                .urgentMaintenanceFlag(isUrgent)
                .indexedAt(Instant.now())
                .build();

        diagnosticRepository.save(document);

        if (isUrgent) {
            log.warn("[URGENT MAINTENANCE ALERT] Kendaraan {} ({}) membutuhkan perhatian khusus! RecordId={}, Cost={}, Notes=\"{}\"",
                    event.getBrandModel(),
                    event.getPlateNumber(),
                    event.getRecordId(),
                    event.getCost(),
                    event.getMechanicNotes());
        } else {
            log.info("Indexed ServiceDiagnosticDocument successfully: recordId={}, urgent=false", event.getRecordId());
        }
    }

    private boolean evaluateUrgency(ServiceLoggedEvent event) {
        if (event.getCost() != null && event.getCost().compareTo(URGENT_COST_THRESHOLD) >= 0) {
            return true;
        }
        if (event.getMechanicNotes() != null && !event.getMechanicNotes().isBlank()) {
            String normalizedNotes = event.getMechanicNotes().toLowerCase(Locale.ROOT);
            return URGENT_KEYWORDS.stream().anyMatch(normalizedNotes::contains);
        }
        return false;
    }
}
