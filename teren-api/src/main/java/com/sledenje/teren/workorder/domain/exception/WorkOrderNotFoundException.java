package com.sledenje.teren.workorder.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceNotFoundException;

public class WorkOrderNotFoundException extends ResourceNotFoundException {

    public WorkOrderNotFoundException(String workOrderId) {
        super("Delovni nalog z ID-jem '" + workOrderId + "' ne obstaja.");
    }

}
