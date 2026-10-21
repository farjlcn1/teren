package com.sledenje.teren.vehicle.domain.exception;

import com.sledenje.teren.common.domain.exception.DomainException;

public class VehicleSyncNotConfiguredException extends DomainException {

    public VehicleSyncNotConfiguredException() {
        super("Zunanja povezava še ni nastavljena.");
    }

}
