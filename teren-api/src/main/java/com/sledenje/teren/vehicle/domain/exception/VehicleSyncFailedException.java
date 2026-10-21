package com.sledenje.teren.vehicle.domain.exception;

import com.sledenje.teren.common.domain.exception.ExternalServiceException;

public class VehicleSyncFailedException extends ExternalServiceException {

    private final String reason;

    /**
     * @param reason kratek razlog brez internih podrobnosti (npr. "HTTP 500"); shrani se tudi kot status sinhronizacije
     */
    public VehicleSyncFailedException(String reason) {
        super("Sinhronizacija ni uspela: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}
