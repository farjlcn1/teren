package com.sledenje.teren.user.domain.exception;

import com.sledenje.teren.common.domain.exception.DomainException;

public class WeakPasswordException extends DomainException {

    public WeakPasswordException(String violation) {
        super(violation);
    }

}
