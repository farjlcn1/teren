package com.sledenje.teren.user.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String userId) {
        super("Uporabnik z ID-jem '" + userId + "' ne obstaja.");
    }

}
