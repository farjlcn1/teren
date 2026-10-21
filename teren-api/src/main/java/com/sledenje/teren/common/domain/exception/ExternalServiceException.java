package com.sledenje.teren.common.domain.exception;

/**
 * Zunanji sistem, od katerega je API odvisen, ni odgovoril pravilno.
 * Presentation nivo jo pretvori v HTTP 502.
 */
public abstract class ExternalServiceException extends DomainException {

    protected ExternalServiceException(String message) {
        super(message);
    }

}
