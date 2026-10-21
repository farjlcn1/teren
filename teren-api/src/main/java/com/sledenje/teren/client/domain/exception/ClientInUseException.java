package com.sledenje.teren.client.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceConflictException;

public class ClientInUseException extends ResourceConflictException {

    public ClientInUseException() {
        super("Stranke ni mogoče izbrisati, ker ima povezane delovne naloge ali dogodke v planu.");
    }

}
