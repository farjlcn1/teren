package com.sledenje.teren.client.domain.exception;

import com.sledenje.teren.common.domain.exception.ResourceNotFoundException;

public class ClientNotFoundException extends ResourceNotFoundException {

    public ClientNotFoundException(String clientId) {
        super("Stranka z ID-jem '" + clientId + "' ne obstaja.");
    }

}
