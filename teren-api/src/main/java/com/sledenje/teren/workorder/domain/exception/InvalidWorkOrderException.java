package com.sledenje.teren.workorder.domain.exception;

import com.sledenje.teren.common.domain.exception.DomainException;

/**
 * Vsebina naloga ne ustreza pravilom. Sporočilo je namenjeno monterju.
 */
public class InvalidWorkOrderException extends DomainException {

    public InvalidWorkOrderException(String message) {
        super(message);
    }

}
