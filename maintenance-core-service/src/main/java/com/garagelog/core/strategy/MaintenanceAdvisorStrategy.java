package com.garagelog.core.strategy;

import com.garagelog.core.entity.ServiceRecord;
import com.garagelog.core.entity.Vehicle;
import com.garagelog.core.entity.VehicleType;

public interface MaintenanceAdvisorStrategy {
    VehicleType getSupportedType();
    String evaluateNextServiceAdvice(Vehicle vehicle, ServiceRecord latestRecord);
}
