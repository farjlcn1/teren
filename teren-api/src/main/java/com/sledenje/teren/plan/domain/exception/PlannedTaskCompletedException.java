package com.sledenje.teren.plan.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceConflictException;

public class PlannedTaskCompletedException extends ResourceConflictException {

    public PlannedTaskCompletedException() {
        super("Opravljenega naloga ni mogoče izbrisati.");
    }

}
