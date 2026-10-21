package com.sledenje.teren.vehicle.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceConflictException;

public class VehiclePlateAlreadyExistsException extends ResourceConflictException {

    public VehiclePlateAlreadyExistsException(String plate) {
        super("Registrska št. " + plate + " že obstaja.");
    }

}
