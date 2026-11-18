# GarageLog — Personal Vehicle Maintenance & Telemetry Platform

**GarageLog** adalah platform backend *event-driven microservices* untuk pencatatan riwayat pemeliharaan kendaraan, analitik biaya operasional, pelacakan telemetri odometer, dan pencarian diagnosa catatan mekanik secara cepat (*full-text search*).

Platform ini dirancang menggunakan **Java 17**, **Spring Boot 3**, **PostgreSQL 16**, **Redis 7**, **Apache Kafka (KRaft Mode)**, dan **Elasticsearch 8** yang seluruhnya terkontainerisasi dengan Docker.

---

## Arsitektur Sistem

```text
+--------+         HTTP REST (8081)         +-----------------------------+
|        | -------------------------------> |  maintenance-core-service   |
| Client |         (Idempotency Key)        |  (Spring Boot 3 / Java 17)  |
+--------+                                  +-----------------------------+
    |                                          |          |            |
    |                                          | JPA/SQL  | Redis      | Kafka Producer
    |                                          |          | Cache &    | (afterCommit)
    |                                          v          | Idempotent v
    |                                  +------------+ +-------+  +------------------------+
    |                                  | PostgreSQL | | Redis |  | Apache Kafka (KRaft)   |
    |                                  |    (16)    | |  (7)  |  | topic:                 |
    |                                  +------------+ +-------+  | garagelog.service.logged
    |                                                            +------------------------+
    |                                                                        |
    |      HTTP REST (8082)                 +-----------------------------+  | Kafka
    | <-----------------------------------> |  diagnostic-search-service  | <+ Consumer
    |   (Full-Text Search & Urgent Alerts)  |  (Spring Boot 3 / Java 17)  |
                                            +-----------------------------+
                                                          |
                                                          v
                                                +-------------------+
                                                | Elasticsearch (8) |
                                                +-------------------+
```

---

## Tabel Pemetaan Fitur Teknis (Technical Assessment Matrix)

