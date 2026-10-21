package com.sledenje.teren.plan.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceNotFoundException;

public class PlannedTaskNotFoundException extends ResourceNotFoundException {

    public PlannedTaskNotFoundException() {
        super("Nalog ne obstaja.");
    }

}
