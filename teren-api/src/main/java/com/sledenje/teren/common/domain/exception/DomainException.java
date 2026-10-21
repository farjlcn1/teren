package com.sledenje.teren.common.domain.exception;

/**
 * Osnova za vse tipizirane napake domenske logike.
 * Sporočilo je namenjeno uporabniku API-ja, zato ne sme vsebovati internih podatkov.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

}
