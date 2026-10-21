package com.sledenje.teren.plan.domain.exception;

import com.sledenje.teren.common.domain.exception.DomainException;

/**
 * Podatki dogodka ne ustrezajo pravilom. Sporočilo je namenjeno uporabniku plana.
 */
public class InvalidPlanGroupException extends DomainException {

    public InvalidPlanGroupException(String message) {
        super(message);
    }

}
