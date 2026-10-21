package com.sledenje.teren.client.domain.exception;

import com.sledenje.teren.common.domain.exception.DomainException;

public class ClientSyncNotConfiguredException extends DomainException {

    public ClientSyncNotConfiguredException() {
        super("Zunanja povezava še ni nastavljena.");
    }

}
