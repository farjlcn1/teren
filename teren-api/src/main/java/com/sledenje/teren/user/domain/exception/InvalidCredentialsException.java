package com.sledenje.teren.user.domain.exception;

import com.sledenje.teren.common.domain.exception.AuthenticationFailedException;

/**
 * Enako sporočilo za neobstoječ email, napačno geslo in onemogočenega uporabnika, da se ne razkrije obstoj računa.
 */
public class InvalidCredentialsException extends AuthenticationFailedException {

    public InvalidCredentialsException() {
        super("Napačen email ali geslo.");
    }

}
