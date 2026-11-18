-- GarageLog Database Initialization Script
-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table: vehicles
CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    brand_model VARCHAR(100) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL CHECK (vehicle_type IN ('ICE_MOTORCYCLE', 'EV_MOTORCYCLE', 'CAR')),
    current_odometer INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table: service_records
CREATE TABLE IF NOT EXISTS service_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    service_date DATE NOT NULL,
    odometer_at_service INTEGER NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('ENGINE_OIL', 'BRAKE_SYSTEM', 'BATTERY_ELECTRICAL', 'TIRES_DRIVETRAIN', 'GENERAL_SERVICE')),
    workshop_name VARCHAR(150) NOT NULL,
    cost NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    mechanic_notes TEXT,
    next_recommended_km INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance & query optimization
CREATE INDEX IF NOT EXISTS idx_vehicles_plate_number ON vehicles(plate_number);
CREATE INDEX IF NOT EXISTS idx_vehicles_vehicle_type ON vehicles(vehicle_type);
CREATE INDEX IF NOT EXISTS idx_service_records_vehicle_id ON service_records(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_service_records_service_date ON service_records(service_date DESC);
CREATE INDEX IF NOT EXISTS idx_service_records_category ON service_records(category);

-- Realistic Data Seeding
-- Vehicle 1: Motor Bensin Komuter (Honda Vario 160 ABS)
-- Vehicle 2: Motor Listrik Konversi (Vespa Super 150 Konversi EV)
INSERT INTO vehicles (id, plate_number, brand_model, vehicle_type, current_odometer, created_at)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'B 4123 KLF', 'Honda Vario 160 ABS', 'ICE_MOTORCYCLE', 18500, CURRENT_TIMESTAMP - INTERVAL '14 MONTHS'),
    ('b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22', 'D 5892 EV', 'Vespa Super 150 Konversi EV', 'EV_MOTORCYCLE', 6200, CURRENT_TIMESTAMP - INTERVAL '10 MONTHS')
ON CONFLICT (plate_number) DO NOTHING;

-- Service Records for Vehicle 1 (ICE Motorcycle - Honda Vario 160)
INSERT INTO service_records (id, vehicle_id, service_date, odometer_at_service, category, workshop_name, cost, mechanic_notes, next_recommended_km, created_at)
VALUES
    ('c201a111-1111-4000-8000-000000000001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', CURRENT_DATE - INTERVAL '13 MONTHS', 1050, 'ENGINE_OIL', 'AHASS Astra Motor Center', 125000.00, 'Servis berkala 1.000 km pertama. Ganti oli mesin SPX2, kuras gram oli mesin baru, dan pengecekan torsi baut as roda.', 4000, CURRENT_TIMESTAMP - INTERVAL '13 MONTHS'),
    ('c201a111-1111-4000-8000-000000000002', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', CURRENT_DATE - INTERVAL '10 MONTHS', 4200, 'ENGINE_OIL', 'Sahabat Motor Service', 140000.00, 'Ganti oli mesin MPX2 dan penggantian oli transmisi gardan matic. Filter udara dibersihkan.', 8000, CURRENT_TIMESTAMP - INTERVAL '10 MONTHS'),
    ('c201a111-1111-4000-8000-000000000003', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', CURRENT_DATE - INTERVAL '7 MONTHS', 8600, 'TIRES_DRIVETRAIN', 'Proban Motoparts Express', 485000.00, 'Ganti ban belakang tubeless ukuran 120/70-14. Servis pembersihan rumah CVT, v-belt dalam kondisi baik, penggantian roller set 15 gram.', 12000, CURRENT_TIMESTAMP - INTERVAL '7 MONTHS'),
    ('c201a111-1111-4000-8000-000000000004', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', CURRENT_DATE - INTERVAL '4 MONTHS', 12450, 'BRAKE_SYSTEM', 'Sentosa Speed Workshop', 195000.00, 'Penggantian kampas rem cakram depan original Nissin dan pengurasan oli rem DOT 4. Cek clearance tuas rem belakang.', 16000, CURRENT_TIMESTAMP - INTERVAL '4 MONTHS'),
    ('c201a111-1111-4000-8000-000000000005', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', CURRENT_DATE - INTERVAL '1 MONTHS', 17100, 'GENERAL_SERVICE', 'AHASS Daya Motor Sukses', 335000.00, 'Servis besar: ganti oli SPX2, penggantian filter udara cartridge, busi NGK Laser Iridium, dan infus pembersihan injector.', 21000, CURRENT_TIMESTAMP - INTERVAL '1 MONTHS')
ON CONFLICT (id) DO NOTHING;

-- Service Records for Vehicle 2 (EV Motorcycle Conversion - Vespa Super 150)
INSERT INTO service_records (id, vehicle_id, service_date, odometer_at_service, category, workshop_name, cost, mechanic_notes, next_recommended_km, created_at)
VALUES
    ('d302b222-2222-4000-8000-000000000001', 'b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22', CURRENT_DATE - INTERVAL '9 MONTHS', 520, 'BATTERY_ELECTRICAL', 'Petrikbike Workshop Listrik', 250000.00, 'Inspeksi awal pasca konversi 500 km: balancing cell baterai LiFePO4 72V 40Ah via smart BMS, kencangkan terminal busbar, dan cek temperatur konektor charger.', 2000, CURRENT_TIMESTAMP - INTERVAL '9 MONTHS'),
    ('d302b222-2222-4000-8000-000000000002', 'b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22', CURRENT_DATE - INTERVAL '7 MONTHS', 1980, 'TIRES_DRIVETRAIN', 'Elders Custom EV Lab', 180000.00, 'Penyetelan ketegangan rantai penggerak motor listrik mid-drive 3kW, pelumasan bearing arm belakang, dan cek kelurusan sproket.', 4000, CURRENT_TIMESTAMP - INTERVAL '7 MONTHS'),
    ('d302b222-2222-4000-8000-000000000003', 'b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22', CURRENT_DATE - INTERVAL '4 MONTHS', 3850, 'BRAKE_SYSTEM', 'Bintang Timur Retrofit', 230000.00, 'Upgrade master rem hidrolik dan kampas rem sintered metal. Setting ulang pengereman regeneratif (regen braking) pada controller Votol EM150-2.', 5500, CURRENT_TIMESTAMP - INTERVAL '4 MONTHS'),
    ('d302b222-2222-4000-8000-000000000004', 'b1ffcd88-8b1a-4de7-aa5c-5aa8ac270b22', CURRENT_DATE - INTERVAL '1 MONTHS', 5700, 'BATTERY_ELECTRICAL', 'Sinarmas EV Care Center', 320000.00, 'Penggantian thermal paste pendingin controller, kalibrasi kurva respon throttle magnetik Hall effect, dan uji proteksi insulasi tegangan tinggi sistem 72V.', 8000, CURRENT_TIMESTAMP - INTERVAL '1 MONTHS')
ON CONFLICT (id) DO NOTHING;
