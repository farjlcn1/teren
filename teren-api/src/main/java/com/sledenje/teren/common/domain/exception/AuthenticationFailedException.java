package com.sledenje.teren.common.domain.exception;

/**
 * Podatki za prijavo niso pravilni. Presentation nivo jo pretvori v HTTP 401.
 */
public abstract class AuthenticationFailedException extends DomainException {

    protected AuthenticationFailedException(String message) {
        super(message);
    }

}
