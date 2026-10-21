package com.sledenje.teren.client.domain.exception;

import com.sledenje.teren.common.domain.exception.ExternalServiceException;

public class ClientSyncFailedException extends ExternalServiceException {

    private final String reason;

    /**
     * @param reason kratek razlog brez internih podrobnosti (npr. "HTTP 500"); shrani se tudi kot status sinhronizacije
     */
    public ClientSyncFailedException(String reason) {
        super("Sinhronizacija ni uspela: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}
