package com.sledenje.teren.plan.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceNotFoundException;

public class PlanGroupNotFoundException extends ResourceNotFoundException {

    public PlanGroupNotFoundException() {
        super("Dogodek ne obstaja.");
    }

}
