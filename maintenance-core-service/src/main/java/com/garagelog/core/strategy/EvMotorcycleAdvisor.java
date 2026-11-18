package com.garagelog.core.strategy;

import com.garagelog.core.entity.ServiceCategory;
import com.garagelog.core.entity.ServiceRecord;
import com.garagelog.core.entity.Vehicle;
import com.garagelog.core.entity.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class EvMotorcycleAdvisor implements MaintenanceAdvisorStrategy {

    private static final int BMS_INSPECTION_INTERVAL_KM = 3000;
    private static final int BRAKE_CHECK_INTERVAL_KM = 4000;

    @Override
    public VehicleType getSupportedType() {
        return VehicleType.EV_MOTORCYCLE;
    }

    @Override
    public String evaluateNextServiceAdvice(Vehicle vehicle, ServiceRecord latestRecord) {
        int currentKm = vehicle.getCurrentOdometer();
        int nextBmsKm = currentKm + BMS_INSPECTION_INTERVAL_KM;
        int nextBrakeKm = currentKm + BRAKE_CHECK_INTERVAL_KM;

        StringBuilder advice = new StringBuilder();
        advice.append(String.format(
            "[EV Advisor] Odometer saat ini: %d km. Powertrain elektrik bebas ganti oli mesin. ",
            currentKm
        ));

        if (latestRecord.getCategory() == ServiceCategory.BATTERY_ELECTRICAL) {
            advice.append(String.format(
                "Pemeriksaan kelistrikan/baterai tercatat. Jadwal diagnosa State-Of-Health (SOH) sel baterai dan konektor BMS berikutnya pada %d km. ",
                nextBmsKm
            ));
        } else {
            advice.append(String.format(
                "Pantau delta tegangan tiap sel baterai via BMS dan pastikan suhu controller stabil. "
            ));
        }

        advice.append(String.format(
            "Verifikasi efektivitas kampas rem regeneratif pada %d km, dan cek tekanan angin ban mingguan untuk efisiensi konsumsi Wh/km optimal.",
            nextBrakeKm
        ));

        return advice.toString();
    }
}
