package com.garagelog.core.strategy;

import com.garagelog.core.entity.ServiceCategory;
import com.garagelog.core.entity.ServiceRecord;
import com.garagelog.core.entity.Vehicle;
import com.garagelog.core.entity.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class IceMotorcycleAdvisor implements MaintenanceAdvisorStrategy {

    private static final int OIL_CHANGE_INTERVAL_KM = 2500;
    private static final int SPARK_PLUG_INTERVAL_KM = 8000;
    private static final int DRIVETRAIN_CVT_INTERVAL_KM = 8000;

    @Override
    public VehicleType getSupportedType() {
        return VehicleType.ICE_MOTORCYCLE;
    }

    @Override
    public String evaluateNextServiceAdvice(Vehicle vehicle, ServiceRecord latestRecord) {
        int currentKm = vehicle.getCurrentOdometer();
        int nextOilKm = currentKm + OIL_CHANGE_INTERVAL_KM;
        int nextDrivetrainKm = currentKm + DRIVETRAIN_CVT_INTERVAL_KM;

        StringBuilder advice = new StringBuilder();
        advice.append(String.format(
            "[ICE Advisor] Odometer saat ini: %d km. ", currentKm
        ));

        if (latestRecord.getCategory() == ServiceCategory.ENGINE_OIL) {
            advice.append(String.format(
                "Oli mesin baru saja diganti. Jadwal ganti oli mesin berikutnya pada %d km (+%d km). ",
                nextOilKm, OIL_CHANGE_INTERVAL_KM
            ));
        } else {
            advice.append(String.format(
                "Pastikan level dan kekentalan oli mesin terpantau (interval standar setiap %d km). ",
                OIL_CHANGE_INTERVAL_KM
            ));
        }

        advice.append(String.format(
            "Cek ketegangan rantai atau kebersihan roller/v-belt CVT pada target %d km, serta inspeksi elektroda busi setiap %d km.",
            nextDrivetrainKm, SPARK_PLUG_INTERVAL_KM
        ));

        return advice.toString();
    }
}
