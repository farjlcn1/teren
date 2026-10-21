package com.sledenje.teren.user.domain.service;

/**
 * Zgoščevanje gesel. Združljivo z obstoječimi hashi iz aplikacije Teren (bcryptjs, $2b$).
 */
public interface PasswordHasher {

    String hash(String password);

    boolean matches(String password, String passwordHash);

}
