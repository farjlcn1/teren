package com.sledenje.teren.common.domain.exception;

/**
 * Operacija ni mogoča zaradi trenutnega stanja zapisa (npr. nanj se sklicujejo drugi zapisi).
 * Presentation nivo jo pretvori v HTTP 409.
 */
public abstract class ResourceConflictException extends DomainException {

    protected ResourceConflictException(String message) {
        super(message);
    }

}
