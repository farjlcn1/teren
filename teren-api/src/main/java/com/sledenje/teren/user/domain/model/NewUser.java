package com.sledenje.teren.user.domain.model;

/**
 * Podatki za vnos novega uporabnika. Nov uporabnik je aktiven in nima pravic.
 *
 * @param password geslo v čisti obliki; shrani se samo njegov BCrypt hash
 */
public record NewUser(String email, String fullName, String password) {
}
