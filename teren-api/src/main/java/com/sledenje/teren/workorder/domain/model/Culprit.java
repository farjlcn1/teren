package com.sledenje.teren.workorder.domain.model;

/**
 * Kdo je povzročil intervencijo. Vrednosti se ujemajo s Postgres enumom "Culprit".
 */
public enum Culprit {
    SLEDENJE,
    STRANKA
}
