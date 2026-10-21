package com.sledenje.teren.user.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceConflictException;

public class UserInUseException extends ResourceConflictException {

    public UserInUseException() {
        super("Uporabnika ni mogoče izbrisati, ker ima delovne naloge, urejanja ali dogodke v planu. "
                + "Namesto tega ga onemogoči.");
    }

}
