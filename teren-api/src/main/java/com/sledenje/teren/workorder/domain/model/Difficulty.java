package com.sledenje.teren.workorder.domain.model;

/**
 * Zahtevnost posega. Vrednosti se ujemajo s Postgres enumom "Difficulty".
 */
public enum Difficulty {
    OSNOVNA,
    ZAHTEVNA
}