| Fitur / Konsep Teknis | Implementasi Utama (File / Class) | Penjelasan Ringkas |
| :--- | :--- | :--- |
| **Spring IoC — Strategy Pattern** | [`MaintenanceAdvisorStrategy.java`](maintenance-core-service/src/main/java/com/garagelog/core/strategy/MaintenanceAdvisorStrategy.java)<br>[`IceMotorcycleAdvisor.java`](maintenance-core-service/src/main/java/com/garagelog/core/strategy/IceMotorcycleAdvisor.java)<br>[`EvMotorcycleAdvisor.java`](maintenance-core-service/src/main/java/com/garagelog/core/strategy/EvMotorcycleAdvisor.java)<br>[`CarAdvisor.java`](maintenance-core-service/src/main/java/com/garagelog/core/strategy/CarAdvisor.java)<br>[`VehicleMaintenanceService.java`](maintenance-core-service/src/main/java/com/garagelog/core/service/VehicleMaintenanceService.java) | Seluruh bean `@Component` di-inject sebagai `List<MaintenanceAdvisorStrategy>` melalui constructor injection dan dipetakan ke `Map<VehicleType, MaintenanceAdvisorStrategy>`. |
| **Advance Native SQL (CTE & Window Functions)** | [`ServiceRecordRepository.java`](maintenance-core-service/src/main/java/com/garagelog/core/repository/ServiceRecordRepository.java)<br>[`TelemetryHistoryProjection.java`](maintenance-core-service/src/main/java/com/garagelog/core/repository/projection/TelemetryHistoryProjection.java) | Query native PostgreSQL menggunakan `WITH` (CTE), `LAG(...) OVER (...)` untuk selisih jarak antar servis, `SUM(cost) OVER (...)` untuk akumulasi biaya, serta kalkulasi status (`OVERDUE`, `DUE_SOON`, `HEALTHY`). |
| **Java Stream API** | [`VehicleMaintenanceService.java`](maintenance-core-service/src/main/java/com/garagelog/core/service/VehicleMaintenanceService.java) (`getCostAnalytics`) | Agregasi biaya total, rata-rata per servis, biaya per km, dan breakdown persentase per kategori (`Collectors.groupingBy`, `Collectors.summarizingDouble`, comparator descending). |
| **Redis — Cache-Aside** | [`RedisCacheConfig.java`](maintenance-core-service/src/main/java/com/garagelog/core/config/RedisCacheConfig.java)<br>[`VehicleMaintenanceService.java`](maintenance-core-service/src/main/java/com/garagelog/core/service/VehicleMaintenanceService.java) | `@Cacheable` (TTL 10 menit, JSON serializer) pada `cost-analytics`, `@CacheEvict` saat ada record baru, serta `RedisTemplate` untuk `vehicle:latest-status:{vehicleId}`. |
| **Redis — Idempotency Key** | [`IdempotencyService.java`](maintenance-core-service/src/main/java/com/garagelog/core/service/IdempotencyService.java)<br>[`ServiceRecordController.java`](maintenance-core-service/src/main/java/com/garagelog/core/controller/ServiceRecordController.java) | Mencegah *double-submit* pada `POST /records` melalui header `X-Idempotency-Key` yang disimpan di Redis (`idempotency:service-record:{key}`, TTL 24 jam) sebelum membuka transaksi DB. |
| **Kafka Producer & Transaction Sync** | [`KafkaTopicConfig.java`](maintenance-core-service/src/main/java/com/garagelog/core/config/KafkaTopicConfig.java)<br>[`ServiceEventPublisher.java`](maintenance-core-service/src/main/java/com/garagelog/core/event/ServiceEventPublisher.java)<br>[`ServiceLoggedEvent.java`](maintenance-core-service/src/main/java/com/garagelog/core/event/ServiceLoggedEvent.java) | Publikasi event ke topic `garagelog.service.logged` (3 partisi, key `vehicleId`) di dalam `TransactionSynchronization.afterCommit()` agar bebas *ghost event*. |
| **Kafka Consumer & Error Handling** | [`ServiceEventConsumer.java`](diagnostic-search-service/src/main/java/com/garagelog/diagnostic/consumer/ServiceEventConsumer.java)<br>[`KafkaConsumerConfig.java`](diagnostic-search-service/src/main/java/com/garagelog/diagnostic/config/KafkaConsumerConfig.java) | Konsumsi event asinkron, *enrichment* otomatis `urgentMaintenanceFlag`, dan `DefaultErrorHandler` dengan `FixedBackOff` (retry 2x). |
| **Elasticsearch Full-Text Search** | [`ServiceDiagnosticDocument.java`](diagnostic-search-service/src/main/java/com/garagelog/diagnostic/document/ServiceDiagnosticDocument.java)<br>[`DiagnosticSearchService.java`](diagnostic-search-service/src/main/java/com/garagelog/diagnostic/service/DiagnosticSearchService.java)<br>[`DiagnosticSearchController.java`](diagnostic-search-service/src/main/java/com/garagelog/diagnostic/controller/DiagnosticSearchController.java) | Indexing idempoten dengan `@Id recordId` serta pencarian `BoolQuery` (`multiMatch` dengan `fuzziness: AUTO` + `queryString` wildcard + filter `category` & `urgentOnly`). |
| **Docker Multi-Stage Containerization** | [`maintenance-core-service/Dockerfile`](maintenance-core-service/Dockerfile)<br>[`diagnostic-search-service/Dockerfile`](diagnostic-search-service/Dockerfile)<br>[`docker-compose.yml`](docker-compose.yml) | Build stage (`maven:3.9.9-eclipse-temurin-17-alpine`) & runtime stage (`eclipse-temurin:17-jre-alpine` non-root) dengan `depends_on: service_healthy`. |

---

## Cara Menjalankan (1 Perintah)

