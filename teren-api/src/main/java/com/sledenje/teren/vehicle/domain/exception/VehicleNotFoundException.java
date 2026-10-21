package com.sledenje.teren.vehicle.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceNotFoundException;

public class VehicleNotFoundException extends ResourceNotFoundException {

    public VehicleNotFoundException(String vehicleId) {
        super("Vozilo z ID-jem '" + vehicleId + "' ne obstaja.");
    }

}
