package com.sledenje.teren.plan.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceConflictException;

/**
 * Dogodka z že opravljenim nalogom ni mogoče izbrisati.
 */
public class PlanGroupCompletedException extends ResourceConflictException {

    public PlanGroupCompletedException() {
        super("Dogodka ni mogoče izbrisati, ker vsebuje že opravljen nalog.");
    }

}