Pastikan **Docker Desktop** sudah berjalan, lalu jalankan perintah berikut dari root folder repository:

```bash
docker compose up -d --build
```

Perintah di atas akan otomatis membangun dan menjalankan **6 container**:
1. `garagelog-postgres` (Port `5432`, otomatis ter-seed dengan 2 kendaraan & 9 riwayat servis)
2. `garagelog-redis` (Port `6379`)
3. `garagelog-kafka` (Port `9092`)
4. `garagelog-elasticsearch` (Port `9200`)
5. `garagelog-maintenance-core` (Port `8081`)
6. `garagelog-diagnostic-search` (Port `8082`)

Cek status seluruh container hingga sehat/berjalan:
```bash
docker compose ps
```

---

## Panduan Pengujian Endpoint (`curl`)

### 1. Daftar Kendaraan (`GET /api/v1/vehicles`)
```bash
curl -s http://localhost:8081/api/v1/vehicles
```
Contoh respons:
```json
[
  {
    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "plateNumber": "B 4123 KLF",
    "brandModel": "Honda Vario 160 ABS",
    "vehicleType": "ICE_MOTORCYCLE",
    "currentOdometer": 18500
  },
  {
    "id": "b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22",
    "plateNumber": "D 5892 EV",
    "brandModel": "Vespa Super 150 Konversi EV",
    "vehicleType": "EV_MOTORCYCLE",
    "currentOdometer": 6200
  }
]
```

### 2. Catat Servis Baru + Idempotency Key (`POST /api/v1/vehicles/{vehicleId}/records`)
```bash
curl -s -X POST http://localhost:8081/api/v1/vehicles/b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22/records \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: req-ev-001" \
  -d '{
    "serviceDate": "2026-11-18",
    "odometerAtService": 6800,
    "category": "BATTERY_ELECTRICAL",
    "workshopName": "Sinarmas EV Care Center",
    "cost": 650000.00,
    "mechanicNotes": "Pengecekan SOH baterai turun dan terasa getar pada mounting motor mid-drive",
    "nextRecommendedKm": 9500
  }'
```
Contoh respons (`201 Created`):
```json
{
  "id": "51467751-7f9d-4d0a-b8ba-d4542971edb3",
  "vehicleId": "b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22",
  "serviceDate": "2026-11-18",
  "odometerAtService": 6800,
  "category": "BATTERY_ELECTRICAL",
  "workshopName": "Sinarmas EV Care Center",
  "cost": 650000.0,
  "mechanicNotes": "Pengecekan SOH baterai turun dan terasa getar pada mounting motor mid-drive",
  "nextRecommendedKm": 9500,
  "advisorAdvice": "[EV Advisor] Odometer saat ini: 6800 km. Powertrain elektrik bebas ganti oli mesin..."
}
```
> Mengirim ulang request yang sama dengan header `X-Idempotency-Key: req-ev-001` akan langsung mengembalikan respons tersimpan dari Redis tanpa menduplikasi data di PostgreSQL maupun event Kafka.

### 3. Riwayat Telemetri & Window Functions (`GET /api/v1/vehicles/{vehicleId}/telemetry-history`)
```bash
curl -s http://localhost:8081/api/v1/vehicles/b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22/telemetry-history
```

### 4. Analitik Biaya & Redis Cache (`GET /api/v1/vehicles/{vehicleId}/cost-analytics`)
```bash
curl -s http://localhost:8081/api/v1/vehicles/b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22/cost-analytics
```

### 5. Full-Text Search Diagnosa di Elasticsearch (`GET /api/v1/diagnostics/search`)
```bash
curl -s "http://localhost:8082/api/v1/diagnostics/search?q=getar&category=BATTERY_ELECTRICAL&urgentOnly=true"
```

### 6. Daftar Peringatan Servis Urgent (`GET /api/v1/diagnostics/urgent-alerts`)
```bash
curl -s http://localhost:8082/api/v1/diagnostics/urgent-alerts
```
