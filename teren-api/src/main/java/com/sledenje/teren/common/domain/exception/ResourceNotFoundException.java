package com.sledenje.teren.common.domain.exception;

/**
 * Zahtevani zapis ne obstaja. Presentation nivo jo pretvori v HTTP 404.
 */
public abstract class ResourceNotFoundException extends DomainException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }

}
