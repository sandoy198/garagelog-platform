package com.garagelog.core.strategy;

import com.garagelog.core.entity.ServiceCategory;
import com.garagelog.core.entity.ServiceRecord;
import com.garagelog.core.entity.Vehicle;
import com.garagelog.core.entity.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class CarAdvisor implements MaintenanceAdvisorStrategy {

    private static final int OIL_CHANGE_INTERVAL_KM = 10000;
    private static final int TIRE_ROTATION_INTERVAL_KM = 10000;

    @Override
    public VehicleType getSupportedType() {
        return VehicleType.CAR;
    }

    @Override
    public String evaluateNextServiceAdvice(Vehicle vehicle, ServiceRecord latestRecord) {
        int currentKm = vehicle.getCurrentOdometer();
        int nextOilKm = currentKm + OIL_CHANGE_INTERVAL_KM;

        StringBuilder advice = new StringBuilder();
        advice.append(String.format(
            "[Car Advisor] Odometer saat ini: %d km. ", currentKm
        ));

        if (latestRecord.getCategory() == ServiceCategory.ENGINE_OIL) {
            advice.append(String.format(
                "Oli mesin dan filter oli telah diganti. Jadwal penggantian oli berikutnya pada %d km (+%d km). ",
                nextOilKm, OIL_CHANGE_INTERVAL_KM
            ));
        } else {
            advice.append(String.format(
                "Periksa interval ganti oli mesin sintetis standar setiap %d km. ",
                OIL_CHANGE_INTERVAL_KM
            ));
        }

        advice.append(String.format(
            "Rekomendasi berikutnya: lakukan rotasi ban dan balancing pada %d km, serta periksa filter udara kabin dan mesin.",
            nextOilKm
        ));

        return advice.toString();
    }
}
