package com.sledenje.teren.user.domain.service;

import com.sledenje.teren.user.domain.exception.InvalidCredentialsException;
import com.sledenje.teren.user.domain.model.User;

public interface AuthenticationService {

    /**
     * Preveri email in geslo. Omejevanje števila poskusov izvaja aplikacija Teren pred klicem.
     *
     * @return prijavljeni uporabnik
     * @throws InvalidCredentialsException če uporabnik ne obstaja, geslo ni pravilno ali je uporabnik onemogočen
     */
    User authenticate(String email, String password);

}
