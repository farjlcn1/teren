package com.sledenje.teren.user.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceConflictException;

public class UserEmailAlreadyExistsException extends ResourceConflictException {

    public UserEmailAlreadyExistsException() {
        super("Uporabnik s tem emailom že obstaja.");
    }

}
